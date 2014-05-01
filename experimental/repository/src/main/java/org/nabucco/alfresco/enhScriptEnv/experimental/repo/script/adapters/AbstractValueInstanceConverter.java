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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.adapters;

import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornValueInstanceConverterRegistry;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornValueInstanceConverterRegistry.ValueInstanceConverter;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class AbstractValueInstanceConverter implements ValueInstanceConverter, InitializingBean
{

    protected NashornValueInstanceConverterRegistry registry;
    protected Class<?> convertableClass;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "registry", this.registry);
        PropertyCheck.mandatory(this, "convertableClass", this.convertableClass);

        this.registry.registerValueInstanceConverter(this.convertableClass, this);
    }

    /**
     * @param registry
     *            the registry to set
     */
    public final void setRegistry(final NashornValueInstanceConverterRegistry registry)
    {
        this.registry = registry;
    }

    /**
     * @param convertableClass
     *            the convertableClass to set
     */
    public final void setConvertableClass(final Class<?> convertableClass)
    {
        this.convertableClass = convertableClass;
    }
}
