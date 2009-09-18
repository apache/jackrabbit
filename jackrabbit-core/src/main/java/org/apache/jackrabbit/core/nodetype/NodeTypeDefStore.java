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
package org.apache.jackrabbit.core.nodetype;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.cnd.CompactNodeTypeDefReader;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeReader;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeWriter;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceMapping;
import org.apache.jackrabbit.spi.commons.nodetype.QDefinitionBuilderFactory;

/**
 * <code>NodeTypeDefStore</code> ...
 */
public class NodeTypeDefStore {

    /** Map of node type names to node type definitions. */
    private final Map<Name, QNodeTypeDefinition> ntDefs;

    /**
     * Empty default constructor.
     */
    public NodeTypeDefStore() throws RepositoryException {
        ntDefs = new HashMap<Name, QNodeTypeDefinition>();
    }

    /**
     * @param in
     * @throws IOException
     * @throws InvalidNodeTypeDefException
     */
    public void load(InputStream in)
            throws IOException, InvalidNodeTypeDefException,
            RepositoryException {
        QNodeTypeDefinition[] types = NodeTypeReader.read(in);
        for (QNodeTypeDefinition type : types) {
            add(type);
        }
    }

    /**
     * Loads node types from a CND stream.
     *
     * @param in reader containing the nodetype definitions
     * @param systemId optional name of the stream
     *
     * @throws IOException if an I/O error during reading occurs
     * @throws InvalidNodeTypeDefException if the CND cannot be parsed
     */
    public void loadCND(Reader in, String systemId)
            throws IOException, InvalidNodeTypeDefException {
        try {
            CompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping> r =
                new CompactNodeTypeDefReader<QNodeTypeDefinition, NamespaceMapping>(
                    in, systemId, new QDefinitionBuilderFactory());

            for (QNodeTypeDefinition qdef: r.getNodeTypeDefinitions()) {
                add(qdef);
            }
        } catch (ParseException e) {
            throw new InvalidNodeTypeDefException("Unable to parse CND stream.", e);
        }
    }

    /**
     * @param out
     * @param registry
     * @throws IOException
     * @throws RepositoryException
     */
    public void store(OutputStream out, NamespaceRegistry registry)
            throws IOException, RepositoryException {
        QNodeTypeDefinition[] types = ntDefs.values().toArray(new QNodeTypeDefinition[ntDefs.size()]);
        NodeTypeWriter.write(out, types, registry);
    }

    /**
     * @param ntd
     */
    public void add(QNodeTypeDefinition ntd) {
        ntDefs.put(ntd.getName(), ntd);
    }

    /**
     * @param name
     * @return
     */
    public boolean remove(Name name) {
        return (ntDefs.remove(name) != null);
    }

    /**
     *
     */
    public void removeAll() {
        ntDefs.clear();
    }

    /**
     * @param name
     * @return
     */
    public boolean contains(Name name) {
        return ntDefs.containsKey(name);
    }

    /**
     * @param name
     * @return
     */
    public QNodeTypeDefinition get(Name name) {
        return ntDefs.get(name);
    }

    /**
     * @return
     */
    public Collection<QNodeTypeDefinition> all() {
        return Collections.unmodifiableCollection(ntDefs.values());
    }
}
