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

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.TransactionListener;

/**
 * A thread local to cache script bindings for simple, one-off script executions in support of the main script execution (e.g. value
 * conversion). Instances of this class will attempt to cleanup the binding upon completion of an enclosing transaction (if one exists).
 * Since bindings managed by instances of this class are not supposed to hold any persistent data, failure to cleanup bindings created
 * outside of an active transaction should not introduce problematic side effects unless used incorrectly.
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class TransactionalBindingsThreadLocal extends ThreadLocal<Bindings> implements TransactionListener
{

    private static final String NASHORN_ENGINE_NAME = "nashorn";

    protected ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(NASHORN_ENGINE_NAME);

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush()
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeCommit(final boolean readOnly)
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeCompletion()
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterCommit()
    {
        this.remove();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterRollback()
    {
        this.remove();
    }

    /**
     * @param scriptEngine
     *            the scriptEngine to set
     */
    public final void setScriptEngine(final ScriptEngine scriptEngine)
    {
        this.scriptEngine = scriptEngine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Bindings initialValue()
    {
        // register for cleanup
        if (AlfrescoTransactionSupport.isActualTransactionActive())
        {
            AlfrescoTransactionSupport.bindListener(this);
        }
        return this.scriptEngine.createBindings();
    }
}
