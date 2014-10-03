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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jdk.nashorn.internal.objects.NativeNumber;
import jdk.nashorn.internal.runtime.JSType;

import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class NativeNumberConverter implements ValueInstanceConverter, InitializingBean
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

        this.registry.registerValueInstanceConverter(NativeNumber.class, this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        // no need to convert java.lang.Number
        return LOWEST_CONFIDENCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        // no need to convert java.lang.Number
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        // clients should check canConvertValueForScript first
        throw new UnsupportedOperationException("This operation is not supported and should not have been called");
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForJavaConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if (NativeNumber.class.isAssignableFrom(valueInstanceClass)
                && (SIMPLE_NUMBER_CLASSES.contains(expectedClass) || expectedClass.isAssignableFrom(Number.class)))
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
    public boolean canConvertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        final boolean canConvert = NativeNumber.class.isInstance(value)
                && (SIMPLE_NUMBER_CLASSES.contains(expectedClass) || expectedClass.isAssignableFrom(Number.class));
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!(value instanceof NativeNumber))
        {
            throw new IllegalArgumentException("value must be a " + NativeNumber.class);
        }

        final double actualValue = JSType.toNumber(value);

        final Object result;
        if (Double.isNaN(actualValue))
        {
            if (Double.class.equals(expectedClass) || Number.class.equals(expectedClass) || Object.class.equals(expectedClass)
                    || double.class.equals(expectedClass))
            {
                result = Double.valueOf(Double.NaN);
            }
            else if (Float.class.equals(expectedClass) || Number.class.equals(expectedClass) || float.class.equals(expectedClass))
            {
                result = Float.valueOf(Float.NaN);
            }
            else if (expectedClass.isPrimitive())
            {
                throw new ClassCastException();
            }
            else
            {
                result = null;
            }
        }
        else if (Double.isInfinite(actualValue))
        {
            if (Double.class.equals(expectedClass) || Number.class.equals(expectedClass) || Object.class.equals(expectedClass)
                    || double.class.equals(expectedClass))
            {
                result = Double.valueOf(actualValue);
            }
            else if (Float.class.equals(expectedClass) || Number.class.equals(expectedClass) || float.class.equals(expectedClass))
            {
                result = Float.valueOf(actualValue == Double.POSITIVE_INFINITY ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY);
            }
            else if (expectedClass.isPrimitive())
            {
                throw new ClassCastException();
            }
            else if (expectedClass.equals(Long.class))
            {
                result = Long.valueOf(actualValue == Double.POSITIVE_INFINITY ? Long.MAX_VALUE : Long.MIN_VALUE);
            }
            else if (expectedClass.equals(Integer.class))
            {
                result = Integer.valueOf(actualValue == Double.POSITIVE_INFINITY ? Integer.MAX_VALUE : Integer.MIN_VALUE);
            }
            else if (expectedClass.equals(Short.class))
            {
                result = Short.valueOf(actualValue == Double.POSITIVE_INFINITY ? Short.MAX_VALUE : Short.MIN_VALUE);
            }
            else if (expectedClass.equals(Byte.class))
            {
                result = Byte.valueOf(actualValue == Double.POSITIVE_INFINITY ? Byte.MAX_VALUE : Byte.MIN_VALUE);
            }
            else
            {
                // there is no more case to cover, but compiler can't determine that
                result = null;
            }
        }
        else
        {
            if (expectedClass.isAssignableFrom(Number.class))
            {
                if (Double.valueOf(actualValue).longValue() == actualValue)
                {
                    result = Long.valueOf(Double.valueOf(actualValue).longValue());
                }
                else
                {
                    result = Double.valueOf(actualValue);
                }
            }
            else if (Integer.class.equals(expectedClass) || int.class.equals(expectedClass))
            {
                result = Integer.valueOf(Double.valueOf(actualValue).intValue());
            }
            else if (Long.class.equals(expectedClass) || long.class.equals(expectedClass))
            {
                result = Long.valueOf(Double.valueOf(actualValue).longValue());
            }
            else if (Float.class.equals(expectedClass) || float.class.equals(expectedClass))
            {
                result = Float.valueOf(Double.valueOf(actualValue).floatValue());
            }
            else if (Double.class.equals(expectedClass) || double.class.equals(expectedClass))
            {
                result = Double.valueOf(Double.valueOf(actualValue).doubleValue());
            }
            else if (Byte.class.equals(expectedClass) || byte.class.equals(expectedClass))
            {
                result = Byte.valueOf(Double.valueOf(actualValue).byteValue());
            }
            else
            {
                result = Short.valueOf(Double.valueOf(actualValue).shortValue());
            }
        }

        return result;
    }

}
