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
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.Scriptable;
import org.springframework.aop.ProxyMethodInvocation;

/**
 * @author Axel Faust
 */
public class ScriptableArrayLikeMapAdapterInterceptor implements MethodInterceptor
{

    private static final ScriptableArrayLikeMapAdapterInterceptor INSTANCE = new ScriptableArrayLikeMapAdapterInterceptor();

    public static ScriptableArrayLikeMapAdapterInterceptor getInstance()
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
                if (proxy instanceof Map<?, ?> && arguments.length > 0 && arguments[0] instanceof Integer)
                {
                    if (!((Map<?, ?>) proxy).containsKey(arguments[0]))
                    {
                        final Object[] keys = ((Map<?, ?>) proxy).keySet().toArray(new Object[0]);
                        if (((Integer) arguments[0]).intValue() >= 0 && keys.length > ((Integer) arguments[0]).intValue())
                        {
                            result = ((Map<?, ?>) proxy).get(keys[((Integer) arguments[0]).intValue()]);
                        }
                        else
                        {
                            // simply delegate further
                            result = invocation.proceed();
                        }
                    }
                    else
                    {
                        // simply delegate further
                        result = invocation.proceed();
                    }
                }
                else
                {
                    // simply delegate further
                    result = invocation.proceed();
                }
                break;
            case HAS:
                if (proxy instanceof Map<?, ?> && arguments.length > 0 && arguments[0] instanceof Integer)
                {
                    if (!((Map<?, ?>) proxy).containsKey(arguments[0]))
                    {
                        final Object[] keys = ((Map<?, ?>) proxy).keySet().toArray(new Object[0]);
                        result = Boolean.valueOf(((Integer) arguments[0]).intValue() >= 0
                                && keys.length > ((Integer) arguments[0]).intValue());
                    }
                    else
                    {
                        // simply delegate further
                        result = invocation.proceed();
                    }
                }
                else
                {
                    // simply delegate further
                    result = invocation.proceed();
                }
                break;
            case PUT:
                if (proxy instanceof Map<?, ?> && arguments.length > 2 && arguments[0] instanceof Integer)
                {
                    if (!((Map<?, ?>) proxy).containsKey(arguments[0]))
                    {
                        // we only support indexed-put for existing keys
                        final Object[] keys = ((Map<?, ?>) proxy).keySet().toArray(new Object[0]);
                        if (((Integer) arguments[0]).intValue() >= 0 && keys.length > ((Integer) arguments[0]).intValue())
                        {
                            @SuppressWarnings("unchecked")
                            final Map<Object, Object> map = (Map<Object, Object>) proxy;
                            map.put(keys[((Integer) arguments[0]).intValue()], arguments[2]);
                            // void return type
                            result = null;
                        }
                        else
                        {
                            // simply delegate further
                            result = invocation.proceed();
                        }
                    }
                    else
                    {
                        // simply delegate further
                        result = invocation.proceed();
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
                    if (!((Map<?, ?>) proxy).containsKey(arguments[0]))
                    {
                        final Object[] keys = ((Map<?, ?>) proxy).keySet().toArray(new Object[0]);
                        if (((Integer) arguments[0]).intValue() >= 0 && keys.length > ((Integer) arguments[0]).intValue())
                        {
                            ((Map<?, ?>) proxy).remove(keys[((Integer) arguments[0]).intValue()]);
                            // void return type
                            result = null;
                        }
                        else
                        {
                            // simply delegate further
                            result = invocation.proceed();
                        }
                    }
                    else
                    {
                        // simply delegate further
                        result = invocation.proceed();
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
