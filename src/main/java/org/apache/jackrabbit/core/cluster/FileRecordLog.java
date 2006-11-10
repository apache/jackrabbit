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

import java.io.File;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;

/**
 * A file record log is a file containing {@link FileRecord}s. Internally,
 * the first 8 bytes contain the revision this log starts with.
 */
class FileRecordLog {

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
                minRevision = in.readLong();
                maxRevision = minRevision + file.length() - 8;
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
            String msg = "Seek allowed exactly once.";
            throw new IllegalStateException(msg);
        }
        open();

        long skiplen = revision - minRevision + 8;
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
     * Append a record to this log.
     *
     * @param record record to add
     * @throws IOException if an I/O error occurs
     */
    public void append(FileRecord record) throws IOException {
        DataOutputStream out = new DataOutputStream(new FileOutputStream(file, true));
        try {
            if (isNew) {
                out.writeLong(record.getRevision());
            }
            record.append(out);
        } finally {
            out.close();
        }
    }

    /**
     * Open this log.
     *
     * @throws IOException if an I/O error occurs
     */
    private void open() throws IOException {
        in = new DataInputStream(new BufferedInputStream(
                new FileInputStream(file)));
    }

    /**
     * Return the underlying input stream.
     *
     * @return underlying input stream
     */
    protected DataInputStream getInputStream() {
        if (in == null) {
            String msg = "Input stream not open.";
            throw new IllegalStateException(msg);
        }
        return in;
    }

    /**
     * Close this log.
     *
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
    }
}