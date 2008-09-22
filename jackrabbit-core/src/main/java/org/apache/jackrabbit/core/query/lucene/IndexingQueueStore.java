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
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.HashSet;

/**
 * <code>IndexingQueueStore</code> implements a store that keeps the uuids of
 * nodes that are pending in the indexing queue. Until Jackrabbit 1.4 this store
 * was also persisted to a {@link FileSystem}. Starting with 1.5 the pending
 * nodes are marked directly in the index with a special field.
 * See {@link FieldNames#REINDEXING_REQUIRED}.
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
     * The file system from where to read pending document UUIDs.
     */
    private final FileSystem fs;

    /**
     * The name of the file for the pending document UUIDs.
     */
    private final String fileName;

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
     */
    public void addUUID(String uuid) {
        pending.add(uuid);
    }

    /**
     * Removes a <code>uuid</code> from the store.
     *
     * @param uuid the uuid to add.
     */
    public void removeUUID(String uuid) {
        pending.remove(uuid);
    }

    /**
     * Closes this queue store.
     */
    public void close() {
        if (pending.isEmpty()) {
            try {
                if (fs.exists(fileName)) {
                    fs.deleteFile(fileName);
                }
            } catch (FileSystemException e) {
                log.warn("unable to delete " + fileName);
            }
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
}
