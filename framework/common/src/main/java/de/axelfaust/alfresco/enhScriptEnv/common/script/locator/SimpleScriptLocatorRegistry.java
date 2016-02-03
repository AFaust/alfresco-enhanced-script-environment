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
package de.axelfaust.alfresco.enhScriptEnv.common.script.locator;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.util.ParameterCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Axel Faust
 */
public class SimpleScriptLocatorRegistry<Script> implements ScriptLocatorRegistry<Script>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleScriptLocatorRegistry.class);

    protected final Map<String, ScriptLocator<Script>> locators = new HashMap<String, ScriptLocator<Script>>();

    @Override
    public synchronized void registerScriptLocator(final String name, final ScriptLocator<Script> scriptLocator)
    {
        ParameterCheck.mandatoryString("name", name);
        ParameterCheck.mandatory("scriptLocator", scriptLocator);
        final ScriptLocator<Script> replaced = this.locators.put(name, scriptLocator);
        if (replaced != null)
        {
            LOGGER.warn("ScriptLocator {} overriden by {} with name {}", new Object[] { replaced, scriptLocator, name });
        }
    }

    @Override
    public synchronized ScriptLocator<Script> getLocator(final String name)
    {
        ParameterCheck.mandatoryString("name", name);
        return this.locators.get(name);
    }

}
