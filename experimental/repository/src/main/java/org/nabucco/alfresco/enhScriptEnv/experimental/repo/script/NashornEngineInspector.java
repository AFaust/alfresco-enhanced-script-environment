package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script;

import jdk.nashorn.internal.objects.Global;
import jdk.nashorn.internal.runtime.Context;

/**
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class NashornEngineInspector
{
    protected Context context;

    protected Global global;

    public void inspect()
    {
        this.context = Context.getContext();
        this.global = Global.instance();
    }

    public Context getContext()
    {
        return this.context;
    }

    public Global getGlobal()
    {
        return this.global;
    }
}