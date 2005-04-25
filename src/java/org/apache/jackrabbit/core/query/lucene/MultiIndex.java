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

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.fs.BasedFileSystem;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.NodeId;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * A <code>MultiIndex</code> consists of a {@link VolatileIndex} and multiple
 * {@link PersistentIndex}es. The goal is to keep most parts of the index open
 * with index readers and write new index data to the volatile index. When
 * the volatile index reaches a certain size (see {@link #setMinMergeDocs(int)} a
 * new persistent index is created with the index data from the volatile index.
 * the new persistent index is then added to the list of already existing
 * persistent indexes. Furhter operations on the new persistent index will
 * however only require an <code>IndexReader</code> which serves for queries
 * but also for delete operations on the index.
 * <p/>
 * The persistent indexes are merged from time to time. The merge behaviour
 * is configurable using the methods: {@link #setMaxMergeDocs(int)},
 * {@link #setMergeFactor(int)} and {@link #setMinMergeDocs(int)}. For detailed
 * description of the configuration parameters see also the lucene
 * <code>IndexWriter</code> class.
 * <p/>
 * This class is not thread-safe. Clients of this class must ensure
 * synchronization of multiple threads. The following conditions must hold true:
 * <ul>
 * <li>Only one thread may use {@link #addDocument(org.apache.lucene.document.Document)}
 * or {@link #removeDocument(org.apache.lucene.index.Term)} at a time.</li>
 * <li>While a thread uses the <code>IndexReader</code> returned by
 * {@link #getIndexReader()} other threads must not call {@link #addDocument(org.apache.lucene.document.Document)}
 * or {@link #removeDocument(org.apache.lucene.index.Term)}</li>
 * <li>Multiple threads may use the <code>IndexReader</code> returned by
 * {@link #getIndexReader()}</li>
 * </ul>
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
     * Compound file flag
     */
    private boolean useCompoundFile = true;

    /**
     * minMergeDocs config parameter
     */
    private int minMergeDocs = 1000;

    /**
     * maxMergeDocs config parameter
     */
    private int maxMergeDocs = 100000;

    /**
     * mergeFactor config parameter
     */
    private int mergeFactor = 10;

    /**
     * Default merge size: 1000
     */
    private static final long DEFAULT_MERGE_SIZE = 1000;

    /**
     * The maximum number of entries in the redo log until the volatile index
     * is merged into the persistent one.
     */
    private long mergeSize = DEFAULT_MERGE_SIZE;

    /**
     * Names of active persistent index directories.
     */
    private final IndexInfos indexNames = new IndexInfos();

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
    private final FileSystem fs;

    /**
     * The query handler
     */
    private final SearchIndex handler;

    /**
     * The volatile index.
     */
    private VolatileIndex volatileIndex;

    /**
     * If not <code>null</code> points to a valid <code>IndexReader</code> that
     * reads from all indexes, including volatile and persistent indexes.
     */
    private IndexReader multiReader;

    /**
     * Creates a new MultiIndex.
     *
     * @param fs the base file system
     * @param handler the search handler
     * @param stateMgr shared item state manager
     * @param rootUUID uuid of the root node
     * @throws FileSystemException if an error occurs
     * @throws IOException if an error occurs
     */
    MultiIndex(FileSystem fs,
               SearchIndex handler,
               ItemStateManager stateMgr,
               String rootUUID) throws FileSystemException, IOException {

        this.fs = fs;
        this.handler = handler;
        migrationCheck();
        boolean doInitialIndex = false;
        if (fs.exists("indexes")) {
            indexNames.read(fs);
        } else {
            doInitialIndex = true;
        }

        // read namespace mappings
        FileSystemResource mapFile = new FileSystemResource(fs, NS_MAPPING_FILE);
        nsMappings = new NamespaceMappings(mapFile);

        try {
            // open persistent indexes
            for (int i = 0; i < indexNames.size(); i++) {
                FileSystem sub = new BasedFileSystem(fs, indexNames.getName(i));
                sub.init();
                PersistentIndex index = new PersistentIndex(indexNames.getName(i), sub, false, handler.getAnalyzer());
                index.setMaxMergeDocs(maxMergeDocs);
                index.setMergeFactor(mergeFactor);
                index.setMinMergeDocs(minMergeDocs);
                index.setUseCompoundFile(useCompoundFile);
                indexes.add(index);
            }

            // create volatile index and check / apply redo log
            // init volatile index
            RedoLog redoLog = new RedoLog(new FileSystemResource(fs, REDO_LOG));

            if (redoLog.hasEntries()) {
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
                        }
                    } else {
                        deleteNodePersistent(entry.uuid);
                    }
                }
                log.warn("Redo changes applied.");
                redoLog.clear();
            }
            volatileIndex = new VolatileIndex(handler.getAnalyzer(), redoLog);
            volatileIndex.setUseCompoundFile(false);

            if (doInitialIndex) {
                // index root node
                NodeState rootState = (NodeState) stateMgr.getItemState(new NodeId(rootUUID));
                createIndex(rootState, stateMgr);
            }
        } catch (ItemStateException e) {
            throw new IOException("Error indexing root node: " + e.getMessage());
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        } catch (RepositoryException e) {
            throw new IOException("Error indexing root node: " + e.getMessage());
        }
    }

    /**
     * Adds a document to the index.
     *
     * @param doc the document to add.
     * @throws IOException if an error occurs while adding the document to the
     *                     index.
     */
    synchronized void addDocument(Document doc) throws IOException {
        multiReader = null;
        volatileIndex.addDocument(doc);
        if (volatileIndex.getRedoLog().getSize() >= mergeSize) {
            log.info("Commiting in-memory index");
            commit();
        }
    }

    /**
     * Deletes documents that match the <code>idTerm</code>.
     *
     * @param idTerm documents that match this term will be deleted.
     * @return the number of deleted documents.
     * @throws IOException if an error occurs while deleting documents.
     */
    synchronized int removeDocument(Term idTerm) throws IOException {
        // flush multi reader if it does not have deletions yet
        if (multiReader != null && !multiReader.hasDeletions()) {
            multiReader = null;
        }
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
     * Returns an <code>IndexReader</code> that spans alls indexes of this
     * <code>MultiIndex</code>.
     *
     * @return an <code>IndexReader</code>.
     * @throws IOException if an error occurs constructing the <code>IndexReader</code>.
     */
    synchronized IndexReader getIndexReader() throws IOException {
        if (multiReader == null) {
            IndexReader[] readers = new IndexReader[indexes.size() + 1];
            for (int i = 0; i < indexes.size(); i++) {
                readers[i] = ((PersistentIndex) indexes.get(i)).getIndexReader();
            }
            readers[readers.length - 1] = volatileIndex.getIndexReader();
            multiReader = new CachingMultiReader(readers);
        }
        return multiReader;
    }

    /**
     * Closes this <code>MultiIndex</code>.
     */
    synchronized void close() {
        multiReader = null;
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

    /**
     * Returns the namespace mappings of this search index.
     * @return the namespace mappings of this search index.
     */
    NamespaceMappings getNamespaceMappings() {
        return nsMappings;
    }

    //-----------------------< properties >-------------------------------------

    /**
     * The lucene index writer property: useCompoundFile
     */
    void setUseCompoundFile(boolean b) {
        useCompoundFile = b;
        // apply to all persistent indexes
        for (int i = 0; i < indexes.size(); i++) {
            ((PersistentIndex) indexes.get(i)).setUseCompoundFile(b);
        }
    }

    /**
     * The lucene index writer property: minMergeDocs
     */
    void setMinMergeDocs(int minMergeDocs) {
        this.minMergeDocs = minMergeDocs;
        // apply to all persistent indexes
        for (int i = 0; i < indexes.size(); i++) {
            ((PersistentIndex) indexes.get(i)).setMinMergeDocs(minMergeDocs);
        }
    }

    /**
     * The lucene index writer property: maxMergeDocs
     */
    void setMaxMergeDocs(int maxMergeDocs) {
        this.maxMergeDocs = maxMergeDocs;
        // apply to all persistent indexes
        for (int i = 0; i < indexes.size(); i++) {
            ((PersistentIndex) indexes.get(i)).setMaxMergeDocs(maxMergeDocs);
        }
    }

    /**
     * The lucene index writer property: mergeFactor
     */
    void setMergeFactor(int mergeFactor) {
        this.mergeFactor = mergeFactor;
        // apply to all persistent indexes
        for (int i = 0; i < indexes.size(); i++) {
            ((PersistentIndex) indexes.get(i)).setMergeFactor(mergeFactor);
        }
    }

    public void setBufferSize(int size) {
        volatileIndex.setBufferSize(size);
    }

    //-------------------------< internal >-------------------------------------

    /**
     * Commits the volatile index to a persistent index, commits persistent
     * indexes (persist deletions) and finally merges indexes if necessary.
     *
     * @throws IOException if an error occurs.
     */
    private void commit() throws IOException {
        // create new index folder
        String name = indexNames.newName();
        FileSystem sub = new BasedFileSystem(fs, name);
        PersistentIndex index;
        try {
            sub.init();
            index = new PersistentIndex(name, sub, true, handler.getAnalyzer());
            index.setMaxMergeDocs(maxMergeDocs);
            index.setMergeFactor(mergeFactor);
            index.setMinMergeDocs(minMergeDocs);
            index.setUseCompoundFile(useCompoundFile);
            indexes.add(index);
            indexNames.addName(name);
            indexNames.write(fs);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
        index.mergeIndex(volatileIndex);

        // commit persistent indexes
        for (int i = 0; i < indexes.size(); i++) {
            ((PersistentIndex) indexes.get(i)).commit();
        }

        // reset redo log
        try {
            volatileIndex.getRedoLog().clear();
        } catch (FileSystemException e) {
            log.error("Internal error: Unable to clear redo log.", e);
        }
        // create new volatile index
        volatileIndex = new VolatileIndex(handler.getAnalyzer(), volatileIndex.getRedoLog());
        volatileIndex.setUseCompoundFile(false);

        maybeMergeIndexes();
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
        addDocument(handler.createDocument(node, nsMappings));
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
     *
     * @param node the node to add.
     * @throws IOException         if an error occurs while writing to the
     *                             index.
     * @throws RepositoryException if any other error occurs
     */
    private void addNodePersistent(NodeState node)
            throws IOException, RepositoryException {
        Document doc = handler.createDocument(node, nsMappings);
        // make sure at least one persistent index exists
        if (indexes.size() == 0) {
            try {
                String name = indexNames.newName();
                FileSystem sub = new BasedFileSystem(fs, name);
                sub.init();
                PersistentIndex index = new PersistentIndex(name, sub, true, handler.getAnalyzer());
                index.setMaxMergeDocs(maxMergeDocs);
                index.setMergeFactor(mergeFactor);
                index.setMinMergeDocs(minMergeDocs);
                index.setUseCompoundFile(useCompoundFile);
                indexes.add(index);
                indexNames.addName(name);
                indexNames.write(fs);
            } catch (FileSystemException e) {
                throw new IOException(e.getMessage());
            }
        }
        // add node to last index
        PersistentIndex last = (PersistentIndex) indexes.get(indexes.size() - 1);
        last.addDocument(doc);
        maybeMergeIndexes();
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
     * Merges multiple persistent index into a single one according to the
     * properties: {@link #setMaxMergeDocs(int)}, {@link #setMergeFactor(int)}
     * and {@link #setMinMergeDocs(int)}.
     *
     * @throws IOException if an error occurs during the merge.
     */
    private void maybeMergeIndexes() throws IOException {
        // remove unused indexes
        for (int i = indexes.size() - 1; i >= 0; i--) {
            PersistentIndex index = (PersistentIndex) indexes.get(i);
            if (!index.hasDocuments()) {
                indexes.remove(i);
                indexNames.removeName(index.getName());
                index.close();
                try {
                    fs.deleteFolder(index.getName());
                } catch (FileSystemException e) {
                    log.warn("Unable to delete obsolete index: " + index.getName());
                    log.error(e.toString());
                }
            }
        }
        try {
            indexNames.write(fs);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }

        // only check for merge if there are more than mergeFactor indexes
        if (indexes.size() >= mergeFactor) {
            long targetMergeDocs = minMergeDocs;
            while (targetMergeDocs <= maxMergeDocs) {
                // find index smaller or equal than current target size
                int minIndex = indexes.size();
                int mergeDocs = 0;
                while (--minIndex >= 0) {
                    PersistentIndex index = (PersistentIndex) indexes.get(minIndex);
                    int numDocs = index.getIndexReader().numDocs();
                    if (numDocs > targetMergeDocs) {
                        break;
                    }
                    mergeDocs += numDocs;
                }

                if (indexes.size() - (minIndex + 1) >= mergeFactor && mergeDocs < maxMergeDocs) {
                    // found a merge to do
                    mergeIndex(minIndex + 1);
                } else {
                    break;
                }
                // increase target size
                targetMergeDocs *= mergeFactor;
            }
        }
    }

    /**
     * Merges indexes <code>indexes.get(i)</code> to <code>indexes.get(indexes.size()
     * - 1)</code> into a new persistent index.
     *
     * @param min the min position inside the indexes list.
     * @throws IOException if an error occurs while merging.
     */
    private void mergeIndex(int min) throws IOException {
        // create new index
        String name = indexNames.newName();
        FileSystem sub = new BasedFileSystem(fs, name);
        PersistentIndex index;
        try {
            sub.init();
            index = new PersistentIndex(name, sub, true, handler.getAnalyzer());
            index.setMaxMergeDocs(maxMergeDocs);
            index.setMergeFactor(mergeFactor);
            index.setMinMergeDocs(minMergeDocs);
            index.setUseCompoundFile(useCompoundFile);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
        // the indexes to merge
        List toMerge = indexes.subList(min, indexes.size());
        IndexReader[] readers = new IndexReader[toMerge.size()];
        for (int i = 0; i < toMerge.size(); i++) {
            readers[i] = ((PersistentIndex) toMerge.get(i)).getIndexReader();
        }
        // do the merge
        index.getIndexWriter().addIndexes(readers);
        index.getIndexWriter().optimize();
        // close and remove obsolete indexes
        for (int i = indexes.size() - 1; i >= min; i--) {
            PersistentIndex pi = (PersistentIndex) indexes.get(i);
            pi.close();
            try {
                fs.deleteFolder(pi.getName());
            } catch (FileSystemException e) {
                log.warn("Unable to delete obsolete index: " + name);
                log.error(e.toString());
            }
            indexNames.removeName(pi.getName());
            indexes.remove(i);
        }
        indexNames.addName(name);
        indexes.add(index);
        try {
            indexNames.write(fs);
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * <b>Note: This check will be removed when Jackrabbit 1.0 is final.</b>
     * <p/>
     * Checks if an old index format is present and moves it to the new
     * subindex structure.
     * @throws FileSystemException if an error occurs.
     * @throws IOException if an error occurs.
     */
    private void migrationCheck() throws FileSystemException, IOException {
        if (fs.exists("segments")) {
            // move to a sub folder
            String name = indexNames.newName();
            fs.createFolder(name);
            // move all files except: redo-log and ns-mappings
            Set exclude = new HashSet();
            exclude.add(REDO_LOG);
            exclude.add(NS_MAPPING_FILE);
            String[] files = fs.listFiles("/");
            for (int i = 0; i < files.length; i++) {
                if (exclude.contains(files[i])) {
                    continue;
                }
                fs.move(files[i], name + FileSystem.SEPARATOR + files[i]);
            }
            indexNames.addName(name);
            indexNames.write(fs);
        }
    }
}
