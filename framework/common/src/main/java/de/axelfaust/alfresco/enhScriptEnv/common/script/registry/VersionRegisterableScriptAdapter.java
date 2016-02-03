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
package de.axelfaust.alfresco.enhScriptEnv.common.script.registry;

import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.alfresco.util.VersionNumber;
import de.axelfaust.alfresco.enhScriptEnv.common.util.CorrectVersionNumberComparator;
import org.springframework.beans.factory.InitializingBean;

/**
 * This implementation class adapts any kind of registerable script to enhance it with version information.
 *
 * @author Axel Faust
 */
public class VersionRegisterableScriptAdapter<Script> implements VersionRegisterableScript<Script>, InitializingBean
{
    protected RegisterableScript<Script> adaptedScript;

    protected VersionNumber version;
    protected VersionNumber appliesFrom;
    protected VersionNumber appliesTo;

    protected boolean appliesFromExclusive;
    protected boolean appliesToExclusive;

    protected Boolean isForCommunity;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "adaptedScript", this.adaptedScript);
        PropertyCheck.mandatory(this, "version", this.version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Script getScriptInstance()
    {
        return this.adaptedScript.getScriptInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final RegisterableScript<Script> o)
    {
        int result = 0;
        if (o instanceof VersionRegisterableScript<?>)
        {
            final VersionNumber otherVersion = ((VersionRegisterableScript<?>) o).getVersion();
            result = CorrectVersionNumberComparator.compareVersions(this.version, otherVersion);

            if (result == 0)
            {
                if (Boolean.TRUE.equals(this.isForCommunity) && Boolean.FALSE.equals(((VersionRegisterableScript<?>) o).isForCommunity()))
                {
                    // Community considered lower than Enterprise
                    result = -1;
                }
                else if (Boolean.FALSE.equals(this.isForCommunity)
                        && Boolean.TRUE.equals(((VersionRegisterableScript<?>) o).isForCommunity()))
                {
                    // Community considered lower than Enterprise
                    result = 1;
                }
            }
        }

        if (result == 0)
        {
            result = this.adaptedScript.compareTo(o);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VersionNumber getVersion()
    {
        return this.version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VersionNumber getAppliesTo()
    {
        return this.appliesTo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAppliesToExclusive()
    {
        return this.appliesToExclusive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VersionNumber getAppliesFrom()
    {
        return this.appliesFrom;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAppliesFromExclusive()
    {
        return this.appliesFromExclusive;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Boolean isForCommunity()
    {
        return this.isForCommunity;
    }

    /**
     * @param adaptedScript
     *            the adaptedScript to set
     */
    public final void setAdaptedScript(final RegisterableScript<Script> adaptedScript)
    {
        this.adaptedScript = adaptedScript;
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
     * @param version
     *            the version to set
     */
    public final void setVersion(final String version)
    {
        ParameterCheck.mandatoryString("version", version);
        this.version = new VersionNumber(version);
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
     * @param appliesFrom
     *            the appliesFrom to set
     */
    public final void setAppliesFrom(final String appliesFrom)
    {
        ParameterCheck.mandatoryString("appliesFrom", appliesFrom);
        this.appliesFrom = new VersionNumber(appliesFrom);
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
     * @param appliesTo
     *            the appliesTo to set
     */
    public final void setAppliesTo(final String appliesTo)
    {
        ParameterCheck.mandatoryString("appliesTo", appliesTo);
        this.appliesTo = new VersionNumber(appliesTo);
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
     * @param isForCommunity
     *            the isForCommunity to set
     */
    public final void setForCommunity(final Boolean isForCommunity)
    {
        this.isForCommunity = isForCommunity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("VersionRegisterableScriptAdapter [");
        if (this.adaptedScript != null)
        {
            builder.append("adaptedScript=");
            builder.append(this.adaptedScript);
            builder.append(", ");
        }
        if (this.version != null)
        {
            builder.append("version=");
            builder.append(this.version);
            builder.append(", ");
        }
        if (this.appliesFrom != null)
        {
            builder.append("appliesFrom=");
            builder.append(this.appliesFrom);
            builder.append(", ");
        }
        if (this.appliesTo != null)
        {
            builder.append("appliesTo=");
            builder.append(this.appliesTo);
            builder.append(", ");
        }
        builder.append("appliesFromExclusive=");
        builder.append(this.appliesFromExclusive);
        builder.append(", appliesToExclusive=");
        builder.append(this.appliesToExclusive);
        builder.append(", ");
        if (this.isForCommunity != null)
        {
            builder.append("isForCommunity=");
            builder.append(this.isForCommunity);
        }
        builder.append("]");
        return builder.toString();
    }

}
