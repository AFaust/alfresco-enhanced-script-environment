/*
 * Copyright 2015 Axel Faust
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

import java.util.Arrays;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.NativeJavaMethod;
import org.springframework.aop.framework.ProxyFactory;

import de.axelfaust.alfresco.enhScriptEnv.common.util.ClassUtils;

/**
 * @author Axel Faust
 */
public class NativeJavaMethodWrappingInterceptor implements MethodInterceptor
{

    private static final NativeJavaMethodWrappingInterceptor INSTANCE = new NativeJavaMethodWrappingInterceptor();

    public static NativeJavaMethodWrappingInterceptor getInstance()
    {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable
    {
        Object result = invocation.proceed();

        if (result instanceof NativeJavaMethod)
        {
            final ProxyFactory proxyFactory = new ProxyFactory();
            proxyFactory.addAdvice(AdapterObjectInterceptor.getInstance());
            proxyFactory.addAdvice(NativeJavaMethodArgumentCorrectingInterceptor.getDefaultInstance());
            proxyFactory.setInterfaces(ClassUtils.collectInterfaces(NativeJavaMethod.class,
                    Arrays.<Class<?>> asList(AdapterObject.class)));
            proxyFactory.setTarget(result);
            result = proxyFactory.getProxy();
        }

        return result;
    }

}
