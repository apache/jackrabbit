/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.RandomAccessOutputStream;
import org.apache.jackrabbit.uuid.Constants;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Implements a redo log for the {@link VolatileIndex}. While nodes are added to
 * and removed from the volatile index (held in memory) a redo log is written to
 * keep track of the changes. In case the Jackrabbit process terminates
 * unexpected the redo log is applied when Jackrabbit is restarted the next
 * time.<br/>
 * When the {@link VolatileIndex} is merged with the peristent index the, redo
 * log is cleared.
 * <p/>
 * This class is not thread-safe.
 */
class RedoLog {

    /** Logger instance for this class */
    private static final Logger log = Logger.getLogger(RedoLog.class);

    /** Implements a {@link EntryCollector} with an empty collect method */
    private static final EntryCollector DUMMY_COLLECTOR = new EntryCollector() {
        public void collect(Entry entry) {
            // do nothing
        }
    };

    /** The log file */
    private final FileSystemResource logFile;

    /** The number of log enties in the log file */
    private int entryCount = 0;

    /** Writer to the log file */
    private Writer out;

    /**
     * Creates a new <code>RedoLog</code> instance based on the file
     * <code>logFile</code>
     * @param log the redo log file.
     */
    RedoLog(FileSystemResource log) throws FileSystemException {
        this.logFile = log;
        // create the log file if not there
        if (!log.exists()) {
            log.makeParentDirs();
            try {
                log.getOutputStream().close();
            } catch (IOException e) {
                throw new FileSystemException("Unable to create redo log file:", e);
            }
        }
        read(DUMMY_COLLECTOR);
    }

    /**
     * Returns <code>true</code> if this redo log contains any entries,
     * <code>false</code> otherwise.
     * @return <code>true</code> if this redo log contains any entries,
     * <code>false</code> otherwise.
     */
    boolean hasEntries() {
        return entryCount > 0;
    }

    /**
     * Returns the number of entries in this redo log.
     * @return the number of entries in this redo log.
     */
    int getSize() {
        return entryCount;
    }

    /**
     * Returns a collection with all {@link Entry} instances in the redo log.
     * @return an collection with all {@link Entry} instances in the redo log.
     * @throws FileSystemException if an error occurs while reading from the
     * redo log.
     */
    Collection getEntries() throws FileSystemException {
        final List entries = new ArrayList();
        read(new EntryCollector() {
            public void collect(Entry entry) {
                entries.add(entry);
            }
        });
        return entries;
    }

    /**
     * Informs this redo log that a node has been added.
     * @param uuid the uuid of the node.
     * @throws FileSystemException if the node cannot be written to the redo
     * log.
     */
    void nodeAdded(String uuid) throws FileSystemException {
        initOut();
        try {
            out.write(new Entry(uuid, Entry.NODE_ADDED).toString());
            out.write('\n');
            entryCount++;
        } catch (IOException e) {
            throw new FileSystemException(e.getMessage(), e);
        }
    }

    /**
     * Informs this redo log that a node has been removed.
     * @param uuid the uuid of the node.
     * @throws FileSystemException if the node cannot be written to the redo
     * log.
     */
    void nodeRemoved(String uuid) throws FileSystemException {
        initOut();
        try {
            out.write(new Entry(uuid, Entry.NODE_REMOVED).toString());
            out.write('\n');
            entryCount++;
        } catch (IOException e) {
            throw new FileSystemException(e.getMessage(), e);
        }
    }

    /**
     * Flushes all pending writes to the underlying file.
     * @throws FileSystemException if an error occurs while writing.
     */
    void flush() throws FileSystemException {
        try {
            if (out != null) {
                out.flush();
            }
        } catch (IOException e) {
            throw new FileSystemException(e.getMessage(), e);
        }
    }

    /**
     * Clears the redo log.
     * @throws FileSystemException if the redo log cannot be cleared.
     */
    void clear() throws FileSystemException {
        try {
            if (out != null) {
                out.close();
                out = null;
            }
            // truncate file
            logFile.getOutputStream().close();
            entryCount = 0;
        } catch (IOException e) {
            throw new FileSystemException(e.getMessage(), e);
        }
    }

