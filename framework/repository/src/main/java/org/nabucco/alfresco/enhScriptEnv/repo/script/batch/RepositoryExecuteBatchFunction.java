/**
 *
 */
package org.nabucco.alfresco.enhScriptEnv.repo.script.batch;

import java.util.Collection;
import java.util.Collections;

import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.common.script.batch.AbstractExecuteBatchFunction;

/**
 * @author <a href="mailto:axel.faust@prodyna.com">Axel Faust</a>, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class RepositoryExecuteBatchFunction extends AbstractExecuteBatchFunction
{

    protected TransactionService transactionService;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "transactionService", this.transactionService);
        super.afterPropertiesSet();
    }

    /**
     * @param transactionService
     *            the transactionService to set
     */
    public final void setTransactionService(final TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeBatch(final Scriptable scope, final Scriptable thisObj, final Collection<Object> workItems,
            final Pair<Scriptable, Function> processCallback, final int batchSize, final Pair<Scriptable, Function> beforeProcessCallback,
            final Pair<Scriptable, Function> afterProcessCallback)
    {
        // TODO: parameter (and max value) for threadCount
        // TODO: parameter for logging interval
        // TODO: Log implementation that logs to the script logger from the provided scope

        final BatchProcessor<Object> batchProcessor = new BatchProcessor<Object>("ScriptBatch",
                this.transactionService.getRetryingTransactionHelper(), new BatchProcessWorkProvider<Object>()
                {
                    private boolean first = true;

                    /**
                     *
                     * {@inheritDoc}
                     */
                    @Override
                    public int getTotalEstimatedWorkSize()
                    {
                        return workItems.size();
                    }

                    /**
                     *
                     * {@inheritDoc}
                     */
                    @Override
                    public Collection<Object> getNextWork()
                    {
                        final Collection<Object> work = this.first ? workItems : Collections.emptySet();
                        this.first = false;
                        return work;
                    }
                }, 2, batchSize, null, LogFactory.getLog(RepositoryExecuteBatchFunction.class), 10);
        final RepositoryExecuteBatchWorker worker = new RepositoryExecuteBatchWorker(this, scope, thisObj, processCallback, beforeProcessCallback,
                afterProcessCallback);
        batchProcessor.process(worker, true);

        // TODO: result / status handling
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeBatch(final Scriptable scope, final Scriptable thisObj, final Pair<Scriptable, Function> workProviderCallback,
            final Pair<Scriptable, Function> processCallback, final int batchSize, final Pair<Scriptable, Function> beforeProcessCallback,
            final Pair<Scriptable, Function> afterProcessCallback)
    {
        // TODO: parameter (and max value) for threadCount
        // TODO: parameter for logging interval
        // TODO: Log implementation that logs to the script logger from the provided scope

        final BatchProcessor<Object> batchProcessor = new BatchProcessor<Object>("ScriptBatch",
                this.transactionService.getRetryingTransactionHelper(), new BatchProcessWorkProvider<Object>()
                {
                    private int fetched = -1;

                    /**
                     *
                     * {@inheritDoc}
                     */
                    @Override
                    public int getTotalEstimatedWorkSize()
                    {
                        return this.fetched;
                    }

                    /**
                     *
                     * {@inheritDoc}
                     */
                    @Override
                    public Collection<Object> getNextWork()
                    {
                        final Collection<Object> nextWork = RepositoryExecuteBatchFunction.this.doProvideNextWork(scope,
                                workProviderCallback);
                        if (this.fetched == -1)
                        {
                            this.fetched = nextWork.size();
                        }
                        else
                        {
                            this.fetched += nextWork.size();
                        }
                        return nextWork;
                    }
                }, 2, batchSize, null, LogFactory.getLog(RepositoryExecuteBatchFunction.class), 10);
        final RepositoryExecuteBatchWorker worker = new RepositoryExecuteBatchWorker(this, scope, thisObj, processCallback, beforeProcessCallback,
                afterProcessCallback);
        batchProcessor.process(worker, true);

        // TODO: result / status handling
    }

    @Override
    protected Collection<Object> doProvideNextWork(final Scriptable scope, final Pair<Scriptable, Function> workProviderCallback)
    {
        return super.doProvideNextWork(scope, workProviderCallback);
    }
}
