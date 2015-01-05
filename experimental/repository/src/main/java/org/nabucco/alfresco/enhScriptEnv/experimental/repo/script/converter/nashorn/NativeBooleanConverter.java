/*
 * Copyright 2014 PRODYNA AG
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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.nashorn;

import jdk.nashorn.internal.objects.NativeBoolean;
import jdk.nashorn.internal.runtime.JSType;

import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * Converter handling Nashorn-specific {@link NativeBoolean native boolean} to Java boolean conversion. This class is
 * only to be used in case some kind of JavaScript-to-Java call passes a native boolean, e.g. because Java API only
 * requires {@code Object}.
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class NativeBooleanConverter implements ValueInstanceConverter, InitializingBean
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

        this.registry.registerValueInstanceConverter(NativeBoolean.class, this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        // no need to convert java.lang.Boolean
        return LOWEST_CONFIDENCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForScript(final Object value, final ValueConverter globalDelegate,
            final Class<?> expectedClass)
    {
        // no need to convert java.lang.Boolean
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
        if (NativeBoolean.class.isAssignableFrom(valueInstanceClass)
                && (expectedClass.isAssignableFrom(Boolean.class) || expectedClass.equals(boolean.class)))
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
        final boolean canConvert = NativeBoolean.class.isInstance(value)
                && (expectedClass.isAssignableFrom(Boolean.class) || expectedClass.equals(boolean.class));
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate,
            final Class<?> expectedClass)
    {
        if (!(value instanceof NativeBoolean))
        {
            throw new IllegalArgumentException("value must be a " + NativeBoolean.class);
        }

        final Object result = Boolean.valueOf(JSType.toBoolean(value));

        return result;
    }

}
