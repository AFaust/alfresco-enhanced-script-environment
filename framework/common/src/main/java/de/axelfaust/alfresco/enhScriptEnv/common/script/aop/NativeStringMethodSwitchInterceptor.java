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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.regexp.NativeRegExp;

/**
 * @author Axel Faust
 */
public class NativeStringMethodSwitchInterceptor implements MethodInterceptor
{

    protected static final Collection<String> REGEX_FN_NAMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("replace")));

    private static final NativeStringMethodSwitchInterceptor INSTANCE = new NativeStringMethodSwitchInterceptor();

    public static NativeStringMethodSwitchInterceptor getInstance()
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

        final Object _this = invocation.getThis();
        if (_this instanceof NativeJavaMethod)
        {
            final Object[] arguments = invocation.getArguments();

            final Method method = invocation.getMethod();
            final String metaMethodName = method.getName();
            final String calledFnName = ((NativeJavaMethod) _this).getFunctionName();
            if ("call".equals(metaMethodName))
            {
                // signature is call(Context cs, Scriptable scope, Scriptable thisObj, Object[] args)
                final Scriptable scope = (Scriptable) arguments[1];
                final Object _stringThis = arguments[2];
                final Object[] fnArgs = (Object[]) arguments[3];

                String backingString = null;
                if (_stringThis instanceof NativeJavaObject)
                {
                    final Object unwrapped = ((NativeJavaObject) _stringThis).unwrap();
                    if (unwrapped instanceof String)
                    {
                        backingString = (String) unwrapped;
                    }
                }

                if (backingString != null)
                {
                    if (REGEX_FN_NAMES.contains(calledFnName) && fnArgs[0] instanceof NativeRegExp)
                    {
                        final Scriptable nativeString = ScriptRuntime.toObject(Context.getCurrentContext(), scope, backingString);
                        result = ScriptableObject.callMethod(nativeString, calledFnName, fnArgs);
                    }
                    // TODO Need to handle other fn/method constellations
                    else
                    {
                        result = invocation.proceed();
                    }
                }
                else
                {
                    result = invocation.proceed();
                }
            }
            else
            {
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
