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

import java.lang.reflect.Field;

import org.alfresco.repo.jscript.ChildAssociation;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.scripts.ScriptException;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.util.Pair;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.rhino2nashorn.SimpleJSObjectSubClassProxyConverter;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ChildAssociationConverter extends SimpleJSObjectSubClassProxyConverter
{

    public ChildAssociationConverter()
    {
        this.javaBaseClass = ChildAssociation.class;
        this.confidence = HIGHEST_CONFIDENCE;
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
            try
            {
                final Field serviceRegistryField = ScriptNode.class.getDeclaredField("services");
                serviceRegistryField.setAccessible(true);
                final ServiceRegistry serviceRegistry = (ServiceRegistry) serviceRegistryField.get(value);

                final Object[] ctorArguments = new Object[] { serviceRegistry,
                        ((ChildAssociation) value).getChildAssociationRef(),
                        DUMMY_SCOPE };
                final Class<?>[] ctorArgumentTypes = new Class[] { ServiceRegistry.class, ChildAssociationRef.class,
                        Scriptable.class };

                result = new Pair<>(ctorArgumentTypes, ctorArguments);
            }
            catch (final NoSuchFieldException ex)
            {
                throw new ScriptException(
                        "Failed to determine constructor parameters for ChildAssociation sub-class proxy",
                        ex);
            }
            catch (final IllegalAccessException ex)
            {
                throw new ScriptException(
                        "Failed to determine constructor parameters for ChildAssociation sub-class proxy",
                        ex);
            }
        }
        else
        {
            result = super.determineForScriptProxyConstructorParameters(value, expectedClass);
        }

        return result;
    }

}
