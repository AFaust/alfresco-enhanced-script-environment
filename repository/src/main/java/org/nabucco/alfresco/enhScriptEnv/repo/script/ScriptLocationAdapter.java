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

import org.alfresco.service.cmr.repository.ScriptLocation;
import org.nabucco.alfresco.enhScriptEnv.common.script.SecurableScript;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ScriptLocationAdapter implements ScriptLocation, SecurableScript
{

    private final ScriptLocation scriptLocation;

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

}
