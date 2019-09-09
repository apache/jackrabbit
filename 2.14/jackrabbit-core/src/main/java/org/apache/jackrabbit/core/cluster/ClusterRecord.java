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

import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.Record;

/**
 * Base cluster record. Used to serialize and deserialize cluster operations
 * using journal records.
 */
public abstract class ClusterRecord {

    /**
     * End marker.
     */
    protected static final char END_MARKER = '\0';

    /**
     * Journal record.
     */
    protected final Record record;

    /**
     * Workspace name.
     */
    protected String workspace;

    /**
     * Create a new instance of this class.
     *
     * @param record journal record
     * @param workspace workspace
     */
    protected ClusterRecord(Record record, String workspace) {
        this.record = record;
        this.workspace = workspace;
    }

    /**
     * Create a new instance of this class. Used for records that do not
     * have a workspace name.
     *
     * @param record journal record
     */
    protected ClusterRecord(Record record) {
        this(record, null);
    }

    /**
     * Deserialize this record.
     *
     * @throws JournalException if an error occurs
     */
    public final void read() throws JournalException {
        doRead();

        readEndMarker();
    }

    /**
     * Deserialize this record. Subclass responsibility.
     *
     * @throws JournalException if an error occurs
     */
    protected abstract void doRead() throws JournalException;

    /**
     * Serialize this record.
     *
     * @throws JournalException if an error occurs
     */
    public final void write() throws JournalException {
        record.writeString(workspace);

        doWrite();

        record.writeChar(END_MARKER);
    }

    /**
     * Serialize this record. Subclass responsibility.
     *
     * @throws JournalException if an error occurs
     */
    protected abstract void doWrite() throws JournalException;

    /**
     * Read end marker.
     *
     * @throws JournalException if an error occurs
     */
    protected void readEndMarker() throws JournalException {
        char c = record.readChar();
        if (c != END_MARKER) {
            String msg = "Expected end marker, found: " + c;
            throw new JournalException(msg);
        }
    }

    /**
     * Process this record, calling the appropriate <code>process</code>
     * method.
     *
     * @param processor processor
     */
    public abstract void process(ClusterRecordProcessor processor);

    /**
     * Update the record.
     *
     * @throws JournalException if an error occurs
     * @see Record#update()
     */
    public void update() throws JournalException {
        record.update();
    }

    /**
     * Cancel updating the record.
     *
     * @see Record#cancelUpdate()
     */
    public void cancelUpdate() {
        record.cancelUpdate();
    }

    /**
     * Return the record revision.
     *
     * @return record revision
     * @see Record#getRevision()
     */
    public long getRevision() {
        return record.getRevision();
    }

    /**
     * Return the workspace name.
     *
     * @return workspace name
     */
    public String getWorkspace() {
        return workspace;
    }
}
