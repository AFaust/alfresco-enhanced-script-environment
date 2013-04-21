package org.nabucco.alfresco.enhScriptEnv.repo.script;

import java.util.Map;

import org.alfresco.service.cmr.repository.ScriptLocation;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 * 
 */
public interface ScriptLocator
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
    ScriptLocation resolveLocation(ScriptLocation referenceLocation, String locationValue);

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
    ScriptLocation resolveLocation(ScriptLocation referenceLocation, String locationValue, Map<String, Object> resolutionParameters);
}