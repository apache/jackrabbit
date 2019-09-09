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
    private int currentScorer;

    /**
     * The next document id to be returned
     */
    private int currentDoc = -1;

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

    @Override
    public int nextDoc() throws IOException {
        while (currentDoc != NO_MORE_DOCS) {
            if (scorers[currentScorer] != null && scorers[currentScorer].nextDoc() != NO_MORE_DOCS) {
                currentDoc = scorers[currentScorer].docID() + starts[currentScorer];
                return currentDoc;
            } else if (++currentScorer < scorers.length) {
                // advance to next scorer
            } else {
                // no more scorers
                currentDoc = NO_MORE_DOCS;
            }
        }
        return currentDoc;
    }

    @Override
    public int docID() {
        return currentDoc;
    }

    @Override
    public float score() throws IOException {
        return scorers[currentScorer].score();
    }

    @Override
    public int advance(int target) throws IOException {
        if (currentDoc == NO_MORE_DOCS) {
            return currentDoc;
        }
        // optimize in the case of an advance to finish.
        // see https://issues.apache.org/jira/browse/JCR-3091
        if (target == NO_MORE_DOCS) {
            // exhaust all the internal scorers
            for (Scorer s : scorers) {
                if (s.docID() != target) {
                    s.advance(target);
                }
            }
            currentDoc = NO_MORE_DOCS;
            return currentDoc;
        }

        currentScorer = scorerIndex(target);
        if (scorers[currentScorer].advance(target - starts[currentScorer]) != NO_MORE_DOCS) {
            currentDoc = scorers[currentScorer].docID() + starts[currentScorer];
            return currentDoc;
        } else {
            if (++currentScorer < scorers.length) {
                // simply move to the next if there is any
                currentDoc = nextDoc();
                return currentDoc;
            } else {
                // no more document
                currentDoc = NO_MORE_DOCS;
                return currentDoc;
            }
        }
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
