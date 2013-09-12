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
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class RepositoryExecuteBatchFunction extends AbstractExecuteBatchFunction
{

    private static final int DEFAULT_MAX_THREADS = 2;

    protected static class CallbackBatchProcessWorkProvider implements BatchProcessWorkProvider<Object>
    {
        private final RepositoryExecuteBatchFunction batchFunction;
        private final Scriptable scope;
        private final Pair<Scriptable, Function> workProviderCallback;
        private int fetched = -1;

        protected CallbackBatchProcessWorkProvider(final RepositoryExecuteBatchFunction batchFunction, final Scriptable scope,
                final Pair<Scriptable, Function> workProviderCallback)
        {
            super();
            this.batchFunction = batchFunction;
            this.scope = scope;
            this.workProviderCallback = workProviderCallback;
        }

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
            final Collection<Object> nextWork = this.batchFunction.doProvideNextWork(this.scope, this.workProviderCallback);
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
    }

    protected static class CollectionBatchWorkProvider implements BatchProcessWorkProvider<Object>
    {
        private final Collection<Object> workItems;
        private boolean first = true;

        protected CollectionBatchWorkProvider(final Collection<Object> workItems)
        {
            super();
            this.workItems = workItems;
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public int getTotalEstimatedWorkSize()
        {
            return this.workItems.size();
        }

        /**
         *
         * {@inheritDoc}
         */
        @Override
        public Collection<Object> getNextWork()
        {
            final Collection<Object> work = this.first ? this.workItems : Collections.emptySet();
            this.first = false;
            return work;
        }
    }

    protected TransactionService transactionService;

    protected int maxThreads = DEFAULT_MAX_THREADS;

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
     * @param maxThreads
     *            the maxThreads to set
     */
    public final void setMaxThreads(final int maxThreads)
    {
        this.maxThreads = maxThreads;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeBatch(final Scriptable scope, final Scriptable thisObj, final Collection<Object> workItems,
            final Pair<Scriptable, Function> processCallback, final int threadCount, final int batchSize,
            final Pair<Scriptable, Function> beforeProcessCallback, final Pair<Scriptable, Function> afterProcessCallback)
    {
        // TODO: parameter for logging interval
        // TODO: Log implementation that logs to the script logger from the provided scope

        final BatchProcessor<Object> batchProcessor = new BatchProcessor<Object>("ScriptBatch",
                this.transactionService.getRetryingTransactionHelper(), new CollectionBatchWorkProvider(workItems), Math.min(threadCount,
                        this.maxThreads), batchSize, null, LogFactory.getLog(RepositoryExecuteBatchFunction.class), 10);
        final RepositoryExecuteBatchWorker worker = new RepositoryExecuteBatchWorker(this, scope, thisObj, processCallback,
				beforeProcessCallback, afterProcessCallback, this.facadeFactory);
        batchProcessor.process(worker, true);

        // TODO: result / status handling
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void executeBatch(final Scriptable scope, final Scriptable thisObj, final Pair<Scriptable, Function> workProviderCallback,
            final Pair<Scriptable, Function> processCallback, final int threadCount, final int batchSize,
            final Pair<Scriptable, Function> beforeProcessCallback, final Pair<Scriptable, Function> afterProcessCallback)
    {
        // TODO: parameter for logging interval
        // TODO: Log implementation that logs to the script logger from the provided scope

        final BatchProcessor<Object> batchProcessor = new BatchProcessor<Object>("ScriptBatch",
                this.transactionService.getRetryingTransactionHelper(), new CallbackBatchProcessWorkProvider(this, scope,
                        workProviderCallback), Math.min(threadCount, this.maxThreads), batchSize, null,
                LogFactory.getLog(RepositoryExecuteBatchFunction.class), 10);
        final RepositoryExecuteBatchWorker worker = new RepositoryExecuteBatchWorker(this, scope, thisObj, processCallback,
				beforeProcessCallback, afterProcessCallback, this.facadeFactory);
        batchProcessor.process(worker, true);

        // TODO: result / status handling
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected Collection<Object> doProvideNextWork(final Scriptable scope, final Pair<Scriptable, Function> workProviderCallback)
    {
        return super.doProvideNextWork(scope, workProviderCallback);
    }
}
