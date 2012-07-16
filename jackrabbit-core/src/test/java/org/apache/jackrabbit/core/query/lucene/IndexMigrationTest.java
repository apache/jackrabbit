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
import org.apache.jackrabbit.core.query.lucene.directory.DirectoryManager;
import org.apache.jackrabbit.core.query.lucene.directory.RAMDirectoryManager;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * <code>IndexMigrationTest</code> contains a test case for JCR-2393.
 */
public class IndexMigrationTest extends TestCase {

    /**
     * Cannot use \uFFFF because of LUCENE-1221.
     */
    private static final char SEP_CHAR = '\uFFFE';

    public void testMigration() throws Exception {
        List<Document> docs = new ArrayList<Document>();
        docs.add(createDocument("ab", "a"));
        docs.add(createDocument("a", "b"));
        docs.add(createDocument("abcd", "c"));
        docs.add(createDocument("abc", "d"));

        DirectoryManager dirMgr = new RAMDirectoryManager();

        PersistentIndex idx = new PersistentIndex("index",
                new StandardAnalyzer(Version.LUCENE_36), Similarity.getDefault(),
                new DocNumberCache(100),
                new IndexingQueue(new IndexingQueueStore(new RAMDirectory())),
                dirMgr, 0);
        idx.addDocuments(docs.toArray(new Document[docs.size()]));
        idx.commit();

        IndexMigration.migrate(idx, dirMgr, SEP_CHAR);
    }

    protected static String createNamedValue14(String name, String value) {
        return name + SEP_CHAR + value;
    }

    protected static Document createDocument(String name, String value) {
        Document doc = new Document();
        doc.add(new Field(FieldNames.UUID, false, UUID.randomUUID().toString(),
                Field.Store.YES, Field.Index.NO, Field.TermVector.NO));
        doc.add(new Field(FieldNames.PROPERTIES, false, createNamedValue14(
                name, value), Field.Store.NO, Field.Index.NOT_ANALYZED,
                Field.TermVector.NO));
        doc.add(new Field(FieldNames.FULLTEXT_PREFIX + ":" + name, true, value,
                Field.Store.NO, Field.Index.ANALYZED,
                Field.TermVector.WITH_POSITIONS_OFFSETS));
        return doc;
    }
}
