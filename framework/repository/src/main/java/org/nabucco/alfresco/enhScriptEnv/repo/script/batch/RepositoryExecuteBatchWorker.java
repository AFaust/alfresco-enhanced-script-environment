/**
 *
 */
package org.nabucco.alfresco.enhScriptEnv.repo.script.batch;

import java.util.Locale;

import org.alfresco.repo.batch.BatchProcessor.BatchProcessWorker;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.util.Pair;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.nabucco.alfresco.enhScriptEnv.common.script.batch.AbstractExecuteBatchWorker;
import org.nabucco.alfresco.enhScriptEnv.common.script.batch.ObjectFacadeFactory;
import org.springframework.extensions.surf.util.I18NUtil;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class RepositoryExecuteBatchWorker extends AbstractExecuteBatchWorker<RepositoryExecuteBatchFunction> implements
		BatchProcessWorker<Object>
{

	protected final String user = AuthenticationUtil.getFullyAuthenticatedUser();
	protected final Locale locale = I18NUtil.getLocale();
	protected final Locale contentLocale = I18NUtil.getContentLocaleOrNull();

	protected RepositoryExecuteBatchWorker(final RepositoryExecuteBatchFunction batchFunction,
			final Scriptable parentScope,
			final Scriptable thisObj, final Pair<Scriptable, Function> processCallback,
			final Pair<Scriptable, Function> beforeProcessCallback,
			final Pair<Scriptable, Function> afterProcessCallback, final ObjectFacadeFactory facadeFactory)
	{
		super(batchFunction, parentScope, thisObj, processCallback, beforeProcessCallback, afterProcessCallback,
				facadeFactory);
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
					final Object toStringResult = ((Function) toString).call(cx, (Scriptable) entry,
							(Scriptable) entry, new Object[0]);
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
			} finally
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
		AuthenticationUtil.setFullyAuthenticatedUser(this.user);

		I18NUtil.setLocale(this.locale);
		if (this.contentLocale != null)
		{
			I18NUtil.setContentLocale(this.contentLocale);
		}

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

		// cleanup execution context
		AuthenticationUtil.clearCurrentSecurityContext();
		AuthenticationUtil.popAuthentication();

		I18NUtil.setLocale(null);
		I18NUtil.setContentLocale(null);
	}

}
