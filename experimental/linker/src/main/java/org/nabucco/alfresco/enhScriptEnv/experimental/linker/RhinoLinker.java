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
package org.nabucco.alfresco.enhScriptEnv.experimental.linker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

import jdk.internal.dynalink.CallSiteDescriptor;
import jdk.internal.dynalink.linker.GuardedInvocation;
import jdk.internal.dynalink.linker.GuardedTypeConversion;
import jdk.internal.dynalink.linker.GuardingTypeConverterFactory;
import jdk.internal.dynalink.linker.LinkRequest;
import jdk.internal.dynalink.linker.LinkerServices;
import jdk.internal.dynalink.linker.TypeBasedGuardingDynamicLinker;
import jdk.internal.dynalink.support.CallSiteDescriptorFactory;
import jdk.nashorn.internal.lookup.MethodHandleFactory;
import jdk.nashorn.internal.lookup.MethodHandleFunctionality;
import jdk.nashorn.internal.runtime.JSType;
import jdk.nashorn.internal.runtime.Undefined;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.UniqueTag;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class RhinoLinker implements TypeBasedGuardingDynamicLinker, GuardingTypeConverterFactory
{

    private static final MethodHandleFunctionality MH = MethodHandleFactory.getFunctionality();

    // method handles of the current class
    private static final MethodHandle IS_SCRIPTABLE_GUARD = findOwnMH_S("isScriptableObject", boolean.class, Object.class);
    private static final MethodHandle RHINO_LINKER_GET = findOwnMH_S("get", Object.class, Scriptable.class, Object.class);
    private static final MethodHandle RHINO_LINKER_PUT = findOwnMH_S("put", Void.TYPE, Scriptable.class, Object.class, Object.class);

    private static final Map<Class<?>, MethodHandle> CONVERTERS = new HashMap<>();
    static
    {
        CONVERTERS.put(boolean.class, findOwnMH_S("toBoolean", boolean.class, Scriptable.class));
        CONVERTERS.put(int.class, findOwnMH_S("toInt32", int.class, Scriptable.class));
        CONVERTERS.put(long.class, findOwnMH_S("toLong", long.class, Scriptable.class));
        CONVERTERS.put(double.class, findOwnMH_S("toNumber", double.class, Scriptable.class));
    }

    public RhinoLinker()
    {
        // NO-OP
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public GuardedInvocation getGuardedInvocation(final LinkRequest linkRequest, final LinkerServices linkerServices) throws Exception
    {
        final LinkRequest requestWithoutContext = linkRequest.withoutRuntimeContext(); // Nashorn has no runtime context
        final Object self = requestWithoutContext.getReceiver();
        final CallSiteDescriptor desc = requestWithoutContext.getCallSiteDescriptor();

        if (desc.getNameTokenCount() < 2 || !"dyn".equals(desc.getNameToken(CallSiteDescriptor.SCHEME)))
        {
            // We only support standard "dyn:*[:*]" operations
            return null;
        }

        GuardedInvocation inv;
        if (self instanceof Scriptable)
        {
            inv = this.lookup(desc, linkRequest, linkerServices);
        }
        else
        {
            throw new AssertionError(); // Should never reach here.
        }

        if (inv != null)
        {
            inv = inv.asTypeSafeReturn(linkerServices, desc.getMethodType());
        }
        return inv;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public GuardedTypeConversion convertToType(final Class<?> sourceType, final Class<?> targetType) throws Exception
    {
        final boolean sourceIsAlwaysScriptable = Scriptable.class.isAssignableFrom(sourceType);
        if (!sourceIsAlwaysScriptable && !sourceType.isAssignableFrom(Scriptable.class))
        {
            return null;
        }

        final MethodHandle converter = CONVERTERS.get(targetType);
        if (converter == null)
        {
            return null;
        }

        return new GuardedTypeConversion(
                new GuardedInvocation(converter, sourceIsAlwaysScriptable ? null : IS_SCRIPTABLE_GUARD).asType(MethodType.methodType(
                        targetType, sourceType)), true);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canLinkType(final Class<?> type)
    {
        final boolean canLink = Scriptable.class.isAssignableFrom(type);
        return canLink;
    }

    protected GuardedInvocation lookup(final CallSiteDescriptor desc, final LinkRequest request, final LinkerServices linkerServices)
            throws Exception
    {
        final String operator = CallSiteDescriptorFactory.tokenizeOperators(desc).get(0);
        final int c = desc.getNameTokenCount();

        switch (operator)
        {
        case "getProp":
        case "getElem":
        case "getMethod":
            if (c > 2)
            {
                return findGetMethod(desc);
            }

            return new GuardedInvocation(RHINO_LINKER_GET, IS_SCRIPTABLE_GUARD);
        case "setProp":
        case "setElem":
            return c > 2 ? findSetMethod(desc) : new GuardedInvocation(RHINO_LINKER_PUT, IS_SCRIPTABLE_GUARD);
        case "call":
            // TODO map to Function#call - should we facade current scope as "scope"-parameter?
            return null;
        case "new":
            // TODO map to Function#construct - should we facade current scope as "scope"-parameter?
            return null;
        default:
            return null;
        }
    }

    protected static GuardedInvocation findGetMethod(final CallSiteDescriptor desc)
    {
        final String name = desc.getNameToken(CallSiteDescriptor.NAME_OPERAND);
        final MethodHandle getter = MH.insertArguments(RHINO_LINKER_GET, 1, name);
        return new GuardedInvocation(getter, IS_SCRIPTABLE_GUARD);
    }

    protected static GuardedInvocation findSetMethod(final CallSiteDescriptor desc)
    {
        final String name = desc.getNameToken(CallSiteDescriptor.NAME_OPERAND);
        final MethodHandle getter = MH.insertArguments(RHINO_LINKER_PUT, 1, name);
        return new GuardedInvocation(getter, IS_SCRIPTABLE_GUARD);
    }

    private static MethodHandle findScriptableMH_V(final String name, final Class<?> rtype, final Class<?>... types)
    {
        return MH.findVirtual(MethodHandles.lookup(), Scriptable.class, name, MH.type(rtype, types));
    }

    private static MethodHandle findOwnMH_S(final String name, final Class<?> rtype, final Class<?>... types)
    {
        return MH.findStatic(MethodHandles.lookup(), RhinoLinker.class, name, MH.type(rtype, types));
    }

    @SuppressWarnings("unused")
    private static boolean isScriptableObject(final Object self)
    {
        return self instanceof Scriptable;
    }

    @SuppressWarnings("unused")
    private static Object get(final Scriptable scriptable, final Object key)
    {
        Context.enter();
        try
        {
            Object rvalue = null;
            if (key instanceof Integer)
            {
                rvalue = scriptable.get(((Integer) key).intValue(), scriptable);
            }
            else if (key instanceof Number)
            {
                final double value = ((Number) key).doubleValue();
                final int index = JSType.isRepresentableAsInt(value) ? (int) value : -1;
                if (index > -1)
                {
                    rvalue = scriptable.get(index, scriptable);
                }
            }
            else if (key instanceof String)
            {
                final String name = (String) key;
                rvalue = scriptable.get(name, scriptable);
            }

            if (Scriptable.NOT_FOUND.equals(rvalue) || UniqueTag.NOT_FOUND.equals(rvalue))
            {
                rvalue = Undefined.getUndefined();
            }
            else if (UniqueTag.NULL_VALUE.equals(rvalue))
            {
                rvalue = null;
            }

            return rvalue;
        }
        finally
        {
            Context.exit();
        }
    }

    @SuppressWarnings("unused")
    private static void put(final Scriptable scriptable, final Object key, final Object value)
    {
        Context.enter();
        try
        {
            if (key instanceof Integer)
            {
                scriptable.put(((Integer) key).intValue(), scriptable, value);
            }
            else if (key instanceof Number)
            {
                final double idxValue = ((Number) key).doubleValue();
                final int index = JSType.isRepresentableAsInt(idxValue) ? (int) idxValue : -1;
                scriptable.put(index, scriptable, value);
            }
            else if (key instanceof String)
            {
                scriptable.put((String) key, scriptable, value);
            }
        }
        finally
        {
            Context.exit();
        }
    }

    @SuppressWarnings("unused")
    private static int toInt32(final Scriptable obj)
    {
        return JSType.toInt32(toNumber(obj));
    }

    @SuppressWarnings("unused")
    private static long toLong(final Scriptable obj)
    {
        return JSType.toLong(toNumber(obj));
    }

    private static double toNumber(final Scriptable obj)
    {
        if (obj != null)
        {
            final Object defaultValue = obj.getDefaultValue(Number.class);
            if (defaultValue instanceof Number)
            {
                return ((Number) defaultValue).doubleValue();
            }
        }
        return 0;
    }

    @SuppressWarnings("unused")
    private static boolean toBoolean(final Scriptable obj)
    {
        return obj != null;
    }
}
