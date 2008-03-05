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

import org.apache.jackrabbit.spi.Name;

import java.io.IOException;

/**
 * <code>QueryHitsAdapter</code> implements an adapter for {@link QueryHits} and
 * exposes them as {@link MultiColumnQueryHits}.
 */
public class QueryHitsAdapter implements MultiColumnQueryHits {

    /**
     * The query hits to adapt.
     */
    private final QueryHits hits;

    /**
     * The single selector name to expose.
     */
    private final Name selectorName;

    /**
     * Creates a new adapter for <code>hits</code>.
     *
     * @param hits the query hits to adapt.
     * @param selectorName the single selector name for the query hits.
     */
    public QueryHitsAdapter(QueryHits hits, Name selectorName) {
        this.hits = hits;
        this.selectorName = selectorName;
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode[] nextScoreNodes() throws IOException {
        ScoreNode sn = hits.nextScoreNode();
        if (sn != null) {
            return new ScoreNode[]{sn};
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Name[] getSelectorNames() {
        return new Name[]{selectorName};
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
        hits.close();
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        return hits.getSize();
    }

    /**
     * {@inheritDoc}
     */
    public void skip(int n) throws IOException {
        hits.skip(n);
    }
}
