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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

/**
 * Collector implementation which simply provides the collection
 * of re-based doc base with scorer.
 */
public abstract class AbstractHitCollector extends Collector {
    protected int base = 0;
    protected Scorer scorer = null;

    @Override
    public void setNextReader(IndexReader reader, int docBase) throws IOException {
        base = docBase;
    }

    @Override
    public void setScorer(Scorer scorer) throws IOException {
        this.scorer = scorer;
    }

    @Override
    public void collect(int doc) throws IOException {
        collect(base + doc, scorer.score());
    }

    /**
     * Called once for every document matching a query, with the re-based document
     * number and its computed score.
     * @param doc the re-based document number.
     * @param score the document's score.
     */
    protected abstract void collect(int doc, float score);

    @Override
    public boolean acceptsDocsOutOfOrder() {
        return false;
    }
}
