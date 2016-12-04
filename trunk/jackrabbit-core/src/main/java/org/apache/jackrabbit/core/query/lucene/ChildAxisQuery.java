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

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.query.lucene.hits.AbstractHitCollector;
import org.apache.jackrabbit.core.query.lucene.hits.AdaptingHits;
import org.apache.jackrabbit.core.query.lucene.hits.Hits;
import org.apache.jackrabbit.core.query.lucene.hits.ScorerHits;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.query.LocationStepQueryNode;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.ArrayList;

/**
 * Implements a lucene <code>Query</code> which returns the child nodes of the
 * nodes selected by another <code>Query</code>.
 */
@SuppressWarnings("serial")
class ChildAxisQuery extends Query implements JackrabbitQuery {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(ChildAxisQuery.class);

    /**
     * Threshold when children calculation is switched to
     * {@link HierarchyResolvingChildrenCalculator}.
     */
    private static int CONTEXT_SIZE_THRESHOLD = 10;

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
    public Weight createWeight(Searcher searcher) {
        return new ChildAxisWeight(searcher);
    }

    /**
     * {@inheritDoc}
     */
    public void extractTerms(Set<Term> terms) {
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
     * {@inheritDoc}
     */
    public String toString(String field) {
        StringBuffer sb = new StringBuffer();
        sb.append("ChildAxisQuery(");
        sb.append(contextQuery);
        sb.append(", ");
        sb.append(nameTest);
        if (position != LocationStepQueryNode.NONE) {
            sb.append(", ");
            sb.append(position);
        }
        sb.append(")");
        return sb.toString();
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
            return new ChildNodesQueryHits(searcher.evaluate(context), session);
        } else {
            return null;
        }
    }

    //-------------------< ChildAxisWeight >------------------------------------

    /**
     * The <code>Weight</code> implementation for this <code>ChildAxisQuery</code>.
     */
    private class ChildAxisWeight extends Weight {

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
        @Override
        public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
            contextScorer = contextQuery.weight(searcher).scorer(reader, scoreDocsInOrder, false);
            if (nameTest != null) {
                nameTestScorer = new NameQuery(nameTest, version, nsMappings).weight(searcher).scorer(reader, scoreDocsInOrder, false);
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

        @Override
        public int nextDoc() throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }

