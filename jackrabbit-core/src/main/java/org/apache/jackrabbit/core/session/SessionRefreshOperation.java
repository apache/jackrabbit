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
package org.apache.jackrabbit.core.session;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.cluster.ClusterException;
import org.apache.jackrabbit.core.cluster.ClusterNode;

/**
 * Operation to refresh the state of a session.
 */
public class SessionRefreshOperation implements SessionOperation<Object> {

    /**
     * Flag to keep transient changes.
     */
    private final boolean keepChanges;

    /**
     * Flag to synchronise with other cluster nodes.
     */
    private final boolean clusterSync;

    /**
     * Creates a session refresh operation.
     *
     * @param keepChanges whether to keep transient changes
     * @param clusterSync whether to synchronise with other cluster nodes
     */
    public SessionRefreshOperation(boolean keepChanges, boolean clusterSync) {
        this.keepChanges = keepChanges;
        this.clusterSync = clusterSync;
    }

    /**
     * Refreshes the session.
     */
    public Object perform(SessionContext context) throws RepositoryException {
        // JCR-1753: Ensure that we are up to date with cluster changes
        ClusterNode cluster = context.getRepositoryContext().getClusterNode();
        if (cluster != null && clusterSync) {
            try {
                cluster.sync();
            } catch (ClusterException e) {
                throw new RepositoryException(
                        "Unable to synchronize with the cluster", e);
            }
        }

        if (!keepChanges) {
            context.getItemStateManager().disposeAllTransientItemStates();
        } else {
            // FIXME should reset Item#status field to STATUS_NORMAL
            // of all non-transient instances; maybe also
            // have to reset stale ItemState instances
        }
        return this;
    }

    //--------------------------------------------------------------< Object >

    /**
     * Returns a string representation of this operation.
     */
    public String toString() {
        return "session.refresh(" + keepChanges + ")";
    }

}