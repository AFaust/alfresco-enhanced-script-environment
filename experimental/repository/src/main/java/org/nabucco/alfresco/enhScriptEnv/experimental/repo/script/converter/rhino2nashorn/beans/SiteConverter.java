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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.rhino2nashorn.beans;

import java.lang.reflect.Field;

import org.alfresco.repo.site.script.Site;
import org.alfresco.scripts.ScriptException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.util.Pair;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.rhino2nashorn.SimpleJSObjectSubClassProxyConverter;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class SiteConverter extends SimpleJSObjectSubClassProxyConverter
{

    public SiteConverter()
    {
        this.javaBaseClass = Site.class;
        this.confidence = HIGHEST_CONFIDENCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Pair<Class<?>[], Object[]> determineForScriptProxyConstructorParameters(final Object value, final Class<?> expectedClass)
    {
        final Pair<Class<?>[], Object[]> result;

        if (value instanceof Site)
        {
            try
            {
                // reflection is simpler and computationally cheaper then re-retrieving siteInfo
                // potentially no difference for services, but for sake of uniformity also handled reflectively
                final Field siteInfoField = Site.class.getDeclaredField("siteInfo");
                final Field siteServiceField = Site.class.getDeclaredField("siteService");
                final Field serviceRegistryField = Site.class.getDeclaredField("serviceRegistry");

                siteInfoField.setAccessible(true);
                siteServiceField.setAccessible(true);
                serviceRegistryField.setAccessible(true);

                final Class<?>[] ctorParameterTypes = new Class<?>[] { SiteInfo.class, ServiceRegistry.class, SiteService.class,
                        Scriptable.class };
                final Object[] ctorParameters = new Object[] { siteInfoField.get(value), serviceRegistryField.get(value),
                        siteServiceField.get(value), DUMMY_SCOPE };

                result = new Pair<>(ctorParameterTypes, ctorParameters);
            }
            catch (final NoSuchFieldException ex)
            {
                throw new ScriptException("Failed to determine constructor parameters for Site sub-class proxy", ex);
            }
            catch (final IllegalAccessException ex)
            {
                throw new ScriptException("Failed to determine constructor parameters for Site sub-class proxy", ex);
            }
        }
        else
        {
            result = super.determineForScriptProxyConstructorParameters(value, expectedClass);
        }

        return result;
    }

}
