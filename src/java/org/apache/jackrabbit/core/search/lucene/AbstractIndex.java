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
import org.apache.log4j.Logger;

import java.io.IOException;

/**
 * Implements common functionality for a lucene index.
 */
abstract class AbstractIndex {

    private static final Logger log = Logger.getLogger(AbstractIndex.class);

    private IndexWriter indexWriter;

    private IndexReader indexReader;

    private Directory directory;

    private Analyzer analyzer;

    private boolean useCompoundFile = true;

    private int minMergeDocs = 1000;

    private int maxMergeDocs = 10000;

    private int mergeFactor = 10;

    AbstractIndex(Analyzer analyzer, Directory directory) throws IOException {
        this.analyzer = analyzer;
        this.directory = directory;

        if (!IndexReader.indexExists(directory)) {
            indexWriter = new IndexWriter(directory, analyzer, true);
            indexWriter.minMergeDocs = minMergeDocs;
            indexWriter.maxMergeDocs = maxMergeDocs;
            indexWriter.mergeFactor = mergeFactor;
            indexWriter.setUseCompoundFile(useCompoundFile);
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
            log.debug("closing IndexWriter.");
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
            log.debug("closing IndexReader.");
            indexReader = null;
        }
        if (indexWriter == null) {
            indexWriter = new IndexWriter(getDirectory(), analyzer, false);
            indexWriter.minMergeDocs = minMergeDocs;
            indexWriter.maxMergeDocs = maxMergeDocs;
            indexWriter.mergeFactor = mergeFactor;
            indexWriter.setUseCompoundFile(useCompoundFile);
        }
        return indexWriter;
    }

    void close() {
        if (indexWriter != null) {
            try {
                indexWriter.close();
            } catch (IOException e) {
                log.warn("Exception closing index writer: " + e.toString());
            }
            indexWriter = null;
        }
        if (indexReader != null) {
            try {
                indexReader.close();
            } catch (IOException e) {
                log.warn("Exception closing index reader: " + e.toString());
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

    void setUseCompoundFile(boolean b) {
        useCompoundFile = b;
        if (indexWriter != null) {
            indexWriter.setUseCompoundFile(b);
        }
    }

    void setMinMergeDocs(int minMergeDocs) {
        this.minMergeDocs = minMergeDocs;
        if (indexWriter != null) {
            indexWriter.minMergeDocs = minMergeDocs;
        }
    }

    void setMaxMergeDocs(int maxMergeDocs) {
        this.maxMergeDocs = maxMergeDocs;
        if (indexWriter != null) {
            indexWriter.maxMergeDocs = maxMergeDocs;
        }
    }

    void setMergeFactor(int mergeFactor) {
        this.mergeFactor = mergeFactor;
        if (indexWriter != null) {
            indexWriter.mergeFactor = mergeFactor;
        }
    }
}
