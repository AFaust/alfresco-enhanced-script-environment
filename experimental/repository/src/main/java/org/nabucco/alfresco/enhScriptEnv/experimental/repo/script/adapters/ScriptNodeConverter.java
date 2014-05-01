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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.alfresco.repo.jscript.Scopeable;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornValueInstanceConverterRegistry.ValueConverter;
import org.springframework.aop.framework.ConstructorArgumentAwareProxyFactory;
import org.springframework.aop.framework.ProxyFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ScriptNodeConverter extends AbstractValueInstanceConverter
{
    protected static final Scriptable DUMMY_SCOPE;
    static
    {
        final Context cx = Context.enter();
        try
        {
            DUMMY_SCOPE = cx.initStandardObjects(null, true);
            DUMMY_SCOPE.delete("Packages");
            DUMMY_SCOPE.delete("getClass");
            DUMMY_SCOPE.delete("java");
            ((NativeObject) DUMMY_SCOPE).sealObject();
        }
        finally
        {
            Context.exit();
        }
    }

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

            proxyFactory.addAdvice(new MethodInterceptor()
            {
                /**
                 *
                 * {@inheritDoc}
                 */
                @Override
                public Object invoke(final MethodInvocation invocation) throws Throwable
                {
                    // need to make sure we have a context
                    Context.enter();
                    try
                    {
                        // also need to make sure we have a scope
                        final Object thisObj = invocation.getThis();
                        final Method method = invocation.getMethod();
                        if (thisObj instanceof Scopeable && !Scopeable.class.equals(method.getDeclaringClass()))
                        {
                            ((Scopeable) thisObj).setScope(DUMMY_SCOPE);
                        }

                        final Object[] arguments = invocation.getArguments();
                        final Class[] parameterTypes = method.getParameterTypes();
                        if ("createNode".equals(method.getName()) && parameterTypes.length >= 3 && Object.class.equals(parameterTypes[2]))
                        {
                            // TODO: map 3rd parameter to Scriptable
                        }
                        else if ("addAspect".equals(method.getName()) && parameterTypes.length == 2)
                        {
                            // TODO: map 2nd parameter to Scriptable
                        }

                        final Object result = invocation.proceed();
                        final Object convertedResult = globalDelegate.convertToNashorn(result);
                        return convertedResult;
                    }
                    finally
                    {
                        Context.exit();
                    }
                }
            });
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

    @SuppressWarnings("rawtypes")
    protected static Class[] collectInterfaces(final Object source, final Collection<Class<?>> predefinedInterfaces)
    {
        final Set<Class<?>> interfaces = new HashSet<Class<?>>(predefinedInterfaces);
        Class<?> implClass = source.getClass();
        while (!Object.class.equals(implClass))
        {
            interfaces.addAll(Arrays.asList(implClass.getInterfaces()));

            implClass = implClass.getSuperclass();
        }

        final Class[] interfacesArr = interfaces.toArray(new Class[0]);
        return interfacesArr;
    }
}
