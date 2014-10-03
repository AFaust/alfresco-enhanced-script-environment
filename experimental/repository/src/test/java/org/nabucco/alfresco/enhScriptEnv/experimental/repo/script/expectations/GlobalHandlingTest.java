package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.expectations;

import static jdk.nashorn.internal.runtime.ScriptRuntime.UNDEFINED;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.objects.Global;
import jdk.nashorn.internal.runtime.Context;
import jdk.nashorn.internal.runtime.Property;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class GlobalHandlingTest
{

    public static class EngineContextInterceptor
    {

        private Context context;

        private Global global;

        public void intercept()
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

    @Test
    public void globalReplacement() throws Exception
    {
        final ScriptEngine nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");

        final Bindings defaultBindings = nashornEngine.createBindings();
        final EngineContextInterceptor interceptor = new EngineContextInterceptor();
        defaultBindings.put("interceptor", interceptor);

        nashornEngine.eval("interceptor.intercept();", defaultBindings);

        final Global customGlobal = new Global(interceptor.getContext());
        copyPublicFields(interceptor.getGlobal(), customGlobal);
        customGlobal.setProto(interceptor.getGlobal());

        final int NON_ENUMERABLE_CONSTANT = Property.NOT_ENUMERABLE | Property.NOT_CONFIGURABLE | Property.NOT_WRITABLE;
        customGlobal.addOwnProperty("context", NON_ENUMERABLE_CONSTANT, null);
        customGlobal.addOwnProperty("arguments", Property.NOT_ENUMERABLE, UNDEFINED);
        customGlobal.addOwnProperty(ScriptEngine.FILENAME, Property.NOT_ENUMERABLE, null);

        final Bindings customBindings = (ScriptObjectMirror) ScriptObjectMirror.wrap(customGlobal, customGlobal);

        Object result = nashornEngine.eval("(function(){var list = new (Java.type('java.util.LinkedList'))(); return list;})();",
                customBindings);

        Assert.assertTrue(result != null);
        Assert.assertTrue(result instanceof LinkedList<?>);

        nashornEngine.eval("obj = {}", customBindings);

        Assert.assertTrue(customBindings.containsKey("obj"));
        Assert.assertFalse(defaultBindings.containsKey("obj"));

        nashornEngine.eval("testStr = 'Test-String';", defaultBindings);

        Assert.assertTrue(defaultBindings.containsKey("testStr"));
        Assert.assertTrue(customBindings.containsKey("testStr"));

        result = nashornEngine.eval("testStr === 'Test-String';", customBindings);
        Assert.assertTrue(Boolean.TRUE.equals(result));
    }

    protected static void copyPublicFields(final Global parent, final Global child) throws Exception
    {
        final Field[] fields = Global.class.getFields();
        for (final Field field : fields)
        {
            if (!Modifier.isStatic(field.getModifiers()))
            {
                field.setAccessible(true);
                field.set(child, field.get(parent));
            }
        }
    }
}
