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

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Default temporary record used for appending to some journal.
 */
public class AppendRecord extends AbstractRecord {

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(AppendRecord.class);

    /**
     * Default prefix for appended records in the file system.
     */
    private static final String DEFAULT_PREFIX = "journal";

    /**
     * Default extension for appended records in the file system.
     */
    private static final String DEFAULT_EXT = ".tmp";

    /**
     * Journal where record is being appended.
     */
    private final AbstractJournal journal;

    /**
     * Producer identifier.
     */
    private final String producerId;

    /**
     * This record's revision.
     */
    private long revision;

    /**
     * Underlying file.
     */
    private File file;

    /**
     * Underlying data output.
     */
    private DataOutputStream dataOut;

    /**
     * Create a new instance of this class.
     *
     * @param journal journal where record is being appended
     * @param producerId producer identifier
     */
    public AppendRecord(AbstractJournal journal, String producerId) {
        super(journal.getResolver());

        this.journal = journal;
        this.producerId = producerId;
        this.revision = 0L;
    }

    /**
     * {@inheritDoc}
     */
    public String getJournalId() {
        return journal.getId();
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
    public void writeByte(int n) throws JournalException {
        open();

        try {
            dataOut.writeByte(n);
        } catch (IOException e) {
            String msg = "I/O error while writing byte.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeChar(char c) throws JournalException {
        open();

        try {
            dataOut.writeChar(c);
        } catch (IOException e) {
            String msg = "I/O error while writing character.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeBoolean(boolean b) throws JournalException {
        open();

        try {
            dataOut.writeBoolean(b);
        } catch (IOException e) {
            String msg = "I/O error while writing boolean.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeInt(int n) throws JournalException {
        open();

        try {
            dataOut.writeInt(n);
        } catch (IOException e) {
            String msg = "I/O error while writing integer.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeString(String s) throws JournalException {
        open();

        try {
            if (s == null) {
                dataOut.writeBoolean(true);
            } else {
                dataOut.writeBoolean(false);
                dataOut.writeUTF(s);
            }
        } catch (IOException e) {
            String msg = "I/O error while writing string.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void write(byte[] b) throws JournalException {
        open();

        try {
            dataOut.write(b);
        } catch (IOException e) {
            String msg = "I/O error while writing a byte array.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void update() throws JournalException {
        boolean succeeded = false;

        try {
            close();
            revision = journal.append(producerId, file);
            succeeded = true;
        } finally {
            dispose();

            journal.unlock(succeeded);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cancelUpdate() {
        if (dataOut != null) {
            dispose();
            
            journal.unlock(false);
        }
    }

    /**
     * Create temporary file and open data output on it.
     *
     * @throws JournalException
     */
    private void open() throws JournalException {
        if (file == null) {
            try {
                file = File.createTempFile(DEFAULT_PREFIX, DEFAULT_EXT);
                dataOut = new DataOutputStream(new FileOutputStream(file));
            } catch (IOException e) {
                String msg = "Unable to create temporary file.";
                throw new JournalException(msg, e);
            }
        }
    }

    /**
     * Close this record, keeping the underlying file.
     *
     * @throws JournalException if an error occurs
     */
    private void close() throws JournalException {
        if (dataOut != null) {
            try {
                dataOut.close();
            } catch (IOException e) {
                String msg = "I/O error while closing stream.";
                throw new JournalException(msg, e);
            } finally {
                dataOut = null;
            }
        }
    }

    /**
     * Dispose this record, deleting the underlying file.
     */
    private void dispose() {
        if (dataOut != null) {
            try {
                dataOut.close();
            } catch (IOException e) {
                String msg = "I/O error while closing stream.";
                log.warn(msg, e);
            } finally {
                dataOut = null;
            }
        }
        if (file != null) {
            file.delete();
            file = null;
        }
    }

    /**
     * Unsupported methods when appending.
     */
    public byte readByte() throws JournalException {
        throw unsupported();
    }

    public char readChar() throws JournalException {
        throw unsupported();
    }

    public boolean readBoolean() throws JournalException {
        throw unsupported();
    }

    public int readInt() throws JournalException {
        throw unsupported();
    }

    public String readString() throws JournalException {
        throw unsupported();
    }

    public void readFully(byte[] b) throws JournalException {
        throw unsupported();
    }

    private JournalException unsupported() {
        String msg = "Reading from an appended record is not supported.";
        return new JournalException(msg);
    }
}
