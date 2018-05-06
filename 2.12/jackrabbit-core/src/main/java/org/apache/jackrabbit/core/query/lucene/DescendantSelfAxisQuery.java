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

import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.Weight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Implements a lucene <code>Query</code> which filters a sub query by checking
 * whether the nodes selected by that sub query are descendants or self of
 * nodes selected by a context query.
 */
@SuppressWarnings("serial")
class DescendantSelfAxisQuery extends Query implements JackrabbitQuery {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(DescendantSelfAxisQuery.class);

    /**
     * The context query
     */
    private final Query contextQuery;

    /**
     * The scorer of the context query
     */
    private Scorer contextScorer;

    /**
     * The sub query to filter
     */
    private final Query subQuery;

    /**
     * The minimal levels required between context and sub nodes for a sub node
     * to match.
     */
    private final int minLevels;

    /**
     * The scorer of the sub query to filter
     */
    private Scorer subScorer;

    /**
     * Creates a new <code>DescendantSelfAxisQuery</code> based on a
     * <code>context</code> and matches all descendants of the context nodes.
     * Whether the context nodes match as well is controlled by
     * <code>includeSelf</code>.
     *
     * @param context     the context for this query.
     * @param includeSelf if <code>true</code> this query acts like a
     *                    descendant-or-self axis. If <code>false</code> this
     *                    query acts like a descendant axis.
     */
    public DescendantSelfAxisQuery(Query context, boolean includeSelf) {
        this(context, new MatchAllDocsQuery(), includeSelf);
    }

    /**
     * Creates a new <code>DescendantSelfAxisQuery</code> based on a
     * <code>context</code> query and filtering the <code>sub</code> query.
     *
     * @param context the context for this query.
     * @param sub     the sub query.
     */
    public DescendantSelfAxisQuery(Query context, Query sub) {
        this(context, sub, true);
    }

    /**
     * Creates a new <code>DescendantSelfAxisQuery</code> based on a
     * <code>context</code> query and filtering the <code>sub</code> query.
     *
     * @param context     the context for this query.
     * @param sub         the sub query.
     * @param includeSelf if <code>true</code> this query acts like a
     *                    descendant-or-self axis. If <code>false</code> this query acts like
     *                    a descendant axis.
     */
    public DescendantSelfAxisQuery(Query context, Query sub, boolean includeSelf) {
        this(context, sub, includeSelf ? 0 : 1);
    }

    /**
     * Creates a new <code>DescendantSelfAxisQuery</code> based on a
     * <code>context</code> query and filtering the <code>sub</code> query.
     *
     * @param context   the context for this query.
     * @param sub       the sub query.
     * @param minLevels the minimal levels required between context and sub
     *                  nodes for a sub node to match.
     */
    public DescendantSelfAxisQuery(Query context, Query sub, int minLevels) {
        this.contextQuery = context;
        this.subQuery = sub;
        this.minLevels = minLevels;
    }

    /**
     * @return the context query of this <code>DescendantSelfAxisQuery</code>.
     */
    Query getContextQuery() {
        return contextQuery;
    }

    /**
     * @return <code>true</code> if the sub query of this <code>DescendantSelfAxisQuery</code>
     *         matches all nodes.
     */
    boolean subQueryMatchesAll() {
        return subQuery instanceof MatchAllDocsQuery;
    }

    /**
     * Returns the minimal levels required between context and sub nodes for a
     * sub node to match.
     * <ul>
     * <li><code>0</code>: a sub node <code>S</code> matches if it is a context
     * node or one of the ancestors of <code>S</code> is a context node.</li>
     * <li><code>1</code>: a sub node <code>S</code> matches if one of the
     * ancestors of <code>S</code> is a context node.</li>
     * <li><code>n</code>: a sub node <code>S</code> matches if
     * <code>S.getAncestor(S.getDepth() - n)</code> is a context node.</li>
     * </ul>
     *
     * @return the minimal levels required between context and sub nodes for a
     *         sub node to match.
     */
    int getMinLevels() {
        return minLevels;
    }

    /**
     * Creates a <code>Weight</code> instance for this query.
     *
     * @param searcher the <code>Searcher</code> instance to use.
     * @return a <code>DescendantSelfAxisWeight</code>.
     */
    public Weight createWeight(Searcher searcher) {
        return new DescendantSelfAxisWeight(searcher);
    }

