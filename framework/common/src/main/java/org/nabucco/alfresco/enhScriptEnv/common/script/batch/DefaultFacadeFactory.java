/*
 * Copyright 2015 PRODYNA AG
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
package org.nabucco.alfresco.enhScriptEnv.common.script.batch;

import java.lang.ref.WeakReference;
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

    // Note: We need to use weak reference for the cached facaded values as the otherwise they would keep the actual objects in memory which
    // are keys into the map thus preventing the JVM from GCing facades AND actual objects no longer reference by a script property / variable

    protected final ThreadLocal<Map<Scriptable, WeakReference<Scriptable>>> facadeByRealObject = new ThreadLocal<Map<Scriptable, WeakReference<Scriptable>>>();

    protected final Map<Scriptable, Map<Scriptable, WeakReference<Scriptable>>> facadeByRealObjectAndReferenceScope = new WeakHashMap<Scriptable, Map<Scriptable, WeakReference<Scriptable>>>();

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
    public Scriptable toFacadedObject(final Scriptable obj, final Scriptable referenceScope, final String accessName)
    {
        final Scriptable facadedObject;

        if (obj != null && !(obj instanceof ObjectFacadingDelegator))
        {
            final Scriptable threadLocalFacadedObj;
            final Map<Scriptable, WeakReference<Scriptable>> facadeByRealObject = this.facadeByRealObject.get();
            if (facadeByRealObject != null)
            {
                final WeakReference<Scriptable> threadLocalFacadedObjRef = facadeByRealObject.get(obj);
                threadLocalFacadedObj = threadLocalFacadedObjRef != null ? threadLocalFacadedObjRef.get() : null;
            }
            else
            {
                threadLocalFacadedObj = null;
                this.facadeByRealObject.set(new WeakHashMap<Scriptable, WeakReference<Scriptable>>());
            }

            if (threadLocalFacadedObj == null)
            {
                Map<Scriptable, WeakReference<Scriptable>> facadeByRealObjectAndReferenceScope = this.facadeByRealObjectAndReferenceScope
                        .get(referenceScope);
                if (facadeByRealObjectAndReferenceScope == null)
                {
                    synchronized (this.facadeByRealObjectAndReferenceScope)
                    {
                        facadeByRealObjectAndReferenceScope = this.facadeByRealObjectAndReferenceScope.get(referenceScope);
                        if (facadeByRealObjectAndReferenceScope == null)
                        {
                            facadeByRealObjectAndReferenceScope = new WeakHashMap<Scriptable, WeakReference<Scriptable>>();
                            this.facadeByRealObjectAndReferenceScope.put(referenceScope, facadeByRealObjectAndReferenceScope);
                        }
                    }
                }

                WeakReference<Scriptable> globalFacadedObjectRef = facadeByRealObjectAndReferenceScope.get(obj);
                Scriptable globalFacadedObject = globalFacadedObjectRef != null ? globalFacadedObjectRef.get() : null;
                if (globalFacadedObject == null)
                {
                    synchronized (facadeByRealObjectAndReferenceScope)
                    {
                        globalFacadedObjectRef = facadeByRealObjectAndReferenceScope.get(obj);
                        globalFacadedObject = globalFacadedObjectRef != null ? globalFacadedObjectRef.get() : null;
                        if (globalFacadedObject == null)
                        {
                            globalFacadedObject = this.toFacadedObjectImpl(obj, referenceScope, accessName);
                            facadeByRealObjectAndReferenceScope.put(obj, new WeakReference<Scriptable>(globalFacadedObject));
                        }
                    }
                }

                facadedObject = globalFacadedObject;

                this.facadeByRealObject.get().put(obj, new WeakReference<Scriptable>(facadedObject));
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

    protected Scriptable toFacadedObjectImpl(final Scriptable obj, final Scriptable referenceScope, final String accessName)
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
            globalFacadedObject = new StateLockingDelegator(referenceScope, obj, this);

            ((StateLockingDelegator) globalFacadedObject).setMostRecentAccessName(accessName);
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
        final Scriptable realObj = facadedObject instanceof ObjectFacadingDelegator ? ((ObjectFacadingDelegator) facadedObject)
                .getDelegee() : facadedObject;
        return realObj;
    }

}
