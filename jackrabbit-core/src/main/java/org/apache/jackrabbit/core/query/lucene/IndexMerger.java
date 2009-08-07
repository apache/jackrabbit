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

import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * Merges indexes in a separate daemon thread.
 */
class IndexMerger extends Thread implements IndexListener {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(IndexMerger.class);

    /**
     * Marker task to signal the background thread to quit.
     */
    private static final Merge QUIT = new Merge(new Index[0]);

    /**
     * minMergeDocs config parameter.
     */
    private int minMergeDocs = SearchIndex.DEFAULT_MIN_MERGE_DOCS;

    /**
     * maxMergeDocs config parameter
     */
    private int maxMergeDocs = SearchIndex.DEFAULT_MAX_MERGE_DOCS;

    /**
     * mergeFactor config parameter
     */
    private int mergeFactor = SearchIndex.DEFAULT_MERGE_FACTOR;

    /**
     * Queue of merge Tasks
     */
    private final Buffer mergeTasks = BufferUtils.blockingBuffer(new UnboundedFifoBuffer());

    /**
     * List of id <code>Term</code> that identify documents that were deleted
     * while a merge was running.
     */
    private final List deletedDocuments = Collections.synchronizedList(new ArrayList());

    /**
     * List of <code>IndexBucket</code>s in ascending document limit.
     */
    private final List indexBuckets = new ArrayList();

    /**
     * The <code>MultiIndex</code> this index merger is working on.
     */
    private final MultiIndex multiIndex;

    /**
     * Monitor object to synchronize merge calculation.
     */
    private final Object lock = new Object();

    /**
     * Mutex that is acquired when replacing indexes on MultiIndex.
     */
    private final Sync indexReplacement = new Mutex();

    /**
     * When released, indicates that this index merger is idle.
     */
    private final Sync mergerIdle = new Mutex();

    /**
     * Creates an <code>IndexMerger</code>.
     *
     * @param multiIndex the <code>MultiIndex</code>.
     */
    IndexMerger(MultiIndex multiIndex) {
        this.multiIndex = multiIndex;
        setName("IndexMerger");
        setDaemon(true);
        try {
            mergerIdle.acquire();
        } catch (InterruptedException e) {
            // will never happen, lock is free upon construction
            throw new InternalError("Unable to acquire mutex after construction");
        }
    }

    /**
     * Informs the index merger that an index was added / created.
     *
     * @param name the name of the index.
     * @param numDocs the number of documents it contains.
     */
    void indexAdded(String name, int numDocs) {
        if (numDocs < 0) {
            throw new IllegalArgumentException("numDocs must be positive");
        }
        // multiple threads may enter this method:
        // - the background thread of this IndexMerger, when it replaces indexes
        //   after a successful merge
        // - a regular thread that updates the workspace
        //
        // therefore we have to synchronize this block
        synchronized (lock) {
            // initially create buckets
            if (indexBuckets.size() == 0) {
                long lower = 0;
                long upper = minMergeDocs;
                while (upper < maxMergeDocs) {
                    indexBuckets.add(new IndexBucket(lower, upper, true));
                    lower = upper + 1;
                    upper *= mergeFactor;
                }
                // one with upper = maxMergeDocs
                indexBuckets.add(new IndexBucket(lower, maxMergeDocs, false));
                // and another one as overflow, just in case...
                indexBuckets.add(new IndexBucket(maxMergeDocs + 1, Long.MAX_VALUE, false));
            }

            // put index in bucket
            IndexBucket bucket = (IndexBucket) indexBuckets.get(indexBuckets.size() - 1);
            for (int i = 0; i < indexBuckets.size(); i++) {
                bucket = (IndexBucket) indexBuckets.get(i);
                if (bucket.fits(numDocs)) {
                    break;
                }
            }
            bucket.add(new Index(name, numDocs));

            if (log.isDebugEnabled()) {
                log.debug("index added: name=" + name + ", numDocs=" + numDocs);
            }

            // if bucket does not allow merge, we don't have to continue
            if (!bucket.allowsMerge()) {
                return;
            }

            // check if we need a merge
            if (bucket.size() >= mergeFactor) {
                long targetMergeDocs = bucket.upper;
                targetMergeDocs = Math.min(targetMergeDocs * mergeFactor, maxMergeDocs);
                // sum up docs in bucket
                List indexesToMerge = new ArrayList();
                int mergeDocs = 0;
                for (Iterator it = bucket.iterator(); it.hasNext() && mergeDocs <= targetMergeDocs;) {
                    indexesToMerge.add(it.next());
                }
                if (indexesToMerge.size() > 2) {
                    // found merge
                    Index[] idxs = (Index[]) indexesToMerge.toArray(new Index[indexesToMerge.size()]);
                    bucket.removeAll(indexesToMerge);
                    if (log.isDebugEnabled()) {
                        log.debug("requesting merge for " + indexesToMerge);
                    }
                    mergeTasks.add(new Merge(idxs));
                    log.debug("merge queue now contains " + mergeTasks.size() + " tasks.");
                }
            }
        }
    }

