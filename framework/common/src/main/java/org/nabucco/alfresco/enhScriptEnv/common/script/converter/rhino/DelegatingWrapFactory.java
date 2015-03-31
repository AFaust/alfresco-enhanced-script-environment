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
package org.nabucco.alfresco.enhScriptEnv.common.script.converter.rhino;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class DelegatingWrapFactory extends WrapFactory
{

    protected final ThreadLocal<Boolean> recursionGuard = new ThreadLocal<Boolean>();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Object wrap(final Context cx, final Scriptable scope, final Object obj, final Class staticType)
    {
        final Object wrapped = super.wrap(cx, scope, obj, staticType);

        final Boolean guardValue = this.recursionGuard.get();
        this.recursionGuard.set(Boolean.TRUE);
        try
        {
            final Object result = wrapped instanceof String ? ValueConverter.GLOBAL_CONVERTER.get()
                    .convertValueForScript(wrapped) : wrapped;

            return result;
        }
        finally
        {
            this.recursionGuard.set(guardValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Scriptable wrapAsJavaObject(final Context cx, final Scriptable scope, final Object javaObject,
            final Class staticType)
    {
        final Scriptable result;

        final Boolean guardValue = this.recursionGuard.get();
        if (Boolean.TRUE.equals(guardValue)
                || !ValueConverter.GLOBAL_CONVERTER.get().canConvertValueForScript(javaObject, Scriptable.class))
        {
            result = super.wrapAsJavaObject(cx, scope, javaObject, staticType);
        }
        else
        {
            this.recursionGuard.set(Boolean.TRUE);
            try
            {
                result = (Scriptable) ValueConverter.GLOBAL_CONVERTER.get().convertValueForScript(javaObject,
                        Scriptable.class);
                if (!(javaObject instanceof Scriptable) && result.getParentScope() == null)
                {
                    result.setParentScope(scope);
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
