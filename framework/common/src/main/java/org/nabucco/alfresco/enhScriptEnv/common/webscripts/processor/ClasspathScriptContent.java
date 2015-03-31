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
package org.nabucco.alfresco.enhScriptEnv.common.webscripts.processor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.MessageFormat;

import org.alfresco.util.ParameterCheck;
import org.springframework.extensions.surf.exception.WebScriptsPlatformException;
import org.springframework.extensions.webscripts.ScriptContent;
import org.springframework.extensions.webscripts.WebScriptException;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ClasspathScriptContent implements ScriptContent
{
    private final String path;

    public ClasspathScriptContent(final String path)
    {
        ParameterCheck.mandatory("path", path);
        this.path = path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream()
    {
        final InputStream stream = getClass().getClassLoader().getResourceAsStream(this.path);
        if (stream == null)
        {
            throw new WebScriptException(MessageFormat.format("Unable to retrieve input stream for script {0}", getPathDescription()));
        }
        return stream;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Reader getReader()
    {
        try
        {
            return new InputStreamReader(getInputStream(), "UTF-8");
        }
        catch (final UnsupportedEncodingException e)
        {
            throw new WebScriptsPlatformException("Unsupported Encoding", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath()
    {
        final URL resource = getClass().getClassLoader().getResource(this.path);
        if (resource == null)
        {
            throw new WebScriptException(MessageFormat.format("Unable to retrieve resource for script {0}", getPathDescription()));
        }
        final String path = resource.toExternalForm();
        return path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPathDescription()
    {
        return "classpath*:" + this.path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCachable()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecure()
    {
        return true;
    }

}
