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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.util.PropertyCheck;
import org.alfresco.util.VersionNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @author Axel Faust
 */
public abstract class VersionRegisterableScriptClasspathScanner<Script> implements InitializingBean
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionRegisterableScriptClasspathScanner.class);

    protected static final String VERSION_PATTERN = "(\\d+(\\.\\d+)*)";
    protected static final String VERSION_RANGE_PATTERN = "(([(\\[])?" + VERSION_PATTERN + ")?-(" + VERSION_PATTERN + "([)\\]])?)?";
    protected static final String EDITION_PATTERN = "(enterprise|general|community)";
    protected static final String SOURCE_FILE_PATTERN = "([^.:|<>/\"*?]+\\.)+js";

    protected List<String> rootResourcePatterns;

    protected ResourcePatternResolver resourcePatternResolver;

    protected ScriptRegistry<Script> scriptRegistry;

    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "resourcePatternResolver", this.resourcePatternResolver);
        PropertyCheck.mandatory(this, "scriptRegistry", this.scriptRegistry);
        PropertyCheck.mandatory(this, "rootResourcePatterns", this.rootResourcePatterns);

        this.scan();
    }

    /**
     * @param rootResourcePatterns
     *            the rootResourcePatterns to set
     */
    public final void setRootResourcePatterns(final List<String> rootResourcePatterns)
    {
        this.rootResourcePatterns = rootResourcePatterns;
    }

    /**
     * @param resourcePatternResolver
     *            the resourcePatternResolver to set
     */
    public final void setResourcePatternResolver(final ResourcePatternResolver resourcePatternResolver)
    {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * @param scriptRegistry
     *            the scriptRegistry to set
     */
    public final void setScriptRegistry(final ScriptRegistry<Script> scriptRegistry)
    {
        this.scriptRegistry = scriptRegistry;
    }

    /**
     * Performs a source file scan operation over the configured root resource patterns
     *
     * @throws IOException
     *             if any exception occurs handling the resources
     */
    protected void scan() throws IOException
    {
        final Collection<String> patternStack = new ArrayList<String>();
        final VersionRegisterableScriptAdapter<Script> dataContainer = new VersionRegisterableScriptAdapter<Script>();

        for (final String rootResourcePattern : this.rootResourcePatterns)
        {
            this.scanNextLevel(rootResourcePattern, patternStack, dataContainer, null);
        }
    }

    /**
     * Scans the next level in a resource path for script source files
     *
     * @param resourcePattern
     *            the root resource pattern
     * @param patternStack
     *            the stack of evaluated token patterns
     * @param versionDataContainer
     *            the container of collected version data
     * @param subRegistry
     *            the sub-registry to register the script in or {@code null} if no sub-registry is to be used
     * @throws IOException
     *             if any exception occurs handling the resource
     */
    protected void scanNextLevel(final String resourcePattern, final Collection<String> patternStack,
            final VersionRegisterableScriptAdapter<Script> versionDataContainer, final String subRegistry) throws IOException
    {
        final Resource[] resources = this.resourcePatternResolver.getResources(resourcePattern + "/*");
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

    /**
     * Constructs an environment specific script instance from a resource path
     *
     * @param resourcePath
     *            the path to the script source file
     * @return the script instance
     * @throws IOException
     *             if any exception occurs handling the resource
     */
    protected abstract RegisterableScript<Script> getScript(final String resourcePath) throws IOException;

    /**
     * Matches and processes an actual source file
     *
     * @param resourcePattern
     *            the resource pattern for the current level
     * @param patternStack
     *            the stack of patterns evaluated thus far
     * @param versionDataContainer
     *            the container for collection version information
     * @param subRegistry
     *            the sub-registry to register the script in or {@code null} if no sub-registry is to be used
     * @param resource
     *            the resource representing the source file
     * @throws IOException
     *             if any exception occurs handling the resource
     */
    protected void matchSourceFile(final String resourcePattern, final Collection<String> patternStack,
            final VersionRegisterableScriptAdapter<Script> versionDataContainer, final String subRegistry, final Resource resource)
            throws IOException
    {
        final String scriptName = resource.getFilename();
        final String simpleScriptName = scriptName.endsWith(".js") ? scriptName.substring(0, scriptName.length() - 3) : scriptName;

        LOGGER.debug("Matched script file {}", scriptName);

        final RegisterableScript<Script> script = this.getScript(resourcePattern + "/" + scriptName);

        if (script != null)
        {

            // copy the script adapter
            final VersionRegisterableScriptAdapter<Script> scriptAdapter = new VersionRegisterableScriptAdapter<Script>();
            scriptAdapter.setForCommunity(versionDataContainer.isForCommunity());
            scriptAdapter.setAppliesFrom(versionDataContainer.getAppliesFrom());
            scriptAdapter.setAppliesTo(versionDataContainer.getAppliesTo());
            scriptAdapter.setAppliesFromExclusive(versionDataContainer.isAppliesFromExclusive());
            scriptAdapter.setAppliesToExclusive(versionDataContainer.isAppliesToExclusive());
            scriptAdapter.setVersion(versionDataContainer.getVersion());

            scriptAdapter.setAdaptedScript(script);

            if (subRegistry == null || subRegistry.trim().length() == 0)
            {
                this.scriptRegistry.registerScript(simpleScriptName, scriptAdapter);
                LOGGER.debug("Registered script {} into script registry {}", scriptAdapter, this.scriptRegistry);
            }
            else
            {
                this.scriptRegistry.registerScript(simpleScriptName, subRegistry, scriptAdapter);
                LOGGER.debug("Registered script {} into sub-registry {} of script registry {}", new Object[] { scriptAdapter, subRegistry,
                        this.scriptRegistry });
            }
        }
    }

    /**
     * Matches and processes a folder level defining a specific edition of alfresco a script is applicable to.
     *
     * @param resourcePattern
     *            the resource pattern for the current level
     * @param patternStack
     *            the stack of patterns evaluated thus far
     * @param versionDataContainer
     *            the container for collection version information
     * @param subRegistry
     *            the sub-registry to register the script in or {@code null} if no sub-registry is to be used
     * @param fileName
     *            the name of the file currently evaluated
     * @throws IOException
     *             if any exception occurs handling the resource
     */
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

    /**
     * Matches and processes a folder level defining a version a script is provided in.
     *
     * @param resourcePattern
     *            the resource pattern for the current level
     * @param patternStack
     *            the stack of patterns evaluated thus far
     * @param versionDataContainer
     *            the container for collection version information
     * @param subRegistry
     *            the sub-registry to register the script in or {@code null} if no sub-registry is to be used
     * @param fileName
     *            the name of the file currently evaluated
     * @throws IOException
     *             if any exception occurs handling the resource
     */
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

    /**
     * Matches and processes a folder level defining a range of versions a script is applicable to.
     *
     * @param resourcePattern
     *            the resource pattern for the current level
     * @param patternStack
     *            the stack of patterns evaluated thus far
     * @param versionDataContainer
     *            the container for collection version information
     * @param subRegistry
     *            the sub-registry to register the script in or {@code null} if no sub-registry is to be used
     * @param fileName
     *            the name of the file currently evaluated
     * @throws IOException
     *             if any exception occurs handling the resource
     */
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
        versionDataContainer.setAppliesToExclusive("]".equals(upperBoundExclusivity));
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
