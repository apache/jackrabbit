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

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Record cursor that returns unseen revisions in ascending order on every
 * iteration.
 */
public class FileRecordIterator implements RecordIterator {

    /**
     * Log files to scan for revisions.
     */
    private File[] logFiles;

    /**
     * Current revision being visited.
     */
    private long revision;

    /**
     * Last revision to visit.
     */
    private long stopRevision;

    /**
     * Namespace resolver.
     */
    private NamespaceResolver resolver;

    /**
     * Name and Path resolver.
     */
    private NamePathResolver npResolver;

    /**
     * Current record log, containing file records.
     */
    private FileRecordLog recordLog;

    /**
     * Current record.
     */
    private ReadRecord record;

    /**
     * Creates a new instance of this class.
     *
     * @param logFiles available log files, sorted ascending by age
     * @param startRevision start point (exclusive)
     * @param stopRevision stop point (inclusive)
     */
    public FileRecordIterator(File[] logFiles, long startRevision, long stopRevision,
                              NamespaceResolver resolver, NamePathResolver npResolver) {
        this.logFiles = logFiles;
        this.revision = startRevision;
        this.stopRevision = stopRevision;
        this.resolver = resolver;
        this.npResolver = npResolver;
    }


    /**
     * Return a flag indicating whether there are next records.
     */
    public boolean hasNext() {
        return revision < stopRevision;
    }

    /**
     * {@inheritDoc}
     */
    public Record nextRecord() throws NoSuchElementException, JournalException {
        if (!hasNext()) {
            String msg = "No next revision.";
            throw new NoSuchElementException(msg);
        }
        try {
            if (record != null) {
                record.close();
                record = null;
            }
        } catch (IOException e) {
            close();
            String msg = "Unable to skip over record.";
            throw new JournalException(msg, e);
        }

        if (recordLog != null) {
            if (!recordLog.contains(revision)) {
                recordLog.close();
                recordLog = null;
            }
        }

        try {
            if (recordLog == null) {
                recordLog = getRecordLog(revision);
            }
        } catch (IOException e) {
            String msg = "Unable to open record log with revision: " + revision;
            throw new JournalException(msg, e);
        }

        try {
            record = recordLog.read(resolver, npResolver);
            revision = record.getRevision();
            return record;
        } catch (IOException e) {
            String msg = "Unable to read record with revision: " + revision;
            throw new JournalException(msg, e);
        }
    }

    /**
     * Close this cursor, releasing its resources.
     */
    public void close() {
        if (recordLog != null) {
            recordLog.close();
        }
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

}
