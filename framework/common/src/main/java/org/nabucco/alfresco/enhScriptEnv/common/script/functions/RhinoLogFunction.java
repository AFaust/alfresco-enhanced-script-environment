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
package org.nabucco.alfresco.enhScriptEnv.common.script.functions;

import java.lang.reflect.Method;
import java.util.Collection;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrappedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.extensions.webscripts.ScriptLogger;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class RhinoLogFunction extends AbstractLogFunction implements IdFunctionCall
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

    private static final Logger LOGGER = LoggerFactory.getLogger(RhinoLogFunction.class);

    protected static String extractStringParameter(final Object[] args, final int expectedIndex)
    {
        final String string;
        if (args.length >= (expectedIndex + 1))
        {
            if (args[expectedIndex] == Undefined.instance || args[expectedIndex] == null)
            {
                string = null;
            }
            else
            {
                string = ScriptRuntime.toString(args, expectedIndex);
            }
        }
        else
        {
            string = null;
        }
        return string;
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

                result = Undefined.instance;
            }
            else if (methodId >= TRACE_ENABLED_FUNC_ID && methodId <= ERROR_ENABLED_FUNC_ID)
            {
                result = this.handleEnablementCheck(scope, thisObj, methodId);
            }
            else if (methodId == SET_LOGGER_FUNC_ID)
            {
                this.handleSetLogger(scope, args);

                result = Undefined.instance;
            }
            else if (methodId == SET_INHERIT_LOGGER_CTX_FUNC_ID)
            {
                this.handleSetLoggerInheritance(scope, args);

                result = Undefined.instance;
            }
            else if (methodId == REGISTER_CHILD_SCOPE_FUNC_ID)
            {
                this.handleRegisterChildScope(scope, args);

                result = Undefined.instance;
            }
            else if (methodId == GET_SYSTEM_FUNC_ID)
            {
                result = ScriptableObject.getProperty(thisObj, SYSTEM_PROP_NAME);
            }
            else if (methodId == OUT_FUNC_ID)
            {
                this.handleOut(args);

                result = Undefined.instance;
            }
            else if (methodId == SET_SCRIPT_LOGGER_FUNC_ID)
            {
                this.handleSetScriptLogger(scope, args);

                result = Undefined.instance;
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
    public void contributeToScope(final Object scope, final boolean trustworthyScript, final boolean mutableScope)
    {
        if (scope instanceof Scriptable)
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

            if (scope instanceof ScriptableObject)
            {
                // use a Java accessor with getter / setter as these don't require a top level call scope when being invoked e.g. as part of
                // model scope initialization
                final LoggerAccessor accessor = new LoggerAccessor((Scriptable) scope, loggerObj);
                try
                {
                    final Method getter = LoggerAccessor.class.getMethod("get", new Class[] { Scriptable.class });
                    final Method setter = LoggerAccessor.class.getMethod("set", new Class[] { Scriptable.class, Scriptable.class });
                    ((ScriptableObject) scope).defineProperty(LOGGER_OBJ_NAME, accessor, getter, setter, ScriptableObject.PERMANENT);
                }
                catch (final NoSuchMethodException ex)
                {
                    LOGGER.error("Can not register log function accessor due to unexpected exception", ex);
                }
            }
            else
            {
                // export as undeleteable property of the scope
                ScriptableObject.defineProperty((Scriptable) scope, LOGGER_OBJ_NAME, loggerObj, ScriptableObject.PERMANENT
                        | ScriptableObject.READONLY);
            }
        }
    }

    protected void handleRegisterChildScope(final Scriptable scope, final Object[] args)
    {
        final Object childScope;
        if (args.length >= 1)
        {
            if (args[0] == Undefined.instance || args[0] == null)
            {
                childScope = null;
            }
            else
            {
                childScope = args[0];
            }
        }
        else
        {
            childScope = null;
        }

        this.handleRegisterChildScope(scope, childScope);
    }

    protected void handleSetLoggerInheritance(final Scriptable scope, final Object[] args)
    {
        final boolean inheritLoggerContext = ScriptRuntime.toBoolean(args, 0);
        this.handleSetLoggerInheritance(scope, inheritLoggerContext);
    }

    protected void handleSetLogger(final Scriptable scope, final Object[] args)
    {
        final String explicitLoggerName = extractStringParameter(args, 0);
        this.handleSetLogger(scope, explicitLoggerName);
    }

    protected Object handleEnablementCheck(final Scriptable scope, final Scriptable thisObj, final int methodId)
    {
        final Object result;
        final Collection<Logger> loggers = this.getLoggers(scope, true);
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
        final Collection<Logger> loggers = this.getLoggers(scope, true);
        final Object scriptLogger = this.getScriptLogger();

        if (args.length == 0 || args[0] == Undefined.instance)
        {
            throw new IllegalArgumentException("No message provided");
        }

        if (args.length == 1)
        {
            final String message = ScriptRuntime.toString(args, 0);
            this.log(methodId, loggers, scriptLogger instanceof Scriptable ? (Scriptable) scriptLogger : null, message);
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
                this.log(methodId, loggers, scriptLogger instanceof Scriptable ? (Scriptable) scriptLogger : null, message, ex);
            }
            else
            {
                final Object[] params = new Object[args.length - 1];
                params[0] = this.valueConverter.convertValueForJava(secondParam);
                for (int argsIdx = 2, idx = 1; argsIdx < args.length && idx < params.length; argsIdx++, idx++)
                {
                    params[idx] = this.valueConverter.convertValueForJava(args[argsIdx]);
                }
                this.log(methodId, loggers, scriptLogger instanceof Scriptable ? (Scriptable) scriptLogger : null, message, params);
            }
        }
    }

    protected void handleOut(final Object[] args)
    {
        final String message = RhinoLogFunction.extractStringParameter(args, 0);

        this.handleOut(message);
    }

    protected void handleSetScriptLogger(final Scriptable scope, final Object[] args)
    {
        if (args.length >= 1)
        {
            final Scriptable scriptLogger = ScriptRuntime.toObject(scope, args[0]);
            final Object registeredScriptLogger = ScriptableObject.getProperty(scope, LOGGER_OBJ_NAME);
            if (registeredScriptLogger != scriptLogger)
            {
                super.handleSetScriptLogger(scriptLogger);
            }
        }
        else
        {
            throw new IllegalArgumentException("Parameter scriptLogger is missing");
        }
    }

    protected boolean isUnreplaceableScriptLogger(final Object scriptLogger)
    {
        final boolean result = !(scriptLogger instanceof NativeJavaObject)
                || !this.getDefaultScriptLoggerClass().isInstance(((NativeJavaObject) scriptLogger).unwrap().getClass());

        return result;
    }

    protected Class<?> getDefaultScriptLoggerClass()
    {
        return ScriptLogger.class;
    }

    protected void exportFunction(final int methodId, final String name, final int arity, final Scriptable scope)
    {
        final IdFunctionObject func = new IdFunctionObject(this, LOG_FUNC_TAG, methodId, name, arity, scope);
        func.sealObject();

        // export as read-only and undeleteable property of the scope
        ScriptableObject.defineProperty(scope, name, func, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
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

    protected class LoggerAccessor
    {
        private final Scriptable scope;
        private final NativeObject logger;

        protected LoggerAccessor(final Scriptable scope, final NativeObject logger)
        {
            this.scope = scope;
            this.logger = logger;
        }

        public Scriptable get(final Scriptable thisObj)
        {
            return this.logger;
        }

        public void set(final Scriptable thisObj, final Scriptable logger)
        {
            RhinoLogFunction.this.handleSetScriptLogger(this.scope, new Object[] { logger });
        }
    }
}
