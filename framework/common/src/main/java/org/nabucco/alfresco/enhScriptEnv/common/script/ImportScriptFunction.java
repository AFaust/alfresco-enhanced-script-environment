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
package org.nabucco.alfresco.enhScriptEnv.common.script;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.util.ParameterCheck;
import org.alfresco.util.PropertyCheck;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.nabucco.alfresco.enhScriptEnv.common.script.locator.ScriptLocator;
import org.nabucco.alfresco.enhScriptEnv.common.script.locator.ScriptLocatorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Import function to allow inclusion of other scripts in arbitrary contexts of a script in execution. This function utilizes the central
 * registry of script locators supplied by the Rhino script processor as well as its compilation / caching framework for constituent
 * scripts.
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ImportScriptFunction<Script extends ReferenceScript> implements IdFunctionCall, ScriptLocatorRegistry<Script>,
        ScopeContributor, InitializingBean
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportScriptFunction.class);

    public static final int IMPORT_FUNC_ID = 0;
    public static final Object IMPORT_FUNC_TAG = new Object();
    public static final String IMPORT_FUNC_NAME = "importScript";
    public static final int ARITY = 2;

    protected final Map<String, ScriptLocator<Script>> scriptLocators = new HashMap<String, ScriptLocator<Script>>();

    protected EnhancedScriptProcessor<Script> scriptProcessor;
    protected ValueConverter valueConverter;

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterPropertiesSet() throws Exception
    {
        PropertyCheck.mandatory(this, "scriptProcessor", this.scriptProcessor);
        PropertyCheck.mandatory(this, "valueConverter", this.valueConverter);

        this.scriptProcessor.registerScopeContributor(this);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object execIdCall(final IdFunctionObject f, final Context cx, final Scriptable scope, final Scriptable thisObj,
            final Object[] args)
    {
        boolean result = false;
        if (f.hasTag(IMPORT_FUNC_TAG))
        {
            if (f.methodId() == IMPORT_FUNC_ID)
            {
                final ReferenceScript referenceLocation = this.scriptProcessor.getContextScriptLocation();

                if (args.length == 0 || args[0] == Undefined.instance || args[0] == null)
                {
                    throw new IllegalArgumentException("No locator type provided");
                }

                if (args.length < 2 || args[1] == Undefined.instance || args[1] == null)
                {
                    throw new IllegalArgumentException("No location value provided");
                }

                final String locatorType = ScriptRuntime.toString(args, 0);
                final String locationValue = ScriptRuntime.toString(args, 1);

                ParameterCheck.mandatoryString("locatorType", locatorType);
                ParameterCheck.mandatoryString("locationValue", locationValue);

                // optional parameters
                // defaults to false (Alfresco 5.0 with Rhino 1.7R4 removes toBoolean(Object[], int))
                final boolean failOnMissingScript = args.length > 2 ? ScriptRuntime.toBoolean(args[2]) : false;
                // defaults to null
                final Scriptable resolutionParams = ScriptRuntime.toObjectOrNull(cx, args.length > 3 ? args[3] : null);
                final Object resolotionParamsJavaObj = this.valueConverter.convertValueForJava(resolutionParams);

                // TODO: Is Scriptable ok as base type for scope? Could be anything, even a function...
                final Scriptable executionScopeParam = ScriptRuntime.toObjectOrNull(cx, args.length > 4 ? args[4] : null);

                final ScriptLocator<Script> scriptLocator = this.scriptLocators.get(locatorType);
                if (scriptLocator != null)
                {
                    final Script location = this.resolveLocationImpl(referenceLocation, locationValue, resolotionParamsJavaObj, scriptLocator);

                    if (location == null)
                    {
                        LOGGER.info("Unable to resolve script location [{}] via locator [{}]", locationValue, locatorType);

                        if (failOnMissingScript)
                        {
                            // TODO: proper msgId
                            throw new ScriptImportException("Unable to resolve script location [{0}] via locator [{1}]", new Object[] {
                                    locationValue, locatorType });
                        }
                    }
                    else
                    {
                        this.importAndExecute(cx, scope, executionScopeParam, location);
                        result = true;
                    }
                }
                else
                {
                    LOGGER.warn("Unknown script locator [{}]", locatorType);

                    if (failOnMissingScript)
                    {
                        // TODO: proper msgId
                        throw new ScriptImportException("Unknown script locator [{0}]", new Object[] { locatorType });
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerScriptLocator(final String name, final ScriptLocator<Script> scriptLocator)
    {
        final ScriptLocator<Script> replaced = this.scriptLocators.put(name, scriptLocator);
        if (replaced != null)
        {
            LOGGER.warn("ScriptLocator {} overriden by {} with name {}", new Object[] { replaced, scriptLocator, name });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contributeToScope(final Scriptable scope, final boolean trustworthyScript, final boolean mutableScope)
    {
        final IdFunctionObject func = new IdFunctionObject(this, IMPORT_FUNC_TAG, IMPORT_FUNC_ID, IMPORT_FUNC_NAME, ARITY, scope);
        func.sealObject();

        // export as read-only and undeleteable property of the scope
        ScriptableObject.defineProperty(scope, IMPORT_FUNC_NAME, func, ScriptableObject.PERMANENT | ScriptableObject.READONLY);
    }

    /**
     * Sets the scriptProcessor to given scriptProcessor.
     *
     * @param scriptProcessor
     *            the scriptProcessor to set
     */
    public final void setScriptProcessor(final EnhancedScriptProcessor<Script> scriptProcessor)
    {
        this.scriptProcessor = scriptProcessor;
    }

    /**
     * Sets the valueConverter to given valueConverter.
     *
     * @param valueConverter
     *            the valueConverter to set
     */
    public final void setValueConverter(final ValueConverter valueConverter)
    {
        this.valueConverter = valueConverter;
    }

    protected void importAndExecute(final Context cx, final Scriptable scope, final Scriptable executionScopeParam, final Script location)
    {
        final Scriptable executionScope;
        if (executionScopeParam != null)
        {
            executionScope = executionScopeParam;
            final Scriptable baseScope;
            final Object executionScopeObj = this.scriptProcessor.initializeScope(location);
            if (executionScopeObj instanceof Scriptable)
            {
                baseScope = (Scriptable) executionScopeObj;
            }
            else
            {
                throw new ScriptImportException("The script processor provided an incomatible scope for the script {}", location);
            }

            // TODO: protect against complex objects being passed as a context - fail when scope already has prototype?
            executionScopeParam.setParentScope(null);
            executionScopeParam.setPrototype(baseScope);
        }
        else
        {
            // TODO: What about insecure script called from secure one?
            // TODO: How to deny access to Packages/getClass/java without denying access to other scope elements?
            executionScope = scope;
        }

        this.scriptProcessor.executeInScope(location, executionScope);
    }

    protected Script resolveLocationImpl(final ReferenceScript referenceLocation, final String locationValue,
            final Object resolutionParams,
            final ScriptLocator<Script> scriptLocator)
    {
        final Script location;
        if (resolutionParams == null)
        {
            location = scriptLocator.resolveLocation(referenceLocation, locationValue);
        }
        else if (resolutionParams instanceof Map<?, ?>)
        {
            // we know the generic parameters from the way this.valueConverter works
            @SuppressWarnings("unchecked")
            final Map<String, Object> resolotionParamsJavaMap = (Map<String, Object>) resolutionParams;
            location = scriptLocator.resolveLocation(referenceLocation, locationValue, resolotionParamsJavaMap);
        }
        else
        {
            LOGGER.warn(
                    "Invalid parameter object for resolution of script location [{}] via locator [{}] - should have been a string-keyed map: [{}]",
                    new Object[] { locationValue, scriptLocator, resolutionParams });
            location = null;
        }
        return location;
    }

}