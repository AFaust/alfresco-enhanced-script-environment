/*
 * Copyright 2016 Axel Faust
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
package de.axelfaust.alfresco.enhScriptEnv.common.script.converter.rhino;

import java.util.Arrays;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;
import org.springframework.aop.framework.ConstructorArgumentAwareProxyFactory;
import org.springframework.aop.framework.ProxyFactory;

import de.axelfaust.alfresco.enhScriptEnv.common.script.aop.AdapterObject;
import de.axelfaust.alfresco.enhScriptEnv.common.script.aop.AdapterObjectInterceptor;
import de.axelfaust.alfresco.enhScriptEnv.common.script.aop.NativeJavaMethodWrappingInterceptor;
import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import de.axelfaust.alfresco.enhScriptEnv.common.util.ClassUtils;

/**
 * @author Axel Faust
 */
public class DelegatingWrapFactory extends WrapFactory
{

    protected final ThreadLocal<Boolean> recursionGuard = new ThreadLocal<Boolean>();

    protected final ThreadLocal<Boolean> wrapDisabled = new ThreadLocal<Boolean>();

    protected Scriptable scope;

    public DelegatingWrapFactory()
    {
        super();
    }

    /**
     * @param scope
     *            the scope to set
     */
    public void setScope(final Scriptable scope)
    {
        this.scope = scope;
    }

    public void disableWrap()
    {
        this.wrapDisabled.set(Boolean.TRUE);
    }

    public void enableWrap()
    {
        this.wrapDisabled.set(Boolean.FALSE);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Object wrap(final Context cx, final Scriptable scope, final Object obj, final Class staticType)
    {
        final Object result;

        if (obj instanceof Scriptable)
        {
            // we may need to convert some objects that are already Scriptable
            result = this.wrapAsJavaObject(cx, scope == null ? this.scope : scope, obj, staticType);
        }
        else
        {
            result = super.wrap(cx, scope == null ? this.scope : scope, obj, staticType);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Scriptable wrapAsJavaObject(final Context cx, final Scriptable scope, final Object javaObject, final Class staticType)
    {
        Scriptable result;

        if (Boolean.TRUE.equals(this.wrapDisabled.get()))
        {
            result = super.wrapAsJavaObject(cx, scope, javaObject, staticType);
        }
        else
        {
            final Boolean guardValue = this.recursionGuard.get();
            if (Boolean.TRUE.equals(guardValue)
                    || !ValueConverter.GLOBAL_CONVERTER.get().canConvertValueForScript(javaObject, Scriptable.class))
            {
                if (javaObject instanceof Scriptable)
                {
                    // either an unconvertable Scriptable or recursive wrap call
                    // this counteracts the override in wrap(Context, Scriptable, Object, Class)
                    result = (Scriptable) javaObject;
                }
                else if (!this.isJavaPrimitiveWrap()
                        && (javaObject instanceof CharSequence || javaObject instanceof Number || javaObject instanceof Boolean))
                {
                    result = ScriptRuntime.toObject(cx, scope, javaObject);
                }
                else
                {
                    // default
                    result = super.wrapAsJavaObject(cx, scope == null ? this.scope : scope, javaObject, staticType);

                    if (result instanceof NativeJavaObject)
                    {
                        final ProxyFactory proxyFactory = new ConstructorArgumentAwareProxyFactory(new Object[] {
                                scope == null ? this.scope : scope, javaObject, staticType }, new Class<?>[] { Scriptable.class,
                                Object.class, Class.class });
                        proxyFactory.addAdvice(AdapterObjectInterceptor.getInstance());
                        proxyFactory.addAdvice(NativeJavaMethodWrappingInterceptor.getInstance());
                        proxyFactory.setInterfaces(ClassUtils.collectInterfaces(NativeJavaObject.class,
                                Arrays.<Class<?>> asList(AdapterObject.class)));
                        proxyFactory.setTarget(result);
                        proxyFactory.setProxyTargetClass(true);
                        result = (Scriptable) proxyFactory.getProxy();
                    }
                }
            }
            else
            {
                this.recursionGuard.set(Boolean.TRUE);
                try
                {
                    result = (Scriptable) ValueConverter.GLOBAL_CONVERTER.get().convertValueForScript(javaObject, Scriptable.class);

                    if (!(javaObject instanceof Scriptable) && result.getParentScope() == null)
                    {
                        result.setParentScope(scope == null ? this.scope : scope);
                    }
                }
                finally
                {
                    this.recursionGuard.set(guardValue);
                }
            }
        }

        return result;
    }
}
