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

/**
 * Default cluster operation implementation.
 */
public class DefaultClusterOperation implements ClusterOperation {

    /**
     * Cluster node.
     */
    private final ClusterNode clusterNode;

    /**
     * Cluster record.
     */
    private final ClusterRecord record;

    /**
     * Create an instance of this class.
     *
     * @param clusterNode cluster node
     * @param record cluster record
     */
    public DefaultClusterOperation(ClusterNode clusterNode,
                                   ClusterRecord record) {

        this.clusterNode = clusterNode;
        this.record = record;
    }

    /**
     * {@inheritDoc}
     */
    public void ended(boolean successful) {
        clusterNode.ended(this, successful);
    }

    /**
     * Return the record.
     *
     * @return the record
     */
    public ClusterRecord getRecord() {
        return record;
    }
}
