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
package org.nabucco.alfresco.enhScriptEnv.share.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alfresco.util.PropertyCheck;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;
import org.nabucco.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.ScopeContributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.extensions.surf.FrameworkBean;
import org.springframework.extensions.surf.LinkBuilder;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.WebFrameworkServiceRegistry;
import org.springframework.extensions.surf.exception.ConnectorServiceException;
import org.springframework.extensions.surf.support.AbstractRequestContext;
import org.springframework.extensions.surf.support.ThreadLocalRequestContext;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.connector.Connector;
import org.springframework.extensions.webscripts.connector.ConnectorService;
import org.springframework.extensions.webscripts.connector.Response;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class VersionInfoContributor implements ScopeContributor, InitializingBean
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionInfoContributor.class);

    protected static final String EDITION_TEAM = "team";
    protected static final String EDITION_COMMUNITY = "community";
    protected static final String EDITION_ENTERPRISE = "enterprise";
    protected static final String EDITION_UNKNOWN = "unknown";

    protected static final String KEY_DESCRIPTOR = "DESCRIPTOR";

    protected static final String KEY_EDITION = "EDITION";
    protected static final String KEY_FULL_VERSION = "FULL_VERSION";
    protected static final String KEY_VERSION = "VERSION";
    protected static final String KEY_SCHEMA = "SCHEMA";
    protected static final String KEY_IS_COMMUNITY = "IS_COMMUNITY";

    protected static final WrapFactory DEFAULT_WRAP_FACTORY = new WrapFactory();

    /**
     * Default interval between version information update checks (24 hours by default, since version upgrades while Share is running are
     * rather infrequent)
     */
    protected static final long DEFAULT_UPDATE_CHECK_INTERVAL = 24 * 60 * 60 * 1000;

    protected static final String CORE_VERSION_PATTERN = "^(\\d+(\\.\\d+)+)";

    protected ConnectorService connectorService;

    protected WebFrameworkServiceRegistry serviceRegistry;

    protected FrameworkBean frameworkUtil;

    protected EnhancedScriptProcessor<? extends ReferenceScript> scriptProcessor;

    protected final Map<String, String> cachedInformation = new HashMap<String, String>();

    protected long lastUpdateCheckTimestamp = 0l;

    protected long nextUpdateInterval = DEFAULT_UPDATE_CHECK_INTERVAL;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "connectorService", this.connectorService);
        PropertyCheck.mandatory(this, "scriptProcessor", this.scriptProcessor);

        PropertyCheck.mandatory(this, "serviceRegistry", this.serviceRegistry);
        PropertyCheck.mandatory(this, "frameworkUtil", this.frameworkUtil);

        this.scriptProcessor.registerScopeContributor(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contributeToScope(final Scriptable scope, final boolean trustworthyScript, final boolean mutableScope)
    {
        final Context context = Context.enter();
        try
        {
            // simple cache check and update / initialization
            final long now = System.currentTimeMillis();
            if (now - this.lastUpdateCheckTimestamp > this.nextUpdateInterval)
            {
                synchronized (this)
                {
                    if (now - this.lastUpdateCheckTimestamp > this.nextUpdateInterval)
                    {
                        this.retrieveReposioryVersionInfo();
                    }
                }
            }

            if (!this.cachedInformation.isEmpty())
            {
                final NativeObject descriptorObj = new NativeObject();

                ScriptableObject.defineConstProperty(descriptorObj, KEY_EDITION);
                ScriptableObject.defineConstProperty(descriptorObj, KEY_FULL_VERSION);
                ScriptableObject.defineConstProperty(descriptorObj, KEY_VERSION);
                ScriptableObject.defineConstProperty(descriptorObj, KEY_SCHEMA);
                ScriptableObject.defineConstProperty(descriptorObj, KEY_IS_COMMUNITY);

                ScriptableObject.putConstProperty(descriptorObj, KEY_EDITION,
                        DEFAULT_WRAP_FACTORY.wrap(context, scope, this.cachedInformation.get(KEY_EDITION), String.class));
                ScriptableObject.putConstProperty(descriptorObj, KEY_FULL_VERSION,
                        DEFAULT_WRAP_FACTORY.wrap(context, scope, this.cachedInformation.get(KEY_FULL_VERSION), String.class));
                ScriptableObject.putConstProperty(descriptorObj, KEY_VERSION,
                        DEFAULT_WRAP_FACTORY.wrap(context, scope, this.cachedInformation.get(KEY_VERSION), String.class));
                ScriptableObject.putConstProperty(descriptorObj, KEY_SCHEMA, DEFAULT_WRAP_FACTORY.wrap(context, scope,
                        this.cachedInformation.containsKey(KEY_SCHEMA) ? this.cachedInformation.get(KEY_SCHEMA) : Integer.valueOf(-1),
                        Integer.class));

                // assume community if flag not set
                ScriptableObject.putConstProperty(descriptorObj, KEY_IS_COMMUNITY, DEFAULT_WRAP_FACTORY
                        .wrap(context,
                                scope,
                                this.cachedInformation.containsKey(KEY_IS_COMMUNITY) ? Boolean.valueOf(this.cachedInformation
                                        .get(KEY_IS_COMMUNITY)) : Boolean.TRUE, Boolean.class));

                descriptorObj.sealObject();

                ScriptableObject.defineConstProperty(scope, KEY_DESCRIPTOR);
                ScriptableObject.putConstProperty(scope, KEY_DESCRIPTOR, descriptorObj);
            }
        }
        finally
        {
            Context.exit();
        }
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
     * @param scriptProcessor
     *            the scriptProcessor to set
     */
    public final void setScriptProcessor(final EnhancedScriptProcessor<? extends ReferenceScript> scriptProcessor)
    {
        this.scriptProcessor = scriptProcessor;
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

    protected void retrieveReposioryVersionInfo()
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

                final Response response = connector.call("/api/server");
                if (response.getStatus().getCode() == Status.STATUS_OK)
                {
                    try
                    {
                        final JSONObject json = new JSONObject(new JSONTokener(response.getResponse()));

                        final JSONObject data = json.getJSONObject("data");
                        if (data != null)
                        {
                            final String edition = data.getString("edition");
                            final String schema = data.getString("schema");
                            final String version = data.getString("version");

                            final String coreVersion;
                            final Matcher versionMatcher = Pattern.compile(CORE_VERSION_PATTERN).matcher(version);
                            if (versionMatcher.matches())
                            {
                                coreVersion = versionMatcher.group(1);
                            }
                            else
                            {
                                coreVersion = null;
                            }

                            this.cachedInformation.put(KEY_EDITION, edition);
                            this.cachedInformation.put(KEY_FULL_VERSION, version);
                            this.cachedInformation.put(KEY_VERSION, coreVersion);
                            this.cachedInformation.put(KEY_SCHEMA, schema);

                            final Boolean isCommunity;
                            if (EDITION_ENTERPRISE.equalsIgnoreCase(edition) || EDITION_TEAM.equalsIgnoreCase(edition))
                            {
                                isCommunity = Boolean.FALSE;
                            }
                            else if (EDITION_UNKNOWN.equalsIgnoreCase(edition) || EDITION_COMMUNITY.equalsIgnoreCase(edition)
                                    || edition == null || edition.trim().length() == 0)
                            {
                                isCommunity = Boolean.TRUE;
                            }
                            else
                            {
                                LOGGER.warn("Unknown / unexpected edition {} - assuming 'community mode'", edition);
                                isCommunity = Boolean.TRUE;
                            }
                            this.cachedInformation.put(KEY_IS_COMMUNITY, isCommunity.toString());

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
