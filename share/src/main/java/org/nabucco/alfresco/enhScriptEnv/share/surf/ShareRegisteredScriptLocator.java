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
package org.nabucco.alfresco.enhScriptEnv.share.surf;

import org.alfresco.util.PropertyCheck;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.nabucco.alfresco.enhScriptEnv.common.webscripts.processor.SurfRegisteredScriptLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.surf.FrameworkBean;
import org.springframework.extensions.surf.LinkBuilder;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.WebFrameworkServiceRegistry;
import org.springframework.extensions.surf.exception.ConnectorServiceException;
import org.springframework.extensions.surf.support.AbstractRequestContext;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.connector.Connector;
import org.springframework.extensions.webscripts.connector.ConnectorContext;
import org.springframework.extensions.webscripts.connector.ConnectorService;
import org.springframework.extensions.webscripts.connector.Response;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ShareRegisteredScriptLocator extends SurfRegisteredScriptLocator
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ShareRegisteredScriptLocator.class);

    protected static final String EDITION_TEAM = "team";
    protected static final String EDITION_COMMUNITY = "community";
    protected static final String EDITION_ENTERPRISE = "enterprise";
    protected static final String EDITION_UNKNOWN = "unknown";

    /**
     * Default interval between version information update checks (24 hours by default, since version upgrades while Share is running are
     * rather infrequent)
     */
    protected static final long DEFAULT_UPDATE_CHECK_INTERVAL = 24 * 60 * 60 * 1000;

    protected ConnectorService connectorService;

    protected WebFrameworkServiceRegistry serviceRegistry;

    protected FrameworkBean frameworkUtil;

    // default is community
    protected boolean isCommunity = true;

    protected long lastUpdateCheckTimestamp = 0l;

    protected long nextUpdateInterval = DEFAULT_UPDATE_CHECK_INTERVAL;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        super.afterPropertiesSet();
        PropertyCheck.mandatory(this, "connectorService", this.connectorService);

        PropertyCheck.mandatory(this, "serviceRegistry", this.serviceRegistry);
        PropertyCheck.mandatory(this, "frameworkUtil", this.frameworkUtil);
    }

    /**
     * @param connectorService
     *            the connectorService to set
     */
    public final void setConnectorService(final ConnectorService connectorService)
    {
        this.connectorService = connectorService;
    }

    /**
     * @param serviceRegistry
     *            the serviceRegistry to set
     */
    public final void setServiceRegistry(final WebFrameworkServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * @param frameworkUtil
     *            the frameworkUtil to set
     */
    public final void setFrameworkUtil(final FrameworkBean frameworkUtil)
    {
        this.frameworkUtil = frameworkUtil;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected boolean isCommunityEdition()
    {
        // simple cache check and update / initialization
        final long now = System.currentTimeMillis();
        if (now - this.lastUpdateCheckTimestamp > this.nextUpdateInterval)
        {
            synchronized (this)
            {
                if (now - this.lastUpdateCheckTimestamp > this.nextUpdateInterval)
                {
                    this.retrieveReposioryEditionInfo();
                }
            }
        }

        return this.isCommunity;
    }

    protected void retrieveReposioryEditionInfo()
    {
        try
        {
            final Connector connector = this.connectorService.getConnector("alfresco");

            // workaround NPE-bugs in RequestCachingConnector when calling "call" outside of active request
            final boolean fakedRequestContext;
            RequestContext requestContext = ThreadLocalRequestContext.getRequestContext();
            if (requestContext == null)
            {
                fakedRequestContext = true;
                requestContext = new AbstractRequestContext(this.serviceRegistry, this.frameworkUtil)
                {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public LinkBuilder getLinkBuilder()
                    {
                        return null;
                    }
                };
            }
            else
            {
                fakedRequestContext = false;
            }

            try
            {

                final Response response = connector.call("/api/server", new ConnectorContext());
                if (response.getStatus().getCode() == Status.STATUS_OK)
                {
                    try
                    {
                        final JSONObject json = new JSONObject(new JSONTokener(response.getResponse()));

                        final JSONObject data = json.getJSONObject("data");
                        if (data != null)
                        {
                            final String edition = data.getString("edition");

                            final boolean isCommunity;
                            if (EDITION_ENTERPRISE.equalsIgnoreCase(edition) || EDITION_TEAM.equalsIgnoreCase(edition))
                            {
                                isCommunity = false;
                            }
                            else if (EDITION_UNKNOWN.equalsIgnoreCase(edition) || EDITION_COMMUNITY.equalsIgnoreCase(edition)
                                    || edition == null || edition.trim().length() == 0)
                            {
                                isCommunity = true;
                            }
                            else
                            {
                                LOGGER.warn("Unknown / unexpected edition {} - assuming 'community mode'", edition);
                                isCommunity = true;
                            }
                            this.isCommunity = isCommunity;

                            this.lastUpdateCheckTimestamp = System.currentTimeMillis();
                            this.nextUpdateInterval = DEFAULT_UPDATE_CHECK_INTERVAL;
                        }
                        else
                        {
                            this.lastUpdateCheckTimestamp = System.currentTimeMillis();
                            // check again in a minute
                            this.nextUpdateInterval = 1000 * 60;
                            LOGGER.warn("Unexpected response content from /api/server");
                        }
                    }
                    catch (final JSONException jsonEx)
                    {
                        this.lastUpdateCheckTimestamp = System.currentTimeMillis();
                        // check again in a minute
                        this.nextUpdateInterval = 1000 * 60;
                        LOGGER.error("Error parsing version information from Repository", jsonEx);
                    }
                }
                else
                {
                    this.lastUpdateCheckTimestamp = System.currentTimeMillis();
                    // check again in a minute
                    this.nextUpdateInterval = 1000 * 60;
                    LOGGER.warn("Failed to load version information from Repository: " + response.getStatus());
                }
            }
            finally
            {
                if (fakedRequestContext && requestContext != null)
                {
                    requestContext.release();
                }
            }
        }
        catch (final ConnectorServiceException conEx)
        {
            this.lastUpdateCheckTimestamp = System.currentTimeMillis();
            // check again in a minute
            this.nextUpdateInterval = 1000 * 60;

            LOGGER.error("Error retrieving connector for version information query to Repository", conEx);
        }
    }
}
