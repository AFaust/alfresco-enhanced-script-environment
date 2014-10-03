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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.rhino2nashorn.beans;

import org.alfresco.repo.jscript.ChildAssociation;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.util.Pair;
import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.rhino2nashorn.SimpleJSObjectSubClassProxyConverter;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ChildAssociationRefToChildAssociationConverter extends SimpleJSObjectSubClassProxyConverter
{

    protected ServiceRegistry serviceRegistry;

    /**
     * @param serviceRegistry
     *            the serviceRegistry to set
     */
    public void setServiceRegistry(final ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "serviceRegistry", this.serviceRegistry);

        super.afterPropertiesSet();
        this.registry.registerValueInstanceConverter(ChildAssociation.class, this);
    }

    public ChildAssociationRefToChildAssociationConverter()
    {
        this.javaBaseClass = ChildAssociationRef.class;
        this.checkBaseClassInConversion = false;
        this.confidence = HIGHEST_CONFIDENCE;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForScriptConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if (this.javaBaseClass.isAssignableFrom(valueInstanceClass)
                && expectedClass.isAssignableFrom(ChildAssociation.class))
        {
            confidence = this.confidence;
        }
        else
        {
            confidence = LOWEST_CONFIDENCE;
        }
        return confidence;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForScript(final Object value, final ValueConverter globalDelegate,
            final Class<?> expectedClass)
    {
        final boolean canConvert = value instanceof ChildAssociationRef
                && expectedClass.isAssignableFrom(ChildAssociation.class);

        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate,
            final Class<?> expectedClass)
    {
        if (!(value instanceof ChildAssociationRef))
        {
            throw new IllegalArgumentException("value must be a " + ChildAssociationRef.class);
        }

        final ChildAssociationRef childAssociationRef = (ChildAssociationRef) value;

        final ChildAssociation result = new ChildAssociation(this.serviceRegistry, childAssociationRef, DUMMY_SCOPE);

        return super.convertValueForScript(result, globalDelegate, expectedClass);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public int getForJavaConversionConfidence(final Class<?> valueInstanceClass, final Class<?> expectedClass)
    {
        final int confidence;
        if (ChildAssociation.class.isAssignableFrom(valueInstanceClass)
                && expectedClass.isAssignableFrom(ChildAssociationRef.class))
        {
            confidence = HIGHEST_CONFIDENCE;
        }
        else
        {
            confidence = LOWEST_CONFIDENCE;
        }
        return confidence;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvertValueForJava(final Object value, final ValueConverter globalDelegate,
            final Class<?> expectedClass)
    {
        final boolean canConvert = value instanceof ChildAssociation
                && expectedClass.isAssignableFrom(ChildAssociationRef.class);
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate,
            final Class<?> expectedClass)
    {
        if (!(value instanceof ChildAssociation))
        {
            throw new IllegalArgumentException("value must be a " + ChildAssociation.class);
        }

        final ChildAssociation childAssociation = (ChildAssociation) value;
        final ChildAssociationRef childAssociationRef = childAssociation.getChildAssociationRef();

        return childAssociationRef;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Pair<Class<?>[], Object[]> determineForScriptProxyConstructorParameters(final Object value,
            final Class<?> expectedClass)
    {
        final Pair<Class<?>[], Object[]> result;

        if (value instanceof ChildAssociation)
        {
            final Object[] ctorArguments = new Object[] { this.serviceRegistry,
                    ((ChildAssociation) value).getChildAssociationRef(),
                    DUMMY_SCOPE };
            final Class<?>[] ctorArgumentTypes = new Class[] { ServiceRegistry.class, ChildAssociationRef.class,
                    Scriptable.class };

            result = new Pair<>(ctorArgumentTypes, ctorArguments);
        }
        else
        {
            result = super.determineForScriptProxyConstructorParameters(value, expectedClass);
        }

        return result;
    }

}
