/*
 * Copyright 2016 Axel Faust
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
package org.nabucco.alfresco.enhScriptEnv.repo.script.util;

import org.alfresco.service.cmr.admin.RepoUsage.LicenseMode;
import org.alfresco.service.descriptor.Descriptor;
import org.alfresco.service.descriptor.DescriptorService;
import org.alfresco.util.PropertyCheck;
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

/**
 * @author Axel Faust
 */
public class VersionInfoContributor implements ScopeContributor, InitializingBean
{
    private static final Logger LOGGER = LoggerFactory.getLogger(VersionInfoContributor.class);

    protected static final String KEY_DESCRIPTOR = "DESCRIPTOR";

    protected static final String KEY_EDITION = "EDITION";
    protected static final String KEY_FULL_VERSION = "FULL_VERSION";
    protected static final String KEY_VERSION = "VERSION";
    protected static final String KEY_SCHEMA = "SCHEMA";
    protected static final String KEY_IS_COMMUNITY = "IS_COMMUNITY";

    protected static final WrapFactory DEFAULT_WRAP_FACTORY = new WrapFactory();

    protected EnhancedScriptProcessor<? extends ReferenceScript> scriptProcessor;
    protected DescriptorService descriptorService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        PropertyCheck.mandatory(this, "descriptorService", this.descriptorService);
        PropertyCheck.mandatory(this, "scriptProcessor", this.scriptProcessor);

        this.scriptProcessor.registerScopeContributor(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contributeToScope(final Object scope, final boolean trustworthyScript, final boolean mutableScope)
    {
        if (scope instanceof Scriptable)
        {
            final Context context = Context.enter();
            try
            {
                final Descriptor serverDescriptor = this.descriptorService.getServerDescriptor();
                if (serverDescriptor != null)
                {
                    final NativeObject descriptorObj = new NativeObject();

                    ScriptableObject.defineConstProperty(descriptorObj, KEY_EDITION);
                    ScriptableObject.defineConstProperty(descriptorObj, KEY_FULL_VERSION);
                    ScriptableObject.defineConstProperty(descriptorObj, KEY_VERSION);
                    ScriptableObject.defineConstProperty(descriptorObj, KEY_SCHEMA);
                    ScriptableObject.defineConstProperty(descriptorObj, KEY_IS_COMMUNITY);

                    ScriptableObject.putConstProperty(descriptorObj, KEY_EDITION,
                            DEFAULT_WRAP_FACTORY.wrap(context, (Scriptable) scope, serverDescriptor.getEdition(), String.class));
                    ScriptableObject.putConstProperty(descriptorObj, KEY_FULL_VERSION,
                            DEFAULT_WRAP_FACTORY.wrap(context, (Scriptable) scope, serverDescriptor.getVersion(), String.class));
                    ScriptableObject.putConstProperty(descriptorObj, KEY_VERSION, DEFAULT_WRAP_FACTORY.wrap(context, (Scriptable) scope,
                            serverDescriptor.getVersionNumber().toString(), String.class));
                    ScriptableObject.putConstProperty(descriptorObj, KEY_SCHEMA, DEFAULT_WRAP_FACTORY.wrap(context, (Scriptable) scope,
                            Integer.valueOf(serverDescriptor.getSchema()), Integer.class));

                    final Boolean isCommunity;
                    final Descriptor currentRepositoryDescriptor = this.descriptorService.getCurrentRepositoryDescriptor();
                    final LicenseMode licenseMode = currentRepositoryDescriptor != null ? currentRepositoryDescriptor.getLicenseMode()
                            : serverDescriptor.getLicenseMode();
                    if (licenseMode == LicenseMode.ENTERPRISE || licenseMode == LicenseMode.TEAM)
                    {
                        isCommunity = Boolean.FALSE;
                    }
                    else if (licenseMode == null || licenseMode == LicenseMode.UNKNOWN)
                    {
                        isCommunity = Boolean.TRUE;
                    }
                    else
                    {
                        LOGGER.warn("Unknown / unexpected license mode {} - assuming 'community mode'", licenseMode);
                        isCommunity = Boolean.TRUE;
                    }

                    ScriptableObject.putConstProperty(descriptorObj, KEY_IS_COMMUNITY,
                            DEFAULT_WRAP_FACTORY.wrap(context, (Scriptable) scope, isCommunity, Boolean.class));

                    descriptorObj.sealObject();

                    ScriptableObject.defineConstProperty((Scriptable) scope, KEY_DESCRIPTOR);
                    ScriptableObject.putConstProperty((Scriptable) scope, KEY_DESCRIPTOR, descriptorObj);
                }
            }
            finally
            {
                Context.exit();
            }
        }
    }

    /**
     * @param descriptorService
     *            the descriptorService to set
     */
    public final void setDescriptorService(final DescriptorService descriptorService)
    {
        this.descriptorService = descriptorService;
    }

    /**
     * @param scriptProcessor
     *            the scriptProcessor to set
     */
    public final void setScriptProcessor(final EnhancedScriptProcessor<? extends ReferenceScript> scriptProcessor)
    {
        this.scriptProcessor = scriptProcessor;
    }

}