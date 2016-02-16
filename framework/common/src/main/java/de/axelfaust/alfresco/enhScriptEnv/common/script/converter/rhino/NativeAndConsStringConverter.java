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
package de.axelfaust.alfresco.enhScriptEnv.common.script.converter.rhino;

import java.util.Arrays;

import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ConstructorArgumentAwareProxyFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.InitializingBean;

import de.axelfaust.alfresco.enhScriptEnv.common.script.aop.AdapterObject;
import de.axelfaust.alfresco.enhScriptEnv.common.script.aop.AdapterObjectInterceptor;
import de.axelfaust.alfresco.enhScriptEnv.common.script.aop.NativeStringEmulatingInterceptor;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import de.axelfaust.alfresco.enhScriptEnv.common.util.ClassUtils;

/**
 * @author Axel Faust
 */
public class NativeAndConsStringConverter implements ValueInstanceConverter, InitializingBean
{

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeAndConsStringConverter.class);

    private static final Scriptable DUMMY_SCOPE;
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

    protected ValueInstanceConverterRegistry registry;

    protected Class<?> consStringClass;

    /**
     * @param registry
     *            the registry to set
     */
    public void setRegistry(final ValueInstanceConverterRegistry registry)
    {
        this.registry = registry;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "registry", this.registry);

        try
        {
            this.consStringClass = Class.forName("org.mozilla.javascript.ConsString");
            this.registry.registerValueInstanceConverter(this.consStringClass, this);
        }
        catch (final ClassNotFoundException cnfe)
        {
            LOGGER.info("Rhino ConsString class is not available - this is normal for an Alfresco release that does not include Rhino 1.7",
                    cnfe);
        }

        this.registry.registerValueInstanceConverter(IdScriptableObject.class, this);
        this.registry.registerValueInstanceConverter(CharSequence.class, this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForJavaConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if ((IdScriptableObject.class.isAssignableFrom(valueInstanceClass) || (this.consStringClass != null && this.consStringClass
                .isAssignableFrom(valueInstanceClass))) && expectedClass.isAssignableFrom(String.class))
        {
            confidence = HIGHEST_CONFIDENCE;
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
    public boolean canConvertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        final boolean canConvert = (((value instanceof IdScriptableObject && "String".equals(((IdScriptableObject) value).getClassName())) || (this.consStringClass != null && this.consStringClass
                .isInstance(value)))) && expectedClass.isAssignableFrom(String.class);
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!((value instanceof IdScriptableObject && "String".equals(((IdScriptableObject) value).getClassName())) || (this.consStringClass != null && this.consStringClass
                .isInstance(value))))
        {
            throw new IllegalArgumentException("value must be a ConsString/NativeString");
        }

        final Object result = Context.jsToJava(value, String.class);
        return result;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;

        if (CharSequence.class.isAssignableFrom(valueInstanceClass) && expectedClass.isAssignableFrom(Scriptable.class))
        {
            confidence = MEDIUM_CONFIDENCE;
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
        final boolean canConvert;

        if (value instanceof CharSequence && expectedClass.isAssignableFrom(Scriptable.class))
        {
            canConvert = true;
        }
        else
        {
            canConvert = false;
        }

        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!(value instanceof CharSequence))
        {
            throw new IllegalArgumentException("value must be a CharSequence");
        }

        final NativeJavaObject nativeJavaObject = new NativeJavaObject(DUMMY_SCOPE, value, value.getClass());

        final ProxyFactory proxyFactory = new ConstructorArgumentAwareProxyFactory(new Object[] { DUMMY_SCOPE, value, value.getClass() },
                new Class<?>[] { Scriptable.class, Object.class, Class.class });
        proxyFactory.addAdvice(AdapterObjectInterceptor.getInstance());
        proxyFactory.addAdvice(NativeStringEmulatingInterceptor.getInstance());
        proxyFactory.setInterfaces(ClassUtils.collectInterfaces(nativeJavaObject, Arrays.<Class<?>> asList(AdapterObject.class)));
        proxyFactory.setTarget(nativeJavaObject);
        proxyFactory.setProxyTargetClass(true);

        final Object result = proxyFactory.getProxy();
        return result;
    }
}
