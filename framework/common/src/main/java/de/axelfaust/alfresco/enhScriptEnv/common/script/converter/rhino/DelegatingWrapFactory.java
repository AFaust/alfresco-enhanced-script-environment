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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;

import de.axelfaust.alfresco.enhScriptEnv.common.script.converter.ValueConverter;

/**
 * @author Axel Faust
 */
public class DelegatingWrapFactory extends WrapFactory
{

    protected final ThreadLocal<Boolean> recursionGuard = new ThreadLocal<Boolean>();

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
        final Scriptable result;

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
                result = super.wrapAsJavaObject(cx, scope == null ? this.scope : scope, javaObject, staticType);
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

        return result;
    }

}
