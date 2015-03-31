/*
 * Copyright 2015 PRODYNA AG
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
package org.nabucco.alfresco.enhScriptEnv.common.script;

import java.util.List;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public interface EnhancedScriptProcessor<SL>
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
     * @return the result of the script execution - may be {@code null} if the script did not yield a direct result
     *
     */
    Object executeInScope(SL location, Object scope);

    /**
     * Executes a script in a provided scope. When the scope is mutable, execution of the script may result in modifications of its state
     * depending on the script being executed.
     *
     * @param source
     *            the source code of the script to execute
     * @param scope
     *            the scope the script is to be executed in
     *
     * @return the result of the script execution - may be {@code null} if the script did not yield a direct result
     */
    Object executeInScope(String source, Object scope);

    /**
     * Retrieves the script location for the current script execution context. This result of this method heavily depends on the state of
     * execution of the current thread and the chain of script executions that resulted in the invocation of the caller.
     *
     * @return the script location object for the script currently being executed resulting in the invocation of the caller. This may be
     *         {@code null} if no script is currently being executed.
     */
    ReferenceScript getContextScriptLocation();

    /**
     * Retrieves the chain of scripts for the current script execution context. This method will not return all the scripts that the current
     * callers thread is currently nested in - instead it will return those scripts that through an unbroken chain of importScript API calls
     * have invoked each other.
     *
     * @return the chain scripts leading to the call on the script currently being executed in the order they have been called, i.e. with
     *         first script at an index position of zero. This may be {@code null} or an empty list if no script is currently being
     *         executed.
     */
    List<ReferenceScript> getScriptCallChain();

    /**
     * Inherits and initializes the call chain for the current context from the provided parent context. Clients may use this operation to
     * initialize necessary call chain information in situations where execution is not performed linearly in the same thread / context.
     *
     * @param parentContext
     *            the context to inherit the call chain from
     * @throws IllegalStateException
     *             when the current context has already been initialized with a call chain
     */
    void inheritCallChain(Object parentContext);

    /**
     * Initializes a new execution scope for the provided script
     *
     * @param location
     *            the script to initialize a execution context for
     * @return the new execution context
     */
    Object initializeScope(SL location);

    /**
     * Registers a scope contributor to be invoked whenever a new scripting scope is initialized for contribution of additional values /
     * functionality to that scope.
     *
     * @param contributor
     *            the contributor to register
     */
    void registerScopeContributor(ScopeContributor contributor);

    /**
     * Notifies the script processor that a debugger has been attached to the script environment. Depending on the internal implementation,
     * this may affect compilation and optimization behavior of the processor.
     */
    void debuggerAttached();

    /**
     * Notifies the script processor that a debugger has been detached from the script environment. Depending on the internal
     * implementation, this may affect compilation and optimization behavior of the processor.
     */
    void debuggerDetached();
}
