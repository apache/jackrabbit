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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;

import java.io.IOException;

/**
 *
 */
abstract class AbstractIndex {

    private IndexWriter indexWriter;

    private IndexReader indexReader;

    private Directory directory;

    private Analyzer analyzer;

    private boolean useCompoundFile = false;

    AbstractIndex(Analyzer analyzer, Directory directory) throws IOException {
        this.analyzer = analyzer;
        this.directory = directory;

        if (!IndexReader.indexExists(directory)) {
            indexWriter = new IndexWriter(directory, analyzer, true);
            indexWriter.setUseCompoundFile(useCompoundFile);
        }
    }

    synchronized void setUseCompoundFile(boolean b) {
        useCompoundFile = b;
        if (indexWriter != null) {
            indexWriter.setUseCompoundFile(b);
        }
    }


    /**
     * Default implementation returns the same instance as passed
     * in the constructor.
     *
     * @return the directory instance passed in the constructor
     * @throws IOException
     */
    Directory getDirectory() throws IOException {
        return this.directory;
    }

    IndexSearcher getIndexSearcher() throws IOException {
        return new IndexSearcher(getIndexReader());
    }

    void addDocument(Document doc) throws IOException {
        getIndexWriter().addDocument(doc);
    }

    void removeDocument(Term idTerm) throws IOException {
        getIndexReader().delete(idTerm);
    }

    protected synchronized IndexReader getIndexReader() throws IOException {
        if (indexWriter != null) {
            indexWriter.close();
            indexWriter = null;
        }
        if (indexReader == null) {
            indexReader = IndexReader.open(getDirectory());
        }
        return indexReader;
    }

    protected synchronized IndexWriter getIndexWriter() throws IOException {
        if (indexReader != null) {
            indexReader.close();
            indexReader = null;
        }
        if (indexWriter == null) {
            indexWriter = new IndexWriter(getDirectory(), analyzer, false);
            indexWriter.setUseCompoundFile(useCompoundFile);
        }
        return indexWriter;
    }

    void close() {
        if (indexWriter != null) {
            try {
                indexWriter.close();
            } catch (IOException e) {
                // FIXME do logging
            }
            indexWriter = null;
        }
        if (indexReader != null) {
            try {
                indexReader.close();
            } catch (IOException e) {
                // FIXME do logging
            }
            indexReader = null;
        }
        if (directory != null) {
            try {
                directory.close();
            } catch (IOException e) {
                directory = null;
            }
        }
    }
}
