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
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.index.TermVectorOffsetInfo;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>AbstractExcerpt</code> implements base functionality for an excerpt
 * provider.
 */
public abstract class AbstractExcerpt implements HighlightingExcerptProvider {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(AbstractExcerpt.class);

    /**
     * The search index.
     */
    protected SearchIndex index;

    /**
     * The current query.
     */
    protected Query query;

    /**
     * Indicates whether the query is already rewritten.
     */
    private boolean rewritten = false;

    /**
     * {@inheritDoc}
     */
    public void init(Query query, SearchIndex index) throws IOException {
        this.index = index;
        this.query = query;
    }

    /**
     * {@inheritDoc}
     */
    public String getExcerpt(NodeId id, int maxFragments, int maxFragmentSize)
            throws IOException {
        IndexReader reader = index.getIndexReader();
        try {
            checkRewritten(reader);
            Term idTerm = TermFactory.createUUIDTerm(id.toString());
            TermDocs tDocs = reader.termDocs(idTerm);
            int docNumber;
            Document doc;
            try {
                if (tDocs.next()) {
                    docNumber = tDocs.doc();
                    doc = reader.document(docNumber);
                } else {
                    // node not found in index
                    return null;
                }
            } finally {
                tDocs.close();
            }
            Fieldable[] fields = doc.getFieldables(FieldNames.FULLTEXT);
            if (fields.length == 0) {
                log.debug("Fulltext field not stored, using {}",
                        SimpleExcerptProvider.class.getName());
                SimpleExcerptProvider exProvider = new SimpleExcerptProvider();
                exProvider.init(query, index);
                return exProvider.getExcerpt(id, maxFragments, maxFragmentSize);
            }
            StringBuffer text = new StringBuffer();
            String separator = "";
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].stringValue().length() == 0) {
                    continue;
                }
                text.append(separator);
                text.append(fields[i].stringValue());
                separator = " ";
            }
            TermFreqVector tfv = reader.getTermFreqVector(
                    docNumber, FieldNames.FULLTEXT);
            if (tfv instanceof TermPositionVector) {
                return createExcerpt((TermPositionVector) tfv, text.toString(),
                        maxFragments, maxFragmentSize);
            } else {
                log.debug("No TermPositionVector on Fulltext field.");
                return null;
            }
        } finally {
            Util.closeOrRelease(reader);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String highlight(String text) throws IOException {
        checkRewritten(null);
        return createExcerpt(createTermPositionVector(text),
                text, 1, (text.length() + 1) * 2);
    }

    /**
     * Creates an excerpt for the given <code>text</code> using token offset
     * information provided by <code>tpv</code>.
     *
     * @param tpv             the term position vector for the fulltext field.
     * @param text            the original text.
     * @param maxFragments    the maximum number of fragments to create.
     * @param maxFragmentSize the maximum number of characters in a fragment.
     * @return the xml excerpt.
     * @throws IOException if an error occurs while creating the excerpt.
     */
    protected abstract String createExcerpt(TermPositionVector tpv,
                                            String text,
                                            int maxFragments,
                                            int maxFragmentSize)
            throws IOException;

    /**
     * @return the extracted terms from the query.
     */
    protected final Set<Term[]> getQueryTerms() {
        Set<Term[]> relevantTerms = new HashSet<Term[]>();
        getQueryTerms(query, relevantTerms);
        return relevantTerms;
    }

    private static void getQueryTerms(Query q, Set<Term[]> relevantTerms) {
        if (q instanceof BooleanQuery) {
            final BooleanQuery bq = (BooleanQuery) q;
            for (BooleanClause clause : bq.getClauses()) {
                getQueryTerms(clause.getQuery(), relevantTerms);
            }
            return;
        }
        //need to preserve insertion order
        Set<Term> extractedTerms = new LinkedHashSet<Term>();
        q.extractTerms(extractedTerms);
        Set<Term> filteredTerms = filterRelevantTerms(extractedTerms);
        if (!filteredTerms.isEmpty()) {
            if (q instanceof PhraseQuery) {
                // inline the terms, basically a 'must all' condition
                relevantTerms.add(filteredTerms.toArray(new Term[] {}));
            } else {
                // each possible term gets a new slot
                for (Term t : filteredTerms) {
                    relevantTerms.add(new Term[] { t });
                }
            }
        }
    }

    private static Set<Term> filterRelevantTerms(Set<Term> extractedTerms) {
      //need to preserve insertion order
        Set<Term> relevantTerms = new LinkedHashSet<Term>();
        // only keep terms for fulltext fields
        for (Term t : extractedTerms) {
            if (t.field().equals(FieldNames.FULLTEXT)) {
                relevantTerms.add(t);
            } else {
                int idx = t.field().indexOf(FieldNames.FULLTEXT_PREFIX);
                if (idx != -1) {
                    relevantTerms.add(new Term(FieldNames.FULLTEXT, t.text()));
                }
            }
        }
        return relevantTerms;
    }

    /**
     * Makes sure the {@link #query} is rewritten. If the query is already
     * rewritten, this method returns immediately.
     *
     * @param reader an optional index reader, if none is passed this method
     *               will retrieve one from the {@link #index} and close it
     *               again after the rewrite operation.
     * @throws IOException if an error occurs while the query is rewritten.
     */
    private void checkRewritten(IndexReader reader) throws IOException {
        if (!rewritten) {
            IndexReader r = reader;
            if (r == null) {
                r = index.getIndexReader();
            }
            try {
                query = query.rewrite(r);
            } finally {
                // only close reader if this method opened one
                if (reader == null) {
                    Util.closeOrRelease(r);
                }
            }
            rewritten = true;
        }
    }

    /**
     * @param text the text.
     * @return a <code>TermPositionVector</code> for the given text.
     */
    private TermPositionVector createTermPositionVector(String text) {
        // term -> TermVectorOffsetInfo[]
        final SortedMap<String, TermVectorOffsetInfo[]> termMap =
            new TreeMap<String, TermVectorOffsetInfo[]>();
        Reader r = new StringReader(text);
        TokenStream ts = index.getTextAnalyzer().tokenStream("", r);
        try {
            while (ts.incrementToken()) {
                OffsetAttribute offset = ts.getAttribute(OffsetAttribute.class);
                TermAttribute term = ts.getAttribute(TermAttribute.class);
                String termText = term.term();
                TermVectorOffsetInfo[] info = termMap.get(termText);
                if (info == null) {
                    info = new TermVectorOffsetInfo[1];
                } else {
                    TermVectorOffsetInfo[] tmp = info;
                    info = new TermVectorOffsetInfo[tmp.length + 1];
                    System.arraycopy(tmp, 0, info, 0, tmp.length);
                }
                info[info.length - 1] = new TermVectorOffsetInfo(
                    offset.startOffset(), offset.endOffset());
                termMap.put(termText, info);
            }
            ts.end();
            ts.close();
        } catch (IOException e) {
            // should never happen, we are reading from a string
        }

        return new TermPositionVector() {

            private String[] terms =
                    (String[]) termMap.keySet().toArray(new String[termMap.size()]);

            public int[] getTermPositions(int index) {
                return null;
            }

            public TermVectorOffsetInfo[] getOffsets(int index) {
                TermVectorOffsetInfo[] info = TermVectorOffsetInfo.EMPTY_OFFSET_INFO;
                if (index >= 0 && index < terms.length) {
                    info = termMap.get(terms[index]);
                }
                return info;
            }

            public String getField() {
                return "";
            }

            public int size() {
                return terms.length;
            }

            public String[] getTerms() {
                return terms;
            }

            public int[] getTermFrequencies() {
                int[] freqs = new int[terms.length];
                for (int i = 0; i < terms.length; i++) {
                    freqs[i] = termMap.get(terms[i]).length;
                }
                return freqs;
            }

            public int indexOf(String term) {
                int res = Arrays.binarySearch(terms, term);
                return res >= 0 ? res : -1;
            }

            public int[] indexesOf(String[] terms, int start, int len) {
                int[] res = new int[len];
                for (int i = 0; i < len; i++) {
                    res[i] = indexOf(terms[i]);
                }
                return res;
            }
        };
    }
}
