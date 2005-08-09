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

import EDU.oswego.cs.dl.util.concurrent.FIFOReadWriteLock;
import org.apache.jackrabbit.Constants;
import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.query.AbstractQueryHandler;
import org.apache.jackrabbit.core.query.ExecutableQuery;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.QName;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Implements a {@link org.apache.jackrabbit.core.query.QueryHandler} using
 * Lucene.
 */
public class SearchIndex extends AbstractQueryHandler {

    /** The logger instance for this class */
    private static final Logger log = Logger.getLogger(SearchIndex.class);

    /**
     * The actual index
     */
    private MultiIndex index;

    /**
     * The analyzer we use for indexing.
     */
    private final Analyzer analyzer;

    /**
     * Read-write lock to synchronize access on the index.
     */
    private final FIFOReadWriteLock readWriteLock = new FIFOReadWriteLock();

    /**
     * minMergeDocs config parameter.
     */
    private int minMergeDocs = 1000;

    /**
     * volatileIdleTime config parameter.
     */
    private int volatileIdleTime = 3;

    /**
     * maxMergeDocs config parameter
     */
    private int maxMergeDocs = 100000;

    /**
     * mergeFactor config parameter
     */
    private int mergeFactor = 10;

    /**
     * Number of documents that are buffered before they are added to the index.
     */
    private int bufferSize = 10;

    /**
     * Compound file flag
     */
    private boolean useCompoundFile = true;

    /**
     * Flag indicating whether document order is enable as the default ordering.
     */
    private boolean documentOrder = true;

    /**
     * If set <code>true</code> the index is checked for consistency on startup.
     * If <code>false</code> a consistency check is only performed when there
     * are entries in the redo log on startup.
     * <p/>
     * Default value is: <code>false</code>.
     */
    private boolean forceConsistencyCheck = false;

