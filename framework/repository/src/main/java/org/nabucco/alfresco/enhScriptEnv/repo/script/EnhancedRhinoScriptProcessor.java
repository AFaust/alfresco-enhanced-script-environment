/*
 * Copyright 2015 PRODYNA AG
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
package org.nabucco.alfresco.enhScriptEnv.repo.script;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.processor.ProcessorExtension;
import org.alfresco.repo.jscript.ClasspathScriptLocation;
import org.alfresco.repo.jscript.RhinoScriptProcessor;
import org.alfresco.repo.jscript.Scopeable;
import org.alfresco.repo.processor.BaseProcessor;
import org.alfresco.scripts.ScriptException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.ScriptProcessor;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.MD5;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;
import org.nabucco.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript.CommonReferencePath;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript.DynamicScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript.ReferencePathType;
import org.nabucco.alfresco.enhScriptEnv.common.script.ScopeContributor;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.rhino.DelegatingWrapFactory;
import org.nabucco.alfresco.enhScriptEnv.common.webscripts.processor.SurfReferencePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.extensions.surf.util.I18NUtil;
import org.springframework.util.FileCopyUtils;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class EnhancedRhinoScriptProcessor extends BaseProcessor implements EnhancedScriptProcessor<ScriptLocation>, ScriptProcessor,
        InitializingBean, ApplicationListener<ContextRefreshedEvent>
{
    private static final String NODE_REF_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s*+)*=(\\s*\\n*\\s+)*\"(([^:]+)://([^/]+)/([^\"]+))\"(\\s*\\n*\\s+)*(/)?>";
    private static final String NODE_REF_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"node\", \"$4\", true);";

    private static final String LEGACY_NAME_PATH_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s+)*=(\\s*\\n*\\s+)*\"(/[^\"]+)\"(\\s*\\n*\\s+)*(/)?>";
    private static final String LEGACY_NAME_PATH_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"legacyNamePath\", \"$4\", true);";

    private static final String CLASSPATH_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s+)*=(\\s*\\n*\\s+)*\"classpath:(/)?([^\"]+)\"(\\s*\\n*\\s+)*(/)?>";
    private static final String CLASSPATH_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"classpath\", \"/$5\", true);";

    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedRhinoScriptProcessor.class);
    private static final Logger LEGACY_CALL_LOGGER = LoggerFactory.getLogger(RhinoScriptProcessor.class.getName() + ".calls");

    private static final List<ReferencePathType> REAL_PATH_SUCCESSION = Collections.<ReferencePathType> unmodifiableList(Arrays
            .<ReferencePathType> asList(CommonReferencePath.FILE, RepositoryReferencePath.FILE_FOLDER_PATH, SurfReferencePath.STORE));

    private static final int DEFAULT_MAX_SCRIPT_CACHE_SIZE = 200;

    // used WeakHashMap here before to avoid accidental leaks but measures for proper cleanup have proven themselves
    // during tests
    protected final Map<Context, List<ReferenceScript>> activeScriptLocationChain = new ConcurrentHashMap<Context, List<ReferenceScript>>();
    protected final Map<Context, List<List<ReferenceScript>>> recursionScriptLocationChains = new ConcurrentHashMap<Context, List<List<ReferenceScript>>>();

    protected boolean shareScopes = true;

    protected Scriptable restrictedShareableScope;
    protected Scriptable unrestrictedShareableScope;

    protected boolean compileScripts = true;
    protected volatile boolean debuggerActive = false;
    protected boolean failoverToLessOptimization = true;
    protected int optimizationLevel = -1;

    protected ValueConverter valueConverter;

    protected final Map<String, Script> scriptCache = new LinkedHashMap<String, Script>(256);
    protected final ReadWriteLock scriptCacheLock = new ReentrantReadWriteLock(true);

    protected final AtomicLong dynamicScriptCounter = new AtomicLong();

    protected final Map<String, Script> dynamicScriptCache = new LinkedHashMap<String, Script>();
    protected final ReadWriteLock dynamicScriptCacheLock = new ReentrantReadWriteLock(true);

    protected int maxScriptCacheSize = DEFAULT_MAX_SCRIPT_CACHE_SIZE;

    protected final Collection<ScopeContributor> registeredContributors = new HashSet<ScopeContributor>();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "valueConverter", this.valueConverter);
        super.register();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event)
    {
        final ValueConverter previousConverter = ValueConverter.GLOBAL_CONVERTER.get();
        ValueConverter.GLOBAL_CONVERTER.set(this.valueConverter);
        try
        {
            Context cx = Context.enter();
            try
            {
                cx.setWrapFactory(new DelegatingWrapFactory());
                this.restrictedShareableScope = this.setupScope(cx, false, true);
            }
            finally
            {
                Context.exit();
            }

            cx = Context.enter();
            try
            {
                cx.setWrapFactory(new DelegatingWrapFactory());
                this.unrestrictedShareableScope = this.setupScope(cx, true, true);
            }
            finally
            {
                Context.exit();
            }
        }
        finally
        {
            ValueConverter.GLOBAL_CONVERTER.set(previousConverter);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(final ScriptLocation location, final Map<String, Object> model)
    {
        ParameterCheck.mandatory("location", location);

        final ReferenceScript actualScript = new ScriptLocationAdapter(location);
        final Script script = this.getCompiledScript(actualScript);
        final String debugScriptName;
        {
            final String path = location.getPath();
            final int i = path.lastIndexOf('/');
            debugScriptName = i != -1 ? path.substring(i + 1) : path;
        }

        final Context cx = Context.enter();
        try
        {
            this.updateLocationChainsBeforeExceution(cx);
            this.activeScriptLocationChain.get(cx).add(actualScript);
            try
            {
                return this.executeScriptImpl(script, model, location.isSecure(), debugScriptName);
            }
            finally
            {
                this.updateLocationChainsAfterReturning(cx);
            }
        }
        finally
        {
            Context.exit();
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

        final ReferenceScript referenceScript = this.toReferenceScript(source);
        final String debugScriptName = referenceScript.getFullName();
        final Script script = this.getCompiledScript(referenceScript);

        final Context cx = Context.enter();
        try
        {
            this.updateLocationChainsBeforeExceution(cx);
            this.activeScriptLocationChain.get(cx).add(new ReferenceScript.DynamicScript(debugScriptName, source));
            try
            {
                return this.executeScriptImpl(script, model, false, debugScriptName);
            }
            finally
            {
                this.updateLocationChainsAfterReturning(cx);
            }
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
    public Object executeInScope(final String source, final Object scope)
    {
        ParameterCheck.mandatoryString("source", source);

        final ReferenceScript referenceScript = this.toReferenceScript(source);
        final String debugScriptName = referenceScript.getFullName();
        final Script script = this.getCompiledScript(referenceScript);

        LOGGER.info("{} Start", debugScriptName);
        LEGACY_CALL_LOGGER.debug("{} Start", debugScriptName);

        final long startTime = System.currentTimeMillis();

        final ValueConverter previousConverter = ValueConverter.GLOBAL_CONVERTER.get();
        ValueConverter.GLOBAL_CONVERTER.set(this.valueConverter);

        final Context cx = Context.enter();
        try
        {
            final DelegatingWrapFactory wrapFactory = new DelegatingWrapFactory();

            cx.setWrapFactory(wrapFactory);

            List<ReferenceScript> currentChain = this.activeScriptLocationChain.get(cx);
            boolean newChain = false;
            if (currentChain == null)
            {
                this.updateLocationChainsBeforeExceution(cx);
                currentChain = this.activeScriptLocationChain.get(cx);
                newChain = true;
            }
            // else: assume the original script chain is continued
            currentChain.add(new ReferenceScript.DynamicScript(debugScriptName, source));

            try
            {
                final Scriptable realScope;
                if (scope == null)
                {
                    if (this.shareScopes)
                    {
                        final Scriptable sharedScope = this.restrictedShareableScope;
                        realScope = cx.newObject(sharedScope);
                        realScope.setPrototype(sharedScope);
                        realScope.setParentScope(null);
                    }
                    else
                    {
                        realScope = this.setupScope(cx, false, false);
                    }
                }
                else if (!(scope instanceof Scriptable))
                {
                    realScope = new NativeJavaObject(null, scope, scope.getClass());
                    if (this.shareScopes)
                    {
                        final Scriptable sharedScope = this.restrictedShareableScope;
                        realScope.setPrototype(sharedScope);
                    }
                    else
                    {
                        final Scriptable baseScope = this.setupScope(cx, false, false);
                        realScope.setPrototype(baseScope);
                    }
                }
                else
                {
                    realScope = (Scriptable) scope;
                }

                wrapFactory.setScope(realScope);

                final Object result = this.executeScriptInScopeImpl(script, realScope);
                return result;
            }
            finally
            {
                currentChain.remove(currentChain.size() - 1);
                if (newChain)
                {
                    this.updateLocationChainsAfterReturning(cx);
                }
            }
        }
        catch (final Exception ex)
        {
            // TODO: error handling / bubbling to caller? how to handle Rhino exceptions if caller is not a script?
            LOGGER.info("{} Exception: {}", debugScriptName, ex);
            LEGACY_CALL_LOGGER.debug("{} Exception: {}", debugScriptName, ex);
            throw new ScriptException("Failed to execute script string: " + ex.getMessage(), ex);
        }
        finally
        {
            Context.exit();

            ValueConverter.GLOBAL_CONVERTER.set(previousConverter);

            final long endTime = System.currentTimeMillis();
            LOGGER.info("{} End {} ms", debugScriptName, Long.valueOf(endTime - startTime));
            LEGACY_CALL_LOGGER.debug("{} End {} ms", debugScriptName, Long.valueOf(endTime - startTime));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object executeInScope(final ScriptLocation location, final Object scope)
    {
        ParameterCheck.mandatory("location", location);

        final ReferenceScript actualScript = new ScriptLocationAdapter(location);
        final Script script = this.getCompiledScript(actualScript);
        final String debugScriptName;
        {
            final String path = location.getPath();
            final int i = path.lastIndexOf('/');
            debugScriptName = i != -1 ? path.substring(i + 1) : path;
        }

        LOGGER.info("{} Start", debugScriptName);
        LEGACY_CALL_LOGGER.debug("{} Start", debugScriptName);

        final long startTime = System.currentTimeMillis();

        final ValueConverter previousConverter = ValueConverter.GLOBAL_CONVERTER.get();
        ValueConverter.GLOBAL_CONVERTER.set(this.valueConverter);

        final Context cx = Context.enter();
        try
        {
            final DelegatingWrapFactory wrapFactory = new DelegatingWrapFactory();
            cx.setWrapFactory(wrapFactory);

            List<ReferenceScript> currentChain = this.activeScriptLocationChain.get(cx);
            boolean newChain = false;
            if (currentChain == null)
            {
                this.updateLocationChainsBeforeExceution(cx);
                currentChain = this.activeScriptLocationChain.get(cx);
                newChain = true;
            }
            // else: assume the original script chain is continued
            currentChain.add(actualScript);
            try
            {

                final Scriptable realScope;
                if (scope == null)
                {
                    if (this.shareScopes)
                    {
                        final Scriptable sharedScope = actualScript.isSecure() ? this.unrestrictedShareableScope
                                : this.restrictedShareableScope;
                        realScope = cx.newObject(sharedScope);
                        realScope.setPrototype(sharedScope);
                        realScope.setParentScope(null);
                    }
                    else
                    {
                        realScope = this.setupScope(cx, actualScript.isSecure(), false);
                    }
                }
                else if (!(scope instanceof Scriptable))
                {
                    realScope = new NativeJavaObject(null, scope, scope.getClass());
                    if (this.shareScopes)
                    {
                        final Scriptable sharedScope = actualScript.isSecure() ? this.unrestrictedShareableScope
                                : this.restrictedShareableScope;
                        realScope.setPrototype(sharedScope);
                    }
                    else
                    {
                        final Scriptable baseScope = this.setupScope(cx, actualScript.isSecure(), false);
                        realScope.setPrototype(baseScope);
                    }
                }
                else
                {
                    realScope = (Scriptable) scope;
                }

                wrapFactory.setScope(realScope);

                final Object result = this.executeScriptInScopeImpl(script, realScope);
                return result;
            }
            finally
            {
                currentChain.remove(currentChain.size() - 1);
                if (newChain)
                {
                    this.updateLocationChainsAfterReturning(cx);
                }
            }
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

            ValueConverter.GLOBAL_CONVERTER.set(previousConverter);

            final long endTime = System.currentTimeMillis();
            LOGGER.info("{} End {} ms", debugScriptName, Long.valueOf(endTime - startTime));
            LEGACY_CALL_LOGGER.debug("{} End {} ms", debugScriptName, Long.valueOf(endTime - startTime));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object initializeScope(final ScriptLocation location)
    {
        ParameterCheck.mandatory("location", location);

        final Scriptable scope;

        final ValueConverter previousConverter = ValueConverter.GLOBAL_CONVERTER.get();
        ValueConverter.GLOBAL_CONVERTER.set(this.valueConverter);

        final Context cx = Context.enter();
        try
        {
            cx.setWrapFactory(new DelegatingWrapFactory());

            final boolean secureScript = location.isSecure();
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
        }
        finally
        {
            Context.exit();

            ValueConverter.GLOBAL_CONVERTER.set(previousConverter);
        }

        return scope;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceScript getContextScriptLocation()
    {
        final List<ReferenceScript> currentChain = this.activeScriptLocationChain.get(Context.getCurrentContext());
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
        final List<ReferenceScript> currentChain = this.activeScriptLocationChain.get(Context.getCurrentContext());
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

        final Context currentContext = Context.getCurrentContext();
        List<ReferenceScript> activeChain = this.activeScriptLocationChain.get(currentContext);
        if (activeChain != null)
        {
            throw new IllegalStateException("Context call chain has already been initialized");
        }

        final List<ReferenceScript> parentChain = this.activeScriptLocationChain.get(parentContext);
        if (parentChain == null)
        {
            throw new IllegalArgumentException("Parent context has no call chain associated with it");
        }

        activeChain = new ArrayList<ReferenceScript>(parentChain);
        this.activeScriptLocationChain.put(currentContext, activeChain);
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
            // can use synchronized here since scope creation / registration should not occur that often in a relevant
            // production scenario
            // (when immutable scopes are shared)
            synchronized (this.registeredContributors)
            {
                this.registeredContributors.add(contributor);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset()
    {
        this.scriptCacheLock.writeLock().lock();
        try
        {
            this.scriptCache.clear();
        }
        finally
        {
            this.scriptCacheLock.writeLock().unlock();
        }
        this.dynamicScriptCacheLock.writeLock().lock();
        try
        {
            this.dynamicScriptCache.clear();
        }
        finally
        {
            this.dynamicScriptCacheLock.writeLock().unlock();
        }
    }

    /**
     * @param valueConverter
     *            the valueConverter to set
     */
    public final void setValueConverter(final ValueConverter valueConverter)
    {
        this.valueConverter = valueConverter;
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
     * @param optimizationLevel
     *            the optimizaionLevel to set
     */
    public final void setOptimizationLevel(final int optimizationLevel)
    {
        if (!Context.isValidOptimizationLevel(optimizationLevel))
        {
            throw new IllegalArgumentException("Invalid optimization level: " + optimizationLevel);
        }
        this.optimizationLevel = optimizationLevel;
    }

    /**
     * @param failoverToLessOptimization
     *            the failoverToLessOptimization to set
     */
    public final void setFailoverToLessOptimization(final boolean failoverToLessOptimization)
    {
        this.failoverToLessOptimization = failoverToLessOptimization;
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

    protected void updateLocationChainsBeforeExceution(final Context currentContext)
    {
        final List<ReferenceScript> activeChain = this.activeScriptLocationChain.get(currentContext);
        if (activeChain != null)
        {
            List<List<ReferenceScript>> recursionChains = this.recursionScriptLocationChains.get(currentContext);
            if (recursionChains == null)
            {
                recursionChains = new LinkedList<List<ReferenceScript>>();
                this.recursionScriptLocationChains.put(currentContext, recursionChains);
            }

            recursionChains.add(0, activeChain);
        }
        this.activeScriptLocationChain.put(currentContext, new LinkedList<ReferenceScript>());
    }

    protected void updateLocationChainsAfterReturning(final Context currentContext)
    {
        this.activeScriptLocationChain.remove(currentContext);
        final List<List<ReferenceScript>> recursionChains = this.recursionScriptLocationChains.get(currentContext);
        if (recursionChains != null)
        {
            final List<ReferenceScript> previousChain = recursionChains.remove(0);
            if (recursionChains.isEmpty())
            {
                this.recursionScriptLocationChains.remove(currentContext);
            }
            this.activeScriptLocationChain.put(currentContext, previousChain);
        }
    }

    protected Script getCompiledScript(final ReferenceScript location)
    {
        Script script = null;
        String realPath = null;

        final Collection<ReferencePathType> supportedReferencePathTypes = location.getSupportedReferencePathTypes();
        for (final ReferencePathType pathType : REAL_PATH_SUCCESSION)
        {
            if (realPath == null && supportedReferencePathTypes.contains(pathType))
            {
                realPath = location.getReferencePath(pathType);
            }
        }

        final String classPath = location.getReferencePath(CommonReferencePath.CLASSPATH);

        if (realPath == null)
        {
            final String path = location instanceof ScriptLocationAdapter ? ((ScriptLocationAdapter) location).getPath() : location
                    .getFullName();

            // check if the path is in classpath form
            // TODO: can we generalize external form file:// to a classpath-relative location? (best-effort)
            if (!path.matches("^(classpath[*]?:).*$") && (classPath == null || !path.equals(classPath)))
            {
                // take path as is - can be anything depending on how content is loaded
                realPath = path;
            }
            else
            {
                // we always want to have a fully-qualified file-protocol path (unless we can generalize all to classpath-relative
                // locations)
                final String resourcePath;
                if (classPath != null && classPath.equals(path))
                {
                    resourcePath = classPath;
                }
                else
                {
                    resourcePath = path.substring(path.indexOf(':') + 1);
                }
                URL resource = this.getClass().getClassLoader().getResource(resourcePath);
                if (resource == null && resourcePath.startsWith("/"))
                {
                    resource = this.getClass().getClassLoader().getResource(resourcePath.substring(1));
                }
                if (resource != null)
                {
                    realPath = resource.toExternalForm();
                }
                else
                {
                    // should not occur in normal circumstances, but since ScriptLocation can be anything...
                    realPath = path;
                }
            }
        }

        // store since it may be reset between cache-check and cache-put, and we don't want debug-enabled scripts cached
        final boolean debuggerActive = this.debuggerActive;
        final boolean dynamicScript = location instanceof DynamicScript;
        // test the cache for a pre-compiled script matching our path
        if (this.compileScripts && !debuggerActive && (dynamicScript || location.isCachable()))
        {
            script = this.lookupScriptCache(dynamicScript ? this.dynamicScriptCache : this.scriptCache,
                    dynamicScript ? this.dynamicScriptCacheLock : this.scriptCacheLock, realPath);
        }

        if (script == null)
        {
            LOGGER.debug("Resolving and compiling script path: {}", realPath);

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

            if (this.compileScripts && !debuggerActive && (dynamicScript || location.isCachable()))
            {
                this.updateScriptCache(dynamicScript ? this.dynamicScriptCache : this.scriptCache,
                        dynamicScript ? this.dynamicScriptCacheLock : this.scriptCacheLock, realPath, script);
            }

            LOGGER.debug("Compiled script for {}", realPath);
        }
        else
        {
            LOGGER.debug("Using previously compiled script for {}", realPath);
        }

        return script;
    }

    protected Script getCompiledScript(final String source, final String path)
    {
        ParameterCheck.mandatoryString("path", path);
        // only mandatory - may be empty string as NO-OP script
        ParameterCheck.mandatory("source", source);
        try
        {
            final Script script;
            final String resolvedSource = this.resolveScriptImports(source);
            final Context cx = Context.enter();
            try
            {
                if (this.compileScripts && !this.debuggerActive)
                {
                    cx.setGeneratingDebug(true);
                    cx.setGeneratingSource(true);

                    int optimizationLevel = this.optimizationLevel;
                    Script bestEffortOptimizedScript = null;

                    while (optimizationLevel >= -1 && bestEffortOptimizedScript == null)
                    {
                        try
                        {
                            cx.setOptimizationLevel(optimizationLevel--);
                            bestEffortOptimizedScript = cx.compileString(resolvedSource, path, 1, null);
                        }
                        catch (final RuntimeException ex)
                        {
                            // unfortunately, all exceptions emitted from compilation are RuntimeExceptions
                            // but at the least, they are RuntimeException specifically
                            if (!this.failoverToLessOptimization || !ex.getClass().isAssignableFrom(RuntimeException.class))
                            {
                                // if failover is not to be attempted or exception is a specialized RuntimeException
                                throw ex;
                            }
                            else if (optimizationLevel > -1)
                            {
                                // we do at least log
                                LOGGER.info(
                                        "Compilation failed of {} failed with runtime exception {} - attempting lower optimization level",
                                        path, ex.getMessage());
                            }
                            else
                            {
                                // we do at least log
                                LOGGER.info("Compilation failed of {} failed with runtime exception {} - no further attempt", path,
                                        ex.getMessage());
                            }
                        }
                    }
                    script = bestEffortOptimizedScript;
                }
                else
                {
                    cx.setOptimizationLevel(-1);
                    script = cx.compileString(resolvedSource, path, 1, null);
                }
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

    protected Script lookupScriptCache(final Map<String, Script> cache, final ReadWriteLock lock, final String key)
    {
        Script script;
        lock.readLock().lock();
        try
        {
            script = cache.get(key);
        }
        finally
        {
            lock.readLock().unlock();
        }
        return script;
    }

    protected void updateScriptCache(final Map<String, Script> cache, final ReadWriteLock lock, final String key, final Script script)
    {
        lock.writeLock().lock();
        try
        {
            cache.put(key, script);

            if (cache.size() > this.maxScriptCacheSize)
            {
                final Iterator<String> keyIterator = cache.keySet().iterator();
                while (cache.size() > this.maxScriptCacheSize)
                {
                    final String keyToRemove = keyIterator.next();
                    cache.remove(keyToRemove);
                }
            }
        }
        finally
        {
            lock.writeLock().unlock();
        }
    }

    protected Object executeScriptImpl(final Script script, final Map<String, Object> model, final boolean secureScript,
            final String debugScriptName) throws AlfrescoRuntimeException
    {
        final long startTime = System.currentTimeMillis();
        LOGGER.info("{} Start", debugScriptName);
        LEGACY_CALL_LOGGER.debug("{} Start", debugScriptName);

        final ValueConverter previousConverter = ValueConverter.GLOBAL_CONVERTER.get();
        ValueConverter.GLOBAL_CONVERTER.set(this.valueConverter);

        final Context cx = Context.enter();
        try
        {
            final DelegatingWrapFactory wrapFactory = new DelegatingWrapFactory();
            cx.setWrapFactory(wrapFactory);

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

            wrapFactory.setScope(scope);

            // insert supplied object model into root of the default scope
            for (final String key : model.keySet())
            {
                final Object obj = model.get(key);

                // set the root scope on appropriate objects
                // this is used to allow native JS object creation etc.
                if (obj instanceof Scopeable)
                {
                    ((Scopeable) obj).setScope(scope);
                }

                // convert/wrap each object to JavaScript compatible
                final Object jsObject = this.valueConverter.convertValueForScript(obj);

                // repeat on resulting object (may have been converted into Scopeable)
                if (jsObject instanceof Scopeable)
                {
                    ((Scopeable) jsObject).setScope(scope);
                }

                // insert into the root scope ready for access by the script
                ScriptableObject.putProperty(scope, key, jsObject);
            }

            // execute the script and return the result
            final Object scriptResult = this.executeScriptInScopeImpl(script, scope);

            return scriptResult;
        }
        catch (final WrappedException w)
        {
            LOGGER.info("{} Exception: {}", debugScriptName, w);
            LEGACY_CALL_LOGGER.debug("{} Exception: {}", debugScriptName, w);

            final Throwable err = w.getWrappedException();
            throw new ScriptException(w.getMessage(), err);
        }
        catch (final Exception ex)
        {
            LOGGER.info("{} Exception: {}", debugScriptName, ex);
            LEGACY_CALL_LOGGER.debug("{} Exception: {}", debugScriptName, ex);

            throw new ScriptException(ex.getMessage(), ex);
        }
        finally
        {
            Context.exit();

            ValueConverter.GLOBAL_CONVERTER.set(previousConverter);

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
            cx.setLocale(I18NUtil.getLocale());

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
                    final Object jsObject = this.valueConverter.convertValueForScript(ex);

                    if (jsObject instanceof Scopeable)
                    {
                        ((Scopeable) jsObject).setScope(scope);
                    }

                    // insert into the scope ready for access by the script
                    ScriptableObject.putProperty(scope, ex.getExtensionName(), jsObject);
                }
            }

            final Object scriptResult = script.exec(cx, scope);

            // extract java object result if wrapped by Rhino
            final Object result = this.valueConverter.convertValueForJava(scriptResult);
            return result;
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

        synchronized (this.registeredContributors)
        {
            for (final ScopeContributor contributor : this.registeredContributors)
            {
                contributor.contributeToScope(scope, trustworthyScript, mutableScope);
            }
        }

        return scope;
    }
}
