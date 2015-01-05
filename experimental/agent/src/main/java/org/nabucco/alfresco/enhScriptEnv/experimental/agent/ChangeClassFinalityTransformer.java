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
package org.nabucco.alfresco.enhScriptEnv.experimental.agent;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ChangeClassFinalityTransformer implements ClassFileTransformer
{

    private static final String DEFINALIZE_CLASS_LIST = "definalizeClassList";

    private final Set<String> classesToTransform = new HashSet<String>();

    public ChangeClassFinalityTransformer(final Map<String, String> arguments)
    {
        final InputStream includedDefinalizeClassListFile = this.getClass().getResourceAsStream("definalizeClassList.txt");
        final InputStreamReader reader = new InputStreamReader(includedDefinalizeClassListFile);
        try
        {
            this.readDefinalizeClassList(reader);
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (final IOException ioex)
            {
                // ignore
            }
        }

        if (arguments.containsKey(DEFINALIZE_CLASS_LIST))
        {
            final String definalizeClassListFile = arguments.get(DEFINALIZE_CLASS_LIST);

            if (definalizeClassListFile != null && !definalizeClassListFile.isEmpty())
            {
                FileReader fReader = null;
                try
                {
                    fReader = new FileReader(definalizeClassListFile);
                    this.readDefinalizeClassList(fReader);
                }
                catch (final FileNotFoundException fex)
                {
                    System.err.println(MessageFormat.format(
                            "ChangeClassFinalityTransformer: File {0} defined by 'definalizeClassList' does not exist",
                            definalizeClassListFile));
                }
                finally
                {
                    if (fReader != null)
                    {
                        try
                        {
                            fReader.close();
                        }
                        catch (final IOException ioex)
                        {
                            // ignore
                        }
                    }
                }
            }
        }

    }

    private void readDefinalizeClassList(final Reader reader)
    {
        final BufferedReader bReader = new BufferedReader(reader);
        try
        {
            String line = null;
            while ((line = bReader.readLine()) != null)
            {
                this.classesToTransform.add(line);
            }
        }
        catch (final IOException ioex)
        {
            System.err.println("IO error reading list of classes to de-finalize");
        }
        finally
        {
            try
            {
                bReader.close();
            }
            catch (final IOException ioex)
            {
                // ignore
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined,
            final ProtectionDomain protectionDomain, final byte[] classfileBuffer) throws IllegalClassFormatException
    {
        final byte[] bytes;

        if (this.classesToTransform.contains(className))
        {
            System.out.println("ChangeClassFinalityTransformer: Transforming class " + className);

            final ClassReader cr = new ClassReader(classfileBuffer);
            final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

            final RemoveFinalModifierClassAdapter adapter = new RemoveFinalModifierClassAdapter(cw);
            cr.accept(adapter, 0);

            bytes = cw.toByteArray();
        }
        else
        {
            bytes = classfileBuffer;
        }

        return bytes;
    }

}
