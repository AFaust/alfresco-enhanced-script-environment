(function(logger)
{
    var logScope = this, Throwable = Java.type("java.lang.Throwable"), MessageFormat = Java.type("java.text.MessageFormat"), loggerDelegate = logger, rootLogger = new Object(), system = new Object(), existingLoggerProp, createLogFn = function(
            level, name)
    {
        var logLevel = level, fnName = name, fn = function(message, opt)
        {
            var params, idx, max, scriptLogger = loggerDelegate.getScriptLogger();
            if (opt !== undefined && opt !== null)
            {
                if (opt instanceof Throwable)
                {
                    loggerDelegate.log(logScope, logLevel, message, opt);

                    if (scriptLogger !== null && scriptLogger.hasOwnProperty(fnName) && typeof scriptLogger[fnName] === "function")
                    {
                        scriptLogger[fnName].call(scriptLogger, message + "\n" + opt.toString());
                    }
                }
                else if (arguments.length > 2)
                {
                    // varargs-like handling
                    params = [];
                    for (idx = 1, max = arguments.length; idx < max; idx++)
                    {
                        params[idx - 1] = arguments[idx];
                    }
                    loggerDelegate.log(logScope, logLevel, message, params);

                    if (scriptLogger !== null && scriptLogger.hasOwnProperty(fnName) && typeof scriptLogger[fnName] === "function")
                    {
                        scriptLogger[fnName].call(scriptLogger, MessageFormat.format(message, params));
                    }
                }
                else
                {
                    loggerDelegate.log(logScope, logLevel, message, opt);

                    if (scriptLogger !== null && scriptLogger.hasOwnProperty(fnName) && typeof scriptLogger[fnName] === "function")
                    {
                        scriptLogger[fnName].call(scriptLogger, MessageFormat.format(message, opt));
                    }
                }
            }
            else
            {
                loggerDelegate.log(logScope, logLevel, message);

                if (scriptLogger !== null && scriptLogger.hasOwnProperty(fnName) && typeof scriptLogger[fnName] === "function")
                {
                    scriptLogger[fnName].call(scriptLogger);
                }
            }
        };

        return fn;
    }, createLogEnabledFn = function(level)
    {
        var logLevel = level, fn = function()
        {
            return loggerDelegate.isEnabled(logScope, logLevel);
        };

        return fn;
    };

    // might have been defined already
    existingLoggerProp = Object.getOwnPropertyDescriptor(this, "logger");
    if (existingLoggerProp === undefined || existingLoggerProp.configurable === true)
    {

        Object.defineProperty(system, "out",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : function(message)
            {
                loggerDelegate.out(message);
            }
        });

        Object.freeze(system);

        Object.defineProperty(rootLogger, "getSystem",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : function()
            {
                return system;
            }
        });

        Object.defineProperty(rootLogger, "registerChildScope",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : function(childScope)
            {
                loggerDelegate.registerChildScope(logScope, childScope);
            }
        });

        Object.defineProperty(rootLogger, "setLogger",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : function(logger)
            {
                loggerDelegate.setLogger(logScope, logger);
            }
        });

        Object.defineProperty(rootLogger, "setInheritLoggerContext",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : function(inheritLoggerContext)
            {
                loggerDelegate.setInheritLoggerContext(logScope, inheritLoggerContext);
            }
        });

        Object.defineProperty(rootLogger, "trace",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : createLogFn("trace", "trace")
        });

        Object.defineProperty(rootLogger, "debug",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : createLogFn("debug", "log")
        });

        Object.defineProperty(rootLogger, "log",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : createLogFn("debug", "log")
        });

        Object.defineProperty(rootLogger, "info",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : createLogFn("info", "info")
        });

        Object.defineProperty(rootLogger, "warn",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : createLogFn("warn", "warn")
        });

        Object.defineProperty(rootLogger, "error",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : createLogFn("error", "error")
        });

        Object.defineProperty(rootLogger, "isTraceEnabled",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : createLogEnabledFn("trace")
        });

        Object.defineProperty(rootLogger, "isDebugEnabled",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : createLogEnabledFn("debug")
        });

        Object.defineProperty(rootLogger, "isDebugLoggingEnabled",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : rootLogger.isDebugEnabled
        });

        Object.defineProperty(rootLogger, "isLoggingEnabled",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : rootLogger.isDebugEnabled
        });

        Object.defineProperty(rootLogger, "isInfoEnabled",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : createLogEnabledFn("info")
        });

        Object.defineProperty(rootLogger, "isInfoLoggingEnabled",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : rootLogger.isInfoEnabled
        });

        Object.defineProperty(rootLogger, "isWarnEnabled",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : createLogEnabledFn("warn")
        });

        Object.defineProperty(rootLogger, "isWarnLoggingEnabled",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : rootLogger.isWarnEnabled
        });

        Object.defineProperty(rootLogger, "isErrorEnabled",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : createLogEnabledFn("error")
        });

        Object.defineProperty(rootLogger, "isErrorLoggingEnabled",
        {
            configurable : false,
            enumerable : true,
            writable : false,
            value : rootLogger.isErrorEnabled
        });

        Object.freeze(rootLogger);

        Object.defineProperty(this, "logger",
        {
            configurable : false,
            enumerable : false,
            get : function()
            {
                return rootLogger;
            },
            set : function(logger)
            {
                if (logger !== rootLogger)
                {
                    loggerDelegate.setScriptLogger(logger);
                }
            }
        });
    }
}(NashornLogFunction));