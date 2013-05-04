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
package org.nabucco.alfresco.enhScriptEnv.common.webscripts.processor;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.AbstractRelativeResolvingScriptLocator;
import org.springframework.extensions.webscripts.ClassPathStore;
import org.springframework.extensions.webscripts.MultiScriptLoader;
import org.springframework.extensions.webscripts.RemoteStore;
import org.springframework.extensions.webscripts.ScriptContent;
import org.springframework.extensions.webscripts.ScriptLoader;
import org.springframework.extensions.webscripts.SearchPath;
import org.springframework.extensions.webscripts.Store;
import org.springframework.extensions.webscripts.WebScriptException;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class StoreScriptLocator extends AbstractRelativeResolvingScriptLocator<ScriptContentAdapter> implements ScriptLoader
{
    protected SearchPath searchPath;
    protected ScriptLoader scriptLoader;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        super.afterPropertiesSet();
        PropertyCheck.mandatory(this, "searchPath", this.searchPath);

        final List<ScriptLoader> loaders = new ArrayList<ScriptLoader>(this.searchPath.getStores().size());
        for (final Store apiStore : this.searchPath.getStores())
        {
            final ScriptLoader loader = apiStore.getScriptLoader();
            if (loader == null)
            {
                throw new WebScriptException(MessageFormat.format("Unable to retrieve script loader for Web Script store {0}",
                        apiStore.getBasePath()));
            }
            loaders.add(loader);
        }
        this.scriptLoader = new MultiScriptLoader(loaders.toArray(new ScriptLoader[loaders.size()]));
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public ScriptContent getScript(final String path)
    {
        return this.scriptLoader.getScript(path);
    }

    /**
     * @param searchPath
     *            the searchPath to set
     */
    public final void setSearchPath(SearchPath searchPath)
    {
        this.searchPath = searchPath;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected String getReferencePath(final ScriptContentAdapter referenceLocation)
    {
        final String referencePath;
        if (referenceLocation.getScriptContent().getClass().getDeclaringClass().isAssignableFrom(RemoteStore.class))
        {
            // awkward check for private RemoteScriptContent
            final String path = referenceLocation.getPath();
            // resolution is guaranteed (how else would this script have been located in the first place?)
            referencePath = determineScriptPath(path, referenceLocation.getPathDescription());
        }
        else if (referenceLocation.getScriptContent().getClass().getDeclaringClass().isAssignableFrom(ClassPathStore.class))
        {
            // awkward check for private ClassPathScriptLocation

            // TODO: would be nice for a proper getter
            final String path = referenceLocation.getPathDescription();
            // resolution is guaranteed (how else would this script have been located inthe first place?)
            referencePath = determineScriptPath(path, path);
        }
        else
        {
            referencePath = null;
        }
        return referencePath;
    }

    protected String determineScriptPath(final String fullPath, final String comparisonDescription)
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

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected ScriptContentAdapter loadScript(final String absolutePath)
    {
        final ScriptContentAdapter result;
        final ScriptContent scriptContent = getScript(absolutePath);
        if (scriptContent != null)
        {
            result = new ScriptContentAdapter(scriptContent);
        }
        else
        {
            result = null;
        }
        return result;
    }
}
