/**
 * 
 */
package org.nabucco.alfresco.enhScriptEnv.repo.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class LegacyNamePathScriptLocator extends AbstractScriptLocator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyNamePathScriptLocator.class);

    protected ServiceRegistry serviceRegistry;
    protected NamespaceService namespaceService;
    protected NodeService nodeService;
    protected FileFolderService fileFolderService;
    protected SearchService searchService;
    protected ContentService contentService;

    protected StoreRef storeRef;
    protected String storePath;

    /**
     * {@inheritDoc}
     */
    @Override
    public ScriptLocation resolveLocation(final ScriptLocation referenceLocation, final String locationValue)
    {
        final ScriptLocation result;
        if (locationValue.startsWith("/"))
        {
            final NodeRef rootNodeRef = this.nodeService.getRootNode(this.storeRef);
            final List<NodeRef> nodes = this.searchService.selectNodes(rootNodeRef, this.storePath, null, this.namespaceService, false);

            if (nodes.size() == 0)
            {
                throw new AlfrescoRuntimeException("Unable to find store path: " + this.storePath);
            }

            final StringTokenizer tokenizer = new StringTokenizer(locationValue, "/");
            final List<String> elements = new ArrayList<String>(6);
            if (tokenizer.hasMoreTokens())
            {
                tokenizer.nextToken();
            }
            while (tokenizer.hasMoreTokens())
            {
                elements.add(tokenizer.nextToken());
            }

            try
            {
                final FileInfo fileInfo = this.fileFolderService.resolveNamePath(nodes.get(0), elements);
                final NodeRef scriptRef = fileInfo.getNodeRef();
                result = new NodeScriptLocation(this.serviceRegistry, scriptRef, ContentModel.PROP_CONTENT);
            }
            catch (final FileNotFoundException err)
            {
                throw new AlfrescoRuntimeException("Unable to load included script repository resource: " + locationValue);
            }
        }
        else
        {
            result = null;
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ScriptLocation resolveLocation(ScriptLocation referenceLocation, String locationValue, Map<String, Object> resolutionParameters)
    {
        // we currently don't support any parameters, so just pass to default implementation
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
     * @param fileFolderService
     *            the fileFolderService to set
     */
    public final void setFileFolderService(final FileFolderService fileFolderService)
    {
        this.fileFolderService = fileFolderService;
    }

    /**
     * @param storePath
     *            the storePath to set
     */
    public final void setStorePath(final String storePath)
    {
        this.storePath = storePath;
    }

    /**
     * @param storeRef
     *            the storeRef to set
     */
    public final void setStoreRef(final StoreRef storeRef)
    {
        this.storeRef = storeRef;
    }

    /**
     * @param storeRef
     *            the storeRef to set
     */
    public final void setStoreRef(final String storeRef)
    {
        this.storeRef = storeRef != null ? new StoreRef(storeRef) : null;
    }
}
