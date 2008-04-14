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
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.lucene.hits.AdaptingHits;
import org.apache.jackrabbit.core.query.lucene.hits.Hits;
import org.apache.jackrabbit.core.query.lucene.hits.ScorerHits;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.query.LocationStepQueryNode;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.Sort;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements a lucene <code>Query</code> which returns the child nodes of the
 * nodes selected by another <code>Query</code>.
 */
class ChildAxisQuery extends Query implements JackrabbitQuery {

    /**
     * The item state manager containing persistent item states.
     */
    private final ItemStateManager itemMgr;

    /**
     * The context query
     */
    private Query contextQuery;

    /**
     * The nameTest to apply on the child axis, or <code>null</code> if all
     * child nodes should be selected.
     */
    private final Name nameTest;

    /**
     * The context position for the selected child node, or
     * {@link LocationStepQueryNode#NONE} if no position is specified.
     */
    private final int position;

    /**
     * The index format version.
     */ 
    private final IndexFormatVersion version;

    /**
     * The internal namespace mappings.
     */
    private final NamespaceMappings nsMappings;

    /**
     * The scorer of the context query
     */
    private Scorer contextScorer;

    /**
     * The scorer of the name test query
     */
    private Scorer nameTestScorer;

    /**
     * Creates a new <code>ChildAxisQuery</code> based on a <code>context</code>
     * query.
     *
     * @param itemMgr the item state manager.
     * @param context the context for this query.
     * @param nameTest a name test or <code>null</code> if any child node is
     * selected.
     * @param version the index format version.
     * @param nsMappings the internal namespace mappings.
     */
    ChildAxisQuery(ItemStateManager itemMgr,
                   Query context,
                   Name nameTest,
                   IndexFormatVersion version,
                   NamespaceMappings nsMappings) {
        this(itemMgr, context, nameTest, LocationStepQueryNode.NONE, version, nsMappings);
    }

    /**
     * Creates a new <code>ChildAxisQuery</code> based on a <code>context</code>
     * query.
     *
     * @param itemMgr the item state manager.
     * @param context the context for this query.
     * @param nameTest a name test or <code>null</code> if any child node is
     * selected.
     * @param position the context position of the child node to select. If
     * <code>position</code> is {@link LocationStepQueryNode#NONE}, the context
     * position of the child node is not checked.
     * @param version the index format version.
     * @param nsMapping the internal namespace mappings.
     */
    ChildAxisQuery(ItemStateManager itemMgr,
                   Query context,
                   Name nameTest,
                   int position,
                   IndexFormatVersion version,
                   NamespaceMappings nsMapping) {
        this.itemMgr = itemMgr;
        this.contextQuery = context;
        this.nameTest = nameTest;
        this.position = position;
        this.version = version;
        this.nsMappings = nsMapping;
    }

    /**
     * @return the context query of this child axis query.
     */
    Query getContextQuery() {
        return contextQuery;
    }

    /**
     * @return <code>true</code> if this child axis query matches any child
     *         node; <code>false</code> otherwise.
     */
    boolean matchesAnyChildNode() {
        return nameTest == null && position == LocationStepQueryNode.NONE;
    }

    /**
     * @return the name test or <code>null</code> if none was specified.
     */
    Name getNameTest() {
        return nameTest;
    }

    /**
     * @return the position check or {@link LocationStepQueryNode#NONE} is none
     *         was specified.
     */
    int getPosition() {
        return position;
    }

    /**
     * Creates a <code>Weight</code> instance for this query.
     *
     * @param searcher the <code>Searcher</code> instance to use.
     * @return a <code>ChildAxisWeight</code>.
     */
    protected Weight createWeight(Searcher searcher) {
        return new ChildAxisWeight(searcher);
    }

    /**
     * {@inheritDoc}
     */
    public void extractTerms(Set terms) {
        contextQuery.extractTerms(terms);
    }

    /**
     * {@inheritDoc}
     */
    public Query rewrite(IndexReader reader) throws IOException {
        Query cQuery = contextQuery.rewrite(reader);
        // only try to compact if no position is specified
        if (position == LocationStepQueryNode.NONE) {
            if (cQuery instanceof DescendantSelfAxisQuery) {
                DescendantSelfAxisQuery dsaq = (DescendantSelfAxisQuery) cQuery;
                if (dsaq.subQueryMatchesAll()) {
                    Query sub;
                    if (nameTest == null) {
                        sub = new MatchAllDocsQuery();
                    } else {
                        sub = new NameQuery(nameTest, version, nsMappings);
                    }
                    return new DescendantSelfAxisQuery(dsaq.getContextQuery(),
                            sub, dsaq.getMinLevels() + 1).rewrite(reader);
                }
            }
        }

        // if we get here we could not compact the query
        if (cQuery == contextQuery) {
            return this;
        } else {
            return new ChildAxisQuery(itemMgr, cQuery, nameTest,
                    position, version, nsMappings);
        }
    }

