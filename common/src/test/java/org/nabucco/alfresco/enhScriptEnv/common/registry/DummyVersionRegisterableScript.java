package org.nabucco.alfresco.enhScriptEnv.common.registry;

import org.alfresco.util.VersionNumber;
import org.nabucco.alfresco.enhScriptEnv.common.script.registry.RegisterableScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.registry.VersionRegisterableScript;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class DummyVersionRegisterableScript implements VersionRegisterableScript<Object>
{
    protected VersionNumber version;
    protected VersionNumber appliesFrom;
    protected VersionNumber appliesTo;

    protected boolean appliesFromExclusive;
    protected boolean appliesToExclusive;

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
        return hashCode() - otherScript.hashCode();
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
    public final void setVersion(VersionNumber version)
    {
        this.version = version;
    }

    /**
     * @param appliesFrom
     *            the appliesFrom to set
     */
    public final void setAppliesFrom(VersionNumber appliesFrom)
    {
        this.appliesFrom = appliesFrom;
    }

    /**
     * @param appliesTo
     *            the appliesTo to set
     */
    public final void setAppliesTo(VersionNumber appliesTo)
    {
        this.appliesTo = appliesTo;
    }

    /**
     * @param appliesFromExclusive
     *            the appliesFromExclusive to set
     */
    public final void setAppliesFromExclusive(boolean appliesFromExclusive)
    {
        this.appliesFromExclusive = appliesFromExclusive;
    }

    /**
     * @param appliesToExclusive
     *            the appliesToExclusive to set
     */
    public final void setAppliesToExclusive(boolean appliesToExclusive)
    {
        this.appliesToExclusive = appliesToExclusive;
    }
}
