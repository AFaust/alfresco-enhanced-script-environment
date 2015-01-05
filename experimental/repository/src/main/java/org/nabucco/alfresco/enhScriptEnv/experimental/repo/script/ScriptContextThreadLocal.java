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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script;

import static jdk.nashorn.internal.runtime.ScriptRuntime.UNDEFINED;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.objects.Global;
import jdk.nashorn.internal.runtime.Context;
import jdk.nashorn.internal.runtime.Property;

import org.alfresco.scripts.ScriptException;
import org.alfresco.util.PropertyCheck;
import org.springframework.beans.factory.InitializingBean;

/**
 * A thread local to cache script context for simple, one-off script executions in support of the main script execution (e.g. value
 * conversion).
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class ScriptContextThreadLocal extends ThreadLocal<ScriptContext> implements InitializingBean
{

    private static final String NASHORN_ENGINE_NAME = "nashorn";

    protected ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(NASHORN_ENGINE_NAME);

    protected Context context;

    protected Global global;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "scriptEngine", this.scriptEngine);

        final ScriptContext globalCtxt = new SimpleScriptContext();
        globalCtxt.setBindings(new SimpleBindings(), ScriptContext.GLOBAL_SCOPE);

        final Bindings scope = this.scriptEngine.createBindings();
        globalCtxt.setBindings(scope, ScriptContext.ENGINE_SCOPE);

        final NashornEngineInspector inspector = new NashornEngineInspector();

        globalCtxt.setAttribute("inspector", inspector, ScriptContext.GLOBAL_SCOPE);
        this.scriptEngine.eval("inspector.inspect();", globalCtxt);

        this.context = inspector.getContext();
        this.global = inspector.getGlobal();

        // even internal scripts won't have access to these (or at least the way they are implemented by Nashorn)
        this.global.deleteOwnProperty(this.global.getProperty("exit"));
        this.global.deleteOwnProperty(this.global.getProperty("quit"));
        this.global.deleteOwnProperty(this.global.getProperty("load"));
        this.global.deleteOwnProperty(this.global.getProperty("loadWithNewGlobal"));
        this.global.deleteOwnProperty(this.global.getProperty("print"));
        this.global.deleteOwnProperty(this.global.getProperty("printf"));
        this.global.deleteOwnProperty(this.global.getProperty("sprintf"));

        // also deal with direct field access
        this.global.exit = this.global.undefined;
        this.global.quit = this.global.undefined;
        this.global.load = this.global.undefined;
        this.global.loadWithNewGlobal = this.global.undefined;
        this.global.print = this.global.undefined;
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
    protected ScriptContext initialValue()
    {
        final ScriptContext ctxt = new SimpleScriptContext();
        ctxt.setBindings(new SimpleBindings(), ScriptContext.GLOBAL_SCOPE);

        final Global global = new Global(this.context);
        try
        {
            // copy public fields (reflectively, so we adapt to changes between Java versions)
            final Field[] fields = Global.class.getFields();
            for (final Field field : fields)
            {
                if (!Modifier.isStatic(field.getModifiers()))
                {
                    field.setAccessible(true);
                    field.set(global, field.get(this.global));
                }
            }
        }
        catch (final IllegalAccessException ex)
        {
            throw new ScriptException("Failed to initialize scope", ex);
        }

        global.setProto(this.global);

        final int NON_ENUMERABLE_CONSTANT = Property.NOT_ENUMERABLE | Property.NOT_CONFIGURABLE | Property.NOT_WRITABLE;
        global.addOwnProperty("context", NON_ENUMERABLE_CONSTANT, null);
        global.addOwnProperty("arguments", Property.NOT_ENUMERABLE, UNDEFINED);
        global.addOwnProperty(ScriptEngine.FILENAME, Property.NOT_ENUMERABLE, null);

        final Bindings scope = (Bindings) ScriptObjectMirror.wrap(global, global);

        ctxt.setBindings(scope, ScriptContext.ENGINE_SCOPE);

        return ctxt;
    }
}
