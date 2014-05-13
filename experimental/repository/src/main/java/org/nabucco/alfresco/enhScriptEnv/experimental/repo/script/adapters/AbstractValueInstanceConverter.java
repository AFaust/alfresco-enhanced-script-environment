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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.adapters;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;

import org.alfresco.repo.jscript.Scopeable;
import org.alfresco.scripts.ScriptException;
import org.alfresco.util.PropertyCheck;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornScriptProcessor;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornValueInstanceConverterRegistry;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornValueInstanceConverterRegistry.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornValueInstanceConverterRegistry.ValueInstanceConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class AbstractValueInstanceConverter implements ValueInstanceConverter, InitializingBean
{
    private static final String NASHORN_ENGINE_NAME = "nashorn";
    protected static final Scriptable DUMMY_SCOPE;
    static
    {
        final Context cx = Context.enter();
        try
        {
            DUMMY_SCOPE = cx.initStandardObjects(null, true);
            DUMMY_SCOPE.delete("Packages");
            DUMMY_SCOPE.delete("getClass");
            DUMMY_SCOPE.delete("java");
            ((NativeObject) DUMMY_SCOPE).sealObject();
        }
        finally
        {
            Context.exit();
        }
    }

    protected class RhinoSpecificBeanInterceptor implements MethodInterceptor
    {
        private final ValueConverter globalDelegate;

        protected RhinoSpecificBeanInterceptor(final ValueConverter globalDelegate)
        {
            this.globalDelegate = globalDelegate;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public Object invoke(final MethodInvocation invocation) throws Throwable
        {
            // need to make sure we have a context
            Context.enter();
            try
            {
                // also need to make sure we have a scope
                final Object thisObj = invocation.getThis();
                final Method method = invocation.getMethod();
                if (thisObj instanceof Scopeable && !Scopeable.class.equals(method.getDeclaringClass()))
                {
                    ((Scopeable) thisObj).setScope(DUMMY_SCOPE);
                }

                AbstractValueInstanceConverter.this.preRhinoBeanInvocation(invocation);

                final Object result = invocation.proceed();
                final Object convertedResult = this.globalDelegate.convertValueForNashorn(result);
                return convertedResult;
            }
            finally
            {
                Context.exit();
            }
        }
    }

    protected final ThreadLocal<Bindings> localBindings = new ThreadLocal<Bindings>()
    {

        /**
         * {@inheritDoc}
         */
        @Override
        protected Bindings initialValue()
        {
            return AbstractValueInstanceConverter.this.scriptEngine.createBindings();
        }

    };

    protected ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(NASHORN_ENGINE_NAME);
    protected NashornValueInstanceConverterRegistry registry;
    protected Class<?> convertableClass;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "scriptEngine", this.scriptEngine);
        PropertyCheck.mandatory(this, "registry", this.registry);
        PropertyCheck.mandatory(this, "convertableClass", this.convertableClass);

        this.registry.registerValueInstanceConverter(this.convertableClass, this);
    }

    /**
     * @param scriptEngine
     *            the scriptEngine to set
     */
    public final void setScriptEngine(final ScriptEngine scriptEngine)
    {
        this.scriptEngine = scriptEngine;
    }

    /**
     * @param registry
     *            the registry to set
     */
    public final void setRegistry(final NashornValueInstanceConverterRegistry registry)
    {
        this.registry = registry;
    }

    /**
     * @param convertableClass
     *            the convertableClass to set
     */
    public final void setConvertableClass(final Class<?> convertableClass)
    {
        this.convertableClass = convertableClass;
    }

    protected void preRhinoBeanInvocation(final MethodInvocation invocation) throws Throwable
    {
        // NO-OP - for sub-classes to implement
    }

    protected Object convertToRhinoScriptable(final Object obj)
    {
        try
        {
            try (final InputStream is = NashornScriptProcessor.class.getResource("resources/nashorn-object-to-rhino.js").openStream())
            {
                try (final Reader isReader = new InputStreamReader(is))
                {
                    // this may be rather expensive per call, so use (potentially reuse) thead-local bindings
                    // TODO: Can we add some cleanup? What is the footprint of this thread-local?
                    final Bindings bindings = this.scriptEngine.createBindings();
                    bindings.put("nashornObj", obj);
                    final ScriptContext ctx = new SimpleScriptContext();
                    ctx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
                    this.scriptEngine.eval(isReader, ctx);
                    final Object result = bindings.get("rhinoObj");
                    return result;
                }
            }
        }
        catch (final IOException ioEx)
        {
            throw new ScriptException("Failed to convert object to Rhino scriptable", ioEx);
        }
        catch (final javax.script.ScriptException scriptEx)
        {
            throw new ScriptException("Failed to convert object to Rhino scriptable", scriptEx);
        }
    }

    @SuppressWarnings("rawtypes")
    protected static Class[] collectInterfaces(final Object source, final Collection<Class<?>> predefinedInterfaces)
    {
        final Set<Class<?>> interfaces = new HashSet<Class<?>>(predefinedInterfaces);
        Class<?> implClass = source.getClass();
        while (!Object.class.equals(implClass))
        {
            interfaces.addAll(Arrays.asList(implClass.getInterfaces()));

            implClass = implClass.getSuperclass();
        }

        final Class[] interfacesArr = interfaces.toArray(new Class[0]);
        return interfacesArr;
    }
}
