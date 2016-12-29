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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.jackrabbit.core.query.lucene.directory.DirectoryManager;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.KeepOnlyLastCommitDeletionPolicy;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.index.TermPositions;
import org.apache.lucene.index.UpgradeIndexMergePolicy;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.ReaderUtil;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>IndexMigration</code> implements a utility that migrates a Jackrabbit
 * 1.4.x index to version 1.5. Until version 1.4.x, indexes used the character
 * '\uFFFF' to separate the name of a property from the value. As of Lucene
 * 2.3 this does not work anymore. See LUCENE-1221. Jackrabbit &gt;= 1.5 uses
 * the character '[' as a separator. Whenever an index is opened from disk, a
 * quick check is run to find out whether a migration is required. See also
 * JCR-1363 for more details.
 */
public class IndexMigration {

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(IndexMigration.class);

    /**
     * Checks if the given <code>index</code> needs to be migrated.
     *
     * @param index the index to check and migration if needed.
     * @param directoryManager the directory manager.
     * @param oldSeparatorChar the old separator char that needs to be replaced.
     * @throws IOException if an error occurs while migrating the index.
     */
    public static void migrate(PersistentIndex index,
                               DirectoryManager directoryManager,
                               char oldSeparatorChar)
            throws IOException {
        Directory indexDir = index.getDirectory();
        log.debug("Checking {} ...", indexDir);
        ReadOnlyIndexReader reader = index.getReadOnlyIndexReader();
        try {
            if (IndexFormatVersion.getVersion(reader).getVersion() >=
                    IndexFormatVersion.V3.getVersion()) {
                // index was created with Jackrabbit 1.5 or higher
                // no need for migration
                log.debug("IndexFormatVersion >= V3, no migration needed");
                return;
            }
            // assert: there is at least one node in the index, otherwise the
            //         index format version would be at least V3
            TermEnum terms = reader.terms(new Term(FieldNames.PROPERTIES, ""));
            try {
                Term t = terms.term();
                if (t.text().indexOf(oldSeparatorChar) == -1) {
                    log.debug("Index already migrated");
                    return;
                }
            } finally {
                terms.close();
            }
        } finally {
            reader.release();
            index.releaseWriterAndReaders();
        }

        // if we get here then the index must be migrated
        log.debug("Index requires migration {}", indexDir);

        String migrationName = index.getName() + "_v36";
        if (directoryManager.hasDirectory(migrationName)) {
            directoryManager.delete(migrationName);
        }

        Directory migrationDir = directoryManager.getDirectory(migrationName);
        final IndexWriterConfig c = new IndexWriterConfig(Version.LUCENE_36, new JackrabbitAnalyzer());
        c.setMergePolicy(new UpgradeIndexMergePolicy(new LogByteSizeMergePolicy()));
        c.setIndexDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy()); 
        try {
            IndexWriter writer = new IndexWriter(migrationDir, c);
            try {
                IndexReader r = new MigrationIndexReader(IndexReader.open(index.getDirectory()),
                        oldSeparatorChar);
                try {
                    writer.addIndexes(r);
                    writer.forceMerge(1);
                    writer.close();
                } finally {
                    r.close();
                }
            } finally {
                writer.close();
            }
        } finally {
            migrationDir.close();
        }
        directoryManager.delete(index.getName());
        if (!directoryManager.rename(migrationName, index.getName())) {
            throw new IOException("failed to move migrated directory " + migrationDir);
        }
        log.info("Migrated " + index.getName());
    }

    //---------------------------< internal helper >----------------------------

    /**
     * An index reader that migrates stored field values and term text on the
     * fly.
     */
    private static class MigrationIndexReader extends FilterIndexReader {

        private final char oldSepChar;

        public MigrationIndexReader(IndexReader in, char oldSepChar) {
            super(in);
            this.oldSepChar = oldSepChar;
        }

        @Override
        public IndexReader[] getSequentialSubReaders() {
            return null;
        }

        @Override
        public FieldInfos getFieldInfos() {
            return ReaderUtil.getMergedFieldInfos(in);
        }

        @Override
        public Document document(int n, FieldSelector fieldSelector)
                throws CorruptIndexException, IOException {
            Document doc = super.document(n, fieldSelector);
            Fieldable[] fields = doc.getFieldables(FieldNames.PROPERTIES);
            if (fields != null) {
                doc.removeFields(FieldNames.PROPERTIES);
                for (Fieldable field : fields) {
                    String value = field.stringValue();
                    value = value.replace(oldSepChar, '[');
                    doc.add(new Field(FieldNames.PROPERTIES, false, value,
                            Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS,
                            Field.TermVector.NO));
                }
            }
            return doc;
        }

        @Override
        public TermEnum terms() throws IOException {
            List<TermEnum> enums = new ArrayList<TermEnum>();
            List<String> fieldNames = new ArrayList<String>(ReaderUtil.getIndexedFields(in));
            Collections.sort(fieldNames);
            for (String fieldName : fieldNames) {
                if (fieldName.equals(FieldNames.PROPERTIES)) {
                    addPropertyTerms(enums);
                } else {
                    enums.add(new RangeScan(in, new Term(fieldName, ""), new Term(fieldName, "\uFFFF")));
                }
            }
            return new MigrationTermEnum(new ChainedTermEnum(enums), oldSepChar);
        }

        @Override
        public TermPositions termPositions() throws IOException {
            return new MigrationTermPositions(in.termPositions(), oldSepChar);
        }

        private void addPropertyTerms(List<TermEnum> enums) throws IOException {
            SortedMap<String, TermEnum> termEnums = new TreeMap<String, TermEnum>(
                    new Comparator<String>() {
                        public int compare(String s1, String s2) {
                            s1 = s1.replace(oldSepChar, '[');
                            s2 = s2.replace(oldSepChar, '[');
                            return s1.compareTo(s2);
                        }
            });
            // scan through terms and find embedded field names
            TermEnum terms = new RangeScan(in,
                    new Term(FieldNames.PROPERTIES, ""),
                    new Term(FieldNames.PROPERTIES, "\uFFFF"));
            String previous = null;
            while (terms.next()) {
                Term t = terms.term();
                String name = t.text().substring(0, t.text().indexOf(oldSepChar) + 1);
                if (!name.equals(previous)) {
                    termEnums.put(name, new RangeScan(in,
                            new Term(FieldNames.PROPERTIES, name),
                            new Term(FieldNames.PROPERTIES, name + "\uFFFF")));
                }
                previous = name;
            }
            enums.addAll(termEnums.values());
        }

        private static class MigrationTermEnum extends FilterTermEnum {

            private final char oldSepChar;

            public MigrationTermEnum(TermEnum in, char oldSepChar) {
                super(in);
                this.oldSepChar = oldSepChar;
            }

            public Term term() {
                Term t = super.term();
                if (t == null) {
                    return t;
                }
                if (t.field().equals(FieldNames.PROPERTIES)) {
                    String text = t.text();
                    return t.createTerm(text.replace(oldSepChar, '['));
                } else {
                    return t;
                }
            }

            TermEnum unwrap() {
                return in;
            }
        }

        private static class MigrationTermPositions extends FilterTermPositions {

            private final char oldSepChar;

            public MigrationTermPositions(TermPositions in, char oldSepChar) {
                super(in);
                this.oldSepChar = oldSepChar;
            }

            public void seek(Term term) throws IOException {
                if (term.field().equals(FieldNames.PROPERTIES)) {
                    char[] text = term.text().toCharArray();
                    text[term.text().indexOf('[')] = oldSepChar;
                    super.seek(term.createTerm(new String(text)));
                } else {
                    super.seek(term);
                }
            }

            public void seek(TermEnum termEnum) throws IOException {
                if (termEnum instanceof MigrationTermEnum) {
                    super.seek(((MigrationTermEnum) termEnum).unwrap());
                } else {
                    super.seek(termEnum);
                }
            }
        }
    }

    static final class ChainedTermEnum extends TermEnum {

        private Queue<TermEnum> queue = new LinkedList<TermEnum>();

        public ChainedTermEnum(Collection<TermEnum> enums) {
            super();
            queue.addAll(enums);
        }

        public boolean next() throws IOException {
            boolean newEnum = false;
            for (;;) {
                TermEnum terms = queue.peek();
                if (terms == null) {
                    // no more enums
                    break;
                }
                if (newEnum && terms.term() != null) {
                    // need to check if enum is already positioned
                    // at first term
                    return true;
                }
                if (terms.next()) {
                    return true;
                } else {
                    queue.remove();
                    terms.close();
                    newEnum = true;
                }
            }
            return false;
        }

        public Term term() {
            TermEnum terms = queue.peek();
            if (terms != null) {
                return terms.term();
            }
            return null;
        }

        public int docFreq() {
            TermEnum terms = queue.peek();
            if (terms != null) {
                return terms.docFreq();
            }
            return 0;
        }

        public void close() throws IOException {
            // close remaining
            while (!queue.isEmpty()) {
                queue.remove().close();
            }
        }
    }
}
