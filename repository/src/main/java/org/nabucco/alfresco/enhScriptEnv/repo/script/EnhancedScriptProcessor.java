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

import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.ScriptProcessor;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public interface EnhancedScriptProcessor extends ScriptProcessor
{

    /**
     * Executes a script in a provided scope. When the scope is mutable, execution of the script may result in modifications of its state
     * depending on the script being executed.
     * 
     * @param location
     *            the location of the script
     * @param scope
     *            the scope the script is to be executed in
     * 
     */
    public void executeInScope(ScriptLocation location, Object scope);

    /**
     * Retrieves the script location for the current script execution context. This result of this method heavily depends on the state of
     * execution of the current thread and the chain of script executions that resulted in the invocation of the caller.
     * 
     * @return the script location object for the script currently being executed resulting in the invocation of the caller. This may be
     *         {@code null} if either no script is currently being executed or the script being executed is of a dynamic nature.
     */
    public ScriptLocation getContextScriptLocation();

    /**
     * Registers a specific script locator with the script processor.
     * 
     * @param name
     *            the name of the locator - to be used in import calls within script files
     * @param scriptLocator
     *            the script locator
     */
    public void registerScriptLocator(String name, ScriptLocator scriptLocator);
}
