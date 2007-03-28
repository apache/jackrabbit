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

import org.apache.jackrabbit.core.NodeId;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.search.Query;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <code>DefaultXMLExcerpt</code> implements an ExcerptProvider.
 */
class DefaultXMLExcerpt implements ExcerptProvider {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(DefaultXMLExcerpt.class);

    /**
     * The search index.
     */
    private SearchIndex index;

    /**
     * The current query.
     */
    private Query query;

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
            Term idTerm = new Term(FieldNames.UUID, id.getUUID().toString());
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
            Field[] fields = doc.getFields(FieldNames.FULLTEXT);
            if (fields == null) {
                log.debug("Fulltext field not stored, using {}",
                        SimpleExcerptProvider.class.getName());
                SimpleExcerptProvider exProvider = new SimpleExcerptProvider();
                exProvider.init(query, index);
                return exProvider.getExcerpt(id, maxFragments, maxFragmentSize);
            }
            StringBuffer text = new StringBuffer();
            String separator = "";
            for (int i = 0; i < fields.length; i++) {
                text.append(separator);
                text.append(fields[i].stringValue());
                // this is a hack! in general multiple fields with the same
                // name are handled properly, that is, offset and position is
                // calculated correctly. there is one case however where
                // the offset gets wrong:
                // if a term text ends with characters that are considered noise
                // then the offset of the next field will be off by the number
                // of noise characters.
                // therefore we delete noise characters at the end of the text
                for (int j = text.length() - 1; j >= 0; j--) {
                    if (Character.isLetterOrDigit(text.charAt(j))) {
                        break;
                    } else {
                        text.deleteCharAt(j);
                    }
                }
                separator = " ";
            }
            TermFreqVector tfv = reader.getTermFreqVector(
                    docNumber, FieldNames.FULLTEXT);
            if (tfv instanceof TermPositionVector) {
                return createExcerpt((TermPositionVector) tfv, text.toString(),
                        maxFragments, maxFragmentSize);
            } else {
                log.debug("No TermPositionVector on Fulltext field, using {}",
                        SimpleExcerptProvider.class.getName());
                SimpleExcerptProvider exProvider = new SimpleExcerptProvider();
                exProvider.init(query, index);
                return exProvider.getExcerpt(id, maxFragments, maxFragmentSize);
            }
        } finally {
            reader.close();
        }
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
    private String createExcerpt(TermPositionVector tpv,
                                 String text,
                                 int maxFragments,
                                 int maxFragmentSize)
            throws IOException {
        return DefaultHighlighter.highlight(tpv, query, FieldNames.FULLTEXT,
                text, "<highlight>", "</highlight>", maxFragments, maxFragmentSize / 2);
    }
}
