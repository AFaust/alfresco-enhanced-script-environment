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

import org.alfresco.model.ContentModel;
import org.alfresco.model.RenditionModel;
import org.alfresco.repo.jscript.CategoryNode;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.thumbnail.script.ScriptThumbnail;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.common.script.converter.ValueConverter;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.rhino2nashorn.SimpleJSObjectSubClassProxyConverter;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class NodeRefToScriptNodeConverter extends SimpleJSObjectSubClassProxyConverter
{

    protected ServiceRegistry serviceRegistry;

    protected NodeService nodeService;

    protected DictionaryService dictionaryService;

    /**
     * @param serviceRegistry
     *            the serviceRegistry to set
     */
    public void setServiceRegistry(final ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * @param nodeService
     *            the nodeService to set
     */
    public void setNodeService(final NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param dictionaryService
     *            the dictionaryService to set
     */
    public void setDictionaryService(final DictionaryService dictionaryService)
    {
        this.dictionaryService = dictionaryService;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "serviceRegistry", this.serviceRegistry);
        PropertyCheck.mandatory(this, "nodeService", this.nodeService);
        PropertyCheck.mandatory(this, "dictionaryService", this.dictionaryService);

        super.afterPropertiesSet();
        this.registry.registerValueInstanceConverter(ScriptNode.class, this);
    }

    public NodeRefToScriptNodeConverter()
    {
        this.javaBaseClass = NodeRef.class;
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
                && (expectedClass.isAssignableFrom(ScriptThumbnail.class) || expectedClass
                        .isAssignableFrom(CategoryNode.class)))
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
        boolean canConvert = value instanceof NodeRef
                && (expectedClass.isAssignableFrom(ScriptThumbnail.class) || expectedClass
                        .isAssignableFrom(CategoryNode.class));

        if (canConvert)
        {
            if (!this.nodeService.exists((NodeRef) value))
            {
                canConvert = false;
            }
            else
            {
                final QName type = this.nodeService.getType((NodeRef) value);
                if (!this.dictionaryService.isSubClass(type, ContentModel.TYPE_CATEGORY)
                        && expectedClass.equals(CategoryNode.class))
                {
                    canConvert = false;
                }
                else if (!(this.dictionaryService.isSubClass(type, ContentModel.TYPE_THUMBNAIL)
                        || this.nodeService.hasAspect((NodeRef) value, RenditionModel.ASPECT_RENDITION))
                        && expectedClass.equals(ScriptThumbnail.class))
                {
                    canConvert = false;
                }
            }
        }
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForScript(final Object value, final ValueConverter globalDelegate,
            final Class<?> expectedClass)
    {
        if (!(value instanceof NodeRef))
        {
            throw new IllegalArgumentException("value must be a " + NodeRef.class);
        }

        final NodeRef nodeRef = (NodeRef) value;

        final QName type = this.nodeService.getType(nodeRef);
        final ScriptNode result;
        if (this.dictionaryService.isSubClass(type, ContentModel.TYPE_CATEGORY)
                && expectedClass.isAssignableFrom(CategoryNode.class))
        {
            result = new CategoryNode(nodeRef, this.serviceRegistry, DUMMY_SCOPE);
        }
        else if (this.dictionaryService.isSubClass(type, ContentModel.TYPE_THUMBNAIL)
                || this.nodeService.hasAspect(nodeRef, RenditionModel.ASPECT_RENDITION))
        {
            result = new ScriptThumbnail(nodeRef, this.serviceRegistry, DUMMY_SCOPE);
        }
        else
        {
            result = new ScriptNode(nodeRef, this.serviceRegistry);
        }

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
        if (ScriptNode.class.isAssignableFrom(valueInstanceClass) && expectedClass.isAssignableFrom(NodeRef.class))
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
        final boolean canConvert = value instanceof ScriptNode && expectedClass.isAssignableFrom(NodeRef.class);
        return canConvert;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value, final ValueConverter globalDelegate,
            final Class<?> expectedClass)
    {
        if (!(value instanceof ScriptNode))
        {
            throw new IllegalArgumentException("value must be a " + ScriptNode.class);
        }

        final ScriptNode scriptNode = (ScriptNode) value;
        final NodeRef nodeRef = scriptNode.getNodeRef();

        return nodeRef;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Pair<Class<?>[], Object[]> determineForScriptProxyConstructorParameters(final Object value,
            final Class<?> expectedClass)
    {
        final Pair<Class<?>[], Object[]> result;

        if (value instanceof ScriptNode)
        {
            final Object[] ctorArguments = new Object[] { ((ScriptNode) value).getNodeRef(), this.serviceRegistry,
                    DUMMY_SCOPE };
            final Class<?>[] ctorArgumentTypes = new Class[] { NodeRef.class, ServiceRegistry.class, Scriptable.class };

            result = new Pair<>(ctorArgumentTypes, ctorArguments);
        }
        else
        {
            result = super.determineForScriptProxyConstructorParameters(value, expectedClass);
        }

        return result;
    }

}
