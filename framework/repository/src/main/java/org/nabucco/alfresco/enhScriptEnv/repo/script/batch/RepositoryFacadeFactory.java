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
package org.nabucco.alfresco.enhScriptEnv.repo.script.batch;

import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.nabucco.alfresco.enhScriptEnv.common.script.batch.DefaultFacadeFactory;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 *
 *
 */
public class RepositoryFacadeFactory extends DefaultFacadeFactory {

	@Override
	protected Scriptable toFacadedObjectImpl(final Scriptable obj, final Scriptable referenceScope,
			final Scriptable thisObj) {
		Scriptable globalFacadedObject;
		if (obj instanceof NativeJavaObject)
		{
			final NativeJavaObject nativeJavaObj = (NativeJavaObject) obj;
			final Object javaObj = nativeJavaObj.unwrap();

			// we know scopeable processor extensions keep their scope in a ThreadLocal which we may have to
			// lazily-initialize for a worker thread
			if (javaObj instanceof BaseScopableProcessorExtension)
			{
				final BaseScopableProcessorExtension scopeable = (BaseScopableProcessorExtension) javaObj;
				if (scopeable.getScope() == null)
				{
					scopeable.setScope(referenceScope);
				}
			}
		}

		globalFacadedObject = super.toFacadedObjectImpl(obj, referenceScope, thisObj);
		return globalFacadedObject;
	}

}
