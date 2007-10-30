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

import org.apache.jackrabbit.namespace.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;

/**
 * File-based journal implementation that appends journal records to a single
 * file.<p/>
 * It is configured through the following properties:
 * <ul>
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
        if (!rootDirectory.exists() || !rootDirectory.isDirectory()) {
            String msg = "Directory specified does either not exist " +
                    "or is not a directory: " + directory;
            throw new JournalException(msg);
        }

        journalFile = new File(rootDirectory, basename + "." + LOG_EXTENSION);
        globalRevision = new LockableFileRevision(new File(rootDirectory, REVISION_NAME));

        log.info("FileJournal initialized at path: " + directory);
    }

    /**
     * {@inheritDoc}
     */
    protected long getRevision() throws JournalException {
        return globalRevision.get();
    }

    /**
     * {@inheritDoc}
     */
    protected RecordIterator getRecords(long startRevision)
            throws JournalException {

        long stopRevision = getRevision();

        File[] logFiles = null;
        if (startRevision < stopRevision) {
            logFiles = rootDirectory.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.startsWith(basename + ".");
                }
            });
            Arrays.sort(logFiles, new Comparator() {
                public int compare(Object o1, Object o2) {
                    File f1 = (File) o1;
                    File f2 = (File) o2;
                    return f1.compareTo(f2);
                }
            });
        }
        return new FileRecordIterator(logFiles, startRevision, stopRevision, getResolver(), getNamePathResolver());
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
                switchLogs();
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
    public void close() {}

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
    private void switchLogs() {
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(basename + ".");
            }
        };
        File[] files = rootDirectory.listFiles(filter);
        Arrays.sort(files, new Comparator() {
            public int compare(Object o1, Object o2) {
                File f1 = (File) o1;
                File f2 = (File) o2;
                return f2.compareTo(f1);
            }
        });
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            String name = file.getName();
            int sep = name.lastIndexOf('.');
            if (sep != -1) {
                String ext = name.substring(sep + 1);
                if (ext.equals(LOG_EXTENSION)) {
                    file.renameTo(new File(rootDirectory, name + ".1"));
                } else {
                    try {
                        int version = Integer.parseInt(ext);
                        String newName = name.substring(0, sep + 1) +
                                String.valueOf(version + 1);
                        file.renameTo(new File(rootDirectory, newName));
                    } catch (NumberFormatException e) {
                        log.warn("Bogusly named journal file, skipped: " + file);
                    }
                }
            }
        }
    }
}
