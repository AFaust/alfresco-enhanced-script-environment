package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.source;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.scripts.ScriptException;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ScriptURLStreamHandler extends URLStreamHandler
{

    protected final ThreadLocal<Map<URL, ReferenceScript>> scriptByUrl = new ThreadLocal<Map<URL, ReferenceScript>>()
    {

        /**
         * {@inheritDoc}
         */
        @Override
        protected Map<URL, ReferenceScript> initialValue()
        {
            return new HashMap<>();
        }

    };

    public void map(final URL url, final ReferenceScript script)
    {
        this.scriptByUrl.get().put(url, script);
    }

    public void unmap(final URL url)
    {
        this.scriptByUrl.get().remove(url);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected URLConnection openConnection(final URL u) throws IOException
    {
        final URLConnection connection;
        if (this.scriptByUrl.get().containsKey(u))
        {
            connection = new ScriptURLConnection(u, this.scriptByUrl.get().get(u));
        }
        else
        {
            throw new ScriptException("No script mapped to " + u);
        }

        return connection;
    }

}
