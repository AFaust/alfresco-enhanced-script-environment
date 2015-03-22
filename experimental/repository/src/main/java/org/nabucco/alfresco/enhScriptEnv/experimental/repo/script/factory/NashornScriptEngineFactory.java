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
package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.factory;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class NashornScriptEngineFactory extends AbstractFactoryBean<ScriptEngine>
{

    protected boolean persistentCache = true;
    protected boolean strict = false;
    protected boolean verifyCode = true;
    protected boolean dumpOnError = true;

    protected String timezone;
    protected String locale;

    /**
     * @param persistentCache
     *            the persistentCache to set
     */
    public void setPersistentCache(final boolean persistentCache)
    {
        this.persistentCache = persistentCache;
    }

    /**
     * @param strict
     *            the strict to set
     */
    public void setStrict(final boolean strict)
    {
        this.strict = strict;
    }

    /**
     * @param verifyCode
     *            the verifyCode to set
     */
    public void setVerifyCode(final boolean verifyCode)
    {
        this.verifyCode = verifyCode;
    }

    /**
     * @param dumpOnError
     *            the dumpOnError to set
     */
    public void setDumpOnError(final boolean dumpOnError)
    {
        this.dumpOnError = dumpOnError;
    }

    /**
     * @param timezone
     *            the timezone to set
     */
    public void setTimezone(final String timezone)
    {
        this.timezone = timezone;
    }

    /**
     * @param locale
     *            the locale to set
     */
    public void setLocale(final String locale)
    {
        this.locale = locale;
    }

    @Override
    public Class<?> getObjectType()
    {
        return ScriptEngine.class;
    }

    @Override
    @SuppressWarnings("restriction")
    // not possible to provide options any other way (except system properties)
    protected ScriptEngine createInstance() throws Exception
    {
        final jdk.nashorn.api.scripting.NashornScriptEngineFactory factory = new jdk.nashorn.api.scripting.NashornScriptEngineFactory();

        final List<String> args = new ArrayList<String>();

        // NOTE: For all options see jdk.nashorn.internal.runtime.resources/Options.properties

        // this is always required for backwards-compatibility for existing scripts
        args.add("--const-as-var=true");

        // never provide scripting extensions
        args.add("-scripting=false");

        // this is true by default, but be explicit
        args.add("--debug-lines=true");
        // this has no default
        args.add("--debug-locals=true");

        args.add("-doe=" + String.valueOf(this.dumpOnError));
        args.add("-pcc=" + String.valueOf(this.persistentCache));
        args.add("--verify-code=" + String.valueOf(this.verifyCode));
        args.add("-strict=" + String.valueOf(this.strict));

        // TODO check with nashorn-dev if at some point it will be possible to have timezone as variable context
        if (this.timezone != null && !this.timezone.trim().isEmpty())
        {
            args.add("-t=" + this.timezone);
        }

        // TODO check with nashorn-dev if at some point it will be possible to have locale as variable context
        if (this.locale != null && !this.locale.trim().isEmpty())
        {
            args.add("-l=" + this.locale);
        }

        args.add("--optimistic-types=yes");

        return factory.getScriptEngine(args.toArray(new String[0]));
    }

}
