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

import org.apache.jackrabbit.core.journal.InstanceRevision;
import org.apache.jackrabbit.core.journal.Journal;
import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.RecordConsumer;
import org.apache.jackrabbit.core.journal.RecordProducer;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

/**
 * Memory-based journal, useful for testing purposes only.
 */
public class MemoryJournal implements Journal {

    /** Revision. */
    private InstanceRevision revision = new MemoryRevision();

    /** Journal id. */
    private String id;

    /**
     * Return this journal's id.
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public InstanceRevision getInstanceRevision() throws JournalException {
        return revision;
    }

    /**
     * {@inheritDoc}
     */
    public RecordProducer getProducer(String identifier)
            throws JournalException {

        throw new JournalException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public void init(String id, NamespaceResolver resolver)
            throws JournalException {

        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    public void register(RecordConsumer consumer) throws JournalException {}

    /**
     * {@inheritDoc}
     */
    public void sync() throws JournalException {}

    /**
     * {@inheritDoc}
     */
    public boolean unregister(RecordConsumer consumer) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void close() {}
}
