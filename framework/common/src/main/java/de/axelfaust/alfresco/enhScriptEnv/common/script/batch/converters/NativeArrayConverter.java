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
package de.axelfaust.alfresco.enhScriptEnv.common.script.batch.converters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import de.axelfaust.alfresco.enhScriptEnv.common.script.batch.ScriptValueToWorkItemCollectionConverter;

/**
 * @author Axel Faust
 */
public class NativeArrayConverter implements ScriptValueToWorkItemCollectionConverter
{

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSupported(final Object workValue)
    {
        final boolean supported = workValue instanceof NativeArray;
        return supported;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<?> convert(final Object workValue)
    {
        final Collection<Object> result;
        if (workValue instanceof NativeArray)
        {
            final NativeArray array = (NativeArray) workValue;
            if (array.getLength() == 0)
            {
                result = Collections.emptySet();
            }
            else
            {
                result = new ArrayList<Object>();
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
                        result.add(element);
                    }
                }
            }
        }
        else
        {
            result = Collections.emptySet();
        }
        return result;
    }

}