    /**
     * {@inheritDoc}
     */
    public String toString(String field) {
        StringBuffer sb = new StringBuffer();
        sb.append("DescendantSelfAxisQuery(");
        sb.append(contextQuery);
        sb.append(", ");
        sb.append(subQuery);
        sb.append(", ");
        sb.append(minLevels);
        sb.append(")");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public void extractTerms(Set<Term> terms) {
        contextQuery.extractTerms(terms);
        subQuery.extractTerms(terms);
    }

    /**
     * {@inheritDoc}
     */
    public Query rewrite(IndexReader reader) throws IOException {
        Query cQuery = contextQuery.rewrite(reader);
        Query sQuery = subQuery.rewrite(reader);
        if (contextQuery instanceof DescendantSelfAxisQuery) {
            DescendantSelfAxisQuery dsaq = (DescendantSelfAxisQuery) contextQuery;
            if (dsaq.subQueryMatchesAll()) {
                return new DescendantSelfAxisQuery(dsaq.getContextQuery(),
                        sQuery, dsaq.getMinLevels() + getMinLevels()).rewrite(reader);
            }
        }
        if (cQuery == contextQuery && sQuery == subQuery) {
            return this;
        } else {
            return new DescendantSelfAxisQuery(cQuery, sQuery, minLevels);
        }
    }

    //------------------------< JackrabbitQuery >-------------------------------

    /**
     * {@inheritDoc}
     */
    public QueryHits execute(final JackrabbitIndexSearcher searcher,
                             final SessionImpl session,
                             final Sort sort) throws IOException {
        if (sort.getSort().length == 0 && subQueryMatchesAll()) {
            // maps path String to ScoreNode
            Map<String, ScoreNode> startingPoints = new TreeMap<String, ScoreNode>();
            QueryHits result = searcher.evaluate(getContextQuery());
            try {
                // minLevels 0 and 1 are handled with a series of
                // NodeTraversingQueryHits directly on result. For minLevels >= 2
                // intermediate ChildNodesQueryHits are required.
                for (int i = 2; i <= getMinLevels(); i++) {
                    result = new ChildNodesQueryHits(result, session);
                }

                ScoreNode sn;
                while ((sn = result.nextScoreNode()) != null) {
                    NodeId id = sn.getNodeId();
                    try {
                        Node node = session.getNodeById(id);
                        startingPoints.put(node.getPath(), sn);
                    } catch (ItemNotFoundException e) {
                        // JCR-3001 access denied to score node, will just skip it
                        log.warn("Access denied to node id {}.", id);
                    } catch (RepositoryException e) {
                        throw Util.createIOException(e);
                    }
                }
            } finally {
                result.close();
            }

            // prune overlapping starting points
            String previousPath = null;
            for (Iterator<String> it = startingPoints.keySet().iterator(); it.hasNext(); ) {
                String path = it.next();
                // if the previous path is a prefix of this path then the
                // current path is obsolete
                if (previousPath != null && path.startsWith(previousPath)) {
                    it.remove();
                } else {
                    previousPath = path;
                }
            }

            final Iterator<ScoreNode> scoreNodes = startingPoints.values().iterator();
            return new AbstractQueryHits() {

                private NodeTraversingQueryHits currentTraversal;

                {
                    fetchNextTraversal();
                }

                public void close() throws IOException {
                    if (currentTraversal != null) {
                        currentTraversal.close();
                    }
                }

                public ScoreNode nextScoreNode() throws IOException {
                    while (currentTraversal != null) {
                        ScoreNode sn = currentTraversal.nextScoreNode();
                        if (sn != null) {
                            return sn;
                        } else {
                            fetchNextTraversal();
                        }
                    }
                    // if we get here there are no more score nodes
                    return null;
                }

                private void fetchNextTraversal() throws IOException {
                    if (currentTraversal != null) {
                        currentTraversal.close();
                    }
                    currentTraversal = null;
                    // We only need one node, but because of the acls, we'll
                    // iterate until we find a good one
                    while (scoreNodes.hasNext()) {
                        ScoreNode sn = scoreNodes.next();
                        NodeId id = sn.getNodeId();
                        try {
                            Node node = session.getNodeById(id);
                            currentTraversal = new NodeTraversingQueryHits(
                                    node, getMinLevels() == 0);
                            break;
                        } catch (ItemNotFoundException e) {
                            // JCR-3001 node access denied, will just skip it
                            log.warn("Access denied to node id {}.", id);
                        } catch (RepositoryException e) {
                            throw Util.createIOException(e);
                        }
                    }
                }
            };
        } else {
            return null;
        }
    }

    //--------------------< DescendantSelfAxisWeight >--------------------------

    /**
     * The <code>Weight</code> implementation for this
     * <code>DescendantSelfAxisWeight</code>.
     */
    private class DescendantSelfAxisWeight extends Weight {

        /**
         * The searcher in use
         */
        private final Searcher searcher;

        /**
         * Creates a new <code>DescendantSelfAxisWeight</code> instance using
         * <code>searcher</code>.
         *
         * @param searcher a <code>Searcher</code> instance.
         */
        private DescendantSelfAxisWeight(Searcher searcher) {
            this.searcher = searcher;
        }

        //-----------------------------< Weight >-------------------------------

        /**
         * Returns this <code>DescendantSelfAxisQuery</code>.
         *
         * @return this <code>DescendantSelfAxisQuery</code>.
         */
        public Query getQuery() {
            return DescendantSelfAxisQuery.this;
        }

        /**
         * {@inheritDoc}
         */
        public float getValue() {
            return 1.0f;
        }

        /**
         * {@inheritDoc}
         */
        public float sumOfSquaredWeights() throws IOException {
            return 1.0f;
        }

        /**
         * {@inheritDoc}
         */
        public void normalize(float norm) {
        }

        /**
         * Creates a scorer for this <code>DescendantSelfAxisScorer</code>.
         *
         * @param reader a reader for accessing the index.
         * @return a <code>DescendantSelfAxisScorer</code>.
         * @throws IOException if an error occurs while reading from the index.
         */
        public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
                boolean topScorer) throws IOException {
            contextScorer = searcher.createNormalizedWeight(contextQuery).scorer(reader, scoreDocsInOrder, false);
            subScorer = searcher.createNormalizedWeight(subQuery).scorer(reader, scoreDocsInOrder, false);
            HierarchyResolver resolver = (HierarchyResolver) reader;
            return new DescendantSelfAxisScorer(searcher.getSimilarity(), reader, resolver);
        }

        /**
         * {@inheritDoc}
         */
        public Explanation explain(IndexReader reader, int doc) throws IOException {
            return new Explanation();
        }
    }

