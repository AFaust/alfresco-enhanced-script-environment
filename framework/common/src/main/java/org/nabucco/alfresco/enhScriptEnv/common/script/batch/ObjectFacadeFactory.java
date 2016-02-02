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
package org.nabucco.alfresco.enhScriptEnv.common.script.batch;

import org.mozilla.javascript.Scriptable;

/**
 * @author Axel Faust
 */
public interface ObjectFacadeFactory
{

    Scriptable toFacadedObject(Scriptable object, Scriptable referenceScope);

    Scriptable toFacadedObject(Scriptable object, Scriptable referenceScope, String accessName);

    Scriptable toRealObject(Scriptable facadedObject, Scriptable referenceScope);

    void clearThread();

    void clearReferenceScope(Scriptable referenceScope);
}
