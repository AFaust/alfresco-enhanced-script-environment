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
import org.springframework.extensions.webscripts.MultiScriptLoader;
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
        final String referencePath = referenceLocation.getReferencePath(SurfReferencePath.STORE);
        return referencePath;
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
            result = new ScriptContentAdapter(scriptContent, this.scriptLoader);
        }
        else
        {
            result = null;
        }
        return result;
    }
}