    /**
     * Always returns 'ChildAxisQuery'.
     *
     * @param field the name of a field.
     * @return 'ChildAxisQuery'.
     */
    public String toString(String field) {
        return "ChildAxisQuery";
    }

    //-------------------< JackrabbitQuery >------------------------------------

    /**
     * {@inheritDoc}
     */
    public QueryHits execute(JackrabbitIndexSearcher searcher,
                             SessionImpl session,
                             Sort sort)
            throws IOException {
        if (sort.getSort().length == 0 && matchesAnyChildNode()) {
            Query context = getContextQuery();
            return new ChildNodesQueryHits(searcher.evaluate(context, sort), session);
        } else {
            return null;
        }
    }

    //-------------------< ChildAxisWeight >------------------------------------

    /**
     * The <code>Weight</code> implementation for this <code>ChildAxisQuery</code>.
     */
    private class ChildAxisWeight implements Weight {

        /**
         * The searcher in use
         */
        private final Searcher searcher;

        /**
         * Creates a new <code>ChildAxisWeight</code> instance using
         * <code>searcher</code>.
         *
         * @param searcher a <code>Searcher</code> instance.
         */
        private ChildAxisWeight(Searcher searcher) {
            this.searcher = searcher;
        }

        /**
         * Returns this <code>ChildAxisQuery</code>.
         *
         * @return this <code>ChildAxisQuery</code>.
         */
        public Query getQuery() {
            return ChildAxisQuery.this;
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
         * Creates a scorer for this <code>ChildAxisQuery</code>.
         *
         * @param reader a reader for accessing the index.
         * @return a <code>ChildAxisScorer</code>.
         * @throws IOException if an error occurs while reading from the index.
         */
        public Scorer scorer(IndexReader reader) throws IOException {
            contextScorer = contextQuery.weight(searcher).scorer(reader);
            if (nameTest != null) {
                nameTestScorer = new NameQuery(nameTest, version, nsMappings).weight(searcher).scorer(reader);
            }
            return new ChildAxisScorer(searcher.getSimilarity(),
                    reader, (HierarchyResolver) reader);
        }

        /**
         * {@inheritDoc}
         */
        public Explanation explain(IndexReader reader, int doc) throws IOException {
            return new Explanation();
        }
    }

    //----------------------< ChildAxisScorer >---------------------------------

    /**
     * Implements a <code>Scorer</code> for this <code>ChildAxisQuery</code>.
     */
    private class ChildAxisScorer extends Scorer {

        /**
         * An <code>IndexReader</code> to access the index.
         */
        private final IndexReader reader;

        /**
         * The <code>HierarchyResolver</code> of the index.
         */
        private final HierarchyResolver hResolver;

        /**
         * The next document id to return
         */
        private int nextDoc = -1;

        /**
         * A <code>Hits</code> instance containing all hits
         */
        private Hits hits;

        /**
         * Creates a new <code>ChildAxisScorer</code>.
         *
         * @param similarity the <code>Similarity</code> instance to use.
         * @param reader     for index access.
         * @param hResolver  the hierarchy resolver of <code>reader</code>.
         */
        protected ChildAxisScorer(Similarity similarity,
                                  IndexReader reader,
                                  HierarchyResolver hResolver) {
            super(similarity);
            this.reader = reader;
            this.hResolver = hResolver;
        }

        /**
         * {@inheritDoc}
         */
        public boolean next() throws IOException {
            calculateChildren();
            do {
                nextDoc = hits.next();
            } while (nextDoc > -1 && !indexIsValid(nextDoc));

            return nextDoc > -1;
        }

        /**
         * {@inheritDoc}
         */
        public int doc() {
            return nextDoc;
        }

        /**
         * {@inheritDoc}
         */
        public float score() throws IOException {
            return 1.0f;
        }

        /**
         * {@inheritDoc}
         */
        public boolean skipTo(int target) throws IOException {
            calculateChildren();
            nextDoc = hits.skipTo(target);
            while (nextDoc > -1 && !indexIsValid(nextDoc)) {
                next();
            }
            return nextDoc > -1;
        }

