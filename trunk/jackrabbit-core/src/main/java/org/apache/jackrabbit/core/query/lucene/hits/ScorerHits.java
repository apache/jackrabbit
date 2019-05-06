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
package org.apache.jackrabbit.core.query.lucene.hits;

import java.io.IOException;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Scorer;

/**
 * Wraps a {@link org.apache.lucene.search.Scorer} in a {@link Hits} instance.
 */
public class ScorerHits implements Hits {

    private final Scorer scorer;

    public ScorerHits(Scorer scorer) {
        this.scorer = scorer;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int doc) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public int next() throws IOException {
        int docId = scorer.nextDoc();
        if (docId != DocIdSetIterator.NO_MORE_DOCS) {
            return docId;
        } else {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int skipTo(int target) throws IOException {
        int docId = scorer.advance(target);
        if (docId != DocIdSetIterator.NO_MORE_DOCS) {
            return docId;
        } else {
            return -1;
        }
    }
}
