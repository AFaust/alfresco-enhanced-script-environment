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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public interface NashornValueInstanceConverterRegistry
{
    /**
     * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
     */
    public interface ValueConverter
    {
        /**
         * Converts the provided object into a Nashorn-compatible value.
         *
         * @param value
         *            the object to convert
         * @return the converted value instance
         */
        Object convertValueForNashorn(Object value);
    }

    /**
     * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
     */
    public interface ValueInstanceConverter
    {

        /**
         * Converts the provided object into a Nashorn-compatible value.
         *
         * @param value
         *            the object to convert
         * @param globalDelegate
         *            the global value instance converter to delegate any sub-instance conversions to
         * @return the converted value instance
         */
        Object convertValueForNashorn(Object value, ValueConverter globalDelegate);

    }

    /**
     * Registers a value instance converter for the class of value the converter supports.
     *
     * @param instanceClass
     *            the class of value the converter supports
     * @param converter
     *            the converter
     */
    void registerValueInstanceConverter(Class<?> instanceClass, ValueInstanceConverter converter);
}
