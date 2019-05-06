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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.store.Directory;

import java.util.TreeMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.io.IOException;

/**
 * <code>IndexHistory</code> implements a history of index segments. Whenever
 * the index is flushed a new {@link IndexInfos} instance is created which
 * represents the current state of the index. This includes the names of the
 * index segments as well as their current generation number.
 */
class IndexHistory {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(IndexHistory.class);

    /**
     * Name of the file that contains the index infos.
     */
    private static final String INDEXES = "indexes";

    /**
     * the directory from where to read the index history.
     */
    private final Directory indexDir;

    /**
     * The maximum age (in milliseconds) of an index infos generation until it
     * is removed.
     */
    private final long maxAge;

    /**
     * Maps generation (Long) to {@link IndexInfos}. Youngest generation first
     * (-> higher value).
     */
    private final Map<Long, IndexInfos> indexInfosMap = new TreeMap<Long, IndexInfos>(Collections.reverseOrder());

    /**
     * Creates a new <code>IndexHistory</code> from the given <code>dir</code>.
     *
     * @param dir the directory from where to read the index history.
     * @param maxAge the maximum age in milliseconds for unused index infos.
     * @throws IOException if an error occurs while reading the index history.
     */
    IndexHistory(Directory dir, long maxAge) throws IOException {
        this.indexDir = dir;
        this.maxAge = maxAge;
        // read all index infos
        String[] names = dir.listAll();
        if (names != null) {
            for (String name : names) {
                if (name.startsWith(INDEXES)) {
                    long gen;
                    if (name.length() == INDEXES.length()) {
                        gen = 0;
                    } else if (name.charAt(INDEXES.length()) == '_') {
                        gen = Long.parseLong(name.substring(INDEXES.length() + 1), Character.MAX_RADIX);
                    } else {
                        continue;
                    }
                    try {
                        IndexInfos infos = new IndexInfos(dir, INDEXES, gen);
                        indexInfosMap.put(gen, infos);
                    } catch (IOException e) {
                        log.warn("ignoring invalid index infos file: " + name);
                    }
                }
            }
        }
    }

    /**
     * Returns the time when the index segment with the given <code>indexName</code>
     * was in use for the last time. The returned time does not accurately
     * say until when an index segment was in use, but it does guarantee that
     * the index segment in question was not in use anymore at the returned
     * time.
     * <p>
     * There are two special cases of return values:
     * <ul>
     * <li>{@link Long#MAX_VALUE}: indicates that the index segment is still in active use.</li>
     * <li>{@link Long#MIN_VALUE}: indicates that there is no index segment with the given name.</li>
     * </ul>
     *
     * @param indexName name of an index segment.
     * @return the time when the index segment with the given name was in use
     *          the last time.
     */
    long getLastUseOf(String indexName) {
        Long previous = null;
        for (Map.Entry<Long, IndexInfos> entry : indexInfosMap.entrySet()) {
            IndexInfos infos = entry.getValue();
            if (infos.contains(indexName)) {
                if (previous == null) {
                    // still in use
                    return Long.MAX_VALUE;
                } else {
                    return previous;
                }
            }
            previous = infos.getLastModified();
        }
        return Long.MIN_VALUE;
    }

    /**
     * Removes index infos older than {@link #maxAge} from this history.
     */
    void pruneOutdated() {
        long threshold = System.currentTimeMillis() - maxAge;
        log.debug("Pruning index infos older than: " + threshold + "(" + indexDir + ")");
        Iterator<IndexInfos> it = indexInfosMap.values().iterator();
        // never prune the current generation
        if (it.hasNext()) {
            IndexInfos infos = it.next();
            log.debug("Skipping first index infos. generation=" + infos.getGeneration());
        }
        while (it.hasNext()) {
            IndexInfos infos = (IndexInfos) it.next();
            if (infos.getLastModified() < threshold) {
                // check associated redo log
                try {
                    String logName = getRedoLogName(infos.getGeneration());
                    if (indexDir.fileExists(logName)) {
                        long lastModified = indexDir.fileModified(logName);
                        if (lastModified > threshold) {
                            log.debug("Keeping redo log with generation={}, timestamp={}",
                                    infos.getGeneration(), lastModified);
                            continue;
                        }
                        // try do delete it
                        try {
                            indexDir.deleteFile(logName);
                            log.debug("Deleted redo log with generation={}, timestamp={}",
                                    infos.getGeneration(), lastModified);
                        } catch (IOException e) {
                            log.warn("Unable to delete: " + indexDir + "/" + logName);
                            continue;
                        }
                    }
                    // delete index infos
                    try {
                        indexDir.deleteFile(infos.getFileName());
                        log.debug("Deleted index infos with generation={}",
                                infos.getGeneration());
                        it.remove();
                    } catch (IOException e) {
                        log.warn("Unable to delete: " + indexDir + "/" + infos.getFileName());
                    }
                } catch (IOException e) {
                    log.warn("Failed to check if {} is outdated: {}",
                            infos.getFileName(), e);
                }
            }
        }
    }

    /**
     * Adds an index infos to the history. This method will not modify nor keep
     * a reference to the passed <code>infos</code>.
     *
     * @param infos the index infos to add.
     */
    void addIndexInfos(IndexInfos infos) {
        // must clone infos because it is modifiable
        indexInfosMap.put(infos.getGeneration(), infos.clone());
    }

    //-------------------------------< internal >-------------------------------

    /**
     * Returns the name of the redo log file with the given generation.
     *
     * @param generation the index infos generation.
     * @return the name of the redo log file with the given generation.
     */
    String getRedoLogName(long generation) {
        if (generation == 0) {
            return DefaultRedoLog.REDO_LOG;
        } else {
            return DefaultRedoLog.REDO_LOG_PREFIX +
                    Long.toString(generation, Character.MAX_RADIX) +
                    DefaultRedoLog.DOT_LOG;
        }
    }
}
