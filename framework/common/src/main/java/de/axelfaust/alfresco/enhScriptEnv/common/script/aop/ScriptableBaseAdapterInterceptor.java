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
import java.util.WeakHashMap;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.springframework.aop.ProxyMethodInvocation;

/**
 * @author Axel Faust
 */
public class ScriptableBaseAdapterInterceptor implements MethodInterceptor
{

    // all internal data structures are cached with proxy as key
    protected final Map<Object, Scriptable> parentScope = new WeakHashMap<Object, Scriptable>();
    protected final Map<Object, Scriptable> prototype = new WeakHashMap<Object, Scriptable>();

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
            case GETPARENTSCOPE:
                result = this.parentScope.get(proxy);
                break;
            case SETPARENTSCOPE:
                if (arguments.length > 0 && arguments[0] instanceof Scriptable)
                {
                    this.parentScope.put(proxy, (Scriptable) arguments[0]);
                }
                // void return type
                result = null;
                break;
            case GETPROTOTYPE:
                result = this.prototype.get(proxy);
                break;
            case SETPROTOTYPE:
                if (arguments.length > 0 && arguments[0] instanceof Scriptable)
                {
                    this.prototype.put(proxy, (Scriptable) arguments[0]);
                }
                // void return type
                result = null;
                break;
            case HASINSTANCE:
                // proxies can never be used for hasInstance checks
                result = Boolean.FALSE;
                break;
            case GETDEFAULVALUE:
                if (arguments.length > 0)
                {
                    if (arguments[0] == null || ScriptRuntime.StringClass == arguments[0])
                    {
                        result = proxy.toString();
                    }
                    else
                    {
                        result = null;
                    }
                }
                else
                {
                    result = null;
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
