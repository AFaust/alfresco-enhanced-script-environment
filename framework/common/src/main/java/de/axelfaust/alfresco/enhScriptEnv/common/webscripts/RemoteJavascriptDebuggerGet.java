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
package de.axelfaust.alfresco.enhScriptEnv.common.webscripts;

import java.util.HashMap;
import java.util.Map;

import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * @author Axel Faust
 */
public class RemoteJavascriptDebuggerGet extends DeclarativeWebScript
{

    private RemoteScriptDebugger debugger;

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected Map<String, Object> executeImpl(final WebScriptRequest req, final Status status)
    {
        // construct model
        final Map<String, Object> model = new HashMap<String, Object>(7, 1.0f);
        model.put("visible", Boolean.valueOf(this.debugger.isActive()));
        return model;
    }

    /**
     * @param debugger
     *            the debugger to set
     */
    public final void setDebugger(final RemoteScriptDebugger debugger)
    {
        this.debugger = debugger;
    }

}
