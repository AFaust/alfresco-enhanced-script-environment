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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.ClassReader;
import org.springframework.asm.ClassWriter;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class ChangeClassFinalityTransformer implements ClassFileTransformer
{

    private static final String DEFINALIZE_CLASS_LIST = "definalizeClassList";

    private final Set<String> classesToTransform = new HashSet<String>();

    public ChangeClassFinalityTransformer(final Map<String, String> arguments)
    {

        if (arguments.containsKey(DEFINALIZE_CLASS_LIST))
        {
            final String definalizeClassListFile = arguments.get(DEFINALIZE_CLASS_LIST);

            if (definalizeClassListFile != null && !definalizeClassListFile.isEmpty())
            {
                try
                {
                    final FileReader fReader = new FileReader(definalizeClassListFile);
                    final BufferedReader bReader = new BufferedReader(fReader);
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
                        System.err.println(MessageFormat.format("IO error reading file {0} defined by 'definalizeClassList'",
                                definalizeClassListFile));
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
                catch (final FileNotFoundException fex)
                {
                    System.err
                            .println(MessageFormat
                                    .format("File {0} defined by 'definalizeClassList' does not exist - agent will not remove final modifier for any class",
                                            definalizeClassListFile));
                }
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
            final ClassReader cr = new ClassReader(classfileBuffer);
            final ClassWriter cw = new ClassWriter(cr, true);

            final RemoveFinalModifierClassAdapter adapter = new RemoveFinalModifierClassAdapter(cw);
            cr.accept(adapter, false);

            bytes = cw.toByteArray();
        }
        else
        {
            bytes = classfileBuffer;
        }

        return bytes;
    }

}
