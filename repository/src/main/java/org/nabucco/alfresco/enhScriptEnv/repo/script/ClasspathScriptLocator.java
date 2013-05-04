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
package org.nabucco.alfresco.enhScriptEnv.repo.script;

import java.net.URL;

import org.alfresco.repo.jscript.ClasspathScriptLocation;
import org.alfresco.repo.web.scripts.RepositoryScriptProcessor;
import org.nabucco.alfresco.enhScriptEnv.common.script.AbstractRelativeResolvingScriptLocator;

/**
 * A script locator able to import scripts from the classpath of the web application. This implementation is able to resolve relative script
 * locations when supplied with a execution context.
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ClasspathScriptLocator extends AbstractRelativeResolvingScriptLocator<ScriptLocationAdapter>
{
    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected String getReferencePath(final ScriptLocationAdapter referenceLocation)
    {
        final String referencePath;
        if (referenceLocation.getScriptLocation() instanceof ClasspathScriptLocation)
        {
            // we know this gives us the path relative to the classpath

            // TODO: would be nice for a proper getter (which seems to have been removed recently)
            referencePath = referenceLocation.toString();
        }
        else if (referenceLocation.getScriptLocation().getClass().getDeclaringClass().isAssignableFrom(RepositoryScriptProcessor.class))
        {
            // awkward check for private RepositoryScriptLocation which encapsulates a ScriptContent instance
            // we know this gives us the classpath*:-prefixed path IF a classpath-based ScriptContent is wrapped
            // (ClasspathScriptLocation)

            // TODO: would be nice for a proper getter
            referencePath = referenceLocation.toString();
        }
        else
        {
            referencePath = null;
        }
        return referencePath;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected ScriptLocationAdapter loadScript(final String absolutePath)
    {
        String absoluteClasspath = absolutePath.startsWith("/") ? absolutePath : "/" + absolutePath;
        final ScriptLocationAdapter result;
        URL scriptResource = getClass().getClassLoader().getResource(absoluteClasspath);
        if (scriptResource == null && absoluteClasspath.startsWith("/"))
        {
            // Some classloaders prefer alfresco/foo to /alfresco/foo, try that
            absoluteClasspath = absoluteClasspath.substring(1);
            scriptResource = getClass().getClassLoader().getResource(absoluteClasspath);
        }

        if (scriptResource != null)
        {
            result = new ScriptLocationAdapter(new ClasspathScriptLocation(absoluteClasspath));
        }
        else
        {
            result = null;
        }
        return result;
    }

}