package org.nabucco.alfresco.enhScriptEnv.repo.script;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ParameterCheck;

/**
 * Script location for a dynamic script maintained as the content of a node within the Alfresco repository.
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class NodeScriptLocation implements ScriptLocation
{

    private final NodeRef node;
    private final QName contentProp;

    private final ServiceRegistry services;

    public NodeScriptLocation(final ServiceRegistry services, final NodeRef node, final QName contentProp)
    {
        ParameterCheck.mandatory("services", services);
        ParameterCheck.mandatory("node", node);
        this.services = services;
        this.node = node;
        this.contentProp = contentProp != null ? contentProp : ContentModel.PROP_CONTENT;
    }

    protected ContentReader getContentReader()
    {
        if (this.services.getNodeService().exists(this.node) == false)
        {
            throw new AlfrescoRuntimeException("Script node does not exist: " + this.node);
        }

        final ContentReader contentReader = this.services.getContentService().getReader(this.node, this.contentProp);
        if (contentReader == null || contentReader.exists() == false)
        {
            throw new AlfrescoRuntimeException("Script node content not found: " + this.node);
        }
        return contentReader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream()
    {
        return getContentReader().getContentInputStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Reader getReader()
    {
        final ContentReader contentReader = getContentReader();
        final String encoding = contentReader.getEncoding();
        final Reader reader;
        try
        {
            if (encoding != null)
            {
                reader = new InputStreamReader(contentReader.getContentInputStream(), encoding);
            }
            else
            {
                reader = new InputStreamReader(contentReader.getContentInputStream());
            }
        }
        catch (IOException e)
        {
            throw new ContentIOException("Failed to create reader with encoding of content", e);
        }
        return reader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath()
    {
        return this.node.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCachable()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecure()
    {
        return false;
    }

    /**
     * @return the node
     */
    public NodeRef getNode()
    {
        return this.node;
    }

    /**
     * @return the contentProp
     */
    public QName getContentProp()
    {
        return this.contentProp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.contentProp == null) ? 0 : this.contentProp.hashCode());
        result = prime * result + ((this.node == null) ? 0 : this.node.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null || !(obj instanceof NodeScriptLocation))
        {
            return false;
        }

        NodeScriptLocation other = (NodeScriptLocation) obj;

        if (this.contentProp == null)
        {
            if (other.contentProp != null)
                return false;
        }
        else if (!this.contentProp.equals(other.contentProp))
            return false;
        if (this.node == null)
        {
            if (other.node != null)
                return false;
        }
        else if (!this.node.equals(other.node))
            return false;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "NodeScriptLocation [node=" + this.node + ", contentProp=" + this.contentProp + "]";
    }

}