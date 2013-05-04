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

import java.io.InputStream;
import java.io.Reader;

import org.nabucco.alfresco.enhScriptEnv.common.script.SecurableScript;
import org.springframework.extensions.webscripts.ScriptContent;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ScriptContentAdapter implements ScriptContent, SecurableScript
{

    private final ScriptContent scriptContent;

    public ScriptContentAdapter(final ScriptContent scriptContent)
    {
        this.scriptContent = scriptContent;
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
     * @return the scriptContent
     */
    public final ScriptContent getScriptContent()
    {
        return this.scriptContent;
    }

}
