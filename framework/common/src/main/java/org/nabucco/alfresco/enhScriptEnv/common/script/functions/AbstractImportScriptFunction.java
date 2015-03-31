/*
 * Copyright 2015 PRODYNA AG
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

import java.util.Map;

import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.ScopeContributor;
import org.nabucco.alfresco.enhScriptEnv.common.script.ScriptImportException;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.locator.ScriptLocator;
import org.nabucco.alfresco.enhScriptEnv.common.script.locator.ScriptLocatorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Abstract import function to allow inclusion of other scripts in arbitrary contexts of a script in execution. This function utilizes the
 * central registry of script locators supplied by the {@link EnhancedScriptProcessor enhanced script processor} as well as its compilation
 * / caching framework for constituent scripts.
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class AbstractImportScriptFunction<Script extends ReferenceScript, S> implements ScopeContributor, InitializingBean
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractImportScriptFunction.class);

    protected EnhancedScriptProcessor<Script> scriptProcessor;
    protected ValueConverter valueConverter;
    protected ScriptLocatorRegistry<Script> locatorRegistry;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "scriptProcessor", this.scriptProcessor);
        PropertyCheck.mandatory(this, "valueConverter", this.valueConverter);
        PropertyCheck.mandatory(this, "locatorRegistry", this.locatorRegistry);

        this.scriptProcessor.registerScopeContributor(this);
    }

    /**
     * Sets the scriptProcessor to given scriptProcessor.
     *
     * @param scriptProcessor
     *            the scriptProcessor to set
     */
    public final void setScriptProcessor(final EnhancedScriptProcessor<Script> scriptProcessor)
    {
        this.scriptProcessor = scriptProcessor;
    }

    /**
     * Sets the valueConverter to given valueConverter.
     *
     * @param valueConverter
     *            the valueConverter to set
     */
    public final void setValueConverter(final ValueConverter valueConverter)
    {
        this.valueConverter = valueConverter;
    }

    /**
     * @param locatorRegistry
     *            the locatorRegistry to set
     */
    public final void setLocatorRegistry(final ScriptLocatorRegistry<Script> locatorRegistry)
    {
        this.locatorRegistry = locatorRegistry;
    }

    protected boolean resolveAndImport(final String locatorType, final String locationValue, final Object resolutionParams,
            final S scope, final Object executionScopeParams, final boolean failOnMissingScript)
    {
        ParameterCheck.mandatoryString("locatorType", locatorType);
        ParameterCheck.mandatoryString("locationValue", locationValue);

        final boolean result;
        final ReferenceScript referenceLocation = this.scriptProcessor.getContextScriptLocation();
        final ScriptLocator<Script> scriptLocator = this.locatorRegistry.getLocator(locatorType);
        if (scriptLocator != null)
        {
            final Script location = this.resolveLocationImpl(referenceLocation, locationValue, resolutionParams, scriptLocator);

            if (location == null)
            {
                LOGGER.info("Unable to resolve script location [{}] via locator [{}]", locationValue, locatorType);

                if (failOnMissingScript)
                {
                    // TODO: proper msgId
                    throw new ScriptImportException("Unable to resolve script location [{0}] via locator [{1}]", new Object[] {
                            locationValue, locatorType });
                }
                result = false;
            }
            else
            {
                this.importAndExecute(location, scope, executionScopeParams);
                result = true;
            }
        }
        else
        {
            LOGGER.warn("Unknown script locator [{}]", locatorType);

            if (failOnMissingScript)
            {
                // TODO: proper msgId
                throw new ScriptImportException("Unknown script locator [{0}]", new Object[] { locatorType });
            }
            result = false;
        }

        return result;
    }

    protected abstract S prepareExecutionScope(Script location, S sourceScope, Object executionScopeParam);

    protected void importAndExecute(final Script location, final S scope, final Object executionScopeParam)
    {
        final S executionScope;
        if (executionScopeParam != null)
        {
            executionScope = this.prepareExecutionScope(location, scope, executionScopeParam);
        }
        else
        {
            // Note: Insecure scripts called without proper isolation (through passed executionScopeParam) will inherit the secure scope and
            // potentially sensitive API. It is the responsibility of any developer that uses import to consider proper isolation.
            executionScope = scope;
        }

        this.scriptProcessor.executeInScope(location, executionScope);
    }

    protected Script resolveLocationImpl(final ReferenceScript referenceLocation, final String locationValue,
            final Object resolutionParams, final ScriptLocator<Script> scriptLocator)
    {
        final Script location;
        if (resolutionParams == null)
        {
            location = scriptLocator.resolveLocation(referenceLocation, locationValue);
        }
        else if (resolutionParams instanceof Map<?, ?>)
        {
            // we know the generic parameters from the way this.valueConverter works
            @SuppressWarnings("unchecked")
            final Map<String, Object> resolotionParamsJavaMap = (Map<String, Object>) resolutionParams;
            location = scriptLocator.resolveLocation(referenceLocation, locationValue, resolotionParamsJavaMap);
        }
        else
        {
            LOGGER.warn(
                    "Invalid parameter object for resolution of script location [{}] via locator [{}] - should have been a string-keyed map: [{}]",
                    new Object[] { locationValue, scriptLocator, resolutionParams });
            location = null;
        }
        return location;
    }

}