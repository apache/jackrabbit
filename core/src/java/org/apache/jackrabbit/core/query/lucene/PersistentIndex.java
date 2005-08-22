/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.io.File;

/**
 * Implements a lucene index which is based on a
 * {@link org.apache.jackrabbit.core.fs.FileSystem}.
 */
class PersistentIndex extends AbstractIndex {

    /** The logger instance for this class */
    private static final Logger log = Logger.getLogger(PersistentIndex.class);

    /** Name of the write lock file */
    private static final String WRITE_LOCK = IndexWriter.WRITE_LOCK_NAME;

    /** Name of the commit lock file */
    private static final String COMMIT_LOCK = IndexWriter.COMMIT_LOCK_NAME;

    /** The name of this persistent index */
    private final String name;

    /** Set to <code>true</code> if this index encountered locks on startup */
    private boolean lockEncountered = false;

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
     * Returns <code>true</code> if this index has valid documents. Returns
     * <code>false</code> if all documents are deleted, or the index does not
     * contain any documents.
     * @return
     * @throws IOException
     */
    boolean hasDocuments() throws IOException {
        if (getIndexReader().numDocs() == 0) {
            return false;
        }
        IndexReader reader = getIndexReader();
        for (int i = 0; i < reader.maxDoc(); i++) {
            if (!reader.isDeleted(i)) {
                return true;
    }
        }
        return false;
    }

    /**
     * Returns the name of this index.
     * @return the name of this index.
     */
    String getName() {
        return name;
    }
}
