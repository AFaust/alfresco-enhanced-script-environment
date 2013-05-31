package org.nabucco.alfresco.enhScriptEnv.common.script.registry;

import java.util.Collection;
import java.util.HashSet;

import org.alfresco.util.ParameterCheck;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
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
