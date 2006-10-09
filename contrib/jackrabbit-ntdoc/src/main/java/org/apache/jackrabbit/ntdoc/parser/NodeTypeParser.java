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
package org.apache.jackrabbit.ntdoc.parser;

import java.io.*;
import java.util.*;

import org.apache.jackrabbit.ntdoc.model.*;

/**
 * This class defines the parser.
 */
public abstract class NodeTypeParser {
    /**
     * System id.
     */
    private String systemId;

    /**
     * Reader.
     */
    private InputStream stream;

    /**
     * Node Types.
     */
    private final NodeTypeSet nodeTypes;

    /**
     * Namespace mapping.
     */
    private final HashMap namespaces;

    /**
     * Construct the parser.
     */
    public NodeTypeParser() {
        this.nodeTypes = new NodeTypeSet();
        this.namespaces = new HashMap();
    }

    /**
     * Return the reader.
     */
    public InputStream getInputStream() {
        return this.stream;
    }

    /**
     * Set the reader.
     */
    public void setInputStream(InputStream stream) {
        this.stream = stream;
    }

    /**
     * Return the reader.
     */
    public Reader getReader() {
        return new InputStreamReader(getInputStream());
    }

    /**
     * Return the system id.
     */
    public String getSystemId() {
        return this.systemId;
    }

    /**
     * Set the system id.
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Return the node types.
     */
    public NodeTypeSet getNodeTypes() {
        return this.nodeTypes;
    }

    /**
     * Add namespace declaration.
     */
    protected void addNamespace(String prefix, String uri)
            throws IOException {
        this.namespaces.put(prefix, uri);
    }

    /**
     * Return prefix from name.
     */
    private String getPrefix(String ntName) {
        int pos = ntName.indexOf(':');
        if (pos > -1) {
            return ntName.substring(0, pos);
        } else {
            return null;
        }
    }

    /**
     * Return namespace from node type name.
     */
    private String getNamespace(String ntName) {
        String uri = null;
        String prefix = getPrefix(ntName);

        if (prefix != null) {
            uri = (String) this.namespaces.get(prefix);
        }

        return uri != null ? uri : "";
    }

    /**
     * Create a node type.
     */
    protected NodeType addNodeType(String name)
            throws IOException {
        NodeType nt = new NodeType(name);
        nt.setNamespace(getNamespace(name));
        this.nodeTypes.addNodeType(nt);
        return nt;
    }

    /**
     * Parse the file.
     */
    public abstract void parse()
            throws IOException;
}
