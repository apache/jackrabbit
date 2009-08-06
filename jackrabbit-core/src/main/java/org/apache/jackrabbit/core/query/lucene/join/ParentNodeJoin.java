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

import org.apache.jackrabbit.core.query.lucene.ScoreNode;
import org.apache.jackrabbit.core.query.lucene.MultiColumnQueryHits;
import org.apache.jackrabbit.core.query.lucene.HierarchyResolver;
import org.apache.jackrabbit.spi.commons.query.qom.ChildNodeJoinConditionImpl;
import org.apache.lucene.index.IndexReader;

/**
 * <code>ParentNodeJoin</code> implements a parent node join condition.
 */
public class ParentNodeJoin extends AbstractCondition {

    /**
     * The child score nodes indexed by their parent document number.
     */
    private final ScoreNodeMap childIndex = new ScoreNodeMap();

    /**
     * The index reader.
     */
    private final IndexReader reader;

    /**
     * Creates a new parent node join condition.
     *
     * @param child     the inner query hits.
     * @param reader    the index reader.
     * @param resolver  the hierarchy resolver.
     * @param condition the QOM child node join condition.
     * @throws IOException if an error occurs while reading from the index.
     */
    public ParentNodeJoin(MultiColumnQueryHits child,
                          IndexReader reader,
                          HierarchyResolver resolver,
                          ChildNodeJoinConditionImpl condition)
            throws IOException {
        super(child);
        this.reader = reader;
        int idx = getIndex(child, condition.getChildSelectorQName());
        ScoreNode[] nodes;
        int[] docNums = new int[1];
        while ((nodes = child.nextScoreNodes()) != null) {
            docNums = resolver.getParents(nodes[idx].getDoc(reader), docNums);
            for (int parentId : docNums) {
                childIndex.addScoreNodes(parentId, nodes);
            }
        }
    }

    /**
     * {@inheritDoc}
     * The outer query hits loop contains the parent score nodes.
     */
    public ScoreNode[][] getMatchingScoreNodes(ScoreNode parent)
            throws IOException {
        return childIndex.getScoreNodes(parent.getDoc(reader));
    }
}
