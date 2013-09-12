/*
 * Copyright 2013 PRODYNA AG
 *
 * Licensed under the Eclipse Public License (EPL), Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/eclipse-1.0.php or
 * http://www.nabucco.org/License.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.nabucco.alfresco.enhScriptEnv.common.script.batch.converters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.nabucco.alfresco.enhScriptEnv.common.script.batch.ScriptValueToWorkItemCollectionConverter;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
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
