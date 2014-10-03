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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.nashorn;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jdk.nashorn.internal.objects.NativeArray;
import jdk.nashorn.internal.objects.NativeJava;
import jdk.nashorn.internal.runtime.arrays.ArrayData;

import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class NativeArrayConverter implements ValueInstanceConverter, InitializingBean
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

        this.registry.registerValueInstanceConverter(NativeArray.class, this);
        this.registry.registerValueInstanceConverter(Collection.class, this);
        this.registry.registerValueInstanceConverter(Object[].class, this);
        this.registry.registerValueInstanceConverter(byte[].class, this);
        this.registry.registerValueInstanceConverter(short[].class, this);
        this.registry.registerValueInstanceConverter(char[].class, this);
        this.registry.registerValueInstanceConverter(int[].class, this);
        this.registry.registerValueInstanceConverter(long[].class, this);
        this.registry.registerValueInstanceConverter(float[].class, this);
        this.registry.registerValueInstanceConverter(double[].class, this);
        this.registry.registerValueInstanceConverter(boolean[].class, this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if ((valueInstanceClass.isArray() || Collection.class.isAssignableFrom(valueInstanceClass))
                && expectedClass.isAssignableFrom(NativeArray.class))
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
    public boolean canConvertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        boolean canConvert = expectedClass.isAssignableFrom(NativeArray.class);

        if (canConvert)
        {
            if (value instanceof Collection<?>)
            {
                final Collection<?> coll = (Collection<?>) value;
                for (final Object element : coll)
                {
                    canConvert = canConvert && globalDelegate.canConvertValueForScript(element);

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
                    canConvert = canConvert && globalDelegate.canConvertValueForScript(Array.get(value, idx));
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
        // Note: Global.wrapAsObject(Object) already deals with Object[], double[], long[] and int[]
        // This covers all these and other arrays / collections for sake of completeness and recursive conversion

        final Object[] arr;
        int arrIdx = 0;
        if (value instanceof Collection<?>)
        {
            final Collection<?> coll = (Collection<?>) value;
            arr = new Object[coll.size()];
            for (final Object element : coll)
            {
                arr[arrIdx++] = globalDelegate.convertValueForScript(element);
            }
        }
        else if (value.getClass().isArray())
        {
            final int length = Array.getLength(value);
            arr = new Object[length];
            for (int idx = 0; idx < length; idx++)
            {
                arr[arrIdx++] = globalDelegate.convertValueForScript(Array.get(value, idx));
            }
        }
        else
        {
            throw new IllegalArgumentException("value must be either collection or array");
        }

        final Object result = NativeJava.from(null, arr);
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
        if (NativeArray.class.isAssignableFrom(valueInstanceClass)
                && (expectedClass.isAssignableFrom(Collection.class) || expectedClass.isArray()))
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
        boolean canConvert = value instanceof NativeArray;

        if (canConvert)
        {
            final NativeArray arr = (NativeArray) value;
            final ArrayData arrayData = arr.getArray();
            final Object[] values = arrayData.asObjectArray();

            final Class<?> expectedComponentClass = expectedClass.isArray() ? expectedClass.getComponentType() : Object.class;
            // arrayData.length is the actual length - either due bug or unknown consideration, values.length may be larger
            for (int idx = 0; idx < arrayData.length() && canConvert; idx++)
            {
                canConvert = canConvert && globalDelegate.canConvertValueForJava(values[idx], expectedComponentClass);
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
        if (!(value instanceof NativeArray))
        {
            throw new IllegalArgumentException("value must be a " + NativeArray.class);
        }

        final NativeArray arr = (NativeArray) value;
        final ArrayData arrayData = arr.getArray();
        final Object[] values = arrayData.asObjectArray();

        final Object result;

        final Class<?> expectedComponentClass = expectedClass.isArray() ? expectedClass.getComponentType() : Object.class;
        final List<Object> list = new ArrayList<Object>();
        // arrayData.length is the actual length - either due bug or unknown consideration, values.length may be larger
        for (int idx = 0; idx < arrayData.length(); idx++)
        {
            final Object converted = globalDelegate.convertValueForJava(values[idx], expectedComponentClass);
            list.add(converted);
        }

        if (expectedClass.isArray())
        {
            final Object newArr = Array.newInstance(expectedComponentClass, list.size());
            for (int idx = 0; idx < list.size(); idx++)
            {
                Array.set(newArr, idx, list.get(idx));
            }
            result = newArr;
        }
        else
        {
            result = list;
        }

        return result;
    }

}
