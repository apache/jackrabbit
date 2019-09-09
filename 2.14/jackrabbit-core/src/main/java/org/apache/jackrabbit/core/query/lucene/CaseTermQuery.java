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

import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.MultiTermQuery;
import org.apache.lucene.search.FilteredTermEnum;
import org.apache.lucene.util.ToStringUtils;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <code>CaseTermQuery</code> implements a term query which convert the term
 * from the index either to upper or lower case before it is matched.
 */
@SuppressWarnings("serial")
abstract class CaseTermQuery extends MultiTermQuery implements TransformConstants {

    /**
     * Indicates whether terms from the index should be lower-cased or
     * upper-cased.
     */
    protected final int transform;
    private final Term term;

    CaseTermQuery(Term term, int transform) {
        this.term = term;
        this.transform = transform;
        setRewriteMethod(CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected FilteredTermEnum getEnum(IndexReader reader) throws IOException {
        return new CaseTermEnum(reader);
    }

    /** Prints a user-readable version of this query. */
    @Override
    public String toString(String field) {
        StringBuffer buffer = new StringBuffer();
        if (!term.field().equals(field)) {
            buffer.append(term.field());
            buffer.append(':');
        }
        buffer.append(term.text());
        buffer.append(ToStringUtils.boost(getBoost()));
        return buffer.toString();
    }

    static final class Upper extends CaseTermQuery {

        Upper(Term term) {
            super(term, TRANSFORM_UPPER_CASE);
        }
    }

    static final class Lower extends CaseTermQuery {

        Lower(Term term) {
            super(term, TRANSFORM_LOWER_CASE);
        }
    }

    private final class CaseTermEnum extends FilteredTermEnum {

        CaseTermEnum(IndexReader reader) throws IOException {
            // gather all terms that match
            // keep them in order and remember the doc frequency as value
            final Map<Term, Integer> orderedTerms =
                new LinkedHashMap<Term, Integer>();

            // there are always two range scans: one with an initial
            // lower case character and another one with an initial upper case
            // character
            List<RangeScan> rangeScans = new ArrayList<RangeScan>(2);
            int nameLength = FieldNames.getNameLength(term.text());
            String propName = term.text().substring(0, nameLength);
            OffsetCharSequence termText = new OffsetCharSequence(nameLength, term.text());
            OffsetCharSequence currentTerm = new OffsetCharSequence(nameLength, term.text(), transform);

            try {
                // start with a term using the lower case character for the first
                // character of the value.
                if (term.text().length() > nameLength) {
                    // start with initial lower case
                    StringBuffer lowerLimit = new StringBuffer(propName);
                    String termStr = termText.toString();
                    String upperTermStr = termStr.toUpperCase();
                    String lowerTermStr = termStr.toLowerCase();
                    
                    lowerLimit.append(upperTermStr);
                    lowerLimit.setCharAt(nameLength, Character.toLowerCase(lowerLimit.charAt(nameLength)));
                    StringBuffer upperLimit = new StringBuffer(propName);
                    upperLimit.append(lowerTermStr);
                    rangeScans.add(new RangeScan(reader,
                            new Term(term.field(), lowerLimit.toString()),
                            new Term(term.field(), upperLimit.toString())));

                    // second scan with upper case start
                    lowerLimit = new StringBuffer(propName);
                    lowerLimit.append(upperTermStr);
                    upperLimit = new StringBuffer(propName);
                    upperLimit.append(lowerTermStr);
                    upperLimit.setCharAt(nameLength, Character.toUpperCase(upperLimit.charAt(nameLength)));
                    rangeScans.add(new RangeScan(reader,
                            new Term(term.field(), lowerLimit.toString()),
                            new Term(term.field(), upperLimit.toString())));

                } else {
                    // use term as is
                    rangeScans.add(new RangeScan(reader, term, term));
                }

                for (TermEnum terms : rangeScans) {
                    do {
                        Term t = terms.term();
                        if (t != null) {
                            currentTerm.setBase(t.text());
                            int compare = currentTerm.compareTo(termText);
                            if (compare == 0) {
                                orderedTerms.put(t, terms.docFreq());
                            }
                        } else {
                            break;
                        }
                    } while (terms.next());
                }
            } finally {
                for (TermEnum terms : rangeScans) {
                    try {
                        terms.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }

            final Iterator<Term> it = orderedTerms.keySet().iterator();

            setEnum(new TermEnum() {

                private Term current;

                {
                    getNext();
                }

                @Override
                public boolean next() {
                    getNext();
                    return current != null;
                }

                @Override
                public Term term() {
                    return current;
                }

                @Override
                public int docFreq() {
                    Integer docFreq = orderedTerms.get(current);
                    return docFreq != null ? docFreq : 0;
                }

                @Override
                public void close() {
                    // nothing to close
                }

                private void getNext() {
                    current = it.hasNext() ? it.next() : null;
                }
            });
        }

        @Override
        protected boolean termCompare(Term term) {
            // they all match
            return true;
        }

        @Override
        public float difference() {
            return 1.0f;
        }

        @Override
        protected boolean endEnum() {
            // todo correct?
            return false;
        }
    }
}
