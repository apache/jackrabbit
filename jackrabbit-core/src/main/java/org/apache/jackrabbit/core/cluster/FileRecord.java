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
import java.io.IOException;

/**
 * Represents a file-based record. Physically, a file record starts with its creator
 * in a length-prefixed, UTF-encoded string, followed by a 4 byte indicating the
 * length of data. All further fields are record-specific.
 */
class FileRecord {

    /**
     * Record creator.
     */
    //private final String creator;
    private final byte[] creator;

    /**
     * Record length.
     */
    private final int length;

    /**
     * Input stream associated with record data.
     */
    private final DataInputStream dataIn;

    /**
     * Revision.
     */
    private long revision;

    /**
     * Flag indicating whether the data associated with this record has been consumed.
     */
    private boolean consumed;

    /**
     * Creates a new instance of this class. Used when opening an existing record.
     *
     * @param creator creator of this record
     * @param length record length
     * @param dataIn input stream containing record data
     */
    public FileRecord(byte[] creator, int length, DataInputStream dataIn) {
        this.creator = creator;
        this.length = length;
        this.dataIn = dataIn;
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
     * Set the journal revision associated with this record. Called after creation
     * of the file record.
     *
     * @param revision revision
     */
    void setRevision(long revision) {
        this.revision = revision;
    }

    /**
     * Return the journal counter associated with the next record. A file record's
     * size is the size of the length-prefixed creator string plus the size of
     * the length-prefixed data.
     *
     * @return next revision
     */
    public long getNextRevision() {
        return revision + FileRecordLog.getRecordSize(creator, length);
    }

    /**
     * Return the creator of this record.
     *
     * @return creator
     */
    public byte[] getCreator() {
        return creator;
    }

    /**
     * Return an input on this record.
     *
     * @param resolver resolver to use when mapping prefixes to full names
     * @return record input
     */
    public RecordInput getInput(NamespaceResolver resolver) {
        consumed = true;
        return new RecordInput(dataIn, resolver);
    }

    /**
     * Skip over this record, positioning the underlying input stream
     * on the next available record.
     *
     * @throws IOException if an I/O error occurs
     */
    public void skip() throws IOException {
        if (!consumed) {
            long skiplen = length;
            while (skiplen > 0) {
                long skipped = dataIn.skip(skiplen);
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
}