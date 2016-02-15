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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.Scriptable;
import org.springframework.aop.ProxyMethodInvocation;

import de.axelfaust.alfresco.enhScriptEnv.common.script.functions.simulated.ListPopFunction;
import de.axelfaust.alfresco.enhScriptEnv.common.script.functions.simulated.ListPushFunction;
import de.axelfaust.alfresco.enhScriptEnv.common.script.functions.simulated.ListShiftFunction;
import de.axelfaust.alfresco.enhScriptEnv.common.script.functions.simulated.ListSpliceFunction;
import de.axelfaust.alfresco.enhScriptEnv.common.script.functions.simulated.ListUnshiftFunction;

/**
 * @author Axel Faust
 */
public class NativeArrayFunctionSimulatingInterceptor implements MethodInterceptor
{

    private static final NativeArrayFunctionSimulatingInterceptor INSTANCE = new NativeArrayFunctionSimulatingInterceptor();

    public static NativeArrayFunctionSimulatingInterceptor getInstance()
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
                    if (proxy instanceof List<?> && arguments[0] instanceof String && arguments[1] instanceof Scriptable)
                    {
                        switch (NativeArrayFunctionName.functionLiteralOf((String) arguments[0]))
                        {
                            case PUSH:
                                result = new ListPushFunction((Scriptable) arguments[1]);
                                break;
                            case POP:
                                result = new ListPopFunction((Scriptable) arguments[1]);
                                break;
                            case SHIFT:
                                result = new ListShiftFunction((Scriptable) arguments[1]);
                                break;
                            case UNSHIFT:
                                result = new ListUnshiftFunction((Scriptable) arguments[1]);
                                break;
                            case SPLICE:
                                result = new ListSpliceFunction((Scriptable) arguments[1]);
                                break;
                            // TODO add additional simulated functions
                            default:
                                result = invocation.proceed();
                        }
                    }
                    else
                    {
                        result = invocation.proceed();
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
