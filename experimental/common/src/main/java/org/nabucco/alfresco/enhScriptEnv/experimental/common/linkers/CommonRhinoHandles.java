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
package org.nabucco.alfresco.enhScriptEnv.experimental.common.linkers;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import jdk.nashorn.internal.lookup.MethodHandleFactory;
import jdk.nashorn.internal.lookup.MethodHandleFunctionality;
import jdk.nashorn.internal.objects.NativeJava;
import jdk.nashorn.internal.runtime.ScriptRuntime;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public final class CommonRhinoHandles
{

    private static final MethodHandleFunctionality MH = MethodHandleFactory.getFunctionality();

    private static final MethodHandle RHINO_TO_NASHORN_ARRAY_FILTER = findOwnMH_S("rhinoToNashornArrayFilter", Object.class, Object.class);

    private static final MethodHandle NASHORN_TO_RHINO_ARRAY_FILTER = findOwnMH_S("nashornToRhinoArrayFilter", Object.class, Object.class);

    private static final MethodHandle NASHORN_TO_RHINO_ARRAY_FILTER_COLLATED = findOwnMH_S("nashornToRhinoArrayFilterCollated",
            Object[].class, Object[].class);

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
    }

    public static MethodHandle applyCommonRhinoHandles(final MethodHandle mh, final int argPos, final boolean collatedArgs)
    {
        MethodHandle result;

        if (collatedArgs)
        {
            result = MH.filterArguments(mh, argPos, NASHORN_TO_RHINO_ARRAY_FILTER_COLLATED);
        }
        else
        {
            result = MH.filterArguments(mh, argPos, NASHORN_TO_RHINO_ARRAY_FILTER);
        }
        result = MH.filterReturnValue(result, RHINO_TO_NASHORN_ARRAY_FILTER);

        return result;
    }

    private static MethodHandle findOwnMH_S(final String name, final Class<?> rtype, final Class<?>... types)
    {
        return MH.findStatic(MethodHandles.lookup(), CommonRhinoHandles.class, name, MH.type(rtype, types));
    }

    @SuppressWarnings("unused")
    private static Object[] nashornToRhinoArrayFilterCollated(final Object[] arg)
    {
        final Object[] result;
        if (arg != null)
        {
            result = new Object[arg.length];
            for (int idx = 0; idx < arg.length; idx++)
            {
                result[idx] = nashornToRhinoArrayFilter(arg[idx]);
            }
        }else{
            result = arg;
        }

        return result;
    }

    @SuppressWarnings("unused")
    private static Object nashornToRhinoArrayFilter(final Object arg)
    {
        final Object result;

        if (arg instanceof jdk.nashorn.internal.objects.NativeArray)
        {
            try
            {
                final Object[] array = (Object[]) NativeJava.to(null, arg, ScriptRuntime.UNDEFINED);
                Context.enter();
                try
                {
                    result = Context.getCurrentContext().newArray(DUMMY_SCOPE, array);
                }
                finally
                {
                    Context.exit();
                }
            }
            catch (final ClassNotFoundException cnfe)
            {
                // guaranteed to not occur - UNDEFINED triggers 'default' conversion which is hard coded and does not look up any class
                throw new RuntimeException(cnfe);
            }
        }
        else
        {
            result = arg;
        }

        return result;
    }

    @SuppressWarnings("unused")
    private static Object rhinoToNashornArrayFilter(final Object arg)
    {
        final Object result;

        if (arg instanceof org.mozilla.javascript.NativeArray)
        {
            final org.mozilla.javascript.NativeArray array = (org.mozilla.javascript.NativeArray) arg;

            // only Rhino 1.7 provides a simple toArray() method
            if (array.getLength() == 0)
            {
                result = NativeJava.from(null, Collections.<Object> emptyList());
            }
            else
            {
                final Collection<Object> list = new ArrayList<>();
                Context.enter();
                try
                {
                    for (long idx = 0, max = array.getLength(); idx < max; idx++)
                    {
                        final Object element;
                        if (idx < Integer.MAX_VALUE)
                        {
                            element = ScriptableObject.getProperty(array, (int) idx);
                        }
                        else
                        {
                            element = ScriptableObject.getProperty(array, String.valueOf(idx));
                        }

                        if (element != null && element != Undefined.instance && element != Scriptable.NOT_FOUND)
                        {
                            list.add(element);
                        }
                    }
                }
                finally
                {
                    Context.exit();
                }

                result = NativeJava.from(null, list);
            }
        }
        else
        {
            result = arg;
        }

        return result;
    }

    private CommonRhinoHandles()
    {
        // NO-OP - just uninstantiable
    }
}
