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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;

import jdk.nashorn.internal.runtime.ScriptObject;

import org.alfresco.repo.jscript.ContentAwareScriptableQNameMap;
import org.alfresco.repo.jscript.NativeMap;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptableHashMap;
import org.alfresco.scripts.ScriptException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.namespace.NamespacePrefixResolverProvider;
import org.alfresco.service.namespace.QNameMap;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.ListLikeMapAdapterInterceptor;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.ScriptableAdapterInterceptor;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.ValueConvertingListInterceptor;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.ValueConvertingMapInterceptor;
import org.springframework.aop.framework.ConstructorArgumentAwareProxyFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.webscripts.ScriptableMap;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
// need to work with internal API to prepare the scope and convert objects
public class NashornValueConverter implements NashornValueInstanceConverterRegistry, ValueConverter, InitializingBean
{

    /**
     * {@link org.mozilla.javascript.NativeDate#getClassName}
     */
    private static final String TYPE_DATE = "Date";

    private static final String NASHORN_ENGINE_NAME = "nashorn";

    protected final Map<Class<?>, ValueInstanceConverter> valueInstanceConvertersByClass = new HashMap<Class<?>, ValueInstanceConverter>();

    protected ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(NASHORN_ENGINE_NAME);

    protected ServiceRegistry serviceRegistry;

