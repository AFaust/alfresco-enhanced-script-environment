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
package org.nabucco.alfresco.enhScriptEnv.common.util;

import java.io.IOException;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Axel Faust
 */
public class SourceFileVisitor implements ClassVisitor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SourceFileVisitor.class);

    private String sourceFile;

    public static String readSourceFile(final Class<?> cls)
    {
        String sourceFileName = null;
        final String className = cls.getName();
        try
        {
            // TODO: make this class fail-safe against lack of ASM but keep this functionality
            final ClassReader clsReader = new ClassReader(className);
            final SourceFileVisitor sourceFileVisitor = new SourceFileVisitor();
            clsReader.accept(sourceFileVisitor, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
            sourceFileName = sourceFileVisitor.getSourceFile();
        }
        catch (final IOException ioEx)
        {
            LOGGER.debug("Failed to read compiled class to determine original source name", ioEx);
        }

        return sourceFileName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitSource(final String source, final String debug)
    {
        this.sourceFile = source;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName,
            final String[] interfaces)
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitOuterClass(final String owner, final String name, final String desc)
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitAttribute(final Attribute attr)
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitInnerClass(final String name, final String outerName, final String innerName, final int access)
    {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
            final String[] exceptions)
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visitEnd()
    {
        // NO-OP
    }

    /**
     * @return the sourceFile
     */
    public final String getSourceFile()
    {
        return this.sourceFile;
    }

}
