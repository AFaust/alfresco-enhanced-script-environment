package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.functions;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import jdk.nashorn.api.scripting.AbstractJSObject;
import jdk.nashorn.internal.objects.NativeJava;
import jdk.nashorn.internal.runtime.ScriptRuntime;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.scripts.ScriptException;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.common.util.ClassUtils;
import org.springframework.asm.Type;

/**
 * JavaScript object implementation encapsulating a (potentially overloaded) Java method
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
// need to work with internal API to implement features and convert objects
public class JavaMethodJSObject extends AbstractJSObject
{

    protected static final String SIGNATURE_PATTERN_EXPRESSION = "\\((\\[*(?:[VZCBSIFJD]|void|boolean|char|byte|short|int|float|long|double|L[^;]+;))*\\)(\\[*(?:[VZCBSIFJD]|void|boolean|char|byte|short|int|float|long|double|L[^;]+;))";

    protected static class Signature
    {

        private final Method method;

        private final Class<?> returnType;

        private final Class<?>[] parameterTypes;

        protected static Collection<Signature> mapToSignature(final Method method, final Properties signatureMappings)
                throws ClassNotFoundException
        {
            final String methodKey = MessageFormat.format("{0}.{1}{2}", method.getDeclaringClass().getCanonicalName(), method.getName(),
                    Type.getMethodDescriptor(method));
            final String mappedDescriptor = signatureMappings.getProperty(methodKey);

            final String[] descriptors;
            final Collection<Signature> signatures = new ArrayList<Signature>();
            if (mappedDescriptor != null)
            {
                if (mappedDescriptor.indexOf('|') == -1)
                {
                    descriptors = new String[] { mappedDescriptor };
                }
                else
                {
                    descriptors = mappedDescriptor.split("\\|");
                }

                for (final String descriptor : descriptors)
                {
                    if (descriptor.matches(SIGNATURE_PATTERN_EXPRESSION))
                    {
                        final Class<?> returnType = org.springframework.util.ClassUtils.forName(Type.getReturnType(descriptor)
                                .getClassName());

                        final Type[] argumentTypes = Type.getArgumentTypes(descriptor);

                        final Class<?>[] parameterTypes = (Class<?>[]) Array.newInstance(Class.class, argumentTypes.length);
                        for (int idx = 0, max = argumentTypes.length; idx < max; idx++)
                        {
                            parameterTypes[idx] = org.springframework.util.ClassUtils.forName(argumentTypes[idx].getClassName());
                        }

                        signatures.add(new Signature(method, returnType, parameterTypes));
                    }
                }
            }
            else
            {
                Class<?> returnType = method.getReturnType();

                // to avoid forcing signature mapping for all operations, this is / these are default return type mapping(s)
                if (Scriptable.class.equals(returnType))
                {
                   // allows conversion into (potential) Nashorn natives this way
                   returnType = Object.class;
                }

                signatures.add(new Signature(method, returnType, method.getParameterTypes()));
            }

            return signatures;
        }

        private Signature(final Method method, final Class<?> returnType, final Class<?>[] parameterTypes)
        {
            this.method = method;

            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
        }

        /**
         * Gets the method for JavaMethodJSObject.Signature.
         *
         * @return method
         */
        public Method getMethod()
        {
            return this.method;
        }

        /**
         * Gets the returnType for JavaMethodJSObject.Signature.
         *
         * @return returnType
         */
        public Class<?> getReturnType()
        {
            return this.returnType;
        }

        /**
         * Gets the parameterTypes for JavaMethodJSObject.Signature.
         *
         * @return parameterTypes
         */
        public Class<?>[] getParameterTypes()
        {
            return this.parameterTypes;
        }

        protected boolean matches(final ValueConverter valueConverter, final Object... args)
        {
            boolean matches;

            if (args.length >= this.parameterTypes.length)
            {
                matches = true;
            }
            else if (args.length == this.parameterTypes.length - 1)
            {
                matches = this.parameterTypes[this.parameterTypes.length - 1].isArray();
            }
            else
            {
                matches = false;
            }

            if (matches && this.parameterTypes.length == 0)
            {
                matches = args.length == 0;
            }
            else
            {
                for (int idx = 0; matches && idx < args.length; idx++)
                {
                    if (idx >= this.parameterTypes.length && this.parameterTypes[this.parameterTypes.length - 1].isArray())
                    {
                        matches = args[idx] == null
                                || ClassUtils.isInstance(args[idx], this.parameterTypes[this.parameterTypes.length - 1].getComponentType())
                                || valueConverter.canConvertValueForJava(args[idx],
                                        this.parameterTypes[this.parameterTypes.length - 1].getComponentType());
                    }
                    else if (idx < this.parameterTypes.length)
                    {
                        if (idx - 1 == this.parameterTypes.length && args.length >= this.parameterTypes.length
                                && this.parameterTypes[this.parameterTypes.length - 1].isArray())
                        {
                            matches = args[idx] == null
                                    || (args.length == this.parameterTypes.length && (ClassUtils.isInstance(args[idx],
                                            this.parameterTypes[idx]) || valueConverter.canConvertValueForJava(args[idx],
                                            this.parameterTypes[idx])))
                                    || (args.length > this.parameterTypes.length && (ClassUtils.isInstance(args[idx],
                                            this.parameterTypes[idx].getComponentType()) || valueConverter.canConvertValueForJava(
                                            args[idx], this.parameterTypes[idx].getComponentType())));
                        }
                        else
                        {
                            matches = args[idx] == null || ClassUtils.isInstance(args[idx], this.parameterTypes[idx])
                                    || valueConverter.canConvertValueForJava(args[idx], this.parameterTypes[idx]);
                        }
                    }
                    else
                    {
                        matches = false;
                    }
                }
            }

            return matches;
        }

        protected Object[] toCallArgs(final Object[] args, final ValueConverter valueConverter)
        {
            final Object[] callArgs = new Object[this.parameterTypes.length];

            if (args.length == this.parameterTypes.length - 1 && this.parameterTypes[this.parameterTypes.length - 1].isArray())
            {
                callArgs[callArgs.length - 1] = new Object[0];
            }

            for (int idx = 0; idx < args.length; idx++)
            {
                if (idx >= this.parameterTypes.length && this.parameterTypes[this.parameterTypes.length - 1].isArray()
                        && this.parameterTypes[this.parameterTypes.length - 1].isArray())
                {
                    ((Object[]) callArgs[callArgs.length - 1])[idx - callArgs.length + 1] = args[idx];
                }
                else if (idx < this.parameterTypes.length)
                {
                    if (idx - 1 == this.parameterTypes.length && args.length >= this.parameterTypes.length
                            && this.parameterTypes[this.parameterTypes.length - 1].isArray())
                    {
                        if (args.length == this.parameterTypes.length)
                        {
                            if (ClassUtils.isInstance(args[idx], this.parameterTypes[idx])
                                    || valueConverter.canConvertValueForJava(args[idx], this.parameterTypes[idx]))
                            {
                                callArgs[idx] = args[idx];
                            }
                            else
                            {
                                callArgs[idx] = new Object[] { args[idx] };
                            }
                        }
                        else
                        {
                            callArgs[callArgs.length - 1] = new Object[args.length - callArgs.length + 1];
                            ((Object[]) callArgs[callArgs.length - 1])[idx - callArgs.length + 1] = args[idx];
                        }
                    }
                    else
                    {
                        callArgs[idx] = args[idx];
                    }
                }
            }

            for (int idx = 0; idx < callArgs.length; idx++)
            {
                callArgs[idx] = valueConverter.convertValueForJava(callArgs[idx], this.parameterTypes[idx]);
            }

            return callArgs;
        }

        protected Object invoke(final Object thiz, final Object[] args, final ValueConverter valueConverter)
                throws InvocationTargetException, IllegalAccessException
        {
            final Object[] callArgs = this.toCallArgs(args, valueConverter);

            final Object invocationResult = this.method.invoke(thiz, callArgs);

            // candidate result based on actual API
            final Object result;
            if (!(void.class.equals(this.returnType) || Void.class.equals(this.returnType)))
            {
                result = valueConverter.convertValueForScript(invocationResult, this.returnType);
            }
            else
            {
                result = invocationResult;
            }

            return result;
        }

        protected boolean isMoreSpecificThan(final Signature signature)
        {
            final boolean isMoreSpecific;

            if (!signature.getMethod().getDeclaringClass().equals(this.method.getDeclaringClass())
                    && signature.getMethod().getDeclaringClass().isAssignableFrom(this.method.getDeclaringClass()))
            {
                isMoreSpecific = true;
            }
            else
            {
                final Class<?>[] otherParamTypes = signature.getParameterTypes();

                if (otherParamTypes[otherParamTypes.length - 1].isArray() && this.parameterTypes.length >= otherParamTypes.length - 1)
                {
                    isMoreSpecific = true;
                }
                else
                {
                    // TODO check parameter types
                    isMoreSpecific = false;
                }
            }

            return isMoreSpecific;
        }
    }

    private final String name;

    private final Collection<Signature> signatures = new ArrayList<Signature>();

    private final ValueConverter valueConverter;

    public JavaMethodJSObject(final String name, final Collection<Method> overloadedMethods, final ValueConverter valueConverter,
            final Properties signatureMappings)
    {
        this.name = name;
        try
        {
            for (final Method method : overloadedMethods)
            {
                this.signatures.addAll(Signature.mapToSignature(method, signatureMappings));
            }
        }
        catch (final ClassNotFoundException ex)
        {
            throw new ScriptException("Error mapping method signature", ex);
        }
        this.valueConverter = valueConverter;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object call(final Object thiz, final Object... args)
    {
        final Object[] realArgs = new Object[args.length];
        System.arraycopy(args, 0, realArgs, 0, args.length);

        // first go around: eliminate undefined
        for (int i = 0; i < realArgs.length; i++)
        {
            if (realArgs[i] == ScriptRuntime.UNDEFINED)
            {
                realArgs[i] = null;
            }
        }

        Signature bestSignature = null;
        for (final Signature signature : this.signatures)
        {
            if (signature.matches(this.valueConverter, realArgs) && (bestSignature == null || signature.isMoreSpecificThan(bestSignature)))
            {
                bestSignature = signature;
            }
        }

        if (bestSignature == null)
        {
            throw new ScriptException("No method applicable for parameters found");
        }

        try
        {
            final Object candidateResult = bestSignature.invoke(thiz, realArgs, this.valueConverter);

            // script-compatible result with some further magic
            // Note: We assume results are passed by-value - not by-reference. Loss of reference to original should not
            // be an issue.
            // Note(2): Only occurences of results passed by-reference (list-like maps) are explcitly ignored.
            final Object result;
            if ((candidateResult instanceof Collection<?> && !(candidateResult instanceof Map<?, ?>))
                    || (candidateResult != null && candidateResult.getClass().isArray()))
            {
                result = NativeJava.from(null, candidateResult);
            }
            else
            {
                result = candidateResult;
            }

            return result;
        }
        catch (final IllegalAccessException ex)
        {
            throw new AlfrescoRuntimeException("Error calling Java method via JSObject interface", ex);
        }
        catch (final InvocationTargetException ex)
        {
            throw new AlfrescoRuntimeException("Error calling Java method via JSObject interface", ex);
        }
    }
}
