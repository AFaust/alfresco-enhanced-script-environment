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

import java.util.ArrayList;
import java.util.List;

import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;
import org.mozilla.javascript.Wrapper;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * Value instance converter implementation supporting NativeMap handling for a Rhino script engine within Surf.
 *
 * @author Axel Faust
 */
public class WrapFactoryConverter implements ValueInstanceConverter, InitializingBean
{

    protected ValueInstanceConverterRegistry registry;

    protected ThreadLocal<List<Object>> currentConversions = new ThreadLocal<List<Object>>()
    {

        /**
         * {@inheritDoc}
         */
        @Override
        protected List<Object> initialValue()
        {
            return new ArrayList<Object>();
        }

    };

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

        this.registry.registerValueInstanceConverter(Object.class, this);
        this.registry.registerValueInstanceConverter(Wrapper.class, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        // lowest confidence so we are always picked last
        return LOWEST_CONFIDENCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        final boolean canConvert = Context.getCurrentContext() != null && expectedClass.isAssignableFrom(Scriptable.class)
                && !this.currentConversions.get().contains(value);
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        // no further distinction - always delegate
        final Context currentContext = Context.getCurrentContext();
        final WrapFactory factory = currentContext.getWrapFactory();

        final Object result;

        // mark for recursion prevention (DelegatingWrapFactory may otherwise indirectly delegate back)
        this.currentConversions.get().add(value);
        try
        {
            // if conversion call is made in a scope-ful context, the caller needs to take care of setting parentScope for Scriptable
            result = factory.wrap(currentContext, null, value, null);
        }
        finally
        {
            this.currentConversions.get().remove(value);
        }

        // we tried to check in advance as best as possible in #canConvertValueForScript
        return expectedClass.isInstance(result) ? result : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getForJavaConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        // lowest confidence so we are always picked last
        return LOWEST_CONFIDENCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        return value instanceof Wrapper
                && (expectedClass.isInstance(((Wrapper) value).unwrap()) || globalDelegate.canConvertValueForJava(
                        ((Wrapper) value).unwrap(), expectedClass));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        final Object result;

        if (value instanceof Wrapper)
        {
            final Object unwrapped = ((Wrapper) value).unwrap();
            if (!expectedClass.isInstance(unwrapped))
            {
                result = globalDelegate.convertValueForJava(unwrapped, expectedClass);
            }
            else
            {
                result = unwrapped;
            }
        }
        else
        {
            result = null;
        }

        return result;
    }

}
