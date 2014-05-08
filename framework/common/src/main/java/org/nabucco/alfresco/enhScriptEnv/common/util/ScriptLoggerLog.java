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
package org.nabucco.alfresco.enhScriptEnv.common.util;

import org.apache.commons.logging.Log;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.nabucco.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import org.nabucco.alfresco.enhScriptEnv.common.script.functions.RhinoLogFunction;
import org.springframework.extensions.webscripts.ScriptLogger;

/**
 * {@link Log} implementation wrapping a Rhino-based script logger without being tied to either {@code ScriptLogger} or {@code LogFunction}
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ScriptLoggerLog implements Log
{

    protected static enum Level
    {
        /** Trace level in {@link LogFunction} only */
        TRACE("trace", "isTraceEnabled"),
        /** Debug level in {@link ScriptLogger} and {@link LogFunction} */
        DEBUG("debug", "isDebugLoggingEnabled"),
        /** Info level in {@link ScriptLogger} and {@link LogFunction} */
        INFO("info", "isInfoLoggingEnabled"),
        /** Warn level in {@link ScriptLogger} and {@link LogFunction} */
        WARN("warn", "isWarnLoggingEnabled"),
        /** Error level in {@link ScriptLogger} and {@link LogFunction} */
        ERROR("error", "isErrorLoggingEnabled");

        private final String logMethod;
        private final String enablementCheckMethod;

        private Level(final String logMethod, final String enablementCheckMethod)
        {
            this.logMethod = logMethod;
            this.enablementCheckMethod = enablementCheckMethod;
        }

        public String getLogMethod()
        {
            return this.logMethod;
        }

        public String getEnablementCheckMethod()
        {
            return this.enablementCheckMethod;
        }
    }

    private final Scriptable scriptLogger;

    private final EnhancedScriptProcessor scriptProcessor;

    private final Context parentContext = Context.getCurrentContext();

    public ScriptLoggerLog(final Scriptable scriptLogger, final EnhancedScriptProcessor scriptProcessor)
    {
        this.scriptLogger = scriptLogger;
        this.scriptProcessor = scriptProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDebugEnabled()
    {
        return this.checkLevelEnabled(Level.DEBUG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isErrorEnabled()
    {
        return this.checkLevelEnabled(Level.ERROR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFatalEnabled()
    {
        // there is nothing higher
        return this.checkLevelEnabled(Level.ERROR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInfoEnabled()
    {
        return this.checkLevelEnabled(Level.INFO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTraceEnabled()
    {
        return this.checkLevelEnabled(Level.TRACE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWarnEnabled()
    {
        return this.checkLevelEnabled(Level.WARN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(final Object message)
    {
        this.doLog(Level.TRACE, String.valueOf(message));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void trace(final Object message, final Throwable t)
    {
        if (message != null)
        {
            this.doLog(Level.TRACE, String.valueOf(message), t);
        }
        else
        {
            this.doLog(Level.TRACE, t != null ? t.getMessage() : null, t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(final Object message)
    {
        this.doLog(Level.DEBUG, String.valueOf(message));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debug(final Object message, final Throwable t)
    {
        if (message != null)
        {
            this.doLog(Level.DEBUG, String.valueOf(message), t);
        }
        else
        {
            this.doLog(Level.DEBUG, t != null ? t.getMessage() : null, t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void info(final Object message)
    {
        this.doLog(Level.INFO, String.valueOf(message));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void info(final Object message, final Throwable t)
    {
        if (message != null)
        {
            this.doLog(Level.INFO, String.valueOf(message), t);
        }
        else
        {
            this.doLog(Level.INFO, t != null ? t.getMessage() : null, t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warn(final Object message)
    {
        this.doLog(Level.WARN, String.valueOf(message));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void warn(final Object message, final Throwable t)
    {
        if (message != null)
        {
            this.doLog(Level.WARN, String.valueOf(message), t);
        }
        else
        {
            this.doLog(Level.WARN, t != null ? t.getMessage() : null, t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final Object message)
    {
        this.doLog(Level.ERROR, String.valueOf(message));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void error(final Object message, final Throwable t)
    {
        if (message != null)
        {
            this.doLog(Level.ERROR, String.valueOf(message), t);
        }
        else
        {
            this.doLog(Level.ERROR, t != null ? t.getMessage() : null, t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fatal(final Object message)
    {
        this.doLog(Level.ERROR, String.valueOf(message));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fatal(final Object message, final Throwable t)
    {
        if (message != null)
        {
            this.doLog(Level.ERROR, String.valueOf(message), t);
        }
        else
        {
            this.doLog(Level.ERROR, t != null ? t.getMessage() : null, t);
        }
    }

    protected void doLog(final Level level, final String message)
    {
        final Context currentContext = Context.getCurrentContext();
        if (currentContext == null)
        {
            Context.enter();
        }
        try
        {
            if (currentContext == null)
            {
                this.scriptProcessor.inheritCallChain(this.parentContext);
            }

            if (ScriptableObject.hasProperty(this.scriptLogger, level.getLogMethod()))
            {
                ScriptableObject.callMethod(this.scriptLogger, level.getLogMethod(), new Object[] { message });
            }
        }
        finally
        {
            if (currentContext == null)
            {
                Context.exit();
            }
        }
    }

    protected void doLog(final Level level, final String message, final Throwable t)
    {
        final Context currentContext = Context.getCurrentContext();
        if (currentContext == null)
        {
            Context.enter();
        }
        try
        {
            if (currentContext == null)
            {
                this.scriptProcessor.inheritCallChain(this.parentContext);
            }

            if (ScriptableObject.hasProperty(this.scriptLogger, level.getLogMethod()))
            {
                ScriptableObject.callMethod(this.scriptLogger, level.getLogMethod(), new Object[] { message, t });
            }
        }
        finally
        {
            if (currentContext == null)
            {
                Context.exit();
            }
        }
    }

    protected boolean checkLevelEnabled(final Level level)
    {
        final Context currentContext = Context.getCurrentContext();
        if (currentContext == null)
        {
            Context.enter();
        }
        try
        {
            if (currentContext == null)
            {
                this.scriptProcessor.inheritCallChain(this.parentContext);
            }

            final Boolean isEnabled;

            if (ScriptableObject.hasProperty(this.scriptLogger, level.getEnablementCheckMethod()))
            {
                final Object result = ScriptableObject.callMethod(this.scriptLogger, level.getEnablementCheckMethod(), new Object[0]);
                isEnabled = (Boolean) Context.jsToJava(result, Boolean.class);
            }
            else
            {
                isEnabled = Boolean.FALSE;
            }

            return Boolean.TRUE.equals(isEnabled);
        }
        finally
        {
            if (currentContext == null)
            {
                Context.exit();
            }
        }
    }

}
