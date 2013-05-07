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

import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;

import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript.ReferencePathType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.ScriptValueConverter;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class LogFunction implements IdFunctionCall, InitializingBean, ScopeContributor
{

    public static final String LOGGER_OBJ_NAME = "logger";

    public static final int TRACE_FUNC_ID = 100;
    public static final int DEBUG_FUNC_ID = 101;
    public static final int INFO_FUNC_ID = 102;
    public static final int WARN_FUNC_ID = 103;
    public static final int ERROR_FUNC_ID = 104;

    public static final Object LOG_FUNC_TAG = new Object();

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

    protected EnhancedScriptProcessor<?> scriptProcessor;

    protected String defaultLoggerPrefix;

    // optional
    protected String legacyLoggerName;

    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "scriptProcessor", this.scriptProcessor);
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
                final Collection<Logger> loggers = this.getLoggers(cx, scope, thisObj);

                if (args.length == 1)
                {
                    final String message = ScriptRuntime.toString(args, 0);
                    log(methodId, loggers, message);
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
                        log(methodId, loggers, message, ex);
                    }
                    else
                    {
                        final Object[] params = new Object[args.length - 1];
                        params[0] = ScriptValueConverter.unwrapValue(secondParam);
                        for (int argsIdx = 2, idx = 1; argsIdx < args.length && idx < params.length; argsIdx++, idx++)
                        {
                            params[idx] = ScriptValueConverter.unwrapValue(args[argsIdx]);
                        }
                        log(methodId, loggers, message, params);
                    }
                }
                else
                {
                    throw new IllegalArgumentException("Parameter message is missing");
                }

                result = null;
            }
            else if (methodId >= TRACE_ENABLED_FUNC_ID && methodId <= ERROR_ENABLED_FUNC_ID)
            {
                final Collection<Logger> loggers = getLoggers(cx, scope, thisObj);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void contributeToScope(final Scriptable scope, final boolean trustworthyScript, final boolean mutableScope)
    {
        final NativeObject loggerObj = new NativeObject();

        exportFunction(DEBUG_FUNC_ID, DEBUG_FUNC_NAME, 1, loggerObj);
        exportFunction(DEBUG_FUNC_ID, LOG_FUNC_NAME, 1, loggerObj);
        exportFunction(TRACE_FUNC_ID, TRACE_FUNC_NAME, 1, loggerObj);
        exportFunction(INFO_FUNC_ID, INFO_FUNC_NAME, 1, loggerObj);
        exportFunction(WARN_FUNC_ID, WARN_FUNC_NAME, 1, loggerObj);
        exportFunction(ERROR_FUNC_ID, ERROR_FUNC_NAME, 1, loggerObj);

        exportFunction(DEBUG_ENABLED_FUNC_ID, DEBUG_ENABLED_FUNC_NAME, 0, loggerObj);
        exportFunction(DEBUG_ENABLED_FUNC_ID, DEBUG_LOGGING_ENABLED_FUNC_NAME, 0, loggerObj);
        exportFunction(DEBUG_ENABLED_FUNC_ID, LOGGING_ENABLED_FUNC_NAME, 0, loggerObj);

        exportFunction(TRACE_ENABLED_FUNC_ID, TRACE_ENABLED_FUNC_NAME, 0, loggerObj);

        exportFunction(DEBUG_ENABLED_FUNC_ID, INFO_ENABLED_FUNC_NAME, 0, loggerObj);
        exportFunction(DEBUG_ENABLED_FUNC_ID, INFO_LOGGING_ENABLED_FUNC_NAME, 0, loggerObj);

        exportFunction(WARN_ENABLED_FUNC_ID, WARN_ENABLED_FUNC_NAME, 0, loggerObj);
        exportFunction(WARN_ENABLED_FUNC_ID, WARN_LOGGING_ENABLED_FUNC_NAME, 0, loggerObj);

        exportFunction(ERROR_ENABLED_FUNC_ID, ERROR_ENABLED_FUNC_NAME, 0, loggerObj);
        exportFunction(ERROR_ENABLED_FUNC_ID, ERROR_LOGGING_ENABLED_FUNC_NAME, 0, loggerObj);

        loggerObj.sealObject();
        ScriptableObject.defineProperty(scope, LOGGER_OBJ_NAME, loggerObj, ScriptableObject.DONTENUM | ScriptableObject.PERMANENT
                | ScriptableObject.READONLY);
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
    public final void setScriptProcessor(EnhancedScriptProcessor scriptProcessor)
    {
        this.scriptProcessor = scriptProcessor;
    }

    protected void exportFunction(final int methodId, final String name, final int arity, final Scriptable scope)
    {
        final IdFunctionObject func = new IdFunctionObject(this, LOG_FUNC_TAG, methodId, name, arity, scope);
        func.sealObject();
        ScriptableObject.defineProperty(scope, name, func, ScriptableObject.DONTENUM | ScriptableObject.PERMANENT
                | ScriptableObject.READONLY);
    }

    protected Collection<Logger> getLoggers(final Context context, final Scriptable scope, final Scriptable thisObj)
    {
        // TODO: cache for reduced impact
        final Collection<Logger> loggers = new HashSet<Logger>();

        if (this.legacyLoggerName != null)
        {
            loggers.add(LoggerFactory.getLogger(this.legacyLoggerName));
        }

        final ReferenceScript contextScriptLocation = this.scriptProcessor.getContextScriptLocation();
        if (contextScriptLocation != null)
        {
            final Collection<ReferencePathType> supportedReferencePathTypes = contextScriptLocation.getSupportedReferencePathTypes();
            for (final ReferencePathType referencePathType : supportedReferencePathTypes)
            {
                final String referencePath = contextScriptLocation.getReferencePath(referencePathType);
                if (referencePath != null)
                {
                    final String loggerSuffix = referencePath.replace('.', '_').replace('/', '.');
                    final String loggerName = MessageFormat
                            .format("{0}.{1}.{2}", this.defaultLoggerPrefix, referencePathType, loggerSuffix);
                    loggers.add(LoggerFactory.getLogger(loggerName));
                }
            }
        }

        return loggers;
    }

    protected void log(final int methodId, final Collection<Logger> loggers, final String message)
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
    }

    protected void log(final int methodId, final Collection<Logger> loggers, final String message, final Throwable ex)
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
    }

    protected void log(final int methodId, final Collection<Logger> loggers, final String message, final Object... params)
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
    }
}
