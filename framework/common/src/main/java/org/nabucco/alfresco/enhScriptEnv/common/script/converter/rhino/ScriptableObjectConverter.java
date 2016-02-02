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
package org.nabucco.alfresco.enhScriptEnv.common.script.converter.rhino;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.IdScriptableObject;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * Value instance converter implementation supporting NativeMap handling for a Rhino script engine within Surf.
 *
 * @author Axel Faust
 */
public class ScriptableObjectConverter implements ValueInstanceConverter, InitializingBean
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

        this.registry.registerValueInstanceConverter(IdScriptableObject.class, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        return LOWEST_CONFIDENCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
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
     * {@inheritDoc}
     */
    @Override
    public int getForJavaConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;

        if (IdScriptableObject.class.isAssignableFrom(valueInstanceClass) && expectedClass.isAssignableFrom(Map.class))
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
        boolean canConvert = value instanceof IdScriptableObject && expectedClass.isAssignableFrom(Map.class);

        if (canConvert)
        {
            final IdScriptableObject object = (IdScriptableObject) value;
            final Object[] propIds = object.getIds();
            for (int i = 0; i < propIds.length; i++)
            {
                // work on each key in turn
                final Object propId = propIds[i];

                // we are only interested in keys that indicate a list of values
                if (propId instanceof String)
                {
                    // get the value out for the specified key
                    final Object val = object.get((String) propId, object);
                    canConvert = canConvert && globalDelegate.canConvertValueForJava(val);
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
        if (!(value instanceof IdScriptableObject))
        {
            throw new IllegalArgumentException("value must be a IdScriptableObject");
        }

        final IdScriptableObject object = (IdScriptableObject) value;

        final Object[] propIds = object.getIds();
        final Map<String, Object> propValues = new HashMap<String, Object>(propIds.length);
        for (int i = 0; i < propIds.length; i++)
        {
            // work on each key in turn
            final Object propId = propIds[i];

            // we are only interested in keys that indicate a list of values
            if (propId instanceof String)
            {
                // get the value out for the specified key
                final Object val = object.get((String) propId, object);
                // recursively call this method to convert the value
                propValues.put((String) propId, globalDelegate.convertValueForJava(val));
            }
        }

        return propValues;
    }

}