    protected final ThreadLocal<Bindings> cachedBindings = new ThreadLocal<Bindings>()
    {

        /**
         * {@inheritDoc}
         */
        @Override
        protected Bindings initialValue()
        {
            return NashornValueConverter.this.scriptEngine.createBindings();
        }

    };

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "scriptEngine", this.scriptEngine);
        PropertyCheck.mandatory(this, "serviceRegistry", this.serviceRegistry);
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
     * @param serviceRegistry
     *            the serviceRegistry to set
     */
    public final void setServiceRegistry(final ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerValueInstanceConverter(final Class<?> instanceClass, final ValueInstanceConverter converter)
    {
        ParameterCheck.mandatory("instanceClass", instanceClass);
        ParameterCheck.mandatory("converter", converter);

        this.valueInstanceConvertersByClass.put(instanceClass, converter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForNashorn(final Object value)
    {
        final Object result = this.convertValueForNashorn(value, Object.class);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T convertValueForNashorn(final Object value, final Class<T> expectedClass)
    {
        final T result;

        // check for Rhino integration data types
        if (value instanceof Scriptable)
        {
            final Scriptable scriptableValue = (Scriptable) value;

            if (Map.class.isAssignableFrom(expectedClass))
            {
                // some kind of map required
                result = expectedClass.cast(this.convertScriptableToMap(scriptableValue, expectedClass));
            }
            else if (Scriptable.class.isAssignableFrom(expectedClass) && scriptableValue instanceof Map<?, ?>)
            {
                // already a Map AND a Scriptable, may be a special kind of Rhino-enabled Map
                result = expectedClass.cast(this.convertScriptableToMap(scriptableValue, expectedClass));
            }
            else if (scriptableValue instanceof NativeJavaObject)
            {
                final Object javaObject = ((NativeJavaObject) scriptableValue).unwrap();
                result = expectedClass.cast(this.convertValueForNashorn(javaObject, expectedClass));
            }
            else if (expectedClass.isAssignableFrom(Date.class) && value instanceof ScriptableObject
                    && TYPE_DATE.equals(((ScriptableObject) value).getClassName()))
            {
                result = expectedClass.cast(Context.jsToJava(value, Date.class));
            }
            else if (scriptableValue instanceof NativeArray)
            {
                // convert Rhino JavaScript array of values to a regular array of objects
                final Object[] propIds = scriptableValue.getIds();
                if (isArray(propIds) == true)
                {
                    final List<Object> propValues = new ArrayList<Object>(propIds.length);
                    for (int i = 0; i < propIds.length; i++)
                    {
                        // work on each key in turn
                        final Object propId = propIds[i];

                        // we are only interested in keys that indicate a list of values
                        if (propId instanceof Integer)
                        {
                            // get the value out for the specified key
                            final Object val = scriptableValue.get(((Integer) propId).intValue(), scriptableValue);
                            propValues.add(val);
                        }
                    }

                    result = this.convertValueForNashorn(propValues, expectedClass);
                }
                else
                {
                    final Map<Object, Object> propValues = new HashMap<Object, Object>(propIds.length);
                    for (final Object propId : propIds)
                    {
                        final Object val = scriptableValue.get(propId.toString(), scriptableValue);
                        propValues.put(propId, val);
                    }

                    result = expectedClass.cast(this.convertValueForNashorn(propValues, expectedClass));
                }
            }
            else
            {
                result = expectedClass.cast(this.converValueForNashornImpl(scriptableValue, expectedClass));
            }
        }
        // special map #2 requires a sub-classing proxy, but doesn't require List-interface add-in
        else if (QNameMap.class.isAssignableFrom(expectedClass) && value instanceof QNameMap<?, ?>)
        {

            result = expectedClass.cast(this.convertQNameMapToMap((QNameMap<?, ?>) value, expectedClass));
        }
        else if (Map.class.equals(expectedClass) && value instanceof Map<?, ?>)
        {
            final ProxyFactory proxyFactory = new ProxyFactory();

            proxyFactory.addAdvice(new ValueConvertingMapInterceptor(this));

            proxyFactory.setInterfaces(collectInterfaces(value, Collections.<Class<?>> emptySet()));
            proxyFactory.setTarget(value);

            result = expectedClass.cast(proxyFactory.getProxy());

        }
        else if (List.class.equals(expectedClass) && value instanceof List<?>)
        {
            final ProxyFactory proxyFactory = new ProxyFactory();

            proxyFactory.addAdvice(new ValueConvertingListInterceptor(this));

            proxyFactory.setInterfaces(collectInterfaces(value, Collections.<Class<?>> emptySet()));
            proxyFactory.setTarget(value);

            result = expectedClass.cast(proxyFactory.getProxy());

        }
        else if (value != null)
        {
            final Class<? extends Object> valueClass = value.getClass();
            if ((expectedClass.isArray() && (valueClass.isArray() || value instanceof List<?>))
                    || (List.class.equals(expectedClass) && valueClass.isArray()))
            {
                if (expectedClass.isArray() && valueClass.isArray())
                {
                    final Object[] arr = (Object[]) value;

                    for (int idx = 0; idx < arr.length; idx++)
                    {
                        arr[idx] = this.convertValueForNashorn(arr[idx], expectedClass.getComponentType());
                    }
                    result = expectedClass.cast(arr);
                }
                else if (expectedClass.isArray() && value instanceof List<?>)
                {
                    final List<?> list = (List<?>) value;
                    final Object[] arr = (Object[]) Array.newInstance(expectedClass.getComponentType(), list.size());
                    for (int idx = 0; idx < list.size(); idx++)
                    {
                        arr[idx] = this.convertValueForNashorn(list.get(idx), expectedClass.getComponentType());
                    }
                    result = expectedClass.cast(arr);
                }
                else
                {
                    final Object[] arr = (Object[]) value;
                    final List<Object> list = new ArrayList<Object>();
                    for (final Object element : arr)
                    {
                        list.add(element);
                    }
                    result = expectedClass.cast(this.convertValueForNashorn(list, expectedClass));
                }
            }
            else if (!expectedClass.isPrimitive())
            {
                // arbitrary object - no special handling (yet)
                result = expectedClass.cast(this.converValueForNashornImpl(value, expectedClass));
            }
            else
            {
                @SuppressWarnings("unchecked")
                final T primitiveResult = (T) value;
                result = primitiveResult;
            }
        }
        else
        {
            result = null;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value)
    {
        final Object result;

        if (value instanceof ScriptObject)
        {
            try
            {
                try (final InputStream is = NashornScriptProcessor.class.getResource("resources/nashorn-object-to-java.js").openStream())
                {
                    try (final Reader isReader = new InputStreamReader(is))
                    {
                        // this may be rather expensive per call, so use (potentially reuse) thead-local bindings
                        // TODO: Can we add some cleanup? What is the footprint of this thread-local?
                        final Bindings bindings = this.cachedBindings.get();
                        bindings.put("nashornObj", value);
                        final ScriptContext ctx = new SimpleScriptContext();
                        ctx.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
                        this.scriptEngine.eval(isReader, ctx);
                        result = bindings.get("javaObj");
                    }
                }
            }
            catch (final IOException ioEx)
            {
                throw new ScriptException("Failed to convert object to Java", ioEx);
            }
            catch (final javax.script.ScriptException scriptEx)
            {
                throw new ScriptException("Failed to convert object to Java", scriptEx);
            }
        }
        else
        {
            result = value;
        }

        return result;
    }

    protected Object converValueForNashornImpl(final Object object, final Class<?> expectedClass)
    {
        ValueInstanceConverter instanceConverter = null;
        Class<?> cls = object.getClass();
        while (instanceConverter == null && !Object.class.equals(cls))
        {
            instanceConverter = this.valueInstanceConvertersByClass.get(cls);
            cls = cls.getSuperclass();
        }

        final Object result;
        if (instanceConverter != null)
        {
            result = instanceConverter.convertValueForNashorn(object, this, expectedClass);
        }
        else
        {
            // no converter - use as is
            result = object;
        }

        return result;
    }

    protected Object convertScriptableToMap(final Scriptable scriptableValue, final Class<?> expectedClass)
    {
        final Object result;

        // special map #1 requires a sub-classing proxy where we add-in List-interface to support Array-like access
        // (like the map supports in Rhino)
        if (ScriptableHashMap.class.isAssignableFrom(expectedClass) && scriptableValue instanceof ScriptableHashMap<?, ?>)
        {
            // note: fortunately, this does not cover the final class ScriptableParameterMap that is "only" returned as Map

            // we know ScriptableHashMap has a no-arg constructor
            final ProxyFactory proxyFactory = new ConstructorArgumentAwareProxyFactory(new Object[0], new Class[0]);

            proxyFactory.addAdvice(new ValueConvertingMapInterceptor(this));
            proxyFactory.addAdvice(new ValueConvertingListInterceptor(this));
            proxyFactory.setInterfaces(collectInterfaces(scriptableValue, Collections.<Class<?>> singleton(List.class)));
            proxyFactory.setTarget(scriptableValue);
            proxyFactory.setProxyTargetClass(true);

            result = proxyFactory.getProxy();
        }
        // special map #2 requires a sub-classing proxy, but doesn't require List-interface add-in
        else if (QNameMap.class.isAssignableFrom(expectedClass) && scriptableValue instanceof QNameMap<?, ?>)
        {

            result = this.convertQNameMapToMap((QNameMap<?, ?>) scriptableValue, expectedClass);
        }
        // special map #3 that we typically can just facade with List-interface add-in to support Array-like access and Scriptable
        else if (ScriptableMap.class.equals(expectedClass) && scriptableValue instanceof ScriptableMap)
        {
            final ProxyFactory proxyFactory = new ProxyFactory();

            proxyFactory.addAdvice(new ScriptableAdapterInterceptor());
            proxyFactory.addAdvice(new ValueConvertingMapInterceptor(this));
            proxyFactory.addAdvice(new ValueConvertingListInterceptor(this));
            proxyFactory.addAdvice(new ListLikeMapAdapterInterceptor());

            proxyFactory.setInterfaces(collectInterfaces(scriptableValue, Collections.<Class<?>> emptySet()));
            proxyFactory.setTarget(scriptableValue);

            result = proxyFactory.getProxy();
        }
        // special map #4 that we typically can just facade with List-interface add-in to support Array-like access without Scriptable
        else if (Map.class.equals(expectedClass) && scriptableValue instanceof LinkedHashMap<?, ?>)
        {
            final ProxyFactory proxyFactory = new ProxyFactory();

            proxyFactory.addAdvice(new ValueConvertingMapInterceptor(this));
            proxyFactory.addAdvice(new ValueConvertingListInterceptor(this));
            proxyFactory.addAdvice(new ListLikeMapAdapterInterceptor());

            proxyFactory.setInterfaces(collectInterfaces(scriptableValue, Collections.<Class<?>> emptySet()));
            // not expected => not exposed
            proxyFactory.removeInterface(Scriptable.class);
            proxyFactory.setTarget(scriptableValue);

            result = proxyFactory.getProxy();

        }
        else if (scriptableValue instanceof NativeMap)
        {
            // default NativeMap handling
            final ProxyFactory proxyFactory = new ProxyFactory();

            final Object wrapped = ((NativeMap) scriptableValue).unwrap();

            proxyFactory.setInterfaces(collectInterfaces(wrapped, Collections.<Class<?>> emptySet()));
            if (Scriptable.class.equals(expectedClass))
            {
                // expected => delegated to Map (if somehow used at all)
                proxyFactory.addAdvice(new ScriptableAdapterInterceptor());
                proxyFactory.addInterface(Scriptable.class);
            }
            proxyFactory.addAdvice(new ValueConvertingMapInterceptor(this));
            proxyFactory.setTarget(wrapped);

            result = proxyFactory.getProxy();
        }
        else if (scriptableValue instanceof Map<?, ?>)
        {
            // default Map handling
            final ProxyFactory proxyFactory = new ProxyFactory();

            proxyFactory.setInterfaces(collectInterfaces(scriptableValue, Collections.<Class<?>> emptySet()));
            if (Scriptable.class.equals(expectedClass))
            {
                // expected => delegated to Map (if somehow used at all)
                proxyFactory.addAdvice(new ScriptableAdapterInterceptor());
            }
            else if (!Scriptable.class.isAssignableFrom(expectedClass))
            {
                // not expected => not exposed
                proxyFactory.removeInterface(Scriptable.class);
            }

            proxyFactory.addAdvice(new ValueConvertingMapInterceptor(this));
            proxyFactory.setTarget(scriptableValue);

            result = proxyFactory.getProxy();
        }
        else
        {
            // default Scriptable handling

            // convert Scriptable object of values to a Map of objects
            final Object[] propIds = scriptableValue.getIds();
            final Map<String, Object> propValues = new HashMap<String, Object>(propIds.length);
            for (int i = 0; i < propIds.length; i++)
            {
                // work on each key in turn
                final Object propId = propIds[i];

                // get the value out for the specified key
                final Object val = propId instanceof String ? scriptableValue.get((String) propId, scriptableValue) : scriptableValue.get(
                        ((Integer) propId).intValue(), scriptableValue);
                propValues.put((String) propId, val);
            }

            final ProxyFactory proxyFactory = new ProxyFactory();
            proxyFactory.setInterfaces(collectInterfaces(propValues, Collections.<Class<?>> emptySet()));
            if (Scriptable.class.equals(expectedClass))
            {
                // expected => delegated to Map (if somehow used at all)
                proxyFactory.addAdvice(new ScriptableAdapterInterceptor());
                proxyFactory.addInterface(Scriptable.class);
            }
            proxyFactory.addAdvice(new ValueConvertingMapInterceptor(this));
            proxyFactory.setTarget(propValues);

            result = proxyFactory.getProxy();
        }

        return result;
    }

    protected Object convertQNameMapToMap(final QNameMap<?, ?> scriptableValue, final Class<?> expectedClass)
    {
        final Object result;
        final Object[] cstructParams;
        final Class[] cstructParamTypes;

        if (scriptableValue instanceof ContentAwareScriptableQNameMap<?, ?>)
        {
            try
            {
                final Field factoryField = QNameMap.class.getField("factory");
                factoryField.setAccessible(true);
                final Object factory = factoryField.get(scriptableValue);
                cstructParams = new Object[] { factory, this.serviceRegistry };
                cstructParamTypes = new Class[] { ScriptNode.class, ServiceRegistry.class };
            }
            catch (final NoSuchFieldException | IllegalAccessException ex)
            {
                throw new ScriptException("Technical error preparing conversion of QNameMap for Nashorn", ex);
            }
        }
        else
        {
            try
            {
                final Field providerField = QNameMap.class.getField("provider");
                providerField.setAccessible(true);
                final Object namespacePrefixResolverProvider = providerField.get(scriptableValue);
                cstructParams = new Object[] { namespacePrefixResolverProvider };
                cstructParamTypes = new Class[] { NamespacePrefixResolverProvider.class };
            }
            catch (final NoSuchFieldException | IllegalAccessException ex)
            {
                throw new ScriptException("Technical error preparing conversion of QNameMap for Nashorn", ex);
            }
        }

        final ProxyFactory proxyFactory = new ConstructorArgumentAwareProxyFactory(cstructParams, cstructParamTypes);

        proxyFactory.setInterfaces(collectInterfaces(scriptableValue, Collections.<Class<?>> emptySet()));
        if (Scriptable.class.isAssignableFrom(expectedClass))
        {
            // expected => delegated to Map (if somehow used at all)
            proxyFactory.addAdvice(new ScriptableAdapterInterceptor());
        }
        else
        {
            // not expected => not exposed
            proxyFactory.removeInterface(Scriptable.class);
        }
        proxyFactory.addAdvice(new ValueConvertingMapInterceptor(this));
        proxyFactory.setTarget(scriptableValue);
        proxyFactory.setProxyTargetClass(true);

        result = proxyFactory.getProxy();
        return result;
    }

    /**
     * Look at the id's of a native array and try to determine whether it's actually an Array or a Hashmap
     *
     * @param ids
     *            id's of the native array
     * @return boolean true if it's an array, false otherwise (ie it's a map)
     */
    protected static boolean isArray(final Object[] ids)
    {
        boolean result = true;
        for (final Object id : ids)
        {
            if (!(id instanceof Integer))
            {
                result = false;
                break;
            }
        }
        return result;
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
