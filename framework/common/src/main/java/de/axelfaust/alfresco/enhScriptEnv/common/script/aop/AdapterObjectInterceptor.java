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

/**
 * Interceptor to fit the {@link AdapterObject} interface upon existing objects without modification of the actual class.
 *
 * @author Axel Faust
 */
public class AdapterObjectInterceptor implements MethodInterceptor
{

    private static final AdapterObjectInterceptor INSTANCE = new AdapterObjectInterceptor();

    public static AdapterObjectInterceptor getInstance()
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
        if (method.getDeclaringClass().equals(AdapterObject.class) && "getBackingObject".equals(method.getName()))
        {
            result = invocation.getThis();
        }
        else
        {
            result = invocation.proceed();
        }

        return result;
    }

}
