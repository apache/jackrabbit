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
import java.util.Arrays;

import org.apache.jackrabbit.core.query.lucene.constraint.Constraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>FilterMultiColumnQuery</code> wraps a multi column query and filters
 * out rows that do not satisfy a given constraint.
 */
public class FilterMultiColumnQuery implements MultiColumnQuery {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(FilterMultiColumnQuery.class);

    /**
     * The query to filter.
     */
    private final MultiColumnQuery query;

    /**
     * The constraint for filtering.
     */
    private final Constraint constraint;

    /**
     * Creates a new filter multi column query for the given <code>query</code>
     * and <code>constraint</code>.
     *
     * @param query      the query to filter.
     * @param constraint the constraint for filtering.
     */
    public FilterMultiColumnQuery(MultiColumnQuery query,
                                  Constraint constraint) {
        this.query = query;
        this.constraint = constraint;
    }

    /**
     * {@inheritDoc}
     */
    public MultiColumnQueryHits execute(final JackrabbitIndexSearcher searcher,
                                        Ordering[] orderings,
                                        long resultFetchHint)
            throws IOException {
        MultiColumnQueryHits hits = new FilterMultiColumnQueryHits(query.execute(
                searcher, orderings, resultFetchHint)) {

            {
                log.debug(Arrays.asList(getSelectorNames()).toString());
            }

            public ScoreNode[] nextScoreNodes() throws IOException {
                ScoreNode[] next;
                do {
                    next = super.nextScoreNodes();
                    if (log.isDebugEnabled()) {
                        if (next != null) {
                            log.debug(Arrays.asList(next).toString());
                        }
                    }
                } while (next != null && !constraint.evaluate(next, getSelectorNames(), searcher));
                return next;
            }

            public int getSize() {
                return -1;
            }

            public void skip(int n) throws IOException {
                while (n-- > 0) {
                    nextScoreNodes();
                }
            }
        };
        if (orderings.length > 0) {
            hits = new SortedMultiColumnQueryHits(hits, orderings, searcher.getIndexReader());
        }
        return hits;
    }
}
