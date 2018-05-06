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
package org.apache.jackrabbit.core.config;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.journal.Journal;
import org.apache.jackrabbit.core.journal.JournalFactory;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

/**
 * Cluster configuration.
 */
public class ClusterConfig implements JournalFactory {

    /**
     * Identifier.
     */
    private final String id;

    /**
     * Sync delay.
     */
    private final long syncDelay;

    /**
     * Stop delay.
     */
    private final long stopDelay;

    /**
     * Journal factory.
     */
    private final JournalFactory jf;

    /**
     * Creates a new cluster configuration.
     *
     * @param id custom cluster node id
     * @param syncDelay syncDelay, in milliseconds
     * @param jf journal factory
     */
    public ClusterConfig(String id, long syncDelay, JournalFactory jf) {
        this(id, syncDelay, -1, jf);
    }

    /**
     * Creates a new cluster configuration.
     *
     * @param id custom cluster node id
     * @param syncDelay syncDelay, in milliseconds
     * @param stopDelay stopDelay in milliseconds
     * @param jf journal factory
     */
    public ClusterConfig(String id, long syncDelay,
                         long stopDelay, JournalFactory jf) {
        this.id = id;
        this.syncDelay = syncDelay;
        this.stopDelay = stopDelay < 0 ? syncDelay * 10 : stopDelay;
        this.jf = jf;
    }

    /**
     * Return the id configuration attribute value.
     *
     * @return id attribute value
     */
    public String getId() {
        return id;
    }

    /**
     * Return the syncDelay configuration attribute value.
     *
     * @return syncDelay
     */
    public long getSyncDelay() {
        return syncDelay;
    }

    /**
     * @return stopDelay the stopDelay configuration attribute value.
     */
    public long getStopDelay() {
        return stopDelay;
    }

    /**
     * Returns an initialized journal instance.
     *
     * @param resolver namespace resolver
     * @return initialized journal
     * @throws RepositoryException 
     * @throws RepositoryException if the journal can not be created
     */
    public Journal getJournal(NamespaceResolver resolver)
            throws RepositoryException {
        return jf.getJournal(resolver);
    }

}
