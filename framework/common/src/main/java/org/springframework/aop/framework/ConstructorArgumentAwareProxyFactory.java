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
package org.springframework.aop.framework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This proxy factory implementation is aware of constructor arguments of potential proxy super classes and can emit proxies for instances
 * of classes that don't provide a default no-argument constructor.
 *
 * @author Axel Faust
 */
public class ConstructorArgumentAwareProxyFactory extends ProxyFactory
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ConstructorArgumentAwareProxyFactory.class);

    private final Object[] ctorArguments;

    private final Class[] ctorArgumentTypes;

    public ConstructorArgumentAwareProxyFactory(final Object[] ctorArguments, final Class[] ctorArgumentTypes)
    {
        if (ctorArguments != null)
        {
            this.ctorArguments = new Object[ctorArguments.length];
            System.arraycopy(ctorArguments, 0, this.ctorArguments, 0, ctorArguments.length);
        }
        else
        {
            this.ctorArguments = null;
        }

        if (ctorArgumentTypes != null)
        {
            this.ctorArgumentTypes = new Class[ctorArgumentTypes.length];
            System.arraycopy(ctorArgumentTypes, 0, this.ctorArgumentTypes, 0, ctorArgumentTypes.length);
        }
        else
        {
            this.ctorArgumentTypes = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getProxy()
    {
        final AopProxy aopProxy = this.createAopProxy();
        this.injectConstructorArguments(aopProxy);
        final Object proxy = aopProxy.getProxy();
        return proxy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getProxy(final ClassLoader classLoader)
    {
        final AopProxy aopProxy = this.createAopProxy();
        this.injectConstructorArguments(aopProxy);
        final Object proxy = aopProxy.getProxy(classLoader);
        return proxy;
    }

    protected void injectConstructorArguments(final AopProxy aopProxy)
    {
        if (this.ctorArguments != null && this.ctorArgumentTypes != null)
        {
            try
            {
                final Method ctorArgSetter = aopProxy.getClass().getMethod("setConstructorArguments", Object[].class, Class[].class);
                ctorArgSetter.invoke(aopProxy, this.ctorArguments, this.ctorArgumentTypes);
            }
            catch (final NoSuchMethodException nsme)
            {
                LOGGER.info("Proxy does not provide setConstructorArguments method - likely Cglib is not available", nsme);
            }
            catch (final InvocationTargetException ite)
            {
                LOGGER.error("Error setting constructor arguments", ite);
            }
            catch (final IllegalAccessException iae)
            {
                LOGGER.error("Error setting constructor arguments", iae);
            }
        }
    }
}