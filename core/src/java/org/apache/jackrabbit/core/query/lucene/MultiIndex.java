/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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
package org.apache.jackrabbit.core.query.lucene;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.log4j.Logger;
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
import java.util.Timer;
import java.util.TimerTask;
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
 * {@link SearchIndex#setMergeFactor(int)} and {@link SearchIndex#setMinMergeDocs(int)}. For detailed
 * description of the configuration parameters see also the lucene
 * <code>IndexWriter</code> class.
 * <p/>
 * This class is thread-safe.
 * <p/>
 * Note on implementation: Multiple modifying threads are synchronized on a
 * <code>MultiIndex</code> instance itself. Sychronization between a modifying
 * thread and reader threads is done using {@link #updateMonitor} and
 * {@link #updateInProgress}.
 */
class MultiIndex {

    /**
     * The logger instance for this class
     */
    private static final Logger log = Logger.getLogger(MultiIndex.class);

    /**
     * Name of the file to persist search internal namespace mappings
     */
    private static final String NS_MAPPING_FILE = "ns_mappings.properties";

    /**
     * Default name of the redo log file
     */
    private static final String REDO_LOG = "redo.log";

    /**
     * Names of active persistent index directories.
     */
    private final IndexInfos indexNames = new IndexInfos("indexes");

    /**
     * Names of index directories that can be deleted.
     */
    private final IndexInfos deletable = new IndexInfos("deletable");

    /**
     * List of persistent indexes.
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
     * The last time this index was modified. That is, a document was added.
     */
    private long lastModificationTime;

    /**
     * The <code>IndexMerger</code> for this <code>MultiIndex</code>.
     */
    private final IndexMerger merger;

    /**
     * Timer to schedule commits of the volatile index after some idle time.
     */
    private final Timer commitTimer = new Timer(true);

    /**
     * Creates a new MultiIndex.
     *
     * @param indexDir the base file system
     * @param handler the search handler
     * @param stateMgr shared item state manager
     * @param rootUUID uuid of the root node
     * @throws IOException if an error occurs
     */
    MultiIndex(File indexDir,
               SearchIndex handler,
               ItemStateManager stateMgr,
               String rootUUID) throws IOException {

        this.indexDir = indexDir;
        this.handler = handler;
        this.cache = new DocNumberCache(handler.getCacheSize());

        boolean doInitialIndex = false;
        if (indexNames.exists(indexDir)) {
            indexNames.read(indexDir);
        } else {
            doInitialIndex = true;
        }
        if (deletable.exists(indexDir)) {
            deletable.read(indexDir);
        }
        // try to remove deletable files if there are any
        attemptDelete();

        // read namespace mappings
        File mapFile = new File(indexDir, NS_MAPPING_FILE);
        nsMappings = new NamespaceMappings(mapFile);

        // initialize IndexMerger
        merger = new IndexMerger(this);
        merger.setMaxMergeDocs(handler.getMaxMergeDocs());
        merger.setMergeFactor(handler.getMergeFactor());
        merger.setMinMergeDocs(handler.getMinMergeDocs());

        try {
            // open persistent indexes
            for (int i = 0; i < indexNames.size(); i++) {
                File sub = new File(indexDir, indexNames.getName(i));
                if (!sub.exists() && !sub.mkdir()) {
                    throw new IOException("Unable to create directory: " + sub.getAbsolutePath());
                }
                PersistentIndex index = new PersistentIndex(indexNames.getName(i),
                        sub, false, handler.getAnalyzer(), cache);
                index.setMaxMergeDocs(handler.getMaxMergeDocs());
                index.setMergeFactor(handler.getMergeFactor());
                index.setMinMergeDocs(handler.getMinMergeDocs());
                index.setUseCompoundFile(handler.getUseCompoundFile());
                indexes.add(index);
                merger.indexAdded(index.getName(), index.getNumDocuments());
            }

            // create volatile index and check / apply redo log
            // init volatile index
            RedoLog redoLog = new RedoLog(new File(indexDir, REDO_LOG));

            if (redoLog.hasEntries()) {
                // when we have entries in the redo log there is no need to reindex
                doInitialIndex = false;

                log.warn("Found uncommitted redo log. Applying changes now...");
                // apply changes to persistent index
                Iterator it = redoLog.getEntries().iterator();
                while (it.hasNext()) {
                    RedoLog.Entry entry = (RedoLog.Entry) it.next();
                    if (entry.type == RedoLog.Entry.NODE_ADDED) {
                        try {
                            NodeState state = (NodeState) stateMgr.getItemState(new NodeId(entry.uuid));
                            addNodePersistent(state);
                        } catch (NoSuchItemStateException e) {
                            // item does not exist anymore
                        } catch (Exception e) {
                            log.warn("Unable to add node to index: ", e);
                        }
                    } else {
                        deleteNodePersistent(entry.uuid);
                    }
                }
                log.warn("Redo changes applied.");
                redoLog.clear();
                redoLogApplied = true;
            }

            volatileIndex = new VolatileIndex(handler.getAnalyzer(), redoLog);
            volatileIndex.setUseCompoundFile(handler.getUseCompoundFile());
            volatileIndex.setBufferSize(handler.getBufferSize());

            // now that we are ready, start index merger
            merger.start();

            if (doInitialIndex) {
                // index root node
                NodeState rootState = (NodeState) stateMgr.getItemState(new NodeId(rootUUID));
                createIndex(rootState, stateMgr);
            }
        } catch (ItemStateException e) {
            throw new IOException("Error indexing root node: " + e.getMessage());
        } catch (RepositoryException e) {
            throw new IOException("Error indexing root node: " + e.getMessage());
        }
        lastModificationTime = System.currentTimeMillis();
        startCommitTimer();
    }

    /**
     * Update the index by removing some documents and adding others.
     *
     * @param remove Iterator of <code>Term</code>s that identify documents to
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
        boolean hasAdditions = add.hasNext();
        try {
            // todo block with remove & add is not atomic
            while (remove.hasNext()) {
                internalRemoveDocument((Term) remove.next());
            }
            while (add.hasNext()) {
                Document doc = (Document) add.next();
                if (doc != null) {
                    internalAddDocument(doc);
                }
            }
        } finally {
            synchronized (updateMonitor) {
                if (hasAdditions) {
                    lastModificationTime = System.currentTimeMillis();
                }
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
     * Deletes the first document that matches the <code>idTerm</code>.
     *
     * @param idTerm document that match this term will be deleted.
     * @throws IOException if an error occurs while deleting the document.
     */
    void removeDocument(Term idTerm) throws IOException {
        List remove = Arrays.asList(new Term[]{idTerm});
        update(remove.iterator(), EmptyIterator.INSTANCE);
    }

    /**
     * Deletes all documents that match the <code>idTerm</code> and immediately
     * commits the changes to the persistent indexes.
     *
     * @param idTerm documents that match this term will be deleted.
     * @return the number of deleted documents.
     * @throws IOException if an error occurs while deleting documents.
     */
    synchronized int removeAllDocuments(Term idTerm) throws IOException {
        synchronized (updateMonitor) {
            updateInProgress = true;
        }
        int num;
        try {
            num = volatileIndex.removeDocument(idTerm);
            for (int i = 0; i < indexes.size(); i++) {
                PersistentIndex index = (PersistentIndex) indexes.get(i);
                num += index.removeDocument(idTerm);
                index.commit();
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
            for (Iterator it = indexes.iterator(); it.hasNext(); ) {
                PersistentIndex index = (PersistentIndex) it.next();
                if (names.contains(index.getName())) {
                    indexReaders.put(index.getReadOnlyIndexReader(listener), index);
                }
            }
        } catch (IOException e) {
            // close readers obtained so far
            for (Iterator it = indexReaders.keySet().iterator(); it.hasNext(); ) {
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
     * @return a new <code>PersistentIndex</code>.
     * @throws IOException if a new index cannot be created.
     */
    synchronized PersistentIndex createIndex() throws IOException {
        File sub = newIndexFolder();
        String name = sub.getName();
        PersistentIndex index = new PersistentIndex(name, sub, true,
                handler.getAnalyzer(), cache);
        index.setMaxMergeDocs(handler.getMaxMergeDocs());
        index.setMergeFactor(handler.getMergeFactor());
        index.setMinMergeDocs(handler.getMinMergeDocs());
        index.setUseCompoundFile(handler.getUseCompoundFile());
        return index;
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
    synchronized void replaceIndexes(String[] obsoleteIndexes,
                                     PersistentIndex index,
                                     Collection deleted)
            throws IOException {
        Set names = new HashSet(Arrays.asList(obsoleteIndexes));
        // delete documents in index
        for (Iterator it = deleted.iterator(); it.hasNext(); ) {
            Term id = (Term) it.next();
            int del = index.removeDocument(id);
            log.error("deleted " + del + " document for id: " + id.text());
        }
        index.commit();

        // now replace indexes
        synchronized (updateMonitor) {
            updateInProgress = true;
        }
        try {
            for (Iterator it = indexes.iterator(); it.hasNext(); ) {
                PersistentIndex idx = (PersistentIndex) it.next();
                if (names.contains(idx.getName())) {
                    it.remove();
                    indexNames.removeName(idx.getName());
                    idx.close();
                    deleteIndex(idx);
                }
            }
            // add new
            indexes.add(index);
            indexNames.addName(index.getName());
            merger.indexAdded(index.getName(), index.getNumDocuments());
            indexNames.write(indexDir);
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
     * Returns an read-only <code>IndexReader</code> that spans alls indexes of this
     * <code>MultiIndex</code>.
     *
     * @return an <code>IndexReader</code>.
     * @throws IOException if an error occurs constructing the <code>IndexReader</code>.
     */
    IndexReader getIndexReader() throws IOException {
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
                ReadOnlyIndexReader[] readers = new ReadOnlyIndexReader[indexes.size() + 1];
                for (int i = 0; i < indexes.size(); i++) {
                    readers[i] = ((PersistentIndex) indexes.get(i)).getReadOnlyIndexReader();
                }
                readers[readers.length - 1] = volatileIndex.getReadOnlyIndexReader();
                multiReader = new CachingMultiReader(readers, cache);
            }
            multiReader.incrementRefCount();
            return multiReader;
        }
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
            commitTimer.cancel();

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
                if (volatileIndex.getRedoLog().hasEntries()) {
                    commit();
                }
            } catch (IOException e) {
                log.error("Exception while closing search index.", e);
            }
            volatileIndex.close();
            for (int i = 0; i < indexes.size(); i++) {
                ((PersistentIndex) indexes.get(i)).close();
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
     * Returns a lucene Document for the <code>node</code>.
     * @param node the node to index.
     * @return the index document.
     * @throws RepositoryException if an error occurs while reading from the
     *   workspace.
     */
    Document createDocument(NodeState node) throws RepositoryException {
        return handler.createDocument(node, nsMappings);
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
     * Deletes the <code>index</code>. If the index directory cannot be removed
     * because (windows) file handles are still open, the directory is marked
     * for future deletion.
     * <p/>
     * This method does not close the index, but rather expects that the index
     * has already been closed.
     *
     * @param index the index to delete.
     */
    void deleteIndex(PersistentIndex index) {
        File dir = new File(indexDir, index.getName());
        if (!deleteIndex(dir)) {
            // try again later
            deletable.addName(index.getName());
        }
        try {
            deletable.write(indexDir);
        } catch (IOException e) {
            log.warn("Exception while writing deletable indexes: " + e);
        }
    }

    //-------------------------< internal >-------------------------------------

    /**
     * Unsynchronized implementation to remove a document from the index. Note:
     * this method will at most remove 1 (one) document from the index. This
     * method assumes <code>idTerm</code> is unique.
     *
     * @param idTerm term that identifies the document to remove.
     * @return number of documents to remove.
     * @throws IOException if an error occurs while updating the index.
     */
    private int internalRemoveDocument(Term idTerm) throws IOException {
        // if the document cannot be deleted from the volatile index
        // delete it from one of the persistent indexes.
        int num = volatileIndex.removeDocument(idTerm);
        if (num == 0) {
            for (int i = indexes.size() - 1; i >= 0; i--) {
                PersistentIndex index = (PersistentIndex) indexes.get(i);
                num = index.removeDocument(idTerm);
                if (num > 0) {
                    return num;
                }
            }
        } else {
            return num;
        }
        return 0;
    }

    /**
     * Unsynchronized implementation to add a document to the index.
     *
     * @param doc the document to add.
     * @throws IOException if an error occurs while adding the document to the
     *                     index.
     */
    private void internalAddDocument(Document doc) throws IOException {
        volatileIndex.addDocument(doc);
        if (volatileIndex.getRedoLog().getSize() >= handler.getMinMergeDocs()) {
            long time = System.currentTimeMillis();
            commit();
            time = System.currentTimeMillis() - time;
            log.info("Committed in-memory index in " + time + "ms.");
        }
    }

    /**
     * Commits the volatile index to a persistent index, commits persistent
     * indexes (persist deletions) and finally merges indexes if necessary.
     *
     * @throws IOException if an error occurs.
     */
    private void commit() throws IOException {

        // check if volatile index contains documents at all
        if (volatileIndex.getIndexReader().numDocs() > 0) {

            File sub = newIndexFolder();
            String name = sub.getName();
            PersistentIndex index = new PersistentIndex(name, sub, true,
                    handler.getAnalyzer(), cache);
            index.setMaxMergeDocs(handler.getMaxMergeDocs());
            index.setMergeFactor(handler.getMergeFactor());
            index.setMinMergeDocs(handler.getMinMergeDocs());
            index.setUseCompoundFile(handler.getUseCompoundFile());
            index.copyIndex(volatileIndex);

            // if merge has been successful add index
            indexes.add(index);
            indexNames.addName(name);
            indexNames.write(indexDir);

            merger.indexAdded(index.getName(), index.getNumDocuments());

            // check if obsolete indexes can be deleted
            // todo move to other place?
            attemptDelete();
        }

        // commit persistent indexes
        for (int i = indexes.size() - 1; i >= 0; i--) {
            PersistentIndex index = (PersistentIndex) indexes.get(i);
            index.commit();
            // check if index still contains documents
            if (index.getNumDocuments() == 0) {
                indexes.remove(i);
                indexNames.removeName(index.getName());
                indexNames.write(indexDir);
                index.close();
                deleteIndex(index);
            }
        }

        // reset redo log
        volatileIndex.getRedoLog().clear();

        // create new volatile index
        volatileIndex = new VolatileIndex(handler.getAnalyzer(), volatileIndex.getRedoLog());
        volatileIndex.setUseCompoundFile(handler.getUseCompoundFile());
        volatileIndex.setBufferSize(handler.getBufferSize());

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
        addDocument(createDocument(node));
        List children = node.getChildNodeEntries();
        for (Iterator it = children.iterator(); it.hasNext();) {
            NodeState.ChildNodeEntry child = (NodeState.ChildNodeEntry) it.next();
            NodeState childState = (NodeState) stateMgr.getItemState(new NodeId(child.getUUID()));
            createIndex(childState, stateMgr);
        }
    }

    /**
     * Adds a node to the persistent index. This method will <b>not</b> aquire a
     * write lock while writing!
     * <p/>
     * If an error occurs when reading from the ItemStateManager an error log
     * message is written and the node is ignored.
     *
     * @param node the node to add.
     * @throws IOException         if an error occurs while writing to the
     *                             index.
     */
    private void addNodePersistent(NodeState node)
            throws IOException {
        Document doc;
        try {
            doc = createDocument(node);
        } catch (RepositoryException e) {
            log.warn("RepositoryException: " + e.getMessage());
            return;
        }
        // make sure at least one persistent index exists
        if (indexes.size() == 0) {
            File sub = newIndexFolder();
            String name = sub.getName();
            PersistentIndex index = new PersistentIndex(name, sub, true,
                    handler.getAnalyzer(), cache);
            index.setMaxMergeDocs(handler.getMaxMergeDocs());
            index.setMergeFactor(handler.getMergeFactor());
            index.setMinMergeDocs(handler.getMinMergeDocs());
            index.setUseCompoundFile(handler.getUseCompoundFile());
            indexes.add(index);
            indexNames.addName(name);
            indexNames.write(indexDir);
        }
        // add node to last index
        PersistentIndex last = (PersistentIndex) indexes.get(indexes.size() - 1);
        last.addDocument(doc);
    }

    /**
     * Removes a node from the persistent index. This method will <b>not</b>
     * aquire a write lock while writing!
     *
     * @param uuid the uuid of the node to remove.
     * @throws IOException if an error occurs while writing to the index.
     */
    private void deleteNodePersistent(String uuid) throws IOException {
        Term idTerm = new Term(FieldNames.UUID, uuid);
        // try to remove node from index until successful
        // use reverse order; nodes that survived for a long time
        // will probably never be deleted.
        for (int i = indexes.size() - 1; i >= 0; i--) {
            PersistentIndex index = (PersistentIndex) indexes.get(i);
            if (index.removeDocument(idTerm) > 0) {
                break;
            }
        }
    }

    /**
     * Attempts to delete all files recorded in {@link #deletable}.
     */
    private void attemptDelete() {
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
     * Starts the commit timer that periodically checks if the volatile index
     * should be committed. The timer task will call {@link #checkCommit()}.
     */
    private void startCommitTimer() {
        commitTimer.schedule(new TimerTask() {
            public void run() {
                checkCommit();
            }
        }, 0, 1000);
    }

    /**
     * Checks the duration between the last modification to this index and the
     * current time and commits the volatile index (if there are changes at all)
     * if the duration (idle time) is more than {@link SearchIndex#getVolatileIdleTime()}
     * seconds.
     */
    private synchronized void checkCommit() {
        long idleTime = System.currentTimeMillis() - lastModificationTime;
        // do not commit if volatileIdleTime is zero or negative
        if (handler.getVolatileIdleTime() > 0
                && idleTime > handler.getVolatileIdleTime() * 1000) {
            try {
                if (volatileIndex.getRedoLog().hasEntries()) {
                    log.info("Committing in-memory index after being idle for " +
                            idleTime + " ms.");
                    synchronized (updateMonitor) {
                        updateInProgress = true;
                    }
                    try {
                        commit();
                    } finally {
                        synchronized (updateMonitor) {
                            lastModificationTime = System.currentTimeMillis();
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
}
