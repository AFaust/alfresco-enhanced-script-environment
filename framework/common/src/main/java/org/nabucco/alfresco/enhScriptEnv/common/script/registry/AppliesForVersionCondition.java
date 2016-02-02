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
package org.nabucco.alfresco.enhScriptEnv.common.script.registry;

import org.alfresco.util.ParameterCheck;
import org.alfresco.util.VersionNumber;
import org.nabucco.alfresco.enhScriptEnv.common.util.CorrectVersionNumberComparator;

/**
 * @author Axel Faust
 */
public class AppliesForVersionCondition implements ScriptSelectionCondition
{

    protected final VersionNumber version;
    protected final Boolean community;

    public AppliesForVersionCondition(final VersionNumber version, final Boolean community)
    {
        ParameterCheck.mandatory("version", version);
        this.version = version;
        this.community = community;
    }

    public AppliesForVersionCondition(final VersionNumber version)
    {
        this(version, null);
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
            final boolean editionMatches = this.community == versionedScript.isForCommunity()
                    || (this.community != null && this.community.equals(versionedScript.isForCommunity()));
            result = appliesFromMatches && appliesToMatches && editionMatches;
        }
        else
        {
            result = false;
        }

        return result;
    }

}
