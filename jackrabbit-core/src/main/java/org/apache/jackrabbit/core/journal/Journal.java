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
package org.apache.jackrabbit.core.journal;

import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

/**
 * Generic journal interface.
 */
public interface Journal {

    /**
     * Initialize journal.
     *
     * @param id id this journal should use to write its own records
     * @param resolver resolver used when reading/writing records
     * @throws JournalException if an error occurs
     */
    void init(String id, NamespaceResolver resolver) throws JournalException;

    /**
     * Register a record consumer.
     *
     * @param consumer record consumer
     * @throws JournalException if an error occurs
     */
    void register(RecordConsumer consumer) throws JournalException;

    /**
     * Unregister a record processor.
     *
     * @param consumer record processor to unregister
     * @return <code>true</code> if the consumer was previously registered;
     *         <code>false</code> otherwise
     */
    boolean unregister(RecordConsumer consumer);

    /**
     * Synchronize contents from journal. This will compare the journal's
     * revision with the revisions of all registered consumers and invoke
     * their {@link RecordConsumer#consume} method when their identifier
     * matches the one found in the records.
     * The startup flag allow for a separate treatment of the initial sync
     * when the cluster nodes starts up. This might be needed for example
     * when there are a lot of old revisions in a database.
     *
     * @param startup indicates if the cluster node is syncing on startup 
     *        or does a normal sync.
     * @throws JournalException if an error occurs
     */
    void sync(boolean startup) throws JournalException;

    /**
     * Return the record producer for a given identifier.
     *
     * @param identifier identifier
     * @return the record producer for a given identifier.
     * @throws JournalException if an error occurs
     */
    RecordProducer getProducer(String identifier) throws JournalException;

    /**
     * Close this journal. This should release any resources still held by this journal.
     */
    void close();

    /**
     * Gets the instance that manages the cluster node's local revision.
     *
     * @return the InstanceRevision manager
     * @throws JournalException on error
     */
    InstanceRevision getInstanceRevision() throws JournalException;

    /**
     * Return an iterator over all records after the specified revision.
     *
     * @param startRevision start point (exlusive)
     * @return an iterator over all records after the specified revision.
     * @throws JournalException if an error occurs
     */
    RecordIterator getRecords(long startRevision)
            throws JournalException;

    /**
     * Return an iterator over all available records in the journal.
     *
     * @return an iterator over all records.
     * @throws JournalException if an error occurs
     */
    RecordIterator getRecords() throws JournalException;
}
