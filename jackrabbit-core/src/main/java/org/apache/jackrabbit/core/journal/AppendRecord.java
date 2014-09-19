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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jackrabbit.core.data.db.ResettableTempFileInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
     * Default size for in-memory records.
     */
    private static final int DEFAULT_IN_MEMORY_SIZE = 1024;

    /**
     * Maximum size for in-memory records.
     */
    private static final int MAXIMUM_IN_MEMORY_SIZE = 65536;

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
     * Underlying data output.
     */
    private DataOutputStream dataOut;

    /**
     * Underlying byte output.
     */
    private ByteArrayOutputStream byteOut;

    /**
     * Underlying file.
     */
    private File file;

    /**
     * Underlying file output.
     */
    private FileOutputStream fileOut;

    /**
     * Flag indicating whether the output is closed.
     */
    private boolean outputClosed;

    /**
     * Create a new instance of this class.
     *
     * @param journal journal where record is being appended
     * @param producerId producer identifier
     */
    public AppendRecord(AbstractJournal journal, String producerId) {
        super(journal.getResolver(), journal.getNamePathResolver());

        this.journal = journal;
        this.producerId = producerId;
        this.revision = 0L;

        byteOut = new ByteArrayOutputStream(DEFAULT_IN_MEMORY_SIZE);
        dataOut = new DataOutputStream(byteOut);
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
     * Set the revision this record represents.
     *
     * @param revision revision
     */
    public void setRevision(long revision) {
        this.revision = revision;
    }

    /**
     * {@inheritDoc}
     */
    public void writeByte(int n) throws JournalException {
        checkOutput();

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
        checkOutput();

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
        checkOutput();

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
        checkOutput();

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
    public void writeLong(long n) throws JournalException {
        checkOutput();

        try {
            dataOut.writeLong(n);
        } catch (IOException e) {
            String msg = "I/O error while writing long.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeString(String s) throws JournalException {
        checkOutput();

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
        checkOutput();

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
    public long update() throws JournalException {
        boolean succeeded = false;

        try {
            int length = dataOut.size();
            closeOutput();

            InputStream in = openInput();

            try {
                journal.append(this, in, length);
                succeeded = true;
                return length;
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    String msg = "I/O error while closing stream.";
                    log.warn(msg, e);
                }
            }
        } finally {
            dispose();

            journal.unlock(succeeded);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cancelUpdate() {
        if (!outputClosed) {
            dispose();

            journal.unlock(false);
        }
    }

    /**
     * Open input on record written.
     */
    private InputStream openInput() throws JournalException {
        if (file != null) {
            try {
                return new ResettableTempFileInputStream(file);
            } catch (IOException e) {
                String msg = "Unable to open file input on: " + file.getPath();
                throw new JournalException(msg, e);
            }
        } else {
            return new ByteArrayInputStream(byteOut.toByteArray());
        }
    }

    /**
     * Check output size and eventually switch to file output.
     *
     * @throws JournalException
     */
    private void checkOutput() throws JournalException {
        if (outputClosed) {
            throw new IllegalStateException("Output closed.");
        }
        if (fileOut == null && byteOut.size() >= MAXIMUM_IN_MEMORY_SIZE) {
            try {
                file = File.createTempFile(DEFAULT_PREFIX, DEFAULT_EXT);
            } catch (IOException e) {
                String msg = "Unable to create temporary file.";
                throw new JournalException(msg, e);
            }
            try {
                fileOut = new FileOutputStream(file);
            } catch (FileNotFoundException e) {
                String msg = "Unable to open output stream on: " + file.getPath();
                throw new JournalException(msg, e);
            }
            dataOut = new DataOutputStream(new BufferedOutputStream(fileOut));

            try {
                dataOut.write(byteOut.toByteArray());
            } catch (IOException e) {
                String msg = "Unable to write in-memory record to file.";
                throw new JournalException(msg, e);
            }
        }
    }

    /**
     * Close output, keeping the underlying file.
     *
     * @throws JournalException if an error occurs
     */
    private void closeOutput() throws JournalException {
        if (!outputClosed) {
            try {
                if (fileOut != null) {
                    dataOut.flush();
                    fileOut.getFD().sync();
                    dataOut.close();
                }
            } catch (IOException e) {
                String msg = "I/O error while closing stream.";
                throw new JournalException(msg, e);
            } finally {
                outputClosed = true;
            }
        }
    }

    /**
     * Dispose this record, deleting the underlying file.
     */
    private void dispose() {
        if (!outputClosed) {
            try {
                dataOut.close();
            } catch (IOException e) {
                String msg = "I/O error while closing stream.";
                log.warn(msg, e);
            } finally {
                outputClosed = true;
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

    public long readLong() throws JournalException {
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
