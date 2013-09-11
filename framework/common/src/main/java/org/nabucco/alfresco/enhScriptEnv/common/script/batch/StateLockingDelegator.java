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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * Instances of this class are {@link Scriptable} and {@link Function} delegators / interceptors that guarantee atomicity for script object
 * manipulation and function invocation in a multi-threaded environment. In the case of functions, instances will also take care to
 * write-lock the state of their owning objects so that internal state change due to execution of the function will appear atomic to other
 * threads.
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class StateLockingDelegator extends ObjectFacadingDelegator
{
    private static final Set<String> TRIVIAL_READ_ONLY_FUNCTION_NAMES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "get", "is", "has", "find", "toString", "search", "query")));

    private static final Set<String> TRIVIAL_READ_ONLY_FUNCTION_PREFIXES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "get", "is", "has", "find", "search", "query")));

    protected static final Map<Scriptable, Map<Scriptable, StateLockingDelegator>> DELEGATOR_MAP_BY_SCOPE_CONTEXT = new WeakHashMap<Scriptable, Map<Scriptable, StateLockingDelegator>>();

    protected final ReentrantReadWriteLock stateLock = new ReentrantReadWriteLock(true);

    protected final ThreadLocal<String> mostRecentAccessName = new ThreadLocal<String>();

    public StateLockingDelegator(final Scriptable refereneScope, final Scriptable delegee, final ObjectFacadeFactory facadeFactory)
    {
        super(refereneScope, delegee, facadeFactory);
    }

    public void setMostRecentAccessName(final String accessName)
    {
        this.mostRecentAccessName.set(accessName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final String name, final Scriptable start)
    {
        // since get operations need to iterate over slots, we need to read-lock to protected against concurrent
        // modification
        this.readLock();
        try
        {
            return super.get(name, start);
        }
        finally
        {
            this.readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(final int index, final Scriptable start)
    {
        // since get operations need to iterate over slots, we need to read-lock to protected against concurrent
        // modification
        this.readLock();
        try
        {
            return super.get(index, start);
        }
        finally
        {
            this.readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has(final String name, final Scriptable start)
    {
        // since has operations need to iterate over slots, we need to read-lock to protected against concurrent
        // modification
        this.readLock();
        try
        {
            return super.has(name, start);
        }
        finally
        {
            this.readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean has(final int index, final Scriptable start)
    {
        // since has operations need to iterate over slots, we need to read-lock to protected against concurrent
        // modification
        this.readLock();
        try
        {
            return super.has(index, start);
        }
        finally
        {
            this.readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final String name, final Scriptable start, final Object value)
    {
        this.writeLock();
        try
        {
            if (start != this && start instanceof StateLockingDelegator)
            {
                // we are the prototype of start that "has" the property being set on start
                // our state and state of start should be updated atomically, so lock start as well
                final StateLockingDelegator startDelegator = (StateLockingDelegator) start;
                startDelegator.writeLock();
                try
                {
                    super.put(name, start, value);
                }
                finally
                {
                    startDelegator.writeUnlock();
                }
            }
            else
            {
                super.put(name, start, value);
            }
        }
        finally
        {
            this.writeUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final int index, final Scriptable start, final Object value)
    {
        this.writeLock();
        try
        {
            if (start != this && start instanceof StateLockingDelegator)
            {
                // we are the prototype of start that "has" the property/index being set on start
                // our state and state of start should be updated atomically, so lock start as well
                final StateLockingDelegator startDelegator = (StateLockingDelegator) start;
                startDelegator.writeLock();
                try
                {
                    super.put(index, start, value);
                }
                finally
                {
                    startDelegator.writeUnlock();
                }
            }
            else
            {
                super.put(index, start, value);
            }
        }
        finally
        {
            this.writeUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final String name)
    {
        this.writeLock();
        try
        {
            super.delete(name);
        }
        finally
        {
            this.writeUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final int index)
    {
        this.writeLock();
        try
        {
            super.delete(index);
        }
        finally
        {
            this.writeUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable getPrototype()
    {
        // getPrototype is atomic by nature, but our superclass executes non-atomic facading logic
        this.readLock();
        try
        {
            return super.getPrototype();
        }
        finally
        {
            this.readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable getParentScope()
    {
        // getParentScope is atomic by nature, but our superclass executes non-atomic facading logic
        this.readLock();
        try
        {
            return super.getParentScope();
        }
        finally
        {
            this.readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getIds()
    {
        this.readLock();
        try
        {
            return super.getIds();
        }
        finally
        {
            this.readUnlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object call(final Context cx, final Scriptable scope, final Scriptable thisObj, final Object[] args)
    {
        // TODO: can we consider functions / methods of processor extensions read-only?

        final boolean presumedReadOnly;
        final String mostRecentAccessName = this.mostRecentAccessName.get();

        if (mostRecentAccessName == null)
        {
            presumedReadOnly = false;
        }
        else
        {
            if (TRIVIAL_READ_ONLY_FUNCTION_NAMES.contains(mostRecentAccessName))
            {
                presumedReadOnly = true;
            }
            else
            {
                boolean readOnlyMatchFound = false;
                for (final String readOnlyNamePrefix : TRIVIAL_READ_ONLY_FUNCTION_PREFIXES)
                {
                    if (!readOnlyMatchFound && mostRecentAccessName.length() > readOnlyNamePrefix.length()
                            && mostRecentAccessName.startsWith(readOnlyNamePrefix))
                    {
                        final String subString = mostRecentAccessName.substring(readOnlyNamePrefix.length(),
                                readOnlyNamePrefix.length() + 1);
                        final String upperCasedSubString = subString.toUpperCase();
                        if (subString.equals(upperCasedSubString))
                        {
                            readOnlyMatchFound = true;
                        }
                    }
                }

                // TODO: improve detection of potential read-only functions

                presumedReadOnly = readOnlyMatchFound;
            }

        }

        // assume execution can modify function state
        if (presumedReadOnly)
        {
            this.readLock();
        }
        else
        {
            this.writeLock();
        }
        try
        {
            Object result;
            // assume execution can modify state of thisObj
            if (thisObj instanceof StateLockingDelegator)
            {
                final StateLockingDelegator thisObjDelegator = (StateLockingDelegator) thisObj;
                if (presumedReadOnly)
                {
                    thisObjDelegator.readLock();
                }
                else
                {
                    thisObjDelegator.writeLock();
                }
                try
                {
                    result = super.call(cx, scope, thisObj, args);
                }
                finally
                {
                    if (presumedReadOnly)
                    {
                        thisObjDelegator.readUnlock();
                    }
                    else
                    {
                        thisObjDelegator.writeUnlock();
                    }
                }
            }
            else
            {
                result = super.call(cx, scope, thisObj, args);
            }

            return result;
        }
        finally
        {
            if (presumedReadOnly)
            {
                this.readUnlock();
            }
            else
            {
                this.writeUnlock();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Scriptable construct(final Context cx, final Scriptable scope, final Object[] args)
    {
        // assume execution only uses function state to construct new instances
        this.readLock();
        try
        {
            final Scriptable constructed = super.construct(cx, scope, args);
            return constructed;
        }
        finally
        {
            this.readUnlock();
        }

    }

    protected void writeLock()
    {
        this.stateLock.writeLock().lock();
    }

    protected void writeUnlock()
    {
        this.stateLock.writeLock().unlock();
    }

    protected void readLock()
    {
        this.stateLock.readLock().lock();
    }

    protected void readUnlock()
    {
        this.stateLock.readLock().unlock();
    }
}
