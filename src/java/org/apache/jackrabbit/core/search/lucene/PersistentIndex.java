/*
 * Copyright 2004 The Apache Software Foundation.
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
public class PersistentIndex extends AbstractIndex {

    private final FileSystem fs;

    PersistentIndex(FileSystem fs,
                    boolean create,
                    Analyzer analyzer)
            throws IOException {

        super(analyzer, FileSystemDirectory.getDirectory(fs, create));
        this.fs = fs;
    }

    void mergeIndex(AbstractIndex index) throws IOException {
        this.getIndexWriter().addIndexes(new IndexReader[]{
            index.getIndexReader()
        });
    }

    Directory getDirectory() throws IOException {
        return FileSystemDirectory.getDirectory(fs, false);
    }
}
