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
package org.nabucco.alfresco.enhScriptEnv.common.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Axel Faust, <a href="http://www.prodyna.com">PRODYNA AG</a>
 */
public abstract class AbstractRhiinoValueConverter implements ValueConverter
{

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertValueForJava(final Object value)
    {
        final Object result;
        if (value instanceof List<?>)
        {
            // deal with lists specifically since they are not handled by Alfresco value converter, but may transitively contain script
            // objects in need of conversion

            @SuppressWarnings("unchecked")
            final List<Object> list = (List<Object>) value;
            for (int idx = 0, max = list.size(); idx < max; idx++)
            {
                final Object element = list.get(idx);
                final Object convertedElement = this.convertValueForJava(element);

                // may just be an identity-conversion / recursion without any need to change this list
                if (element != convertedElement)
                {
                    list.set(idx, convertedElement);
                }
            }

            result = list;
        }
        else if (value instanceof Map<?, ?>)
        {
            // deal with maps specifically since they are not handled by Alfresco value converter, but may transitively contain script
            // objects in need of conversion
            @SuppressWarnings("unchecked")
            final Map<Object, Object> map = (Map<Object, Object>) value;
            final List<Object> keys = new ArrayList<Object>(map.keySet());
            for (final Object key : keys)
            {
                final Object valueForKey = map.get(key);

                final Object convertedValueForKey = this.convertValueForJava(valueForKey);
                final Object convertedKey = this.convertValueForJava(key);

                // may just be an identity-conversion / recursion without any need to change this map
                if (key != convertedKey || valueForKey != convertedValueForKey)
                {
                    map.remove(key);
                    map.put(convertedKey, convertedValueForKey);
                }
            }

            result = map;
        }
        else
        {
            final Object intermediaryResult = this.convertValueForJavaImpl(value);
            if (intermediaryResult instanceof List<?> || intermediaryResult instanceof Map<?, ?>)
            {
                result = this.convertValueForJava(intermediaryResult);
            }
            else
            {
                result = intermediaryResult;
            }
        }

        return result;
    }

    abstract protected Object convertValueForJavaImpl(Object value);
}
