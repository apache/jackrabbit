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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.commons.collections.SequencedHashMap;

import java.io.IOException;
import java.util.Map;
import java.util.Iterator;

/**
 * Implements an in-memory index with a redo log.
 */
class VolatileIndex extends AbstractIndex {

    /**
     * Default value for {@link #bufferSize}.
     */
    private static final int DEFAULT_BUFFER_SIZE = 10;

    /** The redo log */
    private final RedoLog redoLog;

    /** Map of pending documents to add to the index */
    private final Map pending = new SequencedHashMap();

    /**
     * Number of documents that are buffered before they are added to the index.
     */
    private int bufferSize = DEFAULT_BUFFER_SIZE;

    /**
     * Creates a new <code>VolatileIndex</code> using an <code>analyzer</code>
     * and a redo <code>log</code>.
     * @param analyzer the analyzer to use.
     * @param log the redo log.
     * @throws IOException if an error occurs while opening the index.
     */
    VolatileIndex(Analyzer analyzer, RedoLog log) throws IOException {
        super(analyzer, new RAMDirectory());
        redoLog = log;
    }

    /**
     * Returns the redo log of this volatile index.
     * @return the redo log of this volatile index.
     */
    RedoLog getRedoLog() {
        return redoLog;
    }

    /**
     * Overwrites the default implementation by writing an entry to the
     * redo log and then adds it to the pending list.
     * @param doc the document to add to the index.
     * @throws IOException if an error occurs while writing to the redo log
     * or the index.
     */
    void addDocument(Document doc) throws IOException {
        try {
            redoLog.nodeAdded(doc.get(FieldNames.UUID));
            redoLog.flush();
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
        pending.put(doc.get(FieldNames.UUID), doc);
        if (pending.size() >= bufferSize) {
            commitPending();
        }
    }

    /**
     * Overwrites the default implementation by writing an entry to the redo
     * log and then calling the <code>super.removeDocument()</code> method or
     * if the document is in the pending list, removes it from there.
     *
     * @param idTerm the uuid term of the document to remove.
     * @throws IOException if an error occurs while writing to the redo log
     * or the index.
     * @return the number of deleted documents
     */
    int removeDocument(Term idTerm) throws IOException {
        try {
            redoLog.nodeRemoved(idTerm.text());
            redoLog.flush();
        } catch (FileSystemException e) {
            throw new IOException(e.getMessage());
        }
        if (pending.remove(idTerm.text()) != null) {
            // pending document has been removed
            return 1;
        } else {
            // remove document from index
            return super.getIndexReader().delete(idTerm);
        }
    }

    /**
     * Overwrites the implementation in {@link AbstractIndex} to trigger
     * commit of pending documents to index.
     * @return the index reader for this index.
     * @throws IOException if an error occurs building a reader.
     */
    protected synchronized IndexReader getIndexReader() throws IOException {
        commitPending();
        return super.getIndexReader();
    }

    /**
     * Overwrites the implementation in {@link AbstractIndex} to commit
     * pending documents.
     */
    protected synchronized void commit() throws IOException {
        commitPending();
        super.commit();
    }

    /**
     * Sets a new buffer size for pending documents to add to the index.
     * Higher values consume more memory, but help to avoid multiple index
     * cycles when a node is changed / saved multiple times.
     *
     * @param size the new buffer size.
     */
    void setBufferSize(int size) {
        bufferSize = size;
    }

    /**
     * Commits pending documents to the index.
     */
    private void commitPending() throws IOException {
        for (Iterator it = pending.values().iterator(); it.hasNext();) {
            Document doc = (Document) it.next();
            super.addDocument(doc);
            it.remove();
        }
    }
}
