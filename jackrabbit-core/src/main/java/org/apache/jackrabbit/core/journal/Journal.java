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

import org.apache.jackrabbit.namespace.NamespaceResolver;

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
    public void init(String id, NamespaceResolver resolver) throws JournalException;

    /**
     * Register a record consumer.
     *
     * @param consumer record consumer
     * @throws JournalException if an error occurs
     */
    public void register(RecordConsumer consumer) throws JournalException;

    /**
     * Unregister a record processor.
     *
     * @param consumer record processor to unregister
     * @return <code>true</code> if the consumer was previously registered;
     *         <code>false</code> otherwise
     */
    public boolean unregister(RecordConsumer consumer);

    /**
     * Synchronize contents from journal. This will compare the journal's
     * revision with the revisions of all registered consumers and invoke
     * their {@link RecordConsumer#consume} method when their identifier
     * matches the one found in the records.
     *
     * @throws JournalException if an error occurs
     */
    public void sync() throws JournalException;

    /**
     * Return the record producer for a given identifier.
     *
     * @param identifier identifier
     * @throws JournalException if an error occurs
     */
    public RecordProducer getProducer(String identifier) throws JournalException;

    /**
     * Close this journal. This should release any resources still held by this journal.
     */
    public void close();
}