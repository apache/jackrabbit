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
package org.apache.jackrabbit.core.search.lucene;

import EDU.oswego.cs.dl.util.concurrent.FIFOReadWriteLock;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.search.AbstractQueryHandler;
import org.apache.jackrabbit.core.search.QueryConstants;
import org.apache.jackrabbit.core.search.ExecutableQuery;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.NoPrefixDeclaredException;
import org.apache.jackrabbit.core.NodeId;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.IndexSearcher;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;

/**
 * Implements a {@link org.apache.jackrabbit.core.search.QueryHandler} using
 * Lucene.
 */
public class SearchIndex extends AbstractQueryHandler {

    /** The logger instance for this class */
    private static final Logger log = Logger.getLogger(SearchIndex.class);

    /** Name of the write lock file */
    private static final String WRITE_LOCK = "write.lock";

    /** Default name of the redo log file */
    private static final String REDO_LOG = "redo.log";

    /** Name of the file to persist search internal namespace mappings */
    private static final String NS_MAPPING_FILE = "ns_mappings.properties";

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
     * The persistent index.
     */
    private PersistentIndex persistentIndex;

    /**
     * The in-memory index.
     */
    private VolatileIndex volatileIndex;

    /**
     * The analyzer we use for indexing.
     */
    private final Analyzer analyzer;

    /**
     * Internal namespace mappings.
     */
    private NamespaceMappings nsMappings;

    /**
     * Read-write lock to synchronize access on the index.
     */
    private final FIFOReadWriteLock readWriteLock = new FIFOReadWriteLock();

    /**
     * Default constructor.
     */
    public SearchIndex() {
        this.analyzer = new StandardAnalyzer();
    }

