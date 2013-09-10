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

import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class DefaultFacadeFactory implements ObjectFacadeFactory
{

    protected final ThreadLocal<Map<Scriptable, Scriptable>> facadeByRealObject = new ThreadLocal<Map<Scriptable, Scriptable>>();

    protected final Map<Scriptable, Map<Scriptable, Scriptable>> facadeByRealObjectAndReferenceScope = new WeakHashMap<Scriptable, Map<Scriptable, Scriptable>>();

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void clearThread()
    {
        this.facadeByRealObject.remove();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void clearReferenceScope(final Scriptable referenceScope)
    {
        synchronized (this.facadeByRealObjectAndReferenceScope)
        {
            this.facadeByRealObjectAndReferenceScope.remove(referenceScope);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable toFacadedObject(final Scriptable obj, final Scriptable referenceScope)
    {
        final Scriptable facadedObject = this.toFacadedObject(obj, referenceScope, null);
        return facadedObject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable toFacadedObject(final Scriptable obj, final Scriptable referenceScope, final Scriptable thisObj)
    {
        final Scriptable facadedObject;

        if (obj != null && !(obj instanceof ObjectFacadingDelegator))
        {
            final Scriptable threadLocalFacadedObj;
            final Map<Scriptable, Scriptable> facadeByRealObject = this.facadeByRealObject.get();
            if (facadeByRealObject != null)
            {
                threadLocalFacadedObj = facadeByRealObject.get(obj);
            }
            else
            {
                threadLocalFacadedObj = null;
                this.facadeByRealObject.set(new WeakHashMap<Scriptable, Scriptable>());
            }

            if (threadLocalFacadedObj == null)
            {
                Map<Scriptable, Scriptable> facadeByRealObjectAndReferenceScope = this.facadeByRealObjectAndReferenceScope
                        .get(referenceScope);
                if (facadeByRealObjectAndReferenceScope == null)
                {
                    synchronized (this.facadeByRealObjectAndReferenceScope)
                    {
                        facadeByRealObjectAndReferenceScope = this.facadeByRealObjectAndReferenceScope.get(referenceScope);
                        if (facadeByRealObjectAndReferenceScope == null)
                        {
                            facadeByRealObjectAndReferenceScope = new WeakHashMap<Scriptable, Scriptable>();
                            this.facadeByRealObjectAndReferenceScope.put(referenceScope, facadeByRealObjectAndReferenceScope);
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
                            globalFacadedObject = this.toFacadedObjectImpl(obj, referenceScope, thisObj);
                            facadeByRealObjectAndReferenceScope.put(obj, globalFacadedObject);
                        }
                    }
                }

                facadedObject = globalFacadedObject;

                this.facadeByRealObject.get().put(obj, facadedObject);
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

    protected Scriptable toFacadedObjectImpl(final Scriptable obj, final Scriptable referenceScope, final Scriptable thisObj)
    {
        Scriptable globalFacadedObject;
        if (obj instanceof NativeJavaObject)
        {
            final NativeJavaObject nativeJavaObj = (NativeJavaObject) obj;
            final Object javaObj = nativeJavaObj.unwrap();

            if (javaObj instanceof org.alfresco.processor.ProcessorExtension
                    || javaObj instanceof org.springframework.extensions.surf.core.processor.ProcessorExtension)
            {
                // processor extensions should be implemented in a thread-safe manner
                globalFacadedObject = new ObjectFacadingDelegator(referenceScope, obj, this);
            }
            else
            {
                // can be anything - assume to be thread-unsafe
                globalFacadedObject = new StateLockingDelegator(referenceScope, obj, this);
            }
        }
        else if (obj instanceof Function)
        {
            if (thisObj instanceof StateLockingDelegator)
            {
                globalFacadedObject = new StateLockingDelegator(referenceScope, obj, this, (StateLockingDelegator) thisObj);
            }
            else
            {
                globalFacadedObject = new StateLockingDelegator(referenceScope, obj, this);
            }
        }
        else
        {
            globalFacadedObject = new StateLockingDelegator(referenceScope, obj, this);
        }
        return globalFacadedObject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable toRealObject(final Scriptable facadedObject, final Scriptable referenceScope)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
