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
package org.apache.jackrabbit.core.search.lucene;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;

import java.io.IOException;

/**
 * Implements a lucene index which is based on a
 * {@link org.apache.jackrabbit.core.fs.FileSystem}.
 */
class PersistentIndex extends AbstractIndex {

    /** The underlying filesystem to store the index */
    private final FileSystem fs;

    /**
     * Creates a new <code>PersistentIndex</code> based on the file system
     * <code>fs</code>.
     * @param fs the underlying file system.
     * @param create if <code>true</code> an existing index is deleted.
     * @param analyzer the analyzer for text tokenizing.
     * @throws IOException if an error occurs while opening / creating the
     * index.
     */
    PersistentIndex(FileSystem fs, boolean create, Analyzer analyzer)
            throws IOException {
        super(analyzer, FileSystemDirectory.getDirectory(fs, create));
        this.fs = fs;
    }

    /**
     * Merges another index into this persistent index.
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
}
