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
package org.nabucco.alfresco.enhScriptEnv.repo.script;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.model.RenditionModel;
import org.alfresco.processor.ProcessorExtension;
import org.alfresco.repo.jscript.CategoryNode;
import org.alfresco.repo.jscript.ClasspathScriptLocation;
import org.alfresco.repo.jscript.NativeMap;
import org.alfresco.repo.jscript.RhinoScriptProcessor;
import org.alfresco.repo.jscript.Scopeable;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptableHashMap;
import org.alfresco.repo.jscript.ValueConverter;
import org.alfresco.repo.processor.BaseProcessor;
import org.alfresco.repo.thumbnail.script.ScriptThumbnail;
import org.alfresco.scripts.ScriptException;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.ScriptProcessor;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.WrappedException;
import org.nabucco.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import org.nabucco.alfresco.enhScriptEnv.common.script.ImportScriptFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.FileCopyUtils;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class EnhancedRhinoScriptProcessor extends BaseProcessor implements EnhancedScriptProcessor<ScriptLocationAdapter>, ScriptProcessor,
        InitializingBean
{
    private static final String NODE_REF_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s*+)*=(\\s*\\n*\\s+)*\"(([^:]+)://([^/]+)/([a-f0-9]+(-[a-f0-9]+)+))\"(\\s*\\n*\\s+)*(/)?>";
    private static final String NODE_REF_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"node\", \"$4\", true);";

    private static final String LEGACY_NAME_PATH_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s+)*=(\\s*\\n*\\s+)*\"(/[^\"]+)\"(\\s*\\n*\\s+)*(/)?>";
    private static final String LEGACY_NAME_PATH_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"legacyNamePath\", \"$4\", true);";

    private static final String CLASSPATH_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s+)*=(\\s*\\n*\\s+)*\"classpath:(/)?([^\"]+)\"(\\s*\\n*\\s+)*(/)?>";
    private static final String CLASSPATH_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"classpath\", \"/$5\", true);";

    protected static final WrapFactory DEFAULT_WRAP_FACTORY = new WrapFactory()
    {
        /**
         * 
         * {@inheritDoc}
         */
        @Override
        public Scriptable wrapAsJavaObject(final Context cx, final Scriptable scope, final Object javaObject,
                @SuppressWarnings("rawtypes") final Class staticType)
        {
            final Scriptable result;
            if (javaObject instanceof Map<?, ?> && !(javaObject instanceof ScriptableHashMap<?, ?>))
            {
                // no client passing a map to JS should make any assumption about stored object types
                @SuppressWarnings("unchecked")
                final Map<Object, Object> map = (Map<Object, Object>) javaObject;
                result = new NativeMap(scope, map);
            }
            else
            {
                result = super.wrapAsJavaObject(cx, scope, javaObject, staticType);
            }

            return result;
        }
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedRhinoScriptProcessor.class);
    private static final Logger LEGACY_CALL_LOGGER = LoggerFactory.getLogger(RhinoScriptProcessor.class.getName() + ".calls");

    protected final ThreadLocal<List<ScriptLocationAdapter>> activeScriptLocationChain = new ThreadLocal<List<ScriptLocationAdapter>>();
    protected final ThreadLocal<List<List<ScriptLocationAdapter>>> recursionScriptLocationChains = new ThreadLocal<List<List<ScriptLocationAdapter>>>();

    protected WrapFactory wrapFactory = DEFAULT_WRAP_FACTORY;

    protected boolean shareScopes = true;

    protected Scriptable restrictedShareableScope;
    protected Scriptable unrestrictedShareableScope;

    protected boolean compileScripts = true;

    protected final ValueConverter valueConverter = new ValueConverter();

    protected final Map<String, Script> scriptCache = new ConcurrentHashMap<String, Script>(256);

    protected final AtomicLong dynamicScriptCounter = new AtomicLong();

    // TODO: size-limited, TTL restricted dynamic script cache based on scripts hash-value

    protected ImportScriptFunction<ScriptLocationAdapter> importFunction;

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "importFunction", this.importFunction);

        Context cx = Context.enter();
        try
        {
            cx.setWrapFactory(this.wrapFactory);
            this.restrictedShareableScope = this.setupScope(cx, false, true);
        }
        finally
        {
            Context.exit();
        }

        cx = Context.enter();
        try
        {
            cx.setWrapFactory(this.wrapFactory);
            this.unrestrictedShareableScope = this.setupScope(cx, true, true);
        }
        finally
        {
            Context.exit();
        }

        super.register();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(final ScriptLocation location, final Map<String, Object> model)
    {
        ParameterCheck.mandatory("location", location);
        final Script script = this.getCompiledScript(location);
        final String debugScriptName;
        {
            final String path = location.getPath();
            final int i = path.lastIndexOf('/');
            debugScriptName = i != -1 ? path.substring(i + 1) : path;
        }

        this.updateLocationChainsBeforeExceution();
        this.activeScriptLocationChain.get().add(new ScriptLocationAdapter(location));
        try
        {
            return this.executeScriptImpl(script, model, location.isSecure(), debugScriptName);
        }
        finally
        {
            this.updateLocationChainsAfterReturning();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(final NodeRef nodeRef, final QName contentProp, final Map<String, Object> model)
    {
        final NodeScriptLocation scriptLocation = new NodeScriptLocation(this.services, nodeRef, contentProp);
        return this.execute(scriptLocation, model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(final String location, final Map<String, Object> model)
    {
        final ClasspathScriptLocation scriptLocation = new ClasspathScriptLocation(location);
        return this.execute(scriptLocation, model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object executeString(final String source, final Map<String, Object> model)
    {
        ParameterCheck.mandatoryString("source", source);

        // compile the script based on the node content
        final Script script = this.getCompiledScript(source,
                "string://DynamicJS-" + String.valueOf(this.dynamicScriptCounter.getAndIncrement()));

        this.updateLocationChainsBeforeExceution();
        // Note: We are not adding a location object to the chain as initial element - a dynamic JS string has not location
        try
        {
            return this.executeScriptImpl(script, model, true, "string script");
        }
        catch (final Throwable err)
        {
            throw new ScriptException("Failed to execute supplied script: " + err.getMessage(), err);
        }
        finally
        {
            this.updateLocationChainsAfterReturning();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset()
    {
        this.scriptCache.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeInScope(final ScriptLocationAdapter location, final Object scope)
    {
        ParameterCheck.mandatory("location", location);

        final Script script = this.getCompiledScript(location);
        final String debugScriptName;

        {
            final String path = location.getPath();
            final int i = path.lastIndexOf('/');
            debugScriptName = i != -1 ? path.substring(i + 1) : path;
        }

        LOGGER.info("{} Start", debugScriptName);
        LEGACY_CALL_LOGGER.debug("{} Start", debugScriptName);

        // TODO: Can we always be sure we're continuing the previous chain?
        final List<ScriptLocationAdapter> currentChain = this.activeScriptLocationChain.get();
        if (currentChain != null)
        {
            // preliminary safeguard against incorrect invocation - see TODO notice
            currentChain.add(location);
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
                    final Scriptable sharedScope = location.isSecure() ? this.unrestrictedShareableScope : this.restrictedShareableScope;
                    realScope = cx.newObject(sharedScope);
                    realScope.setPrototype(sharedScope);
                    realScope.setParentScope(null);
                }
                else
                {
                    realScope = this.setupScope(cx, location.isSecure(), false);
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
            LEGACY_CALL_LOGGER.debug("{} Exception: {}", debugScriptName, ex);
            throw new ScriptException("Failed to execute script '" + location.toString() + "': " + ex.getMessage(), ex);
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
            LOGGER.info("{} End {} ms", debugScriptName, Long.valueOf(endTime - startTime));
            LEGACY_CALL_LOGGER.debug("{} End {} ms", debugScriptName, Long.valueOf(endTime - startTime));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScriptLocationAdapter getContextScriptLocation()
    {
        final List<ScriptLocationAdapter> currentChain = getActiveScriptLocationChain();
        final ScriptLocationAdapter result;
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
     * @param wrapFactory
     *            the wrapFactory to set
     */
    public final void setWrapFactory(final WrapFactory wrapFactory)
    {
        this.wrapFactory = wrapFactory;
    }

    /**
     * @param shareScopes
     *            the shareScopes to set
     */
    public final void setShareScopes(boolean shareScopes)
    {
        this.shareScopes = shareScopes;
    }

    /**
     * @param compileScripts
     *            the compileScripts to set
     */
    public final void setCompileScripts(boolean compileScripts)
    {
        this.compileScripts = compileScripts;
    }

    /**
     * @param importFunction
     *            the importFunction to set
     */
    public final void setImportFunction(final ImportScriptFunction<ScriptLocationAdapter> importFunction)
    {
        this.importFunction = importFunction;
        this.importFunction.setScriptProcessor(this);
        this.importFunction.setValueConverter(new org.nabucco.alfresco.enhScriptEnv.common.script.ValueConverter()
        {

            /**
             * 
             * {@inheritDoc}
             */
            @Override
            public Object convertValueForJava(Object value)
            {
                return EnhancedRhinoScriptProcessor.this.valueConverter.convertValueForJava(value);
            }

        });
    }

    protected void updateLocationChainsBeforeExceution()
    {
        final List<ScriptLocationAdapter> activeChain = this.activeScriptLocationChain.get();
        if (activeChain != null)
        {
            List<List<ScriptLocationAdapter>> recursionChains = this.recursionScriptLocationChains.get();
            if (recursionChains == null)
            {
                recursionChains = new LinkedList<List<ScriptLocationAdapter>>();
                this.recursionScriptLocationChains.set(recursionChains);
            }

            recursionChains.add(0, activeChain);
        }
        this.activeScriptLocationChain.set(new LinkedList<ScriptLocationAdapter>());
    }

    protected void updateLocationChainsAfterReturning()
    {
        this.activeScriptLocationChain.remove();
        final List<List<ScriptLocationAdapter>> recursionChains = this.recursionScriptLocationChains.get();
        if (recursionChains != null)
        {
            final List<ScriptLocationAdapter> previousChain = recursionChains.remove(0);
            if (recursionChains.isEmpty())
            {
                this.recursionScriptLocationChains.remove();
            }
            this.activeScriptLocationChain.set(previousChain);
        }
    }

    protected List<ScriptLocationAdapter> getActiveScriptLocationChain()
    {
        final List<ScriptLocationAdapter> activeLocations = this.activeScriptLocationChain.get();
        return activeLocations;
    }

    protected Script getCompiledScript(final ScriptLocation location)
    {
        Script script = null;
        final String path = location.getPath();

        // check if the path is in classpath form
        // TODO: can we generalize external form file:// to a classpath-relative location? (best-effort)
        final String realPath;
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

        // test the cache for a pre-compiled script matching our path
        if (this.compileScripts && location.isCachable())
        {
            script = this.scriptCache.get(path);
        }

        if (script == null)
        {
            LOGGER.debug("Resolving and compiling script path: {}", path);

            try
            {
                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                FileCopyUtils.copy(location.getInputStream(), os); // both streams are closed
                final byte[] bytes = os.toByteArray();
                final String source = new String(bytes, "UTF-8");
                script = this.getCompiledScript(source, realPath);
            }
            catch (final IOException err)
            {
                LOGGER.error("Failed to compile supplied script", err);
                throw new ScriptException("Failed to compile supplied script: " + err.getMessage(), err);
            }

            if (this.compileScripts && location.isCachable())
            {
                this.scriptCache.put(path, script);
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
            throw new ScriptException("Failed to compile supplied script: " + ex.getMessage(), ex);
        }
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

    protected Object executeScriptImpl(final Script script, final Map<String, Object> argModel, final boolean secureScript,
            final String debugScriptName) throws AlfrescoRuntimeException
    {
        final long startTime = System.currentTimeMillis();
        LOGGER.info("{} Start", debugScriptName);
        LEGACY_CALL_LOGGER.debug("{} Start", debugScriptName);

        // Convert the model
        final Map<String, Object> model = this.convertToRhinoModel(argModel);

        final Context cx = Context.enter();
        try
        {
            cx.setWrapFactory(this.wrapFactory);

            final Scriptable scope;
            if (this.shareScopes)
            {
                final Scriptable sharedScope = secureScript ? this.unrestrictedShareableScope : this.restrictedShareableScope;
                scope = cx.newObject(sharedScope);
                scope.setPrototype(sharedScope);
                scope.setParentScope(null);
            }
            else
            {
                scope = this.setupScope(cx, secureScript, false);
            }

            // insert supplied object model into root of the default scope
            for (final String key : model.keySet())
            {
                // set the root scope on appropriate objects
                // this is used to allow native JS object creation etc.
                final Object obj = model.get(key);
                if (obj instanceof Scopeable)
                {
                    ((Scopeable) obj).setScope(scope);
                }

                // convert/wrap each object to JavaScript compatible
                final Object jsObject = Context.javaToJS(obj, scope);

                // insert into the root scope ready for access by the script
                ScriptableObject.putProperty(scope, key, jsObject);
            }

            // execute the script and return the result
            final Object scriptResult = this.executeScriptInScopeImpl(script, scope);

            // extract java object result if wrapped by Rhino
            final Object result = this.valueConverter.convertValueForJava(scriptResult);
            return result;
        }
        catch (final WrappedException w)
        {
            LOGGER.info("{} Exception: {}", debugScriptName, w);
            LEGACY_CALL_LOGGER.debug("{} Exception: {}", debugScriptName, w);

            final Throwable err = w.getWrappedException();
            if (err instanceof Exception)
            {
                // TODO
                throw new ScriptException("", err);
            }
            throw new AlfrescoRuntimeException("Technical Error", err);
        }
        catch (final Exception ex)
        {
            LOGGER.info("{} Exception: {}", debugScriptName, ex);
            LEGACY_CALL_LOGGER.debug("{} Exception: {}", debugScriptName, ex);

            // TODO
            throw new ScriptException("", ex);
        }
        finally
        {
            Context.exit();

            final long endTime = System.currentTimeMillis();
            LOGGER.info("{} End {} ms", debugScriptName, Long.valueOf(endTime - startTime));
            LEGACY_CALL_LOGGER.debug("{} End {} ms", debugScriptName, Long.valueOf(endTime - startTime));
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
            // make sure scripts always have the relevant processor extensions available
            for (final ProcessorExtension ex : this.processorExtensions.values())
            {
                if (!ScriptableObject.hasProperty(scope, ex.getExtensionName()))
                {
                    if (ex instanceof Scopeable)
                    {
                        ((Scopeable) ex).setScope(scope);
                    }

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

        // TODO: change to a scope contribution strategy pattern

        // add our controlled script import function
        final IdFunctionObject func = new IdFunctionObject(this.importFunction, ImportScriptFunction.IMPORT_FUNC_TAG,
                ImportScriptFunction.IMPORT_FUNC_ID, ImportScriptFunction.IMPORT_FUNC_NAME, ImportScriptFunction.ARITY, scope);
        if (!mutableScope)
        {
            func.sealObject();
        }
        func.exportAsScopeProperty();

        return scope;
    }

    protected Map<String, Object> convertToRhinoModel(final Map<String, Object> model)
    {
        final Map<String, Object> newModel;
        if (model != null)
        {
            newModel = new HashMap<String, Object>(model.size());
            final DictionaryService dictionaryService = this.services.getDictionaryService();
            final NodeService nodeService = this.services.getNodeService();

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
