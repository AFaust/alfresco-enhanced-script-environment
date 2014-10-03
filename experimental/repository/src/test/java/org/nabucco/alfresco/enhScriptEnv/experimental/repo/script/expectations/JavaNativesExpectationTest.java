package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.expectations;

import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class JavaNativesExpectationTest
{

    @Test
    public void passedNumberHandling() throws Exception
    {
        final ScriptEngine nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");
        final Bindings bindings = nashornEngine.createBindings();
        bindings.put("value", Double.valueOf(1));

        Object result = nashornEngine.eval("value === 1;", bindings);
        Assert.assertEquals(Boolean.TRUE, result);

        result = nashornEngine.eval("value = 2;", bindings);
        Assert.assertEquals(Integer.valueOf(2), result);

        result = nashornEngine.eval("value = 2.5;", bindings);
        Assert.assertEquals(Double.valueOf(2.5), result);

        bindings.put("value", Double.valueOf(3));
        result = nashornEngine.eval("value === 3;", bindings);
        Assert.assertEquals(Boolean.TRUE, result);

        result = nashornEngine.eval("value === 3.0;", bindings);
        Assert.assertEquals(Boolean.TRUE, result);

        bindings.put("valueA", Integer.valueOf(1));
        bindings.put("valueB", Double.valueOf(1));
        result = nashornEngine.eval("valueA === valueB;", bindings);
        Assert.assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void passedStringHandling() throws Exception
    {
        final ScriptEngine nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");
        final Bindings bindings = nashornEngine.createBindings();
        bindings.put("value", "test");

        Object result = nashornEngine.eval("value === 'test';", bindings);
        Assert.assertEquals(Boolean.TRUE, result);

        result = nashornEngine.eval("value.equals('test');", bindings);
        Assert.assertEquals(Boolean.TRUE, result);

        result = nashornEngine.eval("value.startsWith('te');", bindings);
        Assert.assertEquals(Boolean.TRUE, result);

        result = nashornEngine.eval("/test/.test(value);", bindings);
        Assert.assertEquals(Boolean.TRUE, result);

        result = nashornEngine.eval("(value + '') === 'test';", bindings);
        Assert.assertEquals(Boolean.TRUE, result);

        result = nashornEngine.eval("value.localeCompare('testa');", bindings);
        Assert.assertEquals(Double.valueOf(-1), result);

        bindings.put("value", "true");

        result = nashornEngine.eval("value === 'true';", bindings);
        Assert.assertEquals(Boolean.TRUE, result);

        result = nashornEngine.eval("value === true;", bindings);
        Assert.assertEquals(Boolean.FALSE, result);

        result = nashornEngine.eval("value == true;", bindings);
        Assert.assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void retrievedStringHandling() throws Exception
    {
        final ScriptEngine nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");
        final Bindings bindings = nashornEngine.createBindings();

        final Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        bindings.put("map", map);

        Object result = nashornEngine.eval("map.key;", bindings);
        Assert.assertEquals("value", result);

        result = nashornEngine.eval("map.key === 'value';", bindings);
        Assert.assertEquals(Boolean.TRUE, result);

        result = nashornEngine.eval("map['key'];", bindings);
        Assert.assertEquals("value", result);

        result = nashornEngine.eval("map['key'] === 'value';", bindings);
        Assert.assertEquals(Boolean.TRUE, result);

        result = nashornEngine.eval("map.keySet().iterator().next();", bindings);
        Assert.assertEquals("key", result);

        result = nashornEngine.eval("map['key2'] = 'value2';", bindings);
        result = nashornEngine.eval("map.key3 = 'value3';", bindings);
        Assert.assertEquals("value2", map.get("key2"));
        Assert.assertEquals("value3", map.get("key3"));
    }
}
