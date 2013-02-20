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
package org.apache.jackrabbit.core.persistence.check;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.cluster.UpdateEventChannel;

/**
 * Optional interface for Persistence Managers. Allows running consistency
 * checks similar to the base one (see
 * {@link org.apache.jackrabbit.core.persistence.bundle.AbstractBundlePersistenceManager#checkConsistency(String[], boolean, boolean)})
 * but providing a result that can be acted upon.
 * <p>
 * <em>Beware: this interface is designed for unit tests only.</em>
 */
public interface ConsistencyChecker {

    /**
     * Set the update event channel. Needed to inform the cluster of any changes made during repairs.
     *
     * @param eventChannel the update event channel
     */
    public void setEventChannel(UpdateEventChannel eventChannel);

    /**
     * Perform a consistency check of the data. An example are non-existent
     * nodes referenced in a child node entry. The existence of this feature and
     * the scope of the implementation can vary in different PersistenceManager
     * implementations.
     * 
     * @param uuids
     *            list of UUIDs of nodes to be checked. if null, all nodes will
     *            be checked
     * @param recursive
     *            if true, the tree(s) below the given node(s) will be traversed
     *            and checked as well
     * @param fix
     *            if true, any problems found that can be repaired will be
     *            repaired. if false, no data will be modified, instead all
     *            inconsistencies will only get logged
     * @param lostNFoundId
     *            node to which to attach orphaned nodes (or <code>null</code>,
     *            in which case orphaned nodes will not get moved); this node
     *            should be of a node type that allows adding arbitrary child
     *            nodes
     * @param listener
     *            to be called on each node that was checked (may be <code>null</code>)
     */
    ConsistencyReport check(String[] uuids, boolean recursive, boolean fix,
            String lostNFoundId, ConsistencyCheckListener listener)
            throws RepositoryException;
}