    /**
     * Initializes this <code>QueryHandler</code>.
     * @throws IOException if an error occurs while initializing this handler.
     */
    protected void doInit() throws IOException {
        try {
            // check if index is locked, probably from an unclean repository
            // shutdown
            if (getFileSystem().exists(WRITE_LOCK)) {
                log.warn("Removing write lock on search index.");
                try {
                    getFileSystem().deleteFile(WRITE_LOCK);
                } catch (FileSystemException e) {
                    log.error("Unable to remove write lock on search index.");
                }
            }

            boolean create = !getFileSystem().exists("segments");
            persistentIndex = new PersistentIndex(getFileSystem(), create, analyzer);
            persistentIndex.setUseCompoundFile(true);
            FileSystemResource mapFile = new FileSystemResource(getFileSystem(), NS_MAPPING_FILE);
            nsMappings = new NamespaceMappings(mapFile);
            if (create) {
                // index root node
                NodeState rootState = (NodeState) getItemStateProvider().getItemState(new NodeId(getRootUUID()));
                createIndex(rootState);
            }

            // init volatile index
            RedoLog redoLog = new RedoLog(new FileSystemResource(getFileSystem(), REDO_LOG));
            if (redoLog.hasEntries()) {
                log.warn("Found uncommitted redo log. Applying changes now...");
                ItemStateManager itemMgr = getItemStateProvider();
                // apply changes to persistent index
                Iterator it = redoLog.getEntries().iterator();
                while (it.hasNext()) {
                    RedoLog.Entry entry = (RedoLog.Entry) it.next();
                    if (entry.type == RedoLog.Entry.NODE_ADDED) {
                        try {
                            NodeState state = (NodeState) itemMgr.getItemState(new NodeId(entry.uuid));
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
            volatileIndex = new VolatileIndex(analyzer, redoLog);
            volatileIndex.setUseCompoundFile(false);
        } catch (ItemStateException e) {
            throw new IOException("Error indexing root node: " + e.getMessage());
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        } catch (RepositoryException e) {
            throw new IOException("Error indexing root node: " + e.getMessage());
        }
    }

    /**
     * Adds the <code>node</code> to the search index.
     * @param node the node to add.
     * @throws RepositoryException if an error occurs while indexing the node.
     * @throws IOException if an error occurs while adding the node to the index.
     */
    public void addNode(NodeState node) throws RepositoryException, IOException {
        Document doc = NodeIndexer.createDocument(node, getItemStateProvider(), nsMappings);
        try {
            readWriteLock.writeLock().acquire();
        } catch (InterruptedException e) {
            throw new RepositoryException("Failed to aquire write lock.");
        }

        try {
            volatileIndex.addDocument(doc);
            if (volatileIndex.getRedoLog().getSize() > mergeSize) {
                log.info("Merging in-memory index");
                persistentIndex.mergeIndex(volatileIndex);
                // reset redo log
                try {
                    volatileIndex.getRedoLog().clear();
                } catch (FileSystemException e) {
                    log.error("Internal error: Unable to clear redo log.", e);
                }
                // create new volatile index
                volatileIndex = new VolatileIndex(analyzer, volatileIndex.getRedoLog());
                volatileIndex.setUseCompoundFile(false);
            }
        } finally {
            readWriteLock.writeLock().release();
        }
    }

    /**
     * Removes the node with <code>uuid</code> from the search index.
     * @param uuid the UUID of the node to remove from the index.
     * @throws IOException if an error occurs while removing the node from
     * the index.
     */
    public void deleteNode(String uuid) throws IOException {
        Term idTerm = new Term(FieldNames.UUID, uuid);
        try {
            readWriteLock.writeLock().acquire();
        } catch (InterruptedException e) {
            throw new IOException("Failed to aquire write lock.");
        }

        try {
            // if the document cannot be deleted from the volatile index
            // delete it from the persistent index.
            if (volatileIndex.removeDocument(idTerm) == 0) {
                persistentIndex.removeDocument(idTerm);
            }
        } finally {
            readWriteLock.writeLock().release();
        }

    }

    /**
     * Creates a new query by specifying the query statement itself and the
     * language in which the query is stated.  If the query statement is
     * syntactically invalid, given the language specified, an
     * InvalidQueryException is thrown. <code>language</code> must specify a query language
     * string from among those returned by QueryManager.getSupportedQueryLanguages(); if it is not
     * then an <code>InvalidQueryException</code> is thrown.
     *
     * @param session the session of the current user creating the query object.
     * @param itemMgr the item manager of the current user.
     * @param statement the query statement.
     * @param language the syntax of the query statement.
     * @throws InvalidQueryException if statement is invalid or language is unsupported.
     * @return A <code>Query</code> object.
     */
    public ExecutableQuery createExecutableQuery(SessionImpl session,
                                             ItemManager itemMgr,
                                             String statement,
                                             String language)
            throws InvalidQueryException {
        return new QueryImpl(session, itemMgr, this, getPropertyTypeRegistry(), statement, language);
    }

    /**
     * Closes this <code>QueryHandler</code> and frees resources attached
     * to this handler.
     */
    public void close() {
        log.info("Closing search index.");
        try {
            if (volatileIndex.getRedoLog().hasEntries()) {
                persistentIndex.mergeIndex(volatileIndex);
                volatileIndex.getRedoLog().clear();
            }
        } catch (IOException e) {
            log.error("Exception while closing search index.", e);
        } catch (FileSystemException e) {
            log.error("Exception while closing search index.", e);
        }
        volatileIndex.close();
        persistentIndex.close();
    }

    /**
     * Executes the query on the search index.
     * @param query the lucene query.
     * @param orderProps name of the properties for sort order.
     * @param orderSpecs the order specs for the sort order properties.
     * <code>true</code> indicates ascending order, <code>false</code> indicates
     * descending.
     * @return the lucene Hits object.
     * @throws IOException if an error occurs while searching the index.
     */
    Hits executeQuery(Query query,
                             QName[] orderProps,
                             boolean[] orderSpecs) throws IOException {
        try {
            readWriteLock.readLock().acquire();
        } catch (InterruptedException e) {
            throw new IOException("Unable to obtain read lock on search index.");
        }

        Hits hits = null;
        try {
            SortField[] sortFields = new SortField[orderProps.length];
            for (int i = 0; i < orderProps.length; i++) {
                String prop = null;
                if (QueryConstants.JCR_SCORE.equals(orderProps[i])) {
                    // order on jcr:score does not use the natural order as
                    // implemented in lucene. score ascending in lucene means that
                    // higher scores are first. JCR specs that lower score values
                    // are first.
                    sortFields[i] = new SortField(null, SortField.SCORE, orderSpecs[i]);
                } else {
                    try {
                        prop = orderProps[i].toJCRName(nsMappings);
                    } catch (NoPrefixDeclaredException e) {
                        // will never happen
                    }
                    sortFields[i] = new SortField(prop, SortField.STRING, !orderSpecs[i]);
                }
            }

            MultiReader multiReader = new MultiReader(new IndexReader[]{ persistentIndex.getIndexReader(), volatileIndex.getIndexReader()});
            if (sortFields.length > 0) {
                hits = new IndexSearcher(multiReader).search(query, new Sort(sortFields));
            } else {
                hits = new IndexSearcher(multiReader).search(query);
            }
        } finally {
            readWriteLock.readLock().release();
        }

        return hits;
    }

    /**
     * Returns the analyzer in use for indexing.
     * @return the analyzer in use for indexing.
     */
    Analyzer getAnalyzer() {
        return analyzer;
    }

    /**
     * Returns the namespace mappings for the internal representation.
     * @return the namespace mappings for the internal representation.
     */
    NamespaceMappings getNamespaceMappings() {
        return nsMappings;
    }

    //---------------------------< internal >-----------------------------------

    /**
     * Recursively creates an index starting with the NodeState <code>node</code>.
     * @param node the current NodeState.
     * @throws IOException if an error occurs while writing to the index.
     * @throws ItemStateException if an node state cannot be found.
     * @throws RepositoryException if any other error occurs
     */
    private void createIndex(NodeState node)
            throws IOException, ItemStateException, RepositoryException {
        addNodePersistent(node);
        List children = node.getChildNodeEntries();
        ItemStateManager isMgr = getItemStateProvider();
        for (Iterator it = children.iterator(); it.hasNext();) {
            NodeState.ChildNodeEntry child = (NodeState.ChildNodeEntry) it.next();
            createIndex((NodeState) isMgr.getItemState(new NodeId(child.getUUID())));
        }
    }

    /**
     * Adds a node to the persistent index. This method will <b>not</b> aquire a
     * write lock while writing!
     * @param node the node to add.
     * @throws IOException if an error occurs while writing to the index.
     * @throws RepositoryException if any other error occurs
     */
    private void addNodePersistent(NodeState node)
            throws IOException, RepositoryException {
        Document doc = NodeIndexer.createDocument(node, getItemStateProvider(), nsMappings);
        persistentIndex.addDocument(doc);
    }

    /**
     * Removes a node from the persistent index. This method will <b>not</b>
     * aquire a write lock while writing!
     * @param uuid the uuid of the node to remove.
     * @throws IOException if an error occurs while writing to the index.
     */
    private void deleteNodePersistent(String uuid) throws IOException {
        Term idTerm = new Term(FieldNames.UUID, uuid);
        persistentIndex.removeDocument(idTerm);
    }

    //--------------------------< properties >----------------------------------

    public void setUseCompoundFile(boolean b) {
        persistentIndex.setUseCompoundFile(b);
    }

    public void setMinMergeDocs(int minMergeDocs) {
        persistentIndex.setMinMergeDocs(minMergeDocs);
    }

    public void setMaxMergeDocs(int maxMergeDocs) {
        persistentIndex.setMaxMergeDocs(maxMergeDocs);
    }

    public void setMergeFactor(int mergeFactor) {
        persistentIndex.setMergeFactor(mergeFactor);
    }

    public void setRedoSize(int size) {
        mergeSize = size;
    }
}
