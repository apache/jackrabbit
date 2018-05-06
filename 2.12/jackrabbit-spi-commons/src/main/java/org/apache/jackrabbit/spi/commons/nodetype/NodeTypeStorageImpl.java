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
package org.apache.jackrabbit.spi.commons.nodetype;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeExistsException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;

/**
 * This implementation of {@link NodeTypeStorage} keeps a map of the registered {@link QNodeTypeDefinition}
 * in memory.
 */
public class NodeTypeStorageImpl implements NodeTypeStorage {

    private final Map<Name, QNodeTypeDefinition> definitions = new HashMap<Name, QNodeTypeDefinition>();

    public Iterator<QNodeTypeDefinition> getAllDefinitions() throws RepositoryException {
        return definitions.values().iterator();
    }

    /**
     * This implementation returns an iterator over all registered {@link QNodeTypeDefinition}s if
     * <code>nodeTypeNames</code> is <code>null</code>.
     * {@inheritDoc}
     */
    public Iterator<QNodeTypeDefinition> getDefinitions(Name[] nodeTypeNames) throws NoSuchNodeTypeException,
            RepositoryException {

        if (nodeTypeNames == null) {
            return definitions.values().iterator();
        }

        Collection<QNodeTypeDefinition> defs = new ArrayList<QNodeTypeDefinition>(nodeTypeNames.length);

        for (Name name : nodeTypeNames) {
            if (definitions.containsKey(name)) {
                defs.add(definitions.get(name));
            }
            else {
                throw new NoSuchNodeTypeException("{" + name.getNamespaceURI() + "}" + name.getLocalName());
            }
        }

        return defs.iterator();
    }

    public void registerNodeTypes(QNodeTypeDefinition[] nodeTypeDefs, boolean allowUpdate)
            throws RepositoryException {

        if (nodeTypeDefs == null) {
            throw new IllegalArgumentException("nodeTypeDefs must not be null");
        }

        if (!allowUpdate) {
            for (QNodeTypeDefinition ntd : nodeTypeDefs) {
                Name name = ntd.getName();
                if (definitions.containsKey(name)) {
                    throw new NodeTypeExistsException("{" + name.getNamespaceURI() + "}" + name.getLocalName());
                }
            }
        }

        for (QNodeTypeDefinition ntd : nodeTypeDefs) {
            definitions.put(ntd.getName(), ntd);
        }
    }

    public void unregisterNodeTypes(Name[] nodeTypeNames) throws NoSuchNodeTypeException, RepositoryException {
        for (Name name : nodeTypeNames) {
            if (!definitions.containsKey(name)) {
                throw new NoSuchNodeTypeException("{" + name.getNamespaceURI() + "}" + name.getLocalName());
            }
        }

        for (Name name : nodeTypeNames) {
            definitions.remove(name);
        }
    }

}
