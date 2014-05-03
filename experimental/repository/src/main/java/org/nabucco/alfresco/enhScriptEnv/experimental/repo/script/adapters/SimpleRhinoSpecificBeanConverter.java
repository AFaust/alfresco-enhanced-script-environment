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
import org.alfresco.repo.jscript.ScriptNode;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornValueInstanceConverterRegistry.ValueConverter;
import org.springframework.aop.framework.ConstructorArgumentAwareProxyFactory;
import org.springframework.aop.framework.ProxyFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class SimpleRhinoSpecificBeanConverter extends AbstractValueInstanceConverter
{

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object convertToNashorn(final Object valueInstance, final ValueConverter globalDelegate)
    {
        final Object result;
        if (valueInstance instanceof ScriptNode)
        {
            // any processor extension without no-argument-constructor will break this - but there should be none
            final ProxyFactory proxyFactory = new ConstructorArgumentAwareProxyFactory(new Object[0], new Class[0]);

            proxyFactory.addAdvice(new RhinoSpecificBeanInterceptor(globalDelegate));
            proxyFactory.setInterfaces(collectInterfaces(valueInstance, Collections.<Class<?>> emptySet()));
            proxyFactory.setTarget(valueInstance);
            proxyFactory.setProxyTargetClass(true);

            result = proxyFactory.getProxy();
        }
        else
        {
            result = null;
        }
        return result;
    }
}
