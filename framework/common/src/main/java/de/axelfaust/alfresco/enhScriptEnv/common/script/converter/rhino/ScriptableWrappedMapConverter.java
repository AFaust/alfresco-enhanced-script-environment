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

import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.ScriptValueConverter;
import org.springframework.extensions.webscripts.ScriptableWrappedMap;

import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;

/**
 * This converter is primarily used to remove any instances of {@link ScriptableWrappedMap} - when possible - in favor of AOP-based
 * scriptable maps including transparent value conversion (eliminating multiple different conversion approaches, like the
 * {@link ScriptValueConverter} used by {@link ScriptableWrappedMap}). This can be done safely since we retain the wrapped map and thus keep
 * script API internals relying on by-reference identity intact.
 *
 * @author Axel Faust
 */
public class ScriptableWrappedMapConverter implements ValueInstanceConverter, InitializingBean
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

        this.registry.registerValueInstanceConverter(ScriptableWrappedMap.class, this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if (ScriptableWrappedMap.class.isAssignableFrom(valueInstanceClass) && !ScriptableWrappedMap.class.isAssignableFrom(expectedClass))
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
    public boolean canConvertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        final boolean canConvert = ScriptableWrappedMap.class.isInstance(value)
                && !ScriptableWrappedMap.class.isAssignableFrom(expectedClass)
                && globalDelegate.canConvertValueForScript(((ScriptableWrappedMap) value).unwrap().getClass(), expectedClass);
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!(value instanceof ScriptableWrappedMap))
        {
            throw new IllegalArgumentException("value must be a " + ScriptableWrappedMap.class);
        }

        if (ScriptableWrappedMap.class.isAssignableFrom(expectedClass))
        {
            throw new IllegalArgumentException("expected class must not be assignable to " + ScriptableWrappedMap.class);
        }

        final Object result;

        // Unless we have a very specific Map instance, ScriptableFacadeMapConverter should ensure we keep list-like access functionality of
        // ScriptableWrappedMap
        result = globalDelegate.convertValueForScript(((ScriptableWrappedMap) value).unwrap(), expectedClass);

        return result;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForJavaConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        // can't convert anything
        return LOWEST_CONFIDENCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        // can't convert anything
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        // clients should check canConvertValueForJava first
        throw new UnsupportedOperationException("This operation is not supported and should not have been called");
    }
}
