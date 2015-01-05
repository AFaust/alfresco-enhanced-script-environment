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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.functions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.alfresco.scripts.ScriptException;
import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.functions.AbstractImportScriptFunction;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornScriptProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
// need to work with internal API to prepare the scope
public class NashornImportScriptFunction<Script extends ReferenceScript> extends AbstractImportScriptFunction<Script, Object>
{

    private static final String NASHORN_ENGINE_NAME = "nashorn";
    private static final Logger LOGGER = LoggerFactory.getLogger(NashornImportScriptFunction.class);

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

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void contributeToScope(final Object scope, final boolean trustworthyScript, final boolean mutableScope)
    {
        if (scope instanceof ScriptContext)
        {
            final ScriptContext ctxt = (ScriptContext) scope;
            ctxt.setAttribute(ScriptEngine.FILENAME, "contribute-importScript.js", ScriptContext.ENGINE_SCOPE);
            ctxt.setAttribute(NashornImportScriptFunction.class.getSimpleName(), this, ScriptContext.GLOBAL_SCOPE);
            try
            {
                final InputStream is = NashornScriptProcessor.class.getResource("resources/contribute-importScript.js").openStream();
                try (final Reader isReader = new InputStreamReader(is))
                {
                    this.scriptEngine.eval(isReader, ctxt);
                }
            }
            catch (final IOException | javax.script.ScriptException ex)
            {
                throw new ScriptException("Failed to contribute to scope", ex);
            }
        }
    }

    /**
     * Imports a dynamically resolved script.
     *
     * Note: The types of {@code currentScope} and {@code executionScope} have been chosen as restrictions to the Nashorn linker.
     *
     * @param locatorType
     *            the locator type to use for resolution
     * @param locationValue
     *            the location value to resolve to a script
     * @param failOnMissingScript
     *            {@code true} if failure to resolve the script should cause a hard failure / execption
     * @param resolutionParams
     *            arbitrary resolution parameters
     * @param currentScope
     *            the current scope
     * @param executionScope
     *            the isolated scope for the imported script to be executed in - if any
     * @return {@code true} if the import succeeded, {@code false} otherwise
     */
    public boolean importScript(final String locatorType, final String locationValue, final boolean failOnMissingScript,
            final Object resolutionParams, final ScriptObjectMirror currentScope, final ScriptObjectMirror executionScope)
    {
        final boolean result;

        final Object resolutionParamsObj = this.valueConverter.convertValueForJava(resolutionParams);

        result = this.resolveAndImport(locatorType, locationValue, resolutionParamsObj, currentScope, executionScope, failOnMissingScript);

        return result;
    }

    protected void importAndExecute(final Script location, final Object sourceScope, final Object executionScopeParam)
    {
        final Object executionScope;
        if (executionScopeParam != null)
        {
            executionScope = this.prepareExecutionScope(location, sourceScope, executionScopeParam);
        }
        else
        {
            // Note: Insecure scripts called without proper isolation (through passed executionScopeParam) will inherit the secure scope and
            // potentially sensitive API. It is the responsibility of any developer that uses import to consider proper isolation.

            // use ScriptContext if possible
            if (sourceScope instanceof ScriptObjectMirror)
            {
                // use ScriptContext if possible
                if (((ScriptObjectMirror) sourceScope).containsKey("context"))
                {
                    final Object scriptContext = ((ScriptObjectMirror) sourceScope).get("context");
                    if (scriptContext instanceof ScriptContext)
                    {
                        executionScope = scriptContext;
                    }
                    else
                    {
                        executionScope = sourceScope;
                    }
                }
                else
                {
                    executionScope = sourceScope;
                }
            }
            else
            {
                executionScope = sourceScope;
            }
        }

        this.scriptProcessor.executeInScope(location, executionScope);
    }

    @Override
    protected Object prepareExecutionScope(final Script location, final Object sourceScope, final Object executionScopeParam)
    {
        /*
         * NashornScriptProcessor already handles the necessary scope management - we just need to pass it as a compatible type, e.g.
         * Global, ScriptContext, ScriptObjectMirror or Map
         *
         * Due to generic type parameter, we've had to fall back on Object
         */
        final Object result;
        if (executionScopeParam instanceof ScriptObjectMirror)
        {
            LOGGER.debug("Wrapped native script object provided as execution scope: {}", executionScopeParam);
            result = executionScopeParam;
        }
        else
        {
            LOGGER.debug("No execution scope provided - using current scope {}", sourceScope);

            if (sourceScope instanceof ScriptObjectMirror)
            {
                // use ScriptContext if possible
                if (((ScriptObjectMirror) sourceScope).containsKey("context"))
                {
                    final Object scriptContext = ((ScriptObjectMirror) sourceScope).get("context");
                    if (scriptContext instanceof ScriptContext)
                    {
                        result = scriptContext;
                    }
                    else
                    {
                        result = sourceScope;
                    }
                }
                else
                {
                    result = sourceScope;
                }
            }
            else
            {
                result = sourceScope;
            }
        }

        return result;
    }
}