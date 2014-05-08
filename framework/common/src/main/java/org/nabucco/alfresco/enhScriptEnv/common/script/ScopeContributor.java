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


/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public interface ScopeContributor
{

    /**
     * Contributes elements (values / functionality) to a scope of a Rhino-based JavaScript environment.
     *
     * @param scope
     *            the scope to contribute to
     * @param trustworthyScript
     *            {@code true} if the scope is made available to trustworthy scripts (i.e. controlled by deployment processes),
     *            {@code false} otherwise
     * @param mutableScope
     *            {@code true} if the scope should be considered mutable, i.e. is only created for the execution of a single script,
     *            {@code false} otherwise, i.e. when the scope may be reused
     */
    void contributeToScope(Object scope, boolean trustworthyScript, boolean mutableScope);

}
