/*
 * Copyright 2016 Axel Faust
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
package de.axelfaust.alfresco.enhScriptEnv.repo.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.processor.ProcessorExtension;
import org.alfresco.scripts.ScriptException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.ScriptProcessor;
import org.alfresco.service.cmr.repository.ScriptService;
import org.alfresco.service.namespace.QName;
import de.axelfaust.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import de.axelfaust.alfresco.enhScriptEnv.common.script.ReferenceScript;
import de.axelfaust.alfresco.enhScriptEnv.common.script.ScopeContributor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Axel Faust
 */
public class SwitchableScriptProcessor implements ScriptProcessor, EnhancedScriptProcessor<ScriptLocation>, BeanPostProcessor,
        ApplicationContextAware
{

    protected ApplicationContext applicationContext;

    protected ScriptService scriptService;

    protected ScriptProcessor defaultScriptProcessor;

    protected String activeScriptProcessor;

    protected final Map<String, ScriptProcessor> processorByName = new HashMap<String, ScriptProcessor>();

    protected final List<ProcessorExtension> registeredExtensions = new ArrayList<ProcessorExtension>();

    protected final List<ScopeContributor> registeredContributors = new ArrayList<ScopeContributor>();

    public void setApplicationContext(final ApplicationContext applicationContext)
    {
        this.applicationContext = applicationContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException
    {
        final Object result;
        if (bean instanceof ScriptProcessor && bean != this)
        {
            if ("javaScriptProcessor".equals(beanName))
            {
                this.defaultScriptProcessor = (ScriptProcessor) bean;
                this.processorByName.put(beanName, this.defaultScriptProcessor);
                result = this;
            }
            else
            {
                final ScriptProcessor scriptProcessor = (ScriptProcessor) bean;
                if (this.getName().equals(scriptProcessor.getName()) && this.getExtension().equals(scriptProcessor.getExtension()))
                {
                    this.processorByName.put(beanName, scriptProcessor);
                }

                result = bean;
            }
        }
        else
        {
            result = bean;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException
    {
        if (bean instanceof ScriptProcessor)
        {
            // need to make sure that WE are registered, not the other bean
            if (this.scriptService == null)
            {
                this.scriptService = this.applicationContext.getBean("scriptService", ScriptService.class);
            }
            this.scriptService.registerScriptProcessor(this);

            final ScriptProcessor scriptProcessor = (ScriptProcessor) bean;
            for (final ProcessorExtension processorExtension : this.registeredExtensions)
            {
                scriptProcessor.registerProcessorExtension(processorExtension);
            }

            if (scriptProcessor instanceof EnhancedScriptProcessor<?>)
            {
                final EnhancedScriptProcessor<?> enhancedScriptProcessor = (EnhancedScriptProcessor<?>) scriptProcessor;
                for (final ScopeContributor scopeContributor : this.registeredContributors)
                {
                    enhancedScriptProcessor.registerScopeContributor(scopeContributor);
                }
            }
        }
        return bean;
    }

    // need to support this as it may be called on defaultScriptProcessor, which we replace
    public void register()
    {
        if (this.scriptService == null)
        {
            this.scriptService = this.applicationContext.getBean("scriptService", ScriptService.class);
        }
        this.scriptService.registerScriptProcessor(this);
    }

    // need to support this as it may be called on defaultScriptProcessor, which we replace
    public void init()
    {
        if (this.scriptService == null)
        {
            this.scriptService = this.applicationContext.getBean("scriptService", ScriptService.class);
        }
        this.scriptService.registerScriptProcessor(this);
    }

    /**
     * @param scriptService
     *            the scriptService to set
     */
    public final void setScriptService(final ScriptService scriptService)
    {
        this.scriptService = scriptService;
    }

    /**
     * @param activeScriptProcessor
     *            the activeScriptProcessor to set
     */
    public final void setActiveScriptProcessor(final String activeScriptProcessor)
    {
        this.activeScriptProcessor = activeScriptProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName()
    {
        return this.defaultScriptProcessor.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getExtension()
    {
        return this.defaultScriptProcessor.getExtension();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerProcessorExtension(final ProcessorExtension processorExtension)
    {
        this.registeredExtensions.add(processorExtension);
        for (final ScriptProcessor processor : this.processorByName.values())
        {
            processor.registerProcessorExtension(processorExtension);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(final ScriptLocation location, final Map<String, Object> model)
    {
        final ScriptProcessor scriptProcessor = this.processorByName.containsKey(this.activeScriptProcessor) ? this.processorByName
                .get(this.activeScriptProcessor) : this.defaultScriptProcessor;
        return scriptProcessor.execute(location, model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(final NodeRef nodeRef, final QName contentProp, final Map<String, Object> model)
    {
        final ScriptProcessor scriptProcessor = this.processorByName.containsKey(this.activeScriptProcessor) ? this.processorByName
                .get(this.activeScriptProcessor) : this.defaultScriptProcessor;
        return scriptProcessor.execute(nodeRef, contentProp, model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(final String location, final Map<String, Object> model)
    {
        final ScriptProcessor scriptProcessor = this.processorByName.containsKey(this.activeScriptProcessor) ? this.processorByName
                .get(this.activeScriptProcessor) : this.defaultScriptProcessor;
        return scriptProcessor.execute(location, model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object executeString(final String script, final Map<String, Object> model)
    {
        final ScriptProcessor scriptProcessor = this.processorByName.containsKey(this.activeScriptProcessor) ? this.processorByName
                .get(this.activeScriptProcessor) : this.defaultScriptProcessor;
        return scriptProcessor.executeString(script, model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset()
    {
        for (final ScriptProcessor processor : this.processorByName.values())
        {
            processor.reset();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object executeInScope(final ScriptLocation location, final Object scope)
    {
        final ScriptProcessor scriptProcessor = this.processorByName.containsKey(this.activeScriptProcessor) ? this.processorByName
                .get(this.activeScriptProcessor) : this.defaultScriptProcessor;

        final Object result;
        if (scriptProcessor instanceof EnhancedScriptProcessor<?>)
        {
            result = ((EnhancedScriptProcessor<ScriptLocation>) scriptProcessor).executeInScope(location, scope);
        }
        else
        {
            throw new ScriptException("Active script processor does not support EnhancedScriptProcessor");
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object executeInScope(final String source, final Object scope)
    {
        final ScriptProcessor scriptProcessor = this.processorByName.containsKey(this.activeScriptProcessor) ? this.processorByName
                .get(this.activeScriptProcessor) : this.defaultScriptProcessor;

        final Object result;
        if (scriptProcessor instanceof EnhancedScriptProcessor<?>)
        {
            result = ((EnhancedScriptProcessor<?>) scriptProcessor).executeInScope(source, scope);
        }
        else
        {
            throw new ScriptException("Active script processor does not support EnhancedScriptProcessor");
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceScript getContextScriptLocation()
    {
        final ScriptProcessor scriptProcessor = this.processorByName.containsKey(this.activeScriptProcessor) ? this.processorByName
                .get(this.activeScriptProcessor) : this.defaultScriptProcessor;

        final ReferenceScript result;
        if (scriptProcessor instanceof EnhancedScriptProcessor<?>)
        {
            result = ((EnhancedScriptProcessor<?>) scriptProcessor).getContextScriptLocation();
        }
        else
        {
            throw new ScriptException("Active script processor does not support EnhancedScriptProcessor");
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ReferenceScript> getScriptCallChain()
    {
        final ScriptProcessor scriptProcessor = this.processorByName.containsKey(this.activeScriptProcessor) ? this.processorByName
                .get(this.activeScriptProcessor) : this.defaultScriptProcessor;

        final List<ReferenceScript> result;
        if (scriptProcessor instanceof EnhancedScriptProcessor<?>)
        {
            result = ((EnhancedScriptProcessor<?>) scriptProcessor).getScriptCallChain();
        }
        else
        {
            throw new ScriptException("Active script processor does not support EnhancedScriptProcessor");
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inheritCallChain(final Object parentContext)
    {
        final ScriptProcessor scriptProcessor = this.processorByName.containsKey(this.activeScriptProcessor) ? this.processorByName
                .get(this.activeScriptProcessor) : this.defaultScriptProcessor;

        if (scriptProcessor instanceof EnhancedScriptProcessor<?>)
        {
            ((EnhancedScriptProcessor<?>) scriptProcessor).inheritCallChain(parentContext);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object initializeScope(final ScriptLocation location)
    {
        final ScriptProcessor scriptProcessor = this.processorByName.containsKey(this.activeScriptProcessor) ? this.processorByName
                .get(this.activeScriptProcessor) : this.defaultScriptProcessor;

        final Object result;
        if (scriptProcessor instanceof EnhancedScriptProcessor<?>)
        {
            result = ((EnhancedScriptProcessor<ScriptLocation>) scriptProcessor).initializeScope(location);
        }
        else
        {
            throw new ScriptException("Active script processor does not support EnhancedScriptProcessor");
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerScopeContributor(final ScopeContributor contributor)
    {
        for (final ScriptProcessor scriptProcessor : this.processorByName.values())
        {
            if (scriptProcessor instanceof EnhancedScriptProcessor<?>)
            {
                ((EnhancedScriptProcessor<?>) scriptProcessor).registerScopeContributor(contributor);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debuggerAttached()
    {
        final ScriptProcessor scriptProcessor = this.processorByName.containsKey(this.activeScriptProcessor) ? this.processorByName
                .get(this.activeScriptProcessor) : this.defaultScriptProcessor;

        if (scriptProcessor instanceof EnhancedScriptProcessor<?>)
        {
            ((EnhancedScriptProcessor<?>) scriptProcessor).debuggerAttached();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void debuggerDetached()
    {
        final ScriptProcessor scriptProcessor = this.processorByName.containsKey(this.activeScriptProcessor) ? this.processorByName
                .get(this.activeScriptProcessor) : this.defaultScriptProcessor;

        if (scriptProcessor instanceof EnhancedScriptProcessor<?>)
        {
            ((EnhancedScriptProcessor<?>) scriptProcessor).debuggerDetached();
        }
    }

}
