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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Maintains a file-based revision counter with locking, assuring uniqueness.
 */
public class FileRevision implements InstanceRevision {

    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(FileRevision.class);

    /**
     * Underlying random access file.
     */
    protected final RandomAccessFile raf;

    /**
     * Flag indicating whether to sync the file on every write.
     */
    protected final boolean sync;

    /**
     * Cached value.
     */
    protected long value;
    
    /**
     * Flag indicating whether this revision file is closed.
     */
    protected boolean closed;

    /**
     * Creates a new file based revision counter.
     *
     * @param file holding global counter
     * @param sync whether to sync the file on every write
     * 
     * @throws JournalException if some error occurs
     */
    public FileRevision(File file, boolean sync) throws JournalException {
        this.sync = sync;

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            raf = new RandomAccessFile(file, "rw");
            if (raf.length() == 0) {
                set(0);
            }
        } catch (IOException e) {
            String msg = "I/O error while attempting to create new file '" + file + "'.";
            throw new JournalException(msg, e);
        }
    }

    /**
     * Return current counter value.
     *
     * @return counter value
     * @throws JournalException if some error occurs
     */
    public synchronized long get() throws JournalException {
        try {
            if (closed) {
                throw new JournalException("Revision file closed.");
            }
            raf.seek(0L);
            value = raf.readLong();
            return value;
        } catch (IOException e) {
            throw new JournalException("I/O error occurred.", e);
        }
    }

    /**
     * Set current counter value.
     *
     * @param value new counter value
     * @throws JournalException if some error occurs
     */
    public synchronized void set(long value) throws JournalException {
        try {
            if (closed) {
                throw new JournalException("Revision file closed.");
            }
            raf.seek(0L);
            raf.writeLong(value);
            if (sync) {
                raf.getFD().sync();
            }
            this.value = value;
        } catch (IOException e) {
            throw new JournalException("I/O error occurred.", e);
        }
    }
    
    /**
     * Close file revision. Closes underlying random access file.
     */
    public synchronized void close() {
        try {
            raf.close();
            closed = true;
        } catch (IOException e) {
            log.warn("I/O error closing revision file.", e);
        }
    }
}
