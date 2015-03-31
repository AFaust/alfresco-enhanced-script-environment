/*
 * Copyright 2015 PRODYNA AG
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
package org.nabucco.alfresco.enhScriptEnv.common.script.converter.general;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class StringToNumberConverter implements ValueInstanceConverter, InitializingBean
{

    private static final Set<Class<? extends Number>> SIMPLE_NUMBER_CLASSES = Collections
            .unmodifiableSet(new HashSet<Class<? extends Number>>(Arrays.asList(Integer.class, Long.class, Float.class, Double.class,
                    Short.class, Byte.class, int.class, long.class, float.class, double.class, short.class, byte.class)));

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

        this.registry.registerValueInstanceConverter(String.class, this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if (String.class.isAssignableFrom(valueInstanceClass)
                && (expectedClass.isAssignableFrom(Number.class) || SIMPLE_NUMBER_CLASSES.contains(expectedClass)))
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
        boolean canConvert = value instanceof String
                && (expectedClass.isAssignableFrom(Number.class) || SIMPLE_NUMBER_CLASSES.contains(expectedClass));

        if (canConvert)
        {
            try
            {
                canConvert = globalDelegate.canConvertValueForScript(Double.valueOf((String) value), expectedClass);
            }
            catch (final NumberFormatException ex)
            {
                canConvert = false;
            }
        }

        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!(value instanceof String))
        {
            throw new IllegalArgumentException("value must be a " + String.class);
        }

        return globalDelegate.convertValueForScript(Double.valueOf((String) value), expectedClass);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForJavaConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        return this.getForScriptConversionConfidence(valueInstanceClass, expectedClass);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        boolean canConvert = value instanceof String
                && (expectedClass.isAssignableFrom(Number.class) || SIMPLE_NUMBER_CLASSES.contains(expectedClass));

        if (canConvert)
        {
            try
            {
                canConvert = globalDelegate.canConvertValueForJava(Double.valueOf((String) value), expectedClass);
            }
            catch (final NumberFormatException ex)
            {
                canConvert = false;
            }
        }

        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!(value instanceof String))
        {
            throw new IllegalArgumentException("value must be a " + String.class);
        }

        return globalDelegate.convertValueForJava(Double.valueOf((String) value), expectedClass);
    }
}
