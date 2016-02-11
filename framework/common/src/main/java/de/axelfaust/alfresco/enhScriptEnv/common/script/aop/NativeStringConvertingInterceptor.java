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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * @author Axel Faust
 */
public class NativeStringConvertingInterceptor implements MethodInterceptor
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

    private static final NativeStringConvertingInterceptor INSTANCE = new NativeStringConvertingInterceptor();

    public static NativeStringConvertingInterceptor getInstance()
    {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable
    {
        final Object[] arguments = invocation.getArguments();
        Scriptable scope = null;
        for (int idx = 0; idx < arguments.length; idx++)
        {
            if (arguments[idx] instanceof NativeJavaObject)
            {
                final Object unwrapped = ((NativeJavaObject) arguments[idx]).unwrap();
                if (unwrapped instanceof String)
                {
                    // we should find a scope before this, but in case we don't
                    if (scope == null)
                    {
                        scope = DUMMY_SCOPE;
                    }
                    arguments[idx] = ScriptRuntime.toObject(scope, unwrapped);
                }
                else
                {
                    scope = (Scriptable) arguments[idx];
                }
            }
            else if (arguments[idx] instanceof Scriptable)
            {
                scope = (Scriptable) arguments[idx];
            }
        }

        final Object result = invocation.proceed();
        return result;
    }

}
