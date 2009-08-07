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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.search.Similarity;
import org.apache.jackrabbit.core.query.lucene.directory.DirectoryManager;

import java.io.IOException;

/**
 * Implements a lucene index which is based on a
 * {@link org.apache.jackrabbit.core.fs.FileSystem}.
 */
class PersistentIndex extends AbstractIndex {

    /** The name of this persistent index */
    private final String name;

    /**
     * If non <code>null</code>, <code>listener</code> needs to be informed
     * when a document is deleted.
     */
    private IndexListener listener;

    /**
     * Creates a new <code>PersistentIndex</code>.
     *
     * @param name the name of this index.
     * @param analyzer the analyzer for text tokenizing.
     * @param similarity the similarity implementation.
     * @param cache the document number cache
     * @param indexingQueue the indexing queue.
     * @param directoryManager the directory manager.
     * @throws IOException if an error occurs while opening / creating the
     *  index.
     */
    PersistentIndex(String name, Analyzer analyzer,
                    Similarity similarity, DocNumberCache cache,
                    IndexingQueue indexingQueue,
                    DirectoryManager directoryManager)
            throws IOException {
        super(analyzer, similarity, directoryManager.getDirectory(name),
                cache, indexingQueue);
        this.name = name;
        if (isExisting()) {
            IndexMigration.migrate(this, directoryManager);
        }
    }

    /**
     * @inheritDoc
     */
    int removeDocument(Term idTerm) throws IOException {
        int num = super.removeDocument(idTerm);
        if (num > 0 && listener != null) {
            listener.documentDeleted(idTerm);
        }
        return num;
    }

    /**
     * Merges the provided indexes into this index. After this completes, the
     * index is optimized.
     * <p/>
     * The provided IndexReaders are not closed.
     *
     * @param readers the readers of indexes to add.
     * @throws IOException if an error occurs while adding indexes.
     */
    void addIndexes(IndexReader[] readers) throws IOException {
        getIndexWriter().addIndexes(readers);
        getIndexWriter().optimize();
    }

    /**
     * Copies <code>index</code> into this persistent index. This method should
     * only be called when <code>this</code> index is empty otherwise the
     * behaviour is undefined.
     *
     * @param index the index to copy from.
     * @throws IOException if an error occurs while copying.
     */
    void copyIndex(AbstractIndex index) throws IOException {
        // commit changes to directory on other index.
        index.commit(true);
        // simply copy over the files
        byte[] buffer = new byte[1024];
        Directory dir = index.getDirectory();
        Directory dest = getDirectory();
        String[] files = dir.list();
        for (int i = 0; i < files.length; i++) {
            IndexInput in = dir.openInput(files[i]);
            try {
                IndexOutput out = dest.createOutput(files[i]);
                try {
                    long remaining = in.length();
                    while (remaining > 0) {
                        int num = (int) Math.min(remaining, buffer.length);
                        in.readBytes(buffer, 0, num);
                        out.writeBytes(buffer, num);
                        remaining -= num;
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        }
    }

    /**
     * Returns a <code>ReadOnlyIndexReader</code> and registeres
     * <code>listener</code> to send notifications when documents are deleted on
     * <code>this</code> index.
     *
     * @param listener the listener to notify when documents are deleted.
     * @return a <code>ReadOnlyIndexReader</code>.
     * @throws IOException if the reader cannot be obtained.
     */
    synchronized ReadOnlyIndexReader getReadOnlyIndexReader(IndexListener listener)
            throws IOException {
        ReadOnlyIndexReader reader = getReadOnlyIndexReader();
        this.listener = listener;
        return reader;
    }

    /**
     * Removes a potentially registered {@link IndexListener}.
     */
    synchronized void resetListener() {
        this.listener = null;
    }

    /**
     * Returns the number of documents in this persistent index.
     *
     * @return the number of documents in this persistent index.
     * @throws IOException if an error occurs while reading from the index.
     */
    int getNumDocuments() throws IOException {
        return getIndexReader().numDocs();
    }

    /**
     * Returns the name of this index.
     * @return the name of this index.
     */
    String getName() {
        return name;
    }
}
