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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;

import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.objects.NativeJava;

import org.alfresco.repo.jscript.NativeMap;
import org.alfresco.scripts.ScriptException;
import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdScriptableObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornValueInstanceConverterRegistry.ValueConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
// need to work with internal API to prepare the scope and convert objects
public class NashornValueConverter implements NashornValueInstanceConverterRegistry, ValueConverter,
        org.nabucco.alfresco.enhScriptEnv.common.script.ValueConverter, InitializingBean
{

    /**
     * {@link org.mozilla.javascript.NativeDate#getClassName}
     */
    private static final String TYPE_DATE = "Date";

    private static final String NASHORN_ENGINE_NAME = "nashorn";

    protected final Map<Class<?>, ValueInstanceConverter> valueInstanceConvertersByClass = new HashMap<Class<?>, ValueInstanceConverter>();

    protected ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(NASHORN_ENGINE_NAME);

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "scriptEngine", this.scriptEngine);
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
        final Object result;

        // check for Rhino integration data types
        if (value instanceof Scriptable)
        {
            final Scriptable scriptableValue = (Scriptable) value;
            if (value instanceof IdScriptableObject)
            {
                if (TYPE_DATE.equals(((IdScriptableObject) value).getClassName()))
                {
                    result = Context.jsToJava(value, Date.class);
                }
                else if (scriptableValue instanceof NativeArray)
                {
                    // convert Rhino JavaScript array of values to a regular array of objects
                    final Object[] propIds = scriptableValue.getIds();
                    if (isArray(propIds) == true)
                    {
                        result = this.convertRhinoArrayToNashorn(scriptableValue, propIds);
                    }
                    else
                    {
                        final Map<Object, Object> propValues = new HashMap<Object, Object>(propIds.length);
                        for (final Object propId : propIds)
                        {
                            // Get the value and add to the map
                            final Object val = scriptableValue.get(propId.toString(), scriptableValue);
                            propValues.put(this.convertValueForNashorn(propId), this.convertValueForNashorn(val));
                        }

                        result = propValues;
                    }
                }
                else
                {
                    result = this.convertObjectToNashornObject(scriptableValue);
                }
            }
            else if (scriptableValue instanceof NativeMap)
            {
                result = this.convertRhinoMapToMap(scriptableValue);
            }
            else if (scriptableValue instanceof NativeJavaObject)
            {
                result = ((NativeJavaObject) scriptableValue).unwrap();
            }
            else
            {
                result = this.convertObjectToNashornObject(scriptableValue);
            }
        }
        else if (value != null)
        {
            final Class<? extends Object> valueClass = value.getClass();
            if (valueClass.isArray())
            {
                if (valueClass.getComponentType().isPrimitive())
                {
                    result = NativeJava.from(null, value);
                }
                else
                {
                    final Object[] arr1 = (Object[]) value;
                    final Object[] arr2 = new Object[arr1.length];
                    for (int idx = 0; idx < arr1.length; idx++)
                    {
                        arr2[idx] = this.convertValueForNashorn(arr1[idx]);
                    }

                    result = NativeJava.from(null, arr2);
                }
            }
            else
            {
                result = this.convertObjectToNashornObject(value);
            }
        }
        else
        {
            result = value;
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
                        final Bindings bindings = this.scriptEngine.createBindings();
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

    protected Object convertObjectToNashornObject(final Object object)
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
            result = instanceConverter.convertValueForNashorn(object, this);
        }
        else
        {
            // no converter - use as is
            result = object;
        }

        return result;
    }

    protected Object convertRhinoMapToMap(final Scriptable scriptableValue)
    {
        final Object result;
        // convert Scriptable object of values to a Map of objects
        final Object[] propIds = scriptableValue.getIds();
        final Map<String, Object> propValues = new HashMap<String, Object>(propIds.length);
        for (int i = 0; i < propIds.length; i++)
        {
            // work on each key in turn
            final Object propId = propIds[i];

            // we are only interested in keys that indicate a list of values
            if (propId instanceof String)
            {
                // get the value out for the specified key
                final Object val = scriptableValue.get((String) propId, scriptableValue);
                // recursively call this method to convert the value
                propValues.put((String) propId, this.convertValueForNashorn(val));
            }
        }

        result = propValues;

        return result;
    }

    protected Object convertRhinoArrayToNashorn(final Scriptable scriptableValue, final Object[] propIds)
    {
        final Object result;
        final Object intermediateResult;

        // get type of array
        Class<?> componentType = null;

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
                // recursively call this method to convert the value
                propValues.add(this.convertValueForNashorn(val));

                if (componentType == null)
                {
                    componentType = val.getClass();
                }
                else if (!componentType.isInstance(val))
                {
                    Class<?> valClass = val.getClass();
                    while (!valClass.isAssignableFrom(componentType))
                    {
                        valClass = valClass.getSuperclass();
                    }
                    componentType = valClass;
                }
            }
        }

        if (propValues.isEmpty())
        {
            intermediateResult = new Object[0];
        }
        else
        {
            intermediateResult = propValues.toArray((Object[]) Array.newInstance(componentType, 0));
        }

        result = this.convertValueForNashorn(intermediateResult);

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
}
