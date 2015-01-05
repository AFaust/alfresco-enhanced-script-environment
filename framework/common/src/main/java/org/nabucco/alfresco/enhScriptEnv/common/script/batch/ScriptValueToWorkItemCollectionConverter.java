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
package org.nabucco.alfresco.enhScriptEnv.common.script.batch;

import java.util.Collection;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public interface ScriptValueToWorkItemCollectionConverter
{
    /**
     * Determines if this converter supports the conversion of the provided value object to a collection of work items to be processed in a
     * batch
     *
     * @param workValue
     *            the script value object representing a collection of items to be processed in a batch
     * @return {@code true} if this converter supports the value object, {@code false} otherwise
     */
    boolean isSupported(Object workValue);

    /**
     * Converts a value object to a collection of work items to be processed in a batch
     *
     * @param workValue
     *            the script value object representing a collection of items to be processed in a batch
     * @return the standardized collection of items to be processed in a batch
     */
    Collection<?> convert(Object workValue);
}
