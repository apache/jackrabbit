/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.nodetype;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeFormatter;

/**
 * <code>NodeTypeDefStore</code> ...
 */
class NodeTypeDefStore {

    /** Map of node type names to node type definitions. */
    private final HashMap ntDefs;

    /** The node type definition file formatter. */
    private final NodeTypeFormatter formatter;

    /**
     * Empty default constructor.
     */
    NodeTypeDefStore() throws RepositoryException {
        ntDefs = new HashMap();
        formatter = new NodeTypeFormatter();
    }

    /**
     * @param in
     * @throws IOException
     * @throws InvalidNodeTypeDefException
     */
    void load(InputStream in)
            throws IOException, InvalidNodeTypeDefException,
            RepositoryException {
        Collection types = formatter.read(in);
        Iterator iterator = types.iterator();
        while (iterator.hasNext()) {
            add((NodeTypeDef) iterator.next());
        }
    }

    /**
     * @param out
     * @param registry
     * @throws IOException
     * @throws RepositoryException
     */
    void store(OutputStream out, NamespaceRegistry registry)
            throws IOException, RepositoryException {
        formatter.write(out, registry, ntDefs.values());
    }

    /**
     * @param ntd
     */
    void add(NodeTypeDef ntd) {
        ntDefs.put(ntd.getName(), ntd);
    }

    /**
     * @param name
     * @return
     */
    boolean remove(QName name) {
        return ntDefs.remove(name) != null ? true : false;
    }

    /**
     *
     */
    void removeAll() {
        ntDefs.clear();
    }

    /**
     * @param name
     * @return
     */
    boolean contains(QName name) {
        return ntDefs.containsKey(name);
    }

    /**
     * @param name
     * @return
     */
    NodeTypeDef get(QName name) {
        return (NodeTypeDef) ntDefs.get(name);
    }

    /**
     * @return
     */
    Collection all() {
        return Collections.unmodifiableCollection(ntDefs.values());
    }
}
