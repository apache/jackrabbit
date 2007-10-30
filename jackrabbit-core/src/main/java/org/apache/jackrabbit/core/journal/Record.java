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

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

/**
 * Record interface.
 */
public interface Record {

    /**
     * Returns the revision this record represents.
     *
     * @return revision
     */
    public long getRevision();

    /**
     * Return this record's journal identifier.
     *
     * @return journal identifier
     */
    public String getJournalId();

    /**
     * Return this record's producer identifier.
     *
     * @return producer identifier
     */
    public String getProducerId();

    /**
     * Read a byte from the underlying stream.
     *
     * @return byte
     * @throws JournalException if an error occurs
     */
    public byte readByte() throws JournalException;

    /**
     * Read a character from the underlying stream.
     *
     * @return character
     * @throws JournalException if an error occurs
     */
    public char readChar() throws JournalException;

    /**
     * Read a boolean from the underlying stream.
     *
     * @return boolean
     * @throws JournalException if an error occurs
     */
    public boolean readBoolean() throws JournalException;

    /**
     * Read an integer from the underlying stream.
     *
     * @return integer
     * @throws JournalException if an error occurs
     */
    public int readInt() throws JournalException;

    /**
     * Read a string from the underlying stream.
     *
     * @return string or <code>null</code>
     * @throws JournalException if an error occurs
     */
    public String readString() throws JournalException;

    /**
     * Fully read an array of bytes from the underlying stream.
     *
     * @param b byte array
     * @throws JournalException if an error occurs
     */
    public void readFully(byte[] b) throws JournalException;

    /**
     * Read a <code>Name</code> frmo the underlying stream.
     *
     * @return name name
     * @throws JournalException if an error occurs
     */
    public Name readQName() throws JournalException;

    /**
     * Read a <code>Path.Element</code> from the underlying stream.
     *
     * @return path element
     * @throws JournalException if an error occurs
     */
    public Path.Element readPathElement() throws JournalException;

    /**
     * Read a <code>Path</code> from the underlying stream.
     *
     * @return path
     * @throws JournalException if an error occurs
     */
    public Path readPath() throws JournalException;

    /**
     * Read a <code>NodeId</code> from the underlying stream.
     *
     * @return node id
     * @throws JournalException if an error occurs
     */
    public NodeId readNodeId() throws JournalException;

    /**
     * Read a <code>PropertyId</code> from the underlying stream.
     *
     * @return property id
     * @throws JournalException if an error occurs
     */
    public PropertyId readPropertyId() throws JournalException;

    /**
     * Read a <code>NodeTypeDef</code> from the underlying stream.
     *
     * @return node type definition
     * @throws JournalException if an error occurs
     */
    public NodeTypeDef readNodeTypeDef() throws JournalException;

    /**
     * Write a byte to the underlying stream.
     *
     * @param n byte
     * @throws JournalException if an error occurs
     */
    public void writeByte(int n) throws JournalException;

    /**
     * Write a character to the underlying stream.
     *
     * @param c character
     * @throws JournalException if an error occurs
     */
    public void writeChar(char c) throws JournalException;

    /**
     * Write a boolean from the underlying stream.
     *
     * @param b boolean
     * @throws JournalException if an error occurs
     */
    public void writeBoolean(boolean b) throws JournalException;

    /**
     * Write an integer to the underlying stream.
     *
     * @param n integer
     * @throws JournalException if an error occurs
     */
    public void writeInt(int n) throws JournalException;

    /**
     * Write a string to the underlying stream.
     *
     * @param s string, may be <code>null</code>
     * @throws JournalException if an error occurs
     */
    public void writeString(String s) throws JournalException;

    /**
     * Write an array of bytes to the underlying stream.
     *
     * @param b byte array
     * @throws JournalException if an error occurs
     */
    public void write(byte[] b) throws JournalException;

    /**
     * Write a <code>Name</code> to the underlying stream.
     *
     * @param name name
     * @throws JournalException if an error occurs
     */
    public void writeQName(Name name) throws JournalException;

    /**
     * Write a <code>Path.Element</code> to the underlying stream.
     *
     * @param element path element
     * @throws JournalException if an error occurs
     */
    public void writePathElement(Path.Element element) throws JournalException;

    /**
     * Write a <code>Path</code> to the underlying stream.
     *
     * @param path path
     * @throws JournalException if an error occurs
     */
    public void writePath(Path path) throws JournalException;

    /**
     * Write a <code>NodeId</code> to the underlying stream.
     *
     * @param nodeId node id
     * @throws JournalException if an error occurs
     */
    public void writeNodeId(NodeId nodeId) throws JournalException;

    /**
     * Write a <code>PropertyId</code> to the underlying stream.
     *
     * @param propertyId property id
     * @throws JournalException if an error occurs
     */
    public void writePropertyId(PropertyId propertyId) throws JournalException;

    /**
     * Write a <code>NodeTypeDef</code> to the underlying stream.
     *
     * @param ntd node type definition
     * @throws JournalException if an error occurs
     */
    public void writeNodeTypeDef(NodeTypeDef ntd) throws JournalException;

    /**
     * Update the changes made to an appended record. This will also update
     * this record's revision.
     *
     * @throws JournalException if this record has not been appended,
     *                          or if another error occurs
     */
    public void update() throws JournalException;

    /**
     * Cancel the changes made to an appended record.
     */
    public void cancelUpdate();
}