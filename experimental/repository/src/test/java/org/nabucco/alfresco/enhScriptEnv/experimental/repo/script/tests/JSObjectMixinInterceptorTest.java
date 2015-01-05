package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.tests;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.JSObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.GenericGlobalValueConverter;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.aop.JSObjectMixinInterceptor;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.nashorn.JOConverter;
import org.springframework.aop.framework.ProxyFactory;

/**
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class JSObjectMixinInterceptorTest
{

    protected static class BeanClass
    {
        // public fields for direct access
        public boolean propA1;
        public int propB1;
        public double propC1;
        public Object propD1;

        // public fields with accompanying setter/getter
        public boolean propA2;
        public boolean propA2SetterCalled;
        public boolean propA2GetterCalled;
        public int propB2;
        public boolean propB2SetterCalled;
        public boolean propB2GetterCalled;
        public double propC2;
        public boolean propC2SetterCalled;
        public boolean propC2GetterCalled;
        public Object propD2;
        public boolean propD2SetterCalled;
        public boolean propD2GetterCalled;

        // private fields with accompanying setter/getter
        private boolean propA3;
        private int propB3;
        private double propC3;
        private Object propD3;

        /**
         * @return the propA2
         */
        public boolean isPropA2()
        {
            this.propA2GetterCalled = true;
            return this.propA2;
        }

        /**
         * @param propA2
         *            the propA2 to set
         */
        public void setPropA2(final boolean propA2)
        {
            this.propA2 = propA2;
            this.propA2SetterCalled = true;
        }

        /**
         * @return the propB2
         */
        public int getPropB2()
        {
            this.propB2GetterCalled = true;
            return this.propB2;
        }

        /**
         * @param propB2
         *            the propB2 to set
         */
        public void setPropB2(final int propB2)
        {
            this.propB2 = propB2;
            this.propB2SetterCalled = true;
        }

        /**
         * @return the propC2
         */
        public double getPropC2()
        {
            this.propC2GetterCalled = true;
            return this.propC2;
        }

        /**
         * @param propC2
         *            the propC2 to set
         */
        public void setPropC2(final double propC2)
        {
            this.propC2 = propC2;
            this.propC2SetterCalled = true;
        }

        /**
         * @return the propD2
         */
        public Object getPropD2()
        {
            this.propD2GetterCalled = true;
            return this.propD2;
        }

        /**
         * @param propD2
         *            the propD2 to set
         */
        public void setPropD2(final Object propD2)
        {
            this.propD2 = propD2;
            this.propD2SetterCalled = true;
        }

        /**
         * @return the propA3
         */
        public boolean isPropA3()
        {
            return this.propA3;
        }

        /**
         * @param propA3
         *            the propA3 to set
         */
        public void setPropA3(final boolean propA3)
        {
            this.propA3 = propA3;
        }

        /**
         * @return the propB3
         */
        public int getPropB3()
        {
            return this.propB3;
        }

        /**
         * @param propB3
         *            the propB3 to set
         */
        public void setPropB3(final int propB3)
        {
            this.propB3 = propB3;
        }

        /**
         * @return the propC3
         */
        public double getPropC3()
        {
            return this.propC3;
        }

        /**
         * @param propC3
         *            the propC3 to set
         */
        public void setPropC3(final double propC3)
        {
            this.propC3 = propC3;
        }

        /**
         * @return the propD3
         */
        public Object getPropD3()
        {
            return this.propD3;
        }

        /**
         * @param propD3
         *            the propD3 to set
         */
        public void setPropD3(final Object propD3)
        {
            this.propD3 = propD3;
        }

    }

    private static ProxyFactory proxyFactory;
    private static ScriptEngine nashornEngine;

    @BeforeClass
    public static void setupClass() throws ScriptException
    {
        nashornEngine = new ScriptEngineManager().getEngineByName("nashorn");

        proxyFactory = new ProxyFactory();
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addInterface(JSObject.class);

        final GenericGlobalValueConverter valueConverter = new GenericGlobalValueConverter();

        final JOConverter joConverter = new JOConverter();
        joConverter.setRegistry(valueConverter);
        joConverter.afterPropertiesSet();

        final JSObjectMixinInterceptor jsObjectMixinInterceptor = new JSObjectMixinInterceptor(valueConverter, new Properties());

        proxyFactory.addAdvice(jsObjectMixinInterceptor);

        // ensure JSObjectMixingInterceptor has internally initialized the proxy class
        JSObjectMixinInterceptorTest.proxyFactory.setTarget(new BeanClass());
        final Bindings bindings = nashornEngine.createBindings();
        bindings.put("proxy", JSObjectMixinInterceptorTest.proxyFactory.getProxy());
        nashornEngine.eval("proxy.propA1 = false;", bindings);
    }

    private BeanClass bean;
    private BeanClass proxy;
    private Bindings bindings;

    @Before
    public void setupTest()
    {
        this.bean = new BeanClass();
        JSObjectMixinInterceptorTest.proxyFactory.setTarget(this.bean);
        this.proxy = (BeanClass) JSObjectMixinInterceptorTest.proxyFactory.getProxy();

        this.bindings = nashornEngine.createBindings();
        this.bindings.put("proxy", this.proxy);

        // populate values
        this.bean.propA1 = true;
        this.bean.propB1 = 42;
        this.bean.propC1 = 2.125;
        this.bean.propD1 = "This is a test-String";

        this.proxy.propA1 = true;
        this.proxy.propB1 = 42;
        this.proxy.propC1 = 2.125;
        this.proxy.propD1 = "This is a test-String";

        this.bean.propA2 = true;
        this.bean.propB2 = 42;
        this.bean.propC2 = 2.125;
        this.bean.propD2 = "This is a test-String";

        this.proxy.propA2 = true;
        this.proxy.propB2 = 42;
        this.proxy.propC2 = 2.125;
        this.proxy.propD2 = "This is a test-String";

        this.bean.setPropA3(true);
        this.bean.setPropB3(42);
        this.bean.setPropC3(2.125);
        this.bean.setPropD3("This is a test-String");
    }

    @Test
    public void propertyRead() throws ScriptException
    {
        nashornEngine.eval("var Assert = Java.type('org.junit.Assert');", this.bindings);

        // check direct public field access
        nashornEngine.eval("Assert.assertTrue(proxy.propA1);", this.bindings);
        nashornEngine.eval("Assert['assertEquals(long,long)'](42, proxy.propB1);", this.bindings);
        nashornEngine.eval("Assert.assertEquals(2.125, proxy.propC1, 0.001);", this.bindings);
        nashornEngine.eval("Assert.assertEquals('This is a test-String', proxy.propD1);", this.bindings);

        // check public field access via getter
        // #1 verify getter have not been called
        Assert.assertFalse(this.bean.propA2GetterCalled);
        Assert.assertFalse(this.bean.propB2GetterCalled);
        Assert.assertFalse(this.bean.propC2GetterCalled);
        Assert.assertFalse(this.bean.propD2GetterCalled);

        // #2 verify property values
        nashornEngine.eval("Assert.assertTrue(proxy.propA2);", this.bindings);
        nashornEngine.eval("Assert['assertEquals(long,long)'](42, proxy.propB2);", this.bindings);
        nashornEngine.eval("Assert.assertEquals(2.125, proxy.propC2, 0.001);", this.bindings);
        nashornEngine.eval("Assert.assertEquals('This is a test-String', proxy.propD2);", this.bindings);

        // #3 verify getter have been called
        Assert.assertTrue(this.bean.propA2GetterCalled);
        Assert.assertTrue(this.bean.propB2GetterCalled);
        Assert.assertTrue(this.bean.propC2GetterCalled);
        Assert.assertTrue(this.bean.propD2GetterCalled);

        // check private field access via getter
        nashornEngine.eval("Assert.assertTrue(proxy.propA3);", this.bindings);
        nashornEngine.eval("Assert['assertEquals(long,long)'](42, proxy.propB3);", this.bindings);
        nashornEngine.eval("Assert.assertEquals(2.125, proxy.propC3, 0.001);", this.bindings);
        nashornEngine.eval("Assert.assertEquals('This is a test-String', proxy.propD3);", this.bindings);
    }

    @Test
    public void propertyTypeCompatibleSimpleWrite() throws ScriptException
    {
        // simple write = write using assignment operator
        // type compatible = values picked so compiler analysis automatically selects correct Java type

        // check direct public field access
        nashornEngine.eval("proxy.propA1 = false;", this.bindings);
        nashornEngine.eval("proxy.propB1 = 4210;", this.bindings);
        nashornEngine.eval("proxy.propC1 = 42.125;", this.bindings);
        nashornEngine.eval("proxy.propD1 = 'This is another test-String';", this.bindings);

        Assert.assertFalse(this.bean.propA1);
        Assert.assertEquals(4210, this.bean.propB1);
        Assert.assertEquals(42.125, this.bean.propC1, 0.001);
        Assert.assertEquals("This is another test-String", this.bean.propD1);

        // We also check value of public field on proxy (consistency)
        Assert.assertFalse(this.proxy.propA1);
        Assert.assertEquals(4210, this.proxy.propB1);
        Assert.assertEquals(42.125, this.proxy.propC1, 0.001);
        Assert.assertEquals("This is another test-String", this.proxy.propD1);

        // check public field access via setter
        // #1 verify setter have not been called
        Assert.assertFalse(this.bean.propA2SetterCalled);
        Assert.assertFalse(this.bean.propB2SetterCalled);
        Assert.assertFalse(this.bean.propC2SetterCalled);
        Assert.assertFalse(this.bean.propD2SetterCalled);

        // #2 set via setter
        nashornEngine.eval("proxy.propA2 = false;", this.bindings);
        nashornEngine.eval("proxy.propB2 = 4210;", this.bindings);
        nashornEngine.eval("proxy.propC2 = 42.125;", this.bindings);
        nashornEngine.eval("proxy.propD2 = 'This is another test-String';", this.bindings);

        // #3 check values
        Assert.assertFalse(this.bean.propA2);
        Assert.assertEquals(4210, this.bean.propB2);
        Assert.assertEquals(42.125, this.bean.propC2, 0.001);
        Assert.assertEquals("This is another test-String", this.bean.propD2);

        // We also check value of public field on proxy (consistency)
        Assert.assertFalse(this.proxy.propA2);
        Assert.assertEquals(4210, this.proxy.propB2);
        Assert.assertEquals(42.125, this.proxy.propC2, 0.001);
        Assert.assertEquals("This is another test-String", this.proxy.propD2);

        // #4 verify setter have been called
        Assert.assertTrue(this.bean.propA2SetterCalled);
        Assert.assertTrue(this.bean.propB2SetterCalled);
        Assert.assertTrue(this.bean.propC2SetterCalled);
        Assert.assertTrue(this.bean.propD2SetterCalled);

        // check private field access via setter
        nashornEngine.eval("proxy.propA3 = false;", this.bindings);
        nashornEngine.eval("proxy.propB3 = 4210;", this.bindings);
        nashornEngine.eval("proxy.propC3 = 42.125;", this.bindings);
        nashornEngine.eval("proxy.propD3 = 'This is another test-String';", this.bindings);

        Assert.assertFalse(this.bean.isPropA3());
        Assert.assertEquals(4210, this.bean.getPropB3());
        Assert.assertEquals(42.125, this.bean.getPropC3(), 0.001);
        Assert.assertEquals("This is another test-String", this.bean.getPropD3());
    }

    @Test
    public void propertyTypeCompatibleSetterWrite() throws ScriptException
    {
        // setter write = write using explicit setter call
        // type compatible = values picked so compiler analysis automatically selects correct Java type

        // check public field access via explicit setter
        // #1 verify setter have not been called
        Assert.assertFalse(this.bean.propA2SetterCalled);
        Assert.assertFalse(this.bean.propB2SetterCalled);
        Assert.assertFalse(this.bean.propC2SetterCalled);
        Assert.assertFalse(this.bean.propD2SetterCalled);

        // #2 set via setter
        nashornEngine.eval("proxy.setPropA2(false);", this.bindings);
        nashornEngine.eval("proxy.setPropB2(4210);", this.bindings);
        nashornEngine.eval("proxy.setPropC2(42.125);", this.bindings);
        nashornEngine.eval("proxy.setPropD2('This is another test-String');", this.bindings);

        // #3 check values
        Assert.assertFalse(this.bean.propA2);
        Assert.assertEquals(4210, this.bean.propB2);
        Assert.assertEquals(42.125, this.bean.propC2, 0.001);
        Assert.assertEquals("This is another test-String", this.bean.propD2);

        // Direct setter invocation cirumvents JSObjectMixinInterceptor - verify proxy public fields are unchanged
        Assert.assertTrue(this.proxy.propA2);
        Assert.assertEquals(42, this.proxy.propB2);
        Assert.assertEquals(2.125, this.proxy.propC2, 0.001);
        Assert.assertEquals("This is a test-String", this.proxy.propD2);

        // #4 verify setter have been called
        Assert.assertTrue(this.bean.propA2SetterCalled);
        Assert.assertTrue(this.bean.propB2SetterCalled);
        Assert.assertTrue(this.bean.propC2SetterCalled);
        Assert.assertTrue(this.bean.propD2SetterCalled);

        // check private field access via setter
        nashornEngine.eval("proxy.setPropA3(false);", this.bindings);
        nashornEngine.eval("proxy.setPropB3(4210);", this.bindings);
        nashornEngine.eval("proxy.setPropC3(42.125);", this.bindings);
        nashornEngine.eval("proxy.setPropD3('This is another test-String');", this.bindings);

        Assert.assertFalse(this.bean.isPropA3());
        Assert.assertEquals(4210, this.bean.getPropB3());
        Assert.assertEquals(42.125, this.bean.getPropC3(), 0.001);
        Assert.assertEquals("This is another test-String", this.bean.getPropD3());
    }

    @Test
    public void mapReference() throws ScriptException
    {
        // Note: Conversions implicitly tested here courtesy of Nashorn compiler type analysis

        final Map<String, Object> map = new HashMap<String, Object>();

        this.bean.propD1 = map;
        this.bean.propD2 = map;
        this.bean.setPropD3(map);

        nashornEngine.eval("proxy.propD1.d1Str = 'Test';", this.bindings);
        nashornEngine.eval("proxy.propD1.d1Int = 42;", this.bindings);

        Assert.assertTrue(map.containsKey("d1Str"));
        Assert.assertEquals("Test", map.get("d1Str"));
        Assert.assertTrue(map.containsKey("d1Int"));
        Assert.assertEquals(Integer.valueOf(42), map.get("d1Int"));

        nashornEngine.eval("proxy.propD2.d2Str = 'Test';", this.bindings);
        nashornEngine.eval("proxy.propD2.d2Int = 42;", this.bindings);

        Assert.assertTrue(map.containsKey("d2Str"));
        Assert.assertEquals("Test", map.get("d2Str"));
        Assert.assertTrue(map.containsKey("d2Int"));
        Assert.assertEquals(Integer.valueOf(42), map.get("d2Int"));

        nashornEngine.eval("proxy.propD3.d3Str = 'Test';", this.bindings);
        nashornEngine.eval("proxy.propD3.d3Int = 42;", this.bindings);

        Assert.assertTrue(map.containsKey("d3Str"));
        Assert.assertEquals("Test", map.get("d3Str"));
        Assert.assertTrue(map.containsKey("d3Int"));
        Assert.assertEquals(Integer.valueOf(42), map.get("d3Int"));
    }

    @Test
    public void jsObjectConversion() throws ScriptException
    {
        // Note: Conversions implicitly tested here courtesy of JOConverter

        nashornEngine.eval("proxy.propD1 = { str: 'TestString', num: 42 };", this.bindings);

        Assert.assertTrue(this.bean.propD1 instanceof Map<?, ?>);
        Assert.assertTrue(((Map<?, ?>) this.bean.propD1).containsKey("str"));
        Assert.assertTrue(((Map<?, ?>) this.bean.propD1).containsKey("num"));
        Assert.assertEquals("TestString", ((Map<?, ?>) this.bean.propD1).get("str"));
        Assert.assertEquals(Integer.valueOf(42), ((Map<?, ?>) this.bean.propD1).get("num"));

        nashornEngine.eval("proxy.propD2 = { str: 'TestString', num: 42 };", this.bindings);

        Assert.assertTrue(this.bean.propD2 instanceof Map<?, ?>);
        Assert.assertTrue(((Map<?, ?>) this.bean.propD2).containsKey("str"));
        Assert.assertTrue(((Map<?, ?>) this.bean.propD2).containsKey("num"));
        Assert.assertEquals("TestString", ((Map<?, ?>) this.bean.propD2).get("str"));
        Assert.assertEquals(Integer.valueOf(42), ((Map<?, ?>) this.bean.propD2).get("num"));

        nashornEngine.eval("proxy.propD3 = { str: 'TestString', num: 42 };", this.bindings);

        Assert.assertTrue(this.bean.getPropD3() instanceof Map<?, ?>);
        Assert.assertTrue(((Map<?, ?>) this.bean.getPropD3()).containsKey("str"));
        Assert.assertTrue(((Map<?, ?>) this.bean.getPropD3()).containsKey("num"));
        Assert.assertEquals("TestString", ((Map<?, ?>) this.bean.getPropD3()).get("str"));
        Assert.assertEquals(Integer.valueOf(42), ((Map<?, ?>) this.bean.getPropD3()).get("num"));
    }
}
