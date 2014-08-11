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

import java.util.Comparator;

import org.alfresco.util.ParameterCheck;

/**
 * The base interface of a registry {@link ValueInstanceConverter} can be registered with to extend the supported conversion scope.
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public interface ValueInstanceConverterRegistry
{
    /**
     * The base interface for a specific converter capable of handling a specific set of conversions. In order to determine the best-fitting
     * conversion, each converter must provide both a general confidence based on source and expected value types as well as a per-instance
     * convertability check.
     *
     * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
     */
    public interface ValueInstanceConverter
    {
        // confidence is actually not constrained but practically speaking: at some point it gets ridiculous

        public static final int LOWEST_CONFIDENCE = 0;

        public static final int LOW_CONFIDENCE = 250;

        public static final int MEDIUM_CONFIDENCE = 500;

        public static final int HIGH_CONFIDENCE = 750;

        public static final int HIGHEST_CONFIDENCE = 1000;

        /**
         * Retrieves the confidence rating of this value instance converter for a specific Java-compatible to script-compatible value
         * conversion.
         *
         * @param valueInstanceClass
         *            the class of value instance to convert
         * @param expectedClass
         *            the class object the converted value instance is required to be an instance of
         * @return the confidence rating from {@code 0} (lowest) to {@link Integer#MAX_VALUE} (highest) - a higher confidence will cause
         *         this converter to be prioritized when determining the converters capable of handling a specific conversion before
         *         {@link #canConvertValueForScript(Object, ValueConverter, Class) canConvertValueForScript} is called.
         */
        int getForScriptConversionConfidence(Class<?> valueInstanceClass, Class<?> expectedClass);

        /**
         * Checks the possiblity of providing a conversion from Java-compatible to script-compatible value.
         *
         * @param value
         *            the object to convert
         * @param globalDelegate
         *            the global value instance converter to delegate any sub-instance conversions to
         * @param expectedClass
         *            the class object the converted value instance is required to be an instance of
         * @return {@code true} if the value can be converted, {@code false} otherwise
         */
        boolean canConvertValueForScript(Object value, ValueConverter globalDelegate, Class<?> expectedClass);

        /**
         * Converts the provided object into a script-compatible value.
         *
         * @param value
         *            the object to convert
         * @param globalDelegate
         *            the global value instance converter to delegate any sub-instance conversions to
         * @param expectedClass
         *            the class object the converted value instance is required to be an instance of
         * @return the converted value instance
         */
        Object convertValueForScript(Object value, ValueConverter globalDelegate, Class<?> expectedClass);

        /**
         * Retrieves the confidence rating of this value instance converter for a specific script-compatible to Java-compatible value
         * conversion.
         *
         * @param valueInstanceClass
         *            the class of value instance to convert
         * @param expectedClass
         *            the class object the converted value instance is required to be an instance of
         * @return the confidence rating from {@code 0} (lowest) to {@link Integer#MAX_VALUE} (highest) - a higher confidence will cause
         *         this converter to be prioritized when determining the converters capable of handling a specific conversion before
         *         {@link #canConvertValueForJava(Object, ValueConverter, Class) canConvertValueForJava} is called.
         */
        int getForJavaConversionConfidence(Class<?> valueInstanceClass, Class<?> expectedClass);

        /**
         * Checks the possiblity of providing a conversion from script-compatible to Java-compatible value.
         *
         * @param value
         *            the object to convert
         * @param globalDelegate
         *            the global value instance converter to delegate any sub-instance conversions to
         * @param expectedClass
         *            the class object the converted value instance is required to be an instance of
         * @return {@code true} if the value can be converted, {@code false} otherwise
         */
        boolean canConvertValueForJava(Object value, ValueConverter globalDelegate, Class<?> expectedClass);

        /**
         * Converts the provided object into a Java-compatible value.
         *
         * @param value
         *            the object to convert
         * @param globalDelegate
         *            the global value instance converter to delegate any sub-instance conversions to
         * @param expectedClass
         *            the class object the converted value instance is required to be an instance of
         * @return the converted value instance
         */
        Object convertValueForJava(Object value, ValueConverter globalDelegate, Class<?> expectedClass);

    }

    public static class ValueInstanceConverterConfidenceComparator implements Comparator<ValueInstanceConverter>
    {

        private final Class<?> valueInstanceClass;
        private final Class<?> expectedClass;
        private final boolean javaToScript;

        public ValueInstanceConverterConfidenceComparator(final Class<?> valueInstanceClass, final Class<?> expectedClass,
                final boolean javaToScript)
        {
            ParameterCheck.mandatory("valueInstanceClass", valueInstanceClass);
            ParameterCheck.mandatory("expectedClass", expectedClass);

            this.valueInstanceClass = valueInstanceClass;
            this.expectedClass = expectedClass;
            this.javaToScript = javaToScript;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public int compare(final ValueInstanceConverter o1, final ValueInstanceConverter o2)
        {
            final int o1Confidence = this.javaToScript ? o1.getForScriptConversionConfidence(this.valueInstanceClass, this.expectedClass)
                    : o1.getForJavaConversionConfidence(this.valueInstanceClass, this.expectedClass);
            final int o2Confidence = this.javaToScript ? o2.getForScriptConversionConfidence(this.valueInstanceClass, this.expectedClass)
                    : o2.getForJavaConversionConfidence(this.valueInstanceClass, this.expectedClass);

            // higher confidence o1 => negative result => "preceeding" o2
            return o2Confidence - o1Confidence;
        }

    }

    /**
     * Registers a value instance converter for a class / interface of value the converter supports.
     *
     * @param instanceClass
     *            the class / interface of value the converter supports
     * @param converter
     *            the converter
     */
    void registerValueInstanceConverter(Class<?> instanceClass, ValueInstanceConverter converter);
}
