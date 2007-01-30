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
package org.apache.jackrabbit.core.cluster;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.compact.CompactNodeTypeDefReader;
import org.apache.jackrabbit.core.nodetype.compact.ParseException;
import org.apache.jackrabbit.name.NameException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.PathFormat;
import org.apache.jackrabbit.name.MalformedPathException;
import org.apache.jackrabbit.uuid.Constants;
import org.apache.jackrabbit.uuid.UUID;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows reading data from a <code>DataInputStream</code>.
 */
class RecordInput {

    /**
     * Underlying input stream.
     */
    private final DataInputStream in;

    /**
     * Name resolver.
     */
    private final NamespaceResolver resolver;

    /**
     * UUID index.
     */
    private final ArrayList uuidIndex = new ArrayList();

    /**
     * Flag indicating whether this input is closed.
     */
    private boolean closed;

    /**
     * Open an existing file record.
     *
     * @param in       underlying input stream
     * @param resolver namespace resolver
     */
    public RecordInput(DataInputStream in, NamespaceResolver resolver) {
        this.in = in;
        this.resolver = resolver;
    }

    /**
     * Read a byte from the underlying stream.
     *
     * @return byte
     * @throws IOException if an I/O error occurs
     */
    public byte readByte() throws IOException {
        checkOpen();

        return in.readByte();
    }

    /**
     * Read a character from the underlying stream.
     *
     * @return character
     * @throws IOException if an I/O error occurs
     */
    public char readChar() throws IOException {
        checkOpen();

        return in.readChar();
    }

    /**
     * Read a boolean from the underlying stream.
     *
     * @return boolean
     * @throws IOException if an I/O error occurs
     */
    public boolean readBoolean() throws IOException {
        checkOpen();

        return in.readBoolean();
    }

    /**
     * Read an integer from the underlying stream.
     *
     * @return integer
     * @throws IOException if an I/O error occurs
     */
    public int readInt() throws IOException {
        checkOpen();

        return in.readInt();
    }

    /**
     * Read a string from the underlying stream.
     *
     * @return string or <code>null</code>
     * @throws IOException if an I/O error occurs
     */
    public String readString() throws IOException {
        checkOpen();

        boolean isNull = in.readBoolean();
        if (isNull) {
            return null;
        } else {
            return in.readUTF();
        }
    }

    /**
     * Read a <code>QName</code>.
     *
     * @return name
     * @throws IOException if an I/O error occurs
     * @throws NameException if the name retrieved is illegal
     */
    public QName readQName() throws IOException, NameException {
        checkOpen();

        return NameFormat.parse(readString(), resolver);
    }

    /**
     * Read a <code>PathElement</code>.
     *
     * @return path element
     * @throws IOException if an I/O error occurs
     * @throws NameException if the name retrieved is illegal
     */
    public Path.PathElement readPathElement() throws IOException, NameException {
        checkOpen();

        QName name = NameFormat.parse(readString(), resolver);
        int index = readInt();
        if (index != 0) {
            return Path.PathElement.create(name, index);
        } else {
            return Path.PathElement.create(name);
        }
    }

    /**
     * Read a <code>Path</code>.
     *
     * @return path
     * @throws IOException if an I/O error occurs
     * @throws MalformedPathException if the path is malformed
     */
    public Path readPath() throws IOException, MalformedPathException {
        checkOpen();

        return PathFormat.parse(readString(), resolver);
    }

    /**
     * Read a <code>NodeId</code>
     *
     * @return node id
     * @throws IOException if an I/O error occurs
     */
    public NodeId readNodeId() throws IOException {
        checkOpen();

        byte uuidType = readByte();
        if (uuidType == Record.UUID_INDEX) {
            int index = readInt();
            if (index == -1) {
                return null;
            } else {
                return (NodeId) uuidIndex.get(index);
            }
        } else if (uuidType == Record.UUID_LITERAL) {
            byte[] b = new byte[Constants.UUID_BYTE_LENGTH];
            in.readFully(b);
            NodeId nodeId = new NodeId(new UUID(b));
            uuidIndex.add(nodeId);
            return nodeId;
        } else {
            String msg = "UUID type unknown: " + uuidType;
            throw new IOException(msg);
        }
    }

    /**
     * Read a <code>PropertyId</code>
     *
     * @return property id
     * @throws IOException if an I/O error occurs
     * @throws NameException if the name retrieved is illegal
     */
    public PropertyId readPropertyId() throws IOException, NameException  {
        checkOpen();

        return new PropertyId(readNodeId(), readQName());
    }

    /**
     * Read a <code>NodeTypeDef</code>
     */
    public NodeTypeDef readNodeTypeDef() throws IOException, ParseException {
        checkOpen();

        StringReader sr = new StringReader(readString());

        CompactNodeTypeDefReader reader = new CompactNodeTypeDefReader(sr, "(internal)");
        List ntds = reader.getNodeTypeDefs();
        if (ntds.size() != 1) {
            throw new IOException("Expected one node type definition: got " + ntds.size());
        }
        return (NodeTypeDef) ntds.get(0);
    }



    /**
     * Close this input. Does not close underlying stream as this is a shared resource.
     */
    public void close() {
        checkOpen();

        closed = true;
    }

    /**
     * Check that this input is open, throw otherwise.
     *
     * @throws IllegalStateException if input is closed.
     */
    private void checkOpen() throws IllegalStateException {
        if (closed) {
            throw new IllegalStateException("Input closed.");
        }
    }

}
