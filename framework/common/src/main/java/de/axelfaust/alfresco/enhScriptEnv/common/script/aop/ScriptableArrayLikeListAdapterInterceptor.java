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
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.Scriptable;
import org.springframework.aop.ProxyMethodInvocation;

/**
 * @author Axel Faust
 */
public class ScriptableArrayLikeListAdapterInterceptor implements MethodInterceptor
{

    private static final ScriptableArrayLikeListAdapterInterceptor INSTANCE = new ScriptableArrayLikeListAdapterInterceptor();

    public static ScriptableArrayLikeListAdapterInterceptor getInstance()
    {
        return INSTANCE;
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
        if (Scriptable.class.equals(declaringClass) && invocation instanceof ProxyMethodInvocation)
        {
            final ProxyMethodInvocation pInvocation = (ProxyMethodInvocation) invocation;
            final Object proxy = pInvocation.getProxy();

            final String methodName = method.getName();
            final Object[] arguments = invocation.getArguments();

            // String-switch not supported in Java < 8
            switch (ScriptableMethodName.methodLiteralOf(methodName))
            {
                case GET:
                    if (proxy instanceof List<?> && arguments.length > 0 && arguments[0] instanceof Integer)
                    {
                        final int idx = ((Integer) arguments[0]).intValue();
                        result = ((List<?>) proxy).get(idx);
                    }
                    else if (proxy instanceof List<?> && arguments.length > 0 && arguments[0] instanceof String)
                    {
                        if (((String) arguments[0]).matches("\\d+"))
                        {
                            final int idx = Integer.parseInt((String) arguments[0]);
                            result = ((List<?>) proxy).get(idx);
                        }
                        else
                        {
                            result = null;
                        }
                    }
                    else
                    {
                        // simply delegate further
                        result = invocation.proceed();
                    }
                    break;
                case HAS:
                    if (proxy instanceof List<?> && arguments.length > 0 && arguments[0] instanceof Integer)
                    {
                        final int idx = ((Integer) arguments[0]).intValue();
                        result = Boolean.valueOf(idx >= 0 && idx < ((List<?>) proxy).size());
                    }
                    else if (proxy instanceof List<?> && arguments.length > 0 && arguments[0] instanceof String)
                    {
                        if (((String) arguments[0]).matches("\\d+"))
                        {
                            final int idx = Integer.parseInt((String) arguments[0]);
                            result = Boolean.valueOf(idx >= 0 && idx < ((List<?>) proxy).size());
                        }
                        else
                        {
                            result = Boolean.FALSE;
                        }
                    }
                    else
                    {
                        // simply delegate further
                        result = invocation.proceed();
                    }
                    break;
                case PUT:
                    if (proxy instanceof List<?> && arguments.length > 2 && arguments[0] instanceof Integer)
                    {
                        final int idx = ((Integer) arguments[0]).intValue();
                        @SuppressWarnings("unchecked")
                        final List<Object> list = (List<Object>) proxy;
                        result = list.set(idx, arguments[1]);
                    }
                    else if (proxy instanceof List<?> && arguments.length > 2 && arguments[0] instanceof String)
                    {
                        if (((String) arguments[0]).matches("\\d+"))
                        {
                            final int idx = Integer.parseInt((String) arguments[0]);
                            @SuppressWarnings("unchecked")
                            final List<Object> list = (List<Object>) proxy;
                            result = list.set(idx, arguments[1]);
                        }
                        else
                        {
                            throw new IllegalArgumentException("List does not support string-keys");
                        }
                    }
                    else
                    {
                        // simply delegate further
                        result = invocation.proceed();
                    }
                    break;
                case DELETE:
                    if (proxy instanceof Map<?, ?> && arguments.length > 0 && arguments[0] instanceof Integer)
                    {
                        final int idx = ((Integer) arguments[0]).intValue();
                        result = ((List<?>) proxy).remove(idx);
                    }
                    else if (proxy instanceof Map<?, ?> && arguments.length > 0 && arguments[0] instanceof String)
                    {
                        if (((String) arguments[0]).matches("\\d+"))
                        {
                            final int idx = Integer.parseInt((String) arguments[0]);
                            result = ((List<?>) proxy).remove(idx);
                        }
                        else
                        {
                            throw new IllegalArgumentException("List does not support string-keys");
                        }
                    }
                    else
                    {
                        // simply delegate further
                        result = invocation.proceed();
                    }
                    break;
                default:
                    // simply delegate further
                    result = invocation.proceed();
                    break;
            }

        }
        else
        {
            result = invocation.proceed();
        }

        return result;
    }
}
