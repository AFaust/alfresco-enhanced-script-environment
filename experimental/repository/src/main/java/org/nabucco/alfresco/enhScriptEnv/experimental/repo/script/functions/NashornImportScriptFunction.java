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

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import jdk.nashorn.internal.runtime.ScriptObject;

import org.alfresco.scripts.ScriptException;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.ScriptImportException;
import org.nabucco.alfresco.enhScriptEnv.common.script.functions.AbstractImportScriptFunction;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornScriptProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
// need to work with internal API to prepare the scope
public class NashornImportScriptFunction<Script extends ReferenceScript> extends AbstractImportScriptFunction<Script>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(NashornImportScriptFunction.class);

    protected final ScriptEngine engine = new ScriptEngineManager().getEngineByName(NashornScriptProcessor.NASHORN_ENGINE_NAME);

    protected Object prepareExecutionScope(final Script location, final Object sourceScope, final Object executionScopeParam)
    {
        final Object result;
        if (executionScopeParam instanceof ScriptObject)
        {
            LOGGER.debug("Enhancing provided execution scope {}", executionScopeParam);
            final ScriptObject scope = (ScriptObject) executionScopeParam;
            final Bindings executionScope;
            final Object preparedExecutionScope = this.scriptProcessor.initializeScope(location);
            if (!(preparedExecutionScope instanceof Bindings))
            {
                LOGGER.error("Scope {} initialized by script processor is not Bindings-compatible", preparedExecutionScope);
                throw new ScriptImportException("The script processor provided an incomatible scope for the script {}", location);
            }

            // TODO: protect against complex objects being passed as a context - fail when scope already has prototype?

            final Bindings engineGlobal = (Bindings) preparedExecutionScope;
            final ScriptContext ctxt = new SimpleScriptContext();
            ctxt.setBindings(engineGlobal, ScriptContext.ENGINE_SCOPE);

            final Bindings globalGlobal = new SimpleBindings();
            globalGlobal.put("scope", scope);
            ctxt.setBindings(globalGlobal, ScriptContext.GLOBAL_SCOPE);

            try
            {
                final Object scriptResult = this.engine.eval(
                        "(function (obj, proto) { obj.prototype = proto; return obj; }(scope, this));", ctxt);
                if (scriptResult instanceof Bindings)
                {
                    executionScope = (Bindings) scriptResult;
                    executionScope.put("nashorn.global", engineGlobal);
                }
                else
                {
                    LOGGER.error("Nashorn engine did not wrap return value {} into Bindings-compatible object as expected", scriptResult);
                    throw new ScriptImportException("The scope initialization script resulted in an incomatible scope for the script {}",
                            location);
                }

                result = executionScope;
            }
            catch (final javax.script.ScriptException ex)
            {
                throw new ScriptImportException("Failed to prepare execution scope", ex);
            }
        }
        else
        {
            LOGGER.debug("No execution scope provided - using current scope {}", sourceScope);
            result = sourceScope;
        }

        return result;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void contributeToScope(final Object scope, final boolean trustworthyScript, final boolean mutableScope)
    {
        if (scope instanceof Bindings)
        {
            final Bindings global = (Bindings) scope;
            global.put(NashornImportScriptFunction.class.getSimpleName(), this);
            try
            {
                final InputStream is = NashornScriptProcessor.class.getResource("resources/contribute-importScript.js").openStream();
                try (final Reader isReader = new InputStreamReader(is))
                {
                    final ScriptContext ctx = new SimpleScriptContext();
                    ctx.setBindings(global, ScriptContext.ENGINE_SCOPE);
                    this.engine.eval(isReader, ctx);
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

    /**
     * Imports a dynamically resolved script.
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
            final Object resolutionParams, final Object currentScope, final Object executionScope)
    {
        final boolean result;

        final Object resolutionParamsObj = this.valueConverter.convertValueForJava(resolutionParams);

        result = this.resolveAndImport(locatorType, locationValue, resolutionParamsObj, currentScope, executionScope, failOnMissingScript);

        return result;
    }
}