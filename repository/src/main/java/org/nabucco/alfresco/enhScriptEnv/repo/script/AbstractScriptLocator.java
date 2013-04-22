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
package org.nabucco.alfresco.enhScriptEnv.repo.script;

import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author <a href="mailto:axel.faust@prodyna.com">Axel Faust</a>, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class AbstractScriptLocator implements ScriptLocator, InitializingBean
{

    protected String name;
    protected EnhancedScriptProcessor scriptProcessor;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "scriptProcessor", this.scriptProcessor);
        this.scriptProcessor.registerScriptLocator(this.name, this);
    }

    /**
     * @param name
     *            the name to set
     */
    public final void setName(final String name)
    {
        this.name = name;
    }

    /**
     * @param scriptProcessor
     *            the scriptProcessor to set
     */
    public final void setScriptProcessor(final EnhancedScriptProcessor scriptProcessor)
    {
        this.scriptProcessor = scriptProcessor;
    }

}
