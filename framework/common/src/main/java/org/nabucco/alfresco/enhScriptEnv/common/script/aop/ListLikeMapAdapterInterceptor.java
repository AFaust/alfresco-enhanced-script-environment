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
package org.nabucco.alfresco.enhScriptEnv.common.script.aop;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;

/**
 * @author Axel Faust
 */
public class ListLikeMapAdapterInterceptor implements MethodInterceptor
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
        final Object this1 = invocation.getThis();
        if ((List.class.equals(declaringClass) || Collection.class.equals(declaringClass)) && !(this1 instanceof List<?>))
        {
            if (invocation instanceof ProxyMethodInvocation && ((ProxyMethodInvocation) invocation).getProxy() instanceof Map<?, ?>)
            {
                final Map<?, ?> map = (Map<?, ?>) ((ProxyMethodInvocation) invocation).getProxy();
                final String methodName = method.getName();

                boolean proceedInvocation = false;
                Object adaptedResult = null;

                final Object[] arguments = invocation.getArguments();
                final Class<?>[] parameterTypes = method.getParameterTypes();

                // String-switch not supported in Java < 8
                switch (ListMethodName.methodLiteralOf(methodName))
                {
                case SIZE:
                    adaptedResult = Integer.valueOf(map.size());
                    break;
                case ISEMPTY:
                    adaptedResult = Boolean.valueOf(map.isEmpty());
                    break;
                case CONTAINS:
                    adaptedResult = Boolean.valueOf(map.containsValue(arguments[0]));
                    break;
                case ITERATOR:
                    adaptedResult = map.values().iterator();
                    break;
                case TOARRAY:
                    adaptedResult = arguments.length == 1 ? map.values().toArray((Object[]) arguments[0]) : map.values().toArray();
                    break;
                case CONTAINSALL:
                    adaptedResult = Boolean.valueOf(map.values().containsAll((Collection<?>) arguments[0]));
                    break;
                case REMOVEALL:
                    adaptedResult = Boolean.valueOf(map.values().removeAll((Collection<?>) arguments[0]));
                    break;
                case INDEXOF:
                {
                    int idx = 0;
                    int foundIdx = -1;
                    final Iterator<?> valueIterator = map.values().iterator();
                    while (valueIterator.hasNext())
                    {
                        final Object el = valueIterator.next();
                        if (el == arguments[0] || (arguments[0] != null && arguments[0].equals(el)))
                        {
                            foundIdx = idx;
                            break;
                        }
                        idx++;
                    }
                    adaptedResult = Integer.valueOf(foundIdx);
                }
                    break;
                case LASTINDEXOF:
                {
                    int idx = 0;
                    int foundIdx = -1;
                    final Iterator<?> valueIterator = map.values().iterator();
                    while (valueIterator.hasNext())
                    {
                        final Object el = valueIterator.next();
                        if (el == arguments[0] || (arguments[0] != null && arguments[0].equals(el)))
                        {
                            foundIdx = idx;
                        }
                        idx++;
                    }
                    adaptedResult = Integer.valueOf(foundIdx);
                }
                    break;
                case GET:
                {
                    final int targetIdx = ((Integer) arguments[0]).intValue();

                    if (targetIdx < 0 || targetIdx >= map.size())
                    {
                        throw new IndexOutOfBoundsException();
                    }

                    int idx = 0;
                    Object found = null;
                    final Iterator<?> valueIterator = map.values().iterator();
                    while (valueIterator.hasNext())
                    {
                        final Object el = valueIterator.next();
                        if (idx == targetIdx)
                        {
                            found = el;
                            break;
                        }
                        idx++;
                    }
                    adaptedResult = found;
                }
                    break;
                case REMOVE:
                {
                    if (arguments[0] instanceof Integer && int.class.equals(parameterTypes[0]))
                    {
                        final int targetIdx = ((Integer) arguments[0]).intValue();

                        if (targetIdx < 0 || targetIdx >= map.size())
                        {
                            throw new IndexOutOfBoundsException();
                        }

                        int idx = 0;
                        final Iterator<?> keyIterator = map.keySet().iterator();
                        Object keyToRemove = null;
                        while (keyIterator.hasNext())
                        {
                            final Object el = keyIterator.next();
                            if (idx == targetIdx)
                            {
                                keyToRemove = el;
                                break;
                            }
                            idx++;
                        }

                        adaptedResult = keyToRemove != null ? map.remove(keyToRemove) : null;
                    }
                    else
                    {
                        adaptedResult = Boolean.valueOf(map.values().remove(arguments[0]));
                    }
                }
                    break;
                case RETAINALL:
                    adaptedResult = Boolean.valueOf(map.values().retainAll((Collection<?>) arguments[0]));
                    break;
                case CLEAR:
                    map.clear();
                    break;
                case LISTITERATOR: // fallthrough
                case SUBLIST: // fallthrough
                case SET:// fallthrough
                case ADD:// fallthrough
                case ADDALL:
                    // not supported
                    throw new UnsupportedOperationException();
                default:
                    proceedInvocation = true;
                }

                if (proceedInvocation)
                {
                    // may fail (if we have forgotten to map a specific operation or a new operation may have been introduced)
                    result = invocation.proceed();
                }
                else
                {
                    result = adaptedResult;
                }
            }
            else
            {
                // may fail when other interceptors / target do not support List
                result = invocation.proceed();
            }
        }
        else
        {
            // may fail when List was declaring class but type of "this" does not support this interceptor
            result = invocation.proceed();
        }

        return result;
    }
}
