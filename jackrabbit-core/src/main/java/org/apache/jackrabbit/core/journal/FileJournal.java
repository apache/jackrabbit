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

import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * File-based journal implementation that appends journal records to a single
 * file.
 * <p>
 * It is configured through the following properties:
 * <ul>
 * <li><code>revision</code>: the filename where the parent cluster node's revision
 * file should be written to; this is a required property with no default value</li>
 * <li><code>directory</code>: the directory where to keep the journal file as
 * well as the rotated files; this is a required property with no default value</li>
 * <li><code>basename</code>: the basename of journal files; the default
 * value is {@link #DEFAULT_BASENAME}</li>
 * <li><code>maximumSize</code>: the maximum size of an active journal file
 * before rotating it: the default value is {@link #DEFAULT_MAXSIZE} </li>
 * </ul>
 */
public class FileJournal extends AbstractJournal {

    /**
     * Default instance revision file name.
     */
    public static final String DEFAULT_INSTANCE_FILE_NAME = "revision.log";

    /**
     * Global revision counter name, located in the journal directory.
     */
    private static final String REVISION_NAME = "revision";

    /**
     * Log extension.
     */
    private static final String LOG_EXTENSION = "log";

    /**
     * Default base name for journal files.
     */
    private static final String DEFAULT_BASENAME = "journal";

    /**
     * Default max size of a journal file (1MB).
     */
    private static final int DEFAULT_MAXSIZE = 1048576;

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(FileJournal.class);

    /**
     * Directory name, bean property.
     */
    private String directory;

    /**
     * Journal file base name, bean property.
     */
    private String basename;

    /**
     * Maximum size of a journal file before a rotation takes place, bean property.
     */
    private int maximumSize;

    /**
     * Journal root directory.
     */
    private File rootDirectory;

    /**
     * Journal file.
     */
    private File journalFile;

    /**
     * Global revision counter.
     */
    private LockableFileRevision globalRevision;

    /**
     * {@inheritDoc}
     */
    public void init(String id, NamespaceResolver resolver) throws JournalException {
        super.init(id, resolver);

        if (getRevision() == null) {
            File repHome = getRepositoryHome();
            if (repHome == null) {
                String msg = "Revision not specified.";
                throw new JournalException(msg);
            }
            String revision = new File(repHome, DEFAULT_INSTANCE_FILE_NAME).getPath();
            log.info("Revision not specified, using: " + revision);
            setRevision(revision);
        }
        if (directory == null) {
            String msg = "Directory not specified.";
            throw new JournalException(msg);
        }
        if (basename == null) {
            basename = DEFAULT_BASENAME;
        }
        if (maximumSize == 0) {
            maximumSize = DEFAULT_MAXSIZE;
        }
        rootDirectory = new File(directory);

        // JCR-1341: Cluster Journal directory should be created automatically
        rootDirectory.mkdirs();

        if (!rootDirectory.exists() || !rootDirectory.isDirectory()) {
            String msg = "Directory specified does either not exist "
                + "or is not a directory: " + directory;
            throw new JournalException(msg);
        }

        journalFile = new File(rootDirectory, basename + "." + LOG_EXTENSION);
        globalRevision = new LockableFileRevision(new File(rootDirectory, REVISION_NAME));

        log.info("FileJournal initialized at path: " + directory);
    }

    /**
     * {@inheritDoc}
     */
    protected long getGlobalRevision() throws JournalException {
        return globalRevision.get();
    }

    /**
     * {@inheritDoc}
     */
    public RecordIterator getRecords(long startRevision)
            throws JournalException {

        long stopRevision = getGlobalRevision();

        File[] files = null;
        if (startRevision < stopRevision) {
            RotatingLogFile[] logFiles = RotatingLogFile.listFiles(rootDirectory, basename);
            files = new File[logFiles.length];
            for (int i = 0; i < files.length; i++) {
                files[i] = logFiles[i].getFile();
            }
        }
        return new FileRecordIterator(files, startRevision, stopRevision,
                getResolver(), getNamePathResolver());
    }

    /**
     * {@inheritDoc}
     */
    public RecordIterator getRecords() throws JournalException {
        long stopRevision = getGlobalRevision();
        long startRevision = 0;

        RotatingLogFile[] logFiles = RotatingLogFile.listFiles(rootDirectory, basename);
        File[] files = new File[logFiles.length];
        for (int i = 0; i < files.length; i++) {
            files[i] = logFiles[i].getFile();
            if (i == 0) {
                try {
                    FileRecordLog log = new FileRecordLog(files[i]);
                    startRevision = log.getPreviousRevision();
                } catch (IOException e) {
                    String msg = "Unable to read startRevision from first " +
                            "record log file";
                    throw new JournalException(msg, e);
                }
            }
        }
        return new FileRecordIterator(files, startRevision, stopRevision,
                getResolver(), getNamePathResolver());
    }

    /**
     * {@inheritDoc}
     */
    protected void doLock() throws JournalException {
        globalRevision.lock(false);
    }

    /**
     * {@inheritDoc}
     */
    protected void append(AppendRecord record, InputStream in, int length)
            throws JournalException {

        try {
            FileRecordLog recordLog = new FileRecordLog(journalFile);
            if (recordLog.exceeds(maximumSize)) {
                rotateLogs();
                recordLog = new FileRecordLog(journalFile);
            }
            if (recordLog.isNew()) {
                recordLog.init(globalRevision.get());
            }
            long revision = recordLog.append(getId(),
                    record.getProducerId(), in, length);
            globalRevision.set(revision);
            record.setRevision(revision);

        } catch (IOException e) {
            String msg = "Unable to append new record to journal '" + journalFile + "'.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void doUnlock(boolean successful) {
        globalRevision.unlock();
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
    }

    /**
     * {@inheritDoc}
     */
    public InstanceRevision getInstanceRevision() throws JournalException {
        return new FileRevision(new File(getRevision()), true);
    }

    /**
     * Bean getters
     */
    public String getDirectory() {
        return directory;
    }

    public String getBasename() {
        return basename;
    }

    public int getMaximumSize() {
        return maximumSize;
    }

    /**
     * Bean setters
     */
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setBasename(String basename) {
        this.basename = basename;
    }

    public void setMaximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
    }

    /**
     * Move away current journal file (and all other files), incrementing their
     * version counter. A file named <code>journal.N.log</code> gets renamed to
     * <code>journal.(N+1).log</code>, whereas the main journal file gets renamed
     * to <code>journal.1.log</code>.
     */
    private void rotateLogs() {
        RotatingLogFile[] logFiles = RotatingLogFile.listFiles(rootDirectory, basename);
        for (int i = 0; i < logFiles.length; i++) {
            logFiles[i].rotate();
        }
    }
}
