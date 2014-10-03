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
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.Scriptable;
import org.springframework.aop.ProxyMethodInvocation;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ScriptableAdapterInterceptor implements MethodInterceptor
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
        if (Scriptable.class.equals(declaringClass) && invocation instanceof ProxyMethodInvocation)
        {
            final ProxyMethodInvocation pInvocation = (ProxyMethodInvocation) invocation;
            final Object proxy = pInvocation.getProxy();

            final String methodName = method.getName();
            final Object[] arguments = invocation.getArguments();
            final Class<?>[] parameterTypes = method.getParameterTypes();

            boolean redirect = false;
            String redirectMethod = null;
            Object[] redirectParams = null;
            @SuppressWarnings("rawtypes")
            Class[] redirectParamTypes = null;
            Class<?> redirectDeclaringClass = null;
            Object nonRedirectResult = null;

            switch (methodName)
            {
            case "get":
                if (proxy instanceof List<?> && int.class.equals(parameterTypes[0]))
                {
                    redirect = true;
                    redirectDeclaringClass = List.class;
                    redirectMethod = "get";
                    redirectParams = new Object[] { arguments[0] };
                    redirectParamTypes = new Class[] { parameterTypes[0] };
                }
                else if (proxy instanceof Map<?, ?>)
                {
                    redirect = true;
                    redirectDeclaringClass = Map.class;
                    redirectMethod = "get";
                    redirectParams = new Object[] { arguments[0] };
                    redirectParamTypes = new Class[] { Object.class };
                }
                break;
            case "has":
                if (proxy instanceof List<?> && int.class.equals(parameterTypes[0]))
                {
                    nonRedirectResult = Boolean.valueOf(((Integer) arguments[0]).intValue() < ((List<?>) proxy).size());
                }
                else if (proxy instanceof Map<?, ?>)
                {
                    redirect = true;
                    redirectDeclaringClass = Map.class;
                    redirectMethod = "containsKey";
                    redirectParams = new Object[] { arguments[0] };
                    redirectParamTypes = new Class[] { Object.class };
                }
                break;
            case "put":
                if (proxy instanceof List<?> && int.class.equals(parameterTypes[0]))
                {
                    redirect = true;
                    redirectDeclaringClass = List.class;
                    redirectMethod = ((List<?>) proxy).size() > ((Integer) arguments[0]).intValue() ? "set" : "add";
                    redirectParams = new Object[] { arguments[0], arguments[2] };
                    redirectParamTypes = new Class[] { int.class, Object.class };
                }
                else if (proxy instanceof Map<?, ?>)
                {
                    redirect = true;
                    redirectDeclaringClass = Map.class;
                    redirectMethod = "put";
                    redirectParams = new Object[] { arguments[0], arguments[2] };
                    redirectParamTypes = new Class[] { Object.class, Object.class };
                }
                break;
            case "delete":
                if (proxy instanceof List<?> && int.class.equals(parameterTypes[0]))
                {
                    redirect = true;
                    redirectDeclaringClass = List.class;
                    redirectMethod = "remove";
                    redirectParams = arguments;
                    redirectParamTypes = parameterTypes;
                }
                else if (proxy instanceof Map<?, ?>)
                {
                    redirect = true;
                    redirectDeclaringClass = Map.class;
                    redirectMethod = "remove";
                    redirectParams = arguments;
                    redirectParamTypes = new Class[] { Object.class };
                }
                break;
            case "getIds":
                if (proxy instanceof Map<?, ?>)
                {
                    // Note: we do not cover the case of Map + List for getIds, as List is typically only used for indexed access based on
                    // known size and not retrieval of keys
                    nonRedirectResult = ((Map<?, ?>) proxy).keySet().toArray(new Object[0]);
                }
                else if (proxy instanceof List<?>)
                {
                    final int size = ((List<?>) proxy).size();
                    final Object[] arr = new Object[size];
                    for (int idx = 0; idx < size; idx++)
                    {
                        arr[idx] = Integer.valueOf(idx);
                    }
                }
                break;
            case "getClassName":// fallthrough
            case "hasInstance":// fallthrough
            case "getPrototype": // fallthrough
            case "getParentScope": // fallthrough
            case "setPrototype": // fallthrough
            case "setParentScope": // fallthrough
            case "getDefaultValue":
            default:
                // NO-OP
                break;
            }

            final Class<?> returnType = method.getReturnType();
            if (redirect && redirectDeclaringClass != null)
            {
                final Method redirectMethodHandle = redirectDeclaringClass.getMethod(redirectMethod, redirectParamTypes);
                result = redirectMethodHandle.invoke(proxy, redirectParams);
            }
            else if (void.class.equals(returnType))
            {
                result = null;
            }
            else if (nonRedirectResult != null && returnType.isInstance(nonRedirectResult))
            {
                result = nonRedirectResult;
            }
            else if (boolean.class.equals(returnType))
            {
                result = nonRedirectResult instanceof Boolean ? (Boolean) nonRedirectResult : Boolean.FALSE;
            }
            else
            {
                // TODO: what else to do?
                result = null;
            }
        }
        else
        {
            result = invocation.proceed();
        }

        return result;
    }
}
