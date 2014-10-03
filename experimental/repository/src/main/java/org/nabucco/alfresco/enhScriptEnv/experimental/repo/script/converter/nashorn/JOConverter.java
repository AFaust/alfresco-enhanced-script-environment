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

import java.util.HashMap;
import java.util.Map;

import jdk.nashorn.internal.scripts.JO;
import jdk.nashorn.internal.runtime.ScriptObject;

import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * Value converter class capable of converting Nashorn-specific {@link JO native objects} into {@link Map} instances.
 * This class is only to be used in case some kind of JavaScript-to-Java call passes a native object, e.g. because Java
 * API only requires {@code Object}.
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class JOConverter implements ValueInstanceConverter, InitializingBean
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

        this.registry.registerValueInstanceConverter(JO.class, this);
        this.registry.registerValueInstanceConverter(ScriptObject.class, this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        // no need to convert
        return LOWEST_CONFIDENCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForScript(final Object value, final ValueConverter globalDelegate,
            final Class<?> expectedClass)
    {
        // no need to convert
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate,
            final Class<?> expectedClass)
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
        if ((JO.class.isAssignableFrom(valueInstanceClass) || valueInstanceClass.getCanonicalName().startsWith(
                JO.class.getCanonicalName()))
                && expectedClass.isAssignableFrom(Map.class))
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
    public boolean canConvertValueForJava(final Object value, final ValueConverter globalDelegate,
            final Class<?> expectedClass)
    {
        boolean canConvert = (JO.class.isInstance(value) || value.getClass().getCanonicalName()
                .startsWith(JO.class.getCanonicalName()))
                && expectedClass.isAssignableFrom(Map.class);

        if (canConvert)
        {
            final ScriptObject obj = (ScriptObject) value;
            for (final String key : obj.getOwnKeys(false))
            {
                final Object element = obj.get(key);
                canConvert = canConvert && globalDelegate.canConvertValueForJava(element);
            }
        }

        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate,
            final Class<?> expectedClass)
    {
        if (!(value instanceof ScriptObject))
        {
            throw new IllegalArgumentException("value must be a " + ScriptObject.class);
        }

        final ScriptObject obj = (ScriptObject) value;
        final Map<String, Object> propValues = new HashMap<String, Object>();

        for (final String key : obj.getOwnKeys(false))
        {
            final Object element = obj.get(key);
            propValues.put(key, globalDelegate.convertValueForJava(element));
        }

        final Object result = propValues;
        return result;
    }

}
