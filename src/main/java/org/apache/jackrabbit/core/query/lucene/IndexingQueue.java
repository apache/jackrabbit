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
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

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
    private final Map pendingDocuments = new HashMap();

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
     */
    void initialize(MultiIndex index) {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        String[] uuids = queueStore.getPending();
        for (int i = 0; i < uuids.length; i++) {
            try {
                UUID uuid = UUID.fromString(uuids[i]);
                Document doc = index.createDocument(new NodeId(uuid));
                pendingDocuments.put(uuids[i], doc);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID in indexing queue store: " + uuids[i]);
            } catch (RepositoryException e) {
                // node does not exist anymore
                log.debug("Node with uuid {} does not exist anymore", uuids[i]);
                try {
                    queueStore.removeUUID(uuids[i]);
                } catch (IOException ex) {
                    log.warn("Unable to remove node {} from indexing queue",
                            uuids[i], ex);
                }
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
        List finished = new ArrayList();
        synchronized (this) {
            finished.addAll(pendingDocuments.values());
        }

        for (Iterator it = finished.iterator(); it.hasNext(); ) {
            Document doc = (Document) it.next();
            if (!Util.isDocumentReady(doc)) {
                it.remove();
            }
        }
        return (Document[]) finished.toArray(new Document[finished.size()]);
    }

    /**
     * Removes the document with the given <code>uuid</code> from the indexing
     * queue.
     *
     * @param uuid the uuid of the document to return.
     * @return the document for the given <code>uuid</code> or <code>null</code>
     *         if this queue does not contain a document with the given
     *         <code>uuid</code>.
     * @throws IOException if an error occurs removing the document from the
     *                     queue.
     */
    public synchronized Document removeDocument(String uuid) throws IOException {
        checkInitialized();
        Document doc = (Document) pendingDocuments.remove(uuid);
        if (doc != null) {
            queueStore.removeUUID(uuid);
            log.debug("removed node {}. New size of indexing queue: {}",
                    uuid, new Integer(pendingDocuments.size()));
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
     * @throws IOException an error occurs while adding the document to this
     *                     queue.
     */
    public synchronized Document addDocument(Document doc) throws IOException {
        checkInitialized();
        String uuid = doc.get(FieldNames.UUID);
        Document existing = (Document) pendingDocuments.put(uuid, doc);
        log.debug("added node {}. New size of indexing queue: {}",
                uuid, new Integer(pendingDocuments.size()));
        if (existing == null) {
            // document wasn't present, add it to the queue store
            queueStore.addUUID(uuid);
        }
        // return existing if any
        return existing;
    }

    /**
     * Closes this indexing queue and disposes all pending documents.
     *
     * @throws IOException if an error occurs while closing this queue.
     */
    public synchronized void close() throws IOException {
        checkInitialized();
        // go through pending documents and close readers
        for (Iterator it = pendingDocuments.values().iterator(); it.hasNext(); ) {
            Document doc = (Document) it.next();
            Util.disposeDocument(doc);
            it.remove();
        }
        queueStore.close();
    }

    /**
     * Commits any pending changes to this queue store to disk.
     *
     * @throws IOException if an error occurs while writing pending changes to
     *                     disk.
     */
    public synchronized void commit() throws IOException {
        checkInitialized();
        queueStore.commit();
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
}
