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

import org.apache.jackrabbit.core.query.lucene.QueryHits;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.lucene.search.Query;
import org.apache.lucene.index.IndexReader;

/**
 * <code>EvaluationContext</code> defines a context with various resources that
 * are needed for constraint evaluation.
 */
public interface EvaluationContext {

    /**
     * Evaluates the given lucene <code>query</code> and returns the query
     * hits.
     *
     * @param query the lucene query to evaluate.
     * @return the query hits for the given <code>query</code>.
     * @throws IOException if an error occurs while reading from the index.
     */
    public QueryHits evaluate(Query query) throws IOException;

    /**
     * @return the index reader.
     */
    public IndexReader getIndexReader();

    /**
     * @return the session that executes the query.
     */
    public SessionImpl getSession();

    /**
     * @return the shared item state manager of the current workspace.
     */
    public ItemStateManager getItemStateManager();
}
