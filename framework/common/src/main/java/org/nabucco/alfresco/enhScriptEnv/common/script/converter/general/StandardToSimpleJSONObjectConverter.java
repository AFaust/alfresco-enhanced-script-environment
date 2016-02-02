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
package org.nabucco.alfresco.enhScriptEnv.common.script.converter.general;

import java.util.Iterator;

import org.alfresco.scripts.ScriptException;
import org.alfresco.util.PropertyCheck;
import org.json.JSONException;
import org.json.JSONObject;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust
 */
public class StandardToSimpleJSONObjectConverter implements ValueInstanceConverter, InitializingBean
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

        this.registry.registerValueInstanceConverter(JSONObject.class, this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if (org.json.simple.JSONObject.class.equals(expectedClass) && JSONObject.class.isAssignableFrom(valueInstanceClass))
        {
            confidence = HIGH_CONFIDENCE;
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
        return this.canConvertImpl(value, globalDelegate, expectedClass, true);
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

        return this.convertImpl(value, globalDelegate, expectedClass, true);
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
        return this.canConvertImpl(value, globalDelegate, expectedClass, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!(value instanceof JSONObject))
        {
            throw new IllegalArgumentException("value must be a " + JSONObject.class);
        }

        return this.convertImpl(value, globalDelegate, expectedClass, false);
    }

    protected boolean canConvertImpl(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass,
            final boolean forScript)
    {
        boolean canConvert = value instanceof JSONObject && org.json.simple.JSONObject.class.equals(expectedClass);

        if (canConvert)
        {
            final JSONObject jsonObj = (JSONObject) value;

            try
            {
                final Iterator<?> keys = jsonObj.keys();
                for (Object key = keys.next(); keys.hasNext(); key = keys.next())
                {
                    final Object subValue = jsonObj.get(String.valueOf(key));
                    canConvert = canConvert
                            && (forScript ? globalDelegate.canConvertValueForScript(subValue) : globalDelegate
                                    .canConvertValueForJava(subValue));
                }
            }
            catch (final JSONException ex)
            {
                canConvert = false;
            }
        }

        return canConvert;
    }

    protected org.json.simple.JSONObject convertImpl(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass,
            final boolean forScript)
    {
        final JSONObject jsonObj = (JSONObject) value;

        final org.json.simple.JSONObject result = new org.json.simple.JSONObject();
        try
        {
            final Iterator<?> keys = jsonObj.keys();
            for (Object key = keys.next(); keys.hasNext(); key = keys.next())
            {
                final Object subValue = jsonObj.get(String.valueOf(key));

                result.put(key,
                        (forScript ? globalDelegate.canConvertValueForScript(subValue) : globalDelegate.canConvertValueForJava(subValue)));
            }
        }
        catch (final JSONException ex)
        {
            throw new ScriptException("Error converting JSONObject", ex);
        }

        return result;
    }
}
