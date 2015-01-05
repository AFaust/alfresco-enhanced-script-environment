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
package org.nabucco.alfresco.enhScriptEnv.common.script.locator;

import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.ScriptImportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class AbstractScriptLocator<Script extends ReferenceScript> implements ScriptLocator<Script>, InitializingBean
{

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractScriptLocator.class);

    protected String name;
    protected ScriptLocatorRegistry<Script> scriptLocatorRegistry;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "scriptLocatorRegistry", this.scriptLocatorRegistry);
        this.scriptLocatorRegistry.registerScriptLocator(this.name, this);
    }

    /**
     * @param name
     *            the name to set
     */
    public final void setName(final String name)
    {
        this.name = name;
    }

    /**
     * Sets the scriptLocatorRegistry to given scriptLocatorRegistry.
     * 
     * @param scriptLocatorRegistry
     *            the scriptLocatorRegistry to set
     */
    public final void setScriptLocatorRegistry(final ScriptLocatorRegistry<Script> scriptLocatorRegistry)
    {
        this.scriptLocatorRegistry = scriptLocatorRegistry;
    }

    /**
     * Resolves a relative location value against the reference location provided by the currently exectued script.
     * 
     * @param locationValue
     *            The relative location value to resolve
     * @param referenceValue
     *            The reference location to resolve against. This parameter is primarily used in an informative capacity (e.g. for logging)
     *            while the pathBuilder is the work-item.
     * @param pathBuilder
     *            A builder for the final path that should be manipulated during the resolution. The instance passed should be
     *            pre-initialized with the base path determined from the reference location.
     */
    @SuppressWarnings("static-method")
    protected void resolveRelativeLocation(final String locationValue, final String referenceValue, final StringBuilder pathBuilder)
    {
        LOGGER.debug("Resolving relativ classpath location {} from reference {}", locationValue, referenceValue);
        int lastSlash = -1;
        int nextSlash = locationValue.indexOf("/");
        boolean descending = false;
        while (nextSlash != -1)
        {
            final String fragment = locationValue.substring(lastSlash + 1, nextSlash);

            if (fragment.length() != 0)
            {

                if (fragment.equalsIgnoreCase(".."))
                {
                    if (!descending)
                    {
                        // ascend
                        final int deleteFrom = pathBuilder.lastIndexOf("/");
                        if (deleteFrom == -1)
                        {
                            if (pathBuilder.length() > 0)
                            {
                                pathBuilder.delete(0, pathBuilder.length());
                            }
                            else
                            {
                                LOGGER.warn("Resolving {} from reference {} caused ascension beyond root", locationValue, referenceValue);
                                // nowhere to ascend to
                                throw new ScriptImportException(
                                        "Unable to ascend out of classpath - context location: [{0}], script location: [{1}]",
                                        new Object[] { referenceValue, locationValue });
                            }
                        }
                        else
                        {
                            pathBuilder.delete(deleteFrom, pathBuilder.length());
                        }
                    }
                    else
                    {
                        LOGGER.warn("Cannot ascend after descending in resolution of {} from reference {}", locationValue, referenceValue);
                        // no re-ascension
                        throw new ScriptImportException(
                                "Unable to ascend after already descending - context location: [{0}], script location: [{1}]",
                                new Object[] { referenceValue, locationValue });
                    }
                }
                else if (fragment.equalsIgnoreCase("."))
                {
                    descending = true;
                }
                else
                {
                    descending = true;
                    pathBuilder.append("/").append(fragment);
                }
            }

            lastSlash = nextSlash;
            nextSlash = locationValue.indexOf('/', lastSlash + 1);
        }

        if (nextSlash == -1 && lastSlash == -1)
        {
            // no slash found at all
            pathBuilder.append("/");
        }

        pathBuilder.append(lastSlash != -1 ? locationValue.substring(lastSlash) : locationValue);

        LOGGER.debug("Resolved classpath location {} by relative path {} from reference {}", new Object[] { pathBuilder, locationValue,
                referenceValue });
    }
}
