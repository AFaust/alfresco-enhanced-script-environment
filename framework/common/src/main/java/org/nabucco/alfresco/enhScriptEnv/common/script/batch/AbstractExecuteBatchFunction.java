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
package org.nabucco.alfresco.enhScriptEnv.common.script.batch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.alfresco.util.Pair;
import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.nabucco.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import org.nabucco.alfresco.enhScriptEnv.common.script.LogFunction;
import org.nabucco.alfresco.enhScriptEnv.common.script.ScopeContributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class AbstractExecuteBatchFunction implements IdFunctionCall, ScopeContributor, InitializingBean
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExecuteBatchFunction.class);

    public static final int EXECUTE_BATCH_FUNC_ID = 0;
    public static final Object EXECUTE_BATCH_FUNC_TAG = new Object();
    public static final String EXECUTE_BATCH_FUNC_NAME = "executeBatch";
    public static final int ARITY = 3;

    protected EnhancedScriptProcessor<?> scriptProcessor;
    protected List<ScriptValueToWorkItemCollectionConverter> converters = new ArrayList<ScriptValueToWorkItemCollectionConverter>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "scriptProcessor", this.scriptProcessor);
        PropertyCheck.mandatory(this, "converters", this.converters);

        this.scriptProcessor.registerScopeContributor(this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object execIdCall(final IdFunctionObject f, final Context cx, final Scriptable scope, final Scriptable thisObj,
            final Object[] args)
    {
        final NativeObject result = new NativeObject();
        if (f.hasTag(EXECUTE_BATCH_FUNC_TAG))
        {
            if (f.methodId() == EXECUTE_BATCH_FUNC_ID)
            {

                // mandatory parameters
                final Pair<Scriptable, Function> workProviderCallback = this.readCallbackArgument(cx, thisObj, args, 0);
                final Pair<Scriptable, Function> processCallback = this.readCallbackArgument(cx, thisObj, args, 1);
                final int batchSize = ScriptRuntime.toInt32(args, 2);

                // optional parameters
                final Pair<Scriptable, Function> beforeProcessCallback = this.readCallbackArgument(cx, thisObj, args, 3);
                final Pair<Scriptable, Function> afterProcessCallback = this.readCallbackArgument(cx, thisObj, args, 4);

                if (processCallback.getSecond() == null)
                {
                    LOGGER.error("Call to executeBatch did not provide work processing callback");
                    throw new IllegalArgumentException("No process callback has been provided");
                }

                // the first parameter can be of variable type - callback or collection of items
                if (workProviderCallback.getSecond() == null)
                {
                    // no work provider callback function, try array
                    final Object workValue = args[0];
                    if (workValue != null && workValue != Undefined.instance)
                    {
                        final Collection<Object> workItems = new ArrayList<Object>();

                        for (final ScriptValueToWorkItemCollectionConverter converter : this.converters)
                        {
                            if (converter.isSupported(workValue))
                            {
                                final Collection<?> convertedCollection = converter.convert(workValue);
                                if (convertedCollection != null)
                                {
                                    workItems.addAll(convertedCollection);
                                    break;
                                }
                            }
                        }

                        if (!workItems.isEmpty())
                        {
                            this.executeBatch(scope, thisObj, workItems, processCallback, batchSize, beforeProcessCallback,
                                    afterProcessCallback);
                        }
                        else
                        {
                            LOGGER.error("Call to executeBatch neither provided work provider nor work items tp process");
                            throw new IllegalArgumentException("No work has been provided");
                        }
                    }
                    else
                    {
                        LOGGER.error("Call to executeBatch neither provided work provider nor work items tp process");
                        throw new IllegalArgumentException("No work has been provided");
                    }
                }
                else
                {
                    this.executeBatch(scope, thisObj, workProviderCallback, processCallback, batchSize, beforeProcessCallback,
                            afterProcessCallback);
                }
            }
        }

        result.sealObject();

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contributeToScope(final Scriptable scope, final boolean trustworthyScript, final boolean mutableScope)
    {
        final IdFunctionObject func = new IdFunctionObject(this, EXECUTE_BATCH_FUNC_TAG, EXECUTE_BATCH_FUNC_ID, EXECUTE_BATCH_FUNC_NAME,
                ARITY, scope);
        func.sealObject();

        // export as read-only and undeleteable property of the scope
        ScriptableObject.defineProperty(scope, EXECUTE_BATCH_FUNC_NAME, func, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
    }

    /**
     * Sets the scriptProcessor to given scriptProcessor.
     *
     * @param scriptProcessor
     *            the scriptProcessor to set
     */
    public final void setScriptProcessor(final EnhancedScriptProcessor<?> scriptProcessor)
    {
        this.scriptProcessor = scriptProcessor;
    }

    /**
     * @param converters
     *            the converters to set
     */
    public final void setConverters(final List<ScriptValueToWorkItemCollectionConverter> converters)
    {
        this.converters = converters;
    }

    protected abstract void executeBatch(final Scriptable scope, final Scriptable thisObj, final Collection<Object> workItems,
            final Pair<Scriptable, Function> processCallback, final int batchSize, final Pair<Scriptable, Function> beforeProcessCallback,
            final Pair<Scriptable, Function> afterProcessCallback);

    protected abstract void executeBatch(final Scriptable scope, final Scriptable thisObj,
            final Pair<Scriptable, Function> workProviderCallback, final Pair<Scriptable, Function> processCallback, final int batchSize,
            final Pair<Scriptable, Function> beforeProcessCallback, final Pair<Scriptable, Function> afterProcessCallback);

    protected Collection<Object> doProvideNextWork(final Scriptable scope, final Pair<Scriptable, Function> workProviderCallback)
    {
        // this runs in the same thread / context as the caller, so we have an active Context
        final Context cx = Context.getCurrentContext();

        final Scriptable workProviderScope = workProviderCallback.getFirst();
        final Function workProviderFn = workProviderCallback.getSecond();

        final Object workValue = workProviderFn.call(cx, scope, workProviderScope, new Object[0]);

        final Collection<Object> workItems = new ArrayList<Object>();
        if (workValue != null && workValue != Undefined.instance)
        {
            boolean supported = false;
            for (final ScriptValueToWorkItemCollectionConverter converter : this.converters)
            {
                if (converter.isSupported(workValue))
                {
                    final Collection<?> convertedCollection = converter.convert(workValue);
                    if (convertedCollection != null)
                    {
                        workItems.addAll(convertedCollection);
                        supported = true;
                        break;
                    }
                }
            }

            if (!supported)
            {
                LOGGER.error("Work provider callback returned unsupported work item(s): {}", workValue);
                throw new RuntimeException("Work provider callback returned unsuppored work item(s)");
            }
        }
        else
        {
            LOGGER.error("Work provider callback returned 'null' or 'undefined' work item");
            throw new RuntimeException("Work provider callback returned 'null' or 'undefined' work item");
        }

        return workItems;
    }

    protected Scriptable doBeforeProcess(final Scriptable parentScope, final Scriptable thisObj,
            final Pair<Scriptable, Function> beforeProcessCallback)
    {
        final Context cx = Context.enter();
        try
        {
            final Scriptable processScope = cx.newObject(parentScope);
            processScope.setPrototype(parentScope);
            processScope.setParentScope(null);

            // TODO: scope delegate / interceptor for synchronization
            // TODO: update scope on Scopeable objects (managed in a ThreadLocal) - possibly via the same delegate / interceptor

            // check for registerChildScope function on logger and register process scope if function is available
            final Object loggerValue = ScriptableObject.getProperty(parentScope, LogFunction.LOGGER_OBJ_NAME);
            if (loggerValue instanceof Scriptable)
            {
                final Scriptable loggerObj = (Scriptable) loggerValue;
                final Object registerChildScopeFuncValue = ScriptableObject.getProperty(loggerObj,
                        LogFunction.REGISTER_CHILD_SCOPE_FUNC_NAME);
                if (registerChildScopeFuncValue instanceof Function)
                {
                    final Function registerChildScopeFunc = (Function) registerChildScopeFuncValue;
                    registerChildScopeFunc.call(cx, parentScope, thisObj, new Object[] { processScope });
                }
            }

            if (beforeProcessCallback.getSecond() != null)
            {
                final Scriptable beforeProcessCallScope = beforeProcessCallback.getFirst();
                final Function beforeProcessFn = beforeProcessCallback.getSecond();
                beforeProcessFn.call(cx, processScope, beforeProcessCallScope, new Object[0]);
            }

            return processScope;
        }
        finally
        {
            Context.exit();
        }
    }

    protected void doAfterProcess(final Scriptable processScope, final Scriptable thisObj,
            final Pair<Scriptable, Function> afterProcessCallback)
    {
        final Context cx = Context.enter();
        try
        {
            if (afterProcessCallback.getSecond() != null)
            {
                final Scriptable afterProcessCallScope = afterProcessCallback.getFirst();
                final Function afterProcessFn = afterProcessCallback.getSecond();
                afterProcessFn.call(cx, processScope, afterProcessCallScope, new Object[0]);
            }
        }
        finally
        {
            Context.exit();
        }
    }

    protected Pair<Scriptable, Function> readCallbackArgument(final Context cx, final Scriptable defaultScope, final Object[] arguments,
            final int idx)
    {
        final Scriptable param = ScriptRuntime.toObjectOrNull(cx, arguments.length > idx ? arguments[idx] : null);

        final Pair<Scriptable, Function> callback = new Pair<Scriptable, Function>(defaultScope, null);
        if (param instanceof Function)
        {
            callback.setSecond((Function) param);
        }
        else if (param != null)
        {
            final Object fnValue = ScriptableObject.getProperty(param, "fn");
            if (fnValue instanceof Function)
            {
                callback.setSecond((Function) fnValue);
            }
            else
            {
                LOGGER.debug("Value {} for fn is not a function", fnValue);
            }

            final Object scopeValue = ScriptableObject.getProperty(param, "scope");
            if (scopeValue instanceof Scriptable)
            {
                callback.setFirst((Scriptable) scopeValue);
            }
            else if (scopeValue != null)
            {
                LOGGER.debug("Value {} for scope is not an object", fnValue);
            }
        }

        return callback;
    }

}
