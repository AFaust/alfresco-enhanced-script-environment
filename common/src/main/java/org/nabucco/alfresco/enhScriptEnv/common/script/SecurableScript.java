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
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public interface SecurableScript
{

    /**
     * Determines wether a script can be considered secure in that its content / logic is managed by the system and not mutable by end
     * users. A secure script may be granted additional privileges to access the runtime over non-secure scripts.
     * 
     * @return {@code true} if the script can be considered to be a secure script (not user-managed), {@code false} otherwise
     */
    boolean isSecure();
}
