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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.nabucco.alfresco.enhScriptEnv.common.util.ClassUtils;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.AdapterObject;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.AdapterObjectInterceptor;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.MapLengthFacadeInterceptor;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.ValueConvertingMapInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class GenericScriptableConverter implements ValueInstanceConverter, InitializingBean
{
    private static final Map<Object, Object> DUMMY_MAP = new HashMap<Object, Object>();

    protected ValueInstanceConverterRegistry registry;

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

        this.registry.registerValueInstanceConverter(Scriptable.class, this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if (Scriptable.class.isAssignableFrom(valueInstanceClass))
        {
            confidence = LOW_CONFIDENCE;
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
        final boolean canConvert = Scriptable.class.isInstance(value)
                && (expectedClass.isAssignableFrom(Map.class) || globalDelegate.canConvertValueForScript(DUMMY_MAP, expectedClass));
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!(value instanceof Scriptable))
        {
            throw new IllegalArgumentException("value must be a " + Scriptable.class);
        }

        final Object result;

        // convert Scriptable object of values to a Map of objects
        final Scriptable scriptableValue = (Scriptable) value;
        final Object[] propIds = scriptableValue.getIds();
        final Map<Object, Object> propValues = new HashMap<Object, Object>(propIds.length);
        for (int i = 0; i < propIds.length; i++)
        {
            // work on each key in turn
            final Object propId = propIds[i];

            // get the value out for the specified key
            final Object val = propId instanceof String ? scriptableValue.get((String) propId, scriptableValue) : scriptableValue.get(
                    ((Integer) propId).intValue(), scriptableValue);
            propValues.put(propId, val);
        }

        if (expectedClass.isAssignableFrom(Map.class))
        {
            final ProxyFactory proxyFactory = new ProxyFactory();

            proxyFactory.addAdvice(new AdapterObjectInterceptor());
            proxyFactory.addAdvice(new MapLengthFacadeInterceptor());
            proxyFactory.addAdvice(new ValueConvertingMapInterceptor(globalDelegate));

            proxyFactory.setInterfaces(ClassUtils.collectInterfaces(value, Collections.<Class<?>> singleton(AdapterObject.class)));

            proxyFactory.setTarget(value);

            result = proxyFactory.getProxy();
        }
        else
        {
            result = globalDelegate.convertValueForScript(propValues, expectedClass);
        }

        return result;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForJavaConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if (Scriptable.class.isAssignableFrom(valueInstanceClass))
        {
            confidence = LOW_CONFIDENCE;
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
        final boolean canConvert = Scriptable.class.isInstance(value) && globalDelegate.canConvertValueForScript(DUMMY_MAP, expectedClass);
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!(value instanceof Scriptable))
        {
            throw new IllegalArgumentException("value must be a " + Scriptable.class);
        }

        final Object result;

        // convert Scriptable object of values to a Map of objects
        final Scriptable scriptableValue = (Scriptable) value;
        final Object[] propIds = scriptableValue.getIds();
        final Map<Object, Object> propValues = new HashMap<Object, Object>(propIds.length);
        for (int i = 0; i < propIds.length; i++)
        {
            // work on each key in turn
            final Object propId = propIds[i];

            // get the value out for the specified key
            final Object val = propId instanceof String ? scriptableValue.get((String) propId, scriptableValue) : scriptableValue.get(
                    ((Integer) propId).intValue(), scriptableValue);
            propValues.put(propId, val);
        }

        result = globalDelegate.convertValueForJava(propValues, expectedClass);

        return result;
    }
}
