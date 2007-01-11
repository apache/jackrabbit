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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * A file record log is a file containing {@link FileRecord}s. Physically,
 * the first 4 bytes contain a signature, followed by a major and minor version
 * (2 bytes each). The next 8 bytes contain the revision this log starts with.
 * After this, zero or more <code>FileRecord</code>s follow.
 */
class FileRecordLog {

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(FileRecordLog.class);

    /**
     * Record log signature.
     */
    private static final byte[] SIGNATURE = { 'J', 'L', 'O', 'G' };

    /**
     * Known major version.
     */
    private static final short MAJOR_VERSION = 1;

    /**
     * Known minor version.
     */
    private static final short MINOR_VERSION = 0;

    /**
     * Header size. This is the size of {@link #SIGNATURE}, {@link #MAJOR_VERSION},
     * {@link #MINOR_VERSION} and first revision (8 bytes).
     */
    private static final int HEADER_SIZE = 4 + 2 + 2 + 8;

    /**
     * Underlying file.
     */
    private File file;

    /**
     * Flag indicating whether this is a new log.
     */
    private boolean isNew;

    /**
     * Input stream used when seeking a specific record.
     */
    private DataInputStream in;

    /**
     * First revision available in this log.
     */
    private long minRevision;

    /**
     * First revision that is not available in this, but in the next log.
     */
    private long maxRevision;

    /**
     * Create a new instance of this class.
     *
     * @param file file containing record log
     * @throws IOException if an I/O error occurs
     */
    public FileRecordLog(File file) throws IOException {
        this.file = file;

        if (file.exists()) {
            DataInputStream in = new DataInputStream(new FileInputStream(file));

            try {
                readHeader(in);
                minRevision = in.readLong();
                maxRevision = minRevision + file.length() - HEADER_SIZE;
            } finally {
                in.close();
            }
        } else {
            isNew = true;
        }
    }

    /**
     * Return the first revision.
     *
     * @return first revision
     */
    public long getFirstRevision() {
        return minRevision;
    }

    /**
     * Return a flag indicating whether this record log contains a certain revision.
     *
     * @param revision revision to look for
     * @return <code>true</code> if this record log contain a certain revision;
     *         <code>false</code> otherwise
     */
    public boolean contains(long revision) {
        return (revision >= minRevision && revision < maxRevision);
    }

    /**
     * Return a flag indicating whether this record log is new.
     *
     * @return <code>true</code> if this record log is new;
     *         <code>false</code> otherwise
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * Seek an entry. This is an operation that allows the unterlying input stream
     * to be sequentially scanned and must therefore not be called twice.
     *
     * @param revision revision to seek
     * @throws IOException if an I/O error occurs
     */
    public void seek(long revision) throws IOException {
        if (in != null) {
            String msg = "Stream already open: seek() only allowed once.";
            throw new IllegalStateException(msg);
        }
        in = new DataInputStream(new BufferedInputStream(
                new FileInputStream(file)));
        skip(revision - minRevision + HEADER_SIZE);
    }

    /**
     * Skip exactly <code>n</code> bytes. Throws if less bytes are skipped.
     *
     * @param n bytes to skip
     * @throws IOException if an I/O error occurs, or less that <code>n</code> bytes
     *                     were skipped.
     */
    private void skip(long n) throws IOException {
        long skiplen = n;
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

    /**
     * Read the file record at the current seek position.
     *
     * @return file record
     * @throws IOException if an I/O error occurs
     */
    public FileRecord read() throws IOException {
        byte[] creator = new byte[in.readUnsignedShort()];
        in.readFully(creator);
        int length = in.readInt();
        return new FileRecord(creator, length, in);
    }

    /**
     * Append a record to this log. Returns the revision following this record.
     *
     * @param record record to add
     * @return next available revision
     * @throws IOException if an I/O error occurs
     */
    public long append(long revision, byte[] creator, File record) throws IOException {
        DataOutputStream out = new DataOutputStream(new FileOutputStream(file, true));
        try {
            int recordLength = (int) record.length();
            if (isNew) {
                writeHeader(out);
                out.writeLong(revision);
            }
            out.writeShort(creator.length);
            out.write(creator);
            out.writeInt(recordLength);
            append(record, out);
            return revision + getRecordSize(creator, recordLength);
        } finally {
            out.close();
        }
    }

    /**
     * Close this log.
     */
    public void close() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            String msg = "Error while closing record log: " + e.getMessage();
            log.warn(msg);
        }
    }

    /**
     * Return the size of a stored record . A stored record's size is the size of
     * the length-prefixed creator string plus the size of the length-prefixed data.
     *
     * @param creator creator string
     * @param length data length
     * @return size of a stored record
     */
    public static int getRecordSize(byte[] creator, int length) {
        return 2 + creator.length + 4 + length;
    }

    /**
     * Read signature and major/minor version of file and verify.
     *
     * @param in input stream
     * @throws IOException if an I/O error occurs or the file does
     *                     not have a valid header.
     */
    private void readHeader(DataInputStream in) throws IOException {
        byte[] signature = new byte[SIGNATURE.length];
        in.readFully(signature);

        for (int i = 0; i < SIGNATURE.length; i++) {
            if (signature[i] != SIGNATURE[i]) {
                String msg = "Record log '" + file.getPath() +
                        "' has wrong signature: " + toHexString(signature);
                throw new IOException(msg);
            }
        }

        short major = in.readShort();
        in.readShort(); // minor version not used yet

        if (major > MAJOR_VERSION) {
            String msg = "Record log '" + file.getPath() +
                    "' has incompatible major version: " + major;
            throw new IOException(msg);
        }
    }

    /**
     * Write signature and major/minor.
     *
     * @param out input stream
     * @throws IOException if an I/O error occurs.
     */
    private void writeHeader(DataOutputStream out) throws IOException {
        out.write(SIGNATURE);
        out.writeShort(MAJOR_VERSION);
        out.writeShort(MINOR_VERSION);
    }

    /**
     * Append a record to this log's output stream.
     *
     * @param record record to append
     * @param out where to append to
     */
    private static void append(File record, DataOutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int len;

        InputStream in = new BufferedInputStream(new FileInputStream(record));
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
     * Convert a byte array to its hexadecimal string representation.
     */
    private static String toHexString(byte[] b) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            String s = Integer.toHexString(b[i] & 0xff).toUpperCase();
            if (s.length() == 1) {
                buf.append('0');
            }
            buf.append(s);
        }
        return buf.toString();
    }
}