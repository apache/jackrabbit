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

import org.apache.jackrabbit.name.NamespaceResolver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.DataInput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

/**
 * Represents a file-based record. Physically, a file record contains its length in the
 * first 4 bytes, immediately followed by its creator in a length-prefixed, UTF-encoded
 * string. All further fields are record-specific.
 */
class FileRecord {

    /**
     * Indicator for a literal UUID.
     */
    static final byte UUID_LITERAL = 'L';

    /**
     * Indicator for a UUID index.
     */
    static final byte UUID_INDEX = 'I';

    /**
     * Revision.
     */
    private long revision;

    /**
     * Underlying input stream.
     */
    private DataInputStream in;

    /**
     * File use when creating a new record.
     */
    private File file;

    /**
     * Underlying output stream.
     */
    private DataOutputStream out;

    /**
     * Record length.
     */
    private int length;

    /**
     * Creator of a record.
     */
    private String creator;

    /**
     * Bytes used by creator when written in UTF encoding and length-prefixed.
     */
    private int creatorLength;

    /**
     * Flag indicating whether bytes need to be skipped at the end.
     */
    private boolean consumed;

    /**
     * Creates a new file record. Used when opening an existing record.
     *
     * @param revision revision this record represents
     * @param in underlying input stream
     * @throws IOException if reading the creator fails
     */
    public FileRecord(long revision, InputStream in)
            throws IOException {

        this.revision = revision;
        if (in instanceof DataInputStream) {
            this.in = (DataInputStream) in;
        } else {
            this.in = new DataInputStream(in);
        }
        this.length = this.in.readInt();

        readCreator();
    }

    /**
     * Creates a new file record. Used when creating a new record.
     *
     * @param creator creator of this record
     * @param file underlying (temporary) file
     * @throws IOException if writing the creator fails
     */
    public FileRecord(String creator, File file) throws IOException {

        this.creator = creator;
        this.file = file;

        this.out = new DataOutputStream(new FileOutputStream(file));

        writeCreator();
    }

    /**
     * Return the journal revision associated with this record.
     *
     * @return revision
     */
    public long getRevision() {
        return revision;
    }

    /**
     * Set the journal revision associated with this record.
     *
     * @param revision journal revision
     */
    public void setRevision(long revision) {
        this.revision = revision;
    }

    /**
     * Return the journal counter associated with the next record.
     *
     * @return next revision
     */
    public long getNextRevision() {
        return revision + length + 4;
    }

    /**
     * Return the creator of this record.
     *
     * @return creator
     */
    public String getCreator() {
        return creator;
    }

    /**
     * Return an input on this record.
     *
     * @param resolver resolver to use when mapping prefixes to full names
     * @return record input
     */
    public FileRecordInput getInput(NamespaceResolver resolver) {
        consumed = true;
        return new FileRecordInput(in, resolver);
    }

    /**
     * Return an output on this record.
     *
     * @param resolver resolver to use when mapping full names to prefixes
     * @return record output
     */
    public FileRecordOutput getOutput(NamespaceResolver resolver) {
        return new FileRecordOutput(this, out, resolver);
    }

    /**
     * Append this record to some output stream.
     *
     * @param out outputstream to append to
     */
    void append(DataOutputStream out) throws IOException {
        out.writeInt(length);

        byte[] buffer = new byte[8192];
        int len;

        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } finally {
            in.close();
        }
    }

    /**
     * Skip over this record, positioning the underlying input stream
     * on the next available record.
     *
     * @throws IOException if an I/O error occurs
     */
    void skip() throws IOException {
        if (!consumed) {
            long skiplen = length - creatorLength;
            while (skiplen > 0) {
                long skipped = in.skip(skiplen);
                if (skipped <= 0) {
                    break;
                }
                skiplen -= skipped;
            }
            if (skiplen != 0) {
                String msg = "Unable to skip remaining bytes.";
                throw new IOException(msg);
            }
        }
    }

    /**
     * Invoked when output has been closed.
     */
    void closed() {
        length = (int) file.length();
    }

    /**
     * Read creator from the underlying data input stream.
     *
     * @throws IOException if an I/O error occurs
     */
    private void readCreator() throws IOException {
        UTFByteCounter counter = new UTFByteCounter(in);
        creator = DataInputStream.readUTF(counter);
        creatorLength = counter.getBytes();
    }

    /**
     * Write creator to the underlying data output stream.
     *
     * @throws IOException if an I/O error occurs
     */
    private void writeCreator() throws IOException {
        out.writeUTF(creator);
    }

    /**
     * UTF byte counter. Counts the bytes actually read from a given
     * <code>DataInputStream</code> that make up a UTF-encoded string.
     */
    static class UTFByteCounter implements DataInput {

        /**
         * Underlying input stream.
         */
        private final DataInputStream in;

        /**
         * UTF length.
         */
        private int bytes;

        /**
         * Create a new instance of this class.
         *
         * @param in underlying data input stream
         */
        public UTFByteCounter(DataInputStream in) {
            this.in = in;
        }

        /**
         * Return the number of bytes read from the underlying input stream.
         *
         * @return number of bytes
         */
        public int getBytes() {
            return bytes;
        }

        /**
         * @see java.io.DataInputStream#readUnsignedShort()
         *
         * Remember number of bytes read.
         */
        public int readUnsignedShort() throws IOException {
            try {
                return in.readUnsignedShort();
            } finally {
                bytes += 2;
            }
        }

        /**
         * @see java.io.DataInputStream#readUnsignedShort()
         *
         * Remember number of bytes read.
         */
        public void readFully(byte b[]) throws IOException {
            try {
                in.readFully(b);
            } finally {
                bytes += b.length;
            }
        }

        /**
         * @see java.io.DataInputStream#readUnsignedShort()
         *
         * Remember number of bytes read.
         */
        public void readFully(byte b[], int off, int len) throws IOException {
            try {
                in.readFully(b, off, len);
            } finally {
                bytes += b.length;
            }
        }

        /**
         * Methods not implemented.
         */
        public byte readByte() throws IOException {
            throw new IllegalStateException("Unexpected call, deliberately not implemented.");
        }

        public char readChar() throws IOException {
            throw new IllegalStateException("Unexpected call, deliberately not implemented.");
        }

        public double readDouble() throws IOException {
            throw new IllegalStateException("Unexpected call, deliberately not implemented.");
        }

        public float readFloat() throws IOException {
            throw new IllegalStateException("Unexpected call, deliberately not implemented.");
        }

        public int readInt() throws IOException {
            throw new IllegalStateException("Unexpected call, deliberately not implemented.");
       }

        public int readUnsignedByte() throws IOException {
            throw new IllegalStateException("Unexpected call, deliberately not implemented.");
        }

        public long readLong() throws IOException {
            throw new IllegalStateException("Unexpected call, deliberately not implemented.");
        }

        public short readShort() throws IOException {
            throw new IllegalStateException("Unexpected call, deliberately not implemented.");
        }

        public boolean readBoolean() throws IOException {
            throw new IllegalStateException("Unexpected call, deliberately not implemented.");
        }

        public int skipBytes(int n) throws IOException {
            throw new IllegalStateException("Unexpected call, deliberately not implemented.");
        }

        public String readLine() throws IOException {
            throw new IllegalStateException("Unexpected call, deliberately not implemented.");
        }

        public String readUTF() throws IOException {
            throw new IllegalStateException("Unexpected call, deliberately not implemented.");
        }
    }
}