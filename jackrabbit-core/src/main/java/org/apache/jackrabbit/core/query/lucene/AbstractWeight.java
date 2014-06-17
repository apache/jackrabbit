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

import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.index.IndexReader;

import java.io.IOException;

/**
 * <code>AbstractWeight</code> implements base functionality for custom lucene
 * weights in jackrabbit.
 */
@SuppressWarnings("serial")
abstract class AbstractWeight extends Weight {

    /**
     * The searcher for this weight.
     */
    protected final Searcher searcher;

    /**
     * Creates a new <code>AbstractWeight</code> for the given
     * <code>searcher</code>.
     *
     * @param searcher the searcher instance for this weight.
     */
    public AbstractWeight(Searcher searcher) {
        this.searcher = searcher;
    }

    /**
     * Abstract factory method for crating a scorer instance for the
     * specified reader.
     *
     * @param reader the index reader the created scorer instance should use
     * @return the scorer instance
     * @throws IOException if an error occurs while reading from the index
     */
    protected abstract Scorer createScorer(IndexReader reader, boolean scoreDocsInOrder,
            boolean topScorer) throws IOException;

    /**
     * {@inheritDoc}
     * <p>
     * Returns a {@link MultiScorer} if the passed <code>reader</code> is of
     * type {@link MultiIndexReader}.
     */
    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder,
            boolean topScorer) throws IOException {
        if (reader instanceof MultiIndexReader) {
            MultiIndexReader mir = (MultiIndexReader) reader;
            IndexReader[] readers = mir.getIndexReaders();
            int[] starts = new int[readers.length + 1];
            int maxDoc = 0;
            for (int i = 0; i < readers.length; i++) {
                starts[i] = maxDoc;
                maxDoc += readers[i].maxDoc();
            }

            starts[readers.length] = maxDoc;
            Scorer[] scorers = new Scorer[readers.length];
            for (int i = 0; i < readers.length; i++) {
                scorers[i] = scorer(readers[i], scoreDocsInOrder, false);
            }

            return new MultiScorer(searcher.getSimilarity(), scorers, starts);
        } else {
            return createScorer(reader, scoreDocsInOrder, topScorer);
        }
    }

}
