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
package org.nabucco.alfresco.enhScriptEnv.experimental.common.linkers;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.junit.Assert;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.experimental.linker.ThreadLocalLinker;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class RhinoLinkerTest
{

    public Object propertyAccessImpl(final ScriptEngine nashornEngine, final ScriptContext nashornCtx, final Scriptable obj)
    {
        Object result;

        try
        {
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
        catch (final ScriptException se)
        {
            throw new RuntimeException(se);
        }

        return null;
    }

    @Test
    public void propertyAccess()
    {
        final Scriptable obj;
        final Context ctx = Context.enter();
        try
        {
            final Scriptable scope = ctx.initStandardObjects();
            obj = ctx.newObject(scope);

        }
        finally
        {
            Context.exit();
        }

        final String testStr = "Test-Value";
        obj.put("testProp", obj, testStr);
        obj.put(0, obj, testStr);
        obj.put(1, obj, testStr);

        final ScriptEngine nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");

        final ScriptContext nashornCtx = new SimpleScriptContext();
        nashornCtx.setBindings(nashornEngine.createBindings(), ScriptContext.ENGINE_SCOPE);
        nashornCtx.setAttribute("result", Boolean.FALSE, ScriptContext.ENGINE_SCOPE);

        nashornCtx.setBindings(new SimpleBindings(), ScriptContext.GLOBAL_SCOPE);
        nashornCtx.setAttribute("obj", obj, ScriptContext.GLOBAL_SCOPE);

        ThreadLocalLinker.withLinker(new RhinoLinker(), () -> this.propertyAccessImpl(nashornEngine, nashornCtx, obj));
    }

    public Object propertyModificationImpl(final ScriptEngine nashornEngine, final ScriptContext nashornCtx, final Scriptable obj)
    {
        try
        {
            nashornEngine.eval("obj.testProp1 = 'Test-Value1';", nashornCtx);
            Assert.assertEquals("Test-Value1", obj.get("testProp1", obj));

            nashornEngine.eval("obj['testProp2'] = 'Test-Value2';", nashornCtx);
            Assert.assertEquals("Test-Value2", obj.get("testProp2", obj));

            nashornEngine.eval("obj[0] = 'Test-Value3';", nashornCtx);
            Assert.assertEquals("Test-Value3", obj.get(0, obj));

            nashornEngine.eval("obj[2] = 'Test-Value4';", nashornCtx);
            Assert.assertEquals("Test-Value4", obj.get(2, obj));
        }
        catch (final ScriptException se)
        {
            throw new RuntimeException(se);
        }

        return null;
    }

    @Test
    public void propertyModification()
    {
        final Scriptable obj;
        final Context ctx = Context.enter();
        try
        {
            final Scriptable scope = ctx.initStandardObjects();
            obj = ctx.newObject(scope);
        }
        finally
        {
            Context.exit();
        }

        final ScriptEngine nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");

        final ScriptContext nashornCtx = new SimpleScriptContext();
        nashornCtx.setBindings(nashornEngine.createBindings(), ScriptContext.ENGINE_SCOPE);

        nashornCtx.setBindings(new SimpleBindings(), ScriptContext.GLOBAL_SCOPE);
        nashornCtx.setAttribute("obj", obj, ScriptContext.GLOBAL_SCOPE);

        ThreadLocalLinker.withLinker(new RhinoLinker(), () -> this.propertyModificationImpl(nashornEngine, nashornCtx, obj));
    }

    public Object arrayFunctionsImpl(final ScriptEngine nashornEngine, final ScriptContext nashornCtx, final Scriptable arr)
    {
        try
        {
            nashornEngine.eval("arr.push('Test-Value1');", nashornCtx);
            Assert.assertEquals(1, arr.getIds().length);
            Assert.assertEquals("Test-Value1", arr.get(0, arr));

            nashornEngine.eval("resultArr = arr.concat(['Test-Value2', 'Test-Value3']);", nashornCtx);
            final Object resultArr = nashornCtx.getAttribute("resultArr");

            Assert.assertTrue("Executing concat() on Rhino array should transparently create Nashorn array (represented by ScriptObjectMirror)", resultArr instanceof ScriptObjectMirror);
            Assert.assertTrue("Executing concat() on Rhino array should transparently create Nashorn array (represented by ScriptObjectMirror)", ((ScriptObjectMirror)resultArr).isArray());

            Assert.assertEquals(3, ((ScriptObjectMirror)resultArr).size());
            Assert.assertEquals("Test-Value1", ((ScriptObjectMirror)resultArr).get(0));
            Assert.assertEquals("Test-Value2", ((ScriptObjectMirror)resultArr).get(1));
            Assert.assertEquals("Test-Value3", ((ScriptObjectMirror)resultArr).get(2));
        }
        catch (final ScriptException se)
        {
            throw new RuntimeException(se);
        }

        return null;
    }

    @Test
    public void arrayFunctions()
    {
        Scriptable arr;
        final Context ctx = Context.enter();
        try
        {
            final Scriptable scope = ctx.initStandardObjects();
            arr = ctx.newArray(scope, 0);
        }
        finally
        {
            Context.exit();
        }

        final ScriptEngine nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");

        final ScriptContext nashornCtx = new SimpleScriptContext();
        nashornCtx.setBindings(nashornEngine.createBindings(), ScriptContext.ENGINE_SCOPE);

        nashornCtx.setBindings(new SimpleBindings(), ScriptContext.GLOBAL_SCOPE);
        nashornCtx.setAttribute("arr", arr, ScriptContext.GLOBAL_SCOPE);

        ThreadLocalLinker.withLinker(new RhinoLinker(), () -> this.arrayFunctionsImpl(nashornEngine, nashornCtx, arr));
    }
}
