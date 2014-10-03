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

import org.alfresco.repo.jscript.Association;
import org.alfresco.repo.jscript.ChildAssociation;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.util.Pair;
import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.rhino2nashorn.SimpleJSObjectSubClassProxyConverter;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class AssociationRefToAssociationConverter extends SimpleJSObjectSubClassProxyConverter
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
        this.registry.registerValueInstanceConverter(Association.class, this);
    }

    public AssociationRefToAssociationConverter()
    {
        this.javaBaseClass = AssociationRef.class;
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
                && expectedClass.isAssignableFrom(Association.class))
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
        final boolean canConvert = value instanceof AssociationRef
                && expectedClass.isAssignableFrom(Association.class);

        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate,
            final Class<?> expectedClass)
    {
        if (!(value instanceof AssociationRef))
        {
            throw new IllegalArgumentException("value must be a " + AssociationRef.class);
        }

        final AssociationRef associationRef = (AssociationRef) value;

        final Association result = new Association(this.serviceRegistry, associationRef, DUMMY_SCOPE);

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
        if (Association.class.isAssignableFrom(valueInstanceClass)
                && expectedClass.isAssignableFrom(AssociationRef.class))
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
        final boolean canConvert = value instanceof Association
                && expectedClass.isAssignableFrom(AssociationRef.class);
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate,
            final Class<?> expectedClass)
    {
        if (!(value instanceof Association))
        {
            throw new IllegalArgumentException("value must be a " + Association.class);
        }

        final Association association = (Association) value;
        final AssociationRef associationRef = association.getAssociationRef();

        return associationRef;
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
