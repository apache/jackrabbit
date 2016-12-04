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

import junit.framework.TestCase;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * <code>ChainedTermEnumTest</code> implements a test for JCR-2410.
 */
public class ChainedTermEnumTest extends TestCase {

    public void testEnum() throws Exception {
        Collection<TermEnum> enums = new ArrayList<TermEnum>();
        enums.add(createTermEnum("a", 2));
        enums.add(createTermEnum("b", 1));
        enums.add(createTermEnum("c", 0));
        enums.add(createTermEnum("d", 2));
        TermEnum terms = new IndexMigration.ChainedTermEnum(enums);
        List<String> expected = new ArrayList<String>();
        expected.addAll(Arrays.asList("a0", "a1", "b0", "d0", "d1"));
        List<String> result = new ArrayList<String>();
        do {
            Term t = terms.term();
            if (t != null) {
                result.add(t.text());
            }
        } while (terms.next());
        assertEquals(expected, result);
    }

    protected TermEnum createTermEnum(String prefix, int numTerms)
            throws IOException {
        Directory dir = new RAMDirectory();
        IndexWriter writer = new IndexWriter(dir, new IndexWriterConfig(
                Version.LUCENE_36, new StandardAnalyzer(Version.LUCENE_36)));
        try {
            for (int i = 0; i < numTerms; i++) {
                Document doc = new Document();
                doc.add(new Field("field", true, prefix + i, Field.Store.NO,
                        Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO));
                writer.addDocument(doc);
            }
        } finally {
            writer.close();
        }
        IndexReader reader = IndexReader.open(dir);
        try {
            TermEnum terms = reader.terms();
            if (terms.term() == null) {
                // position at first term
                terms.next();
            }
            return terms;
        } finally {
            reader.close();
        }
    }
}
