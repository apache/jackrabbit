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
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.IOExceptionWithCause;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.IndexDeletionPolicy;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.LogMergePolicy;
import org.apache.lucene.index.Payload;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements common functionality for a lucene index.
 * <p>
 * Note on synchronization: This class is not entirely thread-safe. Certain
 * concurrent access is however allowed. Read-only access on this index using
 * {@link #getReadOnlyIndexReader()} is thread-safe. That is, multiple threads
 * my call that method concurrently and use the returned IndexReader at the same
 * time.<br/>
 * Modifying threads must be synchronized externally in a way that only one
 * thread is using the returned IndexReader and IndexWriter instances returned
 * by {@link #getIndexReader()} and {@link #getIndexWriter()} at a time.<br/>
 * Concurrent access by <b>one</b> modifying thread and multiple read-only
 * threads is safe!
 */
abstract class AbstractIndex {

    /** The logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(AbstractIndex.class);

    /** PrintStream that pipes all calls to println(String) into log.info() */
    private static final LoggingPrintStream STREAM_LOGGER = new LoggingPrintStream();

    /** Executor with a pool size equal to the number of available processors */
    private final DynamicPooledExecutor executor = new DynamicPooledExecutor();

    /** The currently set IndexWriter or <code>null</code> if none is set */
    private IndexWriter indexWriter;

    /** The currently set IndexReader or <code>null</code> if none is set */
    private CommittableIndexReader indexReader;

    /** The underlying Directory where the index is stored */
    private Directory directory;

    /** Analyzer we use to tokenize text */
    private Analyzer analyzer;

    /** The similarity in use for indexing and searching. */
    private final Similarity similarity;

    /** Compound file flag */
    private boolean useCompoundFile = true;

    /** termInfosIndexDivisor config parameter */
    private int termInfosIndexDivisor = SearchIndex.DEFAULT_TERM_INFOS_INDEX_DIVISOR;

    /**
     * The document number cache if this index may use one.
     */
    private DocNumberCache cache;

    /** The shared IndexReader for all read-only IndexReaders */
    private SharedIndexReader sharedReader;

    /**
     * The most recent read-only reader if there is any.
     */
    private ReadOnlyIndexReader readOnlyReader;

    /**
     * The indexing queue.
     */
    private IndexingQueue indexingQueue;

    /**
     * Flag that indicates whether there was an index present in the directory
     * when this AbstractIndex was created.
     */
    private boolean isExisting;

    /**
     * Constructs an index with an <code>analyzer</code> and a
     * <code>directory</code>.
     *
     * @param analyzer      the analyzer for text tokenizing.
     * @param similarity    the similarity implementation.
     * @param directory     the underlying directory.
     * @param cache         the document number cache if this index should use
     *                      one; otherwise <code>cache</code> is
     *                      <code>null</code>.
     * @param indexingQueue the indexing queue.
     * @throws IOException if the index cannot be initialized.
     */
    AbstractIndex(Analyzer analyzer,
                  Similarity similarity,
                  Directory directory,
                  DocNumberCache cache,
                  IndexingQueue indexingQueue) throws IOException {
        this.analyzer = analyzer;
        this.similarity = similarity;
        this.directory = directory;
        this.cache = cache;
        this.indexingQueue = indexingQueue;
        this.isExisting = IndexReader.indexExists(directory);

        if (!isExisting) {
            indexWriter = new IndexWriter(directory, new IndexWriterConfig(Version.LUCENE_36, analyzer));
            // immediately close, now that index has been created
            indexWriter.close();
            indexWriter = null;
        }
    }

    /**
     * Default implementation returns the same instance as passed
     * in the constructor.
     *
     * @return the directory instance passed in the constructor
     */
    Directory getDirectory() {
        return directory;
    }

    /**
     * Returns <code>true</code> if this index was openend on a directory with
     * an existing index in it; <code>false</code> otherwise.
     *
     * @return <code>true</code> if there was an index present when this index
     *          was created; <code>false</code> otherwise.
     */
    boolean isExisting() {
        return isExisting;
    }

    /**
     * Adds documents to this index and invalidates the shared reader.
     *
     * @param docs the documents to add.
     * @throws IOException if an error occurs while writing to the index.
     */
    void addDocuments(Document[] docs) throws IOException {
        final List<IOException> exceptions =
            Collections.synchronizedList(new ArrayList<IOException>());
        final CountDownLatch latch = new CountDownLatch(docs.length);

        final IndexWriter writer = getIndexWriter();
        for (final Document doc : docs) {
            executor.execute(new Runnable() {
                public void run() {
                    try {
                        // check if text extractor completed its work
                        Document document = getFinishedDocument(doc);
                        if (log.isDebugEnabled()) {
                            long start = System.nanoTime();
                            writer.addDocument(document);
                            log.debug("Inverted a document in {}us",
                                    (System.nanoTime() - start) / 1000);
                        } else {
                            writer.addDocument(document);
                        }
                    } catch (IOException e) {
                        log.warn("Exception while inverting a document", e);
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        for (;;) {
            try {
                latch.await();
                break;
            } catch (InterruptedException e) {
                // retry
            }
        }
        invalidateSharedReader();

        if (!exceptions.isEmpty()) {
            throw new IOException(
                    exceptions.size() + " of " + docs.length
                    + " background indexer tasks failed", exceptions.get(0));
        }
    }

    /**
     * Removes the document from this index. This call will not invalidate
     * the shared reader. If a subclass whishes to do so, it should overwrite
     * this method and call {@link #invalidateSharedReader()}.
     *
     * @param idTerm the id term of the document to remove.
     * @throws IOException if an error occurs while removing the document.
     * @return number of documents deleted
     */
    int removeDocument(Term idTerm) throws IOException {
        return getIndexReader().deleteDocuments(idTerm);
    }

    /**
     * Returns an <code>IndexReader</code> on this index. This index reader
     * may be used to delete documents.
     *
     * @return an <code>IndexReader</code> on this index.
     * @throws IOException if the reader cannot be obtained.
     */
    protected synchronized CommittableIndexReader getIndexReader() throws IOException {
        if (indexWriter != null) {
            indexWriter.close();
            log.debug("closing IndexWriter.");
            indexWriter = null;
        }
        if (indexReader == null) {
            IndexDeletionPolicy idp = getIndexDeletionPolicy();
            IndexReader reader = IndexReader.open(getDirectory(), idp, false, termInfosIndexDivisor);
            indexReader = new CommittableIndexReader(reader);
        }
        return indexReader;
    }

    /**
     * Returns the index deletion policy for this index. This implementation
     * always returns <code>null</code>.
     *
     * @return the index deletion policy for this index or <code>null</code> if
     *          none is present.
     */
    protected IndexDeletionPolicy getIndexDeletionPolicy() {
        return null;
    }

    /**
     * Returns a read-only index reader, that can be used concurrently with
     * other threads writing to this index. The returned index reader is
     * read-only, that is, any attempt to delete a document from the index
     * will throw an <code>UnsupportedOperationException</code>.
     *
     * @param initCache if the caches in the index reader should be initialized
     *          before the index reader is returned.
     * @return a read-only index reader.
     * @throws IOException if an error occurs while obtaining the index reader.
     */
    synchronized ReadOnlyIndexReader getReadOnlyIndexReader(boolean initCache)
            throws IOException {
        // get current modifiable index reader
        CommittableIndexReader modifiableReader = getIndexReader();
        long modCount = modifiableReader.getModificationCount();
        if (readOnlyReader != null) {
            if (readOnlyReader.getDeletedDocsVersion() == modCount) {
                // reader up-to-date
                readOnlyReader.acquire();
                return readOnlyReader;
            } else {
                // reader outdated
                if (readOnlyReader.getRefCountJr() == 1) {
                    // not in use, except by this index
                    // update the reader
                    readOnlyReader.updateDeletedDocs(modifiableReader);
                    readOnlyReader.acquire();
                    return readOnlyReader;
                } else {
                    // cannot update reader, it is still in use
                    // need to create a new instance
                    readOnlyReader.release();
                    readOnlyReader = null;
                }
            }
        }
        // if we get here there is no up-to-date read-only reader
        if (sharedReader == null) {
            // create new shared reader
            IndexReader reader = IndexReader.open(getDirectory(), termInfosIndexDivisor);
            CachingIndexReader cr = new CachingIndexReader(
                    reader, cache, initCache);
            sharedReader = new SharedIndexReader(cr);
        }
        readOnlyReader = new ReadOnlyIndexReader(sharedReader, 
                modifiableReader.getDeletedDocs(), modCount);
        readOnlyReader.acquire();
        return readOnlyReader;
    }

    /**
     * Returns a read-only index reader, that can be used concurrently with
     * other threads writing to this index. The returned index reader is
     * read-only, that is, any attempt to delete a document from the index
     * will throw an <code>UnsupportedOperationException</code>.
     *
     * @return a read-only index reader.
     * @throws IOException if an error occurs while obtaining the index reader.
     */
    protected ReadOnlyIndexReader getReadOnlyIndexReader()
            throws IOException {
        return getReadOnlyIndexReader(false);
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
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyzer);
            config.setSimilarity(similarity);
            LogMergePolicy mergePolicy = new LogByteSizeMergePolicy();
            mergePolicy.setUseCompoundFile(useCompoundFile);
            mergePolicy.setNoCFSRatio(1.0);
            config.setMergePolicy(mergePolicy);

            indexWriter = new IndexWriter(getDirectory(), config);
            indexWriter.setInfoStream(STREAM_LOGGER);
        }
        return indexWriter;
    }

    /**
     * Commits all pending changes to the underlying <code>Directory</code>.
     * @throws IOException if an error occurs while commiting changes.
     */
    protected void commit() throws IOException {
        commit(false);
    }

    /**
     * Commits all pending changes to the underlying <code>Directory</code>.
     *
     * @param optimize if <code>true</code> the index is optimized after the
     *                 commit.
     * @throws IOException if an error occurs while commiting changes.
     */
    protected synchronized void commit(boolean optimize) throws IOException {
        if (indexReader != null) {
            log.debug("committing IndexReader.");
            indexReader.flush();
        }
        if (indexWriter != null) {
            log.debug("committing IndexWriter.");
            indexWriter.commit();
        }
        // optimize if requested
        if (optimize) {
            IndexWriter writer = getIndexWriter();
            writer.forceMerge(1, true);
            writer.close();
            indexWriter = null;
        }
    }

    /**
     * Closes this index, releasing all held resources.
     */
    synchronized void close() {
        releaseWriterAndReaders();
        if (directory != null) {
            try {
                directory.close();
            } catch (IOException e) {
                directory = null;
            }
        }
        executor.close();
    }

    /**
     * Releases all potentially held index writer and readers.
     */
    protected void releaseWriterAndReaders() {
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
        if (readOnlyReader != null) {
            try {
                readOnlyReader.release();
            } catch (IOException e) {
                log.warn("Exception closing index reader: " + e.toString());
            }
            readOnlyReader = null;
        }
        if (sharedReader != null) {
            try {
                sharedReader.release();
            } catch (IOException e) {
                log.warn("Exception closing index reader: " + e.toString());
            }
            sharedReader = null;
        }
    }

    /**
     * @return the number of bytes this index occupies in memory.
     */
    synchronized long getRamSizeInBytes() {
        if (indexWriter != null) {
            return indexWriter.ramSizeInBytes();
        } else {
            return 0;
        }
    }

    /**
     * Closes the shared reader.
     *
     * @throws IOException if an error occurs while closing the reader.
     */
    protected synchronized void invalidateSharedReader() throws IOException {
        // also close the read-only reader
        if (readOnlyReader != null) {
            readOnlyReader.release();
            readOnlyReader = null;
        }
        // invalidate shared reader
        if (sharedReader != null) {
            sharedReader.release();
            sharedReader = null;
        }
    }

    /**
     * Returns a document that is finished with text extraction and is ready to
     * be added to the index.
     *
     * @param doc the document to check.
     * @return <code>doc</code> if it is finished already or a stripped down
     *         copy of <code>doc</code> without text extractors.
     * @throws IOException if the document cannot be added to the indexing
     *                     queue.
     */
    private Document getFinishedDocument(Document doc) throws IOException {
        if (!Util.isDocumentReady(doc)) {
            Document copy = new Document();
            // mark the document that reindexing is required
            copy.add(new Field(FieldNames.REINDEXING_REQUIRED, false, "",
                    Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO));
            for (Fieldable f : doc.getFields()) {
                Fieldable field = null;
                Field.TermVector tv = getTermVectorParameter(f);
                Field.Store stored = f.isStored() ? Field.Store.YES : Field.Store.NO;
                Field.Index indexed = getIndexParameter(f);
                if (f instanceof LazyTextExtractorField || f.readerValue() != null) {
                    // replace all readers with empty string reader
                    field = new Field(f.name(), new StringReader(""), tv);
                } else if (f.stringValue() != null) {
                    field = new Field(f.name(), false, f.stringValue(), stored,
                            indexed, tv);
                } else if (f.isBinary()) {
                    field = new Field(f.name(), f.getBinaryValue(), stored);
                } else if (f.tokenStreamValue() != null && f.tokenStreamValue() instanceof SingletonTokenStream) {
                    TokenStream tokenStream = f.tokenStreamValue();
                    TermAttribute termAttribute = tokenStream.addAttribute(TermAttribute.class);
                    PayloadAttribute payloadAttribute = tokenStream.addAttribute(PayloadAttribute.class);
                    tokenStream.incrementToken();
                    String value = new String(termAttribute.termBuffer(), 0, termAttribute.termLength());
                    tokenStream.reset();
                    field = new Field(f.name(), new SingletonTokenStream(value, (Payload) payloadAttribute.getPayload().clone()));
                }
                if (field != null) {
                    field.setOmitNorms(f.getOmitNorms());
                    copy.add(field);
                }
            }
            // schedule the original document for later indexing
            Document existing = indexingQueue.addDocument(doc);
            if (existing != null) {
                // the queue already contained a pending document for this
                // node. -> dispose the document
                Util.disposeDocument(existing);
            }
            // use the stripped down copy for now
            doc = copy;
        }
        return doc;
    }

    //-------------------------< properties >-----------------------------------

    /**
     * Whether the index writer should use the compound file format
     */
    void setUseCompoundFile(boolean b) {
        useCompoundFile = b;
    }

    /**
     * @return the current value for termInfosIndexDivisor.
     */
    public int getTermInfosIndexDivisor() {
        return termInfosIndexDivisor;
    }

    /**
     * Sets a new value for termInfosIndexDivisor.
     *
     * @param termInfosIndexDivisor the new value.
     */
    public void setTermInfosIndexDivisor(int termInfosIndexDivisor) {
        this.termInfosIndexDivisor = termInfosIndexDivisor;
    }

    //------------------------------< internal >--------------------------------

    /**
     * Returns the index parameter set on <code>f</code>.
     *
     * @param f a lucene field.
     * @return the index parameter on <code>f</code>.
     */
    private static Field.Index getIndexParameter(Fieldable f) {
        if (!f.isIndexed()) {
            return Field.Index.NO;
        } else if (f.isTokenized()) {
            return Field.Index.ANALYZED;
        } else {
            return Field.Index.NOT_ANALYZED;
        }
    }

    /**
     * Returns the term vector parameter set on <code>f</code>.
     *
     * @param f a lucene field.
     * @return the term vector parameter on <code>f</code>.
     */
    private static Field.TermVector getTermVectorParameter(Fieldable f) {
        if (f.isStorePositionWithTermVector() && f.isStoreOffsetWithTermVector()) {
            return Field.TermVector.WITH_POSITIONS_OFFSETS;
        } else if (f.isStorePositionWithTermVector()) {
            return Field.TermVector.WITH_POSITIONS;
        } else if (f.isStoreOffsetWithTermVector()) {
            return Field.TermVector.WITH_OFFSETS;
        } else if (f.isTermVectorStored()) {
            return Field.TermVector.YES;
        } else {
            return Field.TermVector.NO;
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
