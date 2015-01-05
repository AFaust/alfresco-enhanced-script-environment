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
package org.nabucco.alfresco.enhScriptEnv.experimental.linker;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class RhinoLinkerTest
{

    @Test
    public void propertyAccess() throws Exception
    {
        final Context ctx = Context.enter();
        try
        {
            final Scriptable scope = ctx.initStandardObjects();
            final Scriptable obj = ctx.newObject(scope);

            final String testStr = "Test-Value";
            scope.put("testProp", obj, testStr);
            scope.put(0, obj, testStr);
            scope.put(1, obj, testStr);

            final ScriptEngine nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");

            final ScriptContext nashornCtx = new SimpleScriptContext();
            nashornCtx.setBindings(nashornEngine.createBindings(), ScriptContext.ENGINE_SCOPE);
            nashornCtx.setAttribute("result", Boolean.FALSE, ScriptContext.ENGINE_SCOPE);

            nashornCtx.setBindings(new SimpleBindings(), ScriptContext.GLOBAL_SCOPE);
            nashornCtx.setAttribute("obj", obj, ScriptContext.GLOBAL_SCOPE);

            Object result;

            nashornCtx.setAttribute("result", Boolean.FALSE, ScriptContext.ENGINE_SCOPE);
            nashornEngine.eval("result = obj.testProp === 'Test-Value';", nashornCtx);
            result = nashornCtx.getAttribute("result");
            Assert.assertEquals(Boolean.TRUE, result);

            nashornCtx.setAttribute("result", Boolean.FALSE, ScriptContext.ENGINE_SCOPE);
            nashornEngine.eval("result = obj['testProp'] === 'Test-Value';", nashornCtx);
            result = nashornCtx.getAttribute("result");
            Assert.assertEquals(Boolean.TRUE, result);

            nashornCtx.setAttribute("result", Boolean.FALSE, ScriptContext.ENGINE_SCOPE);
            nashornEngine.eval("result = obj[0] === 'Test-Value';", nashornCtx);
            result = nashornCtx.getAttribute("result");
            Assert.assertEquals(Boolean.TRUE, result);

            nashornCtx.setAttribute("result", Boolean.FALSE, ScriptContext.ENGINE_SCOPE);
            nashornEngine.eval("result = obj[1] === 'Test-Value';", nashornCtx);
            result = nashornCtx.getAttribute("result");
            Assert.assertEquals(Boolean.TRUE, result);
        }
        finally
        {
            Context.exit();
        }
    }

    @Test
    public void propertyModification() throws Exception
    {
        final Context ctx = Context.enter();
        try
        {
            final Scriptable scope = ctx.initStandardObjects();
            final Scriptable obj = ctx.newObject(scope);

            final ScriptEngine nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");

            final ScriptContext nashornCtx = new SimpleScriptContext();
            nashornCtx.setBindings(nashornEngine.createBindings(), ScriptContext.ENGINE_SCOPE);

            nashornCtx.setBindings(new SimpleBindings(), ScriptContext.GLOBAL_SCOPE);
            nashornCtx.setAttribute("obj", obj, ScriptContext.GLOBAL_SCOPE);

            nashornEngine.eval("obj.testProp1 = 'Test-Value1';", nashornCtx);
            Assert.assertEquals("Test-Value1", obj.get("testProp1", obj));

            nashornEngine.eval("obj['testProp2'] = 'Test-Value2';", nashornCtx);
            Assert.assertEquals("Test-Value2", obj.get("testProp2", obj));

            nashornEngine.eval("obj[0] = 'Test-Value3';", nashornCtx);
            Assert.assertEquals("Test-Value3", obj.get(0, obj));

            nashornEngine.eval("obj[2] = 'Test-Value4';", nashornCtx);
            Assert.assertEquals("Test-Value4", obj.get(2, obj));
        }
        finally
        {
            Context.exit();
        }
    }
}
