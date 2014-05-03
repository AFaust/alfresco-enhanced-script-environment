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

import java.lang.reflect.Method;
import java.util.Collections;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornValueInstanceConverterRegistry.ValueConverter;
import org.springframework.aop.framework.ConstructorArgumentAwareProxyFactory;
import org.springframework.aop.framework.ProxyFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ScriptNodeConverter extends AbstractValueInstanceConverter
{

    protected ServiceRegistry serviceRegistry;

    public ScriptNodeConverter()
    {
        this.convertableClass = ScriptNode.class;
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
     *
     * {@inheritDoc}
     */
    @Override
    public Object convertToNashorn(final Object valueInstance, final ValueConverter globalDelegate)
    {
        final Object result;
        if (valueInstance instanceof ScriptNode)
        {
            final ProxyFactory proxyFactory = new ConstructorArgumentAwareProxyFactory(new Object[] {
                    ((ScriptNode) valueInstance).getNodeRef(), this.serviceRegistry }, new Class[] { NodeRef.class, ServiceRegistry.class });

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

    protected void preRhinoBeanInvocation(final MethodInvocation invocation) throws Throwable
    {
        final Method method = invocation.getMethod();
        final Object[] arguments = invocation.getArguments();
        @SuppressWarnings("rawtypes")
        final Class[] parameterTypes = method.getParameterTypes();
        if ("createNode".equals(method.getName()) && parameterTypes.length >= 3 && Object.class.equals(parameterTypes[2]))
        {
            if (arguments[2] != null && !(arguments[2] instanceof Scriptable))
            {
                // as long as ScriptNode.createNode is not adapted, we need to supply Scriptable
                arguments[2] = this.convertToRhinoScriptable(arguments[2]);
            }
        }
        else if ("addAspect".equals(method.getName()) && parameterTypes.length == 2)
        {
            if (arguments[1] != null && !(arguments[1] instanceof Scriptable))
            {
                // this may be rather expensive
                // as long as ScriptNode.addAspect is not adapted, we need to supply Scriptable
                arguments[1] = this.convertToRhinoScriptable(arguments[1]);
            }
        }
    }
}
