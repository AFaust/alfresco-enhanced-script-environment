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
package de.axelfaust.alfresco.enhScriptEnv.common.script.aop;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ReflectiveMethodInvocation;

/**
 * @author Axel Faust
 */
public class LengthFacadeInterceptor implements MethodInterceptor
{

    private final Object defaultNoSuchPropertyValue;

    private final boolean claimSizeProperty;

    public LengthFacadeInterceptor(final Object defaultNoSuchPropertyValue)
    {
        this(defaultNoSuchPropertyValue, true);
    }

    public LengthFacadeInterceptor(final Object defaultNoSuchPropertyValue, final boolean claimSizeProperty)
    {
        this.defaultNoSuchPropertyValue = defaultNoSuchPropertyValue;
        this.claimSizeProperty = claimSizeProperty;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable
    {
        final Object result;

        final Method method = invocation.getMethod();
        final Class<?> declaringClass = method.getDeclaringClass();

        final Object[] arguments = invocation.getArguments();
        if ((Map.class.isAssignableFrom(declaringClass) || List.class.isAssignableFrom(declaringClass) || Collection.class
                .isAssignableFrom(declaringClass)) && invocation instanceof ReflectiveMethodInvocation && arguments.length == 1)
        {
            switch (MapMethodName.methodLiteralOf(method.getName()))
            {
                case GET:
                {
                    // handling get("size") / get("length") if backing map/collection does not contain explicit value
                    final boolean mapDoesNotContainSize = !(invocation.getThis() instanceof Map<?, ?>)
                            || !((Map<?, ?>) invocation.getThis()).containsKey("size");
                    final boolean mapDoesNotContainLength = !(invocation.getThis() instanceof Map<?, ?>)
                            || !((Map<?, ?>) invocation.getThis()).containsKey("length");

                    if (this.claimSizeProperty && "size".equals(arguments[0])
                            && (mapDoesNotContainSize || invocation.getThis() instanceof Collection<?>))
                    {
                        result = this.defaultNoSuchPropertyValue;
                    }
                    else if ("length".equals(arguments[0]) && (mapDoesNotContainLength || invocation.getThis() instanceof Collection<?>))
                    {
                        if (invocation.getThis() instanceof Collection<?>)
                        {
                            result = Integer.valueOf(((Collection<?>) ((ReflectiveMethodInvocation) invocation).getProxy()).size());
                        }
                        else
                        {
                            result = Integer.valueOf(((Map<?, ?>) ((ReflectiveMethodInvocation) invocation).getProxy()).size());
                        }
                    }
                    else
                    {
                        result = invocation.proceed();
                    }
                }
                    break;
                case CONTAINSKEY:
                {
                    // need to explicitly state that we contain both "size" and "length", otherwise script engine may not retrieve value
                    // in case of "size", script engine may fall back on method handle to Map/Collection.size
                    if ((this.claimSizeProperty && "size".equals(arguments[0])) || "length".equals(arguments[0]))
                    {
                        result = Boolean.TRUE;
                    }
                    else
                    {
                        result = invocation.proceed();
                    }
                }
                    break;
                default:
                    result = invocation.proceed();
            }
        }
        else
        {
            result = invocation.proceed();
        }

        return result;
    }
}
