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

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.local.LocalFileSystem;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.uuid.Constants;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.commons.collections.iterators.EmptyIterator;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

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
 * <p/>
 * The persistent indexes are merged from time to time. The merge behaviour
 * is configurable using the methods: {@link SearchIndex#setMaxMergeDocs(int)},
 * {@link SearchIndex#setMergeFactor(int)} and {@link SearchIndex#setMinMergeDocs(int)}.
 * For detailed description of the configuration parameters see also the lucene
 * <code>IndexWriter</code> class.
 * <p/>
 * This class is thread-safe.
 * <p/>
 * Note on implementation: Multiple modifying threads are synchronized on a
 * <code>MultiIndex</code> instance itself. Sychronization between a modifying
 * thread and reader threads is done using {@link #updateMonitor} and
 * {@link #updateInProgress}.
 */
public class MultiIndex {

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(MultiIndex.class);

    /**
     * Default name of the redo log file
     */
    private static final String REDO_LOG = "redo.log";

    /**
     * Name of the file that contains the indexing queue log.
     */
    private static final String INDEXING_QUEUE_FILE = "indexing_queue.log";

    /**
     * Names of active persistent index directories.
     */
    private final IndexInfos indexNames = new IndexInfos("indexes");

    /**
     * Names of index directories that can be deleted.
     */
    private final IndexInfos deletable = new IndexInfos("deletable");

    /**
     * List of open persistent indexes. This list may also contain an open
     * PersistentIndex owned by the IndexMerger daemon. Such an index is not
     * registered with indexNames and <b>must not</b> be used in regular index
     * operations (delete node, etc.)!
     */
    private final List indexes = new ArrayList();

    /**
     * The internal namespace mappings of the query manager.
     */
    private final NamespaceMappings nsMappings;

    /**
     * The base filesystem to store the index.
     */
    private final File indexDir;

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
    private CachingMultiReader multiReader;

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
    private long lastFlushTime;

    /**
     * The <code>IndexMerger</code> for this <code>MultiIndex</code>.
     */
    private final IndexMerger merger;

    /**
     * Timer to schedule flushes of this index after some idle time.
     */
    private static final Timer FLUSH_TIMER = new Timer(true);

    /**
     * Task that is periodically called by {@link #FLUSH_TIMER} and checks
     * if index should be flushed.
     */
    private final Timer.Task flushTask;

    /**
     * The RedoLog of this <code>MultiIndex</code>.
     */
    private final RedoLog redoLog;

    /**
     * The indexing queue with pending text extraction jobs.
     */
    private IndexingQueue indexingQueue;

    /**
     * Set&lt;NodeId> of uuids that should not be indexed.
     */
    private final Set excludedIDs;

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
     * @param indexDir the base file system
     * @param handler the search handler
     * @param excludedIDs   Set&lt;NodeId> that contains uuids that should not
     *                      be indexed nor further traversed.
     * @param mapping the namespace mapping to use
     * @throws IOException if an error occurs
     */
    MultiIndex(File indexDir,
               SearchIndex handler,
               Set excludedIDs,
               NamespaceMappings mapping) throws IOException {

        this.indexDir = indexDir;
        this.handler = handler;
        this.cache = new DocNumberCache(handler.getCacheSize());
        this.redoLog = new RedoLog(new File(indexDir, REDO_LOG));
        this.excludedIDs = new HashSet(excludedIDs);
        this.nsMappings = mapping;

        if (indexNames.exists(indexDir)) {
            indexNames.read(indexDir);
        }
        if (deletable.exists(indexDir)) {
            deletable.read(indexDir);
        }

        // try to remove deletable files if there are any
        attemptDelete();

        // initialize IndexMerger
        merger = new IndexMerger(this);
        merger.setMaxMergeDocs(handler.getMaxMergeDocs());
        merger.setMergeFactor(handler.getMergeFactor());
        merger.setMinMergeDocs(handler.getMinMergeDocs());

        IndexingQueueStore store;
        try {
            LocalFileSystem fs = new LocalFileSystem();
            fs.setRoot(indexDir);
            fs.init();
            store = new IndexingQueueStore(fs, INDEXING_QUEUE_FILE);
        } catch (FileSystemException e) {
            IOException ex = new IOException();
            ex.initCause(e);
            throw ex;
        }

        // initialize indexing queue
        this.indexingQueue = new IndexingQueue(store, this);

        // open persistent indexes
        for (int i = 0; i < indexNames.size(); i++) {
            File sub = new File(indexDir, indexNames.getName(i));
            // only open if it still exists
            // it is possible that indexNames still contains a name for
            // an index that has been deleted, but indexNames has not been
            // written to disk.
            if (!sub.exists()) {
                log.debug("index does not exist anymore: " + sub.getAbsolutePath());
                // move on to next index
                continue;
            }
            PersistentIndex index = new PersistentIndex(
                    indexNames.getName(i), sub,
                    handler.getTextAnalyzer(), cache, indexingQueue);
            index.setMaxMergeDocs(handler.getMaxMergeDocs());
            index.setMergeFactor(handler.getMergeFactor());
            index.setMinMergeDocs(handler.getMinMergeDocs());
            index.setMaxFieldLength(handler.getMaxFieldLength());
            index.setUseCompoundFile(handler.getUseCompoundFile());
            indexes.add(index);
            merger.indexAdded(index.getName(), index.getNumDocuments());
        }

        // init volatile index
        resetVolatileIndex();

        // set index format version
        IndexReader reader = getIndexReader();
        try {
            version = IndexFormatVersion.getVersion(reader);
        } finally {
            reader.close();
        }

        redoLogApplied = redoLog.hasEntries();

        // run recovery
        Recovery.run(this, redoLog);

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

        flushTask = new Timer.Task() {
            public void run() {
                // check if there are any indexing jobs finished
                checkIndexingQueue();
                // check if volatile index should be flushed
                checkFlush();
            }
        };

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
            IndexReader reader = getIndexReader();
            try {
                return reader.numDocs();
            } finally {
                reader.close();
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
     * @throws IOException           if an error occurs while indexing the
     *                               workspace.
     * @throws IllegalStateException if this index is not empty.
     */
    void createInitialIndex(ItemStateManager stateMgr, NodeId rootId)
            throws IOException {
        // only do an initial index if there are no indexes at all
        if (indexNames.size() == 0) {
            reindexing = true;
            try {
                // traverse and index workspace
                executeAndLog(new Start(Action.INTERNAL_TRANSACTION));
                NodeState rootState = (NodeState) stateMgr.getItemState(rootId);
                createIndex(rootState, stateMgr);
                executeAndLog(new Commit(getTransactionId()));
                scheduleFlushTask();
            } catch (Exception e) {
                String msg = "Error indexing workspace";
                IOException ex = new IOException(msg);
                ex.initCause(e);
                throw ex;
            } finally {
                reindexing = false;
            }
        } else {
            throw new IllegalStateException("Index already present");
        }
    }

    /**
     * Atomically updates the index by removing some documents and adding
     * others.
     *
     * @param remove Iterator of <code>UUID</code>s that identify documents to
     *               remove
     * @param add    Iterator of <code>Document</code>s to add. Calls to
     *               <code>next()</code> on this iterator may return
     *               <code>null</code>, to indicate that a node could not be
     *               indexed successfully.
     */
    synchronized void update(Iterator remove, Iterator add) throws IOException {
        synchronized (updateMonitor) {
            updateInProgress = true;
        }
        try {
            long transactionId = nextTransactionId++;
            executeAndLog(new Start(transactionId));

            boolean flush = false;
            while (remove.hasNext()) {
                executeAndLog(new DeleteNode(transactionId, (UUID) remove.next()));
            }
            while (add.hasNext()) {
                Document doc = (Document) add.next();
                if (doc != null) {
                    executeAndLog(new AddNode(transactionId, doc));
                    // commit volatile index if needed
                    flush |= checkVolatileCommit();
                }
            }
            executeAndLog(new Commit(transactionId));

            // flush whole index when volatile index has been commited.
            if (flush) {
                flush();
            }
        } finally {
            synchronized (updateMonitor) {
                updateInProgress = false;
                updateMonitor.notifyAll();
                if (multiReader != null) {
                    multiReader.close();
                    multiReader = null;
                }
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
        List add = Arrays.asList(new Document[]{doc});
        update(EmptyIterator.INSTANCE, add.iterator());
    }

    /**
     * Deletes the first document that matches the <code>uuid</code>.
     *
     * @param uuid document that match this <code>uuid</code> will be deleted.
     * @throws IOException if an error occurs while deleting the document.
     */
    void removeDocument(UUID uuid) throws IOException {
        List remove = Arrays.asList(new UUID[]{uuid});
        update(remove.iterator(), EmptyIterator.INSTANCE);
    }

    /**
     * Deletes all documents that match the <code>uuid</code>.
     *
     * @param uuid documents that match this <code>uuid</code> will be deleted.
     * @return the number of deleted documents.
     * @throws IOException if an error occurs while deleting documents.
     */
    synchronized int removeAllDocuments(UUID uuid) throws IOException {
        synchronized (updateMonitor) {
            updateInProgress = true;
        }
        int num;
        try {
            Term idTerm = new Term(FieldNames.UUID, uuid.toString());
            executeAndLog(new Start(Action.INTERNAL_TRANSACTION));
            num = volatileIndex.removeDocument(idTerm);
            if (num > 0) {
                redoLog.append(new DeleteNode(getTransactionId(), uuid));
            }
            for (int i = 0; i < indexes.size(); i++) {
                PersistentIndex index = (PersistentIndex) indexes.get(i);
                // only remove documents from registered indexes
                if (indexNames.contains(index.getName())) {
                    int removed = index.removeDocument(idTerm);
                    if (removed > 0) {
                        redoLog.append(new DeleteNode(getTransactionId(), uuid));
                    }
                    num += removed;
                }
            }
            executeAndLog(new Commit(getTransactionId()));
        } finally {
            synchronized (updateMonitor) {
                updateInProgress = false;
                updateMonitor.notifyAll();
                if (multiReader != null) {
                    multiReader.close();
                    multiReader = null;
                }
            }
        }
        return num;
    }

    /**
     * Returns <code>IndexReader</code>s for the indexes named
     * <code>indexNames</code>. An <code>IndexListener</code> is registered and
     * notified when documents are deleted from one of the indexes in
     * <code>indexNames</code>.
     * <p/>
     * Note: the number of <code>IndexReaders</code> returned by this method is
     * not necessarily the same as the number of index names passed. An index
     * might have been deleted and is not reachable anymore.
     *
     * @param indexNames the names of the indexes for which to obtain readers.
     * @param listener   the listener to notify when documents are deleted.
     * @return the <code>IndexReaders</code>.
     * @throws IOException if an error occurs acquiring the index readers.
     */
    synchronized IndexReader[] getIndexReaders(String[] indexNames, IndexListener listener)
            throws IOException {
        Set names = new HashSet(Arrays.asList(indexNames));
        Map indexReaders = new HashMap();

        try {
            for (Iterator it = indexes.iterator(); it.hasNext();) {
                PersistentIndex index = (PersistentIndex) it.next();
                if (names.contains(index.getName())) {
                    indexReaders.put(index.getReadOnlyIndexReader(listener), index);
                }
            }
        } catch (IOException e) {
            // close readers obtained so far
            for (Iterator it = indexReaders.keySet().iterator(); it.hasNext();) {
                ReadOnlyIndexReader reader = (ReadOnlyIndexReader) it.next();
                try {
                    reader.close();
                } catch (IOException ex) {
                    log.warn("Exception closing index reader: " + ex);
                }
                ((PersistentIndex) indexReaders.get(reader)).resetListener();
            }
            throw e;
        }

        return (IndexReader[]) indexReaders.keySet().toArray(new IndexReader[indexReaders.size()]);
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
        for (Iterator it = indexes.iterator(); it.hasNext();) {
            PersistentIndex idx = (PersistentIndex) it.next();
            if (idx.getName().equals(indexName)) {
                return idx;
            }
        }

        // otherwise open / create it
        File sub;
        if (indexName == null) {
            sub = newIndexFolder();
            indexName = sub.getName();
        } else {
            sub = new File(indexDir, indexName);
        }
        PersistentIndex index = new PersistentIndex(indexName, sub,
                handler.getTextAnalyzer(), cache, indexingQueue);
        index.setMaxMergeDocs(handler.getMaxMergeDocs());
        index.setMergeFactor(handler.getMergeFactor());
        index.setMinMergeDocs(handler.getMinMergeDocs());
        index.setMaxFieldLength(handler.getMaxFieldLength());
        index.setUseCompoundFile(handler.getUseCompoundFile());

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
     */
    synchronized boolean hasIndex(String indexName) {
        // check existing
        for (Iterator it = indexes.iterator(); it.hasNext();) {
            PersistentIndex idx = (PersistentIndex) it.next();
            if (idx.getName().equals(indexName)) {
                return true;
            }
        }
        // check if it exists on disk
        return new File(indexDir, indexName).exists();
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
                        Collection deleted)
            throws IOException {

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
                Set names = new HashSet(Arrays.asList(obsoleteIndexes));
                for (Iterator it = names.iterator(); it.hasNext();) {
                    // do not try to delete indexes that are already gone
                    String indexName = (String) it.next();
                    if (indexNames.contains(indexName)) {
                        executeAndLog(new DeleteIndex(getTransactionId(), indexName));
                    }
                }

                // Index merger does not log an action when it creates the target
                // index of the merge. We have to do this here.
                executeAndLog(new CreateIndex(getTransactionId(), index.getName()));

                executeAndLog(new AddIndex(getTransactionId(), index.getName()));

                // delete documents in index
                for (Iterator it = deleted.iterator(); it.hasNext();) {
                    Term id = (Term) it.next();
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
                    if (multiReader != null) {
                        multiReader.close();
                        multiReader = null;
                    }
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
    public CachingMultiReader getIndexReader() throws IOException {
        synchronized (updateMonitor) {
            if (multiReader != null) {
                multiReader.incrementRefCount();
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
                List readerList = new ArrayList();
                for (int i = 0; i < indexes.size(); i++) {
                    PersistentIndex pIdx = (PersistentIndex) indexes.get(i);
                    if (indexNames.contains(pIdx.getName())) {
                        readerList.add(pIdx.getReadOnlyIndexReader());
                    }
                }
                readerList.add(volatileIndex.getReadOnlyIndexReader());
                ReadOnlyIndexReader[] readers =
                        (ReadOnlyIndexReader[]) readerList.toArray(new ReadOnlyIndexReader[readerList.size()]);
                multiReader = new CachingMultiReader(readers, cache);
            }
            multiReader.incrementRefCount();
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
     * Closes this <code>MultiIndex</code>.
     */
    void close() {

        // stop index merger
        // when calling this method we must not lock this MultiIndex, otherwise
        // a deadlock might occur
        merger.dispose();

        synchronized (this) {
            // stop timer
            flushTask.cancel();

            // commit / close indexes
            if (multiReader != null) {
                try {
                    multiReader.close();
                } catch (IOException e) {
                    log.error("Exception while closing search index.", e);
                }
                multiReader = null;
            }
            try {
                flush();
            } catch (IOException e) {
                log.error("Exception while closing search index.", e);
            }
            volatileIndex.close();
            for (int i = 0; i < indexes.size(); i++) {
                ((PersistentIndex) indexes.get(i)).close();
            }

            // finally close indexing queue
            try {
                indexingQueue.close();
            } catch (IOException e) {
                log.error("Exception while closing search index.", e);
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
     * Removes the <code>index</code> from the list of active sub indexes. The
     * Index is not acutally deleted right away, but postponed to the transaction
     * commit.
     * <p/>
     * This method does not close the index, but rather expects that the index
     * has already been closed.
     *
     * @param index the index to delete.
     */
    synchronized void deleteIndex(PersistentIndex index) {
        // remove it from the lists if index is registered
        indexes.remove(index);
        indexNames.removeName(index.getName());
        // during recovery it may happen that an index had already been marked
        // deleted, so we need to check if it is already marked deleted.
        synchronized (deletable) {
            if (!deletable.contains(index.getName())) {
                deletable.addName(index.getName());
            }
        }
    }

    /**
     * Flushes this <code>MultiIndex</code>. Persists all pending changes and
     * resets the redo log.
     *
     * @throws IOException if the flush fails.
     */
    void flush() throws IOException {
        synchronized (this) {
            // commit volatile index
            executeAndLog(new Start(Action.INTERNAL_TRANSACTION));
            commitVolatileIndex();

            // commit persistent indexes
            for (int i = indexes.size() - 1; i >= 0; i--) {
                PersistentIndex index = (PersistentIndex) indexes.get(i);
                // only commit indexes we own
                // index merger also places PersistentIndex instances in indexes,
                // but does not make them public by registering the name in indexNames
                if (indexNames.contains(index.getName())) {
                    index.commit();
                    // check if index still contains documents
                    if (index.getNumDocuments() == 0) {
                        executeAndLog(new DeleteIndex(getTransactionId(), index.getName()));
                    }
                }
            }
            executeAndLog(new Commit(getTransactionId()));

            indexNames.write(indexDir);

            // reset redo log
            redoLog.clear();

            lastFlushTime = System.currentTimeMillis();
        }

        // delete obsolete indexes
        attemptDelete();
    }

    //-------------------------< internal >-------------------------------------

    private void scheduleFlushTask() {
        lastFlushTime = System.currentTimeMillis();
        FLUSH_TIMER.schedule(flushTask, 0, 1000);
    }

    /**
     * Resets the volatile index to a new instance.
     */
    private void resetVolatileIndex() throws IOException {
        volatileIndex = new VolatileIndex(
                handler.getTextAnalyzer(), indexingQueue);
        volatileIndex.setUseCompoundFile(handler.getUseCompoundFile());
        volatileIndex.setMaxFieldLength(handler.getMaxFieldLength());
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
            // also flush indexing queue
            indexingQueue.commit();
        }
        return a;
    }

    /**
     * Checks if it is needed to commit the volatile index according to {@link
     * SearchIndex#getMinMergeDocs()}.
     *
     * @return <code>true</code> if the volatile index has been committed,
     *         <code>false</code> otherwise.
     * @throws IOException if an error occurs while committing the volatile
     *                     index.
     */
    private boolean checkVolatileCommit() throws IOException {
        if (volatileIndex.getNumDocuments() >= handler.getMinMergeDocs()) {
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
        if (volatileIndex.getNumDocuments() > 0) {

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
            log.debug("Committed in-memory index in " + time + "ms.");
        }
    }

    /**
     * Recursively creates an index starting with the NodeState
     * <code>node</code>.
     *
     * @param node     the current NodeState.
     * @param stateMgr the shared item state manager.
     * @throws IOException         if an error occurs while writing to the
     *                             index.
     * @throws ItemStateException  if an node state cannot be found.
     * @throws RepositoryException if any other error occurs
     */
    private void createIndex(NodeState node, ItemStateManager stateMgr)
            throws IOException, ItemStateException, RepositoryException {
        NodeId id = node.getNodeId();
        if (excludedIDs.contains(id)) {
            return;
        }
        executeAndLog(new AddNode(getTransactionId(), id.getUUID()));
        checkVolatileCommit();
        List children = node.getChildNodeEntries();
        for (Iterator it = children.iterator(); it.hasNext();) {
            NodeState.ChildNodeEntry child = (NodeState.ChildNodeEntry) it.next();
            NodeState childState = (NodeState) stateMgr.getItemState(child.getId());
            createIndex(childState, stateMgr);
        }
    }

    /**
     * Attempts to delete all files recorded in {@link #deletable}.
     */
    private void attemptDelete() {
        synchronized (deletable) {
            for (int i = deletable.size() - 1; i >= 0; i--) {
                String indexName = deletable.getName(i);
                File dir = new File(indexDir, indexName);
                if (deleteIndex(dir)) {
                    deletable.removeName(i);
                } else {
                    log.info("Unable to delete obsolete index: " + indexName);
                }
            }
            try {
                deletable.write(indexDir);
            } catch (IOException e) {
                log.warn("Exception while writing deletable indexes: " + e);
            }
        }
    }

    /**
     * Deletes the index <code>directory</code>.
     *
     * @param directory the index directory to delete.
     * @return <code>true</code> if the delete was successful,
     *         <code>false</code> otherwise.
     */
    private boolean deleteIndex(File directory) {
        // trivial if it does not exist anymore
        if (!directory.exists()) {
            return true;
        }
        // delete files first
        File[] files = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (!files[i].delete()) {
                return false;
            }
        }
        // now delete directory itself
        return directory.delete();
    }

    /**
     * Returns an new index folder which is empty.
     *
     * @return the new index folder.
     * @throws IOException if the folder cannot be created.
     */
    private File newIndexFolder() throws IOException {
        // create new index folder. make sure it does not exist
        File sub;
        do {
            sub = new File(indexDir, indexNames.newName());
        } while (sub.exists());

        if (!sub.mkdir()) {
            throw new IOException("Unable to create directory: " + sub.getAbsolutePath());
        }
        return sub;
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
                    log.debug("Flushing index after being idle for "
                            + idleTime + " ms.");
                    synchronized (updateMonitor) {
                        updateInProgress = true;
                    }
                    try {
                        flush();
                    } finally {
                        synchronized (updateMonitor) {
                            updateInProgress = false;
                            updateMonitor.notifyAll();
                            if (multiReader != null) {
                                multiReader.close();
                                multiReader = null;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                log.error("Unable to commit volatile index", e);
            }
        }
    }

    /**
     * Checks the indexing queue for finished text extrator jobs and
     * updates the index accordingly if there are any new ones.
     */
    private synchronized void checkIndexingQueue() {
        Document[] docs = indexingQueue.getFinishedDocuments();
        Map finished = new HashMap();
        for (int i = 0; i < docs.length; i++) {
            String uuid = docs[i].get(FieldNames.UUID);
            finished.put(UUID.fromString(uuid), docs[i]);
        }

        // now update index with the remaining ones if there are any
        if (!finished.isEmpty()) {
            log.debug("updating index with {} nodes from indexing queue.",
                    new Long(finished.size()));

            // remove documents from the queue
            for (Iterator it = finished.keySet().iterator(); it.hasNext(); ) {
                try {
                    indexingQueue.removeDocument(it.next().toString());
                } catch (IOException e) {
                    log.error("Failed to remove node from indexing queue", e);
                }
            }

            try {
                update(finished.keySet().iterator(),
                        finished.values().iterator());
            } catch (IOException e) {
                // update failed
                log.warn("Failed to update index with deferred text extraction", e);
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
                index.indexNames.addName(indexName);
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
        private static final int ENTRY_LENGTH = Long.toString(Long.MAX_VALUE).length()
                + Action.ADD_NODE.length()
                + Constants.UUID_FORMATTED_LENGTH
                + 2;

        /**
         * The uuid of the node to add.
         */
        private final UUID uuid;

        /**
         * The document to add to the index, or <code>null</code> if not available.
         */
        private Document doc;

        /**
         * Creates a new AddNode action.
         *
         * @param transactionId the id of the transaction that executes this action.
         * @param uuid the uuid of the node to add.
         */
        AddNode(long transactionId, UUID uuid) {
            super(transactionId, Action.TYPE_ADD_NODE);
            this.uuid = uuid;
        }

        /**
         * Creates a new AddNode action.
         *
         * @param transactionId the id of the transaction that executes this action.
         * @param doc the document to add.
         */
        AddNode(long transactionId, Document doc) {
            this(transactionId, UUID.fromString(doc.get(FieldNames.UUID)));
            this.doc = doc;
        }

        /**
         * Creates a new AddNode action.
         *
         * @param transactionId the id of the transaction that executes this
         *                      action.
         * @param arguments     the arguments to this action. The uuid of the node
         *                      to add
         * @return the AddNode action.
         * @throws IllegalArgumentException if the arguments are malformed. Not a
         *                                  UUID.
         */
        static AddNode fromString(long transactionId, String arguments)
                throws IllegalArgumentException {
            // simple length check
            if (arguments.length() != Constants.UUID_FORMATTED_LENGTH) {
                throw new IllegalArgumentException("arguments is not a uuid");
            }
            return new AddNode(transactionId, UUID.fromString(arguments));
        }

        /**
         * Adds a node to the index.
         *
         * @inheritDoc
         */
        public void execute(MultiIndex index) throws IOException {
            if (doc == null) {
                try {
                    doc = index.createDocument(new NodeId(uuid));
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
            logLine.append(uuid);
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
            for (Iterator it = index.indexes.iterator(); it.hasNext();) {
                PersistentIndex idx = (PersistentIndex) it.next();
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
        private static final int ENTRY_LENGTH = Long.toString(Long.MAX_VALUE).length()
                + Action.DELETE_NODE.length()
                + Constants.UUID_FORMATTED_LENGTH
                + 2;

        /**
         * The uuid of the node to remove.
         */
        private final UUID uuid;

        /**
         * Creates a new DeleteNode action.
         *
         * @param transactionId the id of the transaction that executes this action.
         * @param uuid the uuid of the node to delete.
         */
        DeleteNode(long transactionId, UUID uuid) {
            super(transactionId, Action.TYPE_DELETE_NODE);
            this.uuid = uuid;
        }

        /**
         * Creates a new DeleteNode action.
         *
         * @param transactionId the id of the transaction that executes this
         *                      action.
         * @param arguments     the uuid of the node to delete.
         * @return the DeleteNode action.
         * @throws IllegalArgumentException if the arguments are malformed. Not a
         *                                  UUID.
         */
        static DeleteNode fromString(long transactionId, String arguments) {
            // simple length check
            if (arguments.length() != Constants.UUID_FORMATTED_LENGTH) {
                throw new IllegalArgumentException("arguments is not a uuid");
            }
            return new DeleteNode(transactionId, UUID.fromString(arguments));
        }

        /**
         * Deletes a node from the index.
         *
         * @inheritDoc
         */
        public void execute(MultiIndex index) throws IOException {
            String uuidString = uuid.toString();
            // check if indexing queue is still working on
            // this node from a previous update
            Document doc = index.indexingQueue.removeDocument(uuidString);
            if (doc != null) {
                Util.disposeDocument(doc);
            }
            Term idTerm = new Term(FieldNames.UUID, uuidString);
            // if the document cannot be deleted from the volatile index
            // delete it from one of the persistent indexes.
            int num = index.volatileIndex.removeDocument(idTerm);
            if (num == 0) {
                for (int i = index.indexes.size() - 1; i >= 0; i--) {
                    // only look in registered indexes
                    PersistentIndex idx = (PersistentIndex) index.indexes.get(i);
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
            logLine.append(uuid);
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
