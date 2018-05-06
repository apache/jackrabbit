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

import org.apache.jackrabbit.core.query.lucene.MultiColumnQueryHits;
import org.apache.jackrabbit.core.query.lucene.ScoreNode;
import org.apache.jackrabbit.spi.Name;
import org.apache.lucene.index.IndexReader;

/**
 * <code>SameNodeJoin</code> implements a same node join condition.
 */
public class SameNodeJoin extends AbstractCondition {

    /**
     * A score node map with the score nodes from the inner query hits, indexed
     * by their document number.
     */
    private final ScoreNodeMap innerIndex = new ScoreNodeMap();

    /**
     * The index reader.
     */
    private final IndexReader reader;

    /**
     * Creates a new same node join.
     *
     * @param inner             the inner query hits.
     * @param innerSelectorName the selector name for the inner query hits.
     * @param reader            the index reader.
     * @throws IOException if an error occurs while reading from the index.
     */
    public SameNodeJoin(MultiColumnQueryHits inner,
                        Name innerSelectorName,
                        IndexReader reader) throws IOException {
        super(inner);
        this.reader = reader;
        int idx = getIndex(inner, innerSelectorName);
        ScoreNode[] nodes;
        while ((nodes = inner.nextScoreNodes()) != null) {
            innerIndex.addScoreNodes(nodes[idx].getDoc(reader), nodes);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode[][] getMatchingScoreNodes(ScoreNode outer)
            throws IOException {
        return innerIndex.getScoreNodes(outer.getDoc(reader));
    }
}
