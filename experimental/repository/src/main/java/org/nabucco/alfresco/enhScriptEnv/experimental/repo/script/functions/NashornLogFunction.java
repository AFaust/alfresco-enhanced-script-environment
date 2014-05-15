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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.functions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;

import org.alfresco.repo.jscript.ScriptLogger;
import org.alfresco.scripts.ScriptException;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.functions.AbstractLogFunction;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornScriptProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class NashornLogFunction extends AbstractLogFunction
{
    private static final String NASHORN_ENGINE_NAME = "nashorn";
    private static final Logger LOGGER = LoggerFactory.getLogger(NashornLogFunction.class);

    protected ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(NASHORN_ENGINE_NAME);

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        super.afterPropertiesSet();
        PropertyCheck.mandatory(this, "scriptEngine", this.scriptEngine);
    }

    /**
     * @param scriptEngine
     *            the scriptEngine to set
     */
    public final void setScriptEngine(final ScriptEngine scriptEngine)
    {
        this.scriptEngine = scriptEngine;
    }

    public Object getScriptLogger()
    {
        final Object scriptLogger = super.getScriptLogger();
        return scriptLogger;
    }

    public void setScriptLogger(final Object scriptLogger)
    {
        this.handleSetScriptLogger(scriptLogger);
    }

    public void registerChildScope(final Object parentScope, final Object childScope)
    {
        this.handleRegisterChildScope(parentScope, childScope);
    }

    public void setLogger(final Object scope, final String logger)
    {
        this.handleSetLogger(scope, logger);
    }

    public void setInheritLoggerContext(final Object scope, final boolean inheritLoggerContext)
    {
        this.handleSetLoggerInheritance(scope, inheritLoggerContext);
    }

    public void out(final String message)
    {
        this.handleOut(message);
    }

    public boolean isEnabled(final Object scope, final String level)
    {
        ParameterCheck.mandatory("scope", scope);
        ParameterCheck.mandatoryString("level", level);

        boolean enabled = false;
        final Collection<Logger> loggers = this.getLoggers(scope, true);
        for (final Logger logger : loggers)
        {
            switch (level)
            {
            case "error":
                enabled = logger.isErrorEnabled();
                break;
            case "warn":
                enabled = logger.isWarnEnabled();
                break;
            case "info":
                enabled = logger.isInfoEnabled();
                break;
            case "debug":
                enabled = logger.isDebugEnabled();
                break;
            case "trace":
                enabled = logger.isTraceEnabled();
                break;
            default:
                enabled = false;
            }

            if (enabled)
            {
                break;
            }
        }

        return enabled;
    }

    public void log(final Object scope, final String level, final String message)
    {
        ParameterCheck.mandatory("scope", scope);
        ParameterCheck.mandatoryString("level", level);

        final Collection<Logger> loggers = this.getLoggers(scope, true);
        for (final Logger logger : loggers)
        {
            switch (level)
            {
            case "error":
                logger.error(message);
                break;
            case "warn":
                logger.warn(message);
                break;
            case "info":
                logger.info(message);
                break;
            case "debug":
                logger.debug(message);
                break;
            case "trace":
                logger.trace(message);
                break;
            default:
                // NO-OP
            }
        }
    }

    public void log(final Object scope, final String level, final String message, final Throwable ex)
    {
        ParameterCheck.mandatory("scope", scope);
        ParameterCheck.mandatoryString("level", level);

        final Collection<Logger> loggers = this.getLoggers(scope, true);
        for (final Logger logger : loggers)
        {
            switch (level)
            {
            case "error":
                logger.error(message, ex);
                break;
            case "warn":
                logger.warn(message, ex);
                break;
            case "info":
                logger.info(message, ex);
                break;
            case "debug":
                logger.debug(message, ex);
                break;
            case "trace":
                logger.trace(message, ex);
                break;
            default:
                // NO-OP
            }
        }
    }

    public void log(final Object scope, final String level, final String message, final Object... params)
    {
        ParameterCheck.mandatory("scope", scope);
        ParameterCheck.mandatoryString("level", level);
        ParameterCheck.mandatory("params", params);

        final Object[] realParams = new Object[params.length];
        for (int idx = 0; idx < params.length; idx++)
        {
            realParams[idx] = this.valueConverter.convertValueForJava(params[idx]);
        }

        final Collection<Logger> loggers = this.getLoggers(scope, true);
        for (final Logger logger : loggers)
        {
            switch (level)
            {
            case "error":
                logger.error(message, realParams);
                break;
            case "warn":
                logger.warn(message, realParams);
                break;
            case "info":
                logger.info(message, realParams);
                break;
            case "debug":
                logger.debug(message, realParams);
                break;
            case "trace":
                logger.trace(message, realParams);
                break;
            default:
                // NO-OP
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contributeToScope(final Object scope, final boolean trustworthyScript, final boolean mutableScope)
    {
        if (scope instanceof Bindings)
        {
            final Bindings global = (Bindings) scope;
            global.put(NashornLogFunction.class.getSimpleName(), this);
            try
            {
                final InputStream is = NashornScriptProcessor.class.getResource("resources/contribute-logger.js").openStream();
                try (final Reader isReader = new InputStreamReader(is))
                {
                    final ScriptContext ctx = new SimpleScriptContext();
                    ctx.setBindings(global, ScriptContext.ENGINE_SCOPE);
                    this.scriptEngine.eval(isReader, ctx);
                }
            }
            catch (final IOException ex)
            {
                throw new ScriptException("Failed to contribute to scope", ex);
            }
            catch (final javax.script.ScriptException ex)
            {
                throw new ScriptException("Failed to contribute to scope", ex);
            }
            finally
            {
                global.remove(NashornImportScriptFunction.class.getSimpleName());
            }
        }
    }

    @Override
    protected Logger getLogger()
    {
        return LOGGER;
    }

    protected boolean isUnreplaceableScriptLogger(final Object scriptLogger)
    {
        final boolean result = !this.getDefaultScriptLoggerClass().isInstance(scriptLogger.getClass());

        return result;
    }

    protected Class<?> getDefaultScriptLoggerClass()
    {
        return ScriptLogger.class;
    }
}
