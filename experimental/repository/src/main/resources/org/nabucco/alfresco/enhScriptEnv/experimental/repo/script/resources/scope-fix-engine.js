// minor fix to context attribute lookup (and logging)
// nested function of Nashorn engine.js looks up context from original global, and engine looks up actual global from context
// Nashorn is weird in that "global" is not really an object but a fallback for this === undefined
Object.defineProperty(this, "__noSuchProperty__",
{
    configurable : true,
    enumerable : false,
    writable : true,
    value : function __noSuchProperty__(name)
    {
        'use strict';
        var loggerFactory, realLogger, e, scriptStackFrames;
        try
        {
            return engine.__noSuchProperty__(this, context, name);
        }
        catch (e)
        {
            if (e instanceof ReferenceError)
            {
                // need the script stack frames to manually offset this function when logging the script file name / line number
                scriptStackFrames = e.getStackTrace();

                try
                {
                    realLogger = logger;
                }
                catch(e)
                {
                    // ignore - can't really check hasOwnProperty("logger") on "global" due to this === undefined for "global"
                }
                
                if (realLogger === undefined || realLogger === null)
                {
                    // Java should still be available when ScriptLogger / LogFunction is not available
                    loggerFactory = Java.type("org.slf4j.LoggerFactory");
                    realLogger = loggerFactory.getLogger("org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornScriptProcessor");

                    // log to SLF4J as part of the script processor output
                    realLogger.warn(
                            "No property named {} defined in global or context attributes - script {} caused reference error at line {}", [
                                    name, scriptStackFrames[1].getFileName(), scriptStackFrames[1].getLineNumber() ]);
                }
                else
                {
                    realLogger.warn('No property named ' + name + ' defined in global or context attributes - script '
                            + scriptStackFrames[1].getFileName() + ' caused reference error at line '
                            + scriptStackFrames[1].getLineNumber());
                }
            }
            throw e;
        }
    }
});