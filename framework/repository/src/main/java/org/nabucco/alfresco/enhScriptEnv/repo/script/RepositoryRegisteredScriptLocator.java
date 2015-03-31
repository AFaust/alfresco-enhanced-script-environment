/*
 * Copyright 2015 PRODYNA AG
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
package org.nabucco.alfresco.enhScriptEnv.repo.script;

import org.alfresco.service.cmr.admin.RepoUsage.LicenseMode;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.descriptor.Descriptor;
import org.alfresco.service.descriptor.DescriptorService;
import org.alfresco.util.PropertyCheck;
import org.nabucco.alfresco.enhScriptEnv.common.script.locator.RegisteredScriptLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class RepositoryRegisteredScriptLocator extends RegisteredScriptLocator<ScriptLocation, ScriptLocationAdapter>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryRegisteredScriptLocator.class);

    protected DescriptorService descriptorService;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet()
    {
        super.afterPropertiesSet();

        PropertyCheck.mandatory(this, "descriptorService", this.descriptorService);
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
     *
     * {@inheritDoc}
     */
    @Override
    protected ScriptLocationAdapter convert(final ScriptLocation baseScript)
    {
        return new ScriptLocationAdapter(baseScript);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected boolean isCommunityEdition()
    {
        final boolean result;
        final Descriptor currentRepositoryDescriptor = this.descriptorService.getCurrentRepositoryDescriptor();
        if (currentRepositoryDescriptor == null)
        {
            result = true;
        }
        else
        {
            final LicenseMode licenseMode = currentRepositoryDescriptor.getLicenseMode();
            if (licenseMode == LicenseMode.ENTERPRISE || licenseMode == LicenseMode.TEAM)
            {
                result = false;
            }
            else if (licenseMode == null || licenseMode == LicenseMode.UNKNOWN)
            {
                result = true;
            }
            else
            {
                LOGGER.warn("Unknown / unexpected license mode {} - assuming 'community mode'", licenseMode);
                result = true;
            }
        }

        return result;
    }
}
