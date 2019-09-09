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

import org.apache.jackrabbit.core.query.lucene.MultiColumnQueryHits;
import org.apache.jackrabbit.core.query.lucene.ScoreNode;
import org.apache.jackrabbit.core.query.lucene.HierarchyResolver;
import org.apache.jackrabbit.spi.commons.query.qom.ChildNodeJoinConditionImpl;
import org.apache.lucene.index.IndexReader;

/**
 * <code>ChildNodeJoin</code> implements a child node join condition.
 */
public class ChildNodeJoin extends AbstractCondition {

    /**
     * A score node map with the score nodes from the inner query hits, indexed
     * by the document number of the parent node.
     */
    private final ScoreNodeMap parentIndex = new ScoreNodeMap();

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
     * Reusable list of score nodes.
     */
    private List<ScoreNode[]> tmpScoreNodes = new ArrayList<ScoreNode[]>();

    /**
     * Creates a new child node join condition.
     *
     * @param parent    the inner query hits.
     * @param reader    the index reader.
     * @param resolver  the hierarchy resolver.
     * @param condition the QOM child node join condition.
     * @throws IOException if an error occurs while reading from the index.
     */
    public ChildNodeJoin(MultiColumnQueryHits parent,
                         IndexReader reader,
                         HierarchyResolver resolver,
                         ChildNodeJoinConditionImpl condition)
            throws IOException {
        super(parent);
        this.reader = reader;
        this.resolver = resolver;
        int idx = getIndex(parent, condition.getParentSelectorQName());
        ScoreNode[] nodes;
        while ((nodes = parent.nextScoreNodes()) != null) {
            Integer docNum = nodes[idx].getDoc(reader);
            parentIndex.addScoreNodes(docNum, nodes);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The outer query hits loop contains the child nodes.
     */
    public ScoreNode[][] getMatchingScoreNodes(ScoreNode child) throws IOException {
        docNums = resolver.getParents(child.getDoc(reader), docNums);
        tmpScoreNodes.clear();
        for (int docNum : docNums) {
            ScoreNode[][] sn = parentIndex.getScoreNodes(docNum);
            if (sn != null) {
                for (ScoreNode[] aSn : sn) {
                    tmpScoreNodes.add(aSn);
                }
            }
        }
        if (tmpScoreNodes.isEmpty()) {
            return null;
        } else {
            return tmpScoreNodes.toArray(new ScoreNode[tmpScoreNodes.size()][]);
        }
    }
}