    /**
     * Initializes the {@link #out} stream if it is not yet set.
     * @throws FileSystemException if an error occurs while creating the
     * output stream.
     */
    private void initOut() throws FileSystemException {
        if (out == null) {
            RandomAccessOutputStream raf = logFile.getRandomAccessOutputStream();
            // seek to the end of the file
            try {
                raf.seek(logFile.length());
            } catch (IOException e) {
                throw new FileSystemException(e.getMessage(), e);
            }
            out = new BufferedWriter(new OutputStreamWriter(raf));
        }
    }

    /**
     * Reads the log file and sets the {@link #entryCount} with the number
     * of entries read.
     * @param collector called back for each {@link Entry} read.
     * @throws FileSystemException if an error occurs while reading from the
     * log file.
     */
    private void read(EntryCollector collector) throws FileSystemException {
        InputStream in = logFile.getInputStream();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(logFile.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                try {
                    Entry e = Entry.fromString(line);
                    collector.collect(e);
                    entryCount++;
                } catch (IllegalArgumentException e) {
                    log.warn("Malformed redo entry: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new FileSystemException(e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.warn("Exception while closing redo log: " + e.toString());
                }
            }
        }
    }

    /**
     * Helper class that represents an entry in the redo log.
     */
    public static class Entry {

        /** The length of a log entry: UUID + &lt;space> + (ADD | REM) */
        private static final int ENTRY_LENGTH = Constants.UUID_FORMATTED_LENGTH + 4;

        /** Type constant for node added entry */
        static final int NODE_ADDED = 1;

        /** Type constant for node removed entry */
        static final int NODE_REMOVED = 2;

        /** Type string for node added */
        private static final String ADD = "ADD";

        /** Type string for node removed */
        private static final String REM = "REM";

        /** The uuid of the node */
        public final String uuid;

        /** The type of event */
        public final int type;

        /**
         * Creates a new log entry.
         * @param uuid the uuid of the node
         * @param type the event type.
         */
        private Entry(String uuid, int type) {
            this.uuid = uuid;
            this.type = type;
        }

        /**
         * Parses an line in the redo log and created a {@link Entry}.
         * @param logLine the line from the redo log.
         * @return a log <code>Entry</code>.
         * @throws IllegalArgumentException if the line is malformed.
         */
        static Entry fromString(String logLine) throws IllegalArgumentException {
            if (logLine.length() != ENTRY_LENGTH) {
                throw new IllegalArgumentException("Malformed log entry: " + logLine);
            }
            String uuid = logLine.substring(0, Constants.UUID_FORMATTED_LENGTH);
            String typeString = logLine.substring(Constants.UUID_FORMATTED_LENGTH + 1);
            if (ADD.equals(typeString)) {
                return new Entry(uuid, NODE_ADDED);
            } else if (REM.equals(typeString)) {
                return new Entry(uuid, NODE_REMOVED);
            } else {
                throw new IllegalArgumentException("Unrecognized type string in log entry: " + logLine);
            }
        }

        /**
         * Returns the string representation of this <code>Entry</code>:<br/>
         * UUID &lt;space> (ADD | REM)
         * @return the string representation of this <code>Entry</code>.
         */
        public String toString() {
            return uuid + " " + getStringForType(type);
        }

        /**
         * Returns the string representation for an entry <code>type</code>. If
         * <code>type</code> is {@link #NODE_ADDED}, <code>ADD</code> is
         * returned, otherwise <code>REM</code> is returned.
         * @param type the entry type.
         * @return the string representation for an entry <code>type</code>.
         */
        private static String getStringForType(int type) {
            if (type == NODE_ADDED) {
                return ADD;
            } else {
                return REM;
            }
        }
    }

    //-----------------------< internal >---------------------------------------

    /**
     * Helper interface to collect Entries read from the redo log.
     */
    interface EntryCollector {

        /** Called when an entry is created */
        void collect(Entry entry);
    }
}
