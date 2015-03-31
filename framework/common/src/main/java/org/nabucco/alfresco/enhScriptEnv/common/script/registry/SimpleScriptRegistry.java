/*
 * Copyright 2014 PRODYNA AG
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.alfresco.util.ParameterCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provies a simple, thread-safe but transactionally unsafe script registry implementation. It may be used as a global,
 * deployment-based registry that does not experience updates at runtime after startup of the server.
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class SimpleScriptRegistry<Script> implements ScriptRegistry<Script>
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleScriptRegistry.class);

    protected final Map<String, Collection<RegisterableScript<Script>>> globalScripts = new HashMap<String, Collection<RegisterableScript<Script>>>();
    protected final Map<String, Map<String, Collection<RegisterableScript<Script>>>> subRegistries = new HashMap<String, Map<String, Collection<RegisterableScript<Script>>>>();

    protected final ReadWriteLock globalScriptsLock = new ReentrantReadWriteLock(true);
    protected final ReadWriteLock subRegistriesLock = new ReentrantReadWriteLock(true);

    /**
     * {@inheritDoc}
     */
    @Override
    public Script getScript(final String scriptName)
    {
        ParameterCheck.mandatoryString("scriptName", scriptName);

        final List<RegisterableScript<Script>> scripts = getScriptsByName(scriptName);
        Collections.sort(scripts);
        Collections.reverse(scripts);

        LOGGER.debug("{} global scripts found for name {} without restricting condition", Integer.valueOf(scripts.size()), scriptName);
        final Script result = !scripts.isEmpty() ? scripts.iterator().next().getScriptInstance() : null;
        LOGGER.debug("Global script {} selected for name {} without condition", result, scriptName);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Script getScript(final String scriptName, final ScriptSelectionCondition condition)
    {
        ParameterCheck.mandatoryString("scriptName", scriptName);
        ParameterCheck.mandatory("condition", condition);

        final List<RegisterableScript<Script>> scripts = getScriptsByName(scriptName);
        Collections.sort(scripts);
        Collections.reverse(scripts);

        LOGGER.debug("{} global scripts found for name {} before selection with restricting condition {}",
                new Object[] { Integer.valueOf(scripts.size()), scriptName, condition });

        Script result = null;
        for (final RegisterableScript<Script> script : scripts)
        {
            if (condition.matches(script))
            {
                result = script.getScriptInstance();
                break;
            }
        }

        LOGGER.debug("Global script {} selected for name {} and condition {}", new Object[] { result, scriptName, condition });

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Script getScript(final String scriptName, final String subRegistry)
    {
        ParameterCheck.mandatoryString("scriptName", scriptName);
        ParameterCheck.mandatoryString("subRegistry", subRegistry);

        final List<RegisterableScript<Script>> scripts = getScriptsByName(scriptName, subRegistry);
        Collections.sort(scripts);
        Collections.reverse(scripts);

        LOGGER.debug("{} scripts found for name {} in registry {} without restricting condition",
                new Object[] { Integer.valueOf(scripts.size()), scriptName, subRegistry });
        final Script result = !scripts.isEmpty() ? scripts.iterator().next().getScriptInstance() : null;
        LOGGER.debug("Script {} from registry {} selected for name {} without condition", new Object[] { result, subRegistry, scriptName });
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Script getScript(final String scriptName, final String subRegistry, final ScriptSelectionCondition condition)
    {
        ParameterCheck.mandatoryString("scriptName", scriptName);
        ParameterCheck.mandatoryString("subRegistry", subRegistry);
        ParameterCheck.mandatory("condition", condition);

        final List<RegisterableScript<Script>> scripts = getScriptsByName(scriptName, subRegistry);
        Collections.sort(scripts);
        Collections.reverse(scripts);

        LOGGER.debug("{} scripts found for name {} in registry {} before selection with restricting condition {}",
                new Object[] { Integer.valueOf(scripts.size()), scriptName, subRegistry, condition });

        Script result = null;
        for (final RegisterableScript<Script> script : scripts)
        {
            if (condition.matches(script))
            {
                result = script.getScriptInstance();
                break;
            }
        }

        LOGGER.debug("Script {} from registry {} selected for name {} and condition {}", new Object[] { result, subRegistry, scriptName,
                condition });

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerScript(final String scriptName, final RegisterableScript<Script> script)
    {
        ParameterCheck.mandatoryString("scriptName", scriptName);
        ParameterCheck.mandatory("script", script);

        this.globalScriptsLock.writeLock().lock();
        try
        {
            Collection<RegisterableScript<Script>> scriptsDirect = this.globalScripts.get(scriptName);
            if (scriptsDirect == null)
            {
                scriptsDirect = new LinkedHashSet<RegisterableScript<Script>>();
                this.globalScripts.put(scriptName, scriptsDirect);
            }
            scriptsDirect.add(script);
        }
        finally
        {
            this.globalScriptsLock.writeLock().unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerScript(final String scriptName, final String subRegistry, final RegisterableScript<Script> script)
    {
        ParameterCheck.mandatoryString("scriptName", scriptName);
        ParameterCheck.mandatoryString("subRegistry", subRegistry);
        ParameterCheck.mandatory("script", script);

        this.subRegistriesLock.writeLock().lock();
        try
        {
            Map<String, Collection<RegisterableScript<Script>>> subRegistryScripts = this.subRegistries.get(subRegistry);
            if (subRegistryScripts == null)
            {
                subRegistryScripts = new HashMap<String, Collection<RegisterableScript<Script>>>();
                this.subRegistries.put(subRegistry, subRegistryScripts);
            }

            Collection<RegisterableScript<Script>> scriptsDirect = subRegistryScripts.get(scriptName);
            if (scriptsDirect == null)
            {
                scriptsDirect = new LinkedHashSet<RegisterableScript<Script>>();
                subRegistryScripts.put(scriptName, scriptsDirect);
            }
            scriptsDirect.add(script);
        }
        finally
        {
            this.subRegistriesLock.writeLock().unlock();
        }
    }

    protected List<RegisterableScript<Script>> getScriptsByName(final String scriptName)
    {
        final List<RegisterableScript<Script>> scripts;
        this.globalScriptsLock.readLock().lock();
        try
        {
            final Collection<RegisterableScript<Script>> scriptsDirect = this.globalScripts.get(scriptName);
            // shallow copy to avoid modification
            scripts = scriptsDirect != null ? new ArrayList<RegisterableScript<Script>>(scriptsDirect) : Collections
                    .<RegisterableScript<Script>> emptyList();
        }
        finally
        {
            this.globalScriptsLock.readLock().unlock();
        }
        return scripts;
    }

    protected List<RegisterableScript<Script>> getScriptsByName(final String scriptName, final String subRegistry)
    {
        final List<RegisterableScript<Script>> scripts;
        this.subRegistriesLock.readLock().lock();
        try
        {
            final Map<String, Collection<RegisterableScript<Script>>> subRegistryScripts = this.subRegistries.get(subRegistry);
            if (subRegistryScripts != null)
            {
                final Collection<RegisterableScript<Script>> scriptsDirect = subRegistryScripts.get(scriptName);
                // shallow copy to avoid modification
                scripts = scriptsDirect != null ? new ArrayList<RegisterableScript<Script>>(scriptsDirect) : Collections
                        .<RegisterableScript<Script>> emptyList();
            }
            else
            {
                scripts = Collections.<RegisterableScript<Script>> emptyList();
            }
        }
        finally
        {
            this.subRegistriesLock.readLock().unlock();
        }
        return scripts;
    }

}
