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
package org.nabucco.alfresco.enhScriptEnv.repo.script;

import java.io.Serializable;

import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.Scriptable;

/**
 * An abstract function to extend the runtime API of the Rhino engine with an additional top level function.
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class AbstractFunction implements Scriptable, IdFunctionCall, Serializable
{

    private static final long serialVersionUID = 69233047066793922L;
    protected Scriptable prototype;
    protected Scriptable parent;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final String name, final Scriptable start)
    {
        final Scriptable realStart = start == this ? this.prototype : start;
        return this.prototype.get(name, realStart);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final int index, final Scriptable start)
    {
        final Scriptable realStart = start == this ? this.prototype : start;
        return this.prototype.get(index, realStart);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has(final String name, final Scriptable start)
    {
        final Scriptable realStart = start == this ? this.prototype : start;
        return this.prototype.has(name, realStart);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has(final int index, final Scriptable start)
    {
        final Scriptable realStart = start == this ? this.prototype : start;
        return this.prototype.has(index, realStart);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final String name, final Scriptable start, final Object value)
    {
        final Scriptable realStart = start == this ? this.prototype : start;
        this.prototype.put(name, realStart, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final int index, final Scriptable start, final Object value)
    {
        final Scriptable realStart = start == this ? this.prototype : start;
        this.prototype.put(index, realStart, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final String name)
    {
        this.prototype.delete(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final int index)
    {
        this.prototype.delete(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable getPrototype()
    {
        return this.prototype;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPrototype(final Scriptable prototype)
    {
        this.prototype = prototype;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable getParentScope()
    {
        return this.parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParentScope(final Scriptable parent)
    {
        this.parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getIds()
    {
        return this.prototype.getIds();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getDefaultValue(@SuppressWarnings("rawtypes") final Class hint)
    {
        return this.prototype.getDefaultValue(hint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasInstance(final Scriptable instance)
    {
        return this.prototype.hasInstance(instance);
    }

}