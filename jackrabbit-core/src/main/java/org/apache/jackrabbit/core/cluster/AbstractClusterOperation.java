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

import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.Record;

/**
 * Base implementation of a cluster operation that contains common funtionality
 * that should not be directly exposed in the {@link ClusterOperation} interface.
 */
abstract class AbstractClusterOperation implements ClusterOperation {

    /**
     * Cluster node.
     */
    private ClusterNode clusterNode;

    /**
     * Workspace where operation takes place.
     */
    protected String workspace;

    /**
     * Journal record associated with this operation.
     */
    protected Record record;

    /**
     * Create a new instance of this class.
     *
     * @param clusterNode cluster node
     * @param workspace workspace where operation takes place.
     * @param record journal record
     */
    public AbstractClusterOperation(ClusterNode clusterNode, String workspace,
                                    Record record) {
        this.clusterNode = clusterNode;
        this.workspace = workspace;
        this.record = record;
    }

    /**
     * Return the workspace where this operation takes place.
     *
     * @return workspace
     */
    public String getWorkspace() {
        return workspace;
    }

    /**
     * Return the record used to serialize/deserialize this operation.
     *
     * @return record
     */
    public Record getRecord() {
        return record;
    }

    /**
     * Serialize this operation to the record. Subclass responsibility.
     *
     * @throws JournalException if an error occurs
     */
    protected abstract void write() throws JournalException;

    /**
     * {@inheritDoc}
     */
    public void ended(boolean successful) {
        clusterNode.ended(this, successful);
    }
}
