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
package org.nabucco.alfresco.enhScriptEnv.repo.script.registry;

import org.alfresco.service.cmr.repository.ScriptLocation;
import org.nabucco.alfresco.enhScriptEnv.common.script.registry.RegisterableScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.registry.VersionRegisterableScriptClasspathScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class RepositoryVersionRegisterableScriptClasspathScanner extends VersionRegisterableScriptClasspathScanner<ScriptLocation>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryVersionRegisterableScriptClasspathScanner.class);

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected RegisterableScript<ScriptLocation> getScript(final String resourcePath)
    {
        final RegisterableScript<ScriptLocation> result;
        if (resourcePath.matches("^(classpath(\\*)?:)+.*$"))
        {
            final ClasspathRegisterableScript script = new ClasspathRegisterableScript();
            final String simplePath = resourcePath.replaceAll("classpath(\\*)?:", "");
            final ClassPathResource classpathResource = new ClassPathResource(simplePath);
            script.setScriptResource(classpathResource);
            result = script;
        }
        else
        {
            LOGGER.warn("Can't create script from {} - needs to be a classpath resource", resourcePath);
            result = null;
        }
        return result;
    }

}
