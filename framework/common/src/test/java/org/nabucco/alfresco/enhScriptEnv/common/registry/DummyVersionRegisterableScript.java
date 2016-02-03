package de.axelfaust.alfresco.enhScriptEnv.common.registry;

import org.alfresco.util.VersionNumber;
import de.axelfaust.alfresco.enhScriptEnv.common.script.registry.RegisterableScript;
import de.axelfaust.alfresco.enhScriptEnv.common.script.registry.VersionRegisterableScript;

/**
 * @author Axel Faust
 */
public class DummyVersionRegisterableScript implements VersionRegisterableScript<Object>
{
    protected VersionNumber version;
    protected VersionNumber appliesFrom;
    protected VersionNumber appliesTo;

    protected boolean appliesFromExclusive;
    protected boolean appliesToExclusive;

    protected Boolean forCommunity;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object getScriptInstance()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final RegisterableScript<Object> otherScript)
    {
        return this.hashCode() - otherScript.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final VersionNumber getVersion()
    {
        return this.version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final VersionNumber getAppliesFrom()
    {
        return this.appliesFrom;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final VersionNumber getAppliesTo()
    {
        return this.appliesTo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isAppliesFromExclusive()
    {
        return this.appliesFromExclusive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isAppliesToExclusive()
    {
        return this.appliesToExclusive;
    }

    /**
     * @param version
     *            the version to set
     */
    public final void setVersion(final VersionNumber version)
    {
        this.version = version;
    }

    /**
     * @param appliesFrom
     *            the appliesFrom to set
     */
    public final void setAppliesFrom(final VersionNumber appliesFrom)
    {
        this.appliesFrom = appliesFrom;
    }

    /**
     * @param appliesTo
     *            the appliesTo to set
     */
    public final void setAppliesTo(final VersionNumber appliesTo)
    {
        this.appliesTo = appliesTo;
    }

    /**
     * @param appliesFromExclusive
     *            the appliesFromExclusive to set
     */
    public final void setAppliesFromExclusive(final boolean appliesFromExclusive)
    {
        this.appliesFromExclusive = appliesFromExclusive;
    }

    /**
     * @param appliesToExclusive
     *            the appliesToExclusive to set
     */
    public final void setAppliesToExclusive(final boolean appliesToExclusive)
    {
        this.appliesToExclusive = appliesToExclusive;
    }

    /**
     * @return the forCommunity
     */
    @Override
    public final Boolean isForCommunity()
    {
        return this.forCommunity;
    }

    /**
     * @param forCommunity
     *            the forCommunity to set
     */
    public final void setForCommunity(final Boolean isForCommunity)
    {
        this.forCommunity = isForCommunity;
    }

}
