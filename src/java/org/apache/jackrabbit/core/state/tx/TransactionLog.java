/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.core.state.tx;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Logs operations of a transaction.
 */
class TransactionLog {

    /**
     * Writer for transaction log
     */
    private RandomAccessFile file;

    /**
     * Position for last transaction start
     */
    private long start;

    /**
     * Create a new instance of this class
     *
     * @param file underlying file for this transaction log
     */
    public TransactionLog(File file) throws IOException {
        this.file = new RandomAccessFile(file, "rw");
        this.file.setLength(0);
    }

    /**
     * Close the transaction log. Closes the underlying file.
     */
    public void close() {
        if (file != null) {
            try {
                file.close();
            } catch (IOException e) {
            }

            file = null;
        }
    }

    /**
     * Playback transaction log, i.e. scan all entries upto the last
     * uncommitted transaction and re-create the items
     */
    public synchronized void playback(PlaybackListener listener)
            throws TransactionException {

        checkState();

        try {
            file.seek(start);

            for (; ;) {
                String s = file.readLine();
                if (s == null || s.equals("")) {
                    break;
                }
                switch (s.charAt(0)) {
                    case 'I':
                        listener.elementCreated(elementIdFromLogRecord(s));
                        break;
                    case 'U':
                        listener.elementUpdated(elementIdFromLogRecord(s));
                        break;
                    case 'D':
                        listener.elementDeleted(elementIdFromLogRecord(s));
                        break;
                    case 'P':
                    case 'C':
                    case 'R':
                        break;
                    default:
                        throw new IOException("Bad log record");
                }
            }
        } catch (IOException e) {
            throw new TransactionException("Unable to playback log.", e);
        }
    }

    /**
     * Add a log record to the transaction log. Invoked when a new item
     * has been created.
     *
     * @param id item id
     * @throws TransactionException if an error occurs
     */
    public synchronized void logCreated(String id) throws TransactionException {
        checkState();

        try {
            file.writeBytes("I-");
            file.writeBytes(id);
            file.writeByte('\n');
        } catch (IOException e) {
            throw new TransactionException("Unable to create log record.", e);
        }
    }

    /**
     * Add a log record to the transaction log. Invoked when an existing item
     * has been updated.
     *
     * @param id item id
     * @throws TransactionException if an error occurs
     */
    public synchronized void logUpdated(String id) throws TransactionException {
        checkState();

        try {
            file.writeBytes("U-");
            file.writeBytes(id);
            file.writeByte('\n');
        } catch (IOException e) {
            throw new TransactionException("Unable to create log record.", e);
        }
    }

    /**
     * Add a log record to the transaction log. Invoked when an existing item
     * has been deleted.
     *
     * @param id item id
     * @throws TransactionException if an error occurs
     */
    public synchronized void logDeleted(String id) throws TransactionException {
        checkState();

        try {
            file.writeBytes("D-");
            file.writeBytes(id);
            file.writeByte('\n');
        } catch (IOException e) {
            throw new TransactionException("Unable to create log record.", e);
        }
    }

    /**
     * Add a log record to the transaction log. Invoked when a transaction has
     * been prepared.
     *
     * @throws TransactionException if an error occurs
     */
    public synchronized void logPrepare() throws TransactionException {
        checkState();

        try {
            file.writeBytes("P");
            file.writeByte('\n');
        } catch (IOException e) {
            throw new TransactionException("Unable to create log record.", e);
        }
    }

    /**
     * Add a log record to the transaction log. Invoked when a transaction has
     * been committed.
     *
     * @throws TransactionException if an error occurs
     */
    public synchronized void logCommit() throws TransactionException {
        checkState();

        try {
            file.writeBytes("C");
            file.writeByte('\n');
            start = file.getFilePointer();
        } catch (IOException e) {
            throw new TransactionException("Unable to create log record.", e);
        }
    }

    /**
     * Add a log record to the transaction log. Invoked when a transaction has
     * been rolled back.
     *
     * @throws TransactionException if an error occurs
     */
    public synchronized void logRollback() throws TransactionException {
        checkState();

        try {
            file.writeBytes("R");
            file.writeByte('\n');
            start = file.getFilePointer();
        } catch (IOException e) {
            throw new TransactionException("Unable to create log record.", e);
        }
    }

    /**
     * Check state of this transaction log.
     */
    private void checkState() throws IllegalStateException {
        if (file == null) {
            throw new IllegalStateException("Log already closed.");
        }
    }

    /**
     * Recreates an element id from a log record. If a bad format is discovered,
     * an IOException is generated.
     *
     * @param s log record
     * @return element id
     * @throws IOException if the log record is invalid
     */
    private String elementIdFromLogRecord(String s) throws IOException {
        if (s.length() > 2) {
            return s.substring(2);
        }
        throw new IOException("Bad log record");
    }
}
