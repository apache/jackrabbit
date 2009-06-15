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
package org.apache.jackrabbit.core.query.lucene.constraint;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.commons.query.qom.SelectorImpl;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.core.query.lucene.ScoreNode;
import org.apache.jackrabbit.core.query.lucene.QueryHits;
import org.apache.jackrabbit.core.query.lucene.Util;
import org.apache.jackrabbit.core.query.lucene.LuceneQueryFactory;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.index.IndexReader;

/**
 * <code>QueryConstraint</code> implements a constraint that is based on a
 * lucene query.
 */
public abstract class QueryConstraint extends SelectorBasedConstraint {

    /**
     * The constraint query.
     */
    private final Query constraint;

    /**
     * The lucene query factory.
     */
    private final LuceneQueryFactory factory;

    /**
     * Map of document numbers with their respective score value that match the
     * query constraint.
     */
    private Map<Integer, Float> matches;

    /**
     * Creates a new query constraint using the given lucene query.
     *
     * @param constraint the lucene query constraint.
     * @param selector   the selector for this constraint.
     * @param factory    the lucene query factory.
     */
    public QueryConstraint(Query constraint,
                           SelectorImpl selector,
                           LuceneQueryFactory factory) {
        super(selector);
        this.constraint = constraint;
        this.factory = factory;
    }

    //----------------------------< Constraint >--------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean evaluate(ScoreNode[] row,
                            Name[] selectorNames,
                            EvaluationContext context)
            throws IOException {
        ScoreNode sn = row[getSelectorIndex(selectorNames)];
        return sn != null && evaluate(sn, context);
    }

    //--------------------------------< internal >------------------------------

    /**
     * Evaluates this constraint for the given score node <code>sn</code>.
     *
     * @param sn      the current score node.
     * @param context the evaluation context.
     * @return <code>true</code> if this constraint is satisfied for the given
     *         score node <code>sn</code>; <code>false</code> otherwise.
     * @throws IOException if an error occurs while reading from the index.
     */
    private boolean evaluate(ScoreNode sn, EvaluationContext context)
            throws IOException {
        initMatches(context);
        Float score = matches.get(sn.getDoc(context.getIndexReader()));
        if (score != null) {
            sn.setScore(score);
        }
        return score != null;
    }

    /**
     * Initializes the matches for the constraint query. If the matches are
     * already initialized then this method returns immediately.
     *
     * @param context the evaluation context.
     * @throws IOException if an error occurs while reading from the index.
     */
    private void initMatches(EvaluationContext context) throws IOException {
        if (matches == null) {
            Query selectorQuery;
            BooleanQuery and = new BooleanQuery();
            try {
                selectorQuery = factory.create(getSelector());
                and.add(selectorQuery, BooleanClause.Occur.MUST);
                and.add(constraint, BooleanClause.Occur.MUST);
            } catch (RepositoryException e) {
                throw Util.createIOException(e);
            }

            IndexReader reader = context.getIndexReader();
            QueryHits hits = context.evaluate(and);
            try {
                matches = new HashMap<Integer, Float>();
                ScoreNode sn;
                while ((sn = hits.nextScoreNode()) != null) {
                    matches.put(sn.getDoc(reader), sn.getScore());
                }
            } finally {
                hits.close();
            }
        }
    }
}