    /**
     * @inheritDoc
     */
    public void documentDeleted(Term id) {
        log.debug("document deleted: " + id.text());
        deletedDocuments.add(id);
    }

    /**
     * When the calling thread returns this index merger will be idle, that is
     * there will be no merge tasks pending anymore. The method returns immediately
     * if there are currently no tasks pending at all.
     */
    void waitUntilIdle() throws InterruptedException {
        mergerIdle.acquire();
        // and immediately release again
        mergerIdle.release();
    }

    /**
     * Signals this <code>IndexMerger</code> to stop and waits until it
     * has terminated.
     */
    void dispose() {
        log.debug("dispose IndexMerger");
        // get mutex for index replacements
        try {
            indexReplacement.acquire();
        } catch (InterruptedException e) {
            log.warn("Interrupted while acquiring index replacement sync: " + e);
            // try to stop IndexMerger without the sync
        }

        // clear task queue
        mergeTasks.clear();

        // send quit
        mergeTasks.add(QUIT);
        log.debug("quit sent");

        try {
            // give the merger thread some time to quit,
            // it is possible that the merger is busy working on a large index.
            // if that is the case we will just ignore it and the daemon will
            // die without being able to finish the merge.
            // at this point it is not possible anymore to replace indexes
            // on the MultiIndex because we hold the indexReplacement Sync.
            this.join(500);
            if (isAlive()) {
                log.info("Unable to stop IndexMerger. Daemon is busy.");
            } else {
                log.debug("IndexMerger thread stopped");
            }
            log.debug("merge queue size: " + mergeTasks.size());
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for IndexMerger thread to terminate.");
        }
    }

    /**
     * Implements the index merging.
     */
    public void run() {
        for (;;) {
            boolean isIdle = false;
            if (mergeTasks.size() == 0) {
                mergerIdle.release();
                isIdle = true;
            }
            Merge task = (Merge) mergeTasks.remove();
            if (task == QUIT) {
                mergerIdle.release();
                break;
            }
            if (isIdle) {
                try {
                    mergerIdle.acquire();
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    log.warn("Unable to acquire mergerIdle sync");
                }
            }

            log.debug("accepted merge request");

            // reset deleted documents
            deletedDocuments.clear();

            // get readers
            String[] names = new String[task.indexes.length];
            for (int i = 0; i < task.indexes.length; i++) {
                names[i] = task.indexes[i].name;
            }
            try {
                log.debug("create new index");
                PersistentIndex index = multiIndex.getOrCreateIndex(null);
                boolean success = false;
                try {

                    log.debug("get index readers from MultiIndex");
                    IndexReader[] readers = multiIndex.getIndexReaders(names, this);
                    try {
                        // do the merge
                        long time = System.currentTimeMillis();
                        index.addIndexes(readers);
                        time = System.currentTimeMillis() - time;
                        int docCount = 0;
                        for (int i = 0; i < readers.length; i++) {
                            docCount += readers[i].numDocs();
                        }
                        log.info("merged " + docCount + " documents in " + time + " ms into " + index.getName() + ".");
                    } finally {
                        for (int i = 0; i < readers.length; i++) {
                            try {
                                Util.closeOrRelease(readers[i]);
                            } catch (IOException e) {
                                log.warn("Unable to close IndexReader: " + e);
                            }
                        }
                    }

                    // inform multi index
                    // if we cannot get the sync immediately we have to quit
                    if (!indexReplacement.attempt(0)) {
                        log.debug("index merging canceled");
                        break;
                    }
                    try {
                        log.debug("replace indexes");
                        multiIndex.replaceIndexes(names, index, deletedDocuments);
                    } finally {
                        indexReplacement.release();
                    }

                    success = true;

                } finally {
                    if (!success) {
                        // delete index
                        log.debug("deleting index " + index.getName());
                        multiIndex.deleteIndex(index);
                    }
                }
            } catch (Throwable e) {
                log.error("Error while merging indexes: ", e);
            }
        }
        log.info("IndexMerger terminated");
    }

