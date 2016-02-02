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
package org.nabucco.alfresco.enhScriptEnv.common.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.alfresco.util.ParameterCheck;

/**
 * @author Axel Faust
 */
public final class ClassUtils
{

    private ClassUtils()
    {
        // NO-OP - prevent instantiation
    }

    public static boolean isInstance(final Object value, final Class<?> expectedClass)
    {
        ParameterCheck.mandatory("value", value);
        ParameterCheck.mandatory("expectedClass", expectedClass);

        final boolean result;

        if (expectedClass.isInstance(value))
        {
            result = true;
        }
        else if (expectedClass.isPrimitive()
                && ((expectedClass.equals(int.class) && Integer.class.isInstance(value))
                        || (expectedClass.equals(long.class) && Long.class.isInstance(value))
                        || (expectedClass.equals(float.class) && Float.class.isInstance(value))
                        || (expectedClass.equals(double.class) && Double.class.isInstance(value)) || (expectedClass
                                .equals(short.class) && Short.class.isInstance(value))
                        || (expectedClass.equals(byte.class) && Byte.class.isInstance(value)) || (expectedClass
                                .equals(boolean.class) && Boolean.class.isInstance(value)) || (expectedClass
                        .equals(char.class) && Character.class.isInstance(value))))
        {
            result = true;
        }
        else
        {
            result = false;
        }

        return result;
    }

    public static Class<?>[] collectInterfaces(final Object source, final Collection<Class<?>> predefinedInterfaces)
    {
        ParameterCheck.mandatory("source", source);
        ParameterCheck.mandatoryCollection("predefinedInterfaces", predefinedInterfaces);

        final Set<Class<?>> interfaces = new HashSet<Class<?>>(predefinedInterfaces);
        Class<?> implClass = source.getClass();
        while (!Object.class.equals(implClass))
        {
            interfaces.addAll(Arrays.asList(implClass.getInterfaces()));

            implClass = implClass.getSuperclass();
        }

        final Class<?>[] interfacesArr = interfaces.toArray(new Class<?>[0]);
        return interfacesArr;
    }
}
