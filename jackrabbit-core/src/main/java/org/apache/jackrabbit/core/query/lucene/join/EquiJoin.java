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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private final Map<String, List<ScoreNode[]>> rowsByInnerNodeValue =
        new HashMap<String, List<ScoreNode[]>>();

    private final Map<Integer, Set<String>> valuesByOuterNodeDocument =
        new HashMap<Integer, Set<String>>();

    /**
     * Creates a new equi join condition.
     *
     * @param inner               the inner query hits.
     * @param innerScoreNodeIndex the selector name for the inner query hits.
     * @param nsMappings          the namespace mappings
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
        String innerName = nsMappings.translateName(innerProperty);
        for (Map.Entry<Term, String> entry : getPropertyTerms(innerName)) {
            String value = entry.getValue();
            TermDocs docs = reader.termDocs(entry.getKey());
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
        }

        // Build the valuesByOuterNodeDocument map for efficient lookup in
        // the getMatchingScoreNodes() method
        String outerName = nsMappings.translateName(outerProperty);
        for (Map.Entry<Term, String> entry : getPropertyTerms(outerName)) {
            String value = entry.getValue();
            TermDocs docs = reader.termDocs(entry.getKey());
            while (docs.next()) {
                Set<String> values = valuesByOuterNodeDocument.get(docs.doc());
                if (values == null) {
                    values = new HashSet<String>();
                    valuesByOuterNodeDocument.put(docs.doc(), values);
                }
                values.add(value);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode[][] getMatchingScoreNodes(ScoreNode outer)
            throws IOException {
        List<ScoreNode[]> list = new ArrayList<ScoreNode[]>();

        Set<String> values = valuesByOuterNodeDocument.get(outer.getDoc(reader));
        if (values != null) {
            for (String value : values) {
                List<ScoreNode[]> rows = rowsByInnerNodeValue.get(value);
                if (rows != null) {
                    list.addAll(rows);
                }
            }
        }

        return list.toArray(new ScoreNode[list.size()][]);
    }

    private Set<Map.Entry<Term, String>> getPropertyTerms(String property)
            throws IOException {
        Map<Term, String> map = new HashMap<Term, String>();

        Term prefix = new Term(
                FieldNames.PROPERTIES,
                FieldNames.createNamedValue(property, ""));
        TermEnum terms = reader.terms(prefix);
        do {
            Term term = terms.term();
            if (term == null
                    || !term.field().equals(prefix.field())
                    || !term.text().startsWith(prefix.text())) {
                break;
            }
            map.put(term, term.text().substring(prefix.text().length()));
        } while (terms.next());

        return map.entrySet();
    }

}
