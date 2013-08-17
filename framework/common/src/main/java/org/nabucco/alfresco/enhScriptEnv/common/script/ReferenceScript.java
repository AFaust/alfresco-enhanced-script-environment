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
package org.nabucco.alfresco.enhScriptEnv.common.script;

import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public interface ReferenceScript
{
    /**
     * Marker interface for reference path types
     *
     * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
     */
    interface ReferencePathType
    {
        // marker interface
    }

    /**
     *
     * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
     */
    enum CommonReferencePath implements ReferencePathType
    {
        CLASSPATH, FILE;
    }

    /**
     *
     * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
     */
    class DynamicScript implements ReferenceScript
    {
        private final String name;

        public DynamicScript(final String name)
        {
            this.name = name;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public String getName()
        {
            return this.name;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public boolean isSecure()
        {
            return false;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public String getReferencePath(final ReferencePathType typeOfPath)
        {
            return null;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public Collection<ReferencePathType> getSupportedReferencePathTypes()
        {
            return Collections.emptySet();
        }

    }

    /**
     * Obtains the name of the script
     *
     * @return the name of the script
     */
    String getName();

    /**
     * Determines wether a script can be considered secure in that its content / logic is managed by the system and not mutable by end
     * users. A secure script may be granted additional privileges to access the runtime over non-secure scripts.
     *
     * @return {@code true} if the script can be considered to be a secure script (not user-managed), {@code false} otherwise
     */
    boolean isSecure();

    /**
     * Determines the reference path of this script instance that may be used for purposes of script importing or logging. Since the value
     * set of possible reference path types is unrestricted, a script is not expected to support any arbitrary type of reference path and
     * may in rare occasions support even none.
     *
     * @param typeOfPath
     *            the type of reference path to return
     * @return the reference path or {@code null}
     */
    String getReferencePath(ReferencePathType typeOfPath);

    /**
     * Retrieves the collection of reference path types that the implementation of this script can support. The presence of a specific
     * reference path type in the result collection provides no guarantee that an actual instance of a script can be resolved to a reference
     * path of that type - it only guarantees that the implementation attempts to resolve it to a reference pat of that type.
     *
     * @return the collection of supported reference path types
     */
    Collection<ReferencePathType> getSupportedReferencePathTypes();
}
