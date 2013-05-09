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
package org.nabucco.alfresco.enhScriptEnv.common.script.locator;

import java.util.Map;

import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.registry.ScriptRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class RegisteredScriptLocator<BaseScript, Script extends ReferenceScript> extends AbstractScriptLocator<Script>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisteredScriptLocator.class);

    protected ScriptRegistry<BaseScript> scriptRegistry;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        super.afterPropertiesSet();
        PropertyCheck.mandatory(this, "scriptRegistry", this.scriptRegistry);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public final Script resolveLocation(final Script referenceLocation, final String locationValue)
    {
        final Script result;

        if (!locationValue.contains("@"))
        {
            // no @ in location value => global script
            final BaseScript baseScript = this.scriptRegistry.getScript(locationValue);
            if (baseScript != null)
            {
                result = this.convert(baseScript);
            }
            else
            {
                result = null;
            }
        }
        else
        {
            final String[] fragments = locationValue.split("@");
            if (fragments.length == 2)
            {
                final String scriptName = fragments[0];
                final String subRegistry = fragments[1];

                final BaseScript baseScript = this.scriptRegistry.getScript(scriptName, subRegistry);
                if (baseScript != null)
                {
                    result = this.convert(baseScript);
                }
                else
                {
                    result = null;
                }
            }
            else
            {
                throw new IllegalArgumentException("Too many occurences of '@' in location value");
            }
        }

        return result;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Script resolveLocation(final Script referenceLocation, final String locationValue, final Map<String, Object> resolutionParameters)
    {
        // we currently don't support any parameters, so just pass to default implementation
        if (resolutionParameters != null)
        {
            LOGGER.info(
                    "Implementation does not support resolution parameters - resolution of path {} from reference location {1} will continue with default implementation",
                    locationValue, referenceLocation);
            // TODO: implement lookup using ScriptSelectionCondition
        }
        return resolveLocation(referenceLocation, locationValue);
    }

    /**
     * @param scriptRegistry
     *            the scriptRegistry to set
     */
    public final void setScriptRegistry(ScriptRegistry<BaseScript> scriptRegistry)
    {
        this.scriptRegistry = scriptRegistry;
    }

    /**
     * Converts a basic script instance to the expected locator result script instance type.
     * 
     * @param baseScript
     *            the base script instance to convert
     * @return the converted script instance
     */
    abstract protected Script convert(BaseScript baseScript);
}
