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
package org.nabucco.alfresco.enhScriptEnv.common.script.functions;

import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.ScriptImportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Import function to allow inclusion of other scripts in arbitrary contexts of a script in execution. This function utilizes the central
 * registry of script locators supplied by the Rhino script processor as well as its compilation / caching framework for constituent
 * scripts.
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class RhinoImportScriptFunction<Script extends ReferenceScript> extends AbstractImportScriptFunction<Script> implements
        IdFunctionCall
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RhinoImportScriptFunction.class);

    public static final int IMPORT_FUNC_ID = 0;
    public static final Object IMPORT_FUNC_TAG = new Object();
    public static final String IMPORT_FUNC_NAME = "importScript";
    public static final int ARITY = 2;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public Object execIdCall(final IdFunctionObject f, final Context cx, final Scriptable scope, final Scriptable thisObj,
            final Object[] args)
    {
        final boolean result;
        if (f.hasTag(IMPORT_FUNC_TAG))
        {
            if (f.methodId() == IMPORT_FUNC_ID)
            {
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

                // optional parameters
                // defaults to false
                final boolean failOnMissingScript = ScriptRuntime.toBoolean(args, 2);
                // defaults to null
                final Scriptable resolutionParams = ScriptRuntime.toObjectOrNull(cx, args.length > 3 ? args[3] : null);
                final Object resolutionParamsJavaObj = this.valueConverter.convertValueForJava(resolutionParams, Map.class);

                // TODO: Is Scriptable ok as base type for scope? Could be anything, even a function...
                final Scriptable executionScopeParam = ScriptRuntime.toObjectOrNull(cx, args.length > 4 ? args[4] : null);

                result = this.resolveAndImport(locatorType, locationValue, resolutionParamsJavaObj, scope, executionScopeParam,
                        failOnMissingScript);
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
    public void contributeToScope(final Object scope, final boolean trustworthyScript, final boolean mutableScope)
    {
        if (scope instanceof Scriptable)
        {
            final IdFunctionObject func = new IdFunctionObject(this, IMPORT_FUNC_TAG, IMPORT_FUNC_ID, IMPORT_FUNC_NAME, ARITY,
                    (Scriptable) scope);
            func.sealObject();

            // export as read-only and undeleteable property of the scope
            ScriptableObject.defineProperty((Scriptable) scope, IMPORT_FUNC_NAME, func, ScriptableObject.PERMANENT
                    | ScriptableObject.READONLY);
        }
    }

    protected Object prepareExecutionScope(final Script location, final Object sourceScope, final Object executionScopeParam)
    {
        final Object result;
        if (executionScopeParam instanceof Scriptable)
        {
            final Scriptable executionScope = (Scriptable) executionScopeParam;
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
            executionScope.setParentScope(null);
            executionScope.setPrototype(baseScope);

            result = executionScope;
        }
        else
        {
            result = sourceScope;
        }

        return result;
    }
}