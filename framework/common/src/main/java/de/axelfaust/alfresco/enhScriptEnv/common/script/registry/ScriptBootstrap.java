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
package de.axelfaust.alfresco.enhScriptEnv.common.script.registry;

import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

/**
 * This class provides a simple utility to bootstrap registerable scripts into a script registry on application context startup.
 * 
 * @author Axel Faust
 */
public class ScriptBootstrap<Script> implements InitializingBean
{

    protected ScriptRegistry<Script> registry;
    protected RegisterableScript<Script> script;

    protected String name;
    protected String subRegistry;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "registry", this.registry);
        PropertyCheck.mandatory(this, "script", this.script);
        PropertyCheck.mandatory(this, "name", this.name);

        if (this.subRegistry != null)
        {
            this.registry.registerScript(this.name, this.subRegistry, this.script);
        }
        else
        {
            this.registry.registerScript(this.name, this.script);
        }
    }

    /**
     * @param registry
     *            the registry to set
     */
    public final void setRegistry(ScriptRegistry<Script> registry)
    {
        this.registry = registry;
    }

    /**
     * @param script
     *            the script to set
     */
    public final void setScript(RegisterableScript<Script> script)
    {
        this.script = script;
    }

    /**
     * @param name
     *            the name to set
     */
    public final void setName(String name)
    {
        this.name = name;
    }

    /**
     * @param subRegistry
     *            the subRegistry to set
     */
    public final void setSubRegistry(String subRegistry)
    {
        this.subRegistry = subRegistry;
    }

}
