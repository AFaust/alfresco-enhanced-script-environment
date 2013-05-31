package org.nabucco.alfresco.enhScriptEnv.common.script.registry;

import org.alfresco.util.VersionNumber;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class FallsInVersionRangeCondition implements ScriptSelectionCondition
{
    protected final VersionNumber appliesFrom;
    protected final VersionNumber appliesTo;

    protected final boolean appliesFromExclusive;
    protected final boolean appliesToExclusive;

    public FallsInVersionRangeCondition(final VersionNumber appliesFrom, final boolean appliesFromExclusive, final VersionNumber appliesTo,
            final boolean appliesToExclusive)
    {
        this.appliesFrom = appliesFrom;
        this.appliesFromExclusive = appliesFromExclusive;
        this.appliesTo = appliesTo;
        this.appliesToExclusive = appliesToExclusive;

        if (this.appliesFrom == null ^ this.appliesTo == null)
        {
            throw new IllegalArgumentException("Either appliesFrom or appliesTo are required to be set");
        }
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

            final VersionNumber version = versionedScript.getVersion();

            final boolean appliesFromMatches = this.appliesFrom == null
                    || version.compareTo(this.appliesFrom) > (this.appliesFromExclusive ? 0 : -1);
            final boolean appliesToMatches = this.appliesTo == null
                    || version.compareTo(this.appliesTo) < (this.appliesToExclusive ? 0 : 1);
            result = appliesFromMatches && appliesToMatches;
        }
        else
        {
            result = false;
        }

        return result;
    }
}
