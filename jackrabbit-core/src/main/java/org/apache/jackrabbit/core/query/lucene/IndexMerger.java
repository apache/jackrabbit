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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Merges indexes in a separate daemon thread.
 */
class IndexMerger implements IndexListener {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(IndexMerger.class);

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
     * List of <code>IndexBucket</code>s in ascending document limit.
     */
    private final List<IndexBucket> indexBuckets = new ArrayList<IndexBucket>();

    /**
     * The <code>MultiIndex</code> this index merger is working on.
     */
    private final MultiIndex multiIndex;

    /**
     * The executor of the repository.
     */
    private final Executor executor;

    /**
     * Flag that indicates that this index merger is shuting down and should
     * quit. 
     */
    private final AtomicBoolean quit = new AtomicBoolean(false);

    /**
     * Flag that indicates if this index merger has already been started.
     * @see #start()
     */
    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    /**
     * Monitor object to synchronize merge calculation.
     */
    private final Object lock = new Object();

    /**
     * Read/write lock for index segment replacement. A shared read lock is
     * acquired for an index replacement. An exclusive write lock is acquired
     * when this index merger is shutting down, to prevent further index
     * replacements.
     */
    private final ReadWriteLock indexReplacement = new ReentrantReadWriteLock();

    /**
     * List of merger threads that are currently busy.
     */
    private final List<Worker> busyMergers = new ArrayList<Worker>();

    /**
     * Creates an <code>IndexMerger</code>.
     *
     * @param multiIndex the <code>MultiIndex</code>.
     * @param executor   the executor of the repository.
     */
    IndexMerger(MultiIndex multiIndex, Executor executor) {
        this.multiIndex = multiIndex;
        this.executor = executor;
    }

    /**
     * Starts this index merger.
     */
    void start() {
        isStarted.set(true);
        synchronized (busyMergers) {
            for (Worker worker : busyMergers) {
                worker.unblock();
            }
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
                    IndexBucket indexBucket = new IndexBucket(lower, upper, true);
                    indexBuckets.add(indexBucket);
                    if (log.isDebugEnabled()) {
                        log.debug("IndexBucket (" + indexBucket.toString() +") created.");
                    }
                    lower = upper + 1;
                    upper *= mergeFactor;
                }
                // one with upper = maxMergeDocs
                IndexBucket upperIndexBucket = new IndexBucket(lower, maxMergeDocs, false);
                indexBuckets.add(upperIndexBucket);
                if (log.isDebugEnabled()) {
                    log.debug("IndexBucket (" + upperIndexBucket.toString() + ") created.");
                }
                // and another one as overflow, just in case...
                IndexBucket overlowIndexBucket = new IndexBucket(maxMergeDocs + 1, Long.MAX_VALUE, false);
                indexBuckets.add(overlowIndexBucket);
                if (log.isDebugEnabled()) {
                    log.debug("IndexBucket (" + overlowIndexBucket.toString() + ") created.");
                }
            }

            // put index in bucket
            IndexBucket bucket = indexBuckets.get(indexBuckets.size() - 1);
            for (IndexBucket indexBucket : indexBuckets) {
                bucket = indexBucket;
                if (bucket.fits(numDocs)) {
                    break;
                }
            }
            bucket.add(new Index(name, numDocs));

            if (log.isDebugEnabled()) {
                log.debug("index added name: " + name + ", numDocs: " + numDocs + " into IndexBucket (" + bucket.toString() + ")");
            }

            // if bucket does not allow merge, we don't have to continue
            if (!bucket.allowsMerge()) {
                return;
            }

