package org.nabucco.alfresco.enhScriptEnv.common.script.batch;

import org.alfresco.util.Pair;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeFunction;
import org.mozilla.javascript.Scriptable;

public abstract class AbstractExecuteBatchWorker<EBF extends AbstractExecuteBatchFunction>
{
	protected final EBF batchFunction;

	protected final Scriptable parentScope;
	protected final Scriptable thisObj;
	protected final Pair<Scriptable, Function> processCallback;
	protected final Pair<Scriptable, Function> beforeProcessCallback;
	protected final Pair<Scriptable, Function> afterProcessCallback;

	protected final ObjectFacadeFactory facadeFactory;

	protected final ThreadLocal<Scriptable> processScope = new ThreadLocal<Scriptable>();

	protected AbstractExecuteBatchWorker(final EBF batchFunction, final Scriptable parentScope,
			final Scriptable thisObj,
			final Pair<Scriptable, Function> processCallback, final Pair<Scriptable, Function> beforeProcessCallback,
			final Pair<Scriptable, Function> afterProcessCallback, final ObjectFacadeFactory facadeFactory)
	{
		this.batchFunction = batchFunction;
		this.processCallback = processCallback;
		this.beforeProcessCallback = beforeProcessCallback;
		this.afterProcessCallback = afterProcessCallback;

		this.facadeFactory = facadeFactory;

		this.parentScope = parentScope;
		this.thisObj = thisObj;
	}

	protected void doBeforeProcess()
	{
		final Scriptable processScope = this.batchFunction.doBeforeProcess(this.parentScope, this.thisObj,
				this.beforeProcessCallback);
		this.processScope.set(processScope);
	}

	protected void doAfterProcess()
	{
		this.batchFunction.doAfterProcess(this.processScope.get(), this.thisObj, this.afterProcessCallback);
	}

	protected void doProcess(final Object element)
	{
		final Context cx = Context.enter();
		try
		{
			final Scriptable processOriginalCallScope = this.processCallback.getFirst();
			final Scriptable processCallScope = processOriginalCallScope == this.parentScope ? this.processScope.get()
					: this.facadeFactory.toFacadedObject(this.parentScope, processOriginalCallScope, null);
			final Function processFn = this.processCallback.getSecond();

			if (processFn instanceof NativeFunction)
			{
				// native function has parent scope based on location in source code
				// per batch function contract we need to execute it in our process scope
				final NativeFunction nativeFn = (NativeFunction) processFn;

				final ThreadLocalParentScope threadLocalParentScope = (ThreadLocalParentScope) nativeFn
						.getParentScope();
				threadLocalParentScope.setDelegate(this.processScope.get());
				try
				{
					// execute with thread local parent scope
					nativeFn.call(cx, this.processScope.get(), processCallScope, new Object[] { element });
				} finally
				{
					threadLocalParentScope.removeDelegate();
				}
			}
			else
			{
				// not a native function, so has not associated scope - calling as-is
				processFn.call(cx, this.processScope.get(), processCallScope, new Object[] { element });
			}
		} finally
		{
			Context.exit();
		}
	}
}