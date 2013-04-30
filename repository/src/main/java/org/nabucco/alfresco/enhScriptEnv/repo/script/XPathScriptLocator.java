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

import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Script location for a dynamic script maintained as the content of a node within the Alfresco repository, based upon resolutiuon using
 * {@link SearchService#LANGUAGE_XPATH XPath} queries.
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class XPathScriptLocator extends AbstractScriptLocator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XPathScriptLocator.class);

    protected ServiceRegistry serviceRegistry;
    protected NamespaceService namespaceService;
    protected NodeService nodeService;
    protected ContentService contentService;
    protected SearchService searchService;

    protected StoreRef defaultStoreRef;

    /**
     * {@inheritDoc}
     */
    @Override
    public ScriptLocation resolveLocation(final ScriptLocation referenceLocation, final String locationValue)
    {
        final ScriptLocation result;
        LOGGER.debug("Resolving {} from reference location {}", locationValue, referenceLocation);

        final List<NodeRef> nodes;
        QName contentProp = ContentModel.PROP_CONTENT;
        if (locationValue != null)
        {
            if (referenceLocation instanceof NodeScriptLocation)
            {
                final NodeScriptLocation referenceNodeLocation = (NodeScriptLocation) referenceLocation;
                final NodeRef referenceNode = referenceNodeLocation.getNode();
                contentProp = referenceNodeLocation.getContentProp();

                nodes = this.searchService.selectNodes(referenceNode, locationValue, null, this.namespaceService, true);
            }
            else
            {
                // TODO: what about RepoScriptContent wrapping RepositoryScriptLocation?
                // treat as context free query
                nodes = this.searchService.selectNodes(this.nodeService.getRootNode(this.defaultStoreRef), locationValue, null,
                        this.namespaceService, true);
            }
        }
        else
        {
            nodes = null;
        }

        if (nodes != null && nodes.size() == 1)
        {

            final NodeRef scriptNode = nodes.get(0);

            // check if we can use the same content property as for the reference
            final ContentReader contentReader = this.contentService.getReader(scriptNode, contentProp);
            if (contentReader != null && contentReader.exists())
            {
                result = new NodeScriptLocation(this.serviceRegistry, scriptNode, contentProp);
            }
            else
            {
                // fall back to default content property
                // TODO: maybe search content properties for a JavaScript one
                // TODO: maybe enhance API to allow importing script to specify the property
                result = new NodeScriptLocation(this.serviceRegistry, scriptNode, ContentModel.PROP_CONTENT);
            }

        }
        else
        {
            result = null;
        }

        LOGGER.debug("Resolved {} based on location value {} from reference location {}", new Object[] { result, locationValue,
                referenceLocation });

        return result;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public ScriptLocation resolveLocation(final ScriptLocation referenceLocation, final String locationValue,
            final Map<String, Object> resolutionParameters)
    {
        // we currently don't support any parameters, so just pass to default implementation
        // TODO: support parameters for dynamic content property evaluation
        if (resolutionParameters != null)
        {
            LOGGER.info(
                    "Implementation does not support resolution parameters - resolution of path {} from reference location {1} will continue with default implementation",
                    locationValue, referenceLocation);
        }
        return resolveLocation(referenceLocation, locationValue);
    }

    /**
     * @param serviceRegistry
     *            the serviceRegistry to set
     */
    public void setServiceRegistry(final ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * @param namespaceService
     *            the namespaceService to set
     */
    public final void setNamespaceService(final NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    /**
     * @param nodeService
     *            the nodeService to set
     */
    public final void setNodeService(final NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    /**
     * @param contentService
     *            the contentService to set
     */
    public final void setContentService(final ContentService contentService)
    {
        this.contentService = contentService;
    }

    /**
     * @param searchService
     *            the searchService to set
     */
    public final void setSearchService(final SearchService searchService)
    {
        this.searchService = searchService;
    }

    /**
     * @param defaultStoreRef
     *            the defaultStoreRef to set
     */
    public final void setDefaultStoreRef(final StoreRef defaultStoreRef)
    {
        this.defaultStoreRef = defaultStoreRef;
    }

    /**
     * @param defaultStoreRef
     *            the defaultStoreRef to set
     */
    public final void setDefaultStoreRef(final String defaultStoreRef)
    {
        this.defaultStoreRef = defaultStoreRef != null ? new StoreRef(defaultStoreRef) : null;
    }
}