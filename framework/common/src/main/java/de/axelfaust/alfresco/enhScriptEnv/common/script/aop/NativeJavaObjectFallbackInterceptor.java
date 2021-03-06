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
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.framework.ProxyFactory;

import de.axelfaust.alfresco.enhScriptEnv.common.util.ClassUtils;

/**
 * This interceptor is meant to provide a facade the ability to fallback access operations ({@link Scriptable#get(String, Scriptable) get}
 * and {@link Scriptable#has(String, Scriptable) has}) to the native Java object in addition to any simulated scriptable members /
 * properties.
 *
 * @author Axel Faust
 */
public class NativeJavaObjectFallbackInterceptor implements MethodInterceptor
{

    private static final Scriptable DUMMY_SCOPE;
    static
    {
        final Context cx = Context.enter();
        try
        {
            DUMMY_SCOPE = cx.initStandardObjects(null, true);
            DUMMY_SCOPE.delete("Packages");
            DUMMY_SCOPE.delete("getClass");
            DUMMY_SCOPE.delete("java");
            ((ScriptableObject) DUMMY_SCOPE).sealObject();
        }
        finally
        {
            Context.exit();
        }
    }

    protected static final Map<Object, NativeJavaObject> NATIVE_OBJECT_CACHE = new WeakHashMap<Object, NativeJavaObject>();

    private static final NativeJavaObjectFallbackInterceptor INSTANCE = new NativeJavaObjectFallbackInterceptor();

    public static NativeJavaObjectFallbackInterceptor getInstance()
    {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable
    {
        Object result;
        final Method method = invocation.getMethod();
        final Class<?> declaringClass = method.getDeclaringClass();
        if (Scriptable.class.isAssignableFrom(declaringClass) && invocation instanceof ProxyMethodInvocation)
        {
            final Object _this = invocation.getThis();
            NativeJavaObject nativeObject = NATIVE_OBJECT_CACHE.get(_this);

            final String methodName = method.getName();
            final Object[] arguments = invocation.getArguments();

            // String-switch not supported in Java < 8
            switch (ScriptableMethodName.methodLiteralOf(methodName))
            {
                case GET:
                    result = invocation.proceed();
                    if (result == null)
                    {
                        if (nativeObject == null)
                        {
                            nativeObject = new NativeJavaObject(DUMMY_SCOPE, _this, _this.getClass());
                            NATIVE_OBJECT_CACHE.put(_this, nativeObject);
                        }

                        if (arguments[0] instanceof String)
                        {
                            result = nativeObject.get((String) arguments[0], (Scriptable) arguments[1]);
                        }
                        else if (arguments[0] instanceof Integer)
                        {
                            result = nativeObject.get(((Integer) arguments[0]).intValue(), (Scriptable) arguments[1]);
                        }

                        // checking to avoid unnecessary "undefined" as result (callers of native Java objects only expect null)
                        if (result == Undefined.instance || result == Scriptable.NOT_FOUND)
                        {
                            result = null;
                        }
                        // encapsulate any NativeJavaMethod via AOP to correct arguments, most importantly "this"
                        // can't use NativeJavaMethodWrappingInterceptor due to requirement to handle external-to-internal object cache
                        else if (result instanceof NativeJavaMethod)
                        {
                            final ProxyFactory proxyFactory = new ProxyFactory();
                            proxyFactory.addAdvice(AdapterObjectInterceptor.getInstance());
                            proxyFactory.addAdvice(new NativeJavaMethodArgumentCorrectingInterceptor(NATIVE_OBJECT_CACHE));
                            proxyFactory.addAdvice(NativeJavaMethodWrapFactoryInterceptor.getInstance());
                            proxyFactory.setInterfaces(ClassUtils.collectInterfaces(NativeJavaMethod.class,
                                    Arrays.<Class<?>> asList(AdapterObject.class)));
                            proxyFactory.setTarget(result);
                            result = proxyFactory.getProxy();
                        }
                    }
                    break;
                case HAS:
                    result = invocation.proceed();
                    if (!Boolean.TRUE.equals(result))
                    {
                        if (nativeObject == null)
                        {
                            nativeObject = new NativeJavaObject(DUMMY_SCOPE, _this, _this.getClass());
                            NATIVE_OBJECT_CACHE.put(_this, nativeObject);
                        }

                        if (arguments[0] instanceof String)
                        {
                            result = Boolean.valueOf(nativeObject.has((String) arguments[0], (Scriptable) arguments[1]));
                        }
                        else if (arguments[0] instanceof Integer)
                        {
                            result = Boolean.valueOf(nativeObject.has(((Integer) arguments[0]).intValue(), (Scriptable) arguments[1]));
                        }
                    }
                    break;
                case GETDEFAULTVALUE:
                case GETPARENTSCOPE:
                case GETPROTOTYPE:
                case HASINSTANCE:
                case SETPARENTSCOPE:
                case SETPROTOTYPE:
                    if (nativeObject == null)
                    {
                        nativeObject = new NativeJavaObject(DUMMY_SCOPE, _this, _this.getClass());
                        NATIVE_OBJECT_CACHE.put(_this, nativeObject);
                    }
                    result = method.invoke(nativeObject, arguments);
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
