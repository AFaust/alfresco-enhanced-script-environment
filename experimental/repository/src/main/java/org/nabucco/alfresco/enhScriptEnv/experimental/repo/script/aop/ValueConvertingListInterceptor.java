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
import java.util.List;

import org.alfresco.util.ParameterCheck;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.ValueConverter;
import org.springframework.aop.framework.ProxyFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ValueConvertingListInterceptor implements MethodInterceptor
{

    protected final ProxyFactory listProxyFactory = new ProxyFactory(List.class, this);

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

            switch (method.getName())
            {
            case "get":
                convertResult = true;
                break;
            case "put":
                convertResult = true;
                argIdxToConvert = new int[] { 0, 1 };
                break;
            case "add":
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
            case "remove":
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
            case "indexOf": // fallthrough
            case "lastIndexOf": // fallthrough
            case "contains":
                argIdxToConvert = new int[] { 0 };
                break;
            case "set":
                convertResult = true;
                argIdxToConvert = new int[] { 1 };
                break;
            case "toArray":
                convertResult = true;
                break;
            case "subList":
                convertResult = true;
                break;
            // TODO: we need to support listIterator (potentially JS for-each-loop)
            // TODO: do we need to support containsAll / addAll / removeAll / retainAll?
            default: // NO-OP
            }

            // final Class<?>[] parameterTypes = method.getParameterTypes();
            final Object[] arguments = invocation.getArguments();
            for (final int argIdx : argIdxToConvert)
            {
                // TODO: include parameter type when interface is extended to permit "expectedClass"
                arguments[argIdx] = this.valueConverter.convertValueForJava(arguments[argIdx]);
            }

            final Object actualResult = invocation.proceed();

            if (convertResult)
            {
                if ("subList".equals(method.getName()))
                {
                    synchronized (this.listProxyFactory)
                    {
                        this.listProxyFactory.setTarget(actualResult);
                        result = this.listProxyFactory.getProxy();
                    }
                }
                else
                {
                    result = this.valueConverter.convertValueForNashorn(actualResult, expectedClass);
                }
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
