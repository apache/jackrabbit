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
package org.apache.jackrabbit.core.query.lucene.join;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.apache.jackrabbit.core.query.lucene.ScoreNode;
import org.apache.jackrabbit.core.query.lucene.MultiColumnQueryHits;
import org.apache.jackrabbit.core.query.lucene.HierarchyResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.lucene.index.IndexReader;

/**
 * <code>AncestorNodeJoin</code> implements an ancestor node join condition.
 */
public class AncestorNodeJoin extends AbstractCondition {

    /**
     * A score node map with the score nodes from the inner query hits. The
     * inner score nodes are indexed by the document numbers of their ancestor
     * nodes.
     */
    private final ScoreNodeMap contextIndex = new ScoreNodeMap();

    /**
     * The index reader.
     */
    private final IndexReader reader;

    /**
     * The hierarchy resolver.
     */
    private final HierarchyResolver resolver;

    /**
     * Reusable array of document numbers.
     */
    private int[] docNums = new int[1];

    /**
     * Reusable list of ancestor document numbers.
     */
    private final List<Integer> ancestors = new ArrayList<Integer>();

    /**
     * Creates a new ancestor node join condition.
     *
     * @param context             the inner query hits.
     * @param contextSelectorName the selector name for the inner query hits.
     * @param reader              the index reader.
     * @param resolver            the hierarchy resolver.
     * @throws IOException if an error occurs while reading from the index.
     */
    public AncestorNodeJoin(MultiColumnQueryHits context,
                            Name contextSelectorName,
                            IndexReader reader,
                            HierarchyResolver resolver) throws IOException {
        super(context);
        this.reader = reader;
        this.resolver = resolver;
        int idx = getIndex(context, contextSelectorName);
        ScoreNode[] nodes;
        while ((nodes = context.nextScoreNodes()) != null) {
            Integer docNum = nodes[idx].getDoc(reader);
            ancestors.clear();
            collectAncestors(docNum);
            for (Integer doc : ancestors) {
                contextIndex.addScoreNodes(doc, nodes);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The outer query hits loop contains the ancestor score nodes.
     */
    public ScoreNode[][] getMatchingScoreNodes(ScoreNode ancestor)
            throws IOException {
        Integer doc = ancestor.getDoc(reader);
        return contextIndex.getScoreNodes(doc);
    }

    /**
     * Collects the ancestors of the given <code>doc</code> number into
     * {@link #ancestors}.
     *
     * @param doc the current document number.
     * @throws IOException if an error occurs while reading from the index.
     */
    private void collectAncestors(int doc) throws IOException {
        docNums = resolver.getParents(doc, docNums);
        if (docNums.length == 1) {
            ancestors.add(docNums[0]);
            collectAncestors(docNums[0]);
        } else if (docNums.length > 1) {
            // clone because recursion uses docNums again
            for (int docNum : docNums.clone()) {
                ancestors.add(docNum);
                collectAncestors(docNum);
            }
        }
    }
}
