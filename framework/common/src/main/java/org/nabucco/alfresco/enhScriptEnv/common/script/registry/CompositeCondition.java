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

import java.util.Collection;
import java.util.HashSet;

import org.alfresco.util.ParameterCheck;

/**
 * @author Axel Faust
 */
public class CompositeCondition implements ScriptSelectionCondition
{

    protected final Collection<ScriptSelectionCondition> conditionFragments;

    public CompositeCondition(final Collection<ScriptSelectionCondition> conditionFragments)
    {
        ParameterCheck.mandatory("conditionFragments", conditionFragments);
        this.conditionFragments = new HashSet<ScriptSelectionCondition>(conditionFragments);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final RegisterableScript<?> script)
    {
        final boolean result;

        boolean matchesAll = !this.conditionFragments.isEmpty();
        for (final ScriptSelectionCondition condition : this.conditionFragments)
        {
            matchesAll = matchesAll && condition.matches(script);
        }

        result = matchesAll;

        return result;
    }
}
