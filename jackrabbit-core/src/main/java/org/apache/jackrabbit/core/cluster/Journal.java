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
package org.apache.jackrabbit.core.cluster;

import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.name.NamespaceResolver;

/**
 * Journal interface. Defines operations on a journal that are used to synchronize clustered repository nodes.
 */
public interface Journal {

    /**
     * Initialize journal.
     *
     * @param id id this journal should use to write its own records
     * @param resolver namespace resolver used to map prefixes to URIs and vice-versa.
     * @param processor to invoke when new records are processed
     * @throws JournalException if an error occurs
     */
    public void init(String id, RecordProcessor processor, NamespaceResolver resolver) throws JournalException;

    /**
     * Synchronize contents from journal.
     *
     * @throws JournalException if an error occurs
     */
    public void sync() throws JournalException;

    /**
     * Start an update operation on the journal.
     * @param workspace workspace name, may be <code>null</code>
     *
     * @throws JournalException if an error occurs
     */
    public void begin(String workspace) throws JournalException;

    /**
     * Add item state operations to the journal.
     *
     * @param changes changes to transfer
     * @throws JournalException if an error occurs
     */
    public void log(ChangeLog changes, EventStateCollection esc) throws JournalException;

    /**
     * Log a lock operation.
     *
     * @param nodeId node id
     * @param isDeep flag indicating whether lock is deep
     * @param owner lock owner
     * @throws JournalException if an error occurs
     */
    public void log(NodeId nodeId, boolean isDeep, String owner) throws JournalException;

    /**
     * Log an unlock operation.
     *
     * @param nodeId node id
     * @throws JournalException if an error occurs
     */
    public void log(NodeId nodeId) throws JournalException;

    /**
     * Prepare an update operation on the journal. This locks the journal exclusively for updates until this client
     * either invokes {@link #cancel} or {@link #commit}. If a conflicting intermittent change is detected, this
     * method should throw an exception, signaling that the whole update operation should be undone.
     *
     * @throws JournalException if an error occurs
     */
    public void prepare() throws JournalException;

    /**
     * End this update operation and definitely write changes to the journal.
     *
     * @throws JournalException if an error occurs
     */
    public void commit() throws JournalException;

    /**
     * End this update operation and discards changes made to the journal.
     *
     * @throws JournalException if an error occurs
     */
    public void cancel() throws JournalException;
}