    //-----------------------< merge properties >-------------------------------

    /**
     * The merge factor.
     */
    public void setMergeFactor(int mergeFactor) {
        this.mergeFactor = mergeFactor;
    }


    /**
     * The initial threshold for number of documents to merge to a new index.
     */
    public void setMinMergeDocs(int minMergeDocs) {
        this.minMergeDocs = minMergeDocs;
    }

    /**
     * The maximum number of document to merge.
     */
    public void setMaxMergeDocs(int maxMergeDocs) {
        this.maxMergeDocs = maxMergeDocs;
    }

    //------------------------------< internal >--------------------------------

    /**
     * Implements a simple struct that holds the name of an index and how
     * many document it contains. <code>Index</code> is comparable using the
     * number of documents it contains.
     */
    private static final class Index implements Comparable {

        /**
         * The name of the index.
         */
        private final String name;

        /**
         * The number of documents the index contains.
         */
        private final int numDocs;

        /**
         * Creates a new index struct.
         *
         * @param name name of an index.
         * @param numDocs number of documents it contains.
         */
        Index(String name, int numDocs) {
            this.name = name;
            this.numDocs = numDocs;
        }

        /**
         * Indexes are first ordered by {@link #numDocs} and then by {@link
         * #name}.
         *
         * @param o the other <code>Index</code>.
         * @return a negative integer, zero, or a positive integer as this
         *         Index is less than, equal to, or greater than the specified
         *         Index.
         */
        public int compareTo(Object o) {
            Index other = (Index) o;
            int val = numDocs < other.numDocs ? -1 : (numDocs == other.numDocs ? 0 : 1);
            if (val != 0) {
                return val;
            } else {
                return name.compareTo(other.name);
            }
        }

        /**
         * @inheritDoc
         */
        public String toString() {
            return name + ":" + numDocs;
        }
    }

    /**
     * Defines a merge task, to merge a couple of indexes into a new index.
     */
    private static final class Merge {

        private final Index[] indexes;

        /**
         * Merge task, to merge <code>indexes</code> into a new index with
         * <code>name</code>.
         *
         * @param indexes the indexes to merge.
         */
        Merge(Index[] indexes) {
            this.indexes = new Index[indexes.length];
            System.arraycopy(indexes, 0, this.indexes, 0, indexes.length);
        }
    }

    /**
     * Implements a <code>List</code> with a document limit value. An
     * <code>IndexBucket</code> contains {@link Index}es with documents less
     * or equal the document limit of the bucket.
     */
    private static final class IndexBucket extends ArrayList {

        /**
         * The lower document limit.
         */
        private final long lower;

        /**
         * The upper document limit.
         */
        private final long upper;

        /**
         * Flag indicating if indexes in this bucket can be merged.
         */
        private final boolean allowMerge;

        /**
         * Creates a new <code>IndexBucket</code>. Limits are both inclusive.
         *
         * @param lower document limit.
         * @param upper document limit.
         * @param allowMerge if indexes in this bucket can be merged.
         */
        IndexBucket(long lower, long upper, boolean allowMerge) {
            this.lower = lower;
            this.upper = upper;
            this.allowMerge = allowMerge;
        }

        /**
         * Returns <code>true</code> if the number of documents fit in this
         * <code>IndexBucket</code>; otherwise <code>false</code>
         *
         * @param numDocs the number of documents.
         * @return <code>true</code> if <code>numDocs</code> fit.
         */
        boolean fits(long numDocs) {
            return numDocs >= lower && numDocs <= upper;
        }

        /**
         * Returns <code>true</code> if indexes in this bucket can be merged.
         *
         * @return <code>true</code> if indexes in this bucket can be merged.
         */
        boolean allowsMerge() {
            return allowMerge;
        }
    }
}
