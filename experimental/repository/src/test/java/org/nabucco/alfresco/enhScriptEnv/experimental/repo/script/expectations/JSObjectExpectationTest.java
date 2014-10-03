package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.expectations;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jdk.nashorn.api.scripting.JSObject;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class JSObjectExpectationTest
{

    private static class BaseClass
    {
        public String myMethod()
        {
            return "myMethod-result";
        }
    }

    private static class JSObjectClass extends BaseClass implements JSObject
    {

        @Override
        public Object call(final Object thiz, final Object... args)
        {
            throw new UnsupportedOperationException("call");
        }

        @Override
        public Object newObject(final Object... args)
        {
            throw new UnsupportedOperationException("newObject");
        }

        @Override
        public Object eval(final String s)
        {
            throw new UnsupportedOperationException("eval");
        }

        @Override
        public Object getMember(final String name)
        {
            throw new UnsupportedOperationException("getMember");
        }

        @Override
        public Object getSlot(final int index)
        {
            throw new UnsupportedOperationException("getSlot");
        }

        @Override
        public boolean hasMember(final String name)
        {
            throw new UnsupportedOperationException("hasMember");
        }

        @Override
        public boolean hasSlot(final int slot)
        {
            throw new UnsupportedOperationException("hasSlot");
        }

        @Override
        public void removeMember(final String name)
        {
            throw new UnsupportedOperationException("removeMember");
        }

        @Override
        public void setMember(final String name, final Object value)
        {
            throw new UnsupportedOperationException("setMember");
        }

        @Override
        public void setSlot(final int index, final Object value)
        {
            throw new UnsupportedOperationException("setSlot");
        }

        @Override
        public Set<String> keySet()
        {
            return Collections.emptySet();
        }

        @Override
        public Collection<Object> values()
        {
            return Collections.emptySet();
        }

        @Override
        public boolean isInstance(final Object instance)
        {
            return false;
        }

        @Override
        public boolean isInstanceOf(final Object clazz)
        {
            return false;
        }

        @Override
        public String getClassName()
        {
            return this.getClass().getName();
        }

        @Override
        public boolean isFunction()
        {
            return false;
        }

        @Override
        public boolean isStrictFunction()
        {
            return false;
        }

        @Override
        public boolean isArray()
        {
            return false;
        }

        @Override
        public double toNumber()
        {
            throw new UnsupportedOperationException("toNumber");
        }

    }

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Test
    public void functionResolution() throws Exception
    {
        final ScriptEngine nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");
        final Bindings bindings = nashornEngine.createBindings();
        bindings.put("myObj", new JSObjectClass());

        this.expectedException.expect(UnsupportedOperationException.class);

        final Object result = nashornEngine.eval("myObj.myMethod();", bindings);
        Assert.assertEquals("myMethod-result", result);
    }

}
