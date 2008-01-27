/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.ocm.nodemanagement.impl.jackrabbit;

import javax.jcr.NamespaceRegistry;

import org.apache.jackrabbit.core.nodetype.ItemDef;

import org.apache.jackrabbit.ocm.nodemanagement.impl.BaseNamespaceHelper;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;


/** Jackrabbit namespace helper class.
 *
 * @author <a href="mailto:okiessler@apache.org">Oliver Kiessler</a>
 */
public class NamespaceHelper extends BaseNamespaceHelper
{

    /** JCR namespace registry.
     */
    private NamespaceRegistry registry;

    /** Creates a new instance of NamespaceHelper. */
    public NamespaceHelper()
    {
    }

    /** Returns a QName object from a given JCR item name.
     *
     * @param nodeName JCR item name
     * @return qName
     */
    public Name getName(String itemName)
    {
        Name name = null;

        if (itemName != null && itemName.length() > 0)
        {
            if (itemName.equals("*"))
            {
                name = ItemDef.ANY_NAME;
            }
            else
            {
                String[] parts = itemName.split(":");
                if (parts.length == 2)
                {
                    name = NameFactoryImpl.getInstance().create(getNamespaceUri(parts[0]),parts[1]);
                }
                else if (parts.length == 1)
                {
                    // no namespace set, use default  namespace                	
                	name = NameFactoryImpl.getInstance().create(DEFAULT_NAMESPACE_URI, parts[0]);
                }
            }
        }

        return name;
    }

    /** Returns the namespace URI from a given namespace prefix.
     *
     * @param namespacePrefix
     * @return uri
     */
    public String getNamespaceUri(String namespacePrefix)
    {
        String uri = null;
        try
        {
            uri = getRegistry().getURI(namespacePrefix);
        }
        catch (Exception ne)
        {
            ne.printStackTrace();
        }

        return uri;
    }

    /** Getter for property registry.
     *
     * @return registry
     */
    public NamespaceRegistry getRegistry()
    {
        return registry;
    }

    /** Setter for property registry.
     *
     * @param object registry
     */
    public void setRegistry(NamespaceRegistry object)
    {
        this.registry = object;
    }
}
