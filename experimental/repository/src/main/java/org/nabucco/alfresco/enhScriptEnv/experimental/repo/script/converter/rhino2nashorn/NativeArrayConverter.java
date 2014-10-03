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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.rhino2nashorn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.NativeArray;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class NativeArrayConverter implements ValueInstanceConverter, InitializingBean
{

    private static final HashMap<Object, Object> DUMMY_MAP = new HashMap<Object, Object>();
    private static final Object[] DUMMY_ARRAY = new Object[0];

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
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if (NativeArray.class.isAssignableFrom(valueInstanceClass))
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
        boolean canConvert = NativeArray.class.isInstance(value);

        if (canConvert)
        {
            final NativeArray arr = (NativeArray) value;
            final Object[] ids = arr.getIds();

            if (this.isArray(ids))
            {
                canConvert = canConvert && globalDelegate.canConvertValueForScript(DUMMY_ARRAY, expectedClass);
            }
            else
            {
                canConvert = canConvert && globalDelegate.canConvertValueForScript(DUMMY_MAP, expectedClass);
            }

            if (canConvert)
            {
                for (final Object id : ids)
                {
                    final Object element = id instanceof Integer ? arr.get(((Integer) id).intValue(), arr) : arr.get(id.toString(), arr);

                    if (expectedClass.isArray())
                    {
                        canConvert = canConvert && globalDelegate.canConvertValueForScript(element, expectedClass.getComponentType());
                    }
                    else
                    {
                        canConvert = canConvert && globalDelegate.canConvertValueForScript(element);
                    }
                }
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
        if (!(value instanceof NativeArray))
        {
            throw new IllegalArgumentException("value must be a " + NativeArray.class);
        }

        final NativeArray arr = (NativeArray) value;
        final Object[] ids = arr.getIds();

        final Object result;
        if (this.isArray(ids))
        {
            final List<Object> values = new ArrayList<Object>();

            for (final Object id : ids)
            {
                final Object element = arr.get(((Integer) id).intValue(), arr);
                final Object converted = globalDelegate.convertValueForScript(element,
                        expectedClass.isArray() ? expectedClass.getComponentType() : Object.class);
                values.add(converted);
            }

            result = globalDelegate.convertValueForScript(values, expectedClass);
        }
        else
        {
            final Map<Object, Object> values = new HashMap<Object, Object>();

            for (final Object id : ids)
            {
                final Object element = arr.get(id.toString(), arr);
                final Object converted = globalDelegate.convertValueForScript(element);
                values.put(id, converted);
            }

            result = globalDelegate.convertValueForScript(values, expectedClass);
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
        if (NativeArray.class.isAssignableFrom(valueInstanceClass))
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
        boolean canConvert = NativeArray.class.isInstance(value);

        if (canConvert)
        {
            final NativeArray arr = (NativeArray) value;
            final Object[] ids = arr.getIds();

            if (this.isArray(ids))
            {
                canConvert = canConvert && globalDelegate.canConvertValueForJava(DUMMY_ARRAY, expectedClass);
            }
            else
            {
                canConvert = canConvert && globalDelegate.canConvertValueForJava(DUMMY_MAP, expectedClass);
            }

            if (canConvert)
            {
                for (final Object id : ids)
                {
                    final Object element = id instanceof Integer ? arr.get(((Integer) id).intValue(), arr) : arr.get(id.toString(), arr);

                    if (expectedClass.isArray())
                    {
                        canConvert = canConvert && globalDelegate.canConvertValueForJava(element, expectedClass.getComponentType());
                    }
                    else
                    {
                        canConvert = canConvert && globalDelegate.canConvertValueForJava(element);
                    }
                }
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
        final Object[] ids = arr.getIds();

        final Object result;
        if (this.isArray(ids))
        {
            final List<Object> values = new ArrayList<Object>();

            for (final Object id : ids)
            {
                final Object element = arr.get(((Integer) id).intValue(), arr);
                final Object converted = globalDelegate.convertValueForJava(element,
                        expectedClass.isArray() ? expectedClass.getComponentType() : Object.class);
                values.add(converted);
            }

            result = globalDelegate.convertValueForJava(values, expectedClass);
        }
        else
        {
            final Map<Object, Object> values = new HashMap<Object, Object>();

            for (final Object id : ids)
            {
                final Object element = arr.get(id.toString(), arr);
                final Object converted = globalDelegate.convertValueForJava(element);
                values.put(id, converted);
            }

            result = globalDelegate.convertValueForJava(values, expectedClass);
        }

        return result;
    }

    protected boolean isArray(final Object[] ids)
    {
        boolean result = true;
        for (final Object id : ids)
        {
            if (!(id instanceof Integer))
            {
                result = false;
                break;
            }
        }
        return result;
    }
}