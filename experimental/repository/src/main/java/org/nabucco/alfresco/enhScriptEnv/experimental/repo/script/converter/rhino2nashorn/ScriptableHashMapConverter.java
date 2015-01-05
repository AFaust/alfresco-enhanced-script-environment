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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.rhino2nashorn;

import java.util.Arrays;
import java.util.List;

import org.alfresco.repo.jscript.ScriptableHashMap;
import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.nabucco.alfresco.enhScriptEnv.common.util.ClassUtils;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.AdapterObject;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.AdapterObjectInterceptor;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.ListLikeMapAdapterInterceptor;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.MapLengthFacadeInterceptor;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.ScriptableAdapterInterceptor;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.ValueConvertingMapInterceptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ScriptableHashMapConverter implements ValueInstanceConverter, InitializingBean
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

        this.registry.registerValueInstanceConverter(ScriptableHashMap.class, this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if (ScriptableHashMap.class.isAssignableFrom(valueInstanceClass) && expectedClass.isAssignableFrom(valueInstanceClass))
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
        final boolean canConvert = ScriptableHashMap.class.isInstance(value) && !AdapterObject.class.isInstance(value)
                && expectedClass.isAssignableFrom(value.getClass());
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!(value instanceof ScriptableHashMap<?, ?>))
        {
            throw new IllegalArgumentException("value must be a " + ScriptableHashMap.class);
        }

        final Object result;

        // we know ScriptableHashMap has a no-arg constructor
        final ProxyFactory proxyFactory = new ProxyFactory();

        proxyFactory.addAdvice(new AdapterObjectInterceptor());
        proxyFactory.addAdvice(new MapLengthFacadeInterceptor());
        proxyFactory.addAdvice(new ScriptableAdapterInterceptor());
        proxyFactory.addAdvice(new ListLikeMapAdapterInterceptor());
        proxyFactory.addAdvice(new ValueConvertingMapInterceptor(globalDelegate));

        // TODO ScriptableParameterMap needs delegation to Scriptable.delete / Scriptable.put for tracking of modified state

        proxyFactory.setInterfaces(ClassUtils.collectInterfaces(value, Arrays.<Class<?>> asList(List.class, AdapterObject.class)));

        proxyFactory.setTarget(value);
        proxyFactory.setProxyTargetClass(true);

        result = proxyFactory.getProxy();
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
