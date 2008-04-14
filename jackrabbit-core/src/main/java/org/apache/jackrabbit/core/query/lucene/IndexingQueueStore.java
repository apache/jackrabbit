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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.RandomAccessOutputStream;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.BufferedOutputStream;
import java.util.Set;
import java.util.HashSet;

/**
 * <code>IndexingQueueStore</code> implements the persistent store to keep
 * track of pending document in an indexing queue.
 */
class IndexingQueueStore {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(IndexingQueueStore.class);

    /**
     * Encoding of the indexing queue store.
     */
    private static final String ENCODING = "UTF-8";

    /**
     * Operation identifier for an added node.
     */
    private static final String ADD = "ADD";

    /**
     * Operation identifier for an removed node.
     */
    private static final String REMOVE = "REMOVE";

    /**
     * The UUID Strings of the pending documents.
     */
    private final Set pending = new HashSet();

    /**
     * The file system where to write the pending document UUIDs.
     */
    private final FileSystem fs;

    /**
     * The name of the file for the pending document UUIDs.
     */
    private final String fileName;

    /**
     * Non-null if we are currently writing to the file.
     */
    private Writer out;

    /**
     * Creates a new <code>IndexingQueueStore</code> using the given file
     * system.
     *
     * @param fs       the file system to use.
     * @param fileName the name of the file where to write the pending UUIDs
     *                 to.
     * @throws FileSystemException if an error ocurrs while reading pending
     *                             UUIDs.
     */
    IndexingQueueStore(FileSystem fs, String fileName) throws FileSystemException {
        this.fs = fs;
        this.fileName = fileName;
        readStore();
    }

    /**
     * @return the UUIDs of the pending text extraction jobs.
     */
    public String[] getPending() {
        return (String[]) pending.toArray(new String[pending.size()]);
    }

    /**
     * Adds a <code>uuid</code> to the store.
     *
     * @param uuid the uuid to add.
     * @throws IOException if an error occurs while writing.
     */
    public void addUUID(String uuid) throws IOException {
        writeEntry(ADD, uuid, getLog());
        pending.add(uuid);
    }

    /**
     * Removes a <code>uuid</code> from the store.
     *
     * @param uuid the uuid to add.
     * @throws IOException if an error occurs while writing.
     */
    public void removeUUID(String uuid) throws IOException {
        writeEntry(REMOVE, uuid, getLog());
        pending.remove(uuid);
    }

    /**
     * Commits the pending changes to the file.
     *
     * @throws IOException if an error occurs while writing.
     */
    public void commit() throws IOException {
        if (out != null) {
            out.flush();
            if (pending.size() == 0) {
                out.close();
                out = null;
                // truncate log
                try {
                    fs.getOutputStream(fileName).close();
                } catch (FileSystemException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Flushes and closes this queue store.
     *
     * @throws IOException if an error occurs while writing.
     */
    public void close() throws IOException {
        commit();
        if (out != null) {
            out.close();
        }
    }

    //----------------------------< internal >----------------------------------

    /**
     * Reads all pending UUIDs from the file and puts them into {@link
     * #pending}.
     *
     * @throws FileSystemException if an error occurs while reading.
     */
    private void readStore() throws FileSystemException {
        if (fs.exists(fileName)) {
            try {
                InputStream in = fs.getInputStream(fileName);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(in, ENCODING));
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        int idx = line.indexOf(' ');
                        if (idx == -1) {
                            // invalid line
                            log.warn("invalid line in {}: {}", fileName, line);
                        } else {
                            String cmd = line.substring(0, idx);
                            String uuid = line.substring(idx + 1, line.length());
                            if (ADD.equals(cmd)) {
                                pending.add(uuid);
                            } else if (REMOVE.equals(cmd)) {
                                pending.remove(uuid);
                            } else {
                                // invalid line
                                log.warn("invalid line in {}: {}", fileName, line);
                            }
                        }
                    }
                } finally {
                    in.close();
                }
            } catch (IOException e) {
                throw new FileSystemException(e.getMessage(), e);
            }
        }
    }

    /**
     * Writes an entry to the log file.
     *
     * @param op     the operation. Either {@link #ADD} or {@link #REMOVE}.
     * @param uuid   the uuid of the added or removed node.
     * @param writer the writer where the entry is written to.
     * @throws IOException if an error occurs when writing the entry.
     */
    private static void writeEntry(String op, String uuid, Writer writer) throws IOException {
        StringBuffer buf = new StringBuffer(op);
        buf.append(' ').append(uuid).append('\n');
        writer.write(buf.toString());
    }

    /**
     * Returns the writer to the log file.
     *
     * @return the writer to the log file.
     * @throws IOException if an error occurs while opening the log file.
     */
    private Writer getLog() throws IOException {
        if (out == null) {
            // open file
            try {
                long len = 0;
                if (fs.exists(fileName)) {
                    len = fs.length(fileName);
                }
                RandomAccessOutputStream raos
                        = fs.getRandomAccessOutputStream(fileName);
                raos.seek(len);
                // use buffering
                out = new OutputStreamWriter(
                        new BufferedOutputStream(raos, 1024),
                        ENCODING);
            } catch (FileSystemException e) {
                if (out != null) {
                    out.close();
                    out = null;
                }
                throw Util.createIOException(e);
            }
        }
        return out;
    }
}
