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
package org.apache.jackrabbit.core.journal;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.PrivilegeDefinition;

/**
 * Record interface.
 */
public interface Record {

    /**
     * Returns the revision this record represents.
     *
     * @return revision
     */
    long getRevision();

    /**
     * Return this record's journal identifier.
     *
     * @return journal identifier
     */
    String getJournalId();

    /**
     * Return this record's producer identifier.
     *
     * @return producer identifier
     */
    String getProducerId();

    /**
     * Read a byte from the underlying stream.
     *
     * @return byte
     * @throws JournalException if an error occurs
     */
    byte readByte() throws JournalException;

    /**
     * Read a character from the underlying stream.
     *
     * @return character
     * @throws JournalException if an error occurs
     */
    char readChar() throws JournalException;

    /**
     * Read a boolean from the underlying stream.
     *
     * @return boolean
     * @throws JournalException if an error occurs
     */
    boolean readBoolean() throws JournalException;

    /**
     * Read an integer from the underlying stream.
     *
     * @return integer
     * @throws JournalException if an error occurs
     */
    int readInt() throws JournalException;

    /**
     * Read a long from the underlying stream.
     *
     * @return long value.
     * @throws JournalException if an error occurs
     */
    long readLong() throws JournalException;

    /**
     * Read a string from the underlying stream.
     *
     * @return string or <code>null</code>
     * @throws JournalException if an error occurs
     */
    String readString() throws JournalException;

    /**
     * Fully read an array of bytes from the underlying stream.
     *
     * @param b byte array
     * @throws JournalException if an error occurs
     */
    void readFully(byte[] b) throws JournalException;

    /**
     * Read a <code>Name</code> frmo the underlying stream.
     *
     * @return name name
     * @throws JournalException if an error occurs
     */
    Name readQName() throws JournalException;

    /**
     * Read a named path element from the underlying stream.
     *
     * @return path element
     * @throws JournalException if an error occurs
     */
    Path readPathElement() throws JournalException;

    /**
     * Read a <code>Path</code> from the underlying stream.
     *
     * @return path
     * @throws JournalException if an error occurs
     */
    Path readPath() throws JournalException;

    /**
     * Read a <code>NodeId</code> from the underlying stream.
     *
     * @return node id
     * @throws JournalException if an error occurs
     */
    NodeId readNodeId() throws JournalException;

    /**
     * Read a <code>PropertyId</code> from the underlying stream.
     *
     * @return property id
     * @throws JournalException if an error occurs
     */
    PropertyId readPropertyId() throws JournalException;

    /**
     * Read a <code>NodeTypeDef</code> from the underlying stream.
     *
     * @return node type definition
     * @throws JournalException if an error occurs
     */
    QNodeTypeDefinition readNodeTypeDef() throws JournalException;

    /**
     * Read a <code>PrivilegeDefinition</code> from the underlying stream.
     *
     * @return privilege definition
     * @throws JournalException if an error occurs
     */
    PrivilegeDefinition readPrivilegeDef() throws JournalException;

    /**
     * Write a byte to the underlying stream.
     *
     * @param n byte
     * @throws JournalException if an error occurs
     */
    void writeByte(int n) throws JournalException;

    /**
     * Write a character to the underlying stream.
     *
     * @param c character
     * @throws JournalException if an error occurs
     */
    void writeChar(char c) throws JournalException;

    /**
     * Write a boolean from the underlying stream.
     *
     * @param b boolean
     * @throws JournalException if an error occurs
     */
    void writeBoolean(boolean b) throws JournalException;

    /**
     * Write an integer to the underlying stream.
     *
     * @param n integer
     * @throws JournalException if an error occurs
     */
    void writeInt(int n) throws JournalException;

    /**
     * Write a long to the underlying stream.
     *
     * @param n long
     * @throws JournalException if an error occurs
     */
    void writeLong(long n) throws JournalException;

    /**
     * Write a string to the underlying stream.
     *
     * @param s string, may be <code>null</code>
     * @throws JournalException if an error occurs
     */
    void writeString(String s) throws JournalException;

    /**
     * Write an array of bytes to the underlying stream.
     *
     * @param b byte array
     * @throws JournalException if an error occurs
     */
    void write(byte[] b) throws JournalException;

    /**
     * Write a <code>Name</code> to the underlying stream.
     *
     * @param name name
     * @throws JournalException if an error occurs
     */
    void writeQName(Name name) throws JournalException;

    /**
     * Write a <code>Path.Element</code> to the underlying stream.
     *
     * @param element path element
     * @throws JournalException if an error occurs
     */
    void writePathElement(Path element) throws JournalException;

    /**
     * Write a <code>Path</code> to the underlying stream.
     *
     * @param path path
     * @throws JournalException if an error occurs
     */
    void writePath(Path path) throws JournalException;

    /**
     * Write a <code>NodeId</code> to the underlying stream.
     *
     * @param nodeId node id
     * @throws JournalException if an error occurs
     */
    void writeNodeId(NodeId nodeId) throws JournalException;

    /**
     * Write a <code>PropertyId</code> to the underlying stream.
     *
     * @param propertyId property id
     * @throws JournalException if an error occurs
     */
    void writePropertyId(PropertyId propertyId) throws JournalException;

    /**
     * Write a <code>NodeTypeDef</code> to the underlying stream.
     *
     * @param ntd node type definition
     * @throws JournalException if an error occurs
     */
    void writeNodeTypeDef(QNodeTypeDefinition ntd) throws JournalException;

    /**
     * Write a <code>PrivilegeDefinition</code> to the underlying stream.
     *
     * @param privilegeDefinition privilege definition
     * @throws JournalException if an error occurs
     */
    void writePrivilegeDef(PrivilegeDefinition privilegeDefinition) throws JournalException;

    /**
     * Update the changes made to an appended record. This will also update
     * this record's revision.
     *
     * @return The update size in bytes.
     * @throws JournalException if this record has not been appended,
     *                          or if another error occurs
     */
    long update() throws JournalException;

    /**
     * Cancel the changes made to an appended record.
     */
    void cancelUpdate();

}
