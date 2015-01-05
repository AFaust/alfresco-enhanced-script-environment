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

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.alfresco.scripts.ScriptException;
import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueInstanceConverterRegistry.ValueInstanceConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class AbstractScriptEvaluationBasedConverter implements ValueInstanceConverter, InitializingBean
{
    private static final String NASHORN_ENGINE_NAME = "nashorn";

    protected static final String JAVA_OBJ = "javaObj";

    protected static final String NASHORN_OBJ = "nashornObj";

    protected ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(NASHORN_ENGINE_NAME);

    protected ThreadLocal<ScriptContext> cachedContexts;

    protected ValueInstanceConverterRegistry registry;

    /**
     * {@code true} if Nashorn does not require native Java objects represented by {@link #javaBaseClass} to be converted,{@code false}
     * otherwise. This field defaults to true as Nashorn is supposed to make integration with most native objects a breeze.
     */
    protected boolean skipToNashornMapping = true;

    protected Class<?> javaBaseClass;

    protected Class<?> nashornBaseClass;

    /**
     * @param scriptEngine
     *            the scriptEngine to set
     */
    public final void setScriptEngine(final ScriptEngine scriptEngine)
    {
        this.scriptEngine = scriptEngine;
    }

    /**
     * @param cachedContexts
     *            the cachedContexts to set
     */
    public final void setCachedContexts(final ThreadLocal<ScriptContext> cachedContexts)
    {
        this.cachedContexts = cachedContexts;
    }

    /**
     * @param registry
     *            the registry to set
     */
    public void setRegistry(final ValueInstanceConverterRegistry registry)
    {
        this.registry = registry;
    }

    /**
     * @param skipToNashornMapping
     *            the skipToNashornMapping to set
     */
    public void setSkipToNashornMapping(final boolean skipToNashornMapping)
    {
        this.skipToNashornMapping = skipToNashornMapping;
    }

    /**
     * @param javaBaseClass
     *            the javaBaseClass to set
     */
    public void setJavaBaseClass(final Class<?> javaBaseClass)
    {
        this.javaBaseClass = javaBaseClass;
    }

    /**
     * @param nashornBaseClass
     *            the nashornBaseClass to set
     */
    public void setNashornBaseClass(final Class<?> nashornBaseClass)
    {
        this.nashornBaseClass = nashornBaseClass;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "scriptEngine", this.scriptEngine);
        PropertyCheck.mandatory(this, "cachedBindings", this.cachedContexts);
        PropertyCheck.mandatory(this, "registry", this.registry);

        PropertyCheck.mandatory(this, "javaBaseClass", this.javaBaseClass);
        PropertyCheck.mandatory(this, "nashornBaseClass", this.nashornBaseClass);

        this.registry.registerValueInstanceConverter(this.javaBaseClass, this);
        this.registry.registerValueInstanceConverter(this.nashornBaseClass, this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if (this.javaBaseClass.isAssignableFrom(valueInstanceClass)
                && ((this.skipToNashornMapping && expectedClass.isAssignableFrom(this.javaBaseClass)) || expectedClass
                        .isAssignableFrom(this.nashornBaseClass)))
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
        final boolean canConvert = this.javaBaseClass.isInstance(value)
                && ((this.skipToNashornMapping && expectedClass.isAssignableFrom(this.javaBaseClass)) || expectedClass
                        .isAssignableFrom(this.nashornBaseClass));
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!this.javaBaseClass.isInstance(value))
        {
            throw new IllegalArgumentException("value must be a " + this.javaBaseClass);
        }

        final Object result;

        if (this.skipToNashornMapping)
        {
            result = value;
        }
        else
        {
            // this may be rather expensive per call, so use (potentially reuse) thead-local script contexts
            final ScriptContext ctxt = this.cachedContexts.get();
            try
            {
                ctxt.setAttribute(JAVA_OBJ, value, ScriptContext.GLOBAL_SCOPE);
                this.executeForScriptConversionScript(ctxt, this.scriptEngine);
                result = ctxt.getAttribute(NASHORN_OBJ);
            }
            catch (final javax.script.ScriptException scriptEx)
            {
                throw new ScriptException("Failed to convert object to Nashorn", scriptEx);
            }
            finally
            {
                // clean
                ctxt.removeAttribute(NASHORN_OBJ, ScriptContext.ENGINE_SCOPE);
                ctxt.removeAttribute(JAVA_OBJ, ScriptContext.GLOBAL_SCOPE);
            }
        }

        return result;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForJavaConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if (this.nashornBaseClass.isAssignableFrom(valueInstanceClass) && expectedClass.isAssignableFrom(this.javaBaseClass))
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
    public boolean canConvertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        final boolean canConvert = this.nashornBaseClass.isInstance(value) && expectedClass.isAssignableFrom(this.javaBaseClass);
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate, final Class<?> expectedClass)
    {
        if (!this.nashornBaseClass.isInstance(value))
        {
            throw new IllegalArgumentException("value must be a " + this.nashornBaseClass);
        }

        final Object result;
        // this may be rather expensive per call, so use (potentially reuse) thead-local script contexts
        final ScriptContext ctxt = this.cachedContexts.get();
        try
        {
            ctxt.setAttribute(NASHORN_OBJ, value, ScriptContext.GLOBAL_SCOPE);
            this.executeForJavaConversionScript(ctxt, this.scriptEngine);
            result = ctxt.getAttribute(JAVA_OBJ);
        }
        catch (final javax.script.ScriptException scriptEx)
        {
            throw new ScriptException("Failed to convert object to Nashorn", scriptEx);
        }
        finally
        {
            // clean
            ctxt.removeAttribute(JAVA_OBJ, ScriptContext.ENGINE_SCOPE);
            ctxt.removeAttribute(NASHORN_OBJ, ScriptContext.GLOBAL_SCOPE);
        }

        return result;
    }

    protected abstract void executeForScriptConversionScript(final ScriptContext ctx, final ScriptEngine scriptEngine)
            throws javax.script.ScriptException;

    protected abstract void executeForJavaConversionScript(final ScriptContext ctx, final ScriptEngine scriptEngine)
            throws javax.script.ScriptException;
}
