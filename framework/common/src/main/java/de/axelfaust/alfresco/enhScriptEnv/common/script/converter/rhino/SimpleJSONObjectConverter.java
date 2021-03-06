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

import java.util.Map;

import org.alfresco.util.PropertyCheck;
import org.json.simple.JSONObject;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * Generic value converter that transparently handles {@link JSONObject} values which should not be handled as mere {@link Map} when
 * converting back from script to Java.
 *
 * @author Axel Faust
 */
public class SimpleJSONObjectConverter implements ValueInstanceConverter, InitializingBean
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
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;

        if (JSONObject.class.isAssignableFrom(valueInstanceClass) && Object.class.isAssignableFrom(expectedClass)
                && expectedClass.isAssignableFrom(JSONObject.class))
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
        final boolean canConvert = value instanceof JSONObject && Object.class.isAssignableFrom(expectedClass)
                && expectedClass.isAssignableFrom(JSONObject.class);
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!(value instanceof JSONObject))
        {
            throw new IllegalArgumentException("value must be a JSONObject");
        }

        // no conversion - just transparent passthru (see org.springframework.extensions.webscripts.ScriptValueConverter)
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getForJavaConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;

        if (JSONObject.class.isAssignableFrom(valueInstanceClass) && Object.class.isAssignableFrom(expectedClass)
                && expectedClass.isAssignableFrom(JSONObject.class))
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
        final boolean canConvert = value instanceof JSONObject && Object.class.isAssignableFrom(expectedClass)
                && expectedClass.isAssignableFrom(JSONObject.class);
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!(value instanceof JSONObject))
        {
            throw new IllegalArgumentException("value must be a JSONObject");
        }

        // no conversion - just transparent passthru (see org.springframework.extensions.webscripts.ScriptValueConverter)
        return value;
    }
}
