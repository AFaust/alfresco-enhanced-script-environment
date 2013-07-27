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
package org.nabucco.alfresco.enhScriptEnv.common.script.locator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.alfresco.util.PropertyCheck;
import org.alfresco.util.VersionNumber;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.registry.AppliesForVersionCondition;
import org.nabucco.alfresco.enhScriptEnv.common.script.registry.CompositeCondition;
import org.nabucco.alfresco.enhScriptEnv.common.script.registry.FallsInVersionRangeCondition;
import org.nabucco.alfresco.enhScriptEnv.common.script.registry.ScriptRegistry;
import org.nabucco.alfresco.enhScriptEnv.common.script.registry.ScriptSelectionCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class RegisteredScriptLocator<BaseScript, Script extends ReferenceScript> extends AbstractScriptLocator<Script>
{
    protected static final String CONDITIONS = "conditions";
    protected static final String VERSION = "version";
    protected static final String COMMUNITY = "community";
    protected static final String APPLIES_FROM = "appliesFrom";
    protected static final String APPLIES_TO = "appliesTo";

    protected static final String APPLIES_FROM_EXCLUSIVE = "appliesFromExclusive";
    protected static final String APPLIES_TO_EXCLUSIVE = "appliesToExclusive";

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisteredScriptLocator.class);

    protected ScriptRegistry<BaseScript> scriptRegistry;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        super.afterPropertiesSet();
        PropertyCheck.mandatory(this, "scriptRegistry", this.scriptRegistry);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public final Script resolveLocation(final Script referenceLocation, final String locationValue)
    {
        final Script result;

        result = this.lookupScriptInRegistry(locationValue, null);

        return result;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Script resolveLocation(final Script referenceLocation, final String locationValue, final Map<String, Object> resolutionParameters)
    {
        final Script script;
        // we currently don't support any parameters, so just pass to default implementation
        if (resolutionParameters != null)
        {
            final ScriptSelectionCondition condition = this.extractSelectionCondition(resolutionParameters);
            if (condition != null)
            {
                script = this.lookupScriptInRegistry(locationValue, condition);
            }
            else
            {
                LOGGER.info(
                        "Unable to determine selection condition for resolution of path {} from reference location {} - parameters provided {}",
                        new Object[] { locationValue, referenceLocation, resolutionParameters });
                script = this.resolveLocation(referenceLocation, locationValue);
            }
        }
        else
        {
            script = this.resolveLocation(referenceLocation, locationValue);
        }
        return script;
    }

    /**
     * @param scriptRegistry
     *            the scriptRegistry to set
     */
    public final void setScriptRegistry(final ScriptRegistry<BaseScript> scriptRegistry)
    {
        this.scriptRegistry = scriptRegistry;
    }

    protected Script lookupScriptInRegistry(final String locationValue, final ScriptSelectionCondition condition)
    {
        final Script result;
        if (!locationValue.contains("@"))
        {
            // no @ in location value => global script
            final BaseScript baseScript = condition == null ? this.scriptRegistry.getScript(locationValue) : this.scriptRegistry.getScript(
                    locationValue, condition);
            if (baseScript != null)
            {
                result = this.convert(baseScript);
            }
            else
            {
                result = null;
            }
        }
        else
        {
            final String[] fragments = locationValue.split("@");
            if (fragments.length == 2)
            {
                final String scriptName = fragments[0];
                final String subRegistry = fragments[1];

                final BaseScript baseScript = condition == null ? this.scriptRegistry.getScript(scriptName, subRegistry)
                        : this.scriptRegistry.getScript(scriptName, subRegistry, condition);
                if (baseScript != null)
                {
                    result = this.convert(baseScript);
                }
                else
                {
                    result = null;
                }
            }
            else
            {
                throw new IllegalArgumentException("Too many occurences of '@' in location value");
            }
        }

        return result;
    }

    protected ScriptSelectionCondition extractSelectionCondition(final Map<?, ?> parameters)
    {
        final ScriptSelectionCondition multiCondition = this.extractCompositeCondition(parameters);
        final ScriptSelectionCondition versionCondition = this.extractVersionCondition(parameters);
        final ScriptSelectionCondition versionRangeCondition = this.extractVersionRangeCondition(parameters);

        final List<ScriptSelectionCondition> conditions = new ArrayList<ScriptSelectionCondition>();
        if (multiCondition != null)
        {
            conditions.add(multiCondition);
        }
        if (versionCondition != null)
        {
            conditions.add(versionCondition);
        }
        if (versionRangeCondition != null)
        {
            conditions.add(versionRangeCondition);
        }

        final ScriptSelectionCondition condition;
        if (conditions.isEmpty())
        {
            condition = null;
        }
        else if (conditions.size() == 1)
        {
            condition = conditions.get(0);
        }
        else
        {
            condition = new CompositeCondition(conditions);
        }

        return condition;
    }

    protected ScriptSelectionCondition extractVersionCondition(final Map<?, ?> parameters)
    {
        final ScriptSelectionCondition versionCondition;
        if (parameters.containsKey(VERSION))
        {
            final Object versionNumberObj = parameters.get(VERSION);

            if (versionNumberObj instanceof String)
            {
                final VersionNumber versionNumber = new VersionNumber((String) versionNumberObj);

                Boolean community = Boolean.valueOf(this.isCommunityEdition());
                if (parameters.containsKey(COMMUNITY))
                {
                    final Object communityObj = parameters.get(COMMUNITY);
                    if (communityObj == null)
                    {
                        community = null;
                    }
                    else
                    {
                        community = Boolean.valueOf(toBoolean(communityObj));
                    }
                }

                versionCondition = new AppliesForVersionCondition(versionNumber, community);
            }
            else
            {
                versionCondition = null;
            }
        }
        else
        {
            versionCondition = null;
        }
        return versionCondition;
    }

    protected ScriptSelectionCondition extractVersionRangeCondition(final Map<?, ?> parameters)
    {
        final ScriptSelectionCondition versionRangeCondition;
        if (parameters.containsKey(APPLIES_FROM) || parameters.containsKey(APPLIES_TO))
        {
            final Object appliesFromObj = parameters.get(APPLIES_FROM);
            final Object appliesToObj = parameters.get(APPLIES_TO);
            if (appliesFromObj instanceof String || appliesToObj instanceof String)
            {
                final VersionNumber appliesFrom = appliesFromObj instanceof String ? new VersionNumber((String) appliesFromObj) : null;
                final VersionNumber appliesTo = appliesToObj instanceof String ? new VersionNumber((String) appliesToObj) : null;

                final Object appliesFromExclusiveObj = parameters.get(APPLIES_FROM_EXCLUSIVE);
                final Object appliesToExclusiveObj = parameters.get(APPLIES_TO_EXCLUSIVE);

                final boolean appliesFromExclusive = toBoolean(appliesFromExclusiveObj);
                final boolean appliesToExclusive = toBoolean(appliesToExclusiveObj);

                Boolean community = Boolean.valueOf(this.isCommunityEdition());
                if (parameters.containsKey(COMMUNITY))
                {
                    final Object communityObj = parameters.get(COMMUNITY);
                    if (communityObj == null)
                    {
                        community = null;
                    }
                    else
                    {
                        community = Boolean.valueOf(toBoolean(communityObj));
                    }
                }

                versionRangeCondition = new FallsInVersionRangeCondition(appliesFrom, appliesFromExclusive, appliesTo, appliesToExclusive,
                        community);
            }
            else
            {
                versionRangeCondition = null;
            }
        }
        else
        {
            versionRangeCondition = null;
        }
        return versionRangeCondition;
    }

    protected ScriptSelectionCondition extractCompositeCondition(final Map<?, ?> parameters)
    {
        final ScriptSelectionCondition compositeCondition;
        if (parameters.containsKey(CONDITIONS))
        {
            final Object conditionsObj = parameters.get(CONDITIONS);
            if (conditionsObj instanceof Map<?, ?>)
            {
                // just a single condition object => not a real composite
                compositeCondition = this.extractSelectionCondition((Map<?, ?>) conditionsObj);
            }
            else if (conditionsObj instanceof Collection<?>)
            {
                final Collection<ScriptSelectionCondition> conditions = new HashSet<ScriptSelectionCondition>();
                for (final Object element : (Collection<?>) conditionsObj)
                {
                    if (element instanceof Map<?, ?>)
                    {
                        final ScriptSelectionCondition singleCondition = this.extractSelectionCondition((Map<?, ?>) element);
                        if (singleCondition != null)
                        {
                            conditions.add(singleCondition);
                        }
                    }
                    else
                    {
                        throw new IllegalArgumentException("Condition collection element not supported: " + element.toString());
                    }
                }

                if (conditions.isEmpty())
                {
                    // not a condition at all
                    compositeCondition = null;
                }
                else if (conditions.size() == 1)
                {
                    // just a single condition object => not a real composite
                    compositeCondition = conditions.iterator().next();
                }
                else
                {
                    compositeCondition = new CompositeCondition(conditions);
                }
            }
            else
            {
                throw new IllegalArgumentException("Condition object not supported: " + conditionsObj.toString());
            }
        }
        else
        {
            compositeCondition = null;
        }
        return compositeCondition;
    }

    protected static boolean toBoolean(final Object boolParameter)
    {
        boolean result = false;
        if (boolParameter instanceof Boolean)
        {
            result = ((Boolean) boolParameter).booleanValue();
        }
        else if (boolParameter instanceof String)
        {
            result = Boolean.parseBoolean((String) boolParameter);
        }
        return result;
    }

    /**
     * Converts a basic script instance to the expected locator result script instance type.
     *
     * @param baseScript
     *            the base script instance to convert
     * @return the converted script instance
     */
    abstract protected Script convert(BaseScript baseScript);

    protected boolean isCommunityEdition()
    {
        // by default, we treat any unknown environment as "community edition"
        return true;
    };
}
