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
package de.axelfaust.alfresco.enhScriptEnv.common.script.functions.simulated;

import java.util.List;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 *
 * @author Axel Faust
 */
public class ListUnshiftFunction extends BaseFunction
{
    public ListUnshiftFunction(final Scriptable scope)
    {
        super(scope, null);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object call(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args)
    {
        try
        {
            if (!(thisObj instanceof List<?>))
            {
                throw new IllegalArgumentException("Can only call unshift on List instance");
            }

            @SuppressWarnings("unchecked")
            final List<Object> list = (List<Object>) thisObj;
            for (int idx = 0; idx < args.length; idx++)
            {
                list.add(idx, args[idx]);
            }

            return Double.valueOf(list.size());
        }
        catch (final Throwable e)
        {
            if (e instanceof RuntimeException)
            {
                throw (RuntimeException) e;
            }
            throw new RuntimeException("Error forwarding shift to remove(0)", e);
        }
    }

}