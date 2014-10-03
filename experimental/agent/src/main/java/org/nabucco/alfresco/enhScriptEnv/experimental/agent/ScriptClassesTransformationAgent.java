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
package org.nabucco.alfresco.enhScriptEnv.experimental.agent;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ScriptClassesTransformationAgent
{

    /**
     * Registers instrumentation before main program is called.
     *
     * @param args
     *            arguments for the agent
     * @param inst
     *            the instrumentation API
     */
    public static void premain(final String args, final Instrumentation inst)
    {
        final Map<String, String> arguments = processArguments(args);

        inst.addTransformer(new ChangeClassFinalityTransformer(arguments));
    }

    protected static Map<String, String> processArguments(final String agentArgs)
    {
        final Map<String, String> arguments = new HashMap<String, String>();

        if (agentArgs != null)
        {
            final String[] individualAgentArgs;
            if (agentArgs.indexOf(';') != -1)
            {
                individualAgentArgs = agentArgs.split(";");
            }
            else
            {
                individualAgentArgs = new String[] { agentArgs };
            }

            for (final String arg : individualAgentArgs)
            {
                if (arg.indexOf('=') != -1 && arg.indexOf('=') == arg.length() - 1)
                {
                    arguments.put(arg.substring(0, arg.indexOf('=')), arg.substring(arg.indexOf('=') + 1));
                }
                else
                {
                    arguments.put(arg, "");
                }
            }
        }

        return arguments;
    }
}
