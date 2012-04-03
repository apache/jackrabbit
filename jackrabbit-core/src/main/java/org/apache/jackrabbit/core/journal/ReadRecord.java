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

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.Name;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * Record used for reading.
 */
public class ReadRecord extends AbstractRecord {

    /**
     * This record's journal id.
     */
    private final String journalId;

    /**
     * This record's producer id.
     */
    private final String producerId;

    /**
     * This record's revision.
     */
    private final long revision;

    /**
     * Underlying data input.
     */
    private final DataInputStream dataIn;

    /**
     * This record's length.
     */
    private final int length;

    /**
     * Flag indicating whether this record was consumed.
     */
    private boolean consumed;

    /**
     * Create a new instance of this class.
     */
    public ReadRecord(String journalId, String producerId,
                      long revision, DataInputStream dataIn, int length,
                      NamespaceResolver resolver, NamePathResolver npResolver) {

        super(resolver, npResolver);

        this.journalId = journalId;
        this.producerId = producerId;
        this.revision = revision;
        this.dataIn = dataIn;
        this.length = length;
    }

    /**
     * {@inheritDoc}
     */
    public String getJournalId() {
        return journalId;
    }

    /**
     * {@inheritDoc}
     */
    public String getProducerId() {
        return producerId;
    }

    /**
     * {@inheritDoc}
     */
    public long getRevision() {
        return revision;
    }

    /**
     * {@inheritDoc}
     */
    public byte readByte() throws JournalException {
        consumed = true;

        try {
            return dataIn.readByte();
        } catch (IOException e) {
            String msg = "I/O error while reading byte.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public char readChar() throws JournalException {
        consumed = true;

        try {
            return dataIn.readChar();
        } catch (IOException e) {
            String msg = "I/O error while reading character.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean readBoolean() throws JournalException {
        consumed = true;

        try {
            return dataIn.readBoolean();
        } catch (IOException e) {
            String msg = "I/O error while reading boolean.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int readInt() throws JournalException {
        consumed = true;

        try {
            return dataIn.readInt();
        } catch (IOException e) {
            String msg = "I/O error while reading integer.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long readLong() throws JournalException {
        consumed = true;

        try {
            return dataIn.readLong();
        } catch (IOException e) {
            String msg = "I/O error while reading long.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String readString() throws JournalException {
        consumed = true;

        try {
            boolean isNull = dataIn.readBoolean();
            if (isNull) {
                return null;
            } else {
                return dataIn.readUTF();
            }
        } catch (IOException e) {
            String msg = "I/O error while reading string.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void readFully(byte[] b) throws JournalException {
        consumed = true;

        try {
            dataIn.readFully(b);
        } catch (IOException e) {
            String msg = "I/O error while reading byte array.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * Close this record, eventually skipping unconsumed bytes.
     *
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        if (length != 0) {
            if (!consumed) {
                skip(length);
            }
        } else {
            dataIn.close();
        }
    }

    /**
     * Skip exactly <code>n</code> bytes. Throws if less bytes are skipped.
     *
     * @param n bytes to skip
     * @throws IOException if an I/O error occurs, or less than
     *                     <code>n</code> bytes were skipped.
     */
    private void skip(long n) throws IOException {
        long skiplen = n;
        while (skiplen > 0) {
            long skipped = dataIn.skip(skiplen);
            if (skipped <= 0) {
                break;
            }
            skiplen -= skipped;
        }
        if (skiplen != 0) {
            String msg = "Should have skipped " + n
                + " bytes, only " + (n - skiplen) + " skipped.";
            throw new IOException(msg);
        }
    }

    /**
     * Unsupported methods when appending.
     */
    public void writeByte(int n) throws JournalException {
        throw unsupported();
    }

    public void writeChar(char c) throws JournalException {
        throw unsupported();
    }

    public void writeBoolean(boolean b) throws JournalException {
        throw unsupported();
    }

    public void writeInt(int n) throws JournalException {
        throw unsupported();
    }

    public void writeLong(long n) throws JournalException {
        throw unsupported();
    }

    public void writeString(String s) throws JournalException {
        throw unsupported();
    }

    public void writeQName(Name name) throws JournalException {
        throw unsupported();
    }

    public void write(byte[] b) throws JournalException {
        throw unsupported();
    }

    public long update() throws JournalException {
        throw unsupported();
    }

    public void cancelUpdate() {
    }

    private JournalException unsupported() {
        String msg = "Record has been opened read-only.";
        return new JournalException(msg);
    }
}
