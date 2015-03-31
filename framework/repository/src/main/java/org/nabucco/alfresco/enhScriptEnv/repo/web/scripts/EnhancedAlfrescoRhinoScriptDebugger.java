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
package org.nabucco.alfresco.enhScriptEnv.repo.web.scripts;

import java.util.List;

import org.alfresco.repo.web.scripts.AlfrescoRhinoScriptDebugger;
import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import org.springframework.beans.factory.InitializingBean;

/**
 * An enhanced local Repository-tier Rhino debugger that notifies script processors about (de-)activation.
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class EnhancedAlfrescoRhinoScriptDebugger extends AlfrescoRhinoScriptDebugger implements InitializingBean
{
    private List<EnhancedScriptProcessor<?>> scriptProcessors;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "scriptProcessors", this.scriptProcessors);
        start();
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public synchronized void show()
    {
        final boolean visibleBefore = isVisible();
        super.show();
        final boolean visibleAfter = isVisible();

        if (visibleBefore != visibleAfter)
        {
            for (final EnhancedScriptProcessor<?> scriptProcessor : this.scriptProcessors)
            {
                scriptProcessor.debuggerAttached();
            }
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public synchronized void hide()
    {
        final boolean visibleBefore = isVisible();
        super.hide();
        final boolean visibleAfter = isVisible();

        if (visibleBefore != visibleAfter)
        {
            for (final EnhancedScriptProcessor<?> scriptProcessor : this.scriptProcessors)
            {
                scriptProcessor.debuggerDetached();
            }
        }
    }

    /**
     * @param scriptProcessors
     *            the scriptProcessors to set
     */
    public final void setScriptProcessors(List<EnhancedScriptProcessor<?>> scriptProcessors)
    {
        this.scriptProcessors = scriptProcessors;
    }
}
