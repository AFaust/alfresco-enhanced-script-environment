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
package org.nabucco.alfresco.enhScriptEnv.repo.script.batch;

import java.util.Locale;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.batch.BatchProcessor.BatchProcessWorker;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.util.Pair;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;
import org.nabucco.alfresco.enhScriptEnv.common.script.batch.BaseExecuteBatchWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.surf.util.I18NUtil;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class RepositoryExecuteBatchWorker extends BaseExecuteBatchWorker<RepositoryExecuteBatchFunction> implements
        BatchProcessWorker<Object>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryExecuteBatchWorker.class);

    protected final String fullyAuthenticatedUser = AuthenticationUtil.getFullyAuthenticatedUser();
    protected final String runAsUser = AuthenticationUtil.getRunAsUser();
    protected final Locale locale = I18NUtil.getLocale();
    protected final Locale contentLocale = I18NUtil.getContentLocaleOrNull();

    protected final PlatformTransactionManager txnManager;

    protected RepositoryExecuteBatchWorker(final RepositoryExecuteBatchFunction batchFunction, final Scriptable parentScope,
            final Scriptable thisObj, final Pair<Scriptable, Function> processCallback,
            final Pair<Scriptable, Function> beforeProcessCallback, final Pair<Scriptable, Function> afterProcessCallback,
            final PlatformTransactionManager txnManager)
    {
        super(batchFunction, parentScope, thisObj, processCallback, beforeProcessCallback, afterProcessCallback);
        this.txnManager = txnManager;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public String getIdentifier(final Object entry)
    {
        // should be final but cannot be for sake of exception handling
        String identifier;

        if (entry instanceof Scriptable)
        {
            final Context cx = Context.enter();
            try
            {
                final Object toString = ScriptableObject.getProperty((Scriptable) entry, "toString");
                if (toString instanceof Function)
                {
                    final Object toStringResult = ((Function) toString).call(cx, (Scriptable) entry, (Scriptable) entry, new Object[0]);
                    identifier = ScriptRuntime.toString(toStringResult);
                }
                else if (toString != Scriptable.NOT_FOUND)
                {
                    identifier = ScriptRuntime.toString(toString);
                }
                else if (entry instanceof BaseFunction)
                {
                    final String functionName = ((BaseFunction) entry).getFunctionName();
                    identifier = functionName != null && functionName.length() != 0 ? functionName : entry.toString();
                }
                else
                {
                    identifier = entry.toString();
                }
            }
            catch (final RhinoException ex)
            {
                LOGGER.debug("Exception determining entry identifier via Rhino - falling back to simple toString", ex);
                identifier = entry.toString();
            }
            finally
            {
                Context.exit();
            }
        }
        else if (entry != null)
        {
            identifier = entry.toString();
        }
        else
        {
            identifier = null;
        }

        return identifier;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void beforeProcess() throws Throwable
    {
        // prepare execution context
        AuthenticationUtil.pushAuthentication();
        AuthenticationUtil.setFullyAuthenticatedUser(this.fullyAuthenticatedUser);
        if (this.runAsUser != null && !this.runAsUser.equals(this.fullyAuthenticatedUser))
        {
            AuthenticationUtil.setRunAsUser(this.runAsUser);
        }

        I18NUtil.setLocale(this.locale);
        if (this.contentLocale != null)
        {
            I18NUtil.setContentLocale(this.contentLocale);
        }

        try
        {
            try
            {
                super.doBeforeProcess();
            }
            catch (final WrappedException ex)
            {
                // super should already handle unwrap runtime exceptions
                final Throwable wrappedException = ex.getWrappedException();
                if (wrappedException instanceof RuntimeException)
                {
                    // super should have handled this
                    throw (RuntimeException) wrappedException;
                }
                throw new AlfrescoRuntimeException(wrappedException.getMessage(), wrappedException);
            }
            catch (final Throwable ex)
            {
                throw new AlfrescoRuntimeException(ex.getMessage(), ex);
            }
        }
        catch (final Throwable ex)
        {
            /*
             * The TxnCallback does not propagate non-retryable exceptions to the retrying transaction handler. Some exceptions may be
             * caused by execution of the provided script callback without passing through a service with its transaction interceptor which
             * would mark the transaction for rollback. We have to mark the transaction for rollback manually otherwise we end up with
             * commits of partial changes from the batch. (rollback on any exception is the default behaviour of Alfresco
             * SpringAwareUserTransaction)
             */

            final RuleBasedTransactionAttribute transactionAttribute = new RuleBasedTransactionAttribute();
            transactionAttribute.setReadOnly(true);
            transactionAttribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

            // this never creates a new "real" transaction due to our propagation behavior
            final TransactionStatus transaction = this.txnManager.getTransaction(transactionAttribute);
            try
            {
                if (!transaction.isRollbackOnly())
                {
                    LOGGER.debug("Marking transaction as rollback-only due to exception during batch processing", ex);
                    transaction.setRollbackOnly();
                }
            }
            finally
            {
                // this never actually commits a "real" transaction - it just clears transaction synchronizations
                this.txnManager.commit(transaction);
            }

            throw ex;
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void process(final Object entry) throws Throwable
    {
        try
        {
            try
            {
                super.doProcess(entry);
            }
            catch (final WrappedException ex)
            {
                // super should already handle unwrap runtime exceptions
                final Throwable wrappedException = ex.getWrappedException();
                if (wrappedException instanceof RuntimeException)
                {
                    // super should have handled this
                    throw (RuntimeException) wrappedException;
                }
                throw new AlfrescoRuntimeException(wrappedException.getMessage(), wrappedException);
            }
            catch (final Throwable ex)
            {
                throw new AlfrescoRuntimeException(ex.getMessage(), ex);
            }
        }
        catch (final Throwable ex)
        {
            /*
             * The TxnCallback does not propagate non-retryable exceptions to the retrying transaction handler. Some exceptions may be
             * caused by execution of the provided script callback without passing through a service with its transaction interceptor which
             * would mark the transaction for rollback. We have to mark the transaction for rollback manually otherwise we end up with
             * commits of partial changes from the batch. (rollback on any exception is the default behaviour of Alfresco
             * SpringAwareUserTransaction)
             */

            final RuleBasedTransactionAttribute transactionAttribute = new RuleBasedTransactionAttribute();
            transactionAttribute.setReadOnly(true);
            transactionAttribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);

            final TransactionStatus transaction = this.txnManager.getTransaction(transactionAttribute);
            if (!transaction.isRollbackOnly())
            {
                LOGGER.debug("Marking transaction as rollback-only due to exception during batch processing", ex);
                transaction.setRollbackOnly();
            }

            throw ex;
        }
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public void afterProcess() throws Throwable
    {
        try
        {
            try
            {
                super.doAfterProcess();
            }
            catch (final WrappedException ex)
            {
                // super should already handle unwrap runtime exceptions
                final Throwable wrappedException = ex.getWrappedException();
                if (wrappedException instanceof RuntimeException)
                {
                    // super should have handled this
                    throw (RuntimeException) wrappedException;
                }
                throw new AlfrescoRuntimeException(wrappedException.getMessage(), wrappedException);
            }
            catch (final Throwable ex)
            {
                throw new AlfrescoRuntimeException(ex.getMessage(), ex);
            }
            finally
            {
                // cleanup execution context
                AuthenticationUtil.clearCurrentSecurityContext();
                AuthenticationUtil.popAuthentication();

                I18NUtil.setLocale(null);
                I18NUtil.setContentLocale(null);
            }
        }
        catch (final Throwable ex)
        {
            /*
             * The TxnCallback does not propagate non-retryable exceptions to the retrying transaction handler. Some exceptions may be
             * caused by execution of the provided script callback without passing through a service with its transaction interceptor which
             * would mark the transaction for rollback. We have to mark the transaction for rollback manually otherwise we end up with
             * commits of partial changes from the batch. (rollback on any exception is the default behaviour of Alfresco
             * SpringAwareUserTransaction)
             */

            final RuleBasedTransactionAttribute transactionAttribute = new RuleBasedTransactionAttribute();
            transactionAttribute.setReadOnly(true);
            transactionAttribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);

            // this never creates a new "real" transaction due to our propagation behavior
            final TransactionStatus transaction = this.txnManager.getTransaction(transactionAttribute);
            try
            {
                if (!transaction.isRollbackOnly())
                {
                    LOGGER.debug("Marking transaction as rollback-only due to exception during batch processing", ex);
                    transaction.setRollbackOnly();
                }
            }
            finally
            {
                // this never actually commits a "real" transaction - it just clears transaction synchronizations
                this.txnManager.commit(transaction);
            }

            throw ex;
        }
    }

}