        /**
         * {@inheritDoc}
         *
         * @throws UnsupportedOperationException this implementation always
         *                                       throws an <code>UnsupportedOperationException</code>.
         */
        public Explanation explain(int doc) throws IOException {
            throw new UnsupportedOperationException();
        }

        private void calculateChildren() throws IOException {
            if (hits == null) {

                // collect all context nodes
                Map uuids = new HashMap();
                final Hits contextHits = new AdaptingHits();
                contextScorer.score(new HitCollector() {
                    public void collect(int doc, float score) {
                        contextHits.set(doc);
                    }
                });

                // read the uuids of the context nodes
                for (int i = contextHits.next(); i > -1; i = contextHits.next()) {
                    String uuid = reader.document(i, FieldSelectors.UUID).get(FieldNames.UUID);
                    uuids.put(new Integer(i), uuid);
                }

                // collect all children of the context nodes
                Hits childrenHits = new AdaptingHits();
                if (nameTestScorer != null) {
                    Hits nameHits = new ScorerHits(nameTestScorer);
                    for (int h = nameHits.next(); h > -1; h = nameHits.next()) {
                        if (uuids.containsKey(new Integer(hResolver.getParent(h)))) {
                            childrenHits.set(h);
                        }
                    }
                } else {
                    // get child node entries for each hit
                    for (Iterator it = uuids.values().iterator(); it.hasNext(); ) {
                        String uuid = (String) it.next();
                        NodeId id = new NodeId(UUID.fromString(uuid));
                        try {
                            NodeState state = (NodeState) itemMgr.getItemState(id);
                            Iterator entries = state.getChildNodeEntries().iterator();
                            while (entries.hasNext()) {
                                NodeId childId = ((NodeState.ChildNodeEntry) entries.next()).getId();
                                Term uuidTerm = new Term(FieldNames.UUID, childId.getUUID().toString());
                                TermDocs docs = reader.termDocs(uuidTerm);
                                try {
                                    if (docs.next()) {
                                        childrenHits.set(docs.doc());
                                    }
                                } finally {
                                    docs.close();
                                }
                            }
                        } catch (ItemStateException e) {
                            // does not exist anymore -> ignore
                        }
                    }
                }

                hits = childrenHits;
            }
        }

        private boolean indexIsValid(int i) throws IOException {
            if (position != LocationStepQueryNode.NONE) {
                Document node = reader.document(i, FieldSelectors.UUID_AND_PARENT);
                NodeId parentId = NodeId.valueOf(node.get(FieldNames.PARENT));
                NodeId id = NodeId.valueOf(node.get(FieldNames.UUID));
                try {
                    NodeState state = (NodeState) itemMgr.getItemState(parentId);
                    if (nameTest == null) {
                        // only select this node if it is the child at
                        // specified position
                        if (position == LocationStepQueryNode.LAST) {
                            // only select last
                            List childNodes = state.getChildNodeEntries();
                            if (childNodes.size() == 0
                                    || !((NodeState.ChildNodeEntry) childNodes.get(childNodes.size() - 1))
                                        .getId().equals(id)) {
                                return false;
                            }
                        } else {
                            List childNodes = state.getChildNodeEntries();
                            if (position < 1
                                    || childNodes.size() < position
                                    || !((NodeState.ChildNodeEntry) childNodes.get(position - 1)).getId().equals(id)) {
                                return false;
                            }
                        }
                    } else {
                        // select the node when its index is equal to
                        // specified position
                        if (position == LocationStepQueryNode.LAST) {
                            // only select last
                            NodeState.ChildNodeEntry entry =
                                    state.getChildNodeEntry(id);
                            if (entry == null) {
                                // no such child node, probably deleted meanwhile
                                return false;
                            } else {
                                // only use the last one
                                Name name = entry.getName();
                                List childNodes = state.getChildNodeEntries(name);
                                if (childNodes.size() == 0
                                        || !((NodeState.ChildNodeEntry) childNodes.get(childNodes.size() - 1))
                                            .getId().equals(id)) {
                                    return false;
                                }
                            }
                        } else {
                            NodeState.ChildNodeEntry entry =
                                    state.getChildNodeEntry(id);
                            if (entry == null) {
                                // no such child node, probably has been deleted meanwhile
                                return false;
                            } else {
                                if (entry.getIndex() != position) {
                                    return false;
                                }
                            }
                        }
                    }
                } catch (ItemStateException e) {
                    // ignore this node, probably has been deleted meanwhile
                    return false;
                }
            }
            return true;
        }
    }
}
