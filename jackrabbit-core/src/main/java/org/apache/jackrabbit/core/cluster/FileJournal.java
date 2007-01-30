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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.Comparator;

/**
 * File-based journal implementation. A directory specified as <code>directory</code>
 * bean property will contain log files and a global revision file, containing the
 * next available revision. When the current log file's size exceeds <code>maxSize</code>
 * bytes, it gets renamed to its name appended by '1'. At the same time, all log files
 * already having a version counter, get their version counter incremented by <code>1</code>.
 * <p/>
 * It is configured through the following properties:
 * <ul>
 * <li><code>revision</code>: the filename where the parent cluster node's revision
 * file should be written to; this is a required property with no default value</li>
 * <li><code>directory</code>: the shared directory where journal logs and read from
 * and written to; this is a required property with no default value</li>
 * <li><code>basename</code>: this is the basename of the journal logs created in
 * the shared directory; its default value is <code>journal</code></li>
 * <li><code>maximumSize</code>: this is the maximum size in bytes of a journal log
 * before a new log will be created; its default value is <code>1048576</code> (1MB)</li>
 * </ul>
 * <p/>
 * Technically, the global revision file contains the cumulated file position, i.e. if
 * there are <code>N</code> journal files, with file lengths <code>L[1]</code>...
 * <code>L[N]</code> (excluding the size of the file headers), then the global revision
 * will be L[1]+...+L[N].
 *
 * todo after some iterations, old files should be automatically compressed to save space
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
    private File root;

    /**
     * Journal file.
     */
    private File journal;

    /**
     * Global revision counter.
     */
    private FileRevision globalRevision;

    /**
     * Id as byte array.
     */
    private byte[] rawId;

    /**
     * Bean getter for journal directory.
     * @return directory
     */
    public String getDirectory() {
        return directory;
    }

    /**
     * Bean setter for journal directory.
     * @param directory directory used for journaling
     */
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    /**
     * Bean getter for base name.
     * @return base name
     */
    public String getBasename() {
        return basename;
    }

    /**
     * Bean setter for basename.
     * @param basename base name
     */
    public void setBasename(String basename) {
        this.basename = basename;
    }

    /**
     * Bean getter for maximum size.
     * @return maximum size
     */
    public int getMaximumSize() {
        return maximumSize;
    }

    /**
     * Bean setter for maximum size.
     * @param maximumSize maximum size
     */
    public void setMaximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
    }

    /**
     * {@inheritDoc}
     */
    public void init(String id, RecordProcessor processor, NamespaceResolver resolver)
            throws JournalException {
        
        super.init(id, processor, resolver);

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
        root = new File(directory);
        if (!root.exists() || !root.isDirectory()) {
            String msg = "Directory specified does either not exist or is not a directory: " + directory;
            throw new JournalException(msg);
        }
        try {
            rawId = toRawId(id);
        } catch (IOException e) {
            String msg = "Unable to convert '" + id + "' to its binary representation.";
            throw new JournalException(msg, e);
        }

        journal = new File(root, basename + "." + LOG_EXTENSION);
        globalRevision = new FileRevision(new File(root, REVISION_NAME));

        log.info("FileJournal initialized at path: " + directory);
    }

    /**
     * {@inheritDoc}
     */
    public void sync() throws JournalException {
        File[] logFiles = root.listFiles(new FilenameFilter() {
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

        long instanceValue = getLocalRevision();
        long globalValue = globalRevision.get();

        if (instanceValue < globalValue) {
            FileRecordCursor cursor = new FileRecordCursor(logFiles, instanceValue, globalValue);
            try {
                while (cursor.hasNext()) {
                    FileRecord record = cursor.next();
                    if (!Arrays.equals(rawId, record.getCreator())) {
                        process(record);
                    } else {
                        log.info("Log entry matches journal id, skipped: " + record.getRevision());
                    }
                    setLocalRevision(record.getNextRevision());
                }
            } catch (IOException e) {
                String msg = "Unable to iterate over modified records.";
                throw new JournalException(msg, e);
            } finally {
                cursor.close();
            }
            log.info("Sync finished, instance revision is: " + getLocalRevision());
        }
    }

    /**
     * Process a record.
     *
     * @param record record to process
     * @throws JournalException if an error occurs
     */
    private void process(FileRecord record) throws JournalException {
        RecordInput in = record.getInput(resolver);

        try {
            process(record.getRevision(), in);
        } finally {
            in.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected long lockRevision() throws JournalException {
        globalRevision.lock(false);
        return globalRevision.get();
    }

    /**
     * {@inheritDoc}
     */
    protected void unlockRevision(boolean successful) {
        globalRevision.unlock();
    }

    /**
     * {@inheritDoc}
     */
    protected void append(long revision, File record) throws JournalException {
        try {
            FileRecordLog recordLog = new FileRecordLog(journal);
            if (!recordLog.isNew()) {
                if (revision - recordLog.getFirstRevision() > maximumSize) {
                    switchLogs();
                    recordLog = new FileRecordLog(journal);
                }
            }
            long nextRevision = recordLog.append(revision, rawId, record);
            globalRevision.set(nextRevision);
            setLocalRevision(nextRevision);

        } catch (IOException e) {
            String msg = "Unable to append new record to journal " + journal + ": " + e.getMessage();
            throw new JournalException(msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() {}

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
        File[] files = root.listFiles(filter);
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
                    file.renameTo(new File(root, name + ".1"));
                } else {
                    try {
                        int version = Integer.parseInt(ext);
                        String newName = name.substring(0, sep + 1) +
                                String.valueOf(version + 1);
                        file.renameTo(new File(newName));
                    } catch (NumberFormatException e) {
                        log.warn("Bogusly named journal file, skipped: " + file);
                    }
                }
            }
        }
    }

    /**
     * Convert an id given as string, to its raw form, i.e. to its binary
     * representation, encoded as UTF-8.
     *
     * @throws IOException if an I/O error occurs, which is very unlikely.
     */
    private static byte[] toRawId(String id) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeUTF(id);
        dos.close();

        byte[] b = bos.toByteArray();
        byte[] rawId = new byte[b.length - 2];
        System.arraycopy(b, 2, rawId, 0, rawId.length);
        return rawId;
    }
}
