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

import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;

import org.alfresco.repo.jscript.ClasspathScriptLocation;
import org.alfresco.repo.web.scripts.RepositoryScriptProcessor;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript.ReferencePathType;
import org.nabucco.alfresco.enhScriptEnv.common.webscripts.processor.SurfReferencePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.ClassPathStore;
import org.springframework.extensions.webscripts.RemoteStore;
import org.springframework.extensions.webscripts.ScriptContent;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ScriptLocationAdapter implements ScriptLocation, ReferenceScript
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptLocationAdapter.class);

    protected final ScriptLocation scriptLocation;

    public ScriptLocationAdapter(final ScriptLocation scriptLocation)
    {
        this.scriptLocation = scriptLocation;
    }

    /**
     * @return the scriptLocation
     */
    public final ScriptLocation getScriptLocation()
    {
        return this.scriptLocation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream()
    {
        return this.scriptLocation.getInputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Reader getReader()
    {
        return this.scriptLocation.getReader();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath()
    {
        return this.scriptLocation.getPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCachable()
    {
        return this.scriptLocation.isCachable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecure()
    {
        return this.scriptLocation.isSecure();
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getReferencePath(final ReferencePathType typeOfPath)
    {
        final String result;
        if (typeOfPath instanceof CommonReferencePath)
        {
            result = determineCommonReferencePaths(typeOfPath);
        }
        // TODO: Repository reference path types, i.e. XPath
        else
        {
            LOGGER.debug("Unsupported reference path type {}", typeOfPath);
            result = null;
        }

        LOGGER.debug("Resolved reference path {} for script content {}", result, this.scriptLocation);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ReferencePathType> getSupportedReferencePathTypes()
    {
        return Arrays.<ReferencePathType> asList(CommonReferencePath.FILE, CommonReferencePath.CLASSPATH);
    }

    protected String determineCommonReferencePaths(final ReferencePathType typeOfPath)
    {
        final String result;
        switch ((CommonReferencePath) typeOfPath)
        {
        case FILE:
        {
            final String path = this.getPath();
            if (path.startsWith("file:"))
            {
                result = path;
            }
            else
            {
                result = null;
            }
        }
            break;
        case CLASSPATH:
            result = determineClasspath();
            break;

        default:
            LOGGER.warn("Unsupported (new?) reference path type {}", typeOfPath);
            result = null;
            break;
        }
        return result;
    }

    protected String determineClasspath()
    {
        final String result;
        {
            final Class<?> declaringClassOfLocation = this.getClass().getDeclaringClass();
            final String path;
            if (declaringClassOfLocation != null && declaringClassOfLocation.isAssignableFrom(RepositoryScriptProcessor.class))
            {
                // awkward check for private RepositoryScriptLocation which encapsulates a ScriptContent instance
                // we know this gives us the classpath*:-prefixed path IF a classpath-based ScriptContent is wrapped
                // (ClasspathScriptLocation)
                path = this.scriptLocation.toString();
            }
            else if (this.scriptLocation instanceof ClasspathScriptLocation)
            {
                path = this.scriptLocation.getPath();
            }
            else
            {
                // TODO: try to resolve arbitrary file: paths to classpath
                path = null;
            }

            if (path != null)
            {
                if (path.startsWith("classpath*:") || path.startsWith("classpath:"))
                {
                    result = path.substring(path.indexOf(':') + 1);
                }
                else if (path.indexOf(':') == -1)
                {
                    result = path;
                }
                else
                {
                    result = null;
                }
            }
            else
            {
                result = null;
            }
        }
        return result;
    }
}
