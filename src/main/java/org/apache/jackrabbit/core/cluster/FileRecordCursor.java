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
import java.io.IOException;

/**
 * Record cursor that returns unseen revisions in ascending order on every
 * iteration. When iterating, a record must either be completely processed
 * or its {@link FileRecord#skip()} method must be invoked to guarantee
 * that this cursor is pointing at the next record.
 */
class FileRecordCursor {

    /**
     * Log files to scan for revisions.
     */
    private File[] logFiles;

    /**
     * Next revision to visit.
     */
    private long nextRevision;

    /**
     * Last revision to visit.
     */
    private long lastRevision;

    /**
     * Current record log, containing file records.
     */
    private FileRecordLog recordLog;

    /**
     * Current record.
     */
    private FileRecord record;

    /**
     * Creates a new instance of this class.
     *
     * @param logFiles available log files, sorted ascending by age
     * @param firstRevision first revision to return
     * @param lastRevision last revision to return
     */
    public FileRecordCursor(File[] logFiles, long firstRevision, long lastRevision) {
        this.logFiles = logFiles;
        this.nextRevision = firstRevision;
        this.lastRevision = lastRevision;
    }


    /**
     * Return a flag indicating whether there are next records.
     */
    public boolean hasNext() {
        return nextRevision < lastRevision;
    }

    /**
     * Returns the next record.
     *
     * @throws IllegalStateException if no next revision exists
     * @throws IOException if an I/O error occurs
     */
    public FileRecord next() throws IOException {
        if (!hasNext()) {
            String msg = "No next revision.";
            throw new IllegalStateException(msg);
        }
        if (record != null) {
            record.skip();
            record = null;
        }
        if (recordLog != null) {
            if (!recordLog.contains(nextRevision)) {
                recordLog.close();
                recordLog = null;
            }
        }
        if (recordLog == null) {
            recordLog = getRecordLog(nextRevision);
        }
        record = recordLog.read();
        record.setRevision(nextRevision);
        nextRevision = record.getNextRevision();
        return record;
    }

    /**
     * Return record log containing a given revision.
     *
     * @param revision revision to locate
     * @return record log containing that revision
     * @throws IOException if an I/O error occurs
     */
    private FileRecordLog getRecordLog(long revision) throws IOException {
        for (int i = 0; i < logFiles.length; i++) {
            FileRecordLog recordLog = new FileRecordLog(logFiles[i]);
            if (recordLog.contains(revision)) {
                recordLog.seek(revision);
                return recordLog;
            }
        }
        String msg = "No log file found containing revision: " + revision;
        throw new IOException(msg);
    }

    /**
     * Close this cursor, releasing its resources.
     */
    public void close() {
        if (recordLog != null) {
            recordLog.close();
        }
    }
}