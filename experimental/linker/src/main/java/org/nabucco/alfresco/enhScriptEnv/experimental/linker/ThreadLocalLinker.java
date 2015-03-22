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
package org.nabucco.alfresco.enhScriptEnv.experimental.linker;

import jdk.internal.dynalink.linker.GuardedInvocation;
import jdk.internal.dynalink.linker.GuardedTypeConversion;
import jdk.internal.dynalink.linker.GuardingDynamicLinker;
import jdk.internal.dynalink.linker.GuardingTypeConverterFactory;
import jdk.internal.dynalink.linker.LinkRequest;
import jdk.internal.dynalink.linker.LinkerServices;
import jdk.internal.dynalink.linker.TypeBasedGuardingDynamicLinker;
import jdk.internal.dynalink.linker.ConversionComparator;

/**
 * This proxy linker is available to allow a script hosting environment to dynamically set a custom linker for the current execution
 * context.
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class ThreadLocalLinker implements TypeBasedGuardingDynamicLinker, GuardingTypeConverterFactory, ConversionComparator
{

    private static final ThreadLocal<GuardingDynamicLinker> linker = new ThreadLocal<GuardingDynamicLinker>();

    @FunctionalInterface
    public static interface WithLinkerCallback<T>
    {

        T invoke();
    }

    public static <T> T withLinker(final GuardingDynamicLinker linker, final WithLinkerCallback<T> callback)
    {
        final GuardingDynamicLinker previousLinker = ThreadLocalLinker.linker.get();
        ThreadLocalLinker.linker.set(linker);
        try
        {
            return callback.invoke();
        }
        finally
        {
            ThreadLocalLinker.linker.set(previousLinker);
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canLinkType(final Class<?> type)
    {
        final GuardingDynamicLinker linker = ThreadLocalLinker.linker.get();
        // assume true if provided linker is not itself type-based
        final boolean canLink = linker instanceof TypeBasedGuardingDynamicLinker ? ((TypeBasedGuardingDynamicLinker) linker)
                .canLinkType(type) : true;
        return canLink;
    }

    public Comparison compareConversion(final Class<?> sourceType, final Class<?> targetType1, final Class<?> targetType2)
    {
        final GuardingDynamicLinker linker = ThreadLocalLinker.linker.get();
        final Comparison comparison = linker instanceof ConversionComparator ? ((ConversionComparator) linker).compareConversion(
                sourceType, targetType1, targetType2) : Comparison.INDETERMINATE;
        return comparison;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public GuardedTypeConversion convertToType(final Class<?> sourceType, final Class<?> targetType) throws Exception
    {
        final GuardingDynamicLinker linker = ThreadLocalLinker.linker.get();
        final GuardedTypeConversion conversion = linker instanceof GuardingTypeConverterFactory ? ((GuardingTypeConverterFactory) linker)
                .convertToType(sourceType, targetType) : null;
        return conversion;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public GuardedInvocation getGuardedInvocation(final LinkRequest linkRequest, final LinkerServices linkerServices) throws Exception
    {
        final GuardingDynamicLinker linker = ThreadLocalLinker.linker.get();
        final GuardedInvocation invocation = linker != null ? linker.getGuardedInvocation(linkRequest, linkerServices) : null;
        return invocation;
    }
}
