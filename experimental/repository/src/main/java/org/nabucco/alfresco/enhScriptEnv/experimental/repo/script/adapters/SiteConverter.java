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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.adapters;

import java.util.Collections;

import org.alfresco.repo.site.script.Site;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.ValueConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ConstructorArgumentAwareProxyFactory;
import org.springframework.aop.framework.ProxyFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class SiteConverter extends AbstractValueInstanceConverter
{

    private static final Logger LOGGER = LoggerFactory.getLogger(SiteConverter.class);

    protected SiteService siteService;

    protected ServiceRegistry serviceRegistry;

    /**
     * @param siteService
     *            the siteService to set
     */
    public final void setSiteService(final SiteService siteService)
    {
        this.siteService = siteService;
    }

    /**
     * @param serviceRegistry
     *            the serviceRegistry to set
     */
    public final void setServiceRegistry(final ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForNashorn(final Object valueInstance, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        final Object result;
        if (valueInstance instanceof Site && (Object.class.equals(expectedClass) || Site.class.isAssignableFrom(expectedClass)))
        {
            LOGGER.debug("Creating subclass-proxy for Rhino-specific bean {}", valueInstance);
            final String shortName = ((Site) valueInstance).getShortName();
            final SiteInfo siteInfo = this.siteService.getSite(shortName);
            final ProxyFactory proxyFactory = new ConstructorArgumentAwareProxyFactory(new Object[] { siteInfo, this.serviceRegistry,
                    this.siteService, DUMMY_SCOPE }, new Class[] { SiteInfo.class, ServiceRegistry.class, SiteService.class,
                    Scriptable.class });

            proxyFactory.addAdvice(new RhinoSpecificBeanInterceptor(globalDelegate));
            proxyFactory.setInterfaces(collectInterfaces(valueInstance, Collections.<Class<?>> emptySet()));
            proxyFactory.setTarget(valueInstance);
            proxyFactory.setProxyTargetClass(true);

            result = proxyFactory.getProxy();
        }
        else
        {
            LOGGER.warn("Conversion of value instance {} to expected class {} is not supported", valueInstance, expectedClass);
            result = null;
        }
        return result;
    }

}
