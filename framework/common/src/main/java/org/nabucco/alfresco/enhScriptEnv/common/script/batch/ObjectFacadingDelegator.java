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
package org.nabucco.alfresco.enhScriptEnv.common.script.batch;

import java.util.Map;
import java.util.WeakHashMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Delegator;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ObjectFacadingDelegator extends Delegator
{
    protected static final ThreadLocal<Map<Scriptable, ObjectFacadingDelegator>> FACADE_BY_REAL_OBJECT = new ThreadLocal<Map<Scriptable, ObjectFacadingDelegator>>();

    protected static final Map<Scriptable, Map<Scriptable, ObjectFacadingDelegator>> FACADE_BY_REAL_OBJECT_AND_REFERENCE_SCOPE = new WeakHashMap<Scriptable, Map<Scriptable, ObjectFacadingDelegator>>();

    protected final Scriptable referenceScope;

    public static void clearThread()
    {
        FACADE_BY_REAL_OBJECT.remove();
    }

    public static void clearReferenceScope(final Scriptable referenceScope)
    {
        synchronized (FACADE_BY_REAL_OBJECT_AND_REFERENCE_SCOPE)
        {
            FACADE_BY_REAL_OBJECT_AND_REFERENCE_SCOPE.remove(referenceScope);
        }
    }

    public ObjectFacadingDelegator(final Scriptable referenceScope, final Scriptable delegee)
    {
        this.referenceScope = referenceScope;
        this.setDelegee(delegee);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final String name, final Scriptable start)
    {
        final Object result = super.get(name, toRealObject(start));
        final Object realResult;
        if (result instanceof Scriptable)
        {
            realResult = this.toFacadedObject((Scriptable) result);
        }
        else
        {
            realResult = result;
        }
        return realResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final int index, final Scriptable start)
    {
        final Object result = super.get(index, start);
        final Object realResult;
        if (result instanceof Scriptable)
        {
            realResult = this.toFacadedObject((Scriptable) result);
        }
        else
        {
            realResult = result;
        }
        return realResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has(final String name, final Scriptable start)
    {
        return super.has(name, toRealObject(start));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has(final int index, final Scriptable start)
    {
        return super.has(index, toRealObject(start));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final String name, final Scriptable start, final Object value)
    {
        final Object realValue;
        if (value instanceof Scriptable)
        {
            realValue = this.toFacadedObject((Scriptable) value);
        }
        else
        {
            realValue = value;
        }
        super.put(name, toRealObject(start), realValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final int index, final Scriptable start, final Object value)
    {
        final Object realValue;
        if (value instanceof Scriptable)
        {
            realValue = this.toFacadedObject((Scriptable) value);
        }
        else
        {
            realValue = value;
        }
        super.put(index, toRealObject(start), realValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable getPrototype()
    {
        final Scriptable realResult = this.toFacadedObject(super.getPrototype());
        return realResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPrototype(final Scriptable prototype)
    {
        super.setPrototype(toRealObject(prototype));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable getParentScope()
    {
        final Scriptable realResult = this.toFacadedObject(super.getParentScope());
        return realResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParentScope(final Scriptable parent)
    {
        super.setParentScope(toRealObject(parent));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasInstance(final Scriptable instance)
    {
        final Scriptable realInstance = instance instanceof ObjectFacadingDelegator ? ((ObjectFacadingDelegator) instance).getDelegee()
                : instance;
        return super.hasInstance(realInstance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object call(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args)
    {
        final Object[] realArgs = new Object[args.length];
        System.arraycopy(args, 0, realArgs, 0, args.length);
        for (int idx = 0; idx < realArgs.length; idx++)
        {
            if (realArgs[idx] instanceof Scriptable)
            {
                realArgs[idx] = toRealObject((Scriptable) realArgs[idx]);
            }
        }

        final Object result = super.call(cx, toRealObject(scope), toRealObject(thisObj), realArgs);

        final Object realResult;
        if (result instanceof Scriptable)
        {
            realResult = this.toFacadedObject((Scriptable) result);
        }
        else
        {
            realResult = result;
        }
        return realResult;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable construct(final Context cx, final Scriptable scope, final Object[] args)
    {
        final Object[] realArgs = new Object[args.length];
        System.arraycopy(args, 0, realArgs, 0, args.length);
        for (int idx = 0; idx < realArgs.length; idx++)
        {
            if (realArgs[idx] instanceof Scriptable)
            {
                realArgs[idx] = toRealObject((Scriptable) realArgs[idx]);
            }
        }

        final Scriptable result = super.construct(cx, toRealObject(scope), realArgs);
        final Scriptable realResult = this.toFacadedObject(result);
        return realResult;
    }

    protected static Scriptable toRealObject(final Scriptable obj)
    {
        final Scriptable realObj = obj instanceof ObjectFacadingDelegator ? ((ObjectFacadingDelegator) obj).getDelegee() : obj;
        return realObj;
    }

    protected static Scriptable toFacadedObject(final Scriptable obj, final Scriptable referenceScope, final ObjectFacadingDelegator thisObj)
    {
        final Scriptable facadedObject;

        if (obj != null)
        {
            final Scriptable threadLocalFacadedObj;
            final Map<Scriptable, ObjectFacadingDelegator> facadeByRealObject = FACADE_BY_REAL_OBJECT.get();
            if (facadeByRealObject != null)
            {
                threadLocalFacadedObj = facadeByRealObject.get(obj);
            }
            else
            {
                threadLocalFacadedObj = null;
                FACADE_BY_REAL_OBJECT.set(new WeakHashMap<Scriptable, ObjectFacadingDelegator>());
            }

            if (threadLocalFacadedObj == null)
            {
                Map<Scriptable, ObjectFacadingDelegator> facadeByRealObjectAndReferenceScope = FACADE_BY_REAL_OBJECT_AND_REFERENCE_SCOPE
                        .get(referenceScope);
                if (facadeByRealObjectAndReferenceScope == null)
                {
                    synchronized (FACADE_BY_REAL_OBJECT_AND_REFERENCE_SCOPE)
                    {
                        facadeByRealObjectAndReferenceScope = FACADE_BY_REAL_OBJECT_AND_REFERENCE_SCOPE.get(referenceScope);
                        if (facadeByRealObjectAndReferenceScope == null)
                        {
                            facadeByRealObjectAndReferenceScope = new WeakHashMap<Scriptable, ObjectFacadingDelegator>();
                            FACADE_BY_REAL_OBJECT_AND_REFERENCE_SCOPE.put(referenceScope, facadeByRealObjectAndReferenceScope);
                        }
                    }
                }

                Scriptable globalFacadedObject = facadeByRealObjectAndReferenceScope.get(obj);
                if (globalFacadedObject == null)
                {
                    synchronized (facadeByRealObjectAndReferenceScope)
                    {
                        globalFacadedObject = facadeByRealObjectAndReferenceScope.get(obj);
                        if (globalFacadedObject == null)
                        {
                            if (obj instanceof NativeJavaObject)
                            {
                                final NativeJavaObject nativeJavaObj = (NativeJavaObject) obj;
                                final Object javaObj = nativeJavaObj.unwrap();

                                if (javaObj instanceof org.alfresco.processor.ProcessorExtension
                                        || javaObj instanceof org.springframework.extensions.surf.core.processor.ProcessorExtension)
                                {
                                    // TODO: how to handle scopeable objects in Repository?

                                    // processor extensions should be implemented in a thread-safe manner
                                    globalFacadedObject = new ObjectFacadingDelegator(referenceScope, obj);
                                }
                                else
                                {
                                    // can be anything - assume to be thread-unsafe
                                    globalFacadedObject = new StateLockingDelegator(referenceScope, obj);
                                }
                            }
                            else if (obj instanceof Function)
                            {
                                if (thisObj instanceof StateLockingDelegator)
                                {
                                    globalFacadedObject = new StateLockingDelegator(referenceScope, obj,
                                            (StateLockingDelegator) thisObj);
                                }
                                else
                                {
                                    globalFacadedObject = new StateLockingDelegator(referenceScope, obj);
                                }
                            }
                            else
                            {
                                globalFacadedObject = new StateLockingDelegator(referenceScope, obj);
                            }

                            if (globalFacadedObject instanceof ObjectFacadingDelegator)
                            {
                                facadeByRealObjectAndReferenceScope.put(obj, (ObjectFacadingDelegator) globalFacadedObject);
                            }
                        }
                    }
                }

                facadedObject = globalFacadedObject;

                if (facadedObject instanceof ObjectFacadingDelegator)
                {
                    FACADE_BY_REAL_OBJECT.get().put(obj, (ObjectFacadingDelegator) facadedObject);
                }
            }
            else
            {
                facadedObject = threadLocalFacadedObj;
            }
        }
        else
        {
            facadedObject = obj;
        }

        return facadedObject;
    }

    protected Scriptable toFacadedObject(final Scriptable obj)
    {
        return toFacadedObject(obj, this.referenceScope, this);
    }
}