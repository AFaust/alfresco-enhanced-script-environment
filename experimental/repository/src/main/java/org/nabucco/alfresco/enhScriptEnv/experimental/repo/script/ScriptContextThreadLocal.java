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

import java.util.Arrays;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import jdk.nashorn.internal.objects.Global;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "scriptEngine", this.scriptEngine);
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

        final Bindings scope = this.scriptEngine.createBindings();
        ctxt.setBindings(scope, ScriptContext.ENGINE_SCOPE);

        try
        {
            final NashornEngineInspector inspector = new NashornEngineInspector();
            ctxt.setAttribute("inspector", inspector, ScriptContext.GLOBAL_SCOPE);
            this.scriptEngine.eval("inspector.inspect();", ctxt);
            ctxt.removeAttribute("inspector", ScriptContext.GLOBAL_SCOPE);

            // even secure scripts / contributors won't have access to these (or at least the way they are implemented by Nashorn)
            final Global global = inspector.getGlobal();
            deleteGlobalProperty(global, "exit");
            deleteGlobalProperty(global, "quit");
            deleteGlobalProperty(global, "load");
            deleteGlobalProperty(global, "loadWithNewGlobal");
            deleteGlobalProperty(global, "print");
            deleteGlobalProperty(global, "printf");
            deleteGlobalProperty(global, "sprintf");

            // also deal with direct field access
            global.exit = global.undefined;
            global.quit = global.undefined;
            global.load = global.undefined;
            global.loadWithNewGlobal = global.undefined;
            global.print = global.undefined;
        }
        catch (final javax.script.ScriptException ex)
        {
            throw new ScriptException("Failed to initialize shared script context for current thread");
        }

        return ctxt;
    }

    protected static void deleteGlobalProperty(final Global global, final String property)
    {
        if (Arrays.binarySearch(global.getOwnKeys(true), property) >= 0)
        {
            final Property propertyDesc = global.getProperty(property);
            if (propertyDesc != null)
            {
                global.deleteOwnProperty(propertyDesc);
            }
        }
    }
}
