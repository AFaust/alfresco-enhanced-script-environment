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
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import javax.script.ScriptContext;

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
import jdk.nashorn.internal.objects.Global;
import jdk.nashorn.internal.runtime.JSType;
import jdk.nashorn.internal.runtime.Undefined;
import jdk.nashorn.internal.runtime.linker.NashornCallSiteDescriptor;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
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

    private static final MethodHandle IS_FUNCTION_GUARD = findOwnMH_S("isFunctionObject", boolean.class, Object.class);
    private static final MethodHandle RHINO_LINKER_CALL_TO_APPLY = findOwnMH_S("callToApply", Object.class, MethodHandle.class,
            Function.class, Object[].class);
    private static final MethodHandle RHINO_LINKER_CALL = findOwnMH_S("call", Object.class, Function.class, Scriptable.class,
            Object[].class);

    private static final Map<Class<?>, MethodHandle> CONVERTERS = new HashMap<>();
    static
    {
        CONVERTERS.put(boolean.class, findOwnMH_S("toBoolean", boolean.class, Scriptable.class));
        CONVERTERS.put(int.class, findOwnMH_S("toInt32", int.class, Scriptable.class));
        CONVERTERS.put(long.class, findOwnMH_S("toLong", long.class, Scriptable.class));
        CONVERTERS.put(double.class, findOwnMH_S("toNumber", double.class, Scriptable.class));
    }

    // re-use same Rhino context for same Nashorn context
    private static final Map<ScriptContext, Context> contextForNashornContext = new WeakHashMap<>();
    // re-use same Rhino global for same Rhino context
    private static final Map<Context, Scriptable> rhinoGlobalForContext = new WeakHashMap<>();

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
            return findCallMethod(desc);
        case "new":
            // TODO map to Function#construct - should we facade current scope as "scope"-parameter?
            return null;
        default:
            return null;
        }
    }

    protected static Scriptable getRhinoGlobal(final Context rhinoContext)
    {
        Scriptable global = rhinoGlobalForContext.get(rhinoContext);
        if (global == null)
        {
            global = rhinoContext.initStandardObjects();

            // TODO Do we want to seal global and remove hamrful default operations?
            rhinoGlobalForContext.put(rhinoContext, global);
        }

        return global;
    }

    protected static void setRhinoContextForCurrentNashornContext(final Context context)
    {
        final Global global = Global.instance();
        // even though JDK 8u40 moved "context" from actual property to a pseudo one
        // this works for 8u20 AND 8u40
        final Object expectedContext = global.get("context");

        if (expectedContext instanceof ScriptContext)
        {
            contextForNashornContext.put((ScriptContext) expectedContext, context);
        }
    }

    protected static Context getRhinoContextOrNull()
    {
        final Context context;

        if (Context.getCurrentContext() != null)
        {
            context = Context.getCurrentContext();
        }
        else
        {

            final Global global = Global.instance();
            // even though JDK 8u40 moved "context" from actual property to a pseudo one
            // this works for 8u20 AND 8u40
            final Object expectedContext = global.get("context");

            if (expectedContext instanceof ScriptContext)
            {
                context = contextForNashornContext.get(expectedContext);
            }
            else
            {
                context = null;
            }
        }

        return context;
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
        final MethodHandle setter = MH.insertArguments(RHINO_LINKER_PUT, 1, name);
        return new GuardedInvocation(setter, IS_SCRIPTABLE_GUARD);
    }

    protected static GuardedInvocation findCallMethod(final CallSiteDescriptor desc)
    {
        // TODO If call site is already a vararg, don't do asCollector
        MethodHandle mh = CommonRhinoHandles.applyCommonRhinoHandles(RHINO_LINKER_CALL, 2, true);
        if (NashornCallSiteDescriptor.isApplyToCall(desc))
        {
            mh = MH.insertArguments(RHINO_LINKER_CALL_TO_APPLY, 0, mh);
        }
        return new GuardedInvocation(MH.asCollector(mh, Object[].class, desc.getMethodType().parameterCount() - 2), IS_FUNCTION_GUARD);
    }

    @SuppressWarnings("unused")
    private static Object get(final Scriptable scriptable, final Object key)
    {
        Context.enter(RhinoLinker.getRhinoContextOrNull());
        try
        {
            Object rvalue = null;
            if (key instanceof Integer)
            {
                rvalue = ScriptableObject.getProperty(scriptable, ((Integer) key).intValue());
            }
            else if (key instanceof Number)
            {
                final double value = ((Number) key).doubleValue();
                final int index = JSType.isRepresentableAsInt(value) ? (int) value : -1;
                if (index > -1)
                {
                    rvalue = ScriptableObject.getProperty(scriptable, index);
                }
            }
            else if (key instanceof String)
            {
                final String name = (String) key;
                rvalue = ScriptableObject.getProperty(scriptable, name);
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
        Context.enter(RhinoLinker.getRhinoContextOrNull());
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
    private static Object callToApply(final MethodHandle mh, final Function fn, final Object... args)
    {
        assert args.length >= 2;
        final Object receiver = args[0];
        final Object[] arguments = new Object[args.length - 1];
        System.arraycopy(args, 1, arguments, 0, arguments.length);
        try
        {
            return mh.invokeExact(fn, receiver, arguments);
        }
        catch (final RuntimeException | Error e)
        {
            throw e;
        }
        catch (final Throwable e)
        {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    private static Object call(final Function fn, final Scriptable thiz, final Object[] args)
    {
        final Context ctxt = Context.enter(getRhinoContextOrNull());
        try
        {
            setRhinoContextForCurrentNashornContext(ctxt);
            // TODO Should we facade current scope as Nashorn "scope"-parameter?
            final Scriptable scope = getRhinoGlobal(ctxt);

            final Object result = fn.call(ctxt, scope, thiz, args);
            return result;
        }
        finally
        {
            Context.exit();
        }
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
    private static boolean isFunctionObject(final Object self)
    {
        return self instanceof Function;
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
