/*
 * Copyright 2014 PRODYNA AG
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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.rhino2nashorn;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Properties;

import jdk.nashorn.api.scripting.JSObject;

import org.alfresco.repo.jscript.Scopeable;
import org.alfresco.util.Pair;
import org.alfresco.util.PropertyCheck;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.rhino.DelegatingWrapFactory;
import org.nabucco.alfresco.enhScriptEnv.common.util.ClassUtils;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.AdapterObject;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.AdapterObjectInterceptor;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.JSObjectMixinInterceptor;
import org.springframework.aop.framework.ConstructorArgumentAwareProxyFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
// internal API reguired to provide added value
@SuppressWarnings("restriction")
public class SimpleJSObjectSubClassProxyConverter implements ValueInstanceConverter, InitializingBean
{
    protected static final Scriptable DUMMY_SCOPE;
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

    protected static class RhinoSpecificBeanInterceptor implements MethodInterceptor
    {

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public Object invoke(final MethodInvocation invocation) throws Throwable
        {
            // need to make sure we have a context
            final Context cx = Context.enter();
            try
            {
                cx.setWrapFactory(new DelegatingWrapFactory());

                // also need to make sure we have a scope
                final Object thisObj = invocation.getThis();
                final Method method = invocation.getMethod();
                if (thisObj instanceof Scopeable && !Scopeable.class.equals(method.getDeclaringClass()))
                {
                    ((Scopeable) thisObj).setScope(DUMMY_SCOPE);
                }

                final Object result = invocation.proceed();
                return result;
            }
            finally
            {
                Context.exit();
            }
        }
    }

    protected ValueInstanceConverterRegistry registry;

    protected Properties signatureMappings;

    protected Class<?> javaBaseClass;

    protected boolean checkBaseClassInConversion = true;

    protected int confidence = MEDIUM_CONFIDENCE;

    /**
     * @param registry
     *            the registry to set
     */
    public void setRegistry(final ValueInstanceConverterRegistry registry)
    {
        this.registry = registry;
    }

    /**
     * @param signatureMappings
     *            the signatureMappings to set
     */
    public void setSignatureMappings(final Properties signatureMappings)
    {
        this.signatureMappings = signatureMappings;
    }

    /**
     * @param javaBaseClass
     *            the javaBaseClass to set
     */
    public void setJavaBaseClass(final Class<?> javaBaseClass)
    {
        this.javaBaseClass = javaBaseClass;
    }

    /**
     * Sets the confidence to given confidence.
     *
     * @param confidence
     *            the confidence to set
     */
    public void setConfidence(final int confidence)
    {
        this.confidence = confidence;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "registry", this.registry);
        PropertyCheck.mandatory(this, "javaBaseClass", this.javaBaseClass);

        if (this.signatureMappings == null)
        {
            this.signatureMappings = new Properties();
        }

        this.registry.registerValueInstanceConverter(this.javaBaseClass, this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if (this.javaBaseClass.isAssignableFrom(valueInstanceClass) && expectedClass.isAssignableFrom(valueInstanceClass))
        {
            confidence = this.confidence;
        }
        else
        {
            confidence = LOWEST_CONFIDENCE;
        }
        return confidence;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        final boolean canConvert = this.javaBaseClass.isInstance(value) && expectedClass.isAssignableFrom(value.getClass())
                && !Modifier.isFinal(value.getClass().getModifiers()) && !AdapterObject.class.isInstance(value);
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (this.checkBaseClassInConversion && !this.javaBaseClass.isInstance(value))
        {
            throw new IllegalArgumentException("value must be a " + this.javaBaseClass);
        }

        final Object result;

        // TODO support txn / request-scoped caching of proxy instances per value bean

        final ProxyFactory proxyFactory;

        final Pair<Class<?>[], Object[]> constructorParameters = this.determineForScriptProxyConstructorParameters(value, expectedClass);
        if (constructorParameters.getFirst().length == constructorParameters.getSecond().length)
        {
            if (constructorParameters.getFirst().length == 0)
            {
                proxyFactory = new ProxyFactory();
            }
            else
            {
                proxyFactory = new ConstructorArgumentAwareProxyFactory(constructorParameters.getSecond(), constructorParameters.getFirst());
            }
        }
        else
        {
            throw new IllegalStateException("Constructor parameters have unequal amount of arguments and argument type descriptors");
        }

        proxyFactory.setInterfaces(ClassUtils.collectInterfaces(value, Arrays.<Class<?>> asList(JSObject.class, AdapterObject.class)));

        proxyFactory.addAdvice(new JSObjectMixinInterceptor(globalDelegate, this.signatureMappings));
        proxyFactory.addAdvice(new RhinoSpecificBeanInterceptor());
        proxyFactory.addAdvice(new AdapterObjectInterceptor());

        proxyFactory.setTarget(value);
        proxyFactory.setProxyTargetClass(true);

        result = proxyFactory.getProxy();

        return result;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForJavaConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        // no need to convert back - either handled via AdapterObjectConverter or treated as-is (valid values of a
        // subclass)
        return LOWEST_CONFIDENCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        // no need to convert back - either handled via AdapterObjectConverter or treated as-is (valid values of a
        // subclass)
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        // clients should check canConvertValueForJava first
        throw new UnsupportedOperationException("This operation is not supported and should not have been called");
    }

    protected Pair<Class<?>[], Object[]> determineForScriptProxyConstructorParameters(final Object value, final Class<?> expectedClass)
    {
        // default
        return new Pair<Class<?>[], Object[]>(new Class<?>[0], new Object[0]);
    }
}
