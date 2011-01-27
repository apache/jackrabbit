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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>IndexingQueue</code> implements a queue which contains all the
 * documents with pending text extractor jobs.
 */
class IndexingQueue {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(IndexingQueue.class);

    /**
     * The store to persist uuids of pending documents.
     */
    private final IndexingQueueStore queueStore;

    /**
     * Maps UUID {@link String}s to {@link Document}s.
     */
    private final Map<String, Document> pendingDocuments = new HashMap<String, Document>();

    /**
     * Flag that indicates whether this indexing queue had been
     * {@link #initialize(MultiIndex) initialized}.
     */
    private volatile boolean initialized = false;

    /**
     * Creates an indexing queue.
     *
     * @param queueStore the store where to read the pending extraction jobs.
     */
    IndexingQueue(IndexingQueueStore queueStore) {
        this.queueStore = queueStore;
    }

    /**
     * Initializes the indexing queue.
     *
     * @param index the multi index this indexing queue belongs to.
     * @throws IOException if an error occurs while reading from the index.
     */
    void initialize(MultiIndex index) throws IOException {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        // check index for nodes that need to be reindexed
        CachingMultiIndexReader reader = index.getIndexReader();
        try {
            TermDocs tDocs = reader.termDocs(
                    new Term(FieldNames.REINDEXING_REQUIRED, ""));
            try {
                while (tDocs.next()) {
                    queueStore.addUUID(reader.document(tDocs.doc(),
                            FieldSelectors.UUID).get(FieldNames.UUID));
                }
            } finally {
                tDocs.close();
            }
        } finally {
            reader.release();
        }
        String[] uuids = queueStore.getPending();
        for (String uuid : uuids) {
            try {
                Document doc = index.createDocument(new NodeId(uuid));
                pendingDocuments.put(uuid, doc);
                log.debug("added node {}. New size of indexing queue: {}",
                        uuid, pendingDocuments.size());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID in indexing queue store: " + uuid);
            } catch (RepositoryException e) {
                // node does not exist anymore
                log.debug("Node with uuid {} does not exist anymore", uuid);
                queueStore.removeUUID(uuid);
            }
        }
        initialized = true;
    }

    /**
     * Returns the {@link Document}s that are finished.
     *
     * @return the {@link Document}s that are finished.
     */
    public Document[] getFinishedDocuments() {
        checkInitialized();
        List<Document> finished = new ArrayList<Document>();
        synchronized (this) {
            finished.addAll(pendingDocuments.values());
        }

        Iterator<Document> it = finished.iterator();
        while (it.hasNext()) {
            Document doc = it.next();
            if (!Util.isDocumentReady(doc)) {
                it.remove();
            }
        }
        return finished.toArray(new Document[finished.size()]);
    }

    /**
     * Removes the document with the given <code>uuid</code> from the indexing
     * queue.
     *
     * @param uuid the uuid of the document to return.
     * @return the document for the given <code>uuid</code> or <code>null</code>
     *         if this queue does not contain a document with the given
     *         <code>uuid</code>.
     */
    public synchronized Document removeDocument(String uuid) {
        checkInitialized();
        Document doc = pendingDocuments.remove(uuid);
        if (doc != null) {
            queueStore.removeUUID(uuid);
            log.debug("removed node {}. New size of indexing queue: {}",
                    uuid, pendingDocuments.size());
        }
        return doc;
    }

    /**
     * Adds a document to this indexing queue.
     *
     * @param doc the document to add.
     * @return an existing document in the queue with the same uuid as the one
     *         in <code>doc</code> or <code>null</code> if there was no such
     *         document.
     */
    public synchronized Document addDocument(Document doc) {
        checkInitialized();
        String uuid = doc.get(FieldNames.UUID);
        Document existing = pendingDocuments.put(uuid, doc);
        log.debug("added node {}. New size of indexing queue: {}",
                uuid, pendingDocuments.size());
        if (existing == null) {
            // document wasn't present, add it to the queue store
            queueStore.addUUID(uuid);
        }
        // return existing if any
        return existing;
    }

    /**
     * Closes this indexing queue and disposes all pending documents.
     */
    public synchronized void close() {
        checkInitialized();
        // go through pending documents and close readers
        Iterator<Document> it = pendingDocuments.values().iterator();
        while (it.hasNext()) {
            Document doc = it.next();
            Util.disposeDocument(doc);
            it.remove();
        }
        queueStore.close();
    }

    /**
     * Checks if this indexing queue is initialized and otherwise throws a
     * {@link IllegalStateException}.
     */
    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
    }

    /**
     * Returns the number of pending documents.
     *
     * @return the number of the currently pending documents.
     */
    synchronized int getNumPendingDocuments() {
        return pendingDocuments.size();
    }

}
