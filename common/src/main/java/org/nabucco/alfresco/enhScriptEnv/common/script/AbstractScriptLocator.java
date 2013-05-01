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
package org.nabucco.alfresco.enhScriptEnv.common.script;

import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class AbstractScriptLocator<Script extends SecurableScript> implements ScriptLocator<Script>, InitializingBean
{

    protected String name;
    protected ScriptLocatorRegistry<Script> scriptLocatorRegistry;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        if (this.scriptLocatorRegistry == null)
        {
            throw new IllegalArgumentException("Script locator registry is not initialized");
        }
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

}
