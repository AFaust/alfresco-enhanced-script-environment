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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.util.PropertyCheck;
import org.alfresco.util.VersionNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class VersionRegisterableScriptClasspathScanner<Script> implements InitializingBean, ApplicationContextAware
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionRegisterableScriptClasspathScanner.class);

    protected static final String VERSION_PATTERN = "(\\d+(\\.\\d+)*)";
    protected static final String VERSION_RANGE_PATTERN = "(([(\\[])?" + VERSION_PATTERN + ")?-(" + VERSION_PATTERN + "([)\\]])?)?";
    protected static final String EDITION_PATTERN = "(enterprise|general|community)";
    protected static final String SOURCE_FILE_PATTERN = "([^.:|<>/\"*?]+\\.)+js";

    protected String rootResourcePattern;

    protected ApplicationContext applicationContext;

    protected ScriptRegistry<Script> scriptRegistry;

    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "applicationContext", this.applicationContext);
        PropertyCheck.mandatory(this, "scriptRegistry", this.scriptRegistry);
        PropertyCheck.mandatory(this, "rootResourcePattern", this.rootResourcePattern);

        this.scan();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setApplicationContext(final ApplicationContext applicationContext)
    {
        this.applicationContext = applicationContext;
    }

    /**
     * @param rootResourcePattern
     *            the rootResourcePattern to set
     */
    public final void setRootResourcePattern(final String rootResourcePattern)
    {
        this.rootResourcePattern = rootResourcePattern;
    }

    /**
     * @param scriptRegistry
     *            the scriptRegistry to set
     */
    public final void setScriptRegistry(final ScriptRegistry<Script> scriptRegistry)
    {
        this.scriptRegistry = scriptRegistry;
    }

    protected void scan() throws IOException
    {
        final Collection<String> patternStack = new ArrayList<String>();
        final VersionRegisterableScriptAdapter<Script> dataContainer = new VersionRegisterableScriptAdapter<Script>();

        this.scanNextLevel(this.rootResourcePattern, patternStack, dataContainer, null);
    }

    protected void scanNextLevel(final String resourcePattern, final Collection<String> patternStack,
            final VersionRegisterableScriptAdapter<Script> versionDataContainer, final String subRegistry) throws IOException
    {
        final Resource[] resources = this.applicationContext.getResources(resourcePattern + "/*");
        for (final Resource resource : resources)
        {
            final String fileName = resource.getFilename();

            LOGGER.debug("Matching next level element name {} below resource", fileName, resourcePattern);

            boolean dealtWith = false;

            if (!(patternStack.contains(VERSION_PATTERN) || patternStack.contains(VERSION_RANGE_PATTERN)))
            {
                if (fileName.matches(VERSION_PATTERN))
                {
                    this.matchVersion(resourcePattern, patternStack, versionDataContainer, subRegistry, fileName);
                    dealtWith = true;
                }
                else if (fileName.matches(VERSION_RANGE_PATTERN))
                {
                    this.matchVersionRange(resourcePattern, patternStack, versionDataContainer, subRegistry, fileName);
                    dealtWith = true;
                }
            }

            if (!dealtWith && !patternStack.contains(EDITION_PATTERN))
            {
                if (fileName.matches(EDITION_PATTERN))
                {
                    this.matchEdition(resourcePattern, patternStack, versionDataContainer, subRegistry, fileName);
                    dealtWith = true;
                }
            }

            if (!dealtWith && !patternStack.contains(SOURCE_FILE_PATTERN))
            {
                if (fileName.matches(SOURCE_FILE_PATTERN))
                {
                    this.matchSourceFile(resourcePattern, patternStack, versionDataContainer, subRegistry, resource);
                    dealtWith = true;
                }
            }

            if (!dealtWith && subRegistry == null)
            {
                this.scanNextLevel(resourcePattern + "/" + fileName, patternStack, versionDataContainer, fileName);
                dealtWith = true;
            }

            if (!dealtWith)
            {
                LOGGER.warn("Next level element name {} below resource {} could not be matched to a valid fragment - back-stepping",
                        fileName, resourcePattern);
            }
        }
    }

    protected abstract RegisterableScript<Script> getScript(final Resource resource) throws IOException;

    protected void matchSourceFile(final String resourcePattern, final Collection<String> patternStack,
            final VersionRegisterableScriptAdapter<Script> versionDataContainer, final String subRegistry, final Resource resource)
            throws IOException
    {
        final String scriptName = resource.getFilename();

        LOGGER.debug("Matched script file {}", scriptName);

        final RegisterableScript<Script> script = this.getScript(resource);

        if (script != null)
        {

            // copy the script adapter
            final VersionRegisterableScriptAdapter<Script> scriptAdapter = new VersionRegisterableScriptAdapter<Script>();
            scriptAdapter.setForCommunity(versionDataContainer.isForCommunity());
            scriptAdapter.setAppliesFrom(scriptAdapter.getAppliesFrom());
            scriptAdapter.setAppliesTo(scriptAdapter.getAppliesTo());
            scriptAdapter.setAppliesFromExclusive(scriptAdapter.isAppliesFromExclusive());
            scriptAdapter.setAppliesToExclusive(scriptAdapter.isAppliesToExclusive());
            scriptAdapter.setVersion(scriptAdapter.getVersion());

            scriptAdapter.setAdaptedScript(script);

            if (subRegistry == null || subRegistry.trim().length() == 0)
            {
                this.scriptRegistry.registerScript(scriptName, scriptAdapter);
                LOGGER.info("Registered script {} into script registry {}", scriptAdapter, this.scriptRegistry);
            }
            else
            {
                this.scriptRegistry.registerScript(scriptName, subRegistry, scriptAdapter);
                LOGGER.info("Registered script {} into sub-registry {} of script registry {}", new Object[] { scriptAdapter, subRegistry,
                        this.scriptRegistry });
            }
        }
    }

    protected void matchEdition(final String resourcePattern, final Collection<String> patternStack,
            final VersionRegisterableScriptAdapter<Script> versionDataContainer, final String subRegistry, final String fileName)
            throws IOException
    {
        final Boolean community;
        if ("community".equalsIgnoreCase(fileName))
        {
            community = Boolean.TRUE;
            LOGGER.debug("Matched COMMUNITY edition fragment");
        }
        else if ("enterprise".equalsIgnoreCase(fileName))
        {
            community = Boolean.FALSE;
            LOGGER.debug("Matched ENTERPRISE edition fragment");
        }
        else
        {
            community = null;
            LOGGER.debug("Matched edition agnostic fragment");
        }

        versionDataContainer.setForCommunity(community);
        patternStack.add(EDITION_PATTERN);
        try
        {
            this.scanNextLevel(resourcePattern + "/" + fileName, patternStack, versionDataContainer, subRegistry);
        }
        finally
        {
            versionDataContainer.setForCommunity(null);
            patternStack.remove(EDITION_PATTERN);
        }
    }

    protected void matchVersion(final String resourcePattern, final Collection<String> patternStack,
            final VersionRegisterableScriptAdapter<Script> versionDataContainer, final String subRegistry, final String fileName)
            throws IOException
    {
        final String versionStr = fileName;

        LOGGER.debug("Matched version {} fragment", versionStr);

        versionDataContainer.setVersion(versionStr);
        patternStack.add(VERSION_PATTERN);
        try
        {
            this.scanNextLevel(resourcePattern + "/" + fileName, patternStack, versionDataContainer, subRegistry);
        }
        finally
        {
            versionDataContainer.setVersion((VersionNumber) null);
            patternStack.remove(VERSION_PATTERN);
        }
    }

    protected void matchVersionRange(final String resourcePattern, final Collection<String> patternStack,
            final VersionRegisterableScriptAdapter<Script> versionDataContainer, final String subRegistry, final String fileName)
            throws IOException
    {
        LOGGER.debug("Matched version range {} fragment", fileName);

        final Matcher matcher = Pattern.compile(VERSION_RANGE_PATTERN).matcher(fileName);
        matcher.find();

        final String lowerBoundExclusivity = matcher.group(2);
        final String lowerVersion = matcher.group(3);
        final String upperVersion = matcher.group(6);
        final String upperBoundExclusivity = matcher.group(8);

        versionDataContainer.setAppliesFromExclusive("[".equals(lowerBoundExclusivity));
        versionDataContainer.setAppliesToExclusive("[".equals(upperBoundExclusivity));
        if (lowerVersion != null && lowerVersion.length() != 0)
        {
            versionDataContainer.setAppliesFrom(lowerVersion);
        }
        if (upperVersion != null && upperVersion.length() != 0)
        {
            versionDataContainer.setAppliesTo(upperVersion);
        }

        patternStack.add(VERSION_RANGE_PATTERN);
        try
        {
            this.scanNextLevel(resourcePattern + "/" + fileName, patternStack, versionDataContainer, subRegistry);
        }
        finally
        {
            versionDataContainer.setAppliesFromExclusive(false);
            versionDataContainer.setAppliesToExclusive(false);
            versionDataContainer.setAppliesFrom((VersionNumber) null);
            versionDataContainer.setAppliesTo((VersionNumber) null);
            patternStack.remove(VERSION_RANGE_PATTERN);
        }
    }
}
