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
package org.nabucco.alfresco.enhScriptEnv.common.script.converter;

import java.util.Map;

/**
 * Interface for a script engine-specific value converter capable of converting object between the regular Java and the script environment
 * either by converting them into specific instances or using facading techniques.
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public interface ValueConverter
{

    /**
     * Checks if a specific value from the script environment can be converted to a value that can be used in regular Java components. This
     * operation provides a way to pre-emptively check the support without incurring the actual conversion overhead.
     *
     * @param value
     *            the value to check
     * @return {@code true} if the value can be converted, {@code false} otherwise
     */
    boolean canConvertValueForJava(Object value);

    /**
     * Checks if a specific value from the script environment can be converted to a value that can be used in regular Java components. This
     * operation provides a way to pre-emptively check the support without incurring the actual conversion overhead.
     *
     * @param value
     *            the value to check
     * @param expectedClass
     *            the interface / class expectation the result object has to fulfill
     * @return {@code true} if the value can be converted, {@code false} otherwise
     */
    boolean canConvertValueForJava(Object value, Class<?> expectedClass);

    /**
     * Converts a value from the script environment to a value that can be used in regular Java components. Implementations SHOULD ensure
     * that any non-script-specific {@link Map}-like object is preserved in the conversion while elements, keys or values are recursively
     * put through the conversion.
     *
     * @param value
     *            the value to convert
     * @return the converted value
     */
    Object convertValueForJava(Object value);

    /**
     * Converts a value from the script environment to a value that can be used in regular Java components. Implementations SHOULD ensure
     * that any non-script-specific {@link Map}-like object is preserved in the conversion while elements, keys or values are recursively
     * put through the conversion.
     *
     *
     * @param value
     *            the value to convert
     * @param expectedClass
     *            the interface / class expectation the result object has to fulfill
     * @return the converted value
     */
    Object convertValueForJava(Object value, Class<?> expectedClass);

    /**
     * Checks if a specific object can be converted to a script-compatible value. This operation provides a way to pre-emptively check the
     * support without incurring the actual conversion overhead.
     *
     * @param value
     *            the value to check
     * @return {@code true} if the value can be converted, {@code false} otherwise
     */
    boolean canConvertValueForScript(Object value);

    /**
     * Checks if a specific object can be converted to a script-compatible value. This operation provides a way to pre-emptively check the
     * support without incurring the actual conversion overhead.
     *
     * @param value
     *            the value to check
     * @param expectedClass
     *            the interface / class expectation the result object has to fulfill
     * @return {@code true} if the value can be converted, {@code false} otherwise
     */
    boolean canConvertValueForScript(Object value, Class<?> expectedClass);

    /**
     * Converts the provided object into a script-compatible value. Implementations SHOULD ensure that any non-script-specific {@link Map}
     * -like object is preserved in the conversion while elements, keys or values are recursively put through the conversion.
     *
     * @param value
     *            the object to convert
     * @return the converted value instance
     */
    Object convertValueForScript(Object value);

    /**
     * Converts the provided object into a script-compatible value. Implementations SHOULD ensure that any non-script-specific {@link Map}
     * -like object is preserved in the conversion while elements, keys or values are recursively put through the conversion.
     *
     * @param value
     *            the object to convert
     * @param expectedClass
     *            the interface / class expectation the result object has to fulfill
     * @return the converted value
     */
    Object convertValueForScript(Object value, Class<?> expectedClass);
}
