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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop;

import java.lang.reflect.Method;
import java.util.Map;

import jdk.nashorn.internal.runtime.ScriptRuntime;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ReflectiveMethodInvocation;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class MapLengthFacadeInterceptor implements MethodInterceptor
{

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
        if (Map.class.isAssignableFrom(declaringClass) && invocation instanceof ReflectiveMethodInvocation && arguments.length == 1)
        {
            // TODO Check if we should hide all potential methods of Map-instances by claiming the map contains the name as key and
            // returning UNDEFINED as the value
            switch (method.getName())
            {
            case "get":
            {
                // handling get("size") / get("length") if backing map does not contain explicit value

                if ("size".equals(arguments[0])
                        && (!(invocation.getThis() instanceof Map<?, ?>) || !((Map<?, ?>) invocation.getThis()).containsKey("size")))
                {
                    result = ScriptRuntime.UNDEFINED;
                }
                else if ("length".equals(arguments[0])
                        && (!(invocation.getThis() instanceof Map<?, ?>) || !((Map<?, ?>) invocation.getThis()).containsKey("length")))
                {
                    result = Integer.valueOf(((Map<?, ?>) ((ReflectiveMethodInvocation) invocation).getProxy()).size());
                }
                else
                {
                    result = invocation.proceed();
                }
            }
                break;
            case "containsKey":
            {
                // need to explicitly state that we contain both "size" and "length", otherwise script engine may not retrieve value
                // in case of "size", script engine may fall back on method handle to Map.size
                if ("size".equals(arguments[0]) || "length".equals(arguments[0]))
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
