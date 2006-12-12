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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

import java.io.IOException;
import java.io.File;

/**
 * Implements a lucene index which is based on a
 * {@link org.apache.jackrabbit.core.fs.FileSystem}.
 */
class PersistentIndex extends AbstractIndex {

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(PersistentIndex.class);

    /** Name of the write lock file */
    private static final String WRITE_LOCK = IndexWriter.WRITE_LOCK_NAME;

    /** Name of the commit lock file */
    private static final String COMMIT_LOCK = IndexWriter.COMMIT_LOCK_NAME;

    /** The name of this persistent index */
    private final String name;

    /** Set to <code>true</code> if this index encountered locks on startup */
    private boolean lockEncountered = false;

    /**
     * If non <code>null</code>, <code>listener</code> needs to be informed
     * when a document is deleted.
     */
    private IndexListener listener;

    /**
     * Creates a new <code>PersistentIndex</code> based on the file system
     * <code>indexDir</code>.
     * @param name the name of this index.
     * @param indexDir the directory to store the index.
     * @param create if <code>true</code> an existing index is deleted.
     * @param analyzer the analyzer for text tokenizing.
     * @param cache the document number cache
     * @throws IOException if an error occurs while opening / creating the
     *  index.
     * @throws IOException if an error occurs while opening / creating
     *  the index.
     */
    PersistentIndex(String name, File indexDir, boolean create,
                    Analyzer analyzer, DocNumberCache cache)
            throws IOException {
        super(analyzer, FSDirectory.getDirectory(indexDir, create), cache);
        this.name = name;

        // check if index is locked, probably from an unclean repository
        // shutdown
        File writeLock = new File(indexDir, WRITE_LOCK);
        if (writeLock.exists()) {
            lockEncountered = true;
            log.warn("Removing write lock on search index.");
            if (!writeLock.delete()) {
                log.error("Unable to remove write lock on search index.");
            }
        }
        File commitLock = new File(indexDir, COMMIT_LOCK);
        if (commitLock.exists()) {
            lockEncountered = true;
            log.warn("Removing commit lock on search index.");
            if (!commitLock.delete()) {
                log.error("Unable to remove write lock on search index.");
            }
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
     * Returns <code>true</code> if this index encountered a lock on the file
     * system during startup. This indicates a unclean shutdown.
     *
     * @return <code>true</code> if this index encountered a lock on startup;
     *         <code>false</code> otherwise.
     */
    boolean getLockEncountered() {
        return lockEncountered;
    }

    /**
     * Merges another index into this persistent index. Before <code>index</code>
     * is merged, {@link AbstractIndex#commit()} is called on that
     * <code>index</code>.
     *
     * @param index the other index to merge.
     * @throws IOException if an error occurs while merging.
     */
    void mergeIndex(AbstractIndex index) throws IOException {
        // commit changes to directory on other index.
        index.commit();
        // merge index
        getIndexWriter().addIndexes(new Directory[]{
            index.getDirectory()
        });
        invalidateSharedReader();
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
