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
package org.nabucco.alfresco.enhScriptEnv.repo.script;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.alfresco.repo.jscript.ClasspathScriptLocation;
import org.alfresco.repo.web.scripts.RepositoryScriptProcessor;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ScriptLocationAdapter implements ScriptLocation, ReferenceScript
{
    private static final String FILE_FOLDER_PATH_PATTERN = "^((?!(?:classpath|file):)[^:]+://[^/]+)((?:/[^/]+){2,})$";

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptLocationAdapter.class);

    protected final ScriptLocation scriptLocation;

    // cached for potential performance improvements in multiple retrievals
    protected transient String path;

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
        if (this.path == null)
        {
            this.path = this.scriptLocation.getPath();
        }

        return this.path;
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
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return this.scriptLocation.toString();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getFullName()
    {
        return this.getPath();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        final String scriptName;

        final String path = this.getPath();
        final int i = path.lastIndexOf('/');
        scriptName = i != -1 ? path.substring(i + 1) : path;

        return scriptName;
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
            result = this.determineCommonReferencePaths((CommonReferencePath) typeOfPath);
        }
        else if (typeOfPath instanceof RepositoryReferencePath)
        {
            result = this.determineRepositoryReferencePaths((RepositoryReferencePath) typeOfPath);
        }
        // TODO Support Surf reference paths
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
        final Collection<ReferencePathType> supportedTypes = new ArrayList<ReferenceScript.ReferencePathType>(
                Arrays.<ReferencePathType> asList(CommonReferencePath.FILE, CommonReferencePath.CLASSPATH));

        final ScriptLocation scriptLocation = this.getScriptLocation();

        if (scriptLocation instanceof NodeScriptLocation)
        {
            supportedTypes.add(RepositoryReferencePath.NODE_REF);
            supportedTypes.add(RepositoryReferencePath.CONTENT_PROPERTY);
        }

        supportedTypes.add(RepositoryReferencePath.FILE_FOLDER_PATH);

        return supportedTypes;
    }

    protected String determineRepositoryReferencePaths(final RepositoryReferencePath typeOfPath)
    {
        final String result;
        switch (typeOfPath)
        {
        case NODE_REF:
        {
            final ScriptLocation scriptLocation = this.getScriptLocation();

            if (scriptLocation instanceof NodeScriptLocation)
            {
                result = ((NodeScriptLocation) scriptLocation).getNode().toString();
            }
            // TODO Check possibility of resolving RepoScriptContent to NodeRef
            else
            {
                result = null;
            }
        }
            break;
        case CONTENT_PROPERTY:
        {
            final ScriptLocation scriptLocation = this.getScriptLocation();

            if (scriptLocation instanceof NodeScriptLocation)
            {
                result = ((NodeScriptLocation) scriptLocation).getContentProp().toString();
            }
            else
            {
                result = null;
            }
        }
            break;
        case FILE_FOLDER_PATH:
        {
            final String path = this.getPath();

            // FILE_FOLDER_PATH - based on StoreRef + baseDir + path => at least two path segments after StoreRef, typically more
            if (path.matches(FILE_FOLDER_PATH_PATTERN))
            {
                result = path;
            }
            else
            {
                result = null;
            }
        }
            break;
        default:
            LOGGER.warn("Unsupported (new?) reference path type {}", typeOfPath);
            result = null;
            break;
        }

        return result;
    }

    protected String determineCommonReferencePaths(final CommonReferencePath typeOfPath)
    {
        final String result;
        switch (typeOfPath)
        {
        case FILE:
        {
            final String path = this.getPath();
            if (path.startsWith("file:"))
            {
                result = path.substring(path.indexOf(':') + 1);
            }
            else
            {
                result = null;
            }
        }
            break;
        case CLASSPATH:
            result = this.determineClasspath();
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
