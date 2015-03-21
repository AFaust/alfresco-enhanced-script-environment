/*
 * Copyright 2014 PRODYNA AG
 *
 * Licensed under the Eclipse Public License (EPL), Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.api.scripting.URLReader;
import jdk.nashorn.internal.objects.Global;
import jdk.nashorn.internal.runtime.Context;
import jdk.nashorn.internal.runtime.Property;

import org.alfresco.processor.ProcessorExtension;
import org.alfresco.repo.jscript.ClasspathScriptLocation;
import org.alfresco.repo.jscript.RhinoScriptProcessor;
import org.alfresco.repo.processor.BaseProcessor;
import org.alfresco.scripts.ScriptException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.ScriptProcessor;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.MD5;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript.CommonReferencePath;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript.ReferencePathType;
import org.nabucco.alfresco.enhScriptEnv.common.script.ScopeContributor;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.webscripts.processor.SurfReferencePath;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.source.ScriptURLStreamHandler;
import org.nabucco.alfresco.enhScriptEnv.repo.script.NodeScriptLocation;
import org.nabucco.alfresco.enhScriptEnv.repo.script.RepositoryReferencePath;
import org.nabucco.alfresco.enhScriptEnv.repo.script.ScriptLocationAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class NashornScriptProcessor extends BaseProcessor implements InitializingBean, EnhancedScriptProcessor<ScriptLocation>,
        ScriptProcessor
{

    private static final String SIMPLE_URL_PATTERN = "^[^:/*]+:(?://)?.*";

    private static final String NASHORN_ENGINE_NAME = "nashorn";

    private static final Logger LOGGER = LoggerFactory.getLogger(NashornScriptProcessor.class);
    private static final Logger LEGACY_CALL_LOGGER = LoggerFactory.getLogger(RhinoScriptProcessor.class.getName() + ".calls");

    private static final List<ReferencePathType> SCRIPT_URL_SUCCESSION = Collections.<ReferencePathType> unmodifiableList(Arrays
            .<ReferencePathType> asList(CommonReferencePath.FILE, CommonReferencePath.CLASSPATH, RepositoryReferencePath.FILE_FOLDER_PATH,
                    RepositoryReferencePath.CONTENT_PROPERTY, RepositoryReferencePath.NODE_REF, SurfReferencePath.STORE));

    protected ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(NASHORN_ENGINE_NAME);

    protected Global secureGlobal;
    protected Global restrictedGlobal;
    protected Context context;

    // TODO Switchable debugging support (support in Nashorn is not really switchable)
    // (also, there are currently no real "remote" debuggers in IDEs (apart from NetBeans))
    protected volatile boolean debuggerActive = false;

    protected final ThreadLocal<ScriptContext> currentScriptContext = new ThreadLocal<ScriptContext>();
    protected final Map<ScriptContext, List<ReferenceScript>> scriptLocationChainByRootContext = new ConcurrentHashMap<ScriptContext, List<ReferenceScript>>();
    protected final Map<ScriptContext, ScriptContext> scriptContextToRootContext = new ConcurrentHashMap<ScriptContext, ScriptContext>();

    protected final Queue<ScriptContext> reusableScriptContexts = new ConcurrentLinkedQueue<ScriptContext>();

    protected final Collection<ScopeContributor> registeredContributors = new HashSet<ScopeContributor>();

    protected ValueConverter valueConverter;

    protected ScriptURLStreamHandler urlStreamHandler = new ScriptURLStreamHandler();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "scriptEngine", this.scriptEngine);
        PropertyCheck.mandatory(this, "valueConverter", this.valueConverter);
        PropertyCheck.mandatory(this, "urlStreamHandler", this.urlStreamHandler);
        super.register();
    }

    /**
     * @param valueConverter
     *            the valueConverter to set
     */
    public void setValueConverter(final ValueConverter valueConverter)
    {
        this.valueConverter = valueConverter;
    }

    /**
     * @param urlStreamHandler
     *            the urlStreamHandler to set
     */
    public void setUrlStreamHandler(final ScriptURLStreamHandler urlStreamHandler)
    {
        this.urlStreamHandler = urlStreamHandler;
    }

    /**
     * @param scriptEngine
     *            the scriptEngine to set
     */
    public void setScriptEngine(final ScriptEngine scriptEngine)
    {
        this.scriptEngine = scriptEngine;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object execute(final ScriptLocation location, final Map<String, Object> model)
    {
        ParameterCheck.mandatory("location", location);

        final ScriptLocationAdapter script = new ScriptLocationAdapter(location);

        final ScriptContext thisContext = this.getReusableScriptContext();
        final ScriptContext previousContext = this.beforeScriptExecution(thisContext, script, true);
        try
        {
            return this.executeScriptImpl(script, model, location.isSecure());
        }
        catch (final Exception err)
        {
            if (err instanceof ScriptException)
            {
                throw (ScriptException) err;
            }
            throw new ScriptException("Failed to execute supplied script", err);
        }
        finally
        {
            this.afterScriptExecution(thisContext, previousContext);
            this.reusableScriptContexts.add(thisContext);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object execute(final NodeRef nodeRef, final QName contentProp, final Map<String, Object> model)
    {
        final NodeScriptLocation scriptLocation = new NodeScriptLocation(this.services, nodeRef, contentProp);
        return this.execute(scriptLocation, model);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object execute(final String location, final Map<String, Object> model)
    {
        final ClasspathScriptLocation scriptLocation = new ClasspathScriptLocation(location);
        return this.execute(scriptLocation, model);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object executeString(final String source, final Map<String, Object> model)
    {
        ParameterCheck.mandatoryString("source", source);

        final ReferenceScript script = this.toReferenceScript(source);

        final ScriptContext thisContext = this.getReusableScriptContext();
        final ScriptContext previousContext = this.beforeScriptExecution(thisContext, script, true);
        try
        {
            final Object result = this.executeScriptImpl(script, model, false);
            return result;
        }
        finally
        {
            this.afterScriptExecution(thisContext, previousContext);
            this.reusableScriptContexts.add(thisContext);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object executeInScope(final String source, final Object scope)
    {
        ParameterCheck.mandatoryString("source", source);

        final ReferenceScript script = this.toReferenceScript(source);

        final ScriptContext thisContext = scope instanceof ScriptContext ? (ScriptContext) scope : this.getReusableScriptContext();
        final ScriptContext previousContext = this.beforeScriptExecution(thisContext, script, false);
        try
        {
            return this.executeScriptInScopeImpl(script, scope);
        }
        finally
        {
            this.afterScriptExecution(thisContext, previousContext);
            this.reusableScriptContexts.add(thisContext);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object executeInScope(final ScriptLocation location, final Object scope)
    {
        ParameterCheck.mandatory("location", location);

        final ScriptLocationAdapter script = new ScriptLocationAdapter(location);

        final ScriptContext thisContext = scope instanceof ScriptContext ? (ScriptContext) scope : this.getReusableScriptContext();
        final ScriptContext previousContext = this.beforeScriptExecution(thisContext, script, false);
        try
        {
            return this.executeScriptInScopeImpl(script, scope);
        }
        catch (final RuntimeException err)
        {
            if (err instanceof ScriptException)
            {
                throw (ScriptException) err;
            }
            throw new ScriptException("Failed to execute supplied script", err);
        }
        finally
        {
            this.afterScriptExecution(thisContext, previousContext);
            this.reusableScriptContexts.add(thisContext);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object initializeScope(final ScriptLocation location)
    {
        ParameterCheck.mandatory("location", location);
        try
        {
            final Bindings scope = this.setupScope(location.isSecure());
            return scope;
        }
        catch (final Exception err)
        {
            if (err instanceof ScriptException)
            {
                throw (ScriptException) err;
            }
            throw new ScriptException("Failed to initialize scope", err);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceScript getContextScriptLocation()
    {
        final ScriptContext currentScriptContext = this.currentScriptContext.get();
        ParameterCheck.mandatory("currentScriptContext", currentScriptContext);
        final ScriptContext rootContext = this.scriptContextToRootContext.get(currentScriptContext);
        final List<ReferenceScript> currentChain = this.scriptLocationChainByRootContext.get(rootContext);

        final ReferenceScript result;
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
    public List<ReferenceScript> getScriptCallChain()
    {
        final ScriptContext currentScriptContext = this.currentScriptContext.get();
        final List<ReferenceScript> result;

        if (currentScriptContext != null)
        {
            final ScriptContext rootContext = this.scriptContextToRootContext.get(currentScriptContext);
            final List<ReferenceScript> currentChain = this.scriptLocationChainByRootContext.get(rootContext);

            if (currentChain != null)
            {
                result = new ArrayList<ReferenceScript>(currentChain);
            }
            else
            {
                result = Collections.emptyList();
            }
        }
        else
        {
            result = Collections.emptyList();
        }
        return result;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void inheritCallChain(final Object parentContext)
    {
        ParameterCheck.mandatory("parentContext", parentContext);

        final ScriptContext currentContext = this.currentScriptContext.get();
        final ScriptContext currentRootContext = this.scriptContextToRootContext.get(currentContext);

        List<ReferenceScript> activeChain = this.scriptLocationChainByRootContext.get(currentRootContext);
        if (activeChain != null)
        {
            throw new IllegalStateException("Context call chain has already been initialized");
        }

        final ScriptContext parentRootContext = this.scriptContextToRootContext.get(parentContext);
        final List<ReferenceScript> parentChain = this.scriptLocationChainByRootContext.get(parentRootContext);
        if (parentChain == null)
        {
            throw new IllegalArgumentException("Parent context has no call chain associated with it");
        }

        activeChain = new ArrayList<ReferenceScript>(parentChain);
        this.scriptLocationChainByRootContext.put(currentRootContext, activeChain);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void debuggerAttached()
    {
        this.debuggerActive = true;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void debuggerDetached()
    {
        this.debuggerActive = false;
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

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void reset()
    {
        // NO-OP
    }

    protected ScriptContext getReusableScriptContext()
    {
        ScriptContext ctxt = this.reusableScriptContexts.poll();
        if (ctxt == null)
        {
            ctxt = new SimpleScriptContext();
            ctxt.setBindings(new SimpleBindings(), ScriptContext.GLOBAL_SCOPE);
        }
        else
        {
            // reset
            ctxt.setBindings(new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
            ctxt.setBindings(new SimpleBindings(), ScriptContext.GLOBAL_SCOPE);
        }

        return ctxt;
    }

    protected ReferenceScript toReferenceScript(final String source)
    {
        try
        {
            final MD5 md5 = new MD5();
            final String digest = md5.digest(source.getBytes("UTF-8"));
            final String scriptName = MessageFormat.format("string:///DynamicJS-{0}.js", digest);
            final ReferenceScript script = new ReferenceScript.DynamicScript(scriptName, source);
            return script;
        }
        catch (final UnsupportedEncodingException err)
        {
            throw new ScriptException("Failed process supplied script", err);
        }
    }

    protected URL toScriptURL(final ReferenceScript location)
    {
        URL url = null;

        try
        {
            final Collection<ReferencePathType> supportedReferencePathTypes = location.getSupportedReferencePathTypes();
            for (final ReferencePathType pathType : SCRIPT_URL_SUCCESSION)
            {
                if (url == null && supportedReferencePathTypes.contains(pathType))
                {
                    final String referencePath = location.getReferencePath(pathType);

                    if (referencePath != null)
                    {
                        if (referencePath.matches(SIMPLE_URL_PATTERN))
                        {
                            url = new URL(null, referencePath, this.urlStreamHandler);
                        }
                        else
                        {
                            url = new URL(pathType.toString().toLowerCase(Locale.ENGLISH), "", -1, referencePath, this.urlStreamHandler);
                        }
                    }
                }
            }

            if (url == null)
            {
                final String path = location instanceof ScriptLocationAdapter ? ((ScriptLocationAdapter) location).getPath() : location
                        .getFullName();

                // check if the path is in classpath form
                if (path.matches("^(classpath[*]?:).*$"))
                {
                    url = new URL("classpath", "", -1, path.substring(path.indexOf(':')), this.urlStreamHandler);
                }
                else if (path.matches(SIMPLE_URL_PATTERN))
                {
                    url = new URL(null, path, this.urlStreamHandler);
                }
                else
                {
                    LOGGER.info("Script path / name {} is not in URL form - assuming path", path);
                    url = new URL("script", "", -1, path.startsWith("/") ? path.substring(1) : path, this.urlStreamHandler);
                }
            }
        }
        catch (final MalformedURLException urlEx)
        {
            LOGGER.error("Malformed abstract script URL", urlEx);
            throw new ScriptException("Failed to determine abstract script URL", urlEx);
        }

        return url;
    }

    protected ScriptContext beforeScriptExecution(final ScriptContext thisContext, final ReferenceScript script, final boolean newRoot)
    {
        final ScriptContext previousContext = this.currentScriptContext.get();
        this.currentScriptContext.set(thisContext);

        if (thisContext != previousContext && newRoot)
        {
            this.scriptContextToRootContext.put(thisContext, thisContext);
            this.scriptLocationChainByRootContext.compute(thisContext, (k, v) -> new ArrayList<ReferenceScript>()).add(script);
        }
        else
        {
            final ScriptContext rootContext = this.scriptContextToRootContext.computeIfAbsent(thisContext,
                    k -> this.scriptContextToRootContext.getOrDefault(previousContext, thisContext));
            this.scriptLocationChainByRootContext.computeIfAbsent(rootContext, k -> new ArrayList<ReferenceScript>()).add(script);
        }

        return previousContext;
    }

    protected void afterScriptExecution(final ScriptContext thisContext, final ScriptContext previousContext)
    {
        // remove last location chain element
        final ScriptContext rootContext = this.scriptContextToRootContext.get(thisContext);
        final List<ReferenceScript> chain = this.scriptLocationChainByRootContext.get(rootContext);
        chain.remove(chain.size() - 1);

        if (thisContext != previousContext)
        {
            // remove all data directly associated with the current context
            this.scriptLocationChainByRootContext.remove(thisContext);
            this.scriptContextToRootContext.remove(thisContext);
            this.currentScriptContext.set(previousContext);
        }
    }

    @SuppressWarnings("resource")
    protected Object executeScriptImpl(final ReferenceScript script, final Map<String, Object> argModel, final boolean secureScript)
    {
        final URL scriptUrl = this.toScriptURL(script);
        final String effectiveScriptName = scriptUrl.toExternalForm();

        final long startTime = System.currentTimeMillis();
        LOGGER.debug("{} Start", effectiveScriptName);
        LEGACY_CALL_LOGGER.debug("{} Start", effectiveScriptName);

        final ValueConverter previousConverter = ValueConverter.GLOBAL_CONVERTER.get();
        ValueConverter.GLOBAL_CONVERTER.set(this.valueConverter);
        try
        {
            final Bindings scope = this.setupScope(secureScript);
            try
            {
                final ScriptContext ctxt = this.currentScriptContext.get();
                ctxt.setBindings(scope, ScriptContext.ENGINE_SCOPE);
                ctxt.setAttribute(ScriptEngine.FILENAME, effectiveScriptName, ScriptContext.ENGINE_SCOPE);

                // insert supplied object model into root of the default scope
                for (final String key : argModel.keySet())
                {
                    final Object obj = argModel.get(key);

                    // convert/wrap each object to JavaScript compatible
                    final Object jsObject = this.valueConverter.convertValueForScript(obj);
                    ctxt.setAttribute(key, jsObject, ScriptContext.GLOBAL_SCOPE);
                }

                // Note: reader will be auto-closed by eval()
                this.urlStreamHandler.map(scriptUrl, script);
                try
                {
                    final URLReader reader = new URLReader(scriptUrl);
                    final Object scriptResult = this.scriptEngine.eval(reader, ctxt);
                    final Object result = this.valueConverter.convertValueForJava(scriptResult);

                    return result;
                }
                finally
                {
                    this.urlStreamHandler.unmap(scriptUrl);
                }
            }
            finally
            {
                // TODO return scope
            }
        }
        catch (final Exception ex)
        {
            LOGGER.debug("{} Exception: {}", effectiveScriptName, ex);
            LEGACY_CALL_LOGGER.debug("{} Exception: {}", effectiveScriptName, ex);

            throw new ScriptException(ex.getMessage(), ex);
        }
        finally
        {
            ValueConverter.GLOBAL_CONVERTER.set(previousConverter);

            final long endTime = System.currentTimeMillis();
            LOGGER.debug("{} End {} ms", effectiveScriptName, Long.valueOf(endTime - startTime));
            LEGACY_CALL_LOGGER.debug("{} End {} ms", effectiveScriptName, Long.valueOf(endTime - startTime));
        }
    }

    @SuppressWarnings("resource")
    protected Object executeScriptInScopeImpl(final ReferenceScript script, final Object scope)
    {
        final URL scriptUrl = this.toScriptURL(script);
        final String effectiveScriptName = scriptUrl.toExternalForm();

        LOGGER.debug("{} Start", effectiveScriptName);
        LEGACY_CALL_LOGGER.debug("{} Start", effectiveScriptName);

        final long startTime = System.currentTimeMillis();

        final ValueConverter previousConverter = ValueConverter.GLOBAL_CONVERTER.get();
        ValueConverter.GLOBAL_CONVERTER.set(this.valueConverter);
        try
        {
            final ScriptContext ctxt = this.currentScriptContext.get();

            final boolean isolatedScope;
            final Bindings realScope;
            if (!(scope instanceof ScriptContext && scope == ctxt))
            {
                if (scope instanceof Global)
                {
                    realScope = (Bindings) ScriptObjectMirror.wrap(scope, scope);
                    isolatedScope = false;
                }
                else if (scope instanceof ScriptObjectMirror)
                {
                    final Object unwrapped = ScriptObjectMirror.unwrap(scope, Global.instance());
                    if (unwrapped instanceof Global)
                    {
                        realScope = (Bindings) scope;
                        isolatedScope = false;
                    }
                    else
                    {
                        realScope = this.setupScope(false);

                        final Bindings providedScope = (Bindings) scope;

                        for (final String key : providedScope.keySet())
                        {
                            realScope.put(key, providedScope.get(key));
                        }

                        isolatedScope = true;
                    }
                }
                else
                {
                    realScope = this.setupScope(false);
                    isolatedScope = true;
                }

                ctxt.setBindings(realScope, ScriptContext.ENGINE_SCOPE);

                if (isolatedScope)
                {
                    if (scope instanceof Bindings)
                    {
                        ctxt.setBindings((Bindings) scope, ScriptContext.GLOBAL_SCOPE);
                    }
                    else if (scope instanceof Map<?, ?>)
                    {
                        final Map<String, Object> scopeEntries = new HashMap<String, Object>();
                        for (final Entry<?, ?> entry : ((Map<?, ?>) scope).entrySet())
                        {
                            final String key = String.valueOf(entry.getKey());
                            scopeEntries.put(key, this.valueConverter.convertValueForScript(entry.getValue()));

                            if (realScope.containsKey(key))
                            {
                                // override the value in original scope
                                realScope.put(key, this.valueConverter.convertValueForScript(entry.getValue()));
                            }
                        }
                        ctxt.setBindings(new SimpleBindings(scopeEntries), ScriptContext.GLOBAL_SCOPE);
                    }
                }
            }
            else
            {
                realScope = ctxt.getBindings(ScriptContext.ENGINE_SCOPE);
                isolatedScope = false;
            }

            final Object previousScriptName = ctxt.getAttribute(ScriptEngine.FILENAME, ScriptContext.ENGINE_SCOPE);
            ctxt.setAttribute(ScriptEngine.FILENAME, effectiveScriptName, ScriptContext.ENGINE_SCOPE);

            final Object scriptResult;

            // Note: reader will be auto-closed by eval()
            this.urlStreamHandler.map(scriptUrl, script);
            try
            {
                final URLReader reader = new URLReader(scriptUrl);
                scriptResult = this.scriptEngine.eval(reader, ctxt);
            }
            finally
            {
                this.urlStreamHandler.unmap(scriptUrl);

                if (previousScriptName instanceof String)
                {
                    ctxt.setAttribute(ScriptEngine.FILENAME, previousScriptName, ScriptContext.ENGINE_SCOPE);
                }
            }

            // transfer back any (potential) value changes to members of provided scope
            if (isolatedScope && scope instanceof Map<?, ?>)
            {
                for (final Entry<?, ?> entry : ((Map<?, ?>) scope).entrySet())
                {
                    final String key = String.valueOf(entry.getKey());

                    if (realScope.containsKey(key))
                    {
                        @SuppressWarnings("unchecked")
                        final Entry<String, Object> unsafeEntry = (Entry<String, Object>) entry;

                        if (!(scope instanceof ScriptObjectMirror))
                        {
                            // scope is not a script object - so we need to do JS-to-Java conversion
                            final Class<?> targetClass = entry.getValue() == null ? Object.class : entry.getValue().getClass();
                            unsafeEntry.setValue(this.valueConverter.convertValueForJava(realScope.get(key), targetClass));
                        }
                        else
                        {
                            unsafeEntry.setValue(realScope.get(key));
                        }
                    }
                }
            }

            final Object result = this.valueConverter.convertValueForJava(scriptResult);

            return result;
        }
        catch (final Exception err)
        {
            LOGGER.debug("{} Exception: {}", effectiveScriptName, err);
            LEGACY_CALL_LOGGER.debug("{} Exception: {}", effectiveScriptName, err);
            if (err instanceof ScriptException)
            {
                throw (ScriptException) err;
            }
            throw new ScriptException("Failed to execute supplied script", err);
        }
        finally
        {
            ValueConverter.GLOBAL_CONVERTER.set(previousConverter);

            final long endTime = System.currentTimeMillis();
            LOGGER.debug("{} End {} ms", effectiveScriptName, Long.valueOf(endTime - startTime));
            LEGACY_CALL_LOGGER.debug("{} End {} ms", effectiveScriptName, Long.valueOf(endTime - startTime));
        }
    }

    protected Bindings setupScope(final boolean trustworthyScript) throws IOException, javax.script.ScriptException
    {
        // We tried to use shared prototypes to simplify / speed up scope setup, but a lot of side-effects came up and could not be
        // resolved without dealing with a lot of internals
        // TODO Can we perhaps "reuse" globals after initial use & reset the state (via PropertyMap & spill clone)?
        final Bindings scope = this.scriptEngine.createBindings();

        final ScriptContext ctxt = this.getReusableScriptContext();
        try
        {
            ctxt.setBindings(scope, ScriptContext.ENGINE_SCOPE);

            // we previously patched the engine with custom __noSuchProperty__ including logging
            // but JDK 8u40 eliminated "context" prop and moved __noSuchProperty__ to MethodHandle

            final NashornEngineInspector inspector = new NashornEngineInspector();
            ctxt.setAttribute("inspector", inspector, ScriptContext.GLOBAL_SCOPE);
            this.scriptEngine.eval("inspector.inspect();", ctxt);
            ctxt.removeAttribute("inspector", ScriptContext.GLOBAL_SCOPE);

            for (final ScopeContributor contributor : this.registeredContributors)
            {
                contributor.contributeToScope(ctxt, trustworthyScript, false);
            }

            // even secure scripts / contributors won't have access to these (or at least the way they are implemented by Nashorn)
            final Global global = inspector.getGlobal();
            deleteGlobalProperty(global, "exit");
            deleteGlobalProperty(global, "quit");
            deleteGlobalProperty(global, "load");
            deleteGlobalProperty(global, "loadWithNewGlobal");
            deleteGlobalProperty(global, "print");
            deleteGlobalProperty(global, "printf");
            deleteGlobalProperty(global, "sprintf");

            if (!trustworthyScript)
            {
                deleteGlobalProperty(global, "JavaAdapter"); // mozilla_compat
                deleteGlobalProperty(global, "importPackage");// mozilla_compat
                deleteGlobalProperty(global, "importClass");// mozilla_compat

                deleteGlobalProperty(global, "com");
                deleteGlobalProperty(global, "edu");
                deleteGlobalProperty(global, "java");
                deleteGlobalProperty(global, "javafx");
                deleteGlobalProperty(global, "javax");
                deleteGlobalProperty(global, "org");
                deleteGlobalProperty(global, "Java"); // javaApi
                deleteGlobalProperty(global, "JavaImporter"); // javaImporter
                deleteGlobalProperty(global, "JSAdapter"); // jsadapter
                deleteGlobalProperty(global, "Packages"); // packages
            }

            // also deal with direct field access
            global.exit = global.undefined;
            global.quit = global.undefined;
            global.load = global.undefined;
            global.loadWithNewGlobal = global.undefined;
            global.print = global.undefined;

            if (!trustworthyScript)
            {
                global.com = this.restrictedGlobal.undefined;
                global.edu = this.restrictedGlobal.undefined;
                global.java = this.restrictedGlobal.undefined;
                global.javafx = this.restrictedGlobal.undefined;
                global.javax = this.restrictedGlobal.undefined;
                global.org = this.restrictedGlobal.undefined;
                global.javaApi = this.restrictedGlobal.undefined;
                global.javaImporter = this.restrictedGlobal.undefined;
                global.jsadapter = this.restrictedGlobal.undefined;
                global.packages = this.restrictedGlobal.undefined;
            }

            for (final ProcessorExtension ex : this.processorExtensions.values())
            {
                final String extensionName = ex.getExtensionName();
                if (!scope.containsKey(extensionName))
                {
                    // convert/wrap each to JavaScript compatible
                    final Object jsObject = this.valueConverter.convertValueForScript(ex);
                    scope.put(extensionName, jsObject);
                }
            }
        }
        finally
        {
            this.reusableScriptContexts.add(ctxt);
        }

        return scope;
    }

    protected static void deleteGlobalProperty(final Global global, final String property)
    {
        if (Arrays.binarySearch(global.getOwnKeys(true), property) >= 0)
        {
            final Property propertyDesc = global.getProperty(property);
            if (propertyDesc != null)
            {
                global.deleteOwnProperty(propertyDesc);
            }
        }
    }
}
