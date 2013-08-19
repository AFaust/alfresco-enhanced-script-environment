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
package org.nabucco.alfresco.enhScriptEnv.common.script;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.alfresco.util.Pair;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrappedException;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript.ReferencePathType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class LogFunction implements IdFunctionCall, InitializingBean, ScopeContributor
{

    public static final String LOGGER_OBJ_NAME = "logger";
    public static final Object LOG_FUNC_TAG = new Object();

    public static final int TRACE_FUNC_ID = 100;
    public static final int DEBUG_FUNC_ID = 101;
    public static final int INFO_FUNC_ID = 102;
    public static final int WARN_FUNC_ID = 103;
    public static final int ERROR_FUNC_ID = 104;

    public static final String TRACE_FUNC_NAME = "trace";
    public static final String DEBUG_FUNC_NAME = "debug";
    public static final String INFO_FUNC_NAME = "info";
    public static final String WARN_FUNC_NAME = "warn";
    public static final String ERROR_FUNC_NAME = "error";
    public static final String LOG_FUNC_NAME = "log";

    public static final int TRACE_ENABLED_FUNC_ID = 200;
    public static final int DEBUG_ENABLED_FUNC_ID = 201;
    public static final int INFO_ENABLED_FUNC_ID = 202;
    public static final int WARN_ENABLED_FUNC_ID = 203;
    public static final int ERROR_ENABLED_FUNC_ID = 204;

    public static final String TRACE_ENABLED_FUNC_NAME = "isTraceEnabled";
    public static final String DEBUG_ENABLED_FUNC_NAME = "isDebugEnabled";
    public static final String DEBUG_LOGGING_ENABLED_FUNC_NAME = "isDebugLoggingEnabled";
    public static final String LOGGING_ENABLED_FUNC_NAME = "isLoggingEnabled";
    public static final String INFO_ENABLED_FUNC_NAME = "isInfoEnabled";
    public static final String INFO_LOGGING_ENABLED_FUNC_NAME = "isInfoLoggingEnabled";
    public static final String WARN_ENABLED_FUNC_NAME = "isWarnEnabled";
    public static final String WARN_LOGGING_ENABLED_FUNC_NAME = "isWarnLoggingEnabled";
    public static final String ERROR_ENABLED_FUNC_NAME = "isErrorEnabled";
    public static final String ERROR_LOGGING_ENABLED_FUNC_NAME = "isErrorLoggingEnabled";

    public static final int SET_LOGGER_FUNC_ID = 300;
    public static final int SET_INHERIT_LOGGER_CTX_FUNC_ID = 301;
    public static final int REGISTER_CHILD_SCOPE_FUNC_ID = 302;

    public static final String SET_LOGGER_FUNC_NAME = "setLogger";
    public static final String SET_INHERIT_LOGGER_CTX_FUNC_NAME = "setInheritLoggerContext";
    public static final String REGISTER_CHILD_SCOPE_FUNC_NAME = "registerChildScope";

    public static final int GET_SYSTEM_FUNC_ID = 400;
    public static final int OUT_FUNC_ID = 401;

    public static final String GET_SYSTEM_FUNC_NAME = "getSystem";
    public static final String SYSTEM_PROP_NAME = "system";
    public static final String OUT_FUNC_NAME = "out";

    public static final int SET_SCRIPT_LOGGER_FUNC_ID = 500;

    public static final String SET_SCRIPT_LOGGER_FUNC_NAME = "setScriptLogger";

    private static final Logger LOGGER = LoggerFactory.getLogger(LogFunction.class);

    protected EnhancedScriptProcessor<? extends ReferenceScript> scriptProcessor;

    protected ValueConverter valueConverter;

    protected String defaultLoggerPrefix;

    // optional
    protected String legacyLoggerName;

    /**
     * Logger data map for state management and logger caching
     */
    protected final Map<Scriptable, Map<ReferenceScript, LoggerData>> scopeLoggerData = new WeakHashMap<Scriptable, Map<ReferenceScript, LoggerData>>();

    /**
     * Parent scope registry for bottom-up lookup and including context script in which parent-child relation was registered
     */
    protected final Map<Scriptable, Pair<WeakReference<Scriptable>, ReferenceScript>> scopeParents = new WeakHashMap<Scriptable, Pair<WeakReference<Scriptable>, ReferenceScript>>();

    protected final ReadWriteLock scopeLoggerDataLock = new ReentrantReadWriteLock(true);

    protected final ReadWriteLock scopeParentLock = new ReentrantReadWriteLock(true);

    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "scriptProcessor", this.scriptProcessor);
        PropertyCheck.mandatory(this, "valueConverter", this.valueConverter);
        PropertyCheck.mandatory(this, "defaultLoggerPrefix", this.defaultLoggerPrefix);

        this.scriptProcessor.registerScopeContributor(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execIdCall(final IdFunctionObject f, final Context cx, final Scriptable scope, final Scriptable thisObj,
            final Object[] args)
    {
        final Object result;

        if (f.hasTag(LOG_FUNC_TAG))
        {
            final int methodId = f.methodId();
            if (methodId >= TRACE_FUNC_ID && methodId <= ERROR_FUNC_ID)
            {
                this.handleLogging(cx, scope, thisObj, args, methodId);

                result = null;
            }
            else if (methodId >= TRACE_ENABLED_FUNC_ID && methodId <= ERROR_ENABLED_FUNC_ID)
            {
                result = this.handleEnablementCheck(cx, scope, thisObj, methodId);
            }
            else if (methodId == SET_LOGGER_FUNC_ID)
            {
                this.handleSetLogger(scope, args);

                result = null;
            }
            else if (methodId == SET_INHERIT_LOGGER_CTX_FUNC_ID)
            {
                this.handleSetLoggerInheritance(scope, args);

                result = null;
            }
            else if (methodId == REGISTER_CHILD_SCOPE_FUNC_ID)
            {
                this.handleRegisterChildScope(scope, args);

                result = null;
            }
            else if (methodId == GET_SYSTEM_FUNC_ID)
            {
                result = ScriptableObject.getProperty(thisObj, SYSTEM_PROP_NAME);
            }
            else if (methodId == OUT_FUNC_ID)
            {
                this.handleOut(cx, scope, thisObj, args);

                result = null;
            }
            else if (methodId == SET_SCRIPT_LOGGER_FUNC_ID)
            {
                this.handleSetScriptLogger(cx, scope, thisObj, args);

                result = null;
            }
            else
            {
                throw new IllegalArgumentException(String.valueOf(methodId));
            }
        }
        else
        {
            throw f.unknown();
        }

        return result;
    }

    protected void handleRegisterChildScope(final Scriptable scope, final Object[] args)
    {
        if (args.length >= 1)
        {
            final Object scopeParam = args[0];
            if (scopeParam instanceof Scriptable)
            {
                final Scriptable childScope = (Scriptable) scopeParam;
                final ReferenceScript script = this.scriptProcessor.getContextScriptLocation();
                this.scopeParentLock.writeLock().lock();
                try
                {
                    this.scopeParents.put(childScope, new Pair<WeakReference<Scriptable>, ReferenceScript>(new WeakReference<Scriptable>(
                            scope), script));
                }
                finally
                {
                    this.scopeParentLock.writeLock().unlock();
                }
            }
            else
            {
                throw new IllegalArgumentException("Parameter is not a valid JavaScript scope object");
            }
        }
        else
        {
            throw new IllegalArgumentException("Child scope parameter is missings");
        }
    }

    protected void handleSetLoggerInheritance(final Scriptable scope, final Object[] args)
    {
        final boolean inheritLoggerContext = ScriptRuntime.toBoolean(args, 0);

        final ReferenceScript referenceScript = this.scriptProcessor.getContextScriptLocation();
        final LoggerData loggerData = this.getLoggerData(scope, referenceScript, true);
        loggerData.setInheritLoggerContext(inheritLoggerContext);
    }

    protected void handleSetLogger(final Scriptable scope, final Object[] args)
    {
        if (args.length == 0 || args[0] == Undefined.instance || args[0] == null)
        {
            throw new IllegalArgumentException("No logger name provided");
        }

        final String explicitLogger = ScriptRuntime.toString(args, 0);
        final ReferenceScript referenceScript = this.scriptProcessor.getContextScriptLocation();
        final LoggerData loggerData = this.getLoggerData(scope, referenceScript, true);

        if (loggerData.getLoggers() == null && loggerData.getExplicitLogger() == null)
        {
            loggerData.setExplicitLogger(explicitLogger);
        }
        else
        {
            if (loggerData.getExplicitLogger() != null)
            {
                throw new IllegalStateException("Explicit logger already set");
            }
            else if (loggerData.getLoggers() != null)
            {
                throw new IllegalStateException("Loggers already initialized");
            }
        }
    }

    protected Object handleEnablementCheck(final Context cx, final Scriptable scope, final Scriptable thisObj, final int methodId)
    {
        final Object result;
        final Collection<Logger> loggers = this.getLoggers(cx, scope, true);
        boolean enabled = false;
        for (final Logger logger : loggers)
        {
            switch (methodId)
            {
            case TRACE_ENABLED_FUNC_ID:
                enabled = enabled || logger.isTraceEnabled();
                break;
            case DEBUG_ENABLED_FUNC_ID:
                enabled = enabled || logger.isDebugEnabled();
                break;
            case INFO_ENABLED_FUNC_ID:
                enabled = enabled || logger.isInfoEnabled();
                break;
            case WARN_ENABLED_FUNC_ID:
                enabled = enabled || logger.isWarnEnabled();
                break;
            case ERROR_ENABLED_FUNC_ID:
                enabled = enabled || logger.isErrorEnabled();
                break;
            default:
                throw new IllegalArgumentException("Unsupported method ID");
            }
        }

        result = Boolean.valueOf(enabled);
        return result;
    }

    protected void handleLogging(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args, final int methodId)
    {
        final Collection<Logger> loggers = this.getLoggers(cx, scope, true);
        final ReferenceScript topLevelScript = this.scriptProcessor.getScriptCallChain().get(0);
        final LoggerData globalLoggerData = this.getLoggerData(scope, topLevelScript, false);
        final Scriptable scriptLogger = globalLoggerData != null ? globalLoggerData.getScriptLogger() : null;

        if (args.length == 0 || args[0] == Undefined.instance || args[0] == null)
        {
            throw new IllegalArgumentException("No message provided");
        }

        if (args.length == 1)
        {
            final String message = ScriptRuntime.toString(args, 0);
            this.log(methodId, loggers, scriptLogger, message);
        }
        else if (args.length > 1)
        {
            final String message = ScriptRuntime.toString(args, 0);
            final Object secondParam = args[1];
            if (secondParam instanceof Throwable)
            {
                final Throwable ex;
                if (secondParam instanceof WrappedException)
                {
                    ex = ((WrappedException) secondParam).getWrappedException();
                }
                else
                {
                    ex = (Throwable) secondParam;
                }
                this.log(methodId, loggers, scriptLogger, message, ex);
            }
            else
            {
                final Object[] params = new Object[args.length - 1];
                params[0] = this.valueConverter.convertValueForJava(secondParam);
                for (int argsIdx = 2, idx = 1; argsIdx < args.length && idx < params.length; argsIdx++, idx++)
                {
                    params[idx] = this.valueConverter.convertValueForJava(args[argsIdx]);
                }
                this.log(methodId, loggers, scriptLogger, message, params);
            }
        }
    }

    protected void handleOut(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args)
    {
        if (args.length == 0 || args[0] == Undefined.instance || args[0] == null)
        {
            throw new IllegalArgumentException("No message provided");
        }

        if (args.length >= 1)
        {
            final String message = ScriptRuntime.toString(args, 0);

            final ReferenceScript contextScriptLocation = this.scriptProcessor.getContextScriptLocation();
            LOGGER.warn("Script {} logging to System.out: {}", contextScriptLocation, message);
            System.out.println(message);
        }
    }

    protected void handleSetScriptLogger(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args)
    {
        if (args.length >= 1)
        {
            final Scriptable scriptLogger = ScriptRuntime.toObject(scope, args[0]);
            final Object registeredScriptLogger = ScriptableObject.getProperty(scope, LOGGER_OBJ_NAME);
            if (registeredScriptLogger == scriptLogger)
            {
                throw new IllegalArgumentException("Setting the root-scope logger as scriptLogger is not allowed due to recursion");
            }

            // ScriptLogger is always treated as a top-level context data object, so retrieve the top-level logger data
            final ReferenceScript topLevelScript = this.scriptProcessor.getScriptCallChain().get(0);
            final LoggerData loggerData = this.getLoggerData(scope, topLevelScript, true);
            final Scriptable setScriptLogger = loggerData.getScriptLogger();
            if (setScriptLogger != null)
            {
                throw new IllegalStateException("ScriptLogger has already been set - not allowed to replace it once set");
            }
            else
            {
                loggerData.setScriptLogger(scriptLogger);
            }
        }
        else
        {
            throw new IllegalArgumentException("Parameter scriptLogger is missing");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contributeToScope(final Scriptable scope, final boolean trustworthyScript, final boolean mutableScope)
    {
        final NativeObject loggerObj = new NativeObject();

        this.exportFunction(DEBUG_FUNC_ID, DEBUG_FUNC_NAME, 1, loggerObj);
        this.exportFunction(DEBUG_FUNC_ID, LOG_FUNC_NAME, 1, loggerObj);
        this.exportFunction(TRACE_FUNC_ID, TRACE_FUNC_NAME, 1, loggerObj);
        this.exportFunction(INFO_FUNC_ID, INFO_FUNC_NAME, 1, loggerObj);
        this.exportFunction(WARN_FUNC_ID, WARN_FUNC_NAME, 1, loggerObj);
        this.exportFunction(ERROR_FUNC_ID, ERROR_FUNC_NAME, 1, loggerObj);

        this.exportFunction(DEBUG_ENABLED_FUNC_ID, DEBUG_ENABLED_FUNC_NAME, 0, loggerObj);
        this.exportFunction(DEBUG_ENABLED_FUNC_ID, DEBUG_LOGGING_ENABLED_FUNC_NAME, 0, loggerObj);
        this.exportFunction(DEBUG_ENABLED_FUNC_ID, LOGGING_ENABLED_FUNC_NAME, 0, loggerObj);

        this.exportFunction(TRACE_ENABLED_FUNC_ID, TRACE_ENABLED_FUNC_NAME, 0, loggerObj);

        this.exportFunction(DEBUG_ENABLED_FUNC_ID, INFO_ENABLED_FUNC_NAME, 0, loggerObj);
        this.exportFunction(DEBUG_ENABLED_FUNC_ID, INFO_LOGGING_ENABLED_FUNC_NAME, 0, loggerObj);

        this.exportFunction(WARN_ENABLED_FUNC_ID, WARN_ENABLED_FUNC_NAME, 0, loggerObj);
        this.exportFunction(WARN_ENABLED_FUNC_ID, WARN_LOGGING_ENABLED_FUNC_NAME, 0, loggerObj);

        this.exportFunction(ERROR_ENABLED_FUNC_ID, ERROR_ENABLED_FUNC_NAME, 0, loggerObj);
        this.exportFunction(ERROR_ENABLED_FUNC_ID, ERROR_LOGGING_ENABLED_FUNC_NAME, 0, loggerObj);

        this.exportFunction(REGISTER_CHILD_SCOPE_FUNC_ID, REGISTER_CHILD_SCOPE_FUNC_NAME, 1, loggerObj);
        this.exportFunction(SET_LOGGER_FUNC_ID, SET_LOGGER_FUNC_NAME, 1, loggerObj);
        this.exportFunction(SET_INHERIT_LOGGER_CTX_FUNC_ID, SET_INHERIT_LOGGER_CTX_FUNC_NAME, 1, loggerObj);

        this.exportFunction(GET_SYSTEM_FUNC_ID, GET_SYSTEM_FUNC_NAME, 0, loggerObj);

        this.exportFunction(SET_SCRIPT_LOGGER_FUNC_ID, SET_SCRIPT_LOGGER_FUNC_NAME, 1, loggerObj);

        // define system object
        final NativeObject systemObj = new NativeObject();
        this.exportFunction(OUT_FUNC_ID, OUT_FUNC_NAME, 1, systemObj);
        systemObj.sealObject();

        ScriptableObject.defineProperty(loggerObj, SYSTEM_PROP_NAME, systemObj, ScriptableObject.PERMANENT | ScriptableObject.READONLY);

        loggerObj.sealObject();

        // export as read-only and undeleteable property of the scope
        ScriptableObject.defineProperty(scope, LOGGER_OBJ_NAME, loggerObj, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
    }

    /**
     * @param legacyLoggerName
     *            the legacyLoggerName to set
     */
    public final void setLegacyLoggerName(final String legacyLoggerName)
    {
        this.legacyLoggerName = legacyLoggerName;
    }

    /**
     * @param defaultLoggerPrefix
     *            the defaultLoggerPrefix to set
     */
    public final void setDefaultLoggerPrefix(final String defaultLoggerPrefix)
    {
        this.defaultLoggerPrefix = defaultLoggerPrefix;
    }

    /**
     * @param scriptProcessor
     *            the scriptProcessor to set
     */
    public final void setScriptProcessor(final EnhancedScriptProcessor scriptProcessor)
    {
        this.scriptProcessor = scriptProcessor;
    }

    /**
     * @param valueConverter
     *            the valueConverter to set
     */
    public final void setValueConverter(final ValueConverter valueConverter)
    {
        this.valueConverter = valueConverter;
    }

    protected void exportFunction(final int methodId, final String name, final int arity, final Scriptable scope)
    {
        final IdFunctionObject func = new IdFunctionObject(this, LOG_FUNC_TAG, methodId, name, arity, scope);
        func.sealObject();

        // export as read-only and undeleteable property of the scope
        ScriptableObject.defineProperty(scope, name, func, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
    }

    protected Collection<Logger> getLoggers(final Context context, final Scriptable scope, final boolean createIfNull)
    {
        final Collection<Logger> loggers;

        // TODO: what about logging in functions defined in script A but called from script B (due to scope access or explicit passing)?
        final ReferenceScript referenceScript = this.scriptProcessor.getContextScriptLocation();

        final LoggerData loggerData = this.getLoggerData(scope, referenceScript, createIfNull);
        loggers = this.getLoggers(context, scope, referenceScript, loggerData);

        return loggers;
    }

    protected Collection<Logger> getLoggers(final Context context, final Scriptable scope, final ReferenceScript referenceScript,
            final LoggerData loggerData)
    {
        final Collection<Logger> loggers;
        if (loggerData == null || loggerData.getLoggers() == null)
        {
            loggers = new HashSet<Logger>();
            if (referenceScript != null)
            {
                loggers.addAll(this.getParentLoggers(context, scope, referenceScript));
            }

            if (loggerData != null && loggerData.getExplicitLogger() != null)
            {
                loggers.add(LoggerFactory.getLogger(loggerData.getExplicitLogger()));
            }
            else if (!this.isParentLoggerExplicit(context, scope, referenceScript))
            {
                if (this.legacyLoggerName != null)
                {
                    loggers.add(LoggerFactory.getLogger(this.legacyLoggerName));
                }

                if (referenceScript != null)
                {
                    final Collection<ReferencePathType> supportedReferencePathTypes = referenceScript.getSupportedReferencePathTypes();
                    for (final ReferencePathType referencePathType : supportedReferencePathTypes)
                    {
                        final String referencePath = referenceScript.getReferencePath(referencePathType);
                        if (referencePath != null)
                        {
                            final String loggerSuffix = referencePath.replace('.', '_').replace('/', '.');
                            final String loggerName = MessageFormat.format("{0}.{1}.{2}", this.defaultLoggerPrefix, referencePathType,
                                    loggerSuffix);
                            loggers.add(LoggerFactory.getLogger(loggerName));
                        }
                    }
                }
            }

            if (loggerData != null)
            {
                loggerData.setLoggers(loggers);
            }
        }
        else
        {
            loggers = loggerData.getLoggers();
        }
        return loggers;
    }

    protected Collection<Logger> getParentLoggers(final Context context, final Scriptable scope, final ReferenceScript script)
    {
        // determine parent script from call chain
        final List<? extends ReferenceScript> scriptCallChain = this.scriptProcessor.getScriptCallChain();
        final int scriptIndex = scriptCallChain.indexOf(script);
        final ReferenceScript parentScript = scriptIndex > 0 ? scriptCallChain.get(scriptIndex - 1) : null;

        // determine parent scope from explicit registration
        final Pair<WeakReference<Scriptable>, ReferenceScript> scopeParentPair;

        this.scopeParentLock.readLock().lock();
        try
        {
            scopeParentPair = this.scopeParents.get(scope);
        }
        finally
        {
            this.scopeParentLock.readLock().unlock();
        }
        // use parent scope only if one has been registered and the script it was registered for is the identical script retrieved from the
        // call chain
        final Scriptable parentScope = scopeParentPair == null || (scopeParentPair.getSecond() != parentScript) ? scope : scopeParentPair
                .getFirst().get();

        final LoggerData parentLoggerData = parentScope != null && parentScript != null ? this.getLoggerData(parentScope, parentScript,
                false) : null;
        final Collection<Logger> loggers;
        if (parentLoggerData == null || parentLoggerData.isInheritLoggerContext())
        {
            loggers = this.getLoggers(context, parentScope, parentScript, parentLoggerData);
        }
        else
        {
            loggers = Collections.emptySet();
        }

        return loggers;
    }

    protected boolean isParentLoggerExplicit(final Context context, final Scriptable scope, final ReferenceScript script)
    {
        // determine parent script from call chain
        final List<? extends ReferenceScript> scriptCallChain = this.scriptProcessor.getScriptCallChain();
        final int scriptIndex = scriptCallChain.indexOf(script);
        final ReferenceScript parentScript = scriptIndex > 0 ? scriptCallChain.get(scriptIndex - 1) : null;

        final boolean result;
        if (parentScript != null)
        {
            // determine parent scope from explicit registration
            final Pair<WeakReference<Scriptable>, ReferenceScript> scopeParentPair;

            this.scopeParentLock.readLock().lock();
            try
            {
                scopeParentPair = this.scopeParents.get(scope);
            }
            finally
            {
                this.scopeParentLock.readLock().unlock();
            }
            // use parent scope only if one has been registered and the script it was registered for is the identical script retrieved from
            // the call chain
            final Scriptable parentScope = scopeParentPair == null || (scopeParentPair.getSecond() != parentScript) ? scope
                    : scopeParentPair.getFirst().get();

            final LoggerData parentLoggerData = parentScope != null ? this.getLoggerData(parentScope, parentScript, false) : null;

            // check immediate parent
            final boolean nextParentLoggerIsExplicit = parentLoggerData != null && parentLoggerData.getExplicitLogger() != null
                    && parentLoggerData.isInheritLoggerContext();
            // recursive check unless inheritance is off
            final boolean ancestorLoggerIsExplicit = (parentLoggerData == null || parentLoggerData.isInheritLoggerContext())
                    && this.isParentLoggerExplicit(context, parentScope, parentScript);
            result = nextParentLoggerIsExplicit || ancestorLoggerIsExplicit;
        }
        else
        {
            result = false;
        }
        return result;
    }

    protected Map<ReferenceScript, LoggerData> getScriptLoggerDataForContext(final Scriptable scope, final boolean createIfNull)
    {
        Map<ReferenceScript, LoggerData> dataByScript = null;
        this.scopeLoggerDataLock.readLock().lock();
        try
        {
            dataByScript = this.scopeLoggerData.get(scope);
        }
        finally
        {
            this.scopeLoggerDataLock.readLock().unlock();
        }

        if (dataByScript == null && createIfNull)
        {
            dataByScript = new IdentityHashMap<ReferenceScript, LogFunction.LoggerData>();
            this.scopeLoggerDataLock.writeLock().lock();
            try
            {
                this.scopeLoggerData.put(scope, dataByScript);
            }
            finally
            {
                this.scopeLoggerDataLock.writeLock().unlock();
            }
        }

        return dataByScript;
    }

    protected LoggerData getLoggerData(final Scriptable scope, final ReferenceScript script, final boolean createIfNull)
    {
        // this is like an internal assertion
        ParameterCheck.mandatory("scope", scope);
        ParameterCheck.mandatory("script", script);

        LoggerData loggerData = null;

        final Map<ReferenceScript, LoggerData> loggerDataByScript = this.getScriptLoggerDataForContext(scope, createIfNull);
        loggerData = loggerDataByScript != null ? loggerDataByScript.get(script) : null;
        if (loggerDataByScript != null && loggerData == null && createIfNull)
        {
            loggerData = new LoggerData();
            loggerDataByScript.put(script, loggerData);
        }

        if (loggerData == null)
        {
            // determine parent scope from explicit registration
            final Pair<WeakReference<Scriptable>, ReferenceScript> scopeParentPair;

            this.scopeParentLock.readLock().lock();
            try
            {
                scopeParentPair = this.scopeParents.get(scope);
            }
            finally
            {
                this.scopeParentLock.readLock().unlock();
            }
            if (scopeParentPair != null)
            {
                final Scriptable parentScope = scopeParentPair.getFirst().get();
                if (parentScope != null)
                {
                    loggerData = this.getLoggerData(parentScope, script, createIfNull);
                }
            }
        }
        return loggerData;
    }

    protected void log(final int methodId, final Collection<Logger> loggers, final Scriptable scriptLogger, final String message)
    {
        for (final Logger logger : loggers)
        {
            switch (methodId)
            {
            case TRACE_FUNC_ID:
                logger.trace(message);
                break;
            case DEBUG_FUNC_ID:
                logger.debug(message);
                break;
            case INFO_FUNC_ID:
                logger.info(message);
                break;
            case WARN_FUNC_ID:
                logger.warn(message);
                break;
            case ERROR_FUNC_ID:
                logger.error(message);
                break;
            default:
                throw new IllegalArgumentException("Unsupported method ID");
            }
        }

        if (scriptLogger != null)
        {
            this.log(methodId, scriptLogger, message);
        }
    }

    protected void log(final int methodId, final Collection<Logger> loggers, final Scriptable scriptLogger, final String message,
            final Throwable ex)
    {
        for (final Logger logger : loggers)
        {
            switch (methodId)
            {
            case TRACE_FUNC_ID:
                logger.trace(message, ex);
                break;
            case DEBUG_FUNC_ID:
                logger.debug(message, ex);
                break;
            case INFO_FUNC_ID:
                logger.info(message, ex);
                break;
            case WARN_FUNC_ID:
                logger.warn(message, ex);
                break;
            case ERROR_FUNC_ID:
                logger.error(message, ex);
                break;
            default:
                throw new IllegalArgumentException("Unsupported method ID");
            }
        }

        if (scriptLogger != null)
        {
            this.log(methodId, scriptLogger, message + "\n" + ex.toString());
        }
    }

    protected void log(final int methodId, final Collection<Logger> loggers, final Scriptable scriptLogger, final String message,
            final Object... params)
    {
        for (final Logger logger : loggers)
        {
            switch (methodId)
            {
            case TRACE_FUNC_ID:
                logger.trace(message, params);
                break;
            case DEBUG_FUNC_ID:
                logger.debug(message, params);
                break;
            case INFO_FUNC_ID:
                logger.info(message, params);
                break;
            case WARN_FUNC_ID:
                logger.warn(message, params);
                break;
            case ERROR_FUNC_ID:
                logger.error(message, params);
                break;
            default:
                throw new IllegalArgumentException("Unsupported method ID");
            }
        }

        if (scriptLogger != null)
        {
            final String formattedMessage = MessageFormatter.arrayFormat(message, params);
            this.log(methodId, scriptLogger, formattedMessage);
        }
    }

    protected void log(final int methodId, final Scriptable scriptLogger, final String message)
    {
        final String methodName;
        switch (methodId)
        {
        case DEBUG_FUNC_ID:
            methodName = "debug";
            break;
        case INFO_FUNC_ID:
            methodName = "info";
            break;
        case WARN_FUNC_ID:
            methodName = "warn";
            break;
        case ERROR_FUNC_ID:
            methodName = "error";
            break;
        default:
            // methodId not supported for directing logging to ScriptLogger
            return;
        }

        if (ScriptableObject.hasProperty(scriptLogger, methodName))
        {
            ScriptableObject.callMethod(scriptLogger, methodName, new Object[] { message });
        }
    }

    protected static class LoggerData
    {
        private Collection<Logger> loggers;
        private String explicitLogger;
        private boolean inheritLoggerContext = true;
        private Scriptable scriptLogger;

        public LoggerData()
        {
            // NO-OP
        }

        /**
         * @return the loggers
         */
        public final Collection<Logger> getLoggers()
        {
            return this.loggers;
        }

        /**
         * @param loggers
         *            the loggers to set
         */
        public final void setLoggers(final Collection<Logger> loggers)
        {
            this.loggers = loggers;
        }

        /**
         * @return the explicitLogger
         */
        public final String getExplicitLogger()
        {
            return this.explicitLogger;
        }

        /**
         * @param explicitLogger
         *            the explicitLogger to set
         */
        public final void setExplicitLogger(final String explicitLogger)
        {
            this.explicitLogger = explicitLogger;
        }

        /**
         * @return the inheritLoggerContext
         */
        public final boolean isInheritLoggerContext()
        {
            return this.inheritLoggerContext;
        }

        /**
         * @param inheritLoggerContext
         *            the inheritLoggerContext to set
         */
        public final void setInheritLoggerContext(final boolean inheritLoggerContext)
        {
            this.inheritLoggerContext = inheritLoggerContext;
        }

        /**
         * @return the scriptLogger
         */
        public final Scriptable getScriptLogger()
        {
            return this.scriptLogger;
        }

        /**
         * @param scriptLogger
         *            the legacyLogger to set
         */
        public final void setScriptLogger(final Scriptable scriptLogger)
        {
            this.scriptLogger = scriptLogger;
        }

    }
}
