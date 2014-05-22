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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ValueConvertingMapDelegator<K, V> implements Map<K, V>
{

    protected final Map<K, V> delegate;
    protected final ValueConverter valueConverter;

    public ValueConvertingMapDelegator(final Map<K, V> delegate, final ValueConverter valueConverter)
    {
        this.delegate = delegate;
        this.valueConverter = valueConverter;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return this.delegate.size();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        return this.delegate.isEmpty();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(final Object key)
    {
        final Object realKey = this.valueConverter.convertValueForJava(key);
        return this.delegate.containsKey(realKey);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean containsValue(final Object value)
    {
        final Object realValue = this.valueConverter.convertValueForJava(value);
        return this.delegate.containsValue(realValue);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public V get(final Object key)
    {
        final Object realKey = this.valueConverter.convertValueForJava(key);
        final V realValue = this.delegate.get(realKey);
        @SuppressWarnings("unchecked")
        final V value = (V)this.valueConverter.convertValueForNashorn(realValue);
        return value;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public V put(final K key, final V value)
    {
        @SuppressWarnings("unchecked")
        final K realKey = (K) this.valueConverter.convertValueForJava(key);
        @SuppressWarnings("unchecked")
        final V realValue = (V) this.valueConverter.convertValueForJava(value);
        return this.delegate.put(realKey, realValue);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public V remove(final Object key)
    {
        final Object realKey = this.valueConverter.convertValueForJava(key);
        return this.delegate.remove(realKey);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void putAll(final Map<? extends K, ? extends V> m)
    {
        // TODO: convert values of m to Java
        this.delegate.putAll(m);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void clear()
    {
        this.delegate.clear();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Set<K> keySet()
    {
        // TODO: facade with converting collection
        return this.delegate.keySet();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Collection<V> values()
    {
        // TODO: facade with converting collection
        return this.delegate.values();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet()
    {
        // TODO: facade with converting collection
        return this.delegate.entrySet();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o)
    {
        return this.delegate.equals(o);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return this.delegate.hashCode();
    }

}
