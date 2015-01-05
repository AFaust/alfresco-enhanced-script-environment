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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.functions;

import java.lang.reflect.MalformedParameterizedTypeException;

import javax.script.ScriptEngine;

import org.nabucco.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.functions.AbstractImportScriptFunction;
import org.nabucco.alfresco.enhScriptEnv.common.script.locator.ScriptLocatorRegistry;
import org.nabucco.alfresco.enhScriptEnv.repo.script.ScriptLocationAdapter;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * This class is a Spring-related utility to deal with bean instantiation issues using default Spring bean definitions and generic classes.
 * Specifically, addition of second generic type to take {@link ScriptObjectMirror} in parent class {@link AbstractImportScriptFunction}
 * introduced {@link MalformedParameterizedTypeException}
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("rawtypes")
public class NashornImportScriptFunctionFactoryBean extends AbstractFactoryBean<NashornImportScriptFunction>
{

    protected EnhancedScriptProcessor<ScriptLocationAdapter> scriptProcessor;
    protected ValueConverter valueConverter;
    protected ScriptLocatorRegistry<ScriptLocationAdapter> locatorRegistry;
    protected ScriptEngine scriptEngine;

    /**
     * @param scriptProcessor
     *            the scriptProcessor to set
     */
    public void setScriptProcessor(final EnhancedScriptProcessor<ScriptLocationAdapter> scriptProcessor)
    {
        this.scriptProcessor = scriptProcessor;
    }

    /**
     * @param valueConverter
     *            the valueConverter to set
     */
    public void setValueConverter(final ValueConverter valueConverter)
    {
        this.valueConverter = valueConverter;
    }

    /**
     * @param locatorRegistry
     *            the locatorRegistry to set
     */
    public void setLocatorRegistry(final ScriptLocatorRegistry<ScriptLocationAdapter> locatorRegistry)
    {
        this.locatorRegistry = locatorRegistry;
    }

    /**
     * @param scriptEngine
     *            the scriptEngine to set
     */
    public void setScriptEngine(final ScriptEngine scriptEngine)
    {
        this.scriptEngine = scriptEngine;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Class<NashornImportScriptFunction> getObjectType()
    {
        return NashornImportScriptFunction.class;
    }

    @Override
    protected NashornImportScriptFunction createInstance() throws Exception
    {
        final NashornImportScriptFunction<ScriptLocationAdapter> nisf = new NashornImportScriptFunction<>();

        // can't use configureBean from ConfiguredApplicationContext due to same issues with generic type parameters
        if (this.scriptProcessor != null)
        {
            nisf.setScriptProcessor(this.scriptProcessor);
        }
        if (this.scriptEngine != null)
        {
            nisf.setScriptEngine(this.scriptEngine);
        }
        if (this.valueConverter != null)
        {
            nisf.setValueConverter(this.valueConverter);
        }
        if (this.locatorRegistry != null)
        {
            nisf.setLocatorRegistry(this.locatorRegistry);
        }

        nisf.afterPropertiesSet();
        return nisf;
    }

}
