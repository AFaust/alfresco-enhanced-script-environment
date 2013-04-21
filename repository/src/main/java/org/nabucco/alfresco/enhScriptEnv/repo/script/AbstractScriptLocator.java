/**
 * 
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
