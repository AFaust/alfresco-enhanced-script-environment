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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.converter.nashorn;

import java.util.Date;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import jdk.nashorn.internal.objects.NativeDate;

/**
 * Converter handling Nashorn-specific {@link NativeDate native date} to Java {@link Date} conversion.
 *
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
@SuppressWarnings("restriction")
public class NativeDateConverter extends AbstractScriptEvaluationBasedConverter
{
    private static final String CONVERT_NASHORN_TO_JAVA = "javaObj = new (Java.type('java.lang.Date'))(nashornObj.getTime());";

    private static final String CONVERT_JAVA_TO_NASHORN = "nashornObj = new Date(javaObjObj.getTime());";

    protected NativeDateConverter()
    {
        super();
        this.javaBaseClass = Date.class;
        this.nashornBaseClass = NativeDate.class;
    }

    @Override
    protected void executeForScriptConversionScript(final ScriptContext ctx, final ScriptEngine scriptEngine)
            throws javax.script.ScriptException
    {
        scriptEngine.eval(CONVERT_JAVA_TO_NASHORN, ctx);
    }

    @Override
    protected void executeForJavaConversionScript(final ScriptContext ctx, final ScriptEngine scriptEngine)
            throws javax.script.ScriptException
    {
        scriptEngine.eval(CONVERT_NASHORN_TO_JAVA, ctx);
    }

}
