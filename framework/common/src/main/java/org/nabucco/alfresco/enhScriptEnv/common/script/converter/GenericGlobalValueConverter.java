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
package org.nabucco.alfresco.enhScriptEnv.common.script.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alfresco.util.ParameterCheck;
import org.nabucco.alfresco.enhScriptEnv.common.util.ClassUtils;

/**
 * @author Axel Faust
 */
public class GenericGlobalValueConverter implements ValueConverter, ValueInstanceConverterRegistry
{

    protected final Map<Class<?>, Collection<ValueInstanceConverter>> valueInstanceConvertersByClass = new HashMap<Class<?>, Collection<ValueInstanceConverter>>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerValueInstanceConverter(final Class<?> instanceClass, final ValueInstanceConverter converter)
    {
        ParameterCheck.mandatory("instanceClass", instanceClass);
        ParameterCheck.mandatory("converter", converter);

        Collection<ValueInstanceConverter> converters = this.valueInstanceConvertersByClass.get(instanceClass);
        if (converters == null)
        {
            converters = new HashSet<ValueInstanceConverter>();
            this.valueInstanceConvertersByClass.put(instanceClass, converters);
        }
        converters.add(converter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForJava(final Object value)
    {
        return this.canConvertValueForJava(value, Object.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForJava(final Object value, final Class<?> expectedClass)
    {
        ParameterCheck.mandatory("expectedClass", expectedClass);

        final boolean result;
        if (value != null)
        {
            final ValueInstanceConverter bestFittingConverter = this.lookupBestFittingConverter(value, expectedClass,
                    false);
            result = bestFittingConverter != null || expectedClass.isInstance(value);
        }
        else
        {
            result = true;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value)
    {
        return this.convertValueForJava(value, Object.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final Class<?> expectedClass)
    {
        ParameterCheck.mandatory("expectedClass", expectedClass);

        final Object result;
        if (value != null)
        {
            final ValueInstanceConverter bestFittingConverter = this.lookupBestFittingConverter(value, expectedClass,
                    false);
            if (bestFittingConverter != null)
            {
                result = bestFittingConverter.convertValueForJava(value, this, expectedClass);
            }
            else if (ClassUtils.isInstance(value, expectedClass))
            {
                result = value;
            }
            else
            {
                throw new UnsupportedOperationException("Can't convert " + value + " to " + expectedClass);
            }
        }
        else
        {
            result = value;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForScript(final Object value)
    {
        return this.canConvertValueForScript(value, Object.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForScript(final Object value, final Class<?> expectedClass)
    {
        ParameterCheck.mandatory("expectedClass", expectedClass);

        final boolean result;
        if (value != null)
        {
            final ValueInstanceConverter bestFittingConverter = this.lookupBestFittingConverter(value, expectedClass,
                    true);
            result = bestFittingConverter != null || expectedClass.isInstance(value);
        }
        else
        {
            result = true;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value)
    {
        return this.convertValueForScript(value, Object.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final Class<?> expectedClass)
    {
        ParameterCheck.mandatory("expectedClass", expectedClass);

        final Object result;
        if (value != null)
        {
            final ValueInstanceConverter bestFittingConverter = this.lookupBestFittingConverter(value, expectedClass,
                    true);
            if (bestFittingConverter != null)
            {
                result = bestFittingConverter.convertValueForScript(value, this, expectedClass);
            }
            else if (ClassUtils.isInstance(value, expectedClass))
            {
                result = value;
            }
            else
            {
                throw new UnsupportedOperationException("Can't convert " + value + " to " + expectedClass);
            }
        }
        else
        {
            result = null;
        }
        return result;
    }

    protected ValueInstanceConverter lookupBestFittingConverter(final Object valueInstance,
            final Class<?> expectedClass,
            final boolean javaToScript)
    {
        final Collection<ValueInstanceConverter> converters = this.lookupPrioritizedConverters(valueInstance,
                expectedClass, javaToScript);

        ValueInstanceConverter bestFittingConverter = null;
        for (final ValueInstanceConverter converter : converters)
        {
            final boolean canConvert = javaToScript ? converter.canConvertValueForScript(valueInstance, this,
                    expectedClass) : converter
                    .canConvertValueForJava(valueInstance, this, expectedClass);
            if (canConvert)
            {
                bestFittingConverter = converter;
                break;
            }
        }

        return bestFittingConverter;
    }

    protected Collection<ValueInstanceConverter> lookupPrioritizedConverters(final Object valueInstance,
            final Class<?> expectedClass,
            final boolean javaToScript)
    {
        Collection<ValueInstanceConverter> converters = new HashSet<ValueInstanceConverter>();

        if (valueInstance != null)
        {
            final Set<Class<?>> classesToCheck = new HashSet<Class<?>>();
            Class<? extends Object> currentClass = valueInstance.getClass();
            while (!Object.class.equals(currentClass))
            {
                classesToCheck.add(currentClass);
                classesToCheck.addAll(Arrays.asList(currentClass.getInterfaces()));

                currentClass = currentClass.getSuperclass();
            }
            classesToCheck.add(Object.class);

            for (final Class<?> cls : classesToCheck)
            {
                final Collection<ValueInstanceConverter> clsConverters = this.valueInstanceConvertersByClass.get(cls);
                if (clsConverters != null)
                {
                    converters.addAll(clsConverters);
                }
            }

            if (!converters.isEmpty())
            {
                converters = new ArrayList<ValueInstanceConverter>(converters);
                Collections.sort((List<ValueInstanceConverter>) converters,
                        new ValueInstanceConverterConfidenceComparator(valueInstance.getClass(), expectedClass,
                                javaToScript));
            }
        }

        return converters;
    }
}
