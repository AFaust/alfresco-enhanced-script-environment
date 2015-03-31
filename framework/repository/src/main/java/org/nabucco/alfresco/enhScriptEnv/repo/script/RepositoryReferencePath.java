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
package org.nabucco.alfresco.enhScriptEnv.repo.script;

import org.nabucco.alfresco.enhScriptEnv.common.script.ReferenceScript.ReferencePathType;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public enum RepositoryReferencePath implements ReferencePathType
{

    NODE_REF, CONTENT_PROPERTY, FILE_FOLDER_PATH;
    // TODO Need to register URL handler to resolve Repository stored scripts to source (for Rhino debugger Dim.loadSource())

}
