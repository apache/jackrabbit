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
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.jackrabbit.core.fs.FileSystemException;

import java.io.IOException;

/**
 * Implements an in-memory index with a redo log.
 */
class VolatileIndex extends AbstractIndex {

    /** The redo log */
    private final RedoLog redoLog;

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
     * redo log and then calling the <code>super.addDocument()</code> method.
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
        super.addDocument(doc);
    }

    /**
     * Overwrites the default implementation by writing an entry to the redo
     * log and then calling the <code>super.removeDocument()</code> method.
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
        return super.removeDocument(idTerm);
    }
}
