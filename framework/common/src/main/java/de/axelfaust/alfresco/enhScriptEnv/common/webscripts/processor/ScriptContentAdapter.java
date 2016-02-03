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
package de.axelfaust.alfresco.enhScriptEnv.common.webscripts.processor;

import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;

import de.axelfaust.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.ClassPathStore;
import org.springframework.extensions.webscripts.RemoteStore;
import org.springframework.extensions.webscripts.ScriptContent;
import org.springframework.extensions.webscripts.ScriptLoader;

/**
 * @author Axel Faust
 */
public class ScriptContentAdapter implements ScriptContent, ReferenceScript
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptContentAdapter.class);

    protected final ScriptContent scriptContent;
    protected final ScriptLoader scriptLoader;

    public ScriptContentAdapter(final ScriptContent scriptContent, final ScriptLoader scriptLoader)
    {
        this.scriptContent = scriptContent;
        this.scriptLoader = scriptLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream()
    {
        return this.scriptContent.getInputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Reader getReader()
    {
        return this.scriptContent.getReader();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath()
    {
        return this.scriptContent.getPath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPathDescription()
    {
        return this.scriptContent.getPathDescription();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCachable()
    {
        return this.scriptContent.isCachable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecure()
    {
        return this.scriptContent.isSecure();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return this.scriptContent.toString();
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
            result = this.determineCommonReferencePaths(typeOfPath);
        }
        else if (typeOfPath instanceof SurfReferencePath)
        {
            result = this.determineSurfReferencePaths(typeOfPath);
        }
        else
        {
            LOGGER.debug("Unsupported reference path type {}", typeOfPath);
            result = null;
        }

        LOGGER.debug("Resolved reference path {} for script content {}", result, this.scriptContent);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ReferencePathType> getSupportedReferencePathTypes()
    {
        return Arrays.<ReferencePathType> asList(CommonReferencePath.FILE, CommonReferencePath.CLASSPATH, SurfReferencePath.STORE);
    }

    protected String determineCommonReferencePaths(final ReferencePathType typeOfPath)
    {
        final String result;
        switch ((CommonReferencePath) typeOfPath)
        {
        case FILE:
            final String path = this.getPath();
            if (path.startsWith("file:"))
            {
                result = path.substring(path.indexOf(':') + 1);
            }
            else
            {
                result = null;
            }
            break;
        case CLASSPATH:
            final String pathDescription = this.getPathDescription();
            if (pathDescription.startsWith("classpath:") || pathDescription.startsWith("classpath*:"))
            {
                result = pathDescription.substring(pathDescription.indexOf(':') + 1);
            }
            else
            {
                result = null;
            }
            break;

        default:
            LOGGER.warn("Unsupported (new?) reference path type {}", typeOfPath);
            result = null;
            break;
        }
        return result;
    }

    protected String determineSurfReferencePaths(final ReferencePathType typeOfPath)
    {
        final String result;
        switch ((SurfReferencePath) typeOfPath)
        {
        case STORE:
            result = this.determineStorePath();
            break;

        default:
            LOGGER.warn("Unsupported (new?) reference path type {}", typeOfPath);
            result = null;
            break;
        }
        return result;
    }

    protected String determineStorePath()
    {
        final String referencePath;
        final Class<?> declaringClassOfContent = this.scriptContent.getClass().getDeclaringClass();
        if (declaringClassOfContent != null
                && (declaringClassOfContent.isAssignableFrom(RemoteStore.class) || declaringClassOfContent
                        .isAssignableFrom(ClassPathStore.class)))
        {
            // awkward check for private RemoteScriptContent or ClassPathScriptLocation
            final String path = this.getPath();
            final String pathDescription = this.getPathDescription();
            // resolution is guaranteed (how else would this script have been located in the first place?)
            referencePath = this.determineStorePath(path, pathDescription);

            // TODO Need to prefix remotely stored script path with a store:// protocol and register URL handler to resolve scripts to source (for Rhino debugger Dim.loadSource())
        }
        else
        {
            referencePath = null;
        }
        return referencePath;
    }

    protected String determineStorePath(final String fullPath, final String comparisonDescription)
    {
        String result = null;

        final String[] pathFragments = fullPath.split("/");
        final StringBuilder pathBuilder = new StringBuilder();
        for (int idx = pathFragments.length - 1; idx >= 0; idx--)
        {
            if (pathBuilder.length() != 0)
            {
                pathBuilder.insert(0, '/');
            }
            pathBuilder.insert(0, pathFragments[idx]);

            // brute-force load & verify script with path constructed from the tail
            final ScriptContent scriptContent = this.scriptLoader.getScript(pathBuilder.toString());
            if (scriptContent != null)
            {
                final String pathDescription = scriptContent.getPathDescription();
                if (comparisonDescription.equals(pathDescription))
                {
                    // this is the current script
                    result = pathBuilder.toString();
                    break;
                }
            }
        }

        return result;
    }
}
