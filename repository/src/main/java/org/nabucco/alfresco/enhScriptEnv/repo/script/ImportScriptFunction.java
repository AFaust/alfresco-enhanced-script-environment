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

import java.util.Map;

import org.alfresco.scripts.ScriptException;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import function to allow inclusion of other scripts in arbitrary contexts of a script in execution. This function utilizes the central
 * registry of script locators supplied by the Rhino script processor as well as its compilation / caching framework for constituent
 * scripts.
 * 
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ImportScriptFunction extends AbstractFunction
{
    private static final long serialVersionUID = 5343930372054757549L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportScriptFunction.class);

    public static final int IMPORT_FUNC_ID = 0;
    public static final Object IMPORT_FUNC_TAG = new Object();
    public static final String IMPORT_FUNC_NAME = "importScript";
    public static final int ARITY = 3;

    private final EnhancedScriptProcessor scriptProcessor;
    private final Map<String, ScriptLocator> scriptLocators;

    /**
     * @param scriptLocators
     */
    public ImportScriptFunction(final EnhancedScriptProcessor scriptProcessor, final Map<String, ScriptLocator> scriptLocators)
    {
        super();
        this.scriptProcessor = scriptProcessor;
        this.scriptLocators = scriptLocators;
    }

    @Override
    public String getClassName()
    {
        return ImportScriptFunction.class.getName();
    }

    @Override
    public Object execIdCall(final IdFunctionObject f, final Context cx, final Scriptable scope, final Scriptable thisObj,
            final Object[] args)
    {
        boolean result = false;
        if (f.hasTag(IMPORT_FUNC_TAG))
        {
            if (f.methodId() == IMPORT_FUNC_ID)
            {
                final ScriptLocation referenceLocation = this.scriptProcessor.getContextScriptLocation();

                final String locatorType = ScriptRuntime.toString(args, 0);
                final String locationValue = ScriptRuntime.toString(args, 1);
                final boolean failOnMissingScript = ScriptRuntime.toBoolean(args, 2);

                // TODO: introduce optional parameters, i.e. resolution parameters, explicit execution scope

                final ScriptLocator scriptLocator = this.scriptLocators.get(locatorType);
                if (scriptLocator != null)
                {
                    // TODO: if resolution parameters have been passed from script, pass them to the locator
                    final ScriptLocation location = scriptLocator.resolveLocation(referenceLocation, locationValue);

                    if (location == null)
                    {
                        LOGGER.info("Unable to resolve script location [{0}] via locator [{1}]", locationValue, locatorType);

                        if (failOnMissingScript)
                        {
                            // TODO: proper msgId
                            throw new ScriptException("Unable to resolve script location [{0}] via locator [{1}]", new Object[] {
                                    locationValue, locatorType });
                        }
                    }
                    else
                    {
                        this.scriptProcessor.executeInScope(location, scope);
                        result = true;
                    }
                }
                else
                {
                    LOGGER.warn("Unknown script locator [{0}]", locatorType);

                    if (failOnMissingScript)
                    {
                        // TODO: proper msgId
                        throw new ScriptException("Unknown script locator [{0}]", new Object[] { locatorType });
                    }
                }
            }
            else
            {
                throw new IllegalArgumentException(String.valueOf(f.methodId()));
            }
        }
        else
        {
            throw f.unknown();
        }

        return Boolean.valueOf(result);
    }

}