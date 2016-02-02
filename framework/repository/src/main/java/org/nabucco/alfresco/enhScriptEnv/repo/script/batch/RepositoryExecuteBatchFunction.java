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
package org.nabucco.alfresco.enhScriptEnv.repo.script.batch;

import java.util.Collection;
import java.util.Collections;

import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Pair;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.nabucco.alfresco.enhScriptEnv.common.script.batch.AbstractExecuteBatchFunction;
import org.nabucco.alfresco.enhScriptEnv.common.util.ScriptLoggerLog;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Axel Faust
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

    protected PlatformTransactionManager transactionManager;

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
     * @param transactionManager
     *            the transactionManager to set
     */
    public final void setTransactionManager(final PlatformTransactionManager transactionManager)
    {
        this.transactionManager = transactionManager;
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

        final Log log;
        if (ScriptableObject.hasProperty(scope, "logger"))
        {
            final Object object = ScriptableObject.getProperty(scope, "logger");
            if (object instanceof Scriptable)
            {
                log = new ScriptLoggerLog((Scriptable) object, this.scriptProcessor);
            }
            else
            {
                log = LogFactory.getLog(RepositoryExecuteBatchFunction.class);
            }
        }
        else
        {
            log = LogFactory.getLog(RepositoryExecuteBatchFunction.class);
        }

        final BatchProcessor<Object> batchProcessor = new BatchProcessor<Object>("ScriptBatch",
                this.transactionService.getRetryingTransactionHelper(), new CollectionBatchWorkProvider(workItems), Math.min(threadCount,
                        this.maxThreads), batchSize, null, log, 10);
        final RepositoryExecuteBatchWorker worker = new RepositoryExecuteBatchWorker(this, scope, thisObj, processCallback,
                beforeProcessCallback, afterProcessCallback, this.transactionManager);
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

        final Log log;
        if (ScriptableObject.hasProperty(scope, "logger"))
        {
            final Object object = ScriptableObject.getProperty(scope, "logger");
            if (object instanceof Scriptable)
            {
                log = new ScriptLoggerLog((Scriptable) object, this.scriptProcessor);
            }
            else
            {
                log = LogFactory.getLog(RepositoryExecuteBatchFunction.class);
            }
        }
        else
        {
            log = LogFactory.getLog(RepositoryExecuteBatchFunction.class);
        }

        final BatchProcessor<Object> batchProcessor = new BatchProcessor<Object>("ScriptBatch",
                this.transactionService.getRetryingTransactionHelper(), new CallbackBatchProcessWorkProvider(this, scope,
                        workProviderCallback), Math.min(threadCount, this.maxThreads), batchSize, null, log, 10);
        final RepositoryExecuteBatchWorker worker = new RepositoryExecuteBatchWorker(this, scope, thisObj, processCallback,
                beforeProcessCallback, afterProcessCallback, this.transactionManager);
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
