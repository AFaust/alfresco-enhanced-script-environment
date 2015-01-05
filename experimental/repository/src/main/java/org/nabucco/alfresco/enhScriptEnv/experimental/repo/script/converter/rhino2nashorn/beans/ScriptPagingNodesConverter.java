/*
 * Copyright 2014 PRODYNA AG
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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.rhino2nashorn.beans;

import org.alfresco.repo.jscript.ScriptPagingNodes;
import org.alfresco.util.Pair;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.rhino2nashorn.SimpleJSObjectSubClassProxyConverter;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ScriptPagingNodesConverter extends SimpleJSObjectSubClassProxyConverter
{

    public ScriptPagingNodesConverter()
    {
        this.javaBaseClass = ScriptPagingNodes.class;
        this.confidence = HIGHEST_CONFIDENCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Pair<Class<?>[], Object[]> determineForScriptProxyConstructorParameters(final Object value,
            final Class<?> expectedClass)
    {
        final Pair<Class<?>[], Object[]> result;

        if (value instanceof ScriptPagingNodes)
        {
            final Object[] ctorArguments = new Object[] { ((ScriptPagingNodes) value).getPage(),
                    ((ScriptPagingNodes) value).hasMoreItems(),
                    Integer.valueOf(((ScriptPagingNodes) value).getTotalResultCountLower()),
                    Integer.valueOf(((ScriptPagingNodes) value).getTotalResultCountUpper()) };
            final Class<?>[] ctorArgumentTypes = new Class[] { Scriptable.class, Boolean.class,
                    int.class, int.class };

            result = new Pair<>(ctorArgumentTypes, ctorArguments);
        }
        else
        {
            result = super.determineForScriptProxyConstructorParameters(value, expectedClass);
        }

        return result;
    }

}
