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

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Delegator;
import org.mozilla.javascript.Scriptable;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ObjectFacadingDelegator extends Delegator
{
    protected final Scriptable referenceScope;

    protected final ObjectFacadeFactory facadeFactory;

    public ObjectFacadingDelegator(final Scriptable referenceScope, final Scriptable delegee, final ObjectFacadeFactory facadeFactory)
    {
        this.referenceScope = referenceScope;
        this.setDelegee(delegee);
        this.facadeFactory = facadeFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final String name, final Scriptable start)
    {
        final Object result = super.get(name, this.toRealObject(start));
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
        return super.has(name, this.toRealObject(start));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has(final int index, final Scriptable start)
    {
        return super.has(index, this.toRealObject(start));
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
            realValue = this.toRealObject((Scriptable) value);
        }
        else
        {
            realValue = value;
        }
        super.put(name, this.toRealObject(start), realValue);
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
            realValue = this.toRealObject((Scriptable) value);
        }
        else
        {
            realValue = value;
        }
        super.put(index, this.toRealObject(start), realValue);
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
        super.setPrototype(this.toRealObject(prototype));
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
        super.setParentScope(this.toRealObject(parent));
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
                realArgs[idx] = this.toRealObject((Scriptable) realArgs[idx]);
            }
        }

        final Object realResult = super.call(cx, this.toRealObject(scope), this.toRealObject(thisObj), realArgs);

        final Object result;
        if (realResult instanceof Scriptable)
        {
            result = this.toFacadedObject((Scriptable) realResult);
        }
        else
        {
            result = realResult;
        }
        return result;
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
                realArgs[idx] = this.toRealObject((Scriptable) realArgs[idx]);
            }
        }

        final Scriptable realResult = super.construct(cx, this.toRealObject(scope), realArgs);
        final Scriptable result = this.toFacadedObject(realResult);
        return result;
    }

    protected Scriptable toRealObject(final Scriptable obj)
    {
        final Scriptable realObj = this.facadeFactory.toRealObject(obj, this.referenceScope);
        return realObj;
    }

    protected Scriptable toFacadedObject(final Scriptable obj)
    {
        final Scriptable facadedObject = this.facadeFactory.toFacadedObject(obj, this.referenceScope);
        return facadedObject;
    }

    protected Scriptable toFacadedObject(final Scriptable obj, final String accessName)
    {
        final Scriptable facadedObject = this.facadeFactory.toFacadedObject(obj, this.referenceScope, accessName);
        return facadedObject;
    }
}