    /**
     * If set <code>true</code> errors detected by the consistency check are
     * repaired. If <code>false</code> the errors are only reported in the log.
     * <p/>
     * Default value is: <code>true</code>.
     */
    private boolean autoRepair = true;

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
            QueryHandlerContext context = getContext();
            index = new MultiIndex(context.getFileSystem(), this,
                    context.getItemStateManager(), context.getRootUUID());
            if (index.getRedoLogApplied() || forceConsistencyCheck) {
                log.info("Running consistency check...");
                try {
                    ConsistencyCheck check = ConsistencyCheck.run(index,
                            context.getItemStateManager());
                    if (autoRepair) {
                        check.repair(true);
                    } else {
                        List errors = check.getErrors();
                        if (errors.size() == 0) {
                            log.info("No errors detected.");
                        }
                        for (Iterator it = errors.iterator(); it.hasNext(); ) {
                            ConsistencyCheckError err = (ConsistencyCheckError) it.next();
                            log.info(err.toString());
                        }
                    }
                } catch (IOException e) {
                    log.warn("Failed to run consistency check on index: " + e);
                }
            }
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Adds the <code>node</code> to the search index.
     * @param node the node to add.
     * @throws RepositoryException if an error occurs while indexing the node.
     * @throws IOException if an error occurs while adding the node to the index.
     */
    public void addNode(NodeState node) throws RepositoryException, IOException {
        Document doc = createDocument(node, getNamespaceMappings());
        try {
            readWriteLock.writeLock().acquire();
        } catch (InterruptedException e) {
            throw new RepositoryException("Failed to aquire write lock.");
        }

        try {
            index.addDocument(doc);
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
            index.removeDocument(idTerm);
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
        QueryImpl query = new QueryImpl(session, itemMgr, this,
                getContext().getPropertyTypeRegistry(), statement, language);
        query.setRespectDocumentOrder(documentOrder);
        return query;
    }

    /**
     * Closes this <code>QueryHandler</code> and frees resources attached
     * to this handler.
     */
    public void close() {
        log.info("Closing search index.");
        index.close();
        getContext().destroy();
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
                if (Constants.JCR_SCORE.equals(orderProps[i])) {
                    // order on jcr:score does not use the natural order as
                    // implemented in lucene. score ascending in lucene means that
                    // higher scores are first. JCR specs that lower score values
                    // are first.
                    sortFields[i] = new SortField(null, SortField.SCORE, orderSpecs[i]);
                } else {
                    try {
                        prop = orderProps[i].toJCRName(getNamespaceMappings());
                    } catch (NoPrefixDeclaredException e) {
                        // will never happen
                    }
                    sortFields[i] = new SortField(prop, SharedFieldSortComparator.PROPERTIES, !orderSpecs[i]);
                }
            }

            if (sortFields.length > 0) {
                hits = new IndexSearcher(index.getIndexReader()).search(query, new Sort(sortFields));
            } else {
                hits = new IndexSearcher(index.getIndexReader()).search(query);
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
        return index.getNamespaceMappings();
    }

    /**
     * Creates a lucene <code>Document</code> from a node state using the
     * namespace mappings <code>nsMappings</code>.
     * @param node the node state to index.
     * @param nsMappings the namespace mappings of the search index.
     * @return a lucene <code>Document</code> that contains all properties
     *  of <code>node</code>.
     * @throws RepositoryException if an error occurs while indexing the
     *  <code>node</code>.
     */
    protected Document createDocument(NodeState node, NamespaceMappings nsMappings)
            throws RepositoryException {
        return NodeIndexer.createDocument(node, getContext().getItemStateManager(),
                nsMappings);
    }

    //--------------------------< properties >----------------------------------

    /**
     * The lucene index writer property: useCompoundFile
     */
    public void setUseCompoundFile(boolean b) {
        useCompoundFile = b;
    }

    /**
     * Returns the current value for useCompoundFile.
     *
     * @return the current value for useCompoundFile.
     */
    public boolean getUseCompoundFile() {
        return useCompoundFile;
    }

    /**
     * The lucene index writer property: minMergeDocs
     */
    public void setMinMergeDocs(int minMergeDocs) {
        this.minMergeDocs = minMergeDocs;
    }

    /**
     * Returns the current value for minMergeDocs.
     *
     * @return the current value for minMergeDocs.
     */
    public int getMinMergeDocs() {
        return minMergeDocs;
    }

    /**
     * Sets the property: volatileIdleTime
     *
     * @param volatileIdleTime idle time in seconds
     */
    public void setVolatileIdleTime(int volatileIdleTime) {
        this.volatileIdleTime = volatileIdleTime;
    }

    /**
     * Returns the current value for volatileIdleTime.
     *
     * @return the current value for volatileIdleTime.
     */
    public int getVolatileIdleTime() {
        return volatileIdleTime;
    }

    /**
     * The lucene index writer property: maxMergeDocs
     */
    public void setMaxMergeDocs(int maxMergeDocs) {
        this.maxMergeDocs = maxMergeDocs;
    }

    /**
     * Returns the current value for maxMergeDocs.
     *
     * @return the current value for maxMergeDocs.
     */
    public int getMaxMergeDocs() {
        return maxMergeDocs;
    }

    /**
     * The lucene index writer property: mergeFactor
     */
    public void setMergeFactor(int mergeFactor) {
        this.mergeFactor = mergeFactor;
    }

    /**
     * Returns the current value for the merge factor.
     *
     * @return the current value for the merge factor.
     */
    public int getMergeFactor() {
        return mergeFactor;
    }

    /**
     * @see VolatileIndex#setBufferSize(int)
     */
    public void setBufferSize(int size) {
        bufferSize = size;
    }

    /**
     * Returns the current value for the buffer size.
     *
     * @return the current value for the buffer size.
     */
    public int getBufferSize() {
        return bufferSize;
    }

    public void setRespectDocumentOrder(boolean docOrder) {
        documentOrder = docOrder;
    }

    public boolean getRespectDocumentOrder() {
        return documentOrder;
    }

    public void setForceConsistencyCheck(boolean b) {
        forceConsistencyCheck = b;
    }

    public boolean getForceConsistencyCheck() {
        return forceConsistencyCheck;
    }

    public void setAutoRepair(boolean b) {
        autoRepair = b;
    }

    public boolean getAutoRepair() {
        return autoRepair;
    }
}
