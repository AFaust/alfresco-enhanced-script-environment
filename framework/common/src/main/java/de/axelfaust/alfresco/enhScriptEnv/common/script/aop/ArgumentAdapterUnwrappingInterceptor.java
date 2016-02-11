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

/**
 * @author Axel Faust
 */
public class ArgumentAdapterUnwrappingInterceptor implements MethodInterceptor
{

    private static final ArgumentAdapterUnwrappingInterceptor INSTANCE = new ArgumentAdapterUnwrappingInterceptor();

    public static ArgumentAdapterUnwrappingInterceptor getInstance()
    {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable
    {
        // process parameters, replacing adapter objects with backing objects (i.e. this as proxy to this as NativeJavaObject
        final Object[] arguments = invocation.getArguments();
        for (int idx = 0; idx < arguments.length; idx++)
        {
            if (arguments[idx] instanceof AdapterObject)
            {
                final Object baseObject = ((AdapterObject) arguments[idx]).getBackingObject();
                arguments[idx] = baseObject;
            }
        }

        final Object result = invocation.proceed();
        return result;
    }

}