    //----------------------< DescendantSelfAxisScorer >---------------------------------
    /**
     * Implements a <code>Scorer</code> for this
     * <code>DescendantSelfAxisQuery</code>.
     */
    private class DescendantSelfAxisScorer extends Scorer {

        /**
         * The <code>HierarchyResolver</code> of the index.
         */
        private final HierarchyResolver hResolver;

        /**
         * BitSet storing the id's of selected documents
         */
        private final BitSet contextHits;

        /**
         * Set <code>true</code> once the context hits have been calculated.
         */
        private boolean contextHitsCalculated = false;

        /**
         * Remember document numbers of ancestors during validation
         */
        private int[] ancestorDocs = new int[2];

        /**
         * Reusable array that holds document numbers of parents.
         */
        private int[] pDocs = new int[1];

        /**
         * Reusable array that holds a single document number.
         */
        private final int[] singleDoc = new int[1];

        /**
         * The next document id to be returned
         */
        private int currentDoc = -1;

        /**
         * Creates a new <code>DescendantSelfAxisScorer</code>.
         *
         * @param similarity the <code>Similarity</code> instance to use.
         * @param reader     for index access.
         * @param hResolver  the hierarchy resolver of <code>reader</code>.
         */
        protected DescendantSelfAxisScorer(Similarity similarity,
                                           IndexReader reader,
                                           HierarchyResolver hResolver) {
            super(similarity);
            this.hResolver = hResolver;
            // todo reuse BitSets?
            this.contextHits = new BitSet(reader.maxDoc());
        }

        @Override
        public int nextDoc() throws IOException {
            if (currentDoc == NO_MORE_DOCS) {
                return currentDoc;
            }

            collectContextHits();
            if (contextHits.isEmpty()) {
                currentDoc = NO_MORE_DOCS;
            } else {
                if (subScorer != null) {
                    currentDoc = subScorer.nextDoc();
                } else {
                    currentDoc = NO_MORE_DOCS;
                }
            }
            while (currentDoc != NO_MORE_DOCS) {
                if (isValid(currentDoc)) {
                    return currentDoc;
                }

                // try next
                currentDoc = subScorer.nextDoc();
            }
            return currentDoc;
        }

