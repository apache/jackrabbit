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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;

/**
 * Implements common functionality for a lucene index.
 */
abstract class AbstractIndex {

    /** The logger instance for this class */
    private static final Logger log = Logger.getLogger(AbstractIndex.class);

    /** PrintStream that pipes all calls to println(String) into log.info() */
    private static final LoggingPrintStream STREAM_LOGGER = new LoggingPrintStream();

    /** The currently set IndexWriter or <code>null</code> if none is set */
    private IndexWriter indexWriter;

    /** The currently set IndexReader of <code>null</code> if none is set */
    private IndexReader indexReader;

    /** The underlying Directory where the index is stored */
    private Directory directory;

    /** Analyzer we use to tokenize text */
    private Analyzer analyzer;

    /** Compound file flag */
    private boolean useCompoundFile = true;

    /** minMergeDocs config parameter */
    private int minMergeDocs = 1000;

    /** maxMergeDocs config parameter */
    private int maxMergeDocs = 10000;

    /** mergeFactor config parameter */
    private int mergeFactor = 10;

    /**
     * Constructs an index with an <code>analyzer</code> and a
     * <code>directory</code>.
     * @param analyzer the analyzer for text tokenizing.
     * @param directory the underlying directory.
     * @throws IOException if the index cannot be initialized.
     */
    AbstractIndex(Analyzer analyzer, Directory directory) throws IOException {
        this.analyzer = analyzer;
        this.directory = directory;

        if (!IndexReader.indexExists(directory)) {
            indexWriter = new IndexWriter(directory, analyzer, true);
            indexWriter.minMergeDocs = minMergeDocs;
            indexWriter.maxMergeDocs = maxMergeDocs;
            indexWriter.mergeFactor = mergeFactor;
            indexWriter.setUseCompoundFile(useCompoundFile);
            indexWriter.infoStream = STREAM_LOGGER;
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
        return directory;
    }

    /**
     * Adds a document to this index.
     * @param doc the document to add.
     * @throws IOException if an error occurs while writing to the index.
     */
    void addDocument(Document doc) throws IOException {
        getIndexWriter().addDocument(doc);
    }

    /**
     * Removes the document from this index.
     * @param idTerm the id term of the document to remove.
     * @throws IOException if an error occurs while removing the document.
     * @return number of documents deleted
     */
    int removeDocument(Term idTerm) throws IOException {
        return getIndexReader().delete(idTerm);
    }

    /**
     * Returns an <code>IndexReader</code> on this index.
     * @return an <code>IndexReader</code> on this index.
     * @throws IOException if the reader cannot be obtained.
     */
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

    /**
     * Returns an <code>IndexWriter</code> on this index.
     * @return an <code>IndexWriter</code> on this index.
     * @throws IOException if the writer cannot be obtained.
     */
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
            indexWriter.infoStream = STREAM_LOGGER;
        }
        return indexWriter;
    }

    /**
     * Commits all pending changes to the underlying <code>Directory</code>.
     * After commit both <code>IndexReader</code> and <code>IndexWriter</code>
     * are released.
     * @throws IOException if an error occurs while commiting changes.
     */
    protected synchronized void commit() throws IOException {
        if (indexReader != null) {
            indexReader.close();
            log.debug("closing IndexReader.");
            indexReader = null;
        }
        if (indexWriter != null) {
            indexWriter.close();
            log.debug("closing IndexWriter.");
            indexWriter = null;
        }
    }

    /**
     * Closes this index, releasing all held resources.
     */
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

    //-------------------------< properties >-----------------------------------

    /**
     * The lucene index writer property: useCompountFile
     */
    void setUseCompoundFile(boolean b) {
        useCompoundFile = b;
        if (indexWriter != null) {
            indexWriter.setUseCompoundFile(b);
        }
    }

    /**
     * The lucene index writer property: minMergeDocs
     */
    void setMinMergeDocs(int minMergeDocs) {
        this.minMergeDocs = minMergeDocs;
        if (indexWriter != null) {
            indexWriter.minMergeDocs = minMergeDocs;
        }
    }

    /**
     * The lucene index writer property: maxMergeDocs
     */
    void setMaxMergeDocs(int maxMergeDocs) {
        this.maxMergeDocs = maxMergeDocs;
        if (indexWriter != null) {
            indexWriter.maxMergeDocs = maxMergeDocs;
        }
    }

    /**
     * The lucene index writer property: mergeFactor
     */
    void setMergeFactor(int mergeFactor) {
        this.mergeFactor = mergeFactor;
        if (indexWriter != null) {
            indexWriter.mergeFactor = mergeFactor;
        }
    }

    /**
     * Adapter to pipe info messages from lucene into log messages.
     */
    private static final class LoggingPrintStream extends PrintStream {

        /** Buffer print calls until a newline is written */
        private StringBuffer buffer = new StringBuffer();

        public LoggingPrintStream() {
            super(new OutputStream() {
                public void write(int b) {
                    // do nothing
                }
            });
        }

        public void print(String s) {
            buffer.append(s);
        }

        public void println(String s) {
            buffer.append(s);
            log.debug(buffer.toString());
            buffer.setLength(0);
        }
    }
}
