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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

/**
 * <code>SimilarityQuery</code> implements a query that returns similar nodes
 * for a given node UUID.
 */
@SuppressWarnings("serial")
public class SimilarityQuery extends Query {

    /**
     * The UUID of the node for which to find similar nodes.
     */
    private final String uuid;

    /**
     * The analyzer in use.
     */
    private final Analyzer analyzer;

    public SimilarityQuery(String uuid, Analyzer analyzer) {
        this.uuid = uuid;
        this.analyzer = analyzer;
    }

    /**
     * {@inheritDoc}
     */
    public Query rewrite(IndexReader reader) throws IOException {
        MoreLikeThis more = new MoreLikeThis(reader);
        more.setAnalyzer(analyzer);
        more.setFieldNames(new String[]{FieldNames.FULLTEXT});
        more.setMinWordLen(4);
        Query similarityQuery = null;
        TermDocs td = reader.termDocs(TermFactory.createUUIDTerm(uuid));
        try {
            if (td.next()) {
                similarityQuery = more.like(td.doc());
            }
        } finally {
            td.close();
        }
        if (similarityQuery != null) {
            return similarityQuery.rewrite(reader);
        } else {
            // return dummy query that never matches
            return new BooleanQuery();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString(String field) {
        return "rep:similar(" + uuid + ")";
    }
}