        @Override
        public int docID() {
            return currentDoc;
        }

        @Override
        public float score() throws IOException {
            return subScorer.score();
        }

        @Override
        public int advance(int target) throws IOException {
            if (currentDoc == NO_MORE_DOCS) {
                return currentDoc;
            }

            // optimize in the case of an advance to finish.
            // see https://issues.apache.org/jira/browse/JCR-3082
            if (target == NO_MORE_DOCS) {
                if (subScorer != null) {
                    subScorer.advance(target);
                }
                currentDoc = NO_MORE_DOCS;
                return currentDoc;
            }

            currentDoc = subScorer.advance(target);
            if (currentDoc == NO_MORE_DOCS) {
                return NO_MORE_DOCS;
            } else {
                collectContextHits();
                return isValid(currentDoc) ? currentDoc : nextDoc();
            }
        }

        private void collectContextHits() throws IOException {
            if (!contextHitsCalculated) {
                long time = System.currentTimeMillis();
                if (contextScorer != null) {
                    contextScorer.score(new AbstractHitCollector() {
                        @Override
                        protected void collect(int doc, float score) {
                            contextHits.set(doc);
                        }
                    }); // find all
                }
                contextHitsCalculated = true;
                time = System.currentTimeMillis() - time;
                if (log.isDebugEnabled()) {
                    log.debug("Collected {} context hits in {} ms for {}",
                            new Object[]{
                                    contextHits.cardinality(),
                                    time,
                                    DescendantSelfAxisQuery.this
                            });
                }
            }
        }

        /**
         * Returns <code>true</code> if <code>doc</code> is a valid match from
         * the sub scorer against the context hits. The caller must ensure
         * that the context hits are calculated before this method is called!
         *
         * @param doc the document number.
         * @return <code>true</code> if <code>doc</code> is valid.
         * @throws IOException if an error occurs while reading from the index.
         */
        private boolean isValid(int doc) throws IOException {
            // check self if necessary
            if (minLevels == 0 && contextHits.get(doc)) {
                return true;
            }

            // check if doc is a descendant of one of the context nodes
            pDocs = hResolver.getParents(doc, pDocs);

            if (pDocs.length == 0) {
                return false;
            }

            int ancestorCount = 0;
            // can only remember one parent doc per level
            ancestorDocs[ancestorCount++] = pDocs[0];

            // traverse
            while (pDocs.length != 0) {
                boolean valid = false;
                for (int pDoc : pDocs) {
                    if (ancestorCount >= minLevels && contextHits.get(pDoc)) {
                        valid = true;
                        break;
                    }
                }
                if (valid) {
                    break;
                } else {
                    // load next level
                    pDocs = getParents(pDocs, singleDoc);
                    // resize array if needed
                    if (ancestorCount == ancestorDocs.length) {
                        // double the size of the new array
                        int[] copy = new int[ancestorDocs.length * 2];
                        System.arraycopy(ancestorDocs, 0, copy, 0, ancestorDocs.length);
                        ancestorDocs = copy;
                    }
                    if (pDocs.length != 0) {
                        // can only remember one parent doc per level
                        ancestorDocs[ancestorCount++] = pDocs[0];
                    }
                }
            }

            if (pDocs.length > 0) {
                // since current parentDocs are descendants of one of the context
                // docs we can promote all ancestorDocs to the context hits
                for (int i = 0; i < ancestorCount; i++) {
                    contextHits.set(ancestorDocs[i]);
                }
                return true;
            }
            return false;
        }

        /**
         * Returns the parent document numbers for the given <code>docs</code>.
         *
         * @param docs  the current document numbers, for which to get the
         *              parents.
         * @param pDocs an array of document numbers for reuse as return value.
         * @return the parent document number for the given <code>docs</code>.
         * @throws IOException if an error occurs while reading from the index.
         */
        private int[] getParents(int[] docs, int[] pDocs) throws IOException {
            // optimize single doc
            if (docs.length == 1) {
                return hResolver.getParents(docs[0], pDocs);
            } else {
                pDocs = new int[0];
                for (int doc : docs) {
                    int[] p = hResolver.getParents(doc, new int[0]);
                    int[] tmp = new int[p.length + pDocs.length];
                    System.arraycopy(pDocs, 0, tmp, 0, pDocs.length);
                    System.arraycopy(p, 0, tmp, pDocs.length, p.length);
                    pDocs = tmp;
                }
                return pDocs;
            }
        }
    }
}
