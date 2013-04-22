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
package org.nabucco.alfresco.enhScriptEnv.repo.script;

import java.net.URL;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.jscript.ClasspathScriptLocation;
import org.alfresco.repo.web.scripts.RepositoryScriptProcessor;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A script locator able to import scripts from the classpath of the web application. This implementation is able to resolve relative script
 * locations when supplied with a execution context.
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ClasspathScriptLocator extends AbstractScriptLocator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathScriptLocator.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public ScriptLocation resolveLocation(final ScriptLocation referenceLocation, final String locationValue)
    {
        final ScriptLocation result;
        LOGGER.debug("Resolving {} from reference location {}", locationValue, referenceLocation);

        if (locationValue != null)
        {

            String absoluteClasspath;
            if (referenceLocation != null)
            {
                // potentially relative

                if (locationValue.startsWith("/"))
                {
                    // definitely absolute
                    absoluteClasspath = locationValue;
                }
                else
                {
                    if (referenceLocation instanceof ClasspathScriptLocation)
                    {
                        // we know this gives us the path relative to the classpath

                        // TODO: would be nice for a proper getter (which seems to have been removed recently)
                        final String referencePath = referenceLocation.toString();
                        final StringBuilder pathBuilder = new StringBuilder(referencePath.substring(0, referencePath.lastIndexOf("/")));

                        resolveRelativeLocation(locationValue, referencePath, pathBuilder);

                        absoluteClasspath = pathBuilder.toString();

                    }
                    else if (referenceLocation.getClass().getDeclaringClass().isAssignableFrom(RepositoryScriptProcessor.class))
                    {
                        // awkward check for private RepositoryScriptLocation which encapsulates a ScriptContent instance
                        // we know this gives us the classpath*:-prefixed path IF a classpath-based ScriptContent is wrapped
                        // (ClasspathScriptLocation)

                        // TODO: would be nice for a proper getter
                        final String referencePath = referenceLocation.toString();
                        if (referencePath.startsWith("classpath*:"))
                        {
                            final StringBuilder pathBuilder = new StringBuilder(referencePath.substring(referencePath.indexOf(':') + 1,
                                    referencePath.lastIndexOf('/')));
                            resolveRelativeLocation(locationValue, referencePath, pathBuilder);

                            absoluteClasspath = pathBuilder.toString();
                        }
                        else
                        {
                            LOGGER.info(
                                    "Unable to resolve relative location {} from non-classpath-based ScriptContent-wrapping reference location {}",
                                    locationValue, referenceLocation);
                            // we do not currently support relative resolution for other locations
                            // treat as absolute
                            absoluteClasspath = locationValue;
                        }
                    }
                    else
                    {
                        LOGGER.info("Unable to resolve relative location {} from unknown reference location type of {}", locationValue,
                                referenceLocation);
                        // we do not currently support relative resolution for other locations
                        // treat as absolute
                        absoluteClasspath = locationValue;
                    }
                }

            }
            else
            {
                absoluteClasspath = locationValue;
            }

            if (absoluteClasspath == null)
            {
                result = null;
            }
            else
            {
                URL scriptResource = getClass().getClassLoader().getResource(absoluteClasspath);
                if (scriptResource == null && absoluteClasspath.startsWith("/"))
                {
                    // Some classloaders prefer alfresco/foo to /alfresco/foo, try that
                    absoluteClasspath = absoluteClasspath.substring(1);
                    scriptResource = getClass().getClassLoader().getResource(absoluteClasspath);
                }

                if (scriptResource != null)
                {
                    result = new ClasspathScriptLocation(absoluteClasspath);
                }
                else
                {
                    result = null;
                }
            }

        }
        else
        {
            result = null;
        }

        LOGGER.debug("Resolved {} based on location value {} from reference location {}", result, locationValue, referenceLocation);

        return result;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public ScriptLocation resolveLocation(final ScriptLocation referenceLocation, final String locationValue,
            final Map<String, Object> resolutionParameters)
    {
        // we currently don't support any parameters, so just pass to default implementation
        if (resolutionParameters != null)
        {
            LOGGER.info(
                    "Implementation does not support resolution parameters - resolution of path {} from reference location {1} will continue with default implementation",
                    locationValue, referenceLocation);
        }
        return resolveLocation(referenceLocation, locationValue);
    }

    @SuppressWarnings("static-method")
    protected void resolveRelativeLocation(final String locationValue, final String referenceValue, final StringBuilder pathBuilder)
    {
        LOGGER.debug("Resolving relativ classpath location {} from reference {}", locationValue, referenceValue);
        int lastSlash = -1;
        int nextSlash = locationValue.indexOf("/", 0);
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
                                throw new AlfrescoRuntimeException(
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
                        throw new AlfrescoRuntimeException(
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
        }

        if (nextSlash == -1 && lastSlash == -1)
        {
            // no slash found at all
            pathBuilder.append("/");
        }

        pathBuilder.append(lastSlash != -1 ? locationValue.substring(lastSlash) : locationValue);

        LOGGER.debug("Resolved classpath location {} by relative path {} from reference {}", pathBuilder, locationValue, referenceValue);
    }

}