package org.nabucco.alfresco.enhScriptEnv.experimental.repo.script.source;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.alfresco.util.ParameterCheck;
import org.apache.tools.ant.filters.StringInputStream;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript;
import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript.CommonReferencePath;
import org.springframework.util.FileCopyUtils;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ScriptURLConnection extends URLConnection
{

    private static final String NODE_REF_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s*+)*=(\\s*\\n*\\s+)*\"(([^:]+)://([^/]+)/([^\"]+))\"(\\s*\\n*\\s+)*(/)?>";
    private static final String NODE_REF_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"node\", \"$4\", true);";

    private static final String LEGACY_NAME_PATH_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s+)*=(\\s*\\n*\\s+)*\"(/[^\"]+)\"(\\s*\\n*\\s+)*(/)?>";
    private static final String LEGACY_NAME_PATH_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"legacyNamePath\", \"$4\", true);";

    private static final String CLASSPATH_RESOURCE_IMPORT_PATTERN = "<import(\\s*\\n*\\s+)+resource(\\s*\\n*\\s+)*=(\\s*\\n*\\s+)*\"classpath:(/)?([^\"]+)\"(\\s*\\n*\\s+)*(/)?>";
    private static final String CLASSPATH_RESOURCE_IMPORT_REPLACEMENT = "importScript(\"classpath\", \"/$5\", true);";

    protected final ReferenceScript script;

    protected String processedSource;

    public ScriptURLConnection(final URL url, final ReferenceScript script)
    {
        super(url);
        ParameterCheck.mandatory("script", script);

        this.script = script;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect() throws IOException
    {
        if (this.processedSource == null)
        {
            final String source;

            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            FileCopyUtils.copy(this.script.getInputStream(), os); // both streams are closed
            final byte[] bytes = os.toByteArray();

            // TODO Is this safe - can we "just assume" UTF-8?
            source = new String(bytes, "UTF-8");

            final String classpathResolvedScript = source.replaceAll(CLASSPATH_RESOURCE_IMPORT_PATTERN,
                    CLASSPATH_RESOURCE_IMPORT_REPLACEMENT);
            final String nodeRefResolvedScript = classpathResolvedScript.replaceAll(NODE_REF_RESOURCE_IMPORT_PATTERN,
                    NODE_REF_RESOURCE_IMPORT_REPLACEMENT);
            final String legacyNamePathResolvedScript = nodeRefResolvedScript.replaceAll(LEGACY_NAME_PATH_RESOURCE_IMPORT_PATTERN,
                    LEGACY_NAME_PATH_RESOURCE_IMPORT_REPLACEMENT);

            this.processedSource = legacyNamePathResolvedScript;
        }
    }

    /* The following methods are the minimum requirement to be usable by Nashorn */

    /**
     * {@inheritDoc}
     */
    @Override
    public int getContentLength()
    {
        final int length;

        final String filePath = this.script.getReferencePath(CommonReferencePath.FILE);

        if (filePath != null)
        {
            length = (int) new File(filePath).length();
        }
        else
        {
            // TODO deal with classpath / fileFolderPath / store

            length = 0;
        }

        return length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastModified()
    {
        final long lastModified;

        final String filePath = this.script.getReferencePath(CommonReferencePath.FILE);

        if (filePath != null)
        {
            lastModified = new File(filePath).lastModified();
        }
        else
        {
            // TODO deal with classpath / fileFolderPath / store
            lastModified = 0;
        }

        return lastModified;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream() throws IOException
    {
        this.connect();

        return new StringInputStream(this.processedSource, "UTF-8");
    }

}
