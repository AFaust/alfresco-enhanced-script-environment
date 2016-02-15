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

import org.alfresco.util.ParameterCheck;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueConverter;

/**
 * @author Axel Faust
 */
public class ValueConvertingListInterceptor implements MethodInterceptor
{

    protected final ValueConverter valueConverter;

    public ValueConvertingListInterceptor(final ValueConverter valueConverter)
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

        if (List.class.equals(declaringClass))
        {
            boolean convertResult = false;
            int[] argIdxToConvert = new int[] {};

            final Class<?> expectedClass = method.getReturnType();

            // String-switch not supported in Java < 8
            switch (ListMethodName.methodLiteralOf(method.getName()))
            {
            case GET:
                convertResult = true;
                break;
            case ADD:
                if (boolean.class.equals(expectedClass))
                {
                    // simple add
                    argIdxToConvert = new int[] { 0 };
                }
                else
                {
                    // indexed add
                    argIdxToConvert = new int[] { 1 };
                }
                break;
            case SET:
                convertResult = true;
                argIdxToConvert = new int[] { 1 };
                break;
            case REMOVE:
                if (boolean.class.equals(expectedClass))
                {
                    // simple remove
                    argIdxToConvert = new int[] { 0 };
                }
                else
                {
                    // indexed remove
                    convertResult = true;
                }
                break;
            case INDEXOF: // fallthrough
            case LASTINDEXOF: // fallthrough
            case CONTAINS: // fallthrough
            case CONTAINSALL: // fallthrough
            case RETAINALL: // fallthrough
            case ADDALL: // fallthrough
            case REMOVEALL:
                argIdxToConvert = new int[] { 0 };
                break;
            case TOARRAY:
            case SUBLIST:
                convertResult = true;
                break;
            // TODO We may need to support listIterator (potentially JS for-each-loop)
            default: // NO-OP
            }

            final Object[] arguments = invocation.getArguments();
            final Class<?>[] parameterTypes = invocation.getMethod().getParameterTypes();
            for (final int argIdx : argIdxToConvert)
            {
                arguments[argIdx] = this.valueConverter.convertValueForJava(arguments[argIdx], parameterTypes[argIdx]);
            }

            final Object actualResult = invocation.proceed();

            if (convertResult)
            {
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
