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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.Weight;

/**
 * <code>FilterSearcher</code> wraps another Searcher and forwards all
 * calls to the wrapped Searcher.
 */
class FilterSearcher extends Searcher {

    private Searcher s;

    FilterSearcher(Searcher searcher) {
        this.s = searcher;
    }

    @Override
    public void search(Weight weight, Filter filter, Collector results)
            throws IOException {
        s.search(weight, filter, results);
    }

    @Override
    public void close() throws IOException {
        s.close();
    }

    @Override
    public int docFreq(Term term) throws IOException {
        return s.docFreq(term);
    }

    @Override
    public int maxDoc() throws IOException {
        return s.maxDoc();
    }

    @Override
    public TopDocs search(Weight weight, Filter filter, int n)
            throws IOException {
        return s.search(weight, filter, n);
    }

    @Override
    public Document doc(int i) throws CorruptIndexException, IOException {
        return s.doc(i);
    }

    @Override
    public Document doc(int docid, FieldSelector fieldSelector)
            throws CorruptIndexException, IOException {
        return s.doc(docid, fieldSelector);
    }

    @Override
    public Query rewrite(Query query) throws IOException {
        return s.rewrite(query);
    }

    @Override
    public Explanation explain(Weight weight, int doc) throws IOException {
        return s.explain(weight, doc);
    }

    @Override
    public TopFieldDocs search(Weight weight, Filter filter, int n, Sort sort)
            throws IOException {
        return null;
    }
}
