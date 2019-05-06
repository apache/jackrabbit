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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.RowIterator;

import org.apache.jackrabbit.api.query.JackrabbitQueryResult;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.query.qom.ColumnImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the <code>QueryResult</code> interface.
 */
public abstract class QueryResultImpl implements JackrabbitQueryResult {

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(QueryResultImpl.class);

    /**
     * The search index to execute the query.
     */
    protected final SearchIndex index;

    /**
     * Component context of the current session
     */
    protected final SessionContext sessionContext;

    /**
     * The query instance which created this query result.
     */
    protected final AbstractQueryImpl queryImpl;

    /**
     * The spell suggestion or <code>null</code> if not available.
     */
    protected final SpellSuggestion spellSuggestion;

    /**
     * The columns to select.
     */
    protected final Map<String, ColumnImpl> columns = new LinkedHashMap<String, ColumnImpl>();

    /**
     * The result nodes including their score. This list is populated on a lazy
     * basis while a client iterates through the results.
     * <p>
     * The exact type is: <code>List&lt;ScoreNode[]></code>
     */
    private final List<ScoreNode[]> resultNodes = new ArrayList<ScoreNode[]>();

    /**
     * This is the raw number of results that matched the query, ignoring limit and offset. Only set when accurate.
     */
    private int totalResults = -1;

    /**
     * This is the number of results that matched the query, with limit and offset. Only set when accurate.
     */
    private int numResults = -1;

    /**
     * The selector names associated with the score nodes. The selector names
     * are set when the query is executed via {@link #getResults(long)}.
     */
    private Name[] selectorNames;

    /**
     * The number of results that are invalid, either because a node does not
     * exist anymore or because the session does not have access to the node.
     */
    private int invalid = 0;

    /**
     * If <code>true</code> nodes are returned in document order.
     */
    protected final boolean docOrder;

    /**
     * The excerpt provider or <code>null</code> if none was created yet.
     */
    private ExcerptProvider excerptProvider;

    /**
     * The offset in the total result set
     */
    private final long offset;

    /**
     * The maximum size of this result if limit >= 0
     */
    private final long limit;
    
    private final boolean sizeEstimate;

