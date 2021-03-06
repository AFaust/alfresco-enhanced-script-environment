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

import java.util.Map;

import de.axelfaust.alfresco.enhScriptEnv.common.script.ReferenceScript;

/**
 * @author Axel Faust
 *
 */
public interface ScriptLocator<Script>
{

    /**
     * Resolves a string-based script location to a wrapper instance of the {@link ScriptLocation} interface usable by the repository's
     * script processor. Implementations may support relative script resolution - a reference location is provided in instances an already
     * running script attempts to import another.
     *
     * @param referenceLocation
     *            a reference script location if a script currently in execution attempts to import another, or {@code null} if either no
     *            script is currently being executed or the script being executed is not associated with a script location (e.g. a simple
     *            script string)
     * @param locationValue
     *            the simple location to be resolved to a proper script location
     * @return the resolved script location or {@code null} if it could not be resolved
     *
     */
    Script resolveLocation(ReferenceScript referenceLocation, String locationValue);

    /**
     * Resolves a string-based script location to a wrapper instance of the {@link ScriptLocation} interface usable by the repository's
     * script processor. Implementations may support relative script resolution - a reference location is provided in instances an already
     * running script attempts to import another.
     *
     * @param referenceLocation
     *            a reference script location if a script currently in execution attempts to import another, or {@code null} if either no
     *            script is currently being executed or the script being executed is not associated with a script location (e.g. a simple
     *            script string)
     * @param locationValue
     *            the simple location to be resolved to a proper script location
     * @param resolutionParameters
     *            an arbitrary collection of optional resolution parameters provided by the caller
     * @return the resolved script location or {@code null} if it could not be resolved
     *
     */
    Script resolveLocation(ReferenceScript referenceLocation, String locationValue, Map<String, Object> resolutionParameters);
}