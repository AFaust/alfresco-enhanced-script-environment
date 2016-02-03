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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.alfresco.util.PropertyCheck;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * Generic value converter that supports conversion of collections or iterable objects to regular Java arrays, transparently processing any
 * contained objects according.
 *
 * @author Axel Faust
 */
public class ArrayConverter implements ValueInstanceConverter, InitializingBean
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

        this.registry.registerValueInstanceConverter(Object[].class, this);
        this.registry.registerValueInstanceConverter(byte[].class, this);
        this.registry.registerValueInstanceConverter(short[].class, this);
        this.registry.registerValueInstanceConverter(char[].class, this);
        this.registry.registerValueInstanceConverter(int[].class, this);
        this.registry.registerValueInstanceConverter(long[].class, this);
        this.registry.registerValueInstanceConverter(float[].class, this);
        this.registry.registerValueInstanceConverter(double[].class, this);
        this.registry.registerValueInstanceConverter(boolean[].class, this);

        this.registry.registerValueInstanceConverter(Collection.class, this);
        this.registry.registerValueInstanceConverter(Iterable.class, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;

        if ((valueInstanceClass.isArray() || Collection.class.isAssignableFrom(valueInstanceClass) || Iterable.class
                .isAssignableFrom(valueInstanceClass)) && expectedClass.isArray())
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
        boolean canConvert = expectedClass.isArray();

        if (canConvert)
        {
            final Class<?> componentClass = expectedClass.getComponentType();
            if (value instanceof Iterable<?>)
            {
                final Collection<?> coll = (Collection<?>) value;
                for (final Object element : coll)
                {
                    canConvert = canConvert && globalDelegate.canConvertValueForScript(element, componentClass);

                    if (!canConvert)
                    {
                        break;
                    }
                }
            }
            else if (value.getClass().isArray())
            {
                final int length = Array.getLength(value);
                for (int idx = 0; idx < length && canConvert; idx++)
                {
                    canConvert = canConvert && globalDelegate.canConvertValueForScript(Array.get(value, idx), componentClass);
                }
            }
            else
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
        final Object result = this.convertToArray(value, globalDelegate, expectedClass, true);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getForJavaConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;

        if ((valueInstanceClass.isArray() || Collection.class.isAssignableFrom(valueInstanceClass) || Iterable.class
                .isAssignableFrom(valueInstanceClass)) && expectedClass.isArray())
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
        boolean canConvert = expectedClass.isArray();

        if (canConvert)
        {
            final Class<?> componentClass = expectedClass.getComponentType();
            if (value instanceof Iterable<?>)
            {
                final Collection<?> coll = (Collection<?>) value;
                for (final Object element : coll)
                {
                    canConvert = canConvert && globalDelegate.canConvertValueForJava(element, componentClass);

                    if (!canConvert)
                    {
                        break;
                    }
                }
            }
            else if (value.getClass().isArray())
            {
                final int length = Array.getLength(value);
                for (int idx = 0; idx < length && canConvert; idx++)
                {
                    canConvert = canConvert && globalDelegate.canConvertValueForJava(Array.get(value, idx), componentClass);
                }
            }
            else
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
        final Object result = this.convertToArray(value, globalDelegate, expectedClass, false);

        return result;
    }

    protected Object convertToArray(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass,
            final boolean toScript)
    {
        final Object result;
        final Class<?> valueClass = value.getClass();
        if (valueClass.isArray())
        {
            final Object arr = Array.newInstance(expectedClass.getComponentType(), Array.getLength(value));

            for (int idx = 0; idx < Array.getLength(value); idx++)
            {
                final Object converted = toScript ? globalDelegate.convertValueForScript(Array.get(value, idx),
                        expectedClass.getComponentType()) : globalDelegate.convertValueForJava(Array.get(value, idx),
                        expectedClass.getComponentType());
                Array.set(arr, idx, converted);
            }
            result = arr;
        }
        else
        {
            final Collection<?> coll;
            if (value instanceof Collection<?>)
            {
                coll = (Collection<?>) value;
            }
            else
            {
                final List<Object> list = new ArrayList<Object>();
                final Iterator<?> it = (Iterator<?>) value;
                while (it.hasNext())
                {
                    list.add(it.next());
                }
                coll = list;
            }

            final Object arr = Array.newInstance(expectedClass.getComponentType(), coll.size());
            final Iterator<?> it = coll.iterator();
            for (int idx = 0; it.hasNext(); idx++)
            {
                final Object converted = toScript ? globalDelegate.convertValueForScript(it.next(), expectedClass.getComponentType())
                        : globalDelegate.convertValueForJava(it.next(), expectedClass.getComponentType());
                Array.set(arr, idx, converted);
            }
            result = arr;
        }
        return result;
    }

}
