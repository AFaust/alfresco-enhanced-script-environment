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
package org.springframework.aop.framework;

/**
 * This proxy factory implementation is aware of constructor arguments of potential proxy super classes and can emit proxies for instances
 * of classes that don't provide a default no-argument constructor.
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ConstructorArgumentAwareProxyFactory extends ProxyFactory
{
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
        if (aopProxy instanceof Cglib2AopProxy && this.ctorArguments != null && this.ctorArgumentTypes != null)
        {
            ((Cglib2AopProxy) aopProxy).setConstructorArguments(this.ctorArguments, this.ctorArgumentTypes);
        }
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
        if (aopProxy instanceof Cglib2AopProxy && this.ctorArguments != null && this.ctorArgumentTypes != null)
        {
            ((Cglib2AopProxy) aopProxy).setConstructorArguments(this.ctorArguments, this.ctorArgumentTypes);
        }
        final Object proxy = aopProxy.getProxy(classLoader);
        return proxy;
    }
}