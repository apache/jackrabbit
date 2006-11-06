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
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.UnknownPrefixException;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.PathFormat;
import org.apache.jackrabbit.name.MalformedPathException;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.util.ArrayList;

/**
 * Defines methods to read members out of a file record.
 */
class FileRecordInput {

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
    public FileRecordInput(InputStream in, NamespaceResolver resolver) {
        this.in = new DataInputStream(in);
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
     * @return string
     * @throws IOException if an I/O error occurs
     */
    public String readString() throws IOException {
        checkOpen();

        return in.readUTF();
    }

    /**
     * Read a <code>QName</code>.
     *
     * @return name
     * @throws IOException if an I/O error occurs
     * @throws IllegalNameException if the name retrieved is illegal
     * @throws UnknownPrefixException if the prefix is unknown
     */
    public QName readQName() throws IOException, IllegalNameException, UnknownPrefixException {
        checkOpen();

        return NameFormat.parse(readString(), resolver);
    }

    /**
     * Read a <code>PathElement</code>.
     *
     * @return path element
     * @throws IOException if an I/O error occurs
     * @throws IllegalNameException if the name retrieved is illegal
     * @throws UnknownPrefixException if the prefix is unknown
     */
    public Path.PathElement readPathElement() throws IOException, IllegalNameException, UnknownPrefixException {
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

        byte b = readByte();
        if (b == FileRecord.UUID_INDEX) {
            int index = readInt();
            if (index == -1) {
                return null;
            } else {
                return (NodeId) uuidIndex.get(index);
            }
        } else if (b == FileRecord.UUID_LITERAL) {
            NodeId nodeId = NodeId.valueOf(readString());
            uuidIndex.add(nodeId);
            return nodeId;
        } else {
            String msg = "UUID indicator unknown: " + b;
            throw new IOException(msg);
        }
    }

    /**
     * Read a <code>PropertyId</code>
     *
     * @return property id
     * @throws IOException if an I/O error occurs
     * @throws IllegalNameException if the name retrieved is illegal
     * @throws UnknownPrefixException if the prefix is unknown
     */
    public PropertyId readPropertyId() throws IOException, IllegalNameException, UnknownPrefixException  {
        checkOpen();

        return new PropertyId(readNodeId(), readQName());
    }

    /**
     * Close this input.
     *
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        checkOpen();

        try {
            in.close();
        } finally {
            closed = true;
        }
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
