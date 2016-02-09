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

import org.alfresco.util.ParameterCheck;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueConverter;

/**
 * @author Axel Faust
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

            // String-switch not supported in Java < 8
            switch (MapMethodName.methodLiteralOf(method.getName()))
            {
                case GET: // fallthrough
                case REMOVE:
                    convertResult = true;
                    argIdxToConvert = new int[] { 0 };
                    break;
                case PUT:
                    convertResult = true;
                    argIdxToConvert = new int[] { 0, 1 };
                    break;
                case CONTAINSKEY: // fallthrough
                case CONTAINSVALUE:
                    convertResult = false;
                    argIdxToConvert = new int[] { 0 };
                    break;
                // TODO We may need to support keySet and values (potentially JS for-/for-each-loops)
                // TODO Do we need to support putAll?
                default: // NO-OP
            }

            final Object[] arguments = invocation.getArguments();
            final Class<?>[] parameterTypes = method.getParameterTypes();
            for (final int argIdx : argIdxToConvert)
            {
                arguments[argIdx] = this.valueConverter.convertValueForJava(arguments[argIdx], parameterTypes[argIdx]);
            }

            final Object actualResult = invocation.proceed();

            if (convertResult && actualResult != null)
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
