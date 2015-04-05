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
package org.nabucco.alfresco.enhScriptEnv.common.script.aop;

/**
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public enum ScriptableMethodName
{
    GET, HAS, PUT, DELETE, GETPROTOTYPE, SETPROTOTYPE, GETPARENTSCOPE, SETPARENTSCOPE, GETIDS, GETDEFAULVALUE, HASINSTANCE, UNKNOWN;

    protected static ScriptableMethodName methodLiteralOf(final String methodName)
    {
        ScriptableMethodName value = UNKNOWN;

        for (final ScriptableMethodName literal : values())
        {
            if (literal.name().equalsIgnoreCase(methodName))
            {
                value = literal;
                break;
            }
        }

        return value;
    }
}