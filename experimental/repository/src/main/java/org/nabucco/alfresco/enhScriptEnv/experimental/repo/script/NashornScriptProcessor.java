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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;

import org.alfresco.model.ContentModel;
import org.alfresco.model.RenditionModel;
import org.alfresco.processor.ProcessorExtension;
import org.alfresco.repo.jscript.CategoryNode;
import org.alfresco.repo.jscript.ClasspathScriptLocation;
import org.alfresco.repo.jscript.RhinoScriptProcessor;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.processor.BaseProcessor;
import org.alfresco.repo.thumbnail.script.ScriptThumbnail;
import org.alfresco.scripts.ScriptException;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.ScriptProcessor;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.MD5;
import org.alfresco.util.ParameterCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript.CommonReferencePath;
import org.nabucco.alfresco.enhScriptEnv.common.script.ScopeContributor;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornValueInstanceConverterRegistry.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.repo.script.NodeScriptLocation;
import org.nabucco.alfresco.enhScriptEnv.repo.script.ScriptLocationAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.FileCopyUtils;

public class NashornScriptProcessor extends BaseProcessor implements InitializingBean, EnhancedScriptProcessor<ScriptLocation>,
        ScriptProcessor
{

    protected static final String NASHORN_ENGINE_NAME = "nashorn";

    private static final String NODE_REF_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s*+)*=(\\s*\\n*\\s+)*\"(([^:]+)://([^/]+)/([^\"]+))\"(\\s*\\n*\\s+)*(/)?>";
    private static final String NODE_REF_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"node\", \"$4\", true);";

    private static final String LEGACY_NAME_PATH_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s+)*=(\\s*\\n*\\s+)*\"(/[^\"]+)\"(\\s*\\n*\\s+)*(/)?>";
    private static final String LEGACY_NAME_PATH_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"legacyNamePath\", \"$4\", true);";

    private static final String CLASSPATH_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s+)*=(\\s*\\n*\\s+)*\"classpath:(/)?([^\"]+)\"(\\s*\\n*\\s+)*(/)?>";
    private static final String CLASSPATH_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"classpath\", \"/$5\", true);";

    private static final int DEFAULT_MAX_SCRIPT_CACHE_SIZE = 200;

    private static final Logger LOGGER = LoggerFactory.getLogger(NashornScriptProcessor.class);
    private static final Logger LEGACY_CALL_LOGGER = LoggerFactory.getLogger(RhinoScriptProcessor.class.getName() + ".calls");

    protected final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(NASHORN_ENGINE_NAME);

    // TODO: Is there (switchable) debugging support in Nashorn?
    protected volatile boolean debuggerActive = false;

    protected final ThreadLocal<ScriptContext> currentScriptContext = new ThreadLocal<ScriptContext>();
    protected final Map<ScriptContext, List<ReferenceScript>> scriptLocationChainByRootContext = new ConcurrentHashMap<ScriptContext, List<ReferenceScript>>();
    protected final Map<ScriptContext, ScriptContext> scriptContextToRootContext = new ConcurrentHashMap<ScriptContext, ScriptContext>();

    protected final Collection<ScopeContributor> registeredContributors = new HashSet<ScopeContributor>();

    protected ValueConverter valueConverter = new NashornValueConverter();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object execute(final ScriptLocation location, final Map<String, Object> model)
    {
        ParameterCheck.mandatory("location", location);

        final ReferenceScript script = new ScriptLocationAdapter(location);

        final ScriptContext thisContext = new SimpleScriptContext();
        final ScriptContext previousContext = this.beforeScriptExecution(thisContext, script, true);
        try
        {
            final String source = this.toSource(location);

            return this.executeScriptImpl(source, model, location.isSecure(), script);
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

        try
        {
            final ReferenceScript script = this.toReferenceScript(source);

            final ScriptContext thisContext = new SimpleScriptContext();
            final ScriptContext previousContext = this.beforeScriptExecution(thisContext, script, true);
            try
            {
                final Object result = this.executeScriptImpl(source, model, false, script);
                return result;
            }
            finally
            {
                this.afterScriptExecution(thisContext, previousContext);
            }
        }
        catch (final Throwable err)
        {
            if (err instanceof ScriptException)
            {
                throw (ScriptException) err;
            }
            throw new ScriptException("Failed to execute supplied script", err);
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

        try
        {
            final ReferenceScript script = this.toReferenceScript(source);

            final ScriptContext thisContext = new SimpleScriptContext();
            final ScriptContext previousContext = this.beforeScriptExecution(thisContext, script, false);
            try
            {
                final Object result = this.executeScriptInScopeImpl(source, scope, script);
                return result;
            }
            finally
            {
                this.afterScriptExecution(thisContext, previousContext);
            }
        }
        catch (final Throwable err)
        {
            if (err instanceof ScriptException)
            {
                throw (ScriptException) err;
            }
            throw new ScriptException("Failed to execute supplied script", err);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object executeInScope(final ScriptLocation location, final Object scope)
    {
        ParameterCheck.mandatory("location", location);

        final ReferenceScript script = new ScriptLocationAdapter(location);

        final ScriptContext thisContext = new SimpleScriptContext();
        final ScriptContext previousContext = this.beforeScriptExecution(thisContext, script, false);
        try
        {
            final String source = this.toSource(location);

            final Object result = this.executeScriptInScopeImpl(source, scope, script);
            return result;
        }
        catch (final Throwable err)
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
            final Bindings scope = this.setupScope(location.isSecure(), true);
            return scope;
        }
        catch (final Throwable err)
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
        ParameterCheck.mandatory("currentScriptContext", currentScriptContext);
        final ScriptContext rootContext = this.scriptContextToRootContext.get(currentScriptContext);
        final List<ReferenceScript> currentChain = this.scriptLocationChainByRootContext.get(rootContext);

        final List<ReferenceScript> result;
        if (currentChain != null)
        {
            result = new ArrayList<ReferenceScript>(currentChain);
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
    public void inheritCallChain(final Object parentContext)
    {
        ParameterCheck.mandatory("parentContext", parentContext);

        // TODO
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

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        super.register();
    }

    /**
     * @param valueConverter
     *            the valueConverter to set
     */
    public final void setValueConverter(final ValueConverter valueConverter)
    {
        this.valueConverter = valueConverter;
    }

    protected ReferenceScript toReferenceScript(final String source) throws UnsupportedEncodingException
    {
        final MD5 md5 = new MD5();
        final String digest = md5.digest(source.getBytes("UTF-8"));
        final String scriptName = "string://DynamicJS-" + digest;
        final ReferenceScript script = new ReferenceScript.DynamicScript(scriptName);
        return script;
    }

    protected String toEffectiveScriptName(final ReferenceScript script)
    {
        final String fullName = script.getFullName();
        String effectiveScriptName = script.getReferencePath(CommonReferencePath.FILE);

        if (effectiveScriptName == null)
        {
            // check if the path is in classpath form
            // TODO: can we generalize external form file:// to a classpath-relative location? (best-effort)
            if (!fullName.matches("^(classpath[*]?:).*$"))
            {
                // take path as is - can be anything depending on how content is loaded
                effectiveScriptName = fullName;
            }
            else
            {
                // we always want to have a fully-qualified file-protocol path (unless we can generalize all to classpath-relative
                // locations)
                final String resourcePath = fullName.substring(fullName.indexOf(':') + 1);
                URL resource = this.getClass().getClassLoader().getResource(resourcePath);
                if (resource == null && resourcePath.startsWith("/"))
                {
                    resource = this.getClass().getClassLoader().getResource(resourcePath.substring(1));
                }

                if (resource != null)
                {
                    effectiveScriptName = resource.toExternalForm();
                }
                else
                {
                    // should not occur in normal circumstances, but since ScriptLocation can be anything...
                    effectiveScriptName = fullName;
                }
            }
        }
        return effectiveScriptName;
    }

    // compiler can't know FileCopyUtils.copy closes both streams
    protected String toSource(final ScriptLocation location) throws IOException, UnsupportedEncodingException
    {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        FileCopyUtils.copy(location.getInputStream(), os); // both streams are closed
        final byte[] bytes = os.toByteArray();

        // TODO: is this safe - can we "assume" UTF-8?
        final String source = new String(bytes, "UTF-8");

        return source;
    }

    protected ScriptContext beforeScriptExecution(final ScriptContext thisContext, final ReferenceScript script, final boolean newRoot)
    {
        final ScriptContext previousContext = this.currentScriptContext.get();
        this.currentScriptContext.set(thisContext);

        if (newRoot)
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

        // remove all data directly associated with the current context
        this.scriptLocationChainByRootContext.remove(thisContext);
        this.scriptContextToRootContext.remove(thisContext);
        this.currentScriptContext.set(previousContext);
    }

    /**
     * Resolves the import directives in the specified script to proper import API calls. Supported import directives are of the following
     * form:
     *
     * <pre>
     * <import resource="classpath:alfresco/includeme.js">
     * <import resource="workspace://SpacesStore/6f73de1b-d3b4-11db-80cb-112e6c2ea048">
     * <import resource="/Company Home/Data Dictionary/Scripts/includeme.js">
     * </pre>
     *
     * Either a classpath resource, NodeRef or cm:name path based script can be imported.
     *
     * @param script
     *            The script content to resolve imports in
     *
     * @return a valid script with all includes resolved to a proper import API call
     */
    @SuppressWarnings("static-method")
    protected String resolveScriptImports(final String script)
    {
        final String classpathResolvedScript = script.replaceAll(CLASSPATH_RESOURCE_IMPORT_PATTERN, CLASSPATH_RESOURCE_IMPORT_REPLACEMENT);
        final String nodeRefResolvedScript = classpathResolvedScript.replaceAll(NODE_REF_RESOURCE_IMPORT_PATTERN,
                NODE_REF_RESOURCE_IMPORT_REPLACEMENT);
        final String legacyNamePathResolvedScript = nodeRefResolvedScript.replaceAll(LEGACY_NAME_PATH_RESOURCE_IMPORT_PATTERN,
                LEGACY_NAME_PATH_RESOURCE_IMPORT_REPLACEMENT);

        return legacyNamePathResolvedScript;
    }

    protected Object executeScriptImpl(final String source, final Map<String, Object> argModel, final boolean secureScript,
            final ReferenceScript script)
    {
        final String effectiveScriptName = this.toEffectiveScriptName(script);

        final long startTime = System.currentTimeMillis();
        LOGGER.debug("{} Start", effectiveScriptName);
        LEGACY_CALL_LOGGER.debug("{} Start", effectiveScriptName);

        // Convert the model
        final Map<String, Object> model = this.convertToNashornModel(argModel);

        try
        {
            final Bindings scope = this.setupScope(secureScript, true);

            // insert supplied object model into root of the default scope
            for (final String key : model.keySet())
            {
                final Object obj = model.get(key);

                scope.put(key, this.convertToNashornModel(obj));
            }

            // execute the script and return the result
            final Object scriptResult = this.executeScriptInScopeImpl(source, effectiveScriptName, scope);

            return scriptResult;
        }
        catch (final Exception ex)
        {
            LOGGER.debug("{} Exception: {}", effectiveScriptName, ex);
            LEGACY_CALL_LOGGER.debug("{} Exception: {}", effectiveScriptName, ex);

            throw new ScriptException(ex.getMessage(), ex);
        }
        finally
        {
            final long endTime = System.currentTimeMillis();
            LOGGER.debug("{} End {} ms", effectiveScriptName, Long.valueOf(endTime - startTime));
            LEGACY_CALL_LOGGER.debug("{} End {} ms", effectiveScriptName, Long.valueOf(endTime - startTime));
        }
    }

    protected Object executeScriptInScopeImpl(final String source, final Object scope, final ReferenceScript script)
    {
        final String fullName = script.getFullName();
        String effectiveScriptName = script.getReferencePath(CommonReferencePath.FILE);

        if (effectiveScriptName == null)
        {
            // check if the path is in classpath form
            // TODO: can we generalize external form file:// to a classpath-relative location? (best-effort)
            if (!fullName.matches("^(classpath[*]?:).*$"))
            {
                // take path as is - can be anything depending on how content is loaded
                effectiveScriptName = fullName;
            }
            else
            {
                // we always want to have a fully-qualified file-protocol path (unless we can generalize all to classpath-relative
                // locations)
                final String resourcePath = fullName.substring(fullName.indexOf(':') + 1);
                URL resource = this.getClass().getClassLoader().getResource(resourcePath);
                if (resource == null && resourcePath.startsWith("/"))
                {
                    resource = this.getClass().getClassLoader().getResource(resourcePath.substring(1));
                }

                if (resource != null)
                {
                    effectiveScriptName = resource.toExternalForm();
                }
                else
                {
                    // should not occur in normal circumstances, but since ScriptLocation can be anything...
                    effectiveScriptName = fullName;
                }
            }
        }

        LOGGER.debug("{} Start", effectiveScriptName);
        LEGACY_CALL_LOGGER.debug("{} Start", effectiveScriptName);

        final long startTime = System.currentTimeMillis();

        try
        {
            final Bindings realScope;

            if (scope == null)
            {
                realScope = this.setupScope(false, false);
            }
            else if (!(scope instanceof Bindings))
            {
                // TODO: check if scope is a script object / map and try to use as prototype / delegate
                realScope = this.setupScope(false, true);
            }
            else
            {
                realScope = (Bindings) scope;
            }

            final Object result = this.executeScriptInScopeImpl(source, effectiveScriptName, realScope);
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
            final long endTime = System.currentTimeMillis();
            LOGGER.debug("{} End {} ms", effectiveScriptName, Long.valueOf(endTime - startTime));
            LEGACY_CALL_LOGGER.debug("{} End {} ms", effectiveScriptName, Long.valueOf(endTime - startTime));
        }
    }

    protected Object executeScriptInScopeImpl(final String source, final String effectiveScriptName, final Bindings scope)
            throws javax.script.ScriptException
    {
        // make sure scripts always have the relevant processor extensions available
        for (final ProcessorExtension ex : this.processorExtensions.values())
        {
            final String extensionName = ex.getExtensionName();
            if (!scope.containsKey(extensionName))
            {
                scope.put(extensionName, this.convertToNashornModel(ex));
            }
        }

        final ScriptContext scriptContext = this.currentScriptContext.get();
        scriptContext.setBindings(scope, ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute(ScriptEngine.FILENAME, effectiveScriptName, ScriptContext.ENGINE_SCOPE);

        final Object scriptResult = this.scriptEngine.eval(source, scriptContext);
        return scriptResult;

    }

    @SuppressWarnings("resource")
    protected Bindings setupScope(final boolean trustworthyScript, final boolean mutableScope) throws javax.script.ScriptException,
            IOException
    {
        final Bindings scope = this.scriptEngine.createBindings();

        final ScriptContext context = new SimpleScriptContext();
        context.setBindings(scope, ScriptContext.ENGINE_SCOPE);

        // re-initialize some of the global functions with scope-access to those globals we need to remove
        final InputStream engineIs = this.getClass().getResource("resources/alt-engine.js").openStream();
        final Reader engineReader = new InputStreamReader(engineIs);
        // will inheritently close engineReader
        this.scriptEngine.eval(engineReader, context);

        final InputStream mozIs = this.getClass().getResource("resources/alt-mozilla-compat.js").openStream();
        final Reader mozReader = new InputStreamReader(mozIs);
        // will inheritently close mozReader
        this.scriptEngine.eval(mozReader, context);

        // even secure scripts won't have access to these
        scope.remove("load");
        scope.remove("loadWithNewGlobal");
        scope.remove("exit");
        scope.remove("quit");

        if (!trustworthyScript)
        {
            // remove access to Java
            scope.remove("Java");
            scope.remove("java");
            scope.remove("Packages");
            scope.remove("JavaAdapter");
            scope.remove("importPackage");
            scope.remove("importClass");
        }

        // TODO: create proxies for Scopeable objects / procesor extensions
        // synchronized (this.registeredContributors)
        // {
        // for (final ScopeContributor contributor : this.registeredContributors)
        // {
        // contributor.contributeToScope(scope, trustworthyScript, mutableScope);
        // }
        // }

        return scope;
    }

    protected Object convertToNashornModel(final Object value)
    {
        final Object result;
        if (this.valueConverter != null)
        {
            result = this.valueConverter.convertToNashorn(value);
        }
        else
        {
            result = value;
        }
        return result;
    }

    protected Map<String, Object> convertToNashornModel(final Map<String, Object> model)
    {
        final Map<String, Object> newModel;
        if (model != null)
        {
            newModel = new HashMap<String, Object>(model.size());
            final DictionaryService dictionaryService = this.services.getDictionaryService();
            final NodeService nodeService = this.services.getNodeService();

            // TODO: create proxies for Scopeable objects / procesor extensions
            for (final Map.Entry<String, Object> entry : model.entrySet())
            {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                if (value instanceof NodeRef)
                {
                    final QName type = nodeService.getType((NodeRef) value);
                    if (dictionaryService.isSubClass(type, ContentModel.TYPE_CATEGORY))
                    {
                        newModel.put(key, new CategoryNode((NodeRef) value, this.services));
                    }
                    else if (dictionaryService.isSubClass(type, ContentModel.TYPE_THUMBNAIL)
                            || nodeService.hasAspect((NodeRef) value, RenditionModel.ASPECT_RENDITION))
                    {
                        newModel.put(key, new ScriptThumbnail((NodeRef) value, this.services, null));
                    }
                    else
                    {
                        newModel.put(key, new ScriptNode((NodeRef) value, this.services));
                    }
                }
                else
                {
                    newModel.put(key, value);
                }
            }
        }
        else
        {
            newModel = new HashMap<String, Object>(1, 1.0f);
        }

        return newModel;
    }
}
