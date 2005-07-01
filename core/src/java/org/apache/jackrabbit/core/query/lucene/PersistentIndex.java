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

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.index.IndexReader;
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Implements a lucene index which is based on a
 * {@link org.apache.jackrabbit.core.fs.FileSystem}.
 */
class PersistentIndex extends AbstractIndex {

    /** The logger instance for this class */
    private static final Logger log = Logger.getLogger(PersistentIndex.class);

    /** Name of the write lock file */
    private static final String WRITE_LOCK = "write.lock";

    /** The underlying filesystem to store the index */
    private final FileSystem fs;

    /** The name of this persistent index */
    private final String name;

    /**
     * Creates a new <code>PersistentIndex</code> based on the file system
     * <code>fs</code>.
     * @param name the name of this index.
     * @param fs the underlying file system.
     * @param create if <code>true</code> an existing index is deleted.
     * @param analyzer the analyzer for text tokenizing.
     * @throws IOException if an error occurs while opening / creating the
     *  index.
     * @throws FileSystemException if an error occurs while opening / creating
     *  the index.
     */
    PersistentIndex(String name, FileSystem fs, boolean create, Analyzer analyzer)
            throws FileSystemException, IOException {
        super(analyzer, FileSystemDirectory.getDirectory(fs, create));
        this.name = name;
        this.fs = fs;

        // check if index is locked, probably from an unclean repository
        // shutdown
        if (fs.exists(WRITE_LOCK)) {
            log.warn("Removing write lock on search index.");
            try {
                fs.deleteFile(WRITE_LOCK);
            } catch (FileSystemException e) {
                log.error("Unable to remove write lock on search index.");
            }
        }
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
    }

    /**
     * Returns the underlying directory.
     * @return the directory.
     * @throws IOException if an error occurs.
     */
    Directory getDirectory() throws IOException {
        return FileSystemDirectory.getDirectory(fs, false);
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

    /**
     * Always returns <code>true</code>.
     * @return <code>true</code>.
     */
    protected boolean useCachingReader() {
        return true;
    }
}
