/*
 * Copyright 2015 PRODYNA AG
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
package org.nabucco.alfresco.enhScriptEnv.common.webscripts.processor;

import java.net.URL;

import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript.CommonReferencePath;
import org.nabucco.alfresco.enhScriptEnv.common.script.locator.AbstractRelativeResolvingScriptLocator;
import org.springframework.extensions.webscripts.ScriptLoader;

/**
 * A script locator able to import scripts from the classpath of the web application. This implementation is able to resolve relative script
 * locations when supplied with a execution context.
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ClasspathScriptLocator extends AbstractRelativeResolvingScriptLocator<ScriptContentAdapter>
{
    protected ScriptLoader scriptLoader;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "scriptLoader", this.scriptLoader);
        super.afterPropertiesSet();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected String getReferencePath(final ReferenceScript referenceLocation)
    {
        final String referencePath = referenceLocation.getReferencePath(CommonReferencePath.CLASSPATH);
        return referencePath;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected ScriptContentAdapter loadScript(final String absolutePath)
    {
        String absoluteClasspath = absolutePath.startsWith("/") ? absolutePath : "/" + absolutePath;
        final ScriptContentAdapter result;
        URL scriptResource = this.getClass().getClassLoader().getResource(absoluteClasspath);
        if (scriptResource == null && absoluteClasspath.startsWith("/"))
        {
            // Some classloaders prefer alfresco/foo to /alfresco/foo, try that
            absoluteClasspath = absoluteClasspath.substring(1);
            scriptResource = this.getClass().getClassLoader().getResource(absoluteClasspath);
        }

        if (scriptResource != null)
        {
            result = new ScriptContentAdapter(new ClasspathScriptContent(absoluteClasspath), this.scriptLoader);
        }
        else
        {
            result = null;
        }
        return result;
    }

    /**
     * @param scriptLoader
     *            the scriptLoader to set
     */
    public final void setScriptLoader(final ScriptLoader scriptLoader)
    {
        this.scriptLoader = scriptLoader;
    }

}