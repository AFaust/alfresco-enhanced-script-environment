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
import java.util.Map;

import org.alfresco.util.ParameterCheck;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ValueConvertingMapInterceptor implements MethodInterceptor
{

    protected final ValueConverter valueConverter;

    public ValueConvertingMapInterceptor(final ValueConverter valueConverter)
    {
        ParameterCheck.mandatory("valueConverter", valueConverter);
        this.valueConverter = valueConverter;
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

        if (Map.class.equals(declaringClass))
        {
            boolean convertResult = false;
            int[] argIdxToConvert = new int[] {};

            switch (method.getName())
            {
            case "get": // fallthrough
            case "remove":
                convertResult = true;
                argIdxToConvert = new int[] { 0 };
                break;
            case "put":
                convertResult = true;
                argIdxToConvert = new int[] { 0, 1 };
                break;
            case "containsKey": // fallthrough
            case "containsValue":
                convertResult = false;
                argIdxToConvert = new int[] { 0 };
                break;
            // TODO: we need to support keySet and values (JS for-/for-each-loops)
            // TODO: do we need to support putAll?
            default: // NO-OP
            }

            final Object[] arguments = invocation.getArguments();
            final Class<?>[] parameterTypes = method.getParameterTypes();
            for (final int argIdx : argIdxToConvert)
            {
                arguments[argIdx] = this.valueConverter.convertValueForJava(arguments[argIdx], parameterTypes[argIdx]);
            }

            final Object actualResult = invocation.proceed();

            if (convertResult)
            {
                final Class<?> expectedClass = method.getReturnType();
                result = this.valueConverter.convertValueForScript(actualResult, expectedClass);
            }
            else
            {
                result = actualResult;
            }
        }
        else
        {
            result = invocation.proceed();
        }

        return result;
    }

}
