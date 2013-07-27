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
package org.nabucco.alfresco.enhScriptEnv.share.surf;

import org.nabucco.alfresco.enhScriptEnv.common.webscripts.processor.EnhancedJSScriptProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.config.WebFrameworkConfigElement;
import org.springframework.extensions.surf.AutowireService;
import org.springframework.extensions.webscripts.ScriptProcessor;
import org.springframework.extensions.webscripts.TemplateProcessor;
import org.springframework.extensions.webscripts.processor.FTLTemplateProcessor;
import org.springframework.extensions.webscripts.processor.JSScriptProcessor;

/**
 * This variation of an {@link AutowireService} is necessary to allow the use of alternative template and script processors. The base class
 * that is the default for Spring Surf requires that all script processors extend {@link JSScriptProcessor} and all template processors
 * extend {@link FTLTemplateProcessor}, which is a rather rigid requirement for alternative implementations.
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class TypeUnspecificAutowireService extends AutowireService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TypeUnspecificAutowireService.class);

    // have to use these quirky names to avoid problems with Spring bean definition and property value conversion
    protected TemplateProcessor actualWebscriptsTemplateProcessor;
    protected ScriptProcessor actualWebscriptsScriptProcessor;
    protected TemplateProcessor actualTemplatesTemplateProcessor;
    protected ScriptProcessor actualTemplatesScriptProcessor;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configureMode(final WebFrameworkConfigElement webFrameworkConfig) throws Exception
    {
        final String autowireModeId = (String) webFrameworkConfig.getAutowireModeId();
        if (autowireModeId != null)
        {
            // Autowire Mode: Development
            if ("developer".equalsIgnoreCase(autowireModeId) || "development".equalsIgnoreCase(autowireModeId))
            {
                // For persister context, turn caching off
                webFrameworkConfig.getPersisterConfigDescriptor().setCacheEnabled(false);
                webFrameworkConfig.getPersisterConfigDescriptor().setCacheCheckDelay(0);

                this.autowireScriptProcesspr(this.actualWebscriptsScriptProcessor, false);
                this.autowireScriptProcesspr(this.actualTemplatesScriptProcessor, false);

                this.autowireTemplateProcesspr(this.actualWebscriptsTemplateProcessor, false);
                this.autowireTemplateProcesspr(this.actualTemplatesTemplateProcessor, false);
            }

            // Autowire Mode: Production
            if ("production".equalsIgnoreCase(autowireModeId))
            {
                // For persister context, turn caching on
                webFrameworkConfig.getPersisterConfigDescriptor().setCacheEnabled(true);
                webFrameworkConfig.getPersisterConfigDescriptor().setCacheCheckDelay(-1);

                this.autowireTemplateProcesspr(this.actualWebscriptsTemplateProcessor, true);
                this.autowireTemplateProcesspr(this.actualTemplatesTemplateProcessor, true);

                this.autowireScriptProcesspr(this.actualWebscriptsScriptProcessor, true);
                this.autowireScriptProcesspr(this.actualTemplatesScriptProcessor, true);
            }

            LOGGER.info("Autowire Mode - {}", autowireModeId);
        }
        else
        {
            // If the autowireModeId is set to null then it has not been set, so we cannot configure a mode.
            // This is indicative of the <autowire> (or at the very least its <mode>) element not being found
            // in the Spring Surf configuration.
        }
    }

    protected void autowireScriptProcesspr(final ScriptProcessor processor, final boolean productionMode)
    {
        // necessary for autowire compatibility - it would have been better if Spring only sets this via proper configuration properties
        if (processor instanceof JSScriptProcessor)
        {
            ((JSScriptProcessor) processor).setCompile(productionMode);
        }
        else if (processor instanceof EnhancedJSScriptProcessor)
        {
            ((EnhancedJSScriptProcessor) processor).setCompileScripts(productionMode);
        }
    }

    protected void autowireTemplateProcesspr(final TemplateProcessor processor, final boolean productionMode)
    {
        // necessary for autowire compatibility - it would have been better if Spring only sets this via proper configuration properties
        if (processor instanceof FTLTemplateProcessor)
        {
            ((FTLTemplateProcessor) processor).setUpdateDelay(productionMode ? (60 * 60 * 24) : 0);
        }
    }

    /**
     * @return the actualWebscriptsTemplateProcessor
     */
    public final TemplateProcessor getActualWebscriptsTemplateProcessor()
    {
        return this.actualWebscriptsTemplateProcessor;
    }

    /**
     * @param actualWebscriptsTemplateProcessor
     *            the actualWebscriptsTemplateProcessor to set
     */
    public final void setActualWebscriptsTemplateProcessor(final TemplateProcessor actualWebscriptsTemplateProcessor)
    {
        this.actualWebscriptsTemplateProcessor = actualWebscriptsTemplateProcessor;
    }

    /**
     * @return the actualWebscriptsScriptProcessor
     */
    public final ScriptProcessor getActualWebscriptsScriptProcessor()
    {
        return this.actualWebscriptsScriptProcessor;
    }

    /**
     * @param actualWebscriptsScriptProcessor
     *            the actualWebscriptsScriptProcessor to set
     */
    public final void setActualWebscriptsScriptProcessor(final ScriptProcessor actualWebscriptsScriptProcessor)
    {
        this.actualWebscriptsScriptProcessor = actualWebscriptsScriptProcessor;
    }

    /**
     * @return the actualTemplatesTemplateProcessor
     */
    public final TemplateProcessor getActualTemplatesTemplateProcessor()
    {
        return this.actualTemplatesTemplateProcessor;
    }

    /**
     * @param actualTemplatesTemplateProcessor
     *            the actualTemplatesTemplateProcessor to set
     */
    public final void setActualTemplatesTemplateProcessor(final TemplateProcessor actualTemplatesTemplateProcessor)
    {
        this.actualTemplatesTemplateProcessor = actualTemplatesTemplateProcessor;
    }

    /**
     * @return the actualTemplatesScriptProcessor
     */
    public final ScriptProcessor getActualTemplatesScriptProcessor()
    {
        return this.actualTemplatesScriptProcessor;
    }

    /**
     * @param actualTemplatesScriptProcessor
     *            the actualTemplatesScriptProcessor to set
     */
    public final void setActualTemplatesScriptProcessor(final ScriptProcessor actualTemplatesScriptProcessor)
    {
        this.actualTemplatesScriptProcessor = actualTemplatesScriptProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWebscriptsTemplateProcessor(final FTLTemplateProcessor webscriptsTemplateProcessor)
    {
        this.setActualWebscriptsTemplateProcessor((TemplateProcessor) webscriptsTemplateProcessor);
        super.setWebscriptsTemplateProcessor(webscriptsTemplateProcessor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWebscriptsScriptProcessor(final JSScriptProcessor webscriptsScriptProcessor)
    {
        this.setActualWebscriptsScriptProcessor((ScriptProcessor) webscriptsScriptProcessor);
        super.setWebscriptsScriptProcessor(webscriptsScriptProcessor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTemplatesTemplateProcessor(final FTLTemplateProcessor templatesTemplateProcessor)
    {
        this.setActualTemplatesTemplateProcessor((TemplateProcessor) templatesTemplateProcessor);
        super.setTemplatesTemplateProcessor(templatesTemplateProcessor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTemplatesScriptProcessor(final JSScriptProcessor templatesScriptProcessor)
    {
        this.setActualTemplatesScriptProcessor((ScriptProcessor) templatesScriptProcessor);
        super.setTemplatesScriptProcessor(templatesScriptProcessor);
    }

}
