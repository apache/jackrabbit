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

/**
 * <code>FilterQueryHits</code> implements a {@link QueryHits} filter that
 * forwards each call to the underlying query hits.
 */
public class FilterQueryHits implements QueryHits {

    /**
     * The underlying query hits.
     */
    private final QueryHits hits;

    /**
     * Creates a new <code>FilterQueryHits</code>, which forwards each call to
     * <code>hits</code>.
     *
     * @param hits the underlying query hits.
     */
    public FilterQueryHits(QueryHits hits) {
        this.hits = hits;
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
    public ScoreNode nextScoreNode() throws IOException {
        return hits.nextScoreNode();
    }

    /**
     * {@inheritDoc}
     */
    public void skip(int n) throws IOException {
        hits.skip(n);
    }
}
