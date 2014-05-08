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
package org.nabucco.alfresco.enhScriptEnv.common.script.locator;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public interface ScriptLocatorRegistry<Script>
{
    /**
     * Registers a specific script locator with the script processor.
     *
     * @param name
     *            the name of the locator - to be used in import calls within script files
     * @param scriptLocator
     *            the script locator
     */
    public void registerScriptLocator(String name, ScriptLocator<Script> scriptLocator);

    /**
     * Retrieves a specific script locator.
     *
     * @param name
     *            the name of the locator
     * @return the script locator or {@code null} if no locator was registered with that name
     */
    public ScriptLocator<Script> getLocator(String name);
}
