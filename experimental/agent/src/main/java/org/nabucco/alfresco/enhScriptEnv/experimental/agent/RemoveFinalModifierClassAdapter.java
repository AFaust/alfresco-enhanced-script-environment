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

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public class RemoveFinalModifierClassAdapter extends ClassAdapter
{

    public RemoveFinalModifierClassAdapter(final ClassVisitor cv)
    {
        super(cv);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName,
            final String[] interfaces)
    {
        super.visit(version, access & ~Opcodes.ACC_FINAL, name, signature, superName, interfaces);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
            final String[] exceptions)
    {
        return super.visitMethod(access & ~Opcodes.ACC_FINAL, name, desc, signature, exceptions);
    }
}