    /**
     * Creates a new query result. The concrete sub class is responsible for
     * calling {@link #getResults(long)} after this constructor had been called.
     *
     * @param index           the search index where the query is executed.
     * @param sessionContext component context of the current session
     * @param queryImpl       the query instance which created this query
     *                        result.
     * @param spellSuggestion the spell suggestion or <code>null</code> if none
     *                        is available.
     * @param columns         the select properties of the query.
     * @param documentOrder   if <code>true</code> the result is returned in
     *                        document order.
     * @param limit           the maximum result size
     * @param offset          the offset in the total result set
     * @throws RepositoryException if an error occurs while reading from the
     *                             repository.
     * @throws IllegalArgumentException if any of the columns does not have a
     *                                  column name.
     */
    public QueryResultImpl(
            SearchIndex index, SessionContext sessionContext,
            AbstractQueryImpl queryImpl, SpellSuggestion spellSuggestion,
            ColumnImpl[] columns, boolean documentOrder,
            long offset, long limit) throws RepositoryException {
        this.index = index;
        this.sizeEstimate = index.getSizeEstimate();
        this.sessionContext = sessionContext;
        this.queryImpl = queryImpl;
        this.spellSuggestion = spellSuggestion;
        this.docOrder = documentOrder;
        this.offset = offset;
        this.limit = limit;
        for (ColumnImpl column : columns) {
            String cn = column.getColumnName();
            if (cn == null) {
                String msg = column + " does not have a column name";
                throw new IllegalArgumentException(msg);
            }
            this.columns.put(cn, column);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getSelectorNames() throws RepositoryException {
        String[] names = new String[selectorNames.length];
        for (int i = 0; i < selectorNames.length; i++) {
            names[i] = sessionContext.getJCRName(selectorNames[i]);
        }
        return names;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getColumnNames() throws RepositoryException {
        return columns.keySet().toArray(new String[columns.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public NodeIterator getNodes() throws RepositoryException {
        return new NodeIteratorImpl(sessionContext, getScoreNodes(), 0);
    }

    /**
     * {@inheritDoc}
     */
    public RowIterator getRows() throws RepositoryException {
        if (excerptProvider == null) {
            try {
                excerptProvider = createExcerptProvider();
            } catch (IOException e) {
                throw new RepositoryException(e);
            }
        }
        return new RowIteratorImpl(
                getScoreNodes(), columns,
                selectorNames, sessionContext.getItemManager(),
                index.getContext().getHierarchyManager(),
                sessionContext, 
                sessionContext.getSessionImpl().getValueFactory(),
                excerptProvider, spellSuggestion);
    }

    /**
     * Executes the query for this result and returns hits. The caller must
     * close the query hits when he is done using it.
     *
     * @param resultFetchHint a hint on how many results should be fetched.
     * @return hits for this query result.
     * @throws IOException if an error occurs while executing the query.
     */
    protected abstract MultiColumnQueryHits executeQuery(long resultFetchHint)
            throws IOException;

    /**
     * Creates an excerpt provider for this result set.
     *
     * @return an excerpt provider.
     * @throws IOException if an error occurs.
     */
    protected abstract ExcerptProvider createExcerptProvider()
            throws IOException;

    //--------------------------------< internal >------------------------------

    /**
     * Creates a {@link ScoreNodeIterator} over the query result.
     *
     * @return a {@link ScoreNodeIterator} over the query result.
     */
    private ScoreNodeIterator getScoreNodes() {
        if (docOrder) {
            return new DocOrderScoreNodeIterator(
                    sessionContext.getItemManager(), resultNodes, 0);
        } else {
            return new LazyScoreNodeIteratorImpl();
        }
    }

    /**
     * Attempts to get <code>size</code> results and puts them into {@link
     * #resultNodes}. If the size of {@link #resultNodes} is less than
     * <code>size</code> then there are no more than <code>resultNodes.size()</code>
     * results for this query.
     *
     * @param size the number of results to fetch for the query.
     * @throws RepositoryException if an error occurs while executing the
     *                             query.
     */
    protected void getResults(long size) throws RepositoryException {
        if (log.isDebugEnabled()) {
            log.debug("getResults({}) limit={}", size, limit);
        }
        
        if (!sizeEstimate) {
            // quick check
            // if numResults is set, all relevant results have been fetched
            if (numResults != -1) {
                return;
            }
        }

        long maxResultSize = size;

        // is there any limit?
        if (limit >= 0) {
            maxResultSize = limit;
        }

        if (resultNodes.size() >= maxResultSize && selectorNames != null) {
            // we already have them all
            return;
        }

        // execute it
        MultiColumnQueryHits result = null;
        try {
            long time = System.currentTimeMillis();
            long r1 = IOCounters.getReads();
            result = executeQuery(maxResultSize);
            long r2 = IOCounters.getReads();
            log.debug("query executed in {} ms ({})",
                    System.currentTimeMillis() - time, r2 - r1);
            // set selector names
            selectorNames = result.getSelectorNames();

            List<ScoreNode[]> offsetNodes = new ArrayList<ScoreNode[]>();
            if (resultNodes.isEmpty() && offset > 0) {
                // collect result offset into dummy list
                if (sizeEstimate) {
                    collectScoreNodes(result, new ArrayList<ScoreNode[]>(), offset);                    
                } else {
                    collectScoreNodes(result, offsetNodes, offset);
                }
            } else {
                int start = resultNodes.size() + invalid + (int) offset;
                result.skip(start);
            }

            time = System.currentTimeMillis();
            collectScoreNodes(result, resultNodes, maxResultSize);
            long r3 = IOCounters.getReads();
            log.debug("retrieved ScoreNodes in {} ms ({})",
                    System.currentTimeMillis() - time, r3 - r2);

            if (sizeEstimate) {
                // update numResults
                numResults = result.getSize();                
            } else {
                // update numResults if all results have been fetched 
                // if resultNodes.getSize() is strictly smaller than maxResultSize, it means that all results have been fetched
                int resultSize = resultNodes.size();
                if (resultSize < maxResultSize) {
                    if (resultNodes.isEmpty()) {
                        // if there's no result nodes, the actual totalResults if smaller or equals than the offset
                        totalResults = offsetNodes.size();
                        numResults = 0;
                    }
                    else {
                        totalResults = resultSize + (int) offset;
                        numResults = resultSize;
                    }
                }
                else if (resultSize == limit) {
                    // if there's "limit" results, we can't know the total size (which may be greater), but the result size is the limit
                    numResults = (int) limit;
                }
            }
        } catch (IOException e) {
            throw new RepositoryException(e);
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (IOException e) {
                    log.warn("Unable to close query result: " + e);
                }
            }
        }
    }

    /**
     * Collect score nodes from <code>hits</code> into the <code>collector</code>
     * list until the size of <code>collector</code> reaches <code>maxResults</code>
     * or there are not more results.
     *
     * @param hits the raw hits.
     * @param collector where the access checked score nodes are collected.
     * @param maxResults the maximum number of results in the collector.
     * @throws IOException if an error occurs while reading from hits.
     * @throws RepositoryException if an error occurs while checking access rights.
     */
    private void collectScoreNodes(MultiColumnQueryHits hits,
                                   List<ScoreNode[]> collector,
                                   long maxResults)
            throws IOException, RepositoryException {
        while (collector.size() < maxResults) {
            ScoreNode[] sn = hits.nextScoreNodes();
            if (sn == null) {
                // no more results
                break;
            }
            // check access
            if (isAccessGranted(sn)) {
                collector.add(sn);
            } else {
                invalid++;
            }
        }
    }

    /**
     * Checks if access is granted to all <code>nodes</code>.
     *
     * @param nodes the nodes to check.
     * @return <code>true</code> if read access is granted to all
     *         <code>nodes</code>.
     * @throws RepositoryException if an error occurs while checking access
     *                             rights.
     */
    protected boolean isAccessGranted(ScoreNode[] nodes)
            throws RepositoryException {
        for (ScoreNode node : nodes) {
            try {
                if (node != null && !sessionContext.getAccessManager().canRead(
                        null, node.getNodeId())) {
                    return false;
                }
            } catch (ItemNotFoundException e) {
                // node deleted while query was executed
            }
        }
        return true;
    }

    /**
     * Returns the total number of hits. This is the number of results you
     * will get get if you don't set any limit or offset. This method may return
     * <code>-1</code> if the total size is unknown.
     * <p>
     * If the "sizeEstimate" options is enabled:
     * Keep in mind that this number may get smaller if nodes are found in
     * the result set which the current session has no permission to access.
     * This might be a security problem.
     *
     * @return the total number of hits.
     */
    public int getTotalSize() {
        if (sizeEstimate) {
            if (numResults == -1) {
                return -1;
            } else {
                return numResults - invalid;
            }
        } else {
            return totalResults;
        }
    }

    private final class LazyScoreNodeIteratorImpl implements ScoreNodeIterator {

        private int position = -1;

        private boolean initialized = false;

        private ScoreNode[] next;

        public ScoreNode[] nextScoreNodes() {
            initialize();
            if (next == null) {
                throw new NoSuchElementException();
            }
            ScoreNode[] sn = next;
            fetchNext();
            return sn;
        }

        /**
         * {@inheritDoc}
         */
        public void skip(long skipNum) {
            initialize();
            if (skipNum < 0) {
                throw new IllegalArgumentException("skipNum must not be negative");
            }
            if (skipNum == 0) {
                // do nothing
            } else {
                // attempt to get enough results
                try {
                    getResults(position + invalid + (int) skipNum);
                    if (resultNodes.size() >= position + skipNum) {
                        // skip within already fetched results
                        position += skipNum - 1;
                        fetchNext();
                    } else {
                        // not enough results after getResults()
                        throw new NoSuchElementException();
                    }
                } catch (RepositoryException e) {
                    throw new NoSuchElementException(e.getMessage());
                }
            }
        }

        /**
         * {@inheritDoc}
         * <p>
         * If the "sizeEstimate" options is enabled:
         * This value may shrink when the query result encounters non-existing
         * nodes or the session does not have access to a node.
         */
        public long getSize() {
            if (sizeEstimate) {
                int total = getTotalSize();
                if (total == -1) {
                    return -1;
                }
                long size = offset > total ? 0 : total - offset;
                if (limit >= 0 && size > limit) {
                    return limit;
                } else {
                    return size;
                }                
            } else {
                return numResults;
            }
        }

        /**
         * {@inheritDoc}
         */
        public long getPosition() {
            initialize();
            return position;
        }

        /**
         * @throws UnsupportedOperationException always.
         */
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            initialize();
            return next != null;
        }

        /**
         * {@inheritDoc}
         */
        public Object next() {
            return nextScoreNodes();
        }

        /**
         * Initializes this iterator but only if it is not yet initialized.
         */
        private void initialize() {
            if (!initialized) {
                fetchNext();
                initialized = true;
            }
        }

        /**
         * Fetches the next node to return by this iterator. If this method
         * returns and {@link #next} is <code>null</code> then there is no next
         * node.
         */
        private void fetchNext() {
            next = null;
            int nextPos = position + 1;
            while (next == null) {
                if (nextPos >= resultNodes.size()) {
                    // quick check if there are more results at all
                    if (sizeEstimate) {
                        // this check is only possible if we have numResults
                        if (numResults != -1 && (nextPos + invalid) >= numResults) {
                            break;
                        }
                    } else {
                        // if numResults is set, all relevant results have been fetched
                        if (numResults != -1) {
                            break;
                        }
                    }

                    // fetch more results
                    try {
                        int num;
                        if (resultNodes.size() == 0) {
                            num = index.getResultFetchSize();
                        } else {
                            num = resultNodes.size() * 2;
                        }
                        getResults(num);
                    } catch (RepositoryException e) {
                        log.warn("Exception getting more results: " + e);
                    }
                    // check again
                    if (nextPos >= resultNodes.size()) {
                        // no more valid results
                        break;
                    }
                }
                next = resultNodes.get(nextPos);
            }
            position++;
        }
    }
}
