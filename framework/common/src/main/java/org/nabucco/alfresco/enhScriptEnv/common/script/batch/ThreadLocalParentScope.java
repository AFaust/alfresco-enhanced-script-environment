/*
 * Copyright 2014 PRODYNA AG
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

import org.mozilla.javascript.Scriptable;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ThreadLocalParentScope implements Scriptable
{

    private final Scriptable objectParentScope;

    private final Scriptable globalScope;

    private final ObjectFacadeFactory facadeFactory;

    private final ThreadLocal<Scriptable> delegate = new ThreadLocal<Scriptable>();

    public ThreadLocalParentScope(final Scriptable objectParentScope, final Scriptable globalScope, final ObjectFacadeFactory facadeFactory)
    {
        this.objectParentScope = objectParentScope;
        this.globalScope = globalScope;
        this.facadeFactory = facadeFactory;
    }

    public void setEffectiveParentScope(final Scriptable effectiveParentScope)
    {
        final Scriptable parentScopePrototype = this.facadeFactory.toFacadedObject(this.objectParentScope, this.globalScope);
        effectiveParentScope.setPrototype(parentScopePrototype);
        effectiveParentScope.setParentScope(this.facadeFactory.toFacadedObject(this.objectParentScope.getParentScope(), this.globalScope));
        this.delegate.set(effectiveParentScope);
    }

    public void removeEffectiveParentScope()
    {
        this.delegate.get().setPrototype(null);
        this.delegate.get().setParentScope(null);
        this.delegate.remove();
    }

    /**
     * @return the realParentScope
     */
    public final Scriptable getRealParentScope()
    {
        return this.objectParentScope;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getClassName()
    {
        return this.delegate.get().getClassName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final String name, final Scriptable start)
    {
        return this.delegate.get().get(name, start == this ? this.delegate.get() : start);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final int index, final Scriptable start)
    {
        return this.delegate.get().get(index, start == this ? this.delegate.get() : start);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has(final String name, final Scriptable start)
    {
        return this.delegate.get().has(name, start == this ? this.delegate.get() : start);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has(final int index, final Scriptable start)
    {
        return this.delegate.get().has(index, start == this ? this.delegate.get() : start);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final String name, final Scriptable start, final Object value)
    {
        this.delegate.get().put(name, start == this ? this.delegate.get() : start, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final int index, final Scriptable start, final Object value)
    {
        this.delegate.get().put(index, start == this ? this.delegate.get() : start, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final String name)
    {
        this.delegate.get().delete(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final int index)
    {
        this.delegate.get().delete(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable getPrototype()
    {
        return this.delegate.get().getPrototype();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPrototype(final Scriptable prototype)
    {
        this.delegate.get().setPrototype(prototype);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable getParentScope()
    {
        return this.delegate.get().getParentScope();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParentScope(final Scriptable parent)
    {
        this.delegate.get().setParentScope(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getIds()
    {
        return this.delegate.get().getIds();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getDefaultValue(@SuppressWarnings("rawtypes") final Class hint)
    {
        return this.delegate.get().getDefaultValue(hint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasInstance(final Scriptable instance)
    {
        return this.delegate.get().hasInstance(instance);
    }

}
