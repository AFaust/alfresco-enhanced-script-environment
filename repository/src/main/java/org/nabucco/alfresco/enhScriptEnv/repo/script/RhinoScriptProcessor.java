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
/*
 * This file is a partial re-implementation of the Java class org.alfresco.repo.jscript.RhinoScriptProcessor, which itself is
 * 
 * Copyright (C) 2005-2012 Alfresco Software Limited.
 * 
 * and licensed under LGPL 3 (http://www.gnu.org/licenses/).
 * 
 * The original source of this file can be found in the Alfresco public SVN http://svn.alfresco.com/repos/alfresco-open-mirror/alfresco
 * The revision used as the basis for this partial re-implementation can be retrieved via http://svn.alfresco.com/repos/alfresco-open-mirror/alfresco/COMMUNITYTAGS/V4.2c/root/projects/repository/source/java/org/alfresco/repo/jscript/RhinoScriptProcessor.java
 */
package org.nabucco.alfresco.enhScriptEnv.repo.script;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
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
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.WrappedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.FileCopyUtils;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class RhinoScriptProcessor extends BaseProcessor implements EnhancedScriptProcessor, InitializingBean
{
    private static final String NODE_REF_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s*+)*=(\\s*\\n*\\s+)*\"(([^:]+)://([^/]+)/([a-f0-9]+(-[a-f0-9]+)+))\"(\\s*\\n*\\s+)*(/)?>";
    private static final String NODE_REF_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"node\", \"$4\");";

    private static final String LEGACY_NAME_PATH_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s+)*=(\\s*\\n*\\s+)*\"(/[^\"]+)\"(\\s*\\n*\\s+)*(/)?>";
    private static final String LEGACY_NAME_PATH_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"legacyNamePath\", \"$4\");";

    private static final String CLASSPATH_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s+)*=(\\s*\\n*\\s+)*\"classpath:([^\"]+)\"(\\s*\\n*\\s+)*(/)?>";
    private static final String CLASSPATH_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"classpath\", \"$4\");";

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

    private static final Logger LOGGER = LoggerFactory.getLogger(RhinoScriptProcessor.class);
    private static final Logger CALL_LOGGER = LoggerFactory.getLogger(RhinoScriptProcessor.class.getName() + ".calls");

    protected final ThreadLocal<List<ScriptLocation>> activeScriptLocationStack = new ThreadLocal<List<ScriptLocation>>();
    protected final ThreadLocal<List<List<ScriptLocation>>> recursionScriptLocationStacks = new ThreadLocal<List<List<ScriptLocation>>>();

    protected WrapFactory wrapFactory = DEFAULT_WRAP_FACTORY;

    /** Pre initialized secure scope object. */
    protected Scriptable secureScope;

    /** Pre initialized non secure scope object. */
    protected Scriptable nonSecureScope;

    /** Base Value Converter */
    protected final ValueConverter valueConverter = new ValueConverter();

    /** Flag to enable or disable runtime script compilation */
    protected boolean compile = true;

    /** Flag to enable the sharing of sealed root scopes between scripts executions */
    protected boolean shareSealedScopes = true;

    /** Cache of runtime compiled script instances */
    protected final Map<String, Script> scriptCache = new ConcurrentHashMap<String, Script>(256);

    protected final AtomicLong dynamicScriptCounter = new AtomicLong();

    // TODO: size-limited, TTL restricted dynamic script cache based on scripts hash-value

    protected final Map<String, ScriptLocator> scriptLocators = new HashMap<String, ScriptLocator>();

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        // Initialize the secure scope
        Context cx = Context.enter();
        try
        {
            cx.setWrapFactory(this.wrapFactory);
            this.secureScope = this.initScope(cx, false, true);
        }
        finally
        {
            Context.exit();
        }

        // Initialize the non-secure scope
        cx = Context.enter();
        try
        {
            cx.setWrapFactory(this.wrapFactory);
            this.nonSecureScope = this.initScope(cx, true, true);
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
        this.updateLocationStackBeforeExceution();
        try
        {
            this.activeScriptLocationStack.get().add(location);

            String debugScriptName = null;
            if (CALL_LOGGER.isDebugEnabled())
            {
                final String path = location.getPath();
                final int i = path.lastIndexOf('/');
                debugScriptName = i != -1 ? path.substring(i + 1) : path;
            }
            return this.executeScriptImpl(script, model, location.isSecure(), debugScriptName);
        }
        catch (final Throwable err)
        {
            throw new ScriptException("Failed to execute script '" + location.toString() + "': " + err.getMessage(), err);
        }
        finally
        {
            this.updateLocationStackAfterReturning();
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
        this.updateLocationStackBeforeExceution();
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
            this.updateLocationStackAfterReturning();
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
    public void executeInScope(final ScriptLocation location, final Object scope)
    {
        ParameterCheck.mandatory("location", location);

        final Script script = this.getCompiledScript(location);
        this.updateLocationStackBeforeExceution();

        String debugScriptName = null;
        if (CALL_LOGGER.isDebugEnabled())
        {
            final String path = location.getPath();
            final int i = path.lastIndexOf('/');
            debugScriptName = i != -1 ? path.substring(i + 1) : path;
        }

        final long startTime = System.currentTimeMillis();
        CALL_LOGGER.debug("{} Start", debugScriptName);

        final Context cx = Context.enter();
        try
        {
            this.activeScriptLocationStack.get().add(location);

            final Scriptable realScope;
            if (scope == null || !(scope instanceof Scriptable))
            {
                if (this.shareSealedScopes)
                {
                    final Scriptable sharedScope = location.isSecure() ? this.nonSecureScope : this.secureScope;
                    realScope = cx.newObject(sharedScope);
                    realScope.setPrototype(sharedScope);
                    realScope.setParentScope(null);
                }
                else
                {
                    realScope = this.initScope(cx, location.isSecure(), false);
                }
            }
            else
            {
                // TODO: how to handle scope isolation depending on caller?
                realScope = (Scriptable) scope;
            }

            this.executeScriptInScopeImpl(script, realScope);
        }
        catch (final Throwable err)
        {
            // TODO: error handling / bubbling to caller? how to handle Rhino exceptions if caller is not a script?
            CALL_LOGGER.debug("{} Exception: {}", debugScriptName, err);
            throw new ScriptException("Failed to execute script '" + location.toString() + "': " + err.getMessage(), err);
        }
        finally
        {
            this.updateLocationStackAfterReturning();
            Context.exit();

            final long endTime = System.currentTimeMillis();
            CALL_LOGGER.debug("{} End {} msg", debugScriptName, Long.valueOf(endTime - startTime));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScriptLocation getContextScriptLocation()
    {
        // TODO Auto-generated method stub
        return null;
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
     * @param compile
     *            the compile to set
     */
    public final void setCompile(final boolean compile)
    {
        this.compile = compile;
    }

    /**
     * @param shareSealedScopes
     *            the shareSealedScopes to set
     */
    public final void setShareSealedScopes(final boolean shareSealedScopes)
    {
        this.shareSealedScopes = shareSealedScopes;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public final void registerScriptLocator(final String name, final ScriptLocator scriptLocator)
    {
        ParameterCheck.mandatoryString("name", name);
        ParameterCheck.mandatory("scriptLocator", scriptLocator);

        final ScriptLocator replaced = this.scriptLocators.put(name, scriptLocator);
        if (replaced != null)
        {
            LOGGER.warn("ScriptLocator {} overriden by {} with name {}", replaced, scriptLocator, name);
        }
    }

    protected void updateLocationStackBeforeExceution()
    {
        final List<ScriptLocation> activeStack = this.activeScriptLocationStack.get();
        if (activeStack != null)
        {
            List<List<ScriptLocation>> recursionStacks = this.recursionScriptLocationStacks.get();
            if (recursionStacks == null)
            {
                recursionStacks = new LinkedList<List<ScriptLocation>>();
                this.recursionScriptLocationStacks.set(recursionStacks);
            }

            recursionStacks.add(0, activeStack);
        }
        this.activeScriptLocationStack.set(new LinkedList<ScriptLocation>());
    }

    protected void updateLocationStackAfterReturning()
    {
        this.activeScriptLocationStack.remove();
        final List<List<ScriptLocation>> recursionStacks = this.recursionScriptLocationStacks.get();
        if (recursionStacks != null)
        {
            final List<ScriptLocation> previousStack = recursionStacks.remove(0);
            if (recursionStacks.isEmpty())
            {
                this.recursionScriptLocationStacks.remove();
            }
            this.activeScriptLocationStack.set(previousStack);
        }
    }

    protected List<ScriptLocation> getActiveScriptLocationStack()
    {
        final List<ScriptLocation> activeLocations = this.activeScriptLocationStack.get();
        return activeLocations;
    }

    protected Script getCompiledScript(final ScriptLocation location)
    {
        Script script = null;
        final String path = location.getPath();

        // check if the path is in external form containing a protocol identifier
        // TODO: can we generalize external form file:// to a classpath-relative location?
        final String realPath;
        if (!path.matches("^(classpath(\\*)?:)+.*$"))
        {
            // take path as is, either already a file:// path or StoreRef-prefixed node path
            realPath = path;
        }
        else
        {
            // we always want to have a fully-qualified file-protocol path (unless we can generalize all to classpath-relative locations)
            realPath = this.getClass().getClassLoader().getResource(path).toExternalForm();
        }

        // test the cache for a pre-compiled script matching our path
        if (this.compile && location.isCachable())
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

            // We do not worry about more than one user thread compiling the same script.
            // If more than one request thread compiles the same script and adds it to the
            // cache that does not matter - the results will be the same. Therefore we
            // rely on the ConcurrentHashMap impl to deal both with ensuring the safety of the
            // underlying structure with asynchronous get/put operations and for fast
            // multi-threaded access to the common cache.
            if (this.compile && location.isCachable())
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
        catch (final Throwable err)
        {
            LOGGER.error("Failed to compile supplied script", err);
            if (err instanceof Exception)
            {
                throw new ScriptException("Failed to compile supplied script: " + err.getMessage(), err);
            }
            throw new AlfrescoRuntimeException("Technical Error", err);
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
     * <import resource="relative/path/to/includeme.js">
     * </pre>
     * 
     * Either a classpath resource, NodeRef, XPath or relative cm:name path based script can be imported.
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

    /**
     * Execute the supplied script content. Adds the default data model and custom configured root objects into the root scope for access by
     * the script.
     * 
     * @param script
     *            The script to execute.
     * @param model
     *            Data model containing objects to be added to the root scope.
     * @param secure
     *            True if the script is considered secure and may access java.* libs directly
     * @param debugScriptName
     *            To identify the script in debug messages.
     * 
     * @return result of the script execution, can be null.
     * 
     * @throws AlfrescoRuntimeException
     */
    protected Object executeScriptImpl(final Script script, final Map<String, Object> argModel, final boolean secure,
            final String debugScriptName) throws AlfrescoRuntimeException
    {
        final long startTime = System.currentTimeMillis();
        CALL_LOGGER.debug("{} Start", debugScriptName);

        // Convert the model
        final Map<String, Object> model = this.convertToRhinoModel(argModel);

        final Context cx = Context.enter();
        try
        {
            // Create a thread-specific scope from one of the shared scopes.
            // See http://www.mozilla.org/rhino/scopes.html
            cx.setWrapFactory(this.wrapFactory);

            final Scriptable scope;
            if (this.shareSealedScopes)
            {
                final Scriptable sharedScope = secure ? this.nonSecureScope : this.secureScope;
                scope = cx.newObject(sharedScope);
                scope.setPrototype(sharedScope);
                scope.setParentScope(null);
            }
            else
            {
                scope = this.initScope(cx, secure, false);
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
            CALL_LOGGER.debug("{} Exception: {}", debugScriptName, w);

            final Throwable err = w.getWrappedException();
            if (err instanceof Exception)
            {
                // TODO
                throw new ScriptException("", err);
            }
            throw new AlfrescoRuntimeException("Technical Error", err);
        }
        catch (final Throwable err)
        {
            CALL_LOGGER.debug("{} Exception: {}", debugScriptName, err);

            if (err instanceof Exception)
            {
                // TODO
                throw new ScriptException("", err);
            }
            throw new AlfrescoRuntimeException("Technical Error", err);
        }
        finally
        {
            Context.exit();

            final long endTime = System.currentTimeMillis();
            CALL_LOGGER.debug("{} End {} msg", debugScriptName, Long.valueOf(endTime - startTime));
        }
    }

    protected Object executeScriptInScopeImpl(final Script script, final Scriptable scope)
    {
        final Context cx = Context.enter();
        try
        {
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

    /**
     * Initializes a scope for script execution. The easiest way to embed Rhino is just to create a new scope this way whenever you need
     * one. However, initStandardObjects() is an expensive method to call and it allocates a fair amount of memory.
     * 
     * @param cx
     *            the thread execution context
     * @param secure
     *            Do we consider the script secure? When <code>false</code> this ensures the script may not access insecure java.* libraries
     *            or import any other classes for direct access - only the configured root host objects will be available to the script
     *            writer.
     * @param sealed
     *            Should the scope be sealed, making it immutable? This should be <code>true</code> if a scope is to be reused.
     * @return the scope object
     */
    protected Scriptable initScope(final Context cx, final boolean secure, final boolean sealed)
    {
        final Scriptable scope;
        if (secure)
        {
            // Initialize the non-secure scope
            // allow access to all libraries and objects, including the importer
            // @see http://www.mozilla.org/rhino/ScriptingJava.html
            scope = new ImporterTopLevel(cx, sealed);
        }
        else
        {
            // Initialize the secure scope
            scope = cx.initStandardObjects(null, sealed);
            // remove security issue related objects - this ensures the script may not access
            // insecure java.* libraries or import any other classes for direct access - only
            // the configured root host objects will be available to the script writer
            scope.delete("Packages");
            scope.delete("getClass");
            scope.delete("java");
        }

        // TODO: change to a scope contribution strategy pattern

        // add our controlled script import function
        final ImportScriptFunction importFunc = new ImportScriptFunction(this, Collections.unmodifiableMap(this.scriptLocators));

        // TODO: change function arity to 2 - only locator type and locator hint/value should be mandatory
        final IdFunctionObject func = new IdFunctionObject(importFunc, ImportScriptFunction.IMPORT_FUNC_TAG,
                ImportScriptFunction.IMPORT_FUNC_ID, ImportScriptFunction.IMPORT_FUNC_NAME, ImportScriptFunction.ARITY, scope);
        if (sealed)
        {
            func.sealObject();
        }
        func.exportAsScopeProperty();

        return scope;
    }

    /**
     * Converts the passed model into a Rhino model
     * 
     * @param model
     *            the model
     * 
     * @return Map<String, Object> the converted model
     */
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
