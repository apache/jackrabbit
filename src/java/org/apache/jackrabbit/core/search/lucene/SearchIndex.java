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
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.Sort;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.RepositoryException;
import java.io.IOException;

/**
 * Implements a {@link org.apache.jackrabbit.core.search.QueryHandler} using
 * Lucene.
 */
public class SearchIndex extends AbstractQueryHandler {

    private static final Logger log = Logger.getLogger(SearchIndex.class);

    /** Name of the write lock file */
    private static final String WRITE_LOCK = "write.lock";

    /** Name of the file to persist search internal namespace mappings */
    private static final String NS_MAPPING_FILE = "ns_mappings.properties";

    /**
     * 512k default size
     */
    //private static final long DEFAULT_MERGE_SIZE = 512 * 1024;

    //private long mergeSize = DEFAULT_MERGE_SIZE;

    private PersistentIndex persistentIndex;

    //private VolatileIndex volatileIndex;

    private final Analyzer analyzer;

    private NamespaceMappings nsMappings;

    private final FIFOReadWriteLock readWriteLock = new FIFOReadWriteLock();

    /**
     * Default constructor.
     */
    public SearchIndex() {
        this.analyzer = new StandardAnalyzer();
        //volatileIndex = new VolatileIndex(analyzer);
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
                addNode(rootState);
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
            // FIXME: ??? do logging, simply return?
            return;
        }

        try {
            persistentIndex.addDocument(doc);
        } finally {
            readWriteLock.writeLock().release();
        }

        /*
        volatileIndex.addDocument(doc);
        if (volatileIndex.size() > mergeSize) {
            persistentIndex.mergeIndex(volatileIndex);
            // create new volatile index
            volatileIndex = new VolatileIndex(analyzer);
        }
        */
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
            // FIXME: ??? do logging, simply return?
            return;
        }

        try {
            persistentIndex.removeDocument(idTerm);
        } finally {
            readWriteLock.writeLock().release();
        }

        //volatileIndex.removeDocument(idTerm);
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
        return new QueryImpl(session, itemMgr, this, statement, language);
    }

    /**
     * Closes this <code>QueryHandler</code> and frees resources attached
     * to this handler.
     */
    public void close() {
        /*
        try {
            persistentIndex.mergeIndex(volatileIndex);
        } catch (IOException e) {
            // FIXME do logging
        }
        volatileIndex.close();
        */
        log.info("Closing search index.");
        persistentIndex.close();
    }

    Hits executeQuery(Query query,
                             QName[] orderProps,
                             boolean[] orderSpecs) throws IOException {
        try {
            readWriteLock.readLock().acquire();
        } catch (InterruptedException e) {
            throw new IOException("Unable to obtain read lock on search index.");
        }

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

        Hits hits = null;
        try {
            if (sortFields.length > 0) {
                hits = persistentIndex.getIndexSearcher().search(query,
                        new Sort(sortFields));
            } else {
                hits = persistentIndex.getIndexSearcher().search(query);
            }
        } finally {
            readWriteLock.readLock().release();
        }

        return hits;
    }

    Analyzer getAnalyzer() {
        return analyzer;
    }

    NamespaceMappings getNamespaceMappings() {
        return nsMappings;
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
}
