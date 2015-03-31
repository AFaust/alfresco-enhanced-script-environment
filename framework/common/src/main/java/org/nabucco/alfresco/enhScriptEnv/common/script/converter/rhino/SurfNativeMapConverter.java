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
package org.nabucco.alfresco.enhScriptEnv.common.script.converter.rhino;

import java.util.Map;

import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.NativeMap;

/**
 * Value instance converter implementation supporting NativeMap handling for a Rhino script engine within Surf.
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class SurfNativeMapConverter implements ValueInstanceConverter, InitializingBean
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

        this.registry.registerValueInstanceConverter(Map.class, this);
        this.registry.registerValueInstanceConverter(NativeMap.class, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;

        if (expectedClass.isAssignableFrom(NativeMap.class) && Map.class.isAssignableFrom(valueInstanceClass))
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
        final boolean result = value instanceof Map<?, ?> && expectedClass.isAssignableFrom(NativeMap.class)
                && globalDelegate.canConvertValueForScript(value, Map.class);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!(value instanceof Map<?, ?>))
        {
            throw new IllegalArgumentException("value must be a Map");
        }

        // no further distinction - always create NativeMap

        // the delegate call to convert Map-to-Map is for transparent conversion of keys&values
        @SuppressWarnings("unchecked")
        final Map<Object, Object> map = (Map<Object, Object>) globalDelegate.convertValueForScript(value, Map.class);
        // if conversion call is made in a scope-ful context, the caller needs to take care of setting parentScope for Scriptable
        final Object result = new NativeMap(null, map);

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getForJavaConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;

        if (NativeMap.class.isAssignableFrom(valueInstanceClass) && expectedClass.isAssignableFrom(Map.class))
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
        final boolean result = value instanceof NativeMap && expectedClass.isAssignableFrom(Map.class)
                && globalDelegate.canConvertValueForJava(((NativeMap) value).unwrap(), Map.class);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!(value instanceof NativeMap))
        {
            throw new IllegalArgumentException("value must be a NativeMap");
        }

        // the delegate call to convert Map-to-Map is for transparent conversion of keys&values
        final Object result = globalDelegate.convertValueForJava(((NativeMap) value).unwrap(), Map.class);

        return result;
    }

}
