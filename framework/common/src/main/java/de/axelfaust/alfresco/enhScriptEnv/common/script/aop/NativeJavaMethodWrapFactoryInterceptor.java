/*
 * Copyright 2015 Axel Faust
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
package de.axelfaust.alfresco.enhScriptEnv.common.script.aop;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrapFactory;

import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.rhino.DelegatingWrapFactory;

/**
 * @author Axel Faust
 */
public class NativeJavaMethodWrapFactoryInterceptor implements MethodInterceptor
{

    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE;

    private static final Scriptable DUMMY_SCOPE;
    static
    {
        final Context cx = Context.enter();
        try
        {
            DUMMY_SCOPE = cx.initStandardObjects(null, true);
            DUMMY_SCOPE.delete("Packages");
            DUMMY_SCOPE.delete("getClass");
            DUMMY_SCOPE.delete("java");
            ((ScriptableObject) DUMMY_SCOPE).sealObject();
        }
        finally
        {
            Context.exit();
        }

        // core classes are never redefined, so safe to reference in static map
        final Map<Class<?>, Class<?>> wrapperToPrimitve = new IdentityHashMap<Class<?>, Class<?>>();
        wrapperToPrimitve.put(Boolean.class, Boolean.TYPE);
        wrapperToPrimitve.put(Short.class, Short.TYPE);
        wrapperToPrimitve.put(Integer.class, Integer.TYPE);
        wrapperToPrimitve.put(Long.class, Long.TYPE);
        wrapperToPrimitve.put(Float.class, Float.TYPE);
        wrapperToPrimitve.put(Double.class, Double.TYPE);
        wrapperToPrimitve.put(Character.class, Character.TYPE);
        wrapperToPrimitve.put(Byte.class, Byte.TYPE);
        WRAPPER_TO_PRIMITIVE = Collections.unmodifiableMap(wrapperToPrimitve);
    }

    private static final NativeJavaMethodWrapFactoryInterceptor INSTANCE = new NativeJavaMethodWrapFactoryInterceptor();

    public static NativeJavaMethodWrapFactoryInterceptor getInstance()
    {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable
    {
        final Object[] arguments = invocation.getArguments();
        Scriptable scope = null;
        for (int idx = 0; idx < arguments.length && scope == null; idx++)
        {
            if (arguments[idx] instanceof Scriptable)
            {
                scope = (Scriptable) arguments[idx];
            }
        }

        final Context cx = Context.getCurrentContext();
        final WrapFactory wrapFactory = cx.getWrapFactory();

        // disabling before and enabling after invocation with manual wrapping "seems" pointless
        // this pattern is actually not meant for "this" invocation, but for any calls from Java code to any native script object to avoid
        // polluting Java scope with special AOP-enhanced script objects that Java code may not be able to handle
        if (wrapFactory instanceof DelegatingWrapFactory)
        {
            ((DelegatingWrapFactory) wrapFactory).disableWrap();
        }

        Object result;
        try
        {
            result = invocation.proceed();
        }
        finally
        {
            if (wrapFactory instanceof DelegatingWrapFactory)
            {
                ((DelegatingWrapFactory) wrapFactory).enableWrap();
            }
        }

        if (result != null && Undefined.instance != result && Scriptable.NOT_FOUND != result)
        {
            result = Context.jsToJava(result, Object.class);

            // even if signature may not use primitve, we prefer them (this also avoids wrapping them)
            Class<?> staticType = result.getClass();
            if (WRAPPER_TO_PRIMITIVE.containsKey(staticType))
            {
                staticType = WRAPPER_TO_PRIMITIVE.get(staticType);
            }

            result = wrapFactory.wrap(cx, scope != null ? scope : DUMMY_SCOPE, result, staticType);
        }
        return result;
    }

}
