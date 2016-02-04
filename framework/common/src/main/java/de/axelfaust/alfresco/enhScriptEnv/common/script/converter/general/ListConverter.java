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
package de.axelfaust.alfresco.enhScriptEnv.common.script.converter.general;

import java.util.List;
import java.util.Map;

import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.springframework.beans.factory.InitializingBean;

import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;

/**
 * A simple convert to handle List-to-List conversion where the original List instance is retained but values are recursively put through
 * conversions.
 *
 * @author Axel Faust
 */
public class ListConverter implements ValueInstanceConverter, InitializingBean
{

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

        this.registry.registerValueInstanceConverter(List.class, this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForJavaConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        // in Rhino 1.7, NativeObject implements Map which makes things difficult
        if (Map.class.isAssignableFrom(valueInstanceClass) && !NativeObject.class.isAssignableFrom(valueInstanceClass)
                && (expectedClass.isAssignableFrom(Map.class) || expectedClass.equals(valueInstanceClass)))
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
        // in Rhino 1.7, NativeArray implements List which makes things difficult
        boolean canConvert = value instanceof List<?> && !(value instanceof NativeArray)
                && (expectedClass.isAssignableFrom(List.class) || expectedClass.equals(value.getClass()));

        final List<?> list = (List<?>) value;

        for (int idx = 0, size = list.size(); idx < size; idx++)
        {
            final Object valueForKey = list.get(idx);
            canConvert = canConvert && globalDelegate.canConvertValueForJava(valueForKey);
        }

        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        // in Rhino 1.7, NativeArray implements List which makes things difficult
        if (!(value instanceof List<?>) || value instanceof NativeArray)
        {
            throw new IllegalArgumentException("value must be a List");
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final List<Object> list = (List) value;

        for (int idx = 0, size = list.size(); idx < size; idx++)
        {
            final Object valueForKey = list.get(idx);

            final Object convertedValue = globalDelegate.convertValueForJava(valueForKey);

            if (valueForKey != convertedValue)
            {
                list.set(idx, convertedValue);
            }
        }

        return list;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if (Map.class.isAssignableFrom(valueInstanceClass)
                && (expectedClass.isAssignableFrom(Map.class) || expectedClass.equals(valueInstanceClass)))
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
        boolean canConvert = value instanceof List<?>
                && (expectedClass.isAssignableFrom(List.class) || expectedClass.equals(value.getClass()));

        final List<?> list = (List<?>) value;

        for (int idx = 0, size = list.size(); idx < size; idx++)
        {
            final Object valueForKey = list.get(idx);
            canConvert = canConvert && globalDelegate.canConvertValueForScript(valueForKey);
        }

        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!(value instanceof List<?>))
        {
            throw new IllegalArgumentException("value must be a List");
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final List<Object> list = (List) value;

        for (int idx = 0, size = list.size(); idx < size; idx++)
        {
            final Object valueForKey = list.get(idx);

            final Object convertedValue = globalDelegate.convertValueForScript(valueForKey);

            if (valueForKey != convertedValue)
            {
                list.set(idx, convertedValue);
            }
        }

        return list;
    }
}
