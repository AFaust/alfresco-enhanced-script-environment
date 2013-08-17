/**
 *
 */
package org.nabucco.alfresco.enhScriptEnv.repo.script.batch;

import org.alfresco.repo.batch.BatchProcessor.BatchProcessWorker;
import org.alfresco.util.Pair;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.nabucco.alfresco.enhScriptEnv.common.script.batch.AbstractExecuteBatchWorker;

/**
 * @author <a href="mailto:axel.faust@prodyna.com">Axel Faust</a>, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ExecuteBatchWorker extends AbstractExecuteBatchWorker<RepositoryExecuteBatchFunction> implements BatchProcessWorker<Object>
{

    protected ExecuteBatchWorker(final RepositoryExecuteBatchFunction batchFunction, final Scriptable parentScope,
            final Scriptable thisObj, final Pair<Scriptable, Function> processCallback,
            final Pair<Scriptable, Function> beforeProcessCallback, final Pair<Scriptable, Function> afterProcessCallback)
    {
        super(batchFunction, parentScope, thisObj, processCallback, beforeProcessCallback, afterProcessCallback);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getIdentifier(final Object entry)
    {
        final String identifier;

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
        super.doBeforeProcess();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void process(final Object entry) throws Throwable
    {
        super.doProcess(entry);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterProcess() throws Throwable
    {
        super.doAfterProcess();
    }

}
