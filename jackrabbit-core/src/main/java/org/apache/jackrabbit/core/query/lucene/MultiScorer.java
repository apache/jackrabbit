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
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Similarity;

import java.io.IOException;

/**
 * <code>MultiScorer</code> spans multiple Scorers and returns document numbers
 * and score values in the order as supplied to the constructor of this
 * <code>MultiScorer</code>.
 */
class MultiScorer extends Scorer {

    /**
     * The sub scorers.
     */
    private final Scorer[] scorers;

    /**
     * The document start numbers of the sub scorers.
     */
    private final int[] starts;

    /**
     * Index of the current scorer.
     */
    private int current = 0;

    /**
     * Indicates if there are more documents.
     */
    private boolean hasNext = true;

    /**
     * Creates a new <code>MultiScorer</code> that spans multiple
     * <code>scorers</code>.
     *
     * @param similarity the similarity implementation that should be use.
     * @param scorers the sub scorers.
     * @param starts the document number start for each sub scorer.
     */
    MultiScorer(Similarity similarity, Scorer[] scorers, int[] starts) {
        super(similarity);
        this.scorers = scorers;
        this.starts = starts;
    }

    /**
     * {@inheritDoc}
     */
    public boolean next() throws IOException {
        while (hasNext) {
            if (scorers[current].next()) {
                return true;
            } else if (++current < scorers.length) {
                // advance to next scorer
            } else {
                // no more scorers
                hasNext = false;
            }
        }
        return hasNext;
    }

    /**
     * {@inheritDoc}
     */
    public int doc() {
        return scorers[current].doc() + starts[current];
    }

    /**
     * {@inheritDoc}
     */
    public float score() throws IOException {
        return scorers[current].score();
    }

    /**
     * {@inheritDoc}
     */
    public boolean skipTo(int target) throws IOException {
        current = scorerIndex(target);
        if (scorers[current].skipTo(target - starts[current])) {
            return true;
        } else {
            if (++current < scorers.length) {
                // simply move to the next if there is any
                return next();
            } else {
                // no more document
                hasNext = false;
                return hasNext;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Explanation explain(int doc) throws IOException {
        int scorerIndex = scorerIndex(doc);
        return scorers[scorerIndex].explain(doc - starts[scorerIndex]);
    }

    //--------------------------< internal >------------------------------------

    /**
     * Returns the scorer index for document <code>n</code>.
     * Implementation copied from lucene MultiReader class.
     *
     * @param n document number.
     * @return the scorer index.
     */
    private int scorerIndex(int n) {
        int lo = 0;                                      // search starts array
        int hi = scorers.length - 1;                  // for first element less

        while (hi >= lo) {
            int mid = (lo + hi) >> 1;
            int midValue = starts[mid];
            if (n < midValue) {
                hi = mid - 1;
            } else if (n > midValue) {
                lo = mid + 1;
            } else {                                      // found a match
                while (mid + 1 < scorers.length && starts[mid + 1] == midValue) {
                    mid++;                                  // scan to last match
                }
                return mid;
            }
        }
        return hi;
    }
}