            // check if we need a merge
            if (bucket.size() >= mergeFactor) {
                long targetMergeDocs = bucket.upper;
                targetMergeDocs = Math.min(targetMergeDocs * mergeFactor, maxMergeDocs);
                if (log.isDebugEnabled()) {
                    log.debug("check merge for IndexBucket (" + bucket.toString() + "), targetMergeDocs: " + targetMergeDocs);
                }
                // sum up docs in bucket
                List<Index> indexesToMerge = new ArrayList<Index>();
                int mergeDocs = 0;
                for (Iterator<Index> it = bucket.iterator(); it.hasNext() && mergeDocs <= targetMergeDocs;) {
                    indexesToMerge.add(it.next());
                }
                if (indexesToMerge.size() > 2) {
                    // found merge
                    Index[] idxs = indexesToMerge.toArray(new Index[indexesToMerge.size()]);
                    bucket.removeAll(indexesToMerge);
                    if (log.isDebugEnabled()) {
                        log.debug("requesting merge for " + indexesToMerge);
                    }
                    addMergeTask(new Merge(idxs));
                    if (log.isDebugEnabled()) {
                        int numBusy;
                        synchronized (busyMergers) {
                            numBusy = busyMergers.size();
                        }
                        log.debug("# of busy merge workers: " + numBusy);
                    }
                }
            }
        }
    }

    /**
     * @inheritDoc
     */
    public void documentDeleted(Term id) {
        log.debug("document deleted: " + id.text());
        synchronized (busyMergers) {
            for (Worker w : busyMergers) {
                w.documentDeleted(id);
            }
        }
    }

    /**
     * When the calling thread returns this index merger will be idle, that is
     * there will be no merge tasks pending anymore. The method returns
     * immediately if there are currently no tasks pending at all.
     *
     * @throws InterruptedException if this thread is interrupted while waiting
     *                              for the worker threads to become idle.
     */
    void waitUntilIdle() throws InterruptedException {
        synchronized (busyMergers) {
            while (!busyMergers.isEmpty()) {
                busyMergers.wait();
            }
        }
    }

    /**
     * Signals this <code>IndexMerger</code> to stop and waits until it
     * has terminated.
     */
    void dispose() {
        log.debug("dispose IndexMerger");
        // get exclusive lock on index replacements
        try {
            indexReplacement.writeLock().lockInterruptibly();
        } catch (InterruptedException e) {
            log.warn("Interrupted while acquiring index replacement exclusive lock: " + e);
            // try to stop IndexMerger without the sync
        }

        // set quit
        quit.set(true);
        log.debug("quit flag set");

        try {
            // give the merger threads some time to quit,
            // it is possible that the mergers are busy working on a large index.
            // if that is the case we will just ignore it and the daemon will
            // die without being able to finish the merge.
            // at this point it is not possible anymore to replace indexes
            // on the MultiIndex because we hold all indexReplacement permits.
            Worker[] workers;
            synchronized (busyMergers) {
                workers = busyMergers.toArray(new Worker[busyMergers.size()]);
            }
            for (Worker w : workers) {
                w.join(500);
                if (w.isAlive()) {
                    log.info("Unable to stop IndexMerger.Worker. Daemon is busy.");
                } else {
                    log.debug("IndexMerger.Worker thread stopped");
                }
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted while waiting for IndexMerger threads to terminate.");
        }
    }

    //-----------------------< merge properties >-------------------------------

    /**
     * The merge factor.
     *
     * @param mergeFactor the merge factor.
     */
    public void setMergeFactor(int mergeFactor) {
        this.mergeFactor = mergeFactor;
    }


    /**
     * The initial threshold for number of documents to merge to a new index.
     *
     * @param minMergeDocs the min merge docs number.
     */
    public void setMinMergeDocs(int minMergeDocs) {
        this.minMergeDocs = minMergeDocs;
    }

    /**
     * The maximum number of document to merge.
     *
     * @param maxMergeDocs the max merge docs number.
     */
    public void setMaxMergeDocs(int maxMergeDocs) {
        this.maxMergeDocs = maxMergeDocs;
    }

    //------------------------------< internal >--------------------------------

    private void addMergeTask(Merge task) {
        // only enqueue if still running
        if (!quit.get()) {
            Worker worker = new Worker(task);
            if (isStarted.get()) {
                // immediately unblock if this index merger is already started
                worker.unblock();
            }
            synchronized (busyMergers) {
                busyMergers.add(worker);
            }
            executor.execute(worker);
        }
    }

    /**
     * Implements a simple struct that holds the name of an index and how
     * many document it contains. <code>Index</code> is comparable using the
     * number of documents it contains.
     */
    private static final class Index implements Comparable<Index> {

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
        public int compareTo(Index other) {
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
    private static final class IndexBucket extends ArrayList<Index> {

        private static final long serialVersionUID = 2985514550083374904L;

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
        
        @Override
        public String toString() {
            return "Lower: " + lower + ", Upper: " + upper + ", AllowMerge: " + allowMerge + ", Size: " + size();
        }
    }

    private class Worker implements Runnable, IndexListener {

        /**
         * List of id <code>Term</code> that identify documents that were deleted
         * while a merge was running.
         */
        private final List<Term> deletedDocuments = Collections.synchronizedList(new ArrayList<Term>());

        /**
         * A latch that is set to zero when this worker is unblocked.
         */
        private final CountDownLatch start = new CountDownLatch(1);

        /**
         * Flag that indicates whether this worker has finished its work.
         */
        private final AtomicBoolean terminated = new AtomicBoolean(false);

        /**
         * The merge task.
         */
        private final Merge task;

        /**
         * Creates a new worker which is initially blocked. Call
         * {@link #unblock()} to unblock it.
         *
         * @param task the merge task.
         */
        private Worker(Merge task) {
            this.task = task;
        }

        /**
         * Implements the index merging.
         */
        public void run() {
            // worker is initially suspended
            try {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    // check if we should quit
                    if (!quit.get()) {
                        // enqueue task again and retry with another thread
                        addMergeTask(task);
                    }
                    return;
                }

                log.debug("accepted merge request");

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
                        IndexReader[] readers = multiIndex.getIndexReaders(names, IndexMerger.this);
                        try {
                            // do the merge
                            long time = System.currentTimeMillis();
                            index.addIndexes(readers);
                            time = System.currentTimeMillis() - time;
                            int docCount = 0;
                            for (IndexReader reader : readers) {
                                docCount += reader.numDocs();
                            }
                            log.info("merged " + docCount + " documents in " + time + " ms into " + index.getName() + ".");
                        } finally {
                            for (IndexReader reader : readers) {
                                try {
                                    Util.closeOrRelease(reader);
                                } catch (IOException e) {
                                    log.warn("Unable to close IndexReader: " + e);
                                }
                            }
                        }

                        // inform multi index
                        // if we cannot get the sync immediately we have to quit
                        Lock shared = indexReplacement.readLock();
                        if (!shared.tryLock()) {
                            log.debug("index merging canceled");
                            return;
                        }
                        try {
                            log.debug("replace indexes");
                            multiIndex.replaceIndexes(names, index, deletedDocuments);
                        } finally {
                            shared.unlock();
                        }

                        success = true;

                    } finally {
                        if (!success) {
                            // delete index
                            log.debug("deleting index " + index.getName());
                            multiIndex.deleteIndex(index);
                            // add task again and retry
                            addMergeTask(task);
                        }
                    }
                } catch (Throwable e) {
                    log.error("Error while merging indexes: ", e);
                }
            } finally {
                synchronized (terminated) {
                    terminated.set(true);
                    terminated.notifyAll();
                }
                synchronized (busyMergers) {
                    busyMergers.remove(this);
                    busyMergers.notifyAll();
                }
                log.debug("Worker finished");
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
         * Unblocks this worker and allows it to start with the index merging.
         */
        void unblock() {
            start.countDown();
        }

        /**
         * Waits until this worker is finished or the specified amount of time
         * has elapsed.
         *
         * @param timeout the timeout in milliseconds.
         * @throws InterruptedException if the current thread is interrupted
         *                              while waiting for this worker to
         *                              terminate.
         */
        void join(long timeout) throws InterruptedException {
            synchronized (terminated) {
                while (!terminated.get()) {
                    terminated.wait(timeout);
                }
            }
        }

        /**
         * @return <code>true</code> if this worker is still alive and not yet
         *         terminated.
         */
        boolean isAlive() {
            return !terminated.get();
        }
    }
}
