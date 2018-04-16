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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.query.lucene.directory.DirectoryManager;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A <code>MultiIndex</code> consists of a {@link VolatileIndex} and multiple
 * {@link PersistentIndex}es. The goal is to keep most parts of the index open
 * with index readers and write new index data to the volatile index. When
 * the volatile index reaches a certain size (see {@link SearchIndex#setMinMergeDocs(int)})
 * a new persistent index is created with the index data from the volatile index,
 * the same happens when the volatile index has been idle for some time (see
 * {@link SearchIndex#setVolatileIdleTime(int)}).
 * The new persistent index is then added to the list of already existing
 * persistent indexes. Further operations on the new persistent index will
 * however only require an <code>IndexReader</code> which serves for queries
 * but also for delete operations on the index.
 * <p>
 * The persistent indexes are merged from time to time. The merge behaviour
 * is configurable using the methods: {@link SearchIndex#setMaxMergeDocs(int)},
 * {@link SearchIndex#setMergeFactor(int)} and {@link SearchIndex#setMinMergeDocs(int)}.
 * For detailed description of the configuration parameters see also the lucene
 * <code>IndexWriter</code> class.
 * <p>
 * This class is thread-safe.
 * <p>
 * Note on implementation: Multiple modifying threads are synchronized on a
 * <code>MultiIndex</code> instance itself. Synchronization between a modifying
 * thread and reader threads is done using {@link #updateMonitor} and
 * {@link #updateInProgress}.
 */
public class MultiIndex {

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(MultiIndex.class);

    /**
     * A path factory.
     */
    private static final PathFactory PATH_FACTORY = PathFactoryImpl.getInstance();

    /**
     * Names of active persistent index directories.
     */
    private final IndexInfos indexNames;

    /**
     * The history of the multi index.
     */
    private final IndexHistory indexHistory;

    /**
     * Names of index directories that can be deleted.
     * Key = index name (String), Value = time when last in use (Long)
     */
    private final Map<String, Long> deletable = new HashMap<String, Long>();

    /**
     * List of open persistent indexes. This list may also contain an open
     * PersistentIndex owned by the IndexMerger daemon. Such an index is not
     * registered with indexNames and <b>must not</b> be used in regular index
     * operations (delete node, etc.)!
     */
    private final List<PersistentIndex> indexes =
        new ArrayList<PersistentIndex>();

    /**
     * The internal namespace mappings of the query manager.
     */
    private final NamespaceMappings nsMappings;

    /**
     * The directory manager.
     */
    private final DirectoryManager directoryManager;

    /**
     * The redo log factory
     */
    private final RedoLogFactory redoLogFactory;

    /**
     * The base directory to store the index.
     */
    private final Directory indexDir;

    /**
     * The query handler
     */
    private final SearchIndex handler;

    /**
     * The volatile index.
     */
    private VolatileIndex volatileIndex;

    /**
     * Flag indicating whether an update operation is in progress.
     */
    private boolean updateInProgress = false;

    /**
     * If not <code>null</code> points to a valid <code>IndexReader</code> that
     * reads from all indexes, including volatile and persistent indexes.
     */
    private CachingMultiIndexReader multiReader;

    /**
     * Shared document number cache across all persistent indexes.
     */
    private final DocNumberCache cache;

    /**
     * Monitor to use to synchronize access to {@link #multiReader} and
     * {@link #updateInProgress}.
     */
    private final Object updateMonitor = new Object();

    /**
     * <code>true</code> if the redo log contained entries on startup.
     */
    private boolean redoLogApplied = false;

    /**
     * The time this index was last flushed or a transaction was committed.
     */
    private long lastFlushTime = 0;

    /**
     * The <code>IndexMerger</code> for this <code>MultiIndex</code>.
     */
    private final IndexMerger merger;

    /**
     * Task that is periodically called by the repository timer for checking
     * if index should be flushed.
     */
    private ScheduledFuture<?> flushTask = null;

    /**
     * The RedoLog of this <code>MultiIndex</code>.
     */
    private RedoLog redoLog;

    /**
     * The indexing queue with pending text extraction jobs.
     */
    private IndexingQueue indexingQueue;

    /**
     * Only used for testing purpose. Set to <code>true</code> after finished
     * extraction jobs have been removed from the queue and set to
     * <code>false</code> again after the affected nodes have been updated in
     * the index.
     */
    private boolean indexingQueueCommitPending;

    /**
     * Identifiers of nodes that should not be indexed.
     */
    private final Set<NodeId> excludedIDs;

    /**
     * The next transaction id.
     */
    private long nextTransactionId = 0;

    /**
     * The current transaction id.
     */
    private long currentTransactionId = -1;

    /**
     * Flag indicating whether re-indexing is running.
     */
    private boolean reindexing = false;

    /**
     * The index format version of this multi index.
     */
    private final IndexFormatVersion version;

    /**
     * Creates a new MultiIndex.
     *
     * @param handler the search handler
     * @param excludedIDs identifiers of nodes that should
     *                    neither be indexed nor further traversed
     * @throws IOException if an error occurs
     */
    MultiIndex(SearchIndex handler, Set<NodeId> excludedIDs) throws IOException {
        this.directoryManager = handler.getDirectoryManager();
        this.redoLogFactory = handler.getRedoLogFactory();
        this.indexDir = directoryManager.getDirectory(".");
        this.handler = handler;
        this.cache = new DocNumberCache(handler.getCacheSize());
        this.excludedIDs = new HashSet<NodeId>(excludedIDs);
        this.nsMappings = handler.getNamespaceMappings();

        indexNames = new IndexInfos(indexDir, "indexes");

        this.indexHistory = new IndexHistory(indexDir,
                handler.getMaxHistoryAge() * 1000);

        // as of 1.5 deletable file is not used anymore
        removeDeletable();

        this.redoLog = redoLogFactory.createRedoLog(this);

        // initialize IndexMerger
        merger = new IndexMerger(this, handler.getContext().getExecutor());
        merger.setMaxMergeDocs(handler.getMaxMergeDocs());
        merger.setMergeFactor(handler.getMergeFactor());
        merger.setMinMergeDocs(handler.getMinMergeDocs());

        // initialize indexing queue
        this.indexingQueue = new IndexingQueue(new IndexingQueueStore(indexDir));

        // open persistent indexes
        Iterator<IndexInfo> iterator = indexNames.iterator();
        while (iterator.hasNext()) {
            IndexInfo info = iterator.next();
            String name = info.getName();
            // only open if it still exists
            // it is possible that indexNames still contains a name for
            // an index that has been deleted, but indexNames has not been
            // written to disk.
            if (!directoryManager.hasDirectory(name)) {
                log.debug("index does not exist anymore: " + name);
                // move on to next index
                continue;
            }
            PersistentIndex index = new PersistentIndex(name,
                    handler.getTextAnalyzer(), handler.getSimilarity(),
                    cache, indexingQueue, directoryManager,
                    handler.getMaxHistoryAge());
            index.setUseCompoundFile(handler.getUseCompoundFile());
            index.setTermInfosIndexDivisor(handler.getTermInfosIndexDivisor());
            indexes.add(index);
            merger.indexAdded(index.getName(), index.getNumDocuments());
        }

        // init volatile index
        resetVolatileIndex();

        // set index format version and at the same time
        // initialize hierarchy cache if requested.
        CachingMultiIndexReader reader = getIndexReader(handler.isInitializeHierarchyCache());
        try {
            version = IndexFormatVersion.getVersion(reader);
        } finally {
            reader.release();
        }

        indexingQueue.initialize(this);

        redoLogApplied = redoLog.hasEntries();

        // run recovery
        Recovery.run(this, redoLog);

        // enqueue unused segments for deletion
        enqueueUnusedSegments();
        attemptDelete();

        // now that we are ready, start index merger
        merger.start();

        if (redoLogApplied) {
            // wait for the index merge to finish pending jobs
            try {
                merger.waitUntilIdle();
            } catch (InterruptedException e) {
                // move on
            }
            flush();
        }

        if (indexNames.size() > 0) {
            scheduleFlushTask();
        }
    }

    /**
     * Returns the number of documents in this index.
     *
     * @return the number of documents in this index.
     * @throws IOException if an error occurs while reading from the index.
     */
    int numDocs() throws IOException {
        if (indexNames.size() == 0) {
            return volatileIndex.getNumDocuments();
        } else {
            CachingMultiIndexReader reader = getIndexReader();
            try {
                return reader.numDocs();
            } finally {
                reader.release();
            }
        }
    }

    /**
     * @return the index format version for this multi index.
     */
    IndexFormatVersion getIndexFormatVersion() {
        return version;
    }

    /**
     * Creates an initial index by traversing the node hierarchy starting at the
     * node with <code>rootId</code>.
     *
     * @param stateMgr the item state manager.
     * @param rootId   the id of the node from where to start.
     * @param rootPath the path of the node from where to start.
     * @throws IOException           if an error occurs while indexing the
     *                               workspace.
     * @throws IllegalStateException if this index is not empty.
     */
    void createInitialIndex(ItemStateManager stateMgr,
                            NodeId rootId,
                            Path rootPath)
            throws IOException {
        // only do an initial index if there are no indexes at all
        if (indexNames.size() == 0) {
            reindexing = true;
            try {
                long count = 0;
                // traverse and index workspace
                executeAndLog(new Start(Action.INTERNAL_TRANSACTION));
                NodeState rootState = (NodeState) stateMgr.getItemState(rootId);
                count = createIndex(rootState, rootPath, stateMgr, count);
                checkIndexingQueue(true);
                executeAndLog(new Commit(getTransactionId()));
                log.debug("Created initial index for {} nodes", count);
                releaseMultiReader();
                safeFlush();
            } catch (Exception e) {
                String msg = "Error indexing workspace";
                IOException ex = new IOException(msg);
                ex.initCause(e);
                throw ex;
            } finally {
                reindexing = false;
                scheduleFlushTask();
            }
        } else {
            throw new IllegalStateException("Index already present");
        }
    }

    /**
     * Atomically updates the index by removing some documents and adding
     * others.
     *
     * @param remove collection of <code>id</code>s that identify documents to
     *               remove
     * @param add    collection of <code>Document</code>s to add. Some of the
     *               elements in this collection may be <code>null</code>, to
     *               indicate that a node could not be indexed successfully.
     * @throws IOException if an error occurs while updating the index.
     */
    synchronized void update(
            Collection<NodeId> remove, Collection<Document> add)
            throws IOException {
        // make sure a reader is available during long updates
        if (add.size() > handler.getBufferSize()) {
            try {
                getIndexReader().release();
            } catch (IOException e) {
                // do not fail if an exception is thrown here
                log.warn("unable to prepare index reader for queries during update", e);
            }
        }

        synchronized (updateMonitor) {
            updateInProgress = true;
        }
        try {
            long transactionId = nextTransactionId++;
            executeAndLog(new Start(transactionId));


            long time = System.currentTimeMillis();
            for (NodeId id : remove) {
                executeAndLog(new DeleteNode(transactionId, id));
            }
            time = System.currentTimeMillis() - time;
            log.debug("{} documents deleted in {}ms", remove.size(), time);

            time = System.currentTimeMillis();
            for (Document document : add) {
                if (document != null) {
                    executeAndLog(new AddNode(transactionId, document));
                    // commit volatile index if needed
                    checkVolatileCommit();
                }
            }
            time = System.currentTimeMillis() - time;
            log.debug("{} documents added in {}ms", add.size(), time);
            executeAndLog(new Commit(transactionId));
        } finally {
            synchronized (updateMonitor) {
                updateInProgress = false;
                updateMonitor.notifyAll();
                releaseMultiReader();
            }
        }
    }

    /**
     * Adds a document to the index.
     *
     * @param doc the document to add.
     * @throws IOException if an error occurs while adding the document to the
     *                     index.
     */
    void addDocument(Document doc) throws IOException {
        Collection<NodeId> empty = Collections.emptyList();
        update(empty, Collections.singleton(doc));
    }

    /**
     * Deletes the first document that matches the <code>id</code>.
     *
     * @param id document that match this <code>id</code> will be deleted.
     * @throws IOException if an error occurs while deleting the document.
     */
    void removeDocument(NodeId id) throws IOException {
        Collection<Document> empty = Collections.emptyList();
        update(Collections.singleton(id), empty);
    }

    /**
     * Deletes all documents that match the <code>id</code>.
     *
     * @param id documents that match this <code>id</code> will be deleted.
     * @return the number of deleted documents.
     * @throws IOException if an error occurs while deleting documents.
     */
    synchronized int removeAllDocuments(NodeId id) throws IOException {
        synchronized (updateMonitor) {
            updateInProgress = true;
        }
        int num;
        try {
            Term idTerm = TermFactory.createUUIDTerm(id.toString());
            executeAndLog(new Start(Action.INTERNAL_TRANSACTION));
            num = volatileIndex.removeDocument(idTerm);
            if (num > 0) {
                redoLog.append(new DeleteNode(getTransactionId(), id));
            }
            for (PersistentIndex index : indexes) {
                // only remove documents from registered indexes
                if (indexNames.contains(index.getName())) {
                    int removed = index.removeDocument(idTerm);
                    if (removed > 0) {
                        redoLog.append(new DeleteNode(getTransactionId(), id));
                    }
                    num += removed;
                }
            }
            executeAndLog(new Commit(getTransactionId()));
        } finally {
            synchronized (updateMonitor) {
                updateInProgress = false;
                updateMonitor.notifyAll();
                releaseMultiReader();
            }
        }
        return num;
    }

    /**
     * Returns <code>IndexReader</code>s for the indexes named
     * <code>indexNames</code>. An <code>IndexListener</code> is registered and
     * notified when documents are deleted from one of the indexes in
     * <code>indexNames</code>.
     * <p>
     * Note: the number of <code>IndexReaders</code> returned by this method is
     * not necessarily the same as the number of index names passed. An index
     * might have been deleted and is not reachable anymore.
     *
     * @param indexNames the names of the indexes for which to obtain readers.
     * @param listener   the listener to notify when documents are deleted.
     * @return the <code>IndexReaders</code>.
     * @throws IOException if an error occurs acquiring the index readers.
     */
    synchronized IndexReader[] getIndexReaders(
            String[] indexNames, IndexListener listener) throws IOException {
        Set<String> names = new HashSet<String>(Arrays.asList(indexNames));
        Map<ReadOnlyIndexReader, PersistentIndex> indexReaders =
            new HashMap<ReadOnlyIndexReader, PersistentIndex>();

        try {
            for (PersistentIndex index : indexes) {
                if (names.contains(index.getName())) {
                    indexReaders.put(index.getReadOnlyIndexReader(listener), index);
                }
            }
        } catch (IOException e) {
            // release readers obtained so far
            for (Map.Entry<ReadOnlyIndexReader, PersistentIndex> entry
                    : indexReaders.entrySet()) {
                try {
                    entry.getKey().release();
                } catch (IOException ex) {
                    log.warn("Exception releasing index reader", ex);
                }
                entry.getValue().resetListener();
            }
            throw e;
        }

        return indexReaders.keySet().toArray(new IndexReader[indexReaders.size()]);
    }

    /**
     * Creates a new Persistent index. The new index is not registered with this
     * <code>MultiIndex</code>.
     *
     * @param indexName the name of the index to open, or <code>null</code> if
     *                  an index with a new name should be created.
     * @return a new <code>PersistentIndex</code>.
     * @throws IOException if a new index cannot be created.
     */
    synchronized PersistentIndex getOrCreateIndex(String indexName)
            throws IOException {
        // check existing
        for (PersistentIndex idx : indexes) {
            if (idx.getName().equals(indexName)) {
                return idx;
            }
        }

        // otherwise open / create it
        if (indexName == null) {
            do {
                indexName = indexNames.newName();
            } while (directoryManager.hasDirectory(indexName));
        }
        PersistentIndex index;
        try {
            index = new PersistentIndex(indexName,
                    handler.getTextAnalyzer(), handler.getSimilarity(),
                    cache, indexingQueue, directoryManager,
                    handler.getMaxHistoryAge());
        } catch (IOException e) {
            // do some clean up
            if (!directoryManager.delete(indexName)) {
                deletable.put(indexName, Long.MIN_VALUE);
            }
            throw e;
        }
        index.setUseCompoundFile(handler.getUseCompoundFile());
        index.setTermInfosIndexDivisor(handler.getTermInfosIndexDivisor());

        // add to list of open indexes and return it
        indexes.add(index);
        return index;
    }

    /**
     * Returns <code>true</code> if this multi index has an index segment with
     * the given name. This method even returns <code>true</code> if an index
     * segments has not yet been loaded / initialized but exists on disk.
     *
     * @param indexName the name of the index segment.
     * @return <code>true</code> if it exists; otherwise <code>false</code>.
     * @throws IOException if an error occurs while checking existence of
     *          directory.
     */
    synchronized boolean hasIndex(String indexName) throws IOException {
        // check existing
        for (PersistentIndex idx : indexes) {
            if (idx.getName().equals(indexName)) {
                return true;
            }
        }
        // check if it exists on disk
        return directoryManager.hasDirectory(indexName);
    }

    /**
     * Replaces the indexes with names <code>obsoleteIndexes</code> with
     * <code>index</code>. Documents that must be deleted in <code>index</code>
     * can be identified with <code>Term</code>s in <code>deleted</code>.
     *
     * @param obsoleteIndexes the names of the indexes to replace.
     * @param index      the new index that is the result of a merge of the
     *                   indexes to replace.
     * @param deleted    <code>Term</code>s that identify documents that must be
     *                   deleted in <code>index</code>.
     * @throws IOException if an exception occurs while replacing the indexes.
     */
    void replaceIndexes(String[] obsoleteIndexes,
                        PersistentIndex index,
                        Collection<Term> deleted)
            throws IOException {

        if (handler.isInitializeHierarchyCache()) {
            // force initializing of caches
            long time = System.currentTimeMillis();
            index.getReadOnlyIndexReader(true).release();
            time = System.currentTimeMillis() - time;
            log.debug("hierarchy cache initialized in {} ms", time);
        }

        synchronized (this) {
            synchronized (updateMonitor) {
                updateInProgress = true;
            }
            try {
                // if we are reindexing there is already an active transaction
                if (!reindexing) {
                    executeAndLog(new Start(Action.INTERNAL_TRANS_REPL_INDEXES));
                }
                // delete obsolete indexes
                Set<String> names = new HashSet<String>(Arrays.asList(obsoleteIndexes));
                for (String indexName : names) {
                    // do not try to delete indexes that are already gone
                    if (indexNames.contains(indexName)) {
                        executeAndLog(new DeleteIndex(getTransactionId(), indexName));
                    }
                }

                // Index merger does not log an action when it creates the target
                // index of the merge. We have to do this here.
                executeAndLog(new CreateIndex(getTransactionId(), index.getName()));

                executeAndLog(new AddIndex(getTransactionId(), index.getName()));

                // delete documents in index
                for (Term id : deleted) {
                    index.removeDocument(id);
                }
                index.commit();

                if (!reindexing) {
                    // only commit if we are not reindexing
                    // when reindexing the final commit is done at the very end
                    executeAndLog(new Commit(getTransactionId()));
                }
            } finally {
                synchronized (updateMonitor) {
                    updateInProgress = false;
                    updateMonitor.notifyAll();
                    releaseMultiReader();
                }
            }
        }
        if (reindexing) {
            // do some cleanup right away when reindexing
            attemptDelete();
        }
    }

    /**
     * Returns an read-only <code>IndexReader</code> that spans alls indexes of this
     * <code>MultiIndex</code>.
     *
     * @return an <code>IndexReader</code>.
     * @throws IOException if an error occurs constructing the <code>IndexReader</code>.
     */
    public CachingMultiIndexReader getIndexReader() throws IOException {
        return getIndexReader(false);
    }

    /**
     * Returns an read-only <code>IndexReader</code> that spans alls indexes of this
     * <code>MultiIndex</code>.
     *
     * @param initCache when set <code>true</code> the hierarchy cache is
     *                  completely initialized before this call returns.
     * @return an <code>IndexReader</code>.
     * @throws IOException if an error occurs constructing the <code>IndexReader</code>.
     */
    public synchronized CachingMultiIndexReader getIndexReader(boolean initCache) throws IOException {
        synchronized (updateMonitor) {
            if (multiReader != null) {
                multiReader.acquire();
                return multiReader;
            }
            // no reader available
            // wait until no update is in progress
            while (updateInProgress) {
                try {
                    updateMonitor.wait();
                } catch (InterruptedException e) {
                    throw new IOException("Interrupted while waiting to aquire reader");
                }
            }
            // some other read thread might have created the reader in the
            // meantime -> check again
            if (multiReader == null) {
                List<ReadOnlyIndexReader> readerList =
                    new ArrayList<ReadOnlyIndexReader>();
                for (PersistentIndex pIdx : indexes) {
                    if (indexNames.contains(pIdx.getName())) {
                        readerList.add(pIdx.getReadOnlyIndexReader(initCache));
                    }
                }
                readerList.add(volatileIndex.getReadOnlyIndexReader());
                ReadOnlyIndexReader[] readers =
                    readerList.toArray(new ReadOnlyIndexReader[readerList.size()]);
                multiReader = new CachingMultiIndexReader(readers, cache);
            }
            multiReader.acquire();
            return multiReader;
        }
    }

    /**
     * Returns the volatile index.
     *
     * @return the volatile index.
     */
    VolatileIndex getVolatileIndex() {
        return volatileIndex;
    }

    /**
     * Runs a consistency check on this multi index.
     *
     * @return the consistency check.
     * @throws IOException if an error occurs while running the check.
     */
    ConsistencyCheck runConsistencyCheck() throws IOException {
        return ConsistencyCheck.run(this, handler, excludedIDs);
    }

    /**
     * Closes this <code>MultiIndex</code>.
     */
    void close() {

        // stop index merger
        // when calling this method we must not lock this MultiIndex, otherwise
        // a deadlock might occur
        merger.dispose();

        synchronized (this) {
            // stop timer
            unscheduleFlushTask();

            // commit / close indexes
            try {
                releaseMultiReader();
            } catch (IOException e) {
                log.error("Exception while closing search index.", e);
            }
            try {
                flush();
            } catch (IOException e) {
                log.error("Exception while closing search index.", e);
            }
            volatileIndex.close();
            for (PersistentIndex index : indexes) {
                index.close();
            }

            // close indexing queue
            indexingQueue.close();

            // finally close directory
            try {
                indexDir.close();
            } catch (IOException e) {
                log.error("Exception while closing directory.", e);
            }
        }
    }

    /**
     * Returns the namespace mappings of this search index.
     * @return the namespace mappings of this search index.
     */
    NamespaceMappings getNamespaceMappings() {
        return nsMappings;
    }

    /**
     * Returns the indexing queue for this multi index.
     * @return the indexing queue for this multi index.
     */
    IndexingQueue getIndexingQueue() {
        return indexingQueue;
    }

    /**
     * @return the base directory of the index.
     */
    Directory getDirectory() {
        return indexDir;
    }

    /**
     * @return the current generation of the index names.
     */
    long getIndexGeneration() {
        return indexNames.getGeneration();
    }

    /**
     * Returns a lucene Document for the <code>node</code>.
     *
     * @param node the node to index.
     * @return the index document.
     * @throws RepositoryException if an error occurs while reading from the
     *                             workspace.
     */
    Document createDocument(NodeState node) throws RepositoryException {
        return handler.createDocument(node, nsMappings, version);
    }

    /**
     * Returns a lucene Document for the Node with <code>id</code>.
     *
     * @param id the id of the node to index.
     * @return the index document.
     * @throws RepositoryException if an error occurs while reading from the
     *                             workspace or if there is no node with
     *                             <code>id</code>.
     */
    Document createDocument(NodeId id) throws RepositoryException {
        try {
            NodeState state = (NodeState) handler.getContext().getItemStateManager().getItemState(id);
            return createDocument(state);
        } catch (NoSuchItemStateException e) {
            throw new RepositoryException("Node " + id + " does not exist", e);
        } catch (ItemStateException e) {
            throw new RepositoryException("Error retrieving node: " + id, e);
        }
    }

    /**
     * Returns <code>true</code> if the redo log contained entries while
     * this index was instantiated; <code>false</code> otherwise.
     * @return <code>true</code> if the redo log contained entries.
     */
    boolean getRedoLogApplied() {
        return redoLogApplied;
    }

    /**
     * Removes the <code>index</code> from the list of active sub indexes.
     * Depending on the {@link SearchIndex#getMaxHistoryAge()}, the
     * Index is not deleted right away.
     * <p>
     * This method does not close the index, but rather expects that the index
     * has already been closed.
     *
     * @param index the index to delete.
     */
    synchronized void deleteIndex(PersistentIndex index) {
        // remove it from the lists if index is registered
        indexes.remove(index);
        indexNames.removeName(index.getName());
        synchronized (deletable) {
            log.debug("Moved " + index.getName() + " to deletable");
            deletable.put(index.getName(), System.currentTimeMillis());
        }
    }

    /**
     * Flushes this <code>MultiIndex</code>. Persists all pending changes and
     * resets the redo log.
     *
     * @throws IOException if the flush fails.
     */
    private void flush() throws IOException {
        synchronized (this) {

            // only start transaction when there is something to commit
            boolean transactionStarted = false;

            if (volatileIndex.getNumDocuments() > 0) {
                // commit volatile index
                executeAndLog(new Start(Action.INTERNAL_TRANSACTION));
                transactionStarted = true;
                commitVolatileIndex();
            }

            boolean indexesModified = false;
            // commit persistent indexes
            for (int i = indexes.size() - 1; i >= 0; i--) {
                PersistentIndex index = indexes.get(i);
                // only commit indexes we own
                // index merger also places PersistentIndex instances in indexes,
                // but does not make them public by registering the name in indexNames
                if (indexNames.contains(index.getName())) {
                    long gen = index.getCurrentGeneration();
                    index.commit();
                    if (gen != index.getCurrentGeneration()) {
                        indexesModified = true;
                        log.debug("Committed revision {} of index {}",
                                Long.toString(index.getCurrentGeneration(), Character.MAX_RADIX),
                                index.getName());
                    }
                    // check if index still contains documents
                    if (index.getNumDocuments() == 0) {
                        if (!transactionStarted) {
                            executeAndLog(new Start(Action.INTERNAL_TRANSACTION));
                            transactionStarted = true;
                        }
                        executeAndLog(new DeleteIndex(getTransactionId(), index.getName()));
                    }
                }
            }

            if (transactionStarted) {
                executeAndLog(new Commit(getTransactionId()));
            }

            if (transactionStarted || indexesModified || redoLog.hasEntries()) {
                indexNames.write();

                indexHistory.addIndexInfos(indexNames);

                // close redo.log and create a new one based
                // on the new indexNames generation
                redoLog.close();
                redoLog = redoLogFactory.createRedoLog(this);
            }

            lastFlushTime = System.currentTimeMillis();
        }

        indexHistory.pruneOutdated();

        // delete obsolete indexes
        attemptDelete();
    }

    /**
     * Releases the {@link #multiReader} and sets it <code>null</code>. If the
     * reader is already <code>null</code> this method does nothing. When this
     * method returns {@link #multiReader} is guaranteed to be <code>null</code>
     * even if an exception is thrown.
     * <p>
     * Please note that this method does not take care of any synchronization.
     * A caller must ensure that it is the only thread operating on this multi
     * index, or that it holds the {@link #updateMonitor}.
     *
     * @throws IOException if an error occurs while releasing the reader.
     */
    void releaseMultiReader() throws IOException {
        if (multiReader != null) {
            try {
                multiReader.release();
            } finally {
                multiReader = null;
            }
        }
    }

    //-------------------------< testing only >---------------------------------

    void waitUntilIndexingQueueIsEmpty() {
        IndexingQueue iq = getIndexingQueue();
        synchronized (iq) {
            while (iq.getNumPendingDocuments() > 0 || indexingQueueCommitPending) {
                try {
                    log.debug(
                            "waiting for indexing queue to become empty. {} pending docs.",
                            iq.getNumPendingDocuments());
                    iq.wait();
                    log.debug("notified");
                } catch (InterruptedException e) {
                    // interrupted, check again if queue is empty
                }
            }
        }
    }

    void notifyIfIndexingQueueIsEmpty() {
        IndexingQueue iq = getIndexingQueue();
        synchronized (iq) {
            indexingQueueCommitPending = false;
            if (iq.getNumPendingDocuments() == 0) {
                iq.notifyAll();
            }
        }
    }

    //-------------------------< internal >-------------------------------------

    /**
     * Enqueues unused segments for deletion in {@link #deletable}. This method
     * does not synchronize on {@link #deletable}! A caller must ensure that it
     * is the only one acting on the {@link #deletable} map.
     *
     * @throws IOException if an error occurs while reading directories.
     */
    private void enqueueUnusedSegments() throws IOException {
        // walk through index segments
        for (String name : directoryManager.getDirectoryNames()) {
            if (!name.startsWith("_")) {
                continue;
            }
            long lastUse = indexHistory.getLastUseOf(name);
            if (lastUse != Long.MAX_VALUE) {
                if (log.isDebugEnabled()) {
                    String msg = "Segment " + name + " not is use anymore. ";
                    if (lastUse != Long.MIN_VALUE) {
                        Calendar cal = Calendar.getInstance();
                        DateFormat df = DateFormat.getInstance();
                        cal.setTimeInMillis(lastUse);
                        msg += "Unused since: " + df.format(cal.getTime());
                    } else {
                        msg += "(orphaned)";
                    }
                    log.debug(msg);
                }
                deletable.put(name, lastUse);
            }
        }
        // now prune outdated index infos
        indexHistory.pruneOutdated();
    }

    /**
     * Schedules a background task for flushing the index once per second.
     */
    private void scheduleFlushTask() {
        ScheduledExecutorService executor = handler.getContext().getExecutor();
        flushTask = executor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                // check if there are any indexing jobs finished
                checkIndexingQueue(false);
                // check if volatile index should be flushed
                checkFlush();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Cancels the scheduled background index flush task.
     */
    private void unscheduleFlushTask() {
        if (flushTask != null) {
            flushTask.cancel(false);
            flushTask = null;
        }
    }

    /**
     * Resets the volatile index to a new instance.
     *
     * @throws IOException if the volatile index cannot be reset.
     */
    private void resetVolatileIndex() throws IOException {
        // JCR-3227 close VolatileIndex properly
        if (volatileIndex != null) {
            volatileIndex.close();
        }
        volatileIndex = new VolatileIndex(handler.getTextAnalyzer(),
                handler.getSimilarity(), indexingQueue);
        volatileIndex.setUseCompoundFile(handler.getUseCompoundFile());
        volatileIndex.setBufferSize(handler.getBufferSize());
    }

    /**
     * Returns the current transaction id.
     *
     * @return the current transaction id.
     */
    private long getTransactionId() {
        return currentTransactionId;
    }

    /**
     * Executes action <code>a</code> and appends the action to the redo log if
     * successful.
     *
     * @param a the <code>Action</code> to execute.
     * @return the executed action.
     * @throws IOException         if an error occurs while executing the action
     *                             or appending the action to the redo log.
     */
    private Action executeAndLog(Action a)
            throws IOException {
        a.execute(this);
        redoLog.append(a);
        // please note that flushing the redo log is only required on
        // commit, but we also want to keep track of new indexes for sure.
        // otherwise it might happen that unused index folders are orphaned
        // after a crash.
        if (a.getType() == Action.TYPE_COMMIT || a.getType() == Action.TYPE_ADD_INDEX) {
            redoLog.flush();
        }
        return a;
    }

    /**
     * Checks if it is needed to commit the volatile index according to {@link
     * SearchIndex#getMaxVolatileIndexSize()}.
     *
     * @return <code>true</code> if the volatile index has been committed,
     *         <code>false</code> otherwise.
     * @throws IOException if an error occurs while committing the volatile
     *                     index.
     */
    private boolean checkVolatileCommit() throws IOException {
        if (volatileIndex.getRamSizeInBytes() >= handler.getMaxVolatileIndexSize()) {
            commitVolatileIndex();
            return true;
        }
        return false;
    }

    /**
     * Commits the volatile index to a persistent index. The new persistent
     * index is added to the list of indexes but not written to disk. When this
     * method returns a new volatile index has been created.
     *
     * @throws IOException if an error occurs while writing the volatile index
     *                     to disk.
     */
    private void commitVolatileIndex() throws IOException {

        // check if volatile index contains documents at all
        int volatileIndexDocuments = volatileIndex.getNumDocuments();
        if (volatileIndexDocuments > 0) {

            long time = System.currentTimeMillis();
            // create index
            CreateIndex create = new CreateIndex(getTransactionId(), null);
            executeAndLog(create);

            // commit volatile index
            executeAndLog(new VolatileCommit(getTransactionId(), create.getIndexName()));

            // add new index
            AddIndex add = new AddIndex(getTransactionId(), create.getIndexName());
            executeAndLog(add);

            // create new volatile index
            resetVolatileIndex();

            time = System.currentTimeMillis() - time;
            log.debug("Committed in-memory index containing {} documents in {}ms.", volatileIndexDocuments, time);
        }
    }

    /**
     * Recursively creates an index starting with the NodeState
     * <code>node</code>.
     *
     * @param node     the current NodeState.
     * @param path     the path of the current <code>node</code> state.
     * @param stateMgr the shared item state manager.
     * @param count    the number of nodes already indexed.
     * @return the number of nodes indexed so far.
     * @throws IOException         if an error occurs while writing to the
     *                             index.
     * @throws ItemStateException  if an node state cannot be found.
     * @throws RepositoryException if any other error occurs
     */
    private long createIndex(NodeState node,
                             Path path,
                             ItemStateManager stateMgr,
                             long count)
            throws IOException, ItemStateException, RepositoryException {
        NodeId id = node.getNodeId();
        if (excludedIDs.contains(id)) {
            return count;
        }
        executeAndLog(new AddNode(getTransactionId(), id));
        if (++count % 100 == 0) {
            PathResolver resolver = new DefaultNamePathResolver(
                    handler.getContext().getNamespaceRegistry());
            log.info("indexing... {} ({})", resolver.getJCRPath(path), count);
        }
        if (count % 10 == 0) {
            checkIndexingQueue(true);
        }
        checkVolatileCommit();
        for (ChildNodeEntry child : node.getChildNodeEntries()) {
            Path childPath = PATH_FACTORY.create(path, child.getName(),
                    child.getIndex(), false);
            NodeState childState = null;
            try {
                childState = (NodeState) stateMgr.getItemState(child.getId());
            } catch (NoSuchItemStateException e) {
                handler.getOnWorkspaceInconsistencyHandler().handleMissingChildNode(
                        e, handler, path, node, child);
            } catch (ItemStateException e) {
                // JCR-3268 log bundle corruption and continue
                handler.getOnWorkspaceInconsistencyHandler().logError(e,
                        handler, childPath, node, child);
            }
            if (childState != null) {
                count = createIndex(childState, childPath, stateMgr, count);
            }
        }
        return count;
    }

    /**
     * Attempts to delete all files that are older than
     *{@link SearchIndex#getMaxHistoryAge()}.
     */
    private void attemptDelete() {
        synchronized (deletable) {
            for (Iterator<Map.Entry<String, Long>> it = deletable.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Long> entry = it.next();
                String indexName = entry.getKey();
                long lastUse = entry.getValue();
                if (System.currentTimeMillis() - handler.getMaxHistoryAge() * 1000 > lastUse) {
                    if (directoryManager.delete(indexName)) {
                        it.remove();
                    } else {
                        // JCR-2705: We will retry later, so only a debug log
                        log.debug("Unable to delete obsolete index: {}", indexName);
                    }
                }
            }
        }
    }

    /**
     * Removes the deletable file if it exists. The file is not used anymore
     * in Jackrabbit versions >= 1.5.
     */
    private void removeDeletable() {
        String fileName = "deletable";
        try {
            if (indexDir.fileExists(fileName)) {
                indexDir.deleteFile(fileName);
            }
        } catch (IOException e) {
            log.warn("Unable to remove file 'deletable'.", e);
        }
    }

    /**
     * Checks the duration between the last commit to this index and the
     * current time and flushes the index (if there are changes at all)
     * if the duration (idle time) is more than {@link SearchIndex#getVolatileIdleTime()}
     * seconds.
     */
    private synchronized void checkFlush() {
        long idleTime = System.currentTimeMillis() - lastFlushTime;
        // do not flush if volatileIdleTime is zero or negative
        if (handler.getVolatileIdleTime() > 0
                && idleTime > handler.getVolatileIdleTime() * 1000) {
            try {
                if (redoLog.hasEntries()) {
                    long time = System.currentTimeMillis();
                    log.debug("Flushing index after being idle for "
                            + idleTime + " ms.");
                    safeFlush();
                    time = System.currentTimeMillis() - time;
                    log.debug("Index flushed in " + time + " ms.");
                }
            } catch (IOException e) {
                log.error("Unable to commit volatile index", e);
            }
        }
    }

    void safeFlush() throws IOException{
        synchronized (updateMonitor) {
            updateInProgress = true;
        }
        try {
            flush();
        } finally {
            synchronized (updateMonitor) {
                updateInProgress = false;
                updateMonitor.notifyAll();
                releaseMultiReader();
            }
        }
    }

    /**
     * Checks the indexing queue for finished text extrator jobs and updates the
     * index accordingly if there are any new ones.
     *
     * @param transactionPresent whether a transaction is in progress and the
     *                           current {@link #getTransactionId()} should be
     *                           used. If <code>false</code> a new transaction
     *                           is created when documents are transfered from
     *                           the indexing queue to the index.
     */
    private void checkIndexingQueue(boolean transactionPresent) {
        Map<NodeId, Document> finished = new HashMap<NodeId, Document>();
        for (Document document : indexingQueue.getFinishedDocuments()) {
            NodeId id = new NodeId(document.get(FieldNames.UUID));
            finished.put(id, document);
        }

        // now update index with the remaining ones if there are any
        if (!finished.isEmpty()) {
            log.debug("updating index with {} nodes from indexing queue.",
                    finished.size());

            // Only useful for testing
            synchronized (getIndexingQueue()) {
                indexingQueueCommitPending = true;
            }

            try {
                // remove documents from the queue
                for (NodeId id : finished.keySet()) {
                    indexingQueue.removeDocument(id.toString());
                }

                try {
                    if (transactionPresent) {
                        synchronized (this) {
                            for (NodeId id : finished.keySet()) {
                                executeAndLog(new DeleteNode(getTransactionId(), id));
                            }
                            for (Document document : finished.values()) {
                                executeAndLog(new AddNode(getTransactionId(), document));
                            }
                        }
                    } else {
                        update(finished.keySet(), finished.values());
                    }
                } catch (IOException e) {
                    // update failed
                    log.warn("Failed to update index with deferred text extraction", e);
                }
            } finally {
                // the following method also resets
                // indexingQueueCommitPending back to false
                notifyIfIndexingQueueIsEmpty();
            }
        }
    }

    //------------------------< Actions >---------------------------------------

    /**
     * Defines an action on an <code>MultiIndex</code>.
     */
    public abstract static class Action {

        /**
         * Action identifier in redo log for transaction start action.
         */
        static final String START = "STR";

        /**
         * Action type for start action.
         */
        public static final int TYPE_START = 0;

        /**
         * Action identifier in redo log for add node action.
         */
        static final String ADD_NODE = "ADD";

        /**
         * Action type for add node action.
         */
        public static final int TYPE_ADD_NODE = 1;

        /**
         * Action identifier in redo log for node delete action.
         */
        static final String DELETE_NODE = "DEL";

        /**
         * Action type for delete node action.
         */
        public static final int TYPE_DELETE_NODE = 2;

        /**
         * Action identifier in redo log for transaction commit action.
         */
        static final String COMMIT = "COM";

        /**
         * Action type for commit action.
         */
        public static final int TYPE_COMMIT = 3;

        /**
         * Action identifier in redo log for volatile index commit action.
         */
        static final String VOLATILE_COMMIT = "VOL_COM";

        /**
         * Action type for volatile index commit action.
         */
        public static final int TYPE_VOLATILE_COMMIT = 4;

        /**
         * Action identifier in redo log for index create action.
         */
        static final String CREATE_INDEX = "CRE_IDX";

        /**
         * Action type for create index action.
         */
        public static final int TYPE_CREATE_INDEX = 5;

        /**
         * Action identifier in redo log for index add action.
         */
        static final String ADD_INDEX = "ADD_IDX";

        /**
         * Action type for add index action.
         */
        public static final int TYPE_ADD_INDEX = 6;

        /**
         * Action identifier in redo log for delete index action.
         */
        static final String DELETE_INDEX = "DEL_IDX";

        /**
         * Action type for delete index action.
         */
        public static final int TYPE_DELETE_INDEX = 7;

        /**
         * Transaction identifier for internal actions like volatile index
         * commit triggered by timer thread.
         */
        static final long INTERNAL_TRANSACTION = -1;

        /**
         * Transaction identifier for internal action that replaces indexs.
         */
        static final long INTERNAL_TRANS_REPL_INDEXES = -2;

        /**
         * The id of the transaction that executed this action.
         */
        private final long transactionId;

        /**
         * The action type.
         */
        private final int type;

        /**
         * Creates a new <code>Action</code>.
         *
         * @param transactionId the id of the transaction that executed this
         *                      action.
         * @param type          the action type.
         */
        Action(long transactionId, int type) {
            this.transactionId = transactionId;
            this.type = type;
        }

        /**
         * Returns the transaction id for this <code>Action</code>.
         *
         * @return the transaction id for this <code>Action</code>.
         */
        long getTransactionId() {
            return transactionId;
        }

        /**
         * Returns the action type.
         *
         * @return the action type.
         */
        int getType() {
            return type;
        }

        /**
         * Executes this action on the <code>index</code>.
         *
         * @param index the index where to execute the action.
         * @throws IOException         if the action fails due to some I/O error in
         *                             the index or some other error.
         */
        public abstract void execute(MultiIndex index) throws IOException;

        /**
         * Executes the inverse operation of this action. That is, does an undo
         * of this action. This default implementation does nothing, but returns
         * silently.
         *
         * @param index the index where to undo the action.
         * @throws IOException if the action cannot be undone.
         */
        public void undo(MultiIndex index) throws IOException {
        }

        /**
         * Returns a <code>String</code> representation of this action that can be
         * written to the {@link RedoLog}.
         *
         * @return a <code>String</code> representation of this action.
         */
        public abstract String toString();

        /**
         * Parses an line in the redo log and created an {@link Action}.
         *
         * @param line the line from the redo log.
         * @return an <code>Action</code>.
         * @throws IllegalArgumentException if the line is malformed.
         */
        static Action fromString(String line) throws IllegalArgumentException {
            int endTransIdx = line.indexOf(' ');
            if (endTransIdx == -1) {
                throw new IllegalArgumentException(line);
            }
            long transactionId;
            try {
                transactionId = Long.parseLong(line.substring(0, endTransIdx));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(line);
            }
            int endActionIdx = line.indexOf(' ', endTransIdx + 1);
            if (endActionIdx == -1) {
                // action does not have arguments
                endActionIdx = line.length();
            }
            String actionLabel = line.substring(endTransIdx + 1, endActionIdx);
            String arguments = "";
            if (endActionIdx + 1 <= line.length()) {
                arguments = line.substring(endActionIdx + 1);
            }
            Action a;
            if (actionLabel.equals(Action.ADD_NODE)) {
                a = AddNode.fromString(transactionId, arguments);
            } else if (actionLabel.equals(Action.ADD_INDEX)) {
                a = AddIndex.fromString(transactionId, arguments);
            } else if (actionLabel.equals(Action.COMMIT)) {
                a = Commit.fromString(transactionId, arguments);
            } else if (actionLabel.equals(Action.CREATE_INDEX)) {
                a = CreateIndex.fromString(transactionId, arguments);
            } else if (actionLabel.equals(Action.DELETE_INDEX)) {
                a = DeleteIndex.fromString(transactionId, arguments);
            } else if (actionLabel.equals(Action.DELETE_NODE)) {
                a = DeleteNode.fromString(transactionId, arguments);
            } else if (actionLabel.equals(Action.START)) {
                a = Start.fromString(transactionId, arguments);
            } else if (actionLabel.equals(Action.VOLATILE_COMMIT)) {
                a = VolatileCommit.fromString(transactionId, arguments);
            } else {
                throw new IllegalArgumentException(line);
            }
            return a;
        }
    }

    /**
     * Adds an index to the MultiIndex's active persistent index list.
     */
    private static class AddIndex extends Action {

        /**
         * The name of the index to add.
         */
        private String indexName;

        /**
         * Creates a new AddIndex action.
         *
         * @param transactionId the id of the transaction that executes this
         *                      action.
         * @param indexName     the name of the index to add, or <code>null</code>
         *                      if an index with a new name should be created.
         */
        AddIndex(long transactionId, String indexName) {
            super(transactionId, Action.TYPE_ADD_INDEX);
            this.indexName = indexName;
        }

        /**
         * Creates a new AddIndex action.
         *
         * @param transactionId the id of the transaction that executes this
         *                      action.
         * @param arguments     the name of the index to add.
         * @return the AddIndex action.
         * @throws IllegalArgumentException if the arguments are malformed.
         */
        static AddIndex fromString(long transactionId, String arguments) {
            return new AddIndex(transactionId, arguments);
        }

        /**
         * Adds a sub index to <code>index</code>.
         *
         * @inheritDoc
         */
        public void execute(MultiIndex index) throws IOException {
            PersistentIndex idx = index.getOrCreateIndex(indexName);
            if (!index.indexNames.contains(indexName)) {
                index.indexNames.addName(indexName, idx.getCurrentGeneration());
                // now that the index is in the active list let the merger know about it
                index.merger.indexAdded(indexName, idx.getNumDocuments());
            }
        }

        /**
         * @inheritDoc
         */
        public String toString() {
            StringBuffer logLine = new StringBuffer();
            logLine.append(Long.toString(getTransactionId()));
            logLine.append(' ');
            logLine.append(Action.ADD_INDEX);
            logLine.append(' ');
            logLine.append(indexName);
            return logLine.toString();
        }
    }

    /**
     * Adds a node to the index.
     */
    private static class AddNode extends Action {

        /**
         * The maximum length of a AddNode String.
         */
        private static final int ENTRY_LENGTH =
            Long.toString(Long.MAX_VALUE).length() + Action.ADD_NODE.length()
            + new NodeId(0, 0).toString().length() + 2;

        /**
         * The id of the node to add.
         */
        private final NodeId id;

        /**
         * The document to add to the index, or <code>null</code> if not available.
         */
        private Document doc;

        /**
         * Creates a new AddNode action.
         *
         * @param transactionId the id of the transaction that executes this action.
         * @param id the id of the node to add.
         */
        AddNode(long transactionId, NodeId id) {
            super(transactionId, Action.TYPE_ADD_NODE);
            this.id = id;
        }

        /**
         * Creates a new AddNode action.
         *
         * @param transactionId the id of the transaction that executes this action.
         * @param doc the document to add.
         */
        AddNode(long transactionId, Document doc) {
            this(transactionId, new NodeId(doc.get(FieldNames.UUID)));
            this.doc = doc;
        }

        /**
         * Creates a new AddNode action.
         *
         * @param transactionId the id of the transaction that executes this
         *                      action.
         * @param arguments     The UUID of the node to add
         * @return the AddNode action.
         * @throws IllegalArgumentException if the arguments are malformed. Not a
         *                                  UUID.
         */
        static AddNode fromString(long transactionId, String arguments)
                throws IllegalArgumentException {
            return new AddNode(transactionId, new NodeId(arguments));
        }

        /**
         * Adds a node to the index.
         *
         * @inheritDoc
         */
        public void execute(MultiIndex index) throws IOException {
            if (doc == null) {
                try {
                    doc = index.createDocument(id);
                } catch (RepositoryException e) {
                    // node does not exist anymore
                    log.debug(e.getMessage());
                }
            }
            if (doc != null) {
                index.volatileIndex.addDocuments(new Document[]{doc});
            }
        }

        /**
         * @inheritDoc
         */
        public String toString() {
            StringBuffer logLine = new StringBuffer(ENTRY_LENGTH);
            logLine.append(Long.toString(getTransactionId()));
            logLine.append(' ');
            logLine.append(Action.ADD_NODE);
            logLine.append(' ');
            logLine.append(id);
            return logLine.toString();
        }
    }

    /**
     * Commits a transaction.
     */
    private static class Commit extends Action {

        /**
         * Creates a new Commit action.
         *
         * @param transactionId the id of the transaction that is committed.
         */
        Commit(long transactionId) {
            super(transactionId, Action.TYPE_COMMIT);
        }

        /**
         * Creates a new Commit action.
         *
         * @param transactionId the id of the transaction that executes this
         *                      action.
         * @param arguments     ignored by this method.
         * @return the Commit action.
         */
        static Commit fromString(long transactionId, String arguments) {
            return new Commit(transactionId);
        }

        /**
         * Touches the last flush time (sets it to the current time).
         *
         * @inheritDoc
         */
        public void execute(MultiIndex index) throws IOException {
            index.lastFlushTime = System.currentTimeMillis();
        }

        /**
         * @inheritDoc
         */
        public String toString() {
            return Long.toString(getTransactionId()) + ' ' + Action.COMMIT;
        }
    }

    /**
     * Creates an new sub index but does not add it to the active persistent index
     * list.
     */
    private static class CreateIndex extends Action {

        /**
         * The name of the index to add.
         */
        private String indexName;

        /**
         * Creates a new CreateIndex action.
         *
         * @param transactionId the id of the transaction that executes this
         *                      action.
         * @param indexName     the name of the index to add, or <code>null</code>
         *                      if an index with a new name should be created.
         */
        CreateIndex(long transactionId, String indexName) {
            super(transactionId, Action.TYPE_CREATE_INDEX);
            this.indexName = indexName;
        }

        /**
         * Creates a new CreateIndex action.
         *
         * @param transactionId the id of the transaction that executes this
         *                      action.
         * @param arguments     the name of the index to create.
         * @return the AddIndex action.
         * @throws IllegalArgumentException if the arguments are malformed.
         */
        static CreateIndex fromString(long transactionId, String arguments) {
            // when created from String, this action is executed as redo action
            return new CreateIndex(transactionId, arguments);
        }

        /**
         * Creates a new index.
         *
         * @inheritDoc
         */
        public void execute(MultiIndex index) throws IOException {
            PersistentIndex idx = index.getOrCreateIndex(indexName);
            indexName = idx.getName();
        }

        /**
         * @inheritDoc
         */
        public void undo(MultiIndex index) throws IOException {
            if (index.hasIndex(indexName)) {
                PersistentIndex idx = index.getOrCreateIndex(indexName);
                idx.close();
                index.deleteIndex(idx);
            }
        }

        /**
         * @inheritDoc
         */
        public String toString() {
            StringBuffer logLine = new StringBuffer();
            logLine.append(Long.toString(getTransactionId()));
            logLine.append(' ');
            logLine.append(Action.CREATE_INDEX);
            logLine.append(' ');
            logLine.append(indexName);
            return logLine.toString();
        }

        /**
         * Returns the index name that has been created. If this method is called
         * before {@link #execute(MultiIndex)} it will return <code>null</code>.
         *
         * @return the name of the index that has been created.
         */
        String getIndexName() {
            return indexName;
        }
    }

    /**
     * Closes and deletes an index that is no longer in use.
     */
    private static class DeleteIndex extends Action {

        /**
         * The name of the index to add.
         */
        private String indexName;

        /**
         * Creates a new DeleteIndex action.
         *
         * @param transactionId the id of the transaction that executes this
         *                      action.
         * @param indexName     the name of the index to delete.
         */
        DeleteIndex(long transactionId, String indexName) {
            super(transactionId, Action.TYPE_DELETE_INDEX);
            this.indexName = indexName;
        }

        /**
         * Creates a new DeleteIndex action.
         *
         * @param transactionId the id of the transaction that executes this
         *                      action.
         * @param arguments     the name of the index to delete.
         * @return the DeleteIndex action.
         * @throws IllegalArgumentException if the arguments are malformed.
         */
        static DeleteIndex fromString(long transactionId, String arguments) {
            return new DeleteIndex(transactionId, arguments);
        }

        /**
         * Removes a sub index from <code>index</code>.
         *
         * @inheritDoc
         */
        public void execute(MultiIndex index) throws IOException {
            // get index if it exists
            for (PersistentIndex idx : index.indexes) {
                if (idx.getName().equals(indexName)) {
                    idx.close();
                    index.deleteIndex(idx);
                    break;
                }
            }
        }

        /**
         * @inheritDoc
         */
        public String toString() {
            StringBuffer logLine = new StringBuffer();
            logLine.append(Long.toString(getTransactionId()));
            logLine.append(' ');
            logLine.append(Action.DELETE_INDEX);
            logLine.append(' ');
            logLine.append(indexName);
            return logLine.toString();
        }
    }

    /**
     * Deletes a node from the index.
     */
    private static class DeleteNode extends Action {

        /**
         * The maximum length of a DeleteNode String.
         */
        private static final int ENTRY_LENGTH =
            Long.toString(Long.MAX_VALUE).length() + Action.DELETE_NODE.length()
            + new NodeId(0, 0).toString().length() + 2;

        /**
         * The id of the node to remove.
         */
        private final NodeId id;

        /**
         * Creates a new DeleteNode action.
         *
         * @param transactionId the id of the transaction that executes this action.
         * @param id the id of the node to delete.
         */
        DeleteNode(long transactionId, NodeId id) {
            super(transactionId, Action.TYPE_DELETE_NODE);
            this.id = id;
        }

        /**
         * Creates a new DeleteNode action.
         *
         * @param transactionId the id of the transaction that executes this
         *                      action.
         * @param arguments     the UUID of the node to delete.
         * @return the DeleteNode action.
         * @throws IllegalArgumentException if the arguments are malformed. Not a
         *                                  UUID.
         */
        static DeleteNode fromString(long transactionId, String arguments) {
            return new DeleteNode(transactionId, new NodeId(arguments));
        }

        /**
         * Deletes a node from the index.
         *
         * @inheritDoc
         */
        public void execute(MultiIndex index) throws IOException {
            String uuidString = id.toString();
            // check if indexing queue is still working on
            // this node from a previous update
            Document doc = index.indexingQueue.removeDocument(uuidString);
            if (doc != null) {
                Util.disposeDocument(doc);
                index.notifyIfIndexingQueueIsEmpty();
            }
            Term idTerm = TermFactory.createUUIDTerm(uuidString);
            // if the document cannot be deleted from the volatile index
            // delete it from one of the persistent indexes.
            int num = index.volatileIndex.removeDocument(idTerm);
            if (num == 0) {
                for (int i = index.indexes.size() - 1; i >= 0; i--) {
                    // only look in registered indexes
                    PersistentIndex idx = index.indexes.get(i);
                    if (index.indexNames.contains(idx.getName())) {
                        num = idx.removeDocument(idTerm);
                        if (num > 0) {
                            return;
                        }
                    }
                }
            }
        }

        /**
         * @inheritDoc
         */
        public String toString() {
            StringBuffer logLine = new StringBuffer(ENTRY_LENGTH);
            logLine.append(Long.toString(getTransactionId()));
            logLine.append(' ');
            logLine.append(Action.DELETE_NODE);
            logLine.append(' ');
            logLine.append(id);
            return logLine.toString();
        }
    }

    /**
     * Starts a transaction.
     */
    private static class Start extends Action {

        /**
         * Creates a new Start transaction action.
         *
         * @param transactionId the id of the transaction that started.
         */
        Start(long transactionId) {
            super(transactionId, Action.TYPE_START);
        }

        /**
         * Creates a new Start action.
         *
         * @param transactionId the id of the transaction that executes this
         *                      action.
         * @param arguments     ignored by this method.
         * @return the Start action.
         */
        static Start fromString(long transactionId, String arguments) {
            return new Start(transactionId);
        }

        /**
         * Sets the current transaction id on <code>index</code>.
         *
         * @inheritDoc
         */
        public void execute(MultiIndex index) throws IOException {
            index.currentTransactionId = getTransactionId();
        }

        /**
         * @inheritDoc
         */
        public String toString() {
            return Long.toString(getTransactionId()) + ' ' + Action.START;
        }
    }

    /**
     * Commits the volatile index to disk.
     */
    private static class VolatileCommit extends Action {

        /**
         * The name of the target index to commit to.
         */
        private final String targetIndex;

        /**
         * Creates a new VolatileCommit action.
         *
         * @param transactionId the id of the transaction that executes this action.
         * @param targetIndex   the name of the index where the volatile index
         *                      will be committed.
         */
        VolatileCommit(long transactionId, String targetIndex) {
            super(transactionId, Action.TYPE_VOLATILE_COMMIT);
            this.targetIndex = targetIndex;
        }

        /**
         * Creates a new VolatileCommit action.
         *
         * @param transactionId the id of the transaction that executes this
         *                      action.
         * @param arguments     ignored by this implementation.
         * @return the VolatileCommit action.
         */
        static VolatileCommit fromString(long transactionId, String arguments) {
            return new VolatileCommit(transactionId, arguments);
        }

        /**
         * Commits the volatile index to disk.
         *
         * @inheritDoc
         */
        public void execute(MultiIndex index) throws IOException {
            VolatileIndex volatileIndex = index.getVolatileIndex();
            PersistentIndex persistentIndex = index.getOrCreateIndex(targetIndex);
            persistentIndex.copyIndex(volatileIndex);
            index.resetVolatileIndex();
        }

        /**
         * @inheritDoc
         */
        public String toString() {
            StringBuffer logLine = new StringBuffer();
            logLine.append(Long.toString(getTransactionId()));
            logLine.append(' ');
            logLine.append(Action.VOLATILE_COMMIT);
            logLine.append(' ');
            logLine.append(targetIndex);
            return logLine.toString();
        }
    }
}
