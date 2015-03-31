/*
 * Copyright 2015 PRODYNA AG
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
package org.nabucco.alfresco.enhScriptEnv.common.webscripts.processor;

import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.locator.RegisteredScriptLocator;
import org.springframework.extensions.webscripts.ScriptContent;
import org.springframework.extensions.webscripts.ScriptLoader;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class SurfRegisteredScriptLocator extends RegisteredScriptLocator<ScriptContent, ScriptContentAdapter>
{
    protected ScriptLoader scriptLoader;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        super.afterPropertiesSet();
        PropertyCheck.mandatory(this, "scriptLoader", this.scriptLoader);
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    protected ScriptContentAdapter convert(final ScriptContent baseScript)
    {
        return new ScriptContentAdapter(baseScript, this.scriptLoader);
    }

    /**
     * @param scriptLoader
     *            the scriptLoader to set
     */
    public final void setScriptLoader(final ScriptLoader scriptLoader)
    {
        this.scriptLoader = scriptLoader;
    }

}
