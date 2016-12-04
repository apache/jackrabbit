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
import java.util.Collection;
import java.util.Iterator;

/**
 * <code>DefaultQueryHits</code> implements {@link QueryHits} based on a
 * collection of {@link ScoreNode}s.
 */
public class DefaultQueryHits extends AbstractQueryHits {

    /**
     * The total number of score nodes.
     */
    private final int size;

    /**
     * An iterator over the query nodes.
     */
    private final Iterator<ScoreNode> scoreNodes;

    /**
     * Creates a new <code>DefaultQueryHits</code> instance based on the passed
     * <code>scoreNodes</code>.
     *
     * @param scoreNodes a collection of {@link ScoreNode}s.
     */
    public DefaultQueryHits(Collection<ScoreNode> scoreNodes) {
        this.size = scoreNodes.size();
        this.scoreNodes = scoreNodes.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode nextScoreNode() throws IOException {
        if (scoreNodes.hasNext()) {
            return (ScoreNode) scoreNodes.next();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return size;
    }
}
