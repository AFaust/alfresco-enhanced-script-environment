/*
 * Copyright 2013 PRODYNA AG
 *
 * Licensed under the Eclipse Public License (EPL), Version 1.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/eclipse-1.0.php or
 * http://www.nabucco.org/License.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.adapters;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sf.cglib.core.ClassEmitter;
import net.sf.cglib.core.ClassNameReader;
import net.sf.cglib.core.CodeEmitter;
import net.sf.cglib.core.Constants;
import net.sf.cglib.core.MethodInfo;
import net.sf.cglib.core.ReflectUtils;
import net.sf.cglib.core.Signature;
import net.sf.cglib.core.TypeUtils;

import org.alfresco.scripts.ScriptException;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.NashornValueInstanceConverterRegistry.ValueConverter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ConstructorArgumentAwareProxyFactory;
import org.springframework.aop.framework.ProxyFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class SimpleRhinoSpecificBeanConverter extends AbstractValueInstanceConverter
{
    public abstract static class BaseDelegate
    {
        protected final Object delegate;

        public BaseDelegate(final Object delegate)
        {
            this.delegate = delegate;
        }
    }

    protected static class DelegatorEmitter extends ClassEmitter
    {
        private static final String FIELD_NAME = "delegate";

        private static final Signature CSTRUC = TypeUtils.parseConstructor(new Type[] { Constants.TYPE_OBJECT });

        protected DelegatorEmitter(final ClassVisitor v, final Type parent, final String className, final Class<?> delegateClass)
        {
            super(v);

            this.begin_class(Opcodes.V1_2, Opcodes.ACC_PUBLIC, className, parent, new Type[0], Constants.SOURCE_FILE);

            final CodeEmitter cstruct = this.begin_method(Opcodes.ACC_PUBLIC, CSTRUC, null);
            cstruct.load_this();
            cstruct.load_arg(0);
            cstruct.super_invoke_constructor(CSTRUC);
            cstruct.return_value();
            cstruct.end_method();

            final Method[] declaredMethods = delegateClass.getMethods();
            for (final Method method : declaredMethods)
            {
                if (!method.getDeclaringClass().equals(Object.class))
                {
                    final MethodInfo methodInfo = ReflectUtils.getMethodInfo(method);
                    final CodeEmitter methodEmitter = this.begin_method(Opcodes.ACC_PUBLIC, methodInfo.getSignature(),
                            methodInfo.getExceptionTypes());
                    methodEmitter.load_this();
                    methodEmitter.getfield(parent, FIELD_NAME, Constants.TYPE_OBJECT);
                    methodEmitter.checkcast(methodInfo.getClassInfo().getType());
                    methodEmitter.load_args();
                    methodEmitter.invoke(methodInfo);
                    methodEmitter.return_value();
                    methodEmitter.end_method();
                }
            }

            this.end_class();
        }
    }

    private final static Logger LOGGER = LoggerFactory.getLogger(SimpleRhinoSpecificBeanConverter.class);

    private final static Map<Class<?>, Class<?>> DELEGATE_CLASSES = new HashMap<Class<?>, Class<?>>();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForNashorn(final Object valueInstance, final ValueConverter globalDelegate)
    {
        final Object result;
        final Object realValue;

        if (valueInstance != null)
        {
            final ProxyFactory proxyFactory;
            if (Modifier.isFinal(valueInstance.getClass().getModifiers()))
            {
                // cannot subclass to proxy
                final Class<?> delegateClass = getOrGenerateDelegator(valueInstance.getClass());

                try
                {
                    final Constructor<?> cstruct = delegateClass.getConstructor(Object.class);
                    realValue = cstruct.newInstance(valueInstance);
                }
                catch (final Exception ex)
                {
                    LOGGER.error("Failed to instantiate delegator", ex);
                    throw new ScriptException("Failed to instantiate delegator", ex);
                }

                proxyFactory = new ConstructorArgumentAwareProxyFactory(new Object[] { valueInstance }, new Class[] { Object.class });
            }
            else
            {
                realValue = valueInstance;
                // any processor extension without no-argument-constructor will break this - but there should be none
                proxyFactory = new ConstructorArgumentAwareProxyFactory(new Object[0], new Class[0]);
            }

            proxyFactory.addAdvice(new RhinoSpecificBeanInterceptor(globalDelegate));
            proxyFactory.setInterfaces(collectInterfaces(valueInstance, Collections.<Class<?>> emptySet()));
            proxyFactory.setTarget(realValue);
            proxyFactory.setProxyTargetClass(true);

            result = proxyFactory.getProxy();
        }
        else
        {
            result = valueInstance;
        }

        return result;
    }

    protected synchronized static Class<?> getOrGenerateDelegator(final Class<?> originalClass)
    {
        final Class<?> delegatorClass = DELEGATE_CLASSES.computeIfAbsent(originalClass, k -> generateDelegator(k));

        return delegatorClass;
    }

    protected static Class<?> generateDelegator(final Class<?> originalClass)
    {
        final Class<?> gen;
        try
        {
            final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

            new DelegatorEmitter(cw, Type.getType(BaseDelegate.class), originalClass.getName() + "_Delegate", originalClass);

            final byte[] classBytes = cw.toByteArray();
            final String className = ClassNameReader.getClassName(new ClassReader(classBytes));
            gen = ReflectUtils.defineClass(className, classBytes, SimpleRhinoSpecificBeanConverter.class.getClassLoader());
        }
        catch (final Exception ex)
        {
            LOGGER.error("Failed to generate delegator class", ex);
            throw new ScriptException("Failed to generate delegator class", ex);
        }

        return gen;
    }
}
