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

import java.util.Collection;
import java.util.Collections;

import org.mozilla.javascript.NativeJavaObject;
import de.axelfaust.alfresco.enhScriptEnv.common.script.batch.ScriptValueToWorkItemCollectionConverter;

/**
 * Converts a Java collection wrapped as a Rhino {@link NativeJavaObject} into a collection of work items.
 *
 * @author Axel Faust
 */
public class WrappedJavaCollectionConverter implements ScriptValueToWorkItemCollectionConverter
{

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSupported(final Object workValue)
    {
        final boolean supported;
        if (workValue instanceof NativeJavaObject)
        {
            final Object unwrapped = ((NativeJavaObject) workValue).unwrap();
            supported = unwrapped instanceof Collection<?>;
        }
        else
        {
            supported = false;
        }

        return supported;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<?> convert(final Object workValue)
    {
        final Collection<?> converted;
        if (workValue instanceof NativeJavaObject)
        {
            final Object unwrapped = ((NativeJavaObject) workValue).unwrap();
            converted = unwrapped instanceof Collection<?> ? (Collection<?>) unwrapped : Collections.emptySet();
        }
        else
        {
            converted = Collections.emptySet();
        }
        return converted;
    }

}
