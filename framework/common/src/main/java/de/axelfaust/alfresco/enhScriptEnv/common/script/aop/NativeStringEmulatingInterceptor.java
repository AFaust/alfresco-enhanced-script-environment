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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.springframework.aop.framework.ProxyFactory;

/**
 * @author Axel Faust
 */
public class NativeStringEmulatingInterceptor implements MethodInterceptor
{

    private static final NativeStringEmulatingInterceptor INSTANCE = new NativeStringEmulatingInterceptor();

    public static NativeStringEmulatingInterceptor getInstance()
    {
        return INSTANCE;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable
    {
        Object result;
        final Method method = invocation.getMethod();
        final Class<?> declaringClass = method.getDeclaringClass();

        final Object _this = invocation.getThis();
        String backingString = null;
        if (_this instanceof NativeJavaObject)
        {
            final Object unwrapped = ((NativeJavaObject) _this).unwrap();
            if (unwrapped instanceof String)
            {
                backingString = (String) unwrapped;
            }
        }

        final Object[] arguments = invocation.getArguments();

        // CharSequence support is to make ScriptRuntime.shallowEq handle === properly
        if (backingString != null && CharSequence.class.equals(declaringClass))
        {
            result = method.invoke(backingString, arguments);
        }
        else if (backingString != null && Scriptable.class.equals(declaringClass))
        {
            result = invocation.proceed();

            if (ScriptableMethodName.methodLiteralOf(method.getName()) == ScriptableMethodName.GET)
            {
                final Object propertyKey = arguments[0];

                if (result instanceof NativeJavaMethod)
                {
                    final ProxyFactory proxyFactory = new ProxyFactory();
                    proxyFactory.addAdvice(AdapterObjectInterceptor.getInstance());
                    proxyFactory.addAdvice(ArgumentAdapterUnwrappingInterceptor.getInstance());
                    proxyFactory.addAdvice(NativeStringMethodSwitchInterceptor.getInstance());
                    proxyFactory.setInterfaces(Function.class, AdapterObject.class);
                    proxyFactory.setTarget(result);
                    result = proxyFactory.getProxy();
                }
                // Handle native-only String functions
                else if (Scriptable.NOT_FOUND.equals(result) && propertyKey instanceof String)
                {
                    final Scriptable nativeString = ScriptRuntime.toObject(Context.getCurrentContext(), (Scriptable) arguments[1],
                            backingString);
                    final Object nativeProperty = ScriptableObject.getProperty(nativeString, (String) propertyKey);

                    if (nativeProperty instanceof Function)
                    {
                        final ProxyFactory proxyFactory = new ProxyFactory();
                        proxyFactory.addAdvice(AdapterObjectInterceptor.getInstance());
                        proxyFactory.addAdvice(ArgumentAdapterUnwrappingInterceptor.getInstance());
                        proxyFactory.addAdvice(NativeStringConvertingInterceptor.getInstance());
                        proxyFactory.setInterfaces(Function.class, AdapterObject.class);
                        proxyFactory.setTarget(nativeProperty);
                        result = proxyFactory.getProxy();
                    }
                    else
                    {
                        result = nativeProperty;
                    }
                }
            }
        }
        else
        {
            result = invocation.proceed();
        }

        return result;
    }
}
