/*
 * Copyright 2013 PRODYNA AG
 *
 * Licensed under the Eclipse Public License (EPL), Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/eclipse-1.0.php or
 * http://www.nabucco.org/License.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
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

        if (this.appliesFrom == null && this.appliesTo == null)
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
