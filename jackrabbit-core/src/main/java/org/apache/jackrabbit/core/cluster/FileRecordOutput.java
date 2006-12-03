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
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.PathFormat;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;

import java.io.IOException;
import java.io.DataOutputStream;
import java.util.ArrayList;

/**
 * Allows writing data to a <code>FileRecord</code>.
 */
class FileRecordOutput {

    /**
     * File record.
     */
    private final FileRecord record;

    /**
     * Underlying output stream.
     */
    private final DataOutputStream out;

    /**
     * Name resolver.
     */
    private final NamespaceResolver resolver;

    /**
     * UUID index.
     */
    private final ArrayList uuidIndex = new ArrayList();

    /**
     * Flag indicating whether this output is closed.
     */
    private boolean closed;

    /**
     * Create a new file record.
     *
     * @param record   file record
     * @param out      outputstream to write to
     * @param resolver namespace resolver
     */
    public FileRecordOutput(FileRecord record, DataOutputStream out, NamespaceResolver resolver) {
        this.record = record;
        this.out = out;
        this.resolver = resolver;
    }

    /**
     * Write a byte to the underlying stream.
     *
     * @param n byte
     * @throws IOException if an I/O error occurs
     */
    public void writeByte(int n) throws IOException {
        checkOpen();

        out.writeByte(n);
    }

    /**
     * Write a character to the underlying stream.
     *
     * @param c character
     * @throws IOException if an I/O error occurs
     */
    public void writeChar(char c) throws IOException {
        checkOpen();

        out.writeChar(c);
    }

    /**
     * Write a boolean from the underlying stream.
     *
     * @param b boolean
     * @throws IOException if an I/O error occurs
     */
    public void writeBoolean(boolean b) throws IOException {
        checkOpen();

        out.writeBoolean(b);
    }

    /**
     * Write an integer to the underlying stream.
     *
     * @param n integer
     * @throws IOException if an I/O error occurs
     */
    public void writeInt(int n) throws IOException {
        checkOpen();

        out.writeInt(n);
    }

    /**
     * Write a string from the underlying stream.
     *
     * @param s string
     * @throws IOException if an I/O error occurs
     */
    public void writeString(String s) throws IOException {
        checkOpen();

        out.writeUTF(s);
    }

    /**
     * Write a <code>QName</code>.
     *
     * @param name name
     * @throws IOException if an I/O error occurs
     * @throws NoPrefixDeclaredException if the prefix is not declared
     */
    public void writeQName(QName name) throws IOException, NoPrefixDeclaredException {
        checkOpen();

        writeString(NameFormat.format(name, resolver));
    }

    /**
     * Write a <code>PathElement</code>.
     *
     * @param element path element
     * @throws IOException if an I/O error occurs
     * @throws NoPrefixDeclaredException if the prefix is not declared
     */
    public void writePathElement(Path.PathElement element) throws IOException, NoPrefixDeclaredException {
        checkOpen();

        writeQName(element.getName());
        writeInt(element.getIndex());
    }

    /**
     * Write a <code>Path</code>.
     *
     * @param path path
     * @throws IOException if an I/O error occurs
     * @throws NoPrefixDeclaredException if the prefix is not declared
     */
    public void writePath(Path path) throws IOException, NoPrefixDeclaredException {
        checkOpen();

        writeString(PathFormat.format(path, resolver));
    }

    /**
     * Write a <code>NodeId</code>. Since the same node ids are likely to appear multiple times,
     * only the first one will actually be literally appended, while all other reference the
     * previous entry's index.
     *
     * @param nodeId node id
     * @throws IOException if an I/O error occurs
     */
    public void writeNodeId(NodeId nodeId) throws IOException {
        checkOpen();

        if (nodeId == null) {
            writeByte(FileRecord.UUID_INDEX);
            writeInt(-1);
        } else {
            int index = getOrCreateIndex(nodeId);
            if (index != -1) {
                writeByte(FileRecord.UUID_INDEX);
                writeInt(index);
            } else {
                writeByte(FileRecord.UUID_LITERAL);
                out.write(nodeId.getUUID().getRawBytes());
            }
        }
    }

    /**
     * Write a <code>PropertyId</code>
     *
     * @param propertyId property id
     * @throws IOException if an I/O error occurs
     * @throws NoPrefixDeclaredException if the prefix is not declared
     */
    public void writePropertyId(PropertyId propertyId) throws IOException, NoPrefixDeclaredException {
        checkOpen();

        writeNodeId(propertyId.getParentId());
        writeQName(propertyId.getName());
    }

    /**
     * Close this output.
     *
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        checkOpen();

        try {
            out.close();
        } finally {
            closed = true;
            record.closed();
        }
    }

    /**
     * Get a <code>NodeId</code>'s existing cache index, creating a new entry if necesary.
     *
     * @param nodeId nodeId to lookup
     * @return cache index of existing entry or <code>-1</code> to indicate the entry was added
     */
    private int getOrCreateIndex(NodeId nodeId) {
        int index = uuidIndex.indexOf(nodeId);
        if (index == -1) {
            uuidIndex.add(nodeId);
        }
        return index;
    }

    /**
     * Check that this output is open, throw otherwise.
     *
     * @throws IllegalStateException if output is closed.
     */
    private void checkOpen() throws IllegalStateException {
        if (closed) {
            throw new IllegalStateException("Output closed.");
        }
    }
}
