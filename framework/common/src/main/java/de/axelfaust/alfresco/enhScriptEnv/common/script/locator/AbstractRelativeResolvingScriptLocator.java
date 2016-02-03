/*
 * Copyright 2016 Axel Faust
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
package de.axelfaust.alfresco.enhScriptEnv.common.script.locator;

import java.util.Map;

import de.axelfaust.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A script locator able to import scripts from the classpath of the web application. This implementation is able to resolve relative script
 * locations when supplied with a execution context.
 *
 * @author Axel Faust
 */
public abstract class AbstractRelativeResolvingScriptLocator<Script extends ReferenceScript> extends AbstractScriptLocator<Script>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRelativeResolvingScriptLocator.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public final Script resolveLocation(final ReferenceScript referenceLocation, final String locationValue)
    {
        final Script result;
        LOGGER.debug("Resolving {} from reference location {}", locationValue, referenceLocation);

        if (locationValue != null)
        {

            String absolutePath;
            if (referenceLocation != null)
            {
                // potentially relative

                if (locationValue.startsWith("/"))
                {
                    // definitely absolute
                    absolutePath = locationValue;
                }
                else
                {
                    final String referencePath = this.getReferencePath(referenceLocation);
                    if (referencePath != null)
                    {
                        final StringBuilder pathBuilder = new StringBuilder();
                        if (referencePath.contains("/"))
                        {
                            pathBuilder.append(referencePath.substring(referencePath.contains(":") ? referencePath.indexOf(':') + 1 : 0,
                                    referencePath.lastIndexOf('/') + 1));
                        }
                        this.resolveRelativeLocation(locationValue, referencePath, pathBuilder);

                        absolutePath = pathBuilder.toString();
                    }
                    else
                    {
                        LOGGER.info("Unable to resolve relative location {} from unsupported reference location type of {}", locationValue,
                                referenceLocation);
                        // we do not currently support relative resolution for other locations
                        // treat as absolute
                        absolutePath = locationValue;
                    }
                }

            }
            else
            {
                absolutePath = locationValue;
            }

            if (absolutePath == null)
            {
                result = null;
            }
            else
            {
                result = this.loadScript(absolutePath);
            }

        }
        else
        {
            result = null;
        }

        LOGGER.debug("Resolved {} based on location value {} from reference location {}", new Object[] { result, locationValue,
                referenceLocation });

        return result;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Script resolveLocation(final ReferenceScript referenceLocation, final String locationValue,
            final Map<String, Object> resolutionParameters)
    {
        // we currently don't support any parameters, so just pass to default implementation
        if (resolutionParameters != null)
        {
            LOGGER.info(
                    "Implementation does not support resolution parameters - resolution of path {} from reference location {1} will continue with default implementation",
                    locationValue, referenceLocation);
        }
        return this.resolveLocation(referenceLocation, locationValue);
    }

    protected abstract String getReferencePath(ReferenceScript referenceLocation);

    protected abstract Script loadScript(String absolutePath);

}