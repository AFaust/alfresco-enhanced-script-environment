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
package org.nabucco.alfresco.enhScriptEnv.repo.script.util;

import org.alfresco.service.cmr.admin.RepoUsage.LicenseMode;
import org.alfresco.service.descriptor.Descriptor;
import org.alfresco.service.descriptor.DescriptorService;
import org.alfresco.util.PropertyCheck;
import org.alfresco.util.VersionNumber;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;
import org.nabucco.alfresco.enhScriptEnv.common.script.EnhancedScriptProcessor;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.ScopeContributor;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class VersionInfoContributor implements ScopeContributor, InitializingBean
{
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
    public void contributeToScope(final Scriptable scope, final boolean trustworthyScript, final boolean mutableScope)
    {
        final Context context = Context.enter();
        try
        {
            final Descriptor currentRepositoryDescriptor = this.descriptorService.getCurrentRepositoryDescriptor();
            if (currentRepositoryDescriptor != null)
            {
                final VersionNumber versionNumber = currentRepositoryDescriptor.getVersionNumber();

                final NativeObject descriptorObj = new NativeObject();

                ScriptableObject.defineConstProperty(descriptorObj, "VERSION");
                ScriptableObject.defineConstProperty(descriptorObj, "DESCRIPTOR");
                ScriptableObject.defineConstProperty(descriptorObj, "IS_COMMUNITY");

                ScriptableObject.putConstProperty(descriptorObj, "VERSION",
                        DEFAULT_WRAP_FACTORY.wrap(context, scope, versionNumber.toString(), String.class));
                ScriptableObject.putConstProperty(descriptorObj, "DESCRIPTOR",
                        DEFAULT_WRAP_FACTORY.wrap(context, scope, currentRepositoryDescriptor, descriptorObj.getClass()));

                final Boolean isCommunity;
                if (currentRepositoryDescriptor.getLicenseMode() == null
                        || currentRepositoryDescriptor.getLicenseMode() == LicenseMode.UNKNOWN)
                {
                    isCommunity = Boolean.TRUE;
                }
                else
                {
                    isCommunity = Boolean.FALSE;
                }

                ScriptableObject.putConstProperty(descriptorObj, "IS_COMMUNITY",
                        DEFAULT_WRAP_FACTORY.wrap(context, scope, isCommunity, descriptorObj.getClass()));

                descriptorObj.sealObject();

                ScriptableObject.defineConstProperty(scope, "REPOSITORY");
                ScriptableObject.putConstProperty(scope, "REPOSITORY", descriptorObj);
            }
        }
        finally
        {
            Context.exit();
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

}
