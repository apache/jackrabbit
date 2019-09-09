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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.Collections;

import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Memory-based journal, useful for testing purposes only.
 */
public class MemoryJournal extends AbstractJournal {

    /**
     * Default read delay: none.
     */
    private static final long DEFAULT_READ_DELAY = 0;

    /**
     * Default write delay: none.
     */
    private static final long DEFAULT_WRITE_DELAY = 0;

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(MemoryJournal.class);

    /**
     * Revision.
     */
    private InstanceRevision revision = new MemoryRevision();

    /**
     * List of records.
     */
    private List<MemoryRecord> records = Collections.synchronizedList(new ArrayList<MemoryRecord>());

    /**
     * Set the read delay, i.e. the time in ms to wait before returning
     * a record.
     */
    private long readDelay = DEFAULT_READ_DELAY;

    /**
     * Set the write delay, i.e. the time in ms to wait before appending
     * a record.
     */
    private long writeDelay = DEFAULT_WRITE_DELAY;

    /**
     * Flag indicating whether this journal is closed.
     */
    private boolean closed;

    /**
     * {@inheritDoc}
     */
    public InstanceRevision getInstanceRevision() throws JournalException {
        return revision;
    }

    /**
     * {@inheritDoc}
     */
    public void init(String id, NamespaceResolver resolver)
            throws JournalException {

        super.init(id, resolver);
    }

    /**
     * {@inheritDoc}
     */
    protected void doLock() throws JournalException {
        checkState();
    }

    @Override
    protected void appending(AppendRecord record) {
        record.setRevision(records.size()+1);
    }

    /**
     * {@inheritDoc}
     */
    protected void append(AppendRecord record, InputStream in, int length)
            throws JournalException {

        checkState();

        byte[] data = new byte[length];
        int off = 0;

        while (off < data.length) {
            try {
                int len = in.read(data, off, data.length - off);
                if (len < 0) {
                    String msg = "Unexpected end of record after " + off + " bytes.";
                    throw new JournalException(msg);
                }
                off += len;
            } catch (IOException e) {
                String msg = "I/O error after " + off + " bytes.";
                throw new JournalException(msg, e);
            }
        }
        try {
            Thread.sleep(writeDelay);
        } catch (InterruptedException e) {
            throw new JournalException("Interrupted in append().");
        }
        records.add(new MemoryRecord(getId(), record.getProducerId(), data));
    }

    /**
     * {@inheritDoc}
     */
    protected void doUnlock(boolean successful) {
        try {
            checkState();
        } catch (JournalException e) {
            log.warn("Journal already closed while unlocking.");
        }
    }

    /**
     * {@inheritDoc}
     */
    public RecordIterator getRecords(long startRevision)
            throws JournalException {

        checkState();

        startRevision = Math.max(startRevision, 0);
        long stopRevision = records.size();

        return new MemoryRecordIterator(startRevision, stopRevision);
    }

    /**
     * {@inheritDoc}
     */
    public RecordIterator getRecords() throws JournalException {
        return new MemoryRecordIterator(0, records.size());
    }

    /**
     * Set records. Used to share records between two journal implementations.
     *
     * @param records array list that should back up this memory journal
     */
    public void setRecords(List<MemoryRecord> records) {
        this.records = records;
    }

    /**
     * Return the read delay in milliseconds.
     *
     * @return read delay
     */
    public long getReadDelay() {
        return readDelay;
    }

    /**
     * Set the read delay in milliseconds.
     *
     * @param readDelay read delay
     */
    public void setReadDelay(long readDelay) {
        this.readDelay = readDelay;
    }

    /**
     * Return the write delay in milliseconds.
     *
     * @return write delay
     */
    public long getWriteDelay() {
        return writeDelay;
    }

    /**
     * Set the write delay in milliseconds.
     *
     * @param writeDelay write delay
     */
    public void setWriteDelay(long writeDelay) {
        this.writeDelay = writeDelay;
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        closed = true;
    }

    /**
     * Check state of this journal.
     */
    private void checkState() throws JournalException {
        if (closed) {
            throw new JournalException("Journal closed.");
        }
    }

    /**
     * Memory record.
     */
    public static class MemoryRecord {

        /**
         * Journal id.
         */
        private String journalId;

        /**
         * Producer id.
         */
        private String producerId;

        /**
         * Data.
         */
        private byte[] data;

        /**
         * Create a new instance of this class
         *
         * @param journalId journal id
         * @param producerId producer id
         * @param data data
         */
        public MemoryRecord(String journalId, String producerId, byte[] data) {
            this.journalId = journalId;
            this.producerId = producerId;
            this.data = data;
        }

        /**
         * Return the journal id.
         *
         * @return the journal id
         */
        public String getJournalId() {
            return journalId;
        }

        /**
         * Return the producer id.
         *
         * @return the producer id
         */
        public String getProducerId() {
            return producerId;
        }

        /**
         * Return the data.
         *
         * @return data
         */
        public byte[] getData() {
            return data;
        }
    }

    /**
     * Record iterator implementation.
     */
    public class MemoryRecordIterator implements RecordIterator {

        /**
         * Current revision.
         */
        private long revision;

        /**
         * Last revision.
         */
        private final long stopRevision;

        /**
         * Create a new instance of this class.
         *
         * @param startRevision start revision
         * @param stopRevision stop revision
         */
        public MemoryRecordIterator(long startRevision, long stopRevision) {
            this.revision = startRevision;
            this.stopRevision = stopRevision;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return revision < stopRevision;
        }

        /**
         * {@inheritDoc}
         */
        public Record nextRecord() throws NoSuchElementException,
                JournalException {

            int index = (int) revision;
            MemoryRecord record = records.get(index);

            checkState();

            byte[] data = record.getData();
            DataInputStream dataIn = new DataInputStream(
                    new ByteArrayInputStream(data));

            try {
                Thread.sleep(readDelay);
            } catch (InterruptedException e) {
                throw new JournalException("Interrupted in read().");
            }

            return new ReadRecord(record.getJournalId(), record.getProducerId(),
                    ++revision, dataIn, data.length,
                    getResolver(), getNamePathResolver());
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            // nothing to be done here
        }
    }
}
