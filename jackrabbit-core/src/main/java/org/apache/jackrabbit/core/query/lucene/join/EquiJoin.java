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
package org.apache.jackrabbit.core.query.lucene.join;

import static org.apache.jackrabbit.core.query.lucene.FieldNames.PROPERTIES;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.query.lucene.MultiColumnQueryHits;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.core.query.lucene.ScoreNode;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;

/**
 * <code>EquiJoin</code> implements an equi join condition.
 */
public class EquiJoin extends AbstractCondition {

    /**
     * The index reader.
     */
    private final IndexReader reader;

    private final Term outerTerm;

    private final Map<String, List<ScoreNode[]>> rowsByInnerNodeValue =
        new HashMap<String, List<ScoreNode[]>>();

    /**
     * Creates a new equi join condition.
     *
     * @param inner               the inner query hits.
     * @param innerScoreNodeIndex the selector name for the inner query hits.
     * @param scs                 the sort comparator source.
     * @param reader              the index reader.
     * @param innerProperty       the name of the property of the inner query
     *                            hits.
     * @param outerProperty       the name of the property of the outer query
     *                            hits.
     * @throws IOException if an error occurs while reading from the index.
     * @throws IllegalNameException 
     */
    public EquiJoin(
            MultiColumnQueryHits inner, int innerScoreNodeIndex,
            NamespaceMappings nsMappings, IndexReader reader,
            Name innerProperty, Name outerProperty)
            throws IOException, IllegalNameException {
        super(inner);
        this.reader = reader;

        Term innerTerm = new Term(PROPERTIES, FieldNames.createNamedValue(
                nsMappings.translateName(innerProperty), ""));
        this.outerTerm = new Term(PROPERTIES, FieldNames.createNamedValue(
                nsMappings.translateName(outerProperty), ""));

        // create lookup map
        Map<Integer, List<ScoreNode[]>> rowsByInnerDocument =
            new HashMap<Integer, List<ScoreNode[]>>();
        ScoreNode[] row = inner.nextScoreNodes();
        while (row != null) {
            int document = row[innerScoreNodeIndex].getDoc(reader);
            List<ScoreNode[]> rows = rowsByInnerDocument.get(document);
            if (rows == null) {
                rows = new ArrayList<ScoreNode[]>();
                rowsByInnerDocument.put(document, rows);
            }
            rows.add(row);
            row = inner.nextScoreNodes();
        }

        // Build the rowsByInnerNodeValue map for efficient lookup in
        // the getMatchingScoreNodes() method
        TermEnum terms = reader.terms(innerTerm);
        do {
            Term term = terms.term();
            if (term == null
                    || !term.field().equals(innerTerm.field())
                    || !term.text().startsWith(innerTerm.text())) {
                break;
            }

            String value = term.text().substring(innerTerm.text().length());
            TermDocs docs = reader.termDocs(terms.term());
            while (docs.next()) {
                List<ScoreNode[]> match = rowsByInnerDocument.get(docs.doc());
                if (match != null) {
                    List<ScoreNode[]> rows = rowsByInnerNodeValue.get(value); 
                    if (rows == null) {
                        rows = new ArrayList<ScoreNode[]>();
                        rowsByInnerNodeValue.put(value, rows);
                    }
                    rows.addAll(match);
                }
            }
        } while (terms.next());
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode[][] getMatchingScoreNodes(ScoreNode outer)
            throws IOException {
        List<ScoreNode[]> list = new ArrayList<ScoreNode[]>();

        int document = outer.getDoc(reader);
        TermEnum terms = reader.terms(outerTerm);
        do {
            Term term = terms.term();
            if (term == null
                    || !term.field().equals(outerTerm.field())
                    || !term.text().startsWith(outerTerm.text())) {
                break;
            }

            List<ScoreNode[]> rows = rowsByInnerNodeValue.get(
                    terms.term().text().substring(outerTerm.text().length()));
            if (rows != null) {
                TermDocs docs = reader.termDocs(terms.term());
                while (docs.next()) {
                    if (docs.doc() == document) {
                        list.addAll(rows);
                        break;
                    }
                }
            }
        } while (terms.next());

        return list.toArray(new ScoreNode[list.size()][]);
    }

}
