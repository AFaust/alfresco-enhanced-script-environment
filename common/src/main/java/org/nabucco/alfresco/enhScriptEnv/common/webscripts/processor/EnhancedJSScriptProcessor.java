/*
 * Copyright 2013 PRODYNA AG
 *
 * Licensed under the Eclipse Public License (EPL), Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/eclipse-1.0.php or
 * http://www.nabucco.org/License.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.nabucco.alfresco.enhScriptEnv.common.webscripts.processor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.WrappedException;
import org.nabucco.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import org.nabucco.alfresco.enhScriptEnv.common.script.ScopeContributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.extensions.surf.core.processor.ProcessorExtension;
import org.springframework.extensions.webscripts.ScriptContent;
import org.springframework.extensions.webscripts.ScriptLoader;
import org.springframework.extensions.webscripts.ScriptProcessor;
import org.springframework.extensions.webscripts.ScriptValueConverter;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.processor.BaseRegisterableScriptProcessor;
import org.springframework.extensions.webscripts.processor.JSScriptProcessor.PresentationWrapFactory;
import org.springframework.util.FileCopyUtils;

/**
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class EnhancedJSScriptProcessor extends BaseRegisterableScriptProcessor implements ScriptProcessor,
        EnhancedScriptProcessor<ScriptContentAdapter>, InitializingBean
{
    private static final String CLASSPATH_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s+)*=(\\s*\\n*\\s+)*\"classpath:(/)?([^\"]+)\"(\\s*\\n*\\s+)*(/)?>";
    private static final String CLASSPATH_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"classpath\", \"/$5\", true);";

    private static final String STORE_PATH_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s+)*=(\\s*\\n*\\s+)*\"([^\"]+)\"(\\s*\\n*\\s+)*(/)?>";
    private static final String STORE_PATH_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"storePath\", \"$4\", true);";

    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedJSScriptProcessor.class);

    // reuse existing implementation
    private static final WrapFactory WRAP_FACTORY = new PresentationWrapFactory();

    protected final ThreadLocal<List<ScriptContentAdapter>> activeScriptContentChain = new ThreadLocal<List<ScriptContentAdapter>>();
    protected final ThreadLocal<List<List<ScriptContentAdapter>>> recursionScriptContentChains = new ThreadLocal<List<List<ScriptContentAdapter>>>();

    protected boolean shareScopes = true;

    protected Scriptable unrestrictedShareableScope;
    protected Scriptable restrictedShareableScope;

    protected boolean compileScripts = true;

    protected ScriptLoader standardScriptLoader;

    protected final Map<String, Script> scriptCache = new ConcurrentHashMap<String, Script>(256);

    protected final Collection<ScopeContributor> registeredContributors = new HashSet<ScopeContributor>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "standardScriptLoader", this.standardScriptLoader);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return "javascript";
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getExtension()
    {
        return "js";
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void executeInScope(final ScriptContentAdapter content, final Object scope)
    {
        ParameterCheck.mandatory("content", content);

        final Script script = this.getCompiledScript(content);
        final String debugScriptName;

        {
            final String path = content.getPath();
            final int i = path.lastIndexOf('/');
            debugScriptName = i != -1 ? path.substring(i + 1) : path;
        }

        LOGGER.info("{} Start", debugScriptName);

        // TODO: Can we always be sure we're continuing the previous chain?
        final List<ScriptContentAdapter> currentChain = this.activeScriptContentChain.get();
        if (currentChain != null)
        {
            // preliminary safeguard against incorrect invocation - see TODO notice
            currentChain.add(content);
        }

        final long startTime = System.currentTimeMillis();
        final Context cx = Context.enter();
        try
        {

            final Scriptable realScope;
            if (scope == null || !(scope instanceof Scriptable))
            {
                if (this.shareScopes)
                {
                    final Scriptable sharedScope = content.isSecure() ? this.unrestrictedShareableScope : this.restrictedShareableScope;
                    realScope = cx.newObject(sharedScope);
                    realScope.setPrototype(sharedScope);
                    realScope.setParentScope(null);
                }
                else
                {
                    realScope = this.setupScope(cx, content.isSecure(), false);
                }
            }
            else
            {
                realScope = (Scriptable) scope;
            }

            this.executeScriptInScopeImpl(script, realScope);
        }
        catch (final Exception ex)
        {
            // TODO: error handling / bubbling to caller? how to handle Rhino exceptions if caller is not a script?
            LOGGER.info("{} Exception: {}", debugScriptName, ex);
            throw new WebScriptException(MessageFormat.format("Failed to execute script {0}: {1}", content.toString(), ex.getMessage()), ex);
        }
        finally
        {
            Context.exit();
            if (currentChain != null)
            {
                // preliminary safeguard against incorrect invocation - see TODO notice
                currentChain.remove(currentChain.size() - 1);
            }

            final long endTime = System.currentTimeMillis();
            LOGGER.info("{} End {} msg", debugScriptName, Long.valueOf(endTime - startTime));
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public ScriptContentAdapter getContextScriptLocation()
    {
        final List<ScriptContentAdapter> currentChain = this.activeScriptContentChain.get();
        final ScriptContentAdapter result;
        if (currentChain != null && !currentChain.isEmpty())
        {
            result = currentChain.get(currentChain.size() - 1);
        }
        else
        {
            result = null;
        }
        return result;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void init()
    {
        Context cx = Context.enter();
        try
        {
            cx.setWrapFactory(WRAP_FACTORY);
            this.unrestrictedShareableScope = setupScope(cx, true, false);
        }
        finally
        {
            Context.exit();
        }

        cx = Context.enter();
        try
        {
            cx.setWrapFactory(WRAP_FACTORY);
            this.restrictedShareableScope = setupScope(cx, false, false);
        }
        finally
        {
            Context.exit();
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void register()
    {
        this.getScriptProcessorRegistry().registerScriptProcessor(this);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public ScriptContent findScript(final String path)
    {
        final ScriptContent script = this.standardScriptLoader.getScript(path);
        return script;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Object executeScript(final String path, final Map<String, Object> model)
    {
        // locate script within web script stores
        final ScriptContent scriptLocation = findScript(path);
        if (scriptLocation == null)
        {
            LOGGER.info("Unable to locate script {}", path);
            throw new WebScriptException(MessageFormat.format("Unable to locate script {0}", path));
        }
        // execute script
        return executeScript(scriptLocation, model);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Object executeScript(final ScriptContent content, final Map<String, Object> model)
    {
        ParameterCheck.mandatory("content", content);
        final Script script = this.getCompiledScript(content);
        final String debugScriptName;
        {
            final String path = content.getPath();
            final int i = path.lastIndexOf('/');
            debugScriptName = i != -1 ? path.substring(i + 1) : path;
        }

        this.updateContentChainsBeforeExceution();
        this.activeScriptContentChain.get().add(new ScriptContentAdapter(content, this.standardScriptLoader));
        try
        {
            return this.executeScriptImpl(script, model, content.isSecure(), debugScriptName);
        }
        finally
        {
            this.updateContentChainsAfterReturning();
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Object unwrapValue(final Object value)
    {
        return ScriptValueConverter.unwrapValue(value);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void reset()
    {
        this.scriptCache.clear();
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void registerScopeContributor(final ScopeContributor contributor)
    {
        if (contributor != null)
        {
            // can use synchronized here since scope creation / registration should not occur that often in a relevant production scenario
            // (when immutable scopes are shared)
            synchronized (this.registeredContributors)
            {
                this.registeredContributors.add(contributor);
            }
        }
    }

    protected Script getCompiledScript(final ScriptContent content)
    {
        Script script = null;
        final String path = content.getPath();
        final String realPath;

        // check if the path is in classpath form
        // TODO: can we generalize external form file:// to a classpath-relative location? (best-effort)
        if (!path.matches("^(classpath(\\*)?:)+.*$"))
        {
            // take path as is - can be anything depending on how content is loaded
            realPath = path;
        }
        else
        {
            // we always want to have a fully-qualified file-protocol path (unless we can generalize all to classpath-relative locations)
            realPath = this.getClass().getClassLoader().getResource(path.substring(path.indexOf(':') + 1)).toExternalForm();
        }

        if (this.compileScripts && content.isCachable())
        {
            script = this.scriptCache.get(content.getPath());
        }

        if (script == null)
        {
            LOGGER.debug("Resolving and compiling script path: {}", path);

            try
            {
                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                FileCopyUtils.copy(content.getInputStream(), os); // both streams are closed
                final byte[] bytes = os.toByteArray();
                final String source = new String(bytes, "UTF-8");
                script = this.getCompiledScript(source, realPath);
            }
            catch (final IOException ex)
            {
                LOGGER.error("Failed to compile supplied script", ex);
                throw new WebScriptException(MessageFormat.format("Failed to load supplied script: {0}", ex.getMessage()), ex);
            }

            if (this.compileScripts && content.isCachable())
            {
                this.scriptCache.put(content.getPath(), script);

            }
            LOGGER.debug("Compiled script for {}", path);
        }
        else
        {
            LOGGER.debug("Using previously compiled script for {}", path);
        }

        return script;
    }

    protected Script getCompiledScript(final String source, final String path)
    {
        ParameterCheck.mandatoryString("source", source);
        ParameterCheck.mandatoryString("path", path);
        try
        {
            final Script script;
            final String resolvedSource = this.resolveScriptImports(source);
            final Context cx = Context.enter();
            try
            {
                if (this.compileScripts)
                {
                    cx.setOptimizationLevel(9);
                }
                script = cx.compileString(resolvedSource, path, 1, null);
            }
            finally
            {
                Context.exit();
            }
            return script;
        }
        catch (final Exception ex)
        {
            // we don't know the sorts of exceptions that can come from Rhino, so handle any and all exceptions
            LOGGER.error("Failed to compile supplied script", ex);
            throw new WebScriptException(MessageFormat.format("Failed to compile supplied script: {0}", ex.getMessage()), ex);
        }
    }

    @SuppressWarnings("static-method")
    protected String resolveScriptImports(final String script)
    {
        final String classpathResolvedScript = script.replaceAll(CLASSPATH_RESOURCE_IMPORT_PATTERN, CLASSPATH_RESOURCE_IMPORT_REPLACEMENT);
        final String storePathResolvedScript = classpathResolvedScript.replaceAll(STORE_PATH_RESOURCE_IMPORT_PATTERN,
                STORE_PATH_RESOURCE_IMPORT_REPLACEMENT);

        return storePathResolvedScript;
    }

    protected Scriptable setupScope(final Context executionContext, final boolean trustworthyScript, final boolean mutableScope)
    {
        final Scriptable scope;
        if (trustworthyScript)
        {
            // allow access to all libraries and objects
            scope = new ImporterTopLevel(executionContext, !mutableScope);
        }
        else
        {
            scope = executionContext.initStandardObjects(null, !mutableScope);
            // Remove reflection / Java-interactivity capabilities
            scope.delete("Packages");
            scope.delete("getClass");
            scope.delete("java");
        }

        synchronized (this.registeredContributors)
        {
            for (final ScopeContributor contributor : this.registeredContributors)
            {
                contributor.contributeToScope(scope, trustworthyScript, mutableScope);
            }
        }

        return scope;
    }

    protected Object executeScriptImpl(final Script script, final Map<String, Object> model, final boolean trustworthyScript,
            final String debugScriptName) throws AlfrescoRuntimeException
    {
        final long startTime = System.currentTimeMillis();
        LOGGER.info("{} Start", debugScriptName);

        final Context cx = Context.enter();
        try
        {
            cx.setWrapFactory(WRAP_FACTORY);

            final Scriptable scope;
            if (this.shareScopes)
            {
                final Scriptable sharedScope = trustworthyScript ? this.unrestrictedShareableScope : this.restrictedShareableScope;
                scope = cx.newObject(sharedScope);
                scope.setPrototype(sharedScope);
                scope.setParentScope(null);
            }
            else
            {
                scope = this.setupScope(cx, trustworthyScript, false);
            }

            if (model != null)
            {
                // insert supplied object model into root of the default scope
                for (final String key : model.keySet())
                {
                    final Object obj = model.get(key);
                    // convert/wrap each object to JavaScript compatible
                    final Object jsObject = Context.javaToJS(obj, scope);

                    // insert into the root scope ready for access by the script
                    ScriptableObject.putProperty(scope, key, jsObject);
                }
            }

            // execute the script and return the result
            final Object scriptResult = this.executeScriptInScopeImpl(script, scope);

            return scriptResult;
        }
        catch (final WrappedException w)
        {
            LOGGER.info("{} Exception: {}", debugScriptName, w);

            final Throwable err = w.getWrappedException();
            throw new WebScriptException(err.getMessage(), err);
        }
        catch (final Exception ex)
        {
            LOGGER.info("{} Exception: {}", debugScriptName, ex);
            throw new WebScriptException(ex.getMessage(), ex);
        }
        finally
        {
            Context.exit();

            final long endTime = System.currentTimeMillis();
            LOGGER.info("{} End {} ms", debugScriptName, Long.valueOf(endTime - startTime));
        }
    }

    protected Object executeScriptInScopeImpl(final Script script, final Scriptable scope)
    {
        final Context cx = Context.enter();
        try
        {
            if (this.compileScripts)
            {
                cx.setOptimizationLevel(9);
            }
            cx.setWrapFactory(WRAP_FACTORY);
            // make sure scripts always have the relevant processor extensions available
            for (final ProcessorExtension ex : this.processorExtensions.values())
            {
                if (!ScriptableObject.hasProperty(scope, ex.getExtensionName()))
                {
                    // convert/wrap each to JavaScript compatible
                    final Object jsObject = Context.javaToJS(ex, scope);

                    // insert into the scope ready for access by the script
                    ScriptableObject.putProperty(scope, ex.getExtensionName(), jsObject);
                }
            }

            final Object scriptResult = script.exec(cx, scope);
            return scriptResult;
        }
        finally
        {
            Context.exit();
        }
    }

    protected void updateContentChainsBeforeExceution()
    {
        final List<ScriptContentAdapter> activeChain = this.activeScriptContentChain.get();
        if (activeChain != null)
        {
            List<List<ScriptContentAdapter>> recursionChains = this.recursionScriptContentChains.get();
            if (recursionChains == null)
            {
                recursionChains = new LinkedList<List<ScriptContentAdapter>>();
                this.recursionScriptContentChains.set(recursionChains);
            }

            recursionChains.add(0, activeChain);
        }
        this.activeScriptContentChain.set(new LinkedList<ScriptContentAdapter>());
    }

    protected void updateContentChainsAfterReturning()
    {
        this.activeScriptContentChain.remove();
        final List<List<ScriptContentAdapter>> recursionChains = this.recursionScriptContentChains.get();
        if (recursionChains != null)
        {
            final List<ScriptContentAdapter> previousChain = recursionChains.remove(0);
            if (recursionChains.isEmpty())
            {
                this.recursionScriptContentChains.remove();
            }
            this.activeScriptContentChain.set(previousChain);
        }
    }

    /**
     * @param shareScopes
     *            the shareScopes to set
     */
    public final void setShareScopes(final boolean shareScopes)
    {
        this.shareScopes = shareScopes;
    }

    /**
     * @param compileScripts
     *            the compileScripts to set
     */
    public final void setCompileScripts(final boolean compileScripts)
    {
        this.compileScripts = compileScripts;
    }

    /**
     * @param standardScriptLoader
     *            the standardScriptLoader to set
     */
    public final void setStandardScriptLoader(final ScriptLoader standardScriptLoader)
    {
        this.standardScriptLoader = standardScriptLoader;
    }
}
