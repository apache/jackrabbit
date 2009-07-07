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
import java.nio.channels.FileLock;

/**
 * Maintains a file-based revision counter with locking, assuring uniqueness.
 */
class LockableFileRevision {

    /**
     * Logger.
     */
    private static final Logger log = LoggerFactory.getLogger(LockableFileRevision.class);

    /**
     * Underlying file.
     */
    private final File file;

    /**
     * Underlying random access file.
     */
    private RandomAccessFile raf;

    /**
     * File lock.
     */
    private FileLock lock;

    /**
     * Current lock count.
     */
    private int locks;

    /**
     * Creates a new file based revision counter.
     *
     * @param file holding global counter
     */
    public LockableFileRevision(File file) {
        this.file = file;

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            String msg = "I/O error while attempting to create new file '" + file + "': " + e.getMessage();
            log.warn(msg);
        }
    }

    /**
     * Lock underlying file.
     *
     * @param shared whether to allow other readers or not
     */
    public synchronized void lock(boolean shared) throws JournalException {
        if (lock == null) {
            try {
                raf = new RandomAccessFile(file, shared ? "r" : "rw");
                lock = raf.getChannel().lock(0L, Long.MAX_VALUE, shared);
            } catch (IOException e) {
                String msg = "I/O error occurred.";
                throw new JournalException(msg, e);
            } finally {
                if (lock == null && raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e) {
                        String msg = "I/O error while closing file " + file.getPath() + ": " + e.getMessage();
                        log.warn(msg);
                    }
                    raf = null;
                }
            }
        }
        locks++;
    }

    /**
     * Unlock underlying file.
     */
    public synchronized void unlock() {
        if (lock != null && --locks == 0) {
            try {
                lock.release();
            } catch (IOException e) {
                String msg = "I/O error while releasing lock: " + e.getMessage();
                log.warn(msg);
            }
            lock = null;

            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    String msg = "I/O error while closing file: " + e.getMessage();
                    log.warn(msg);
                }
            }
            raf = null;
        }
    }

    /**
     * Return current counter value.
     *
     * @return counter value
     * @throws JournalException if some error occurs
     */
    public long get() throws JournalException {
        lock(true);

        try {
            long value = 0L;
            if (raf.length() > 0) {
                raf.seek(0L);
                value = raf.readLong();
            }
            return value;

        } catch (IOException e) {
            throw new JournalException("I/O error occurred: ", e);
        } finally {
            unlock();
        }
    }

    /**
     * Set current counter value.
     *
     * @param value new counter value
     * @throws JournalException if some error occurs
     */
    public void set(long value) throws JournalException {
        lock(false);

        try {
            raf.seek(0L);
            raf.writeLong(value);
        } catch (IOException e) {
            throw new JournalException("I/O error occurred.", e);
        } finally {
            unlock();
        }
    }

}
