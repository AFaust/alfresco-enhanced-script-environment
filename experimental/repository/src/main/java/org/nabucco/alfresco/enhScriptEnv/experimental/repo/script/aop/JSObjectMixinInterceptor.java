package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.internal.runtime.ScriptRuntime;

import org.alfresco.processor.ProcessorExtension;
import org.alfresco.repo.jscript.Scopeable;
import org.alfresco.repo.processor.BaseProcessorExtension;
import org.alfresco.util.PropertyCheck;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.functions.JavaMethodJSObject;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.beans.factory.InitializingBean;

/**
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class JSObjectMixinInterceptor implements MethodInterceptor, InitializingBean
{

    private static Map<Class<?>, Map<String, Collection<Method>>> MEMBER_METHODS = new HashMap<Class<?>, Map<String, Collection<Method>>>();
    private static Map<Class<?>, Map<String, Method>> GETTER_METHODS = new HashMap<Class<?>, Map<String, Method>>();
    private static Map<Class<?>, Map<String, Collection<Method>>> SETTER_METHODS = new HashMap<Class<?>, Map<String, Collection<Method>>>();

    private static Map<Class<?>, Map<String, Field>> PUBLIC_FIELDS = new HashMap<Class<?>, Map<String, Field>>();

    private static Collection<Class<?>> CLASSES_TO_IGNORE = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays
            .asList(Scriptable.class, JSObject.class, AdapterObject.class, Scopeable.class, ProcessorExtension.class,
                    BaseProcessorExtension.class)));

    private static void initialize(final Class<?> cls)
    {
        if (!PUBLIC_FIELDS.containsKey(cls) || !SETTER_METHODS.containsKey(cls) || !GETTER_METHODS.containsKey(cls)
                || !MEMBER_METHODS.containsKey(cls))
        {
            final Map<String, Collection<Method>> memberMethods = new HashMap<String, Collection<Method>>();
            final Map<String, Method> getterMethods = new HashMap<String, Method>();
            final Map<String, Collection<Method>> setterMethods = new HashMap<String, Collection<Method>>();

            final Map<String, Field> publicFields = new HashMap<String, Field>();

            for (final Method method : cls.getMethods())
            {
                if (Modifier.isStatic(method.getModifiers()) || CLASSES_TO_IGNORE.contains(method.getDeclaringClass()))
                {
                    continue;
                }

                final String methodName = method.getName();

                // TODO How to handle presence of both getXY and isXY? Currently last one wins..

                if (methodName.startsWith("get") && methodName.length() > 3 && method.getParameterTypes().length == 0)
                {
                    final String propertyName = decapitalize(methodName.substring(3));
                    getterMethods.put(propertyName, method);
                }
                else if (methodName.startsWith("is") && methodName.length() > 2
                        && method.getParameterTypes().length == 0
                        && method.getReturnType() == boolean.class)
                {
                    final String propertyName = decapitalize(methodName.substring(2));
                    getterMethods.put(propertyName, method);
                }
                else if (methodName.startsWith("set") && methodName.length() > 3
                        && method.getParameterTypes().length == 1)
                {
                    final String propertyName = decapitalize(methodName.substring(3));
                    Collection<Method> setters = setterMethods.get(propertyName);
                    if (setters == null)
                    {
                        setters = new HashSet<Method>();
                        setterMethods.put(propertyName, setters);
                    }
                    setters.add(method);
                }

                Collection<Method> methods = memberMethods.get(methodName);
                if (methods == null)
                {
                    methods = new HashSet<Method>();
                    memberMethods.put(methodName, methods);
                }
                methods.add(method);
            }

            for (final Field field : cls.getFields())
            {
                if (Modifier.isStatic(field.getModifiers()) || CLASSES_TO_IGNORE.contains(field.getDeclaringClass()))
                {
                    continue;
                }

                publicFields.put(field.getName(), field);
            }

            MEMBER_METHODS.put(cls, memberMethods);
            SETTER_METHODS.put(cls, setterMethods);
            GETTER_METHODS.put(cls, getterMethods);

            PUBLIC_FIELDS.put(cls, publicFields);
        }
    }

    /* Copied from AbstractJavaLinker for consistency */
    private static String decapitalize(final String str)
    {
        assert str != null;
        if (str.isEmpty())
        {
            return str;
        }

        final char c0 = str.charAt(0);
        if (Character.isLowerCase(c0))
        {
            return str;
        }

        // If it has two consecutive upper-case characters, i.e. "URL", don't decapitalize
        if (str.length() > 1 && Character.isUpperCase(str.charAt(1)))
        {
            return str;
        }

        final char c[] = str.toCharArray();
        c[0] = Character.toLowerCase(c0);
        return new String(c);
    }

    protected final ValueConverter valueConverter;

    protected final Properties signatureMappings;

    public JSObjectMixinInterceptor(final ValueConverter valueConverter, final Properties signatureMappings)
    {
        this.valueConverter = valueConverter;
        this.signatureMappings = signatureMappings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "valueConverter", this.valueConverter);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable
    {
        final Method method = invocation.getMethod();
        final Object result;
        if (JSObject.class.equals(method.getDeclaringClass()))
        {
            final Object proxy;
            if (invocation instanceof ProxyMethodInvocation)
            {
                proxy = ((ProxyMethodInvocation) invocation).getProxy();
            }
            else
            {
                proxy = null;
            }

            final Object[] arguments = invocation.getArguments();
            final Object thisObj = invocation.getThis();
            final Class<? extends Object> thisClass = thisObj.getClass();

            switch (method.getName())
            {
            // no complex object supports Array-like access or use as function
                case "isArray":
                case "isStrictFunction":
                case "isFunction":
                case "isInstance":
                    result = Boolean.FALSE;
                    break;
                case "toNumber":
                    result = Double.valueOf(Double.NaN);
                    break;
                case "getClassName":
                    result = thisClass.getName();
                    break;
                case "isInstanceOf":
                    if (arguments.length > 0 && arguments[0] instanceof JSObject)
                    {
                        // actually unexpected - isInstance(JSObject) is checked and invoked (if supported) with high
                        // priority than isInstanceOf
                        result = Boolean.valueOf(((JSObject) arguments[0]).isInstance(proxy == null ? thisObj : proxy));
                    }
                    else
                    {
                        result = Boolean.FALSE;
                    }
                    break;
                case "getMember":
                    initialize(thisClass);
                    if (arguments.length == 1 && arguments[0] instanceof String)
                    {
                        result = this.getValue(thisObj, proxy, (String) arguments[0], thisClass);
                    }
                    else
                    {
                        result = ScriptRuntime.UNDEFINED;
                    }
                    break;
                case "hasMember":
                    initialize(thisClass);

                    if (arguments.length == 1 && arguments[0] instanceof String)
                    {
                        // only consider properties for this
                        final Set<String> readablePropertyNames = new HashSet<String>();
                        readablePropertyNames.addAll(PUBLIC_FIELDS.get(thisClass).keySet());
                        readablePropertyNames.addAll(GETTER_METHODS.get(thisClass).keySet());

                        result = Boolean.valueOf(readablePropertyNames.contains(arguments[0]));
                    }
                    else
                    {
                        result = Boolean.FALSE;
                    }
                    break;
                case "setMember":
                    initialize(thisClass);

                    if (arguments.length == 2 && arguments[0] instanceof String)
                    {
                        this.setValue(thisObj, proxy, (String) arguments[0], arguments[1], thisClass);
                    }
                    result = null;
                    break;
                case "keySet":
                    initialize(thisClass);

                    {
                        // only consider properties for this
                        final Set<String> readablePropertyNames = new HashSet<String>();
                        readablePropertyNames.addAll(PUBLIC_FIELDS.get(thisClass).keySet());
                        readablePropertyNames.addAll(GETTER_METHODS.get(thisClass).keySet());

                        result = readablePropertyNames;
                    }
                    break;
                case "values":
                    initialize(thisClass);

                    {
                        final Set<String> readablePropertyNames = new HashSet<String>();
                        readablePropertyNames.addAll(PUBLIC_FIELDS.get(thisClass).keySet());
                        readablePropertyNames.addAll(GETTER_METHODS.get(thisClass).keySet());

                        final Collection<Object> values = new ArrayList<Object>();
                        for (final String propertyName : readablePropertyNames)
                        {
                            values.add(this.getValue(thisObj, proxy, propertyName, thisClass));
                        }
                        result = values;
                    }

                    break;

                case "hasSlot":
                    // unsupported operation with no-op implementation
                    result = Boolean.FALSE;
                    break;
                case "setSlot":
                case "getSlot":
                case "removeMember":
                    // unsupported operation with no-op implementation
                    result = null;
                    break;
                default:
                    // really unsupported operations
                    throw new UnsupportedOperationException(method.getName());
            }
        }
        else
        {
            result = invocation.proceed();
        }
        return result;
    }

    protected void setValue(final Object thisObj, final Object proxyObj, final String fieldName, final Object value,
            final Class<?> thisClass) throws InvocationTargetException
    {
        final Map<String, Collection<Method>> setterByFieldName = SETTER_METHODS.get(thisClass);
        final Map<String, Field> fieldByFieldName = PUBLIC_FIELDS.get(thisClass);

        try
        {
            if (setterByFieldName.containsKey(fieldName))
            {
                final Collection<Method> setters = setterByFieldName.get(fieldName);

                Method bestMatchingSetter = null;
                if (setters.size() == 1)
                {
                    bestMatchingSetter = setters.iterator().next();
                }
                else
                {
                    // simple lookup while valueConverter does not support result type expectation
                    final Object javaValue = this.valueConverter.convertValueForJava(value);
                    final Class<?> javaValueClass = javaValue != null ? javaValue.getClass() : null;
                    Class<?> mostSpecificParameterType = null;
                    if (javaValueClass != null)
                    {
                        for (final Method method : setters)
                        {
                            final Class<?> parameterType = method.getParameterTypes()[0];
                            if (parameterType.isAssignableFrom(javaValueClass)
                                    && (mostSpecificParameterType == null || mostSpecificParameterType
                                            .isAssignableFrom(parameterType)))
                            {
                                mostSpecificParameterType = parameterType;
                                bestMatchingSetter = method;
                            }
                        }
                    }

                    // fallback - just use the first one
                    if (bestMatchingSetter == null)
                    {
                        bestMatchingSetter = setters.iterator().next();
                    }
                }

                final Class<?> parameterType = bestMatchingSetter.getParameterTypes()[0];
                final Object parameter = this.valueConverter.convertValueForJava(value, parameterType);

                if (proxyObj != null && bestMatchingSetter.getDeclaringClass().isInstance(proxyObj))
                {
                    bestMatchingSetter.invoke(proxyObj, parameter);
                }
                else
                {
                    bestMatchingSetter.invoke(thisObj, parameter);
                }

                // in case of public field set via setter, synchronize field value with proxy if proxy is compatible
                if (fieldByFieldName.containsKey(fieldName) && proxyObj != null)
                {
                    final Field field = fieldByFieldName.get(fieldName);
                    if (field.getDeclaringClass().isInstance(proxyObj))
                    {
                        final Object valueFromThis = field.get(thisObj);
                        field.set(proxyObj, valueFromThis);
                    }
                }
            }
            else if (fieldByFieldName.containsKey(fieldName))
            {
                final Field field = fieldByFieldName.get(fieldName);

                final Class<?> fieldType = field.getType();
                final Object parameter = this.valueConverter.convertValueForJava(value, fieldType);

                // thisObj is decisive
                field.set(thisObj, parameter);
                // if proxy is compatible, set public field too (consistency for Java access)
                if (proxyObj != null && field.getDeclaringClass().isInstance(proxyObj))
                {
                    field.set(proxyObj, parameter);
                }
            }
        }
        catch (final IllegalAccessException iaex)
        {
            // we don't expect this as we only call public getter / access public fields
            throw new InvocationTargetException(iaex);
        }
    }

    protected Object getValue(final Object thisObj, final Object proxyObj, final String fieldName,
            final Class<?> thisClass)
            throws InvocationTargetException
    {
        final Object value;

        final Map<String, Method> getterByFieldName = GETTER_METHODS.get(thisClass);
        final Map<String, Field> fieldByFieldName = PUBLIC_FIELDS.get(thisClass);
        final Map<String, Collection<Method>> memberByName = MEMBER_METHODS.get(thisClass);

        try
        {
            if (getterByFieldName.containsKey(fieldName))
            {
                final Method method = getterByFieldName.get(fieldName);
                final Object returnValue;

                if (proxyObj != null && method.getDeclaringClass().isInstance(proxyObj))
                {
                    returnValue = method.invoke(proxyObj, new Object[0]);
                }
                else
                {
                    returnValue = method.invoke(thisObj, new Object[0]);
                }

                value = this.valueConverter.convertValueForScript(returnValue);
            }
            else if (fieldByFieldName.containsKey(fieldName))
            {
                final Field field = fieldByFieldName.get(fieldName);
                final Object fieldValue = field.get(thisObj);
                value = this.valueConverter.convertValueForScript(fieldValue);
            }
            else if (memberByName.containsKey(fieldName))
            {
                final Collection<Method> overloadedMethods = memberByName.get(fieldName);
                value = new JavaMethodJSObject(fieldName, overloadedMethods, this.valueConverter,
                        this.signatureMappings);
            }
            else
            {
                value = ScriptRuntime.UNDEFINED;
            }
        }
        catch (final IllegalAccessException iaex)
        {
            // we don't expect this as we only call public getter / access public fields
            throw new InvocationTargetException(iaex);
        }

        return value;
    }

}