            calculateChildren();
            do {
                nextDoc = hits.next();
            } while (nextDoc > -1 && !indexIsValid(nextDoc));

            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }
            return nextDoc;
        }

        @Override
        public int docID() {
            return nextDoc;
        }

        @Override
        public float score() throws IOException {
            return 1.0f;
        }

        @Override
        public int advance(int target) throws IOException {
            if (nextDoc == NO_MORE_DOCS) {
                return nextDoc;
            }
            
            // optimize in the case of an advance to finish.
            // see https://issues.apache.org/jira/browse/JCR-3091
            if (target == NO_MORE_DOCS) {
                hits.skipTo(target);
                nextDoc = NO_MORE_DOCS;
                return nextDoc;
            }

            calculateChildren();
            nextDoc = hits.skipTo(target);
            if (nextDoc < 0) {
                nextDoc = NO_MORE_DOCS;
            }

            while (nextDoc != NO_MORE_DOCS && !indexIsValid(nextDoc)) {
                nextDoc();
            }
            return nextDoc;
        }

        private void calculateChildren() throws IOException {
            if (hits == null) {

                final ChildrenCalculator[] calc = new ChildrenCalculator[1];
                if (nameTestScorer == null) {
                    // always use simple in that case
                    calc[0] = new SimpleChildrenCalculator(reader, hResolver);
                    contextScorer.score(new AbstractHitCollector() {
                        @Override
                        protected void collect(int doc, float score) {
                            calc[0].collectContextHit(doc);
                        }
                    });
                } else {
                    // start simple but switch once threshold is reached
                    calc[0] = new SimpleChildrenCalculator(reader, hResolver);
                    contextScorer.score(new AbstractHitCollector() {

                        private List<Integer> docIds = new ArrayList<Integer>();

                        @Override
                        protected void collect(int doc, float score) {
                            calc[0].collectContextHit(doc);
                            if (docIds != null) {
                                docIds.add(doc);
                                if (docIds.size() > CONTEXT_SIZE_THRESHOLD) {
                                    // switch
                                    calc[0] = new HierarchyResolvingChildrenCalculator(
                                            reader, hResolver);
                                    for (int docId : docIds) {
                                        calc[0].collectContextHit(docId);
                                    }
                                    // indicate that we switched
                                    docIds = null;
                                }
                            }
                        }
                    });
                }

                hits = calc[0].getHits();
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
                        List<ChildNodeEntry> childNodes = state.getChildNodeEntries();
                        if (position == LocationStepQueryNode.LAST) {
                            // only select last
                            if (childNodes.size() == 0
                                    || !(childNodes.get(childNodes.size() - 1)).getId().equals(id)) {
                                return false;
                            }
                        } else {
                            if (position < 1
                                    || childNodes.size() < position
                                    || !(childNodes.get(position - 1)).getId().equals(id)) {
                                return false;
                            }
                        }
                    } else {
                        // select the node when its index is equal to
                        // specified position
                        if (position == LocationStepQueryNode.LAST) {
                            // only select last
                            ChildNodeEntry entry =
                                    state.getChildNodeEntry(id);
                            if (entry == null) {
                                // no such child node, probably deleted meanwhile
                                return false;
                            } else {
                                // only use the last one
                                Name name = entry.getName();
                                List<ChildNodeEntry> childNodes = state.getChildNodeEntries(name);
                                if (childNodes.size() == 0
                                        || !(childNodes.get(childNodes.size() - 1)).getId().equals(id)) {
                                    return false;
                                }
                            }
                        } else {
                            ChildNodeEntry entry =
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

    /**
     * Base class to calculate the children for a context query.
     */
    private abstract class ChildrenCalculator {

        /**
         * The current index reader.
         */
        protected final IndexReader reader;

        /**
         * The current hierarchy resolver.
         */
        protected final HierarchyResolver hResolver;

        /**
         * Creates a new children calculator with the given index reader and
         * hierarchy resolver.
         *
         * @param reader the current index reader.
         * @param hResolver the current hierarchy resolver.
         */
        public ChildrenCalculator(IndexReader reader,
                                  HierarchyResolver hResolver) {
            this.reader = reader;
            this.hResolver = hResolver;
        }

        /**
         * Collects a context hit.
         *
         * @param doc the lucene document number of the context hit.
         */
        protected abstract void collectContextHit(int doc);

        /**
         * @return the hits that contains the children.
         * @throws IOException if an error occurs while reading from the index.
         */
        public abstract Hits getHits() throws IOException;
    }

    /**
     * An implementation of a children calculator using the item state manager.
     */
    private final class SimpleChildrenCalculator extends ChildrenCalculator {

        /**
         * The context hits.
         */
        private final Hits contextHits = new AdaptingHits();

        /**
         * Creates a new simple children calculator.
         *
         * @param reader the current index reader.
         * @param hResolver the current hierarchy resolver.
         */
        public SimpleChildrenCalculator(IndexReader reader,
                                        HierarchyResolver hResolver) {
            super(reader, hResolver);
        }

        /**
         * {@inheritDoc}
         */
        protected void collectContextHit(int doc) {
            contextHits.set(doc);
        }

        /**
         * {@inheritDoc}
         */
        public Hits getHits() throws IOException {
            // read the uuids of the context nodes
            Map<Integer, String> uuids = new HashMap<Integer, String>();
            for (int i = contextHits.next(); i > -1; i = contextHits.next()) {
                String uuid = reader.document(i, FieldSelectors.UUID).get(FieldNames.UUID);
                uuids.put(i, uuid);
            }

            // get child node entries for each hit
            Hits childrenHits = new AdaptingHits();
            for (String uuid : uuids.values()) {
                NodeId id = new NodeId(uuid);
                try {
                    long time = System.currentTimeMillis();
                    NodeState state = (NodeState) itemMgr.getItemState(id);
                    time = System.currentTimeMillis() - time;
                    log.debug("got NodeState with id {} in {} ms.", id, time);
                    List<ChildNodeEntry> entries;
                    if (nameTest != null) {
                        entries = state.getChildNodeEntries(nameTest);
                    } else {
                        // get all children
                        entries = state.getChildNodeEntries();
                    }
                    for (ChildNodeEntry entry : entries) {
                        NodeId childId = entry.getId();
                        Term uuidTerm = TermFactory.createUUIDTerm(childId.toString());
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
            return childrenHits;
        }
    }

    /**
     * An implementation of a children calculator that uses the hierarchy
     * resolver. This implementation requires that
     * {@link ChildAxisQuery#nameTestScorer} is non null.
     */
    private final class HierarchyResolvingChildrenCalculator
            extends ChildrenCalculator {

        /**
         * The document numbers of the context hits.
         */
        private final Set<Integer> docIds = new HashSet<Integer>();

        /**
         * Creates a new hierarchy resolving children calculator.
         *
         * @param reader the current index reader.
         * @param hResolver the current hierarchy resolver.
         */
        public HierarchyResolvingChildrenCalculator(IndexReader reader,
                                                    HierarchyResolver hResolver) {
            super(reader, hResolver);
        }

        /**
         * {@inheritDoc}
         */
        protected void collectContextHit(int doc) {
            docIds.add(doc);
        }

        /**
         * {@inheritDoc}
         */
        public Hits getHits() throws IOException {
            long time = System.currentTimeMillis();
            Hits childrenHits = new AdaptingHits();
            Hits nameHits = new ScorerHits(nameTestScorer);
            int[] docs = new int[1];
            for (int h = nameHits.next(); h > -1; h = nameHits.next()) {
                docs = hResolver.getParents(h, docs);
                if (docs.length == 1) {
                    // optimize single value
                    if (docIds.contains(docs[0])) {
                        childrenHits.set(h);
                    }
                } else {
                    for (int i = 0; i < docs.length; i++) {
                        if (docIds.contains(docs[i])) {
                            childrenHits.set(h);
                        }
                    }
                }
            }
            time = System.currentTimeMillis() - time;

            log.debug("Filtered hits in {} ms.", time);
            return childrenHits;
        }
    }
}
