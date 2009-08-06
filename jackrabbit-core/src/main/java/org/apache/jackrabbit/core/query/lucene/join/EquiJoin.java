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
import org.apache.lucene.search.SortComparatorSource;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.ScoreDoc;

/**
 * <code>EquiJoin</code> implements an equi join condition.
 */
public class EquiJoin extends AbstractCondition {

    /**
     * Reusable score doc for value lookups.
     */
    private final ScoreDoc sDoc = new ScoreDoc(-1, 1.0f);

    /**
     * The index reader.
     */
    private final IndexReader reader;

    /**
     * Map of inner score nodes indexed by the value of their join property.
     */
    private final ScoreNodeMap innerScoreNodes = new ScoreNodeMap();

    /**
     * The score doc comparator for the outer query hits.
     */
    private final ScoreDocComparator outerLookup;

    /**
     * Creates a new equi join condition.
     *
     * @param inner               the inner query hits.
     * @param innerScoreNodeIndex the selector name for the inner query hits.
     * @param scs                 the sort comparator source.
     * @param reader              the index reader.
     * @param innerProperty       the name of the property of the inner query
     *                            hits.
     * @param outerProperty       the name of the property of the outer query
     *                            hits.
     * @throws IOException if an error occurs while reading from the index.
     */
    public EquiJoin(MultiColumnQueryHits inner,
                    int innerScoreNodeIndex,
                    SortComparatorSource scs,
                    IndexReader reader,
                    Name innerProperty,
                    Name outerProperty) throws IOException {
        super(inner);
        this.reader = reader;
        this.outerLookup = scs.newComparator(reader, outerProperty.toString());
        ScoreDocComparator comparator = scs.newComparator(reader, innerProperty.toString());
        ScoreNode[] nodes;
        // create lookup map
        while ((nodes = inner.nextScoreNodes()) != null) {
            sDoc.doc = nodes[innerScoreNodeIndex].getDoc(reader);
            Comparable value = comparator.sortValue(sDoc);
            if (value != null) {
                innerScoreNodes.addScoreNodes(value, nodes);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode[][] getMatchingScoreNodes(ScoreNode outer)
            throws IOException {
        sDoc.doc = outer.getDoc(reader);
        Comparable value = outerLookup.sortValue(sDoc);
        return innerScoreNodes.getScoreNodes(value);
    }
}
