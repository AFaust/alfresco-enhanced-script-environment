package org.nabucco.alfresco.enhScriptEnv.common.script.registry;

import org.alfresco.util.ParameterCheck;
import org.alfresco.util.VersionNumber;
import org.nabucco.alfresco.enhScriptEnv.common.util.CorrectVersionNumberComparator;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class AppliesForVersionCondition implements ScriptSelectionCondition
{

    protected final VersionNumber version;

    public AppliesForVersionCondition(final VersionNumber version)
    {
        ParameterCheck.mandatory("version", version);
        this.version = version;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final RegisterableScript<?> script)
    {
        final boolean result;

        if (script instanceof VersionRegisterableScript<?>)
        {
            final VersionRegisterableScript<?> versionedScript = (VersionRegisterableScript<?>) script;

            final VersionNumber appliesFrom = versionedScript.getAppliesFrom();
            final VersionNumber appliesTo = versionedScript.getAppliesTo();

            final boolean appliesToExclusive = versionedScript.isAppliesToExclusive();
            final boolean appliesFromExclusive = versionedScript.isAppliesFromExclusive();

            final boolean appliesFromMatches = appliesFrom == null
                    || CorrectVersionNumberComparator.compareVersions(this.version, appliesFrom) > (appliesFromExclusive ? 0 : -1);
            final boolean appliesToMatches = appliesTo == null
                    || CorrectVersionNumberComparator.compareVersions(this.version, appliesTo) < (appliesToExclusive ? 0 : 1);
            result = appliesFromMatches && appliesToMatches;
        }
        else
        {
            result = false;
        }

        return result;
    }

}
