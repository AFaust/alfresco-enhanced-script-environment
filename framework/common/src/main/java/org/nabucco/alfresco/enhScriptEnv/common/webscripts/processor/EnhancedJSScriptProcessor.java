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

import java.lang.reflect.Method;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
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
import org.alfresco.util.MD5;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.debug.DebuggableScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.ScopeContributor;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript.CommonReferencePath;
import org.nabucco.alfresco.enhScriptEnv.common.util.SourceFileVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
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
public class EnhancedJSScriptProcessor extends BaseRegisterableScriptProcessor implements InitializingBean, ScriptProcessor,
        EnhancedScriptProcessor<ScriptContent>
{
    private static final String CLASSPATH_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s+)*=(\\s*\\n*\\s+)*\"classpath:(/)?([^\"]+)\"(\\s*\\n*\\s+)*(/)?>";
    private static final String CLASSPATH_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"classpath\", \"/$5\", true);";

    private static final String STORE_PATH_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s+)*=(\\s*\\n*\\s+)*\"([^\"]+)\"(\\s*\\n*\\s+)*(/)?>";
    private static final String STORE_PATH_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"storePath\", \"$4\", true);";

    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancedJSScriptProcessor.class);

    private static final int DEFAULT_MAX_SCRIPT_CACHE_SIZE = 200;

    private static final boolean ASM_AVAILABLE;
    static
    {
        boolean asmAvailable = false;
        try
        {
            final Class<?> classReaderCls = Class.forName("org.objectweb.asm.ClassReader");
            asmAvailable = classReaderCls != null;
        }
        catch (final ClassNotFoundException clsNotFoundEx)
        {
            // NO-OP
        }
        ASM_AVAILABLE = asmAvailable;
    }

    // reuse existing implementation
    private static final WrapFactory WRAP_FACTORY = new PresentationWrapFactory();

    // used WeakHashMap here before to avoid accidental leaks but measures for proper cleanup have proven themselves during tests
    protected final Map<Context, List<ReferenceScript>> activeScriptContentChain = new ConcurrentHashMap<Context, List<ReferenceScript>>();
    protected final Map<Context, List<List<ReferenceScript>>> recursionScriptContentChains = new ConcurrentHashMap<Context, List<List<ReferenceScript>>>();

    protected boolean shareScopes = true;

    protected Scriptable unrestrictedShareableScope;
    protected Scriptable restrictedShareableScope;

    protected boolean compileScripts = true;
    protected volatile boolean debuggerActive = false;
    protected boolean failoverToLessOptimization = true;
    protected int optimizationLevel = -1;

    protected ScriptLoader standardScriptLoader;

    protected final Map<String, Script> scriptCache = new LinkedHashMap<String, Script>(256);
    protected final ReadWriteLock scriptCacheLock = new ReentrantReadWriteLock(true);

    protected final AtomicLong dynamicScriptCounter = new AtomicLong();

    protected final Map<String, Script> dynamicScriptByHashCache = new LinkedHashMap<String, Script>();
    protected final ReadWriteLock dynamicScriptCacheLock = new ReentrantReadWriteLock(true);

    protected int maxScriptCacheSize = DEFAULT_MAX_SCRIPT_CACHE_SIZE;

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
    public Object executeInScope(final String source, final Object scope)
    {
        ParameterCheck.mandatoryString("source", source);

        // compile the script based on the node content
        Script script = null;
        final MD5 md5 = new MD5();
        final String digest = md5.digest(source.getBytes());

        script = this.lookupScriptCache(this.dynamicScriptByHashCache, this.dynamicScriptCacheLock, digest);

        final String debugScriptName;
        if (script == null)
        {
            debugScriptName = "string://DynamicJS-" + String.valueOf(this.dynamicScriptCounter.getAndIncrement());
            script = this.getCompiledScript(source, debugScriptName);

            if (this.compileScripts)
            {
                this.updateScriptCache(this.dynamicScriptByHashCache, this.dynamicScriptCacheLock, digest, script);
            }
        }
        else if (script instanceof NativeFunction)
        {
            final DebuggableScript debuggableView = ((NativeFunction) script).getDebuggableView();
            if (debuggableView != null)
            {
                debugScriptName = debuggableView.getSourceName();
            }
            else
            {
                // obviously not an interpreted script
                if (ASM_AVAILABLE)
                {
                    final String sourceFileName = SourceFileVisitor.readSourceFile(script.getClass());
                    debugScriptName = sourceFileName != null ? sourceFileName : ("string://Cached-DynamicJS-" + digest);
                }
                else
                {
                    debugScriptName = "string://Cached-DynamicJS-" + digest;
                }
            }
        }
        else
        {
            debugScriptName = "string://Cached-DynamicJS-" + digest;
        }

        LOGGER.info("{} Start", debugScriptName);

        final long startTime = System.currentTimeMillis();
        final Context cx = Context.enter();
        try
        {
            List<ReferenceScript> currentChain = this.activeScriptContentChain.get(cx);
            boolean newChain = false;
            if (currentChain == null)
            {
                this.updateContentChainsBeforeExceution(cx);
                currentChain = this.activeScriptContentChain.get(cx);
                newChain = true;
            }
            // else: assume the original script chain is continued
            currentChain.add(new ReferenceScript.DynamicScript(debugScriptName));

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

                final Object result = this.executeScriptInScopeImpl(script, realScope);
                return result;
            }
            finally
            {
                currentChain.remove(currentChain.size() - 1);
                if (newChain)
                {
                    this.updateContentChainsAfterReturning(cx);
                }
            }
        }
        catch (final Exception ex)
        {
            // TODO: error handling / bubbling to caller? how to handle Rhino exceptions if caller is not a script?
            LOGGER.info("{} Exception: {}", debugScriptName, ex);
            throw new WebScriptException(MessageFormat.format("Failed to execute script string: {1}", ex.getMessage()), ex);
        }
        finally
        {
            Context.exit();

            final long endTime = System.currentTimeMillis();
            LOGGER.info("{} End {} msg", debugScriptName, Long.valueOf(endTime - startTime));
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object executeInScope(final ScriptContent content, final Object scope)
    {
        ParameterCheck.mandatory("content", content);

        final Script script = this.getCompiledScript(content);
        final ScriptContentAdapter contentAdapter = new ScriptContentAdapter(content, this.standardScriptLoader);
        final String debugScriptName = contentAdapter.getName();

        LOGGER.info("{} Start", debugScriptName);

        final long startTime = System.currentTimeMillis();
        final Context cx = Context.enter();
        try
        {
            List<ReferenceScript> currentChain = this.activeScriptContentChain.get(cx);
            boolean newChain = false;
            if (currentChain == null)
            {
                this.updateContentChainsBeforeExceution(cx);
                currentChain = this.activeScriptContentChain.get(cx);
                newChain = true;
            }
            // else: assume the original script chain is continued
            currentChain.add(contentAdapter);

            try
            {

                final Scriptable realScope;
                if (scope == null)
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
                else if (!(scope instanceof Scriptable))
                {
                    realScope = new NativeJavaObject(null, scope, scope.getClass());
                    if (this.shareScopes)
                    {
                        final Scriptable sharedScope = content.isSecure() ? this.unrestrictedShareableScope : this.restrictedShareableScope;
                        realScope.setPrototype(sharedScope);
                    }
                    else
                    {
                        final Scriptable baseScope = this.setupScope(cx, content.isSecure(), false);
                        realScope.setPrototype(baseScope);
                    }
                }
                else
                {
                    realScope = (Scriptable) scope;
                }

                final Object result = this.executeScriptInScopeImpl(script, realScope);
                return result;
            }
            finally
            {
                currentChain.remove(currentChain.size() - 1);
                if (newChain)
                {
                    this.updateContentChainsAfterReturning(cx);
                }
            }
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

            final long endTime = System.currentTimeMillis();
            LOGGER.info("{} End {} msg", debugScriptName, Long.valueOf(endTime - startTime));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object initializeScope(final ScriptContent location)
    {
        ParameterCheck.mandatory("location", location);

        final Scriptable scope;
        final Context cx = Context.enter();
        try
        {
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
        }

        return scope;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public ReferenceScript getContextScriptLocation()
    {
        final List<ReferenceScript> currentChain = this.activeScriptContentChain.get(Context.getCurrentContext());
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
        final List<ReferenceScript> currentChain = this.activeScriptContentChain.get(Context.getCurrentContext());
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
        List<ReferenceScript> activeChain = this.activeScriptContentChain.get(currentContext);
        if (activeChain != null)
        {
            throw new IllegalStateException("Context call chain has already been initialized");
        }

        final List<ReferenceScript> parentChain = this.activeScriptContentChain.get(parentContext);
        if (parentChain == null)
        {
            throw new IllegalArgumentException("Parent context has no call chain associated with it");
        }

        activeChain = new ArrayList<ReferenceScript>(parentChain);
        this.activeScriptContentChain.put(currentContext, activeChain);
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
            this.unrestrictedShareableScope = this.setupScope(cx, true, false);
        }
        finally
        {
            Context.exit();
        }

        cx = Context.enter();
        try
        {
            cx.setWrapFactory(WRAP_FACTORY);
            this.restrictedShareableScope = this.setupScope(cx, false, false);
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
        final ScriptContent scriptLocation = this.findScript(path);
        if (scriptLocation == null)
        {
            LOGGER.info("Unable to locate script {}", path);
            throw new WebScriptException(MessageFormat.format("Unable to locate script {0}", path));
        }
        // execute script
        return this.executeScript(scriptLocation, model);
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
        final ScriptContentAdapter contentAdapter = new ScriptContentAdapter(content, this.standardScriptLoader);
        final String debugScriptName = contentAdapter.getName();

        final Context cx = Context.enter();
        try
        {
            this.updateContentChainsBeforeExceution(cx);
            this.activeScriptContentChain.get(cx).add(contentAdapter);
            try
            {
                return this.executeScriptImpl(script, model, contentAdapter.isSecure(), debugScriptName);
            }
            finally
            {
                this.updateContentChainsAfterReturning(cx);
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
            this.dynamicScriptByHashCache.clear();
        }
        finally
        {
            this.dynamicScriptCacheLock.writeLock().unlock();
        }
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

    protected Script getCompiledScript(final ScriptContent content)
    {
        Script script = null;
        final String path = content.getPath();

        String realPath = null;
        String classPath = null;

        if (content instanceof ReferenceScript)
        {
            realPath = ((ReferenceScript) content).getReferencePath(CommonReferencePath.FILE);
            classPath = ((ReferenceScript) content).getReferencePath(CommonReferencePath.CLASSPATH);
        }

        if (realPath == null)
        {
            // check if the path is in classpath form
            // TODO: can we generalize external form file:// to a classpath-relative location? (best-effort)
            if (!path.matches("^(classpath(\\*)?:)+.*$"))
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
        // test the cache for a pre-compiled script matching our path
        if (this.compileScripts && !debuggerActive && content.isCachable())
        {
            script = this.lookupScriptCache(this.scriptCache, this.scriptCacheLock, content.getPath());
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

            if (this.compileScripts && !debuggerActive && content.isCachable())
            {
                this.updateScriptCache(this.scriptCache, this.scriptCacheLock, path, script);

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

        // With Alfresco 4.2.2/4.1.8, NativeJSON backported from Rhino 1.7R4 is added
        // reflectively check availability and initialize in scope
        try
        {
            final Class<?> nativeJSON = Class.forName("org.mozilla.javascript.NativeJSON");
            final Method init = nativeJSON.getMethod("init", new Class[] { Scriptable.class, boolean.class });
            init.invoke(null, new Object[] { scope, Boolean.valueOf(!mutableScope) });
        }
        catch (final Exception ex)
        {
            // NO-OP - earlier versions simply don't support it
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

    protected void updateContentChainsBeforeExceution(final Context currentContext)
    {
        final List<ReferenceScript> activeChain = this.activeScriptContentChain.get(currentContext);
        if (activeChain != null)
        {
            List<List<ReferenceScript>> recursionChains = this.recursionScriptContentChains.get(currentContext);
            if (recursionChains == null)
            {
                recursionChains = new LinkedList<List<ReferenceScript>>();
                this.recursionScriptContentChains.put(currentContext, recursionChains);
            }

            recursionChains.add(0, activeChain);
        }
        this.activeScriptContentChain.put(currentContext, new LinkedList<ReferenceScript>());
    }

    protected void updateContentChainsAfterReturning(final Context currentContext)
    {
        this.activeScriptContentChain.remove(currentContext);
        final List<List<ReferenceScript>> recursionChains = this.recursionScriptContentChains.get(currentContext);
        if (recursionChains != null)
        {
            final List<ReferenceScript> previousChain = recursionChains.remove(0);
            if (recursionChains.isEmpty())
            {
                this.recursionScriptContentChains.remove(currentContext);
            }
            this.activeScriptContentChain.put(currentContext, previousChain);
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

    /**
     * @param standardScriptLoader
     *            the standardScriptLoader to set
     */
    public final void setStandardScriptLoader(final ScriptLoader standardScriptLoader)
    {
        this.standardScriptLoader = standardScriptLoader;
    }
}
