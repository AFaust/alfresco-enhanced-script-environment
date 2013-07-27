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
package org.nabucco.alfresco.enhScriptEnv.repo.script.registry;

import org.alfresco.repo.jscript.ClasspathScriptLocation;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.registry.RegisterableScript;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ClasspathRegisterableScript implements RegisterableScript<ScriptLocation>, InitializingBean
{

    protected ClassPathResource scriptResource;

    /**
     * 
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "scriptResource", this.scriptResource);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final RegisterableScript<ScriptLocation> o)
    {
        final ScriptLocation otherScriptLocation = o.getScriptInstance();
        final ScriptLocation scriptLocation = this.getScriptInstance();

        final String otherPath = otherScriptLocation.getPath();
        final String path = scriptLocation.getPath();
        return path.compareTo(otherPath);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public ScriptLocation getScriptInstance()
    {
        final String path = this.scriptResource.getPath();
        final String realPath = path.startsWith("classpath:") ? path.substring("classpath:".length()) : path;
        final ClasspathScriptLocation scriptLocation = new ClasspathScriptLocation(realPath);
        return scriptLocation;
    }

    /**
     * @param scriptResource
     *            the scriptResource to set
     */
    public final void setScriptResource(final ClassPathResource scriptResource)
    {
        this.scriptResource = scriptResource;
    }

}
