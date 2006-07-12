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
package org.apache.jackrabbit.jcr2spi.operation;

import org.apache.jackrabbit.spi.NodeId;

import javax.jcr.RepositoryException;
import javax.jcr.ItemExistsException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.PathNotFoundException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;

/**
 * <code>Update</code>...
 */
public class Restore extends AbstractOperation {

    private final NodeId nodeId;
    private final NodeId[] versionIds;
    private final boolean removeExisting;

    private Restore(NodeId nodeId, NodeId[] versionIds, boolean removeExisting) {
        this.nodeId = nodeId;
        this.versionIds = versionIds;
        this.removeExisting = removeExisting;

        addAffectedItemId(nodeId);
        for (int i = 0; i < versionIds.length; i++) {
            addAffectedItemId(versionIds[i]);
        }
    }

    //----------------------------------------------------------< Operation >---
    /**
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        visitor.visit(this);
    }

    //----------------------------------------< Access Operation Parameters >---
    public NodeId getNodeId() {
        return nodeId;
    }

    public NodeId[] getVersionIds() {
        return versionIds;
    }

    public boolean removeExisting() {
        return removeExisting;
    }

    //------------------------------------------------------------< Factory >---
    /**
     *
     * @param nodeId
     * @param versionId
     * @return
     */
    public static Operation create(NodeId nodeId, NodeId versionId, boolean removeExisting) {
        if (nodeId == null || versionId == null) {
            throw new IllegalArgumentException("Neither nodeId nor versionId must be null.");
        }
        Restore up = new Restore(nodeId, new NodeId[] {versionId}, removeExisting);
        return up;
    }

    /**
     *
     * @param versionIds
     * @return
     */
    public static Operation create(NodeId[] versionIds, boolean removeExisting) {
        if (versionIds == null) {
            throw new IllegalArgumentException("Neither versionIds must not be null.");
        }
        Restore up = new Restore(null, versionIds, removeExisting);
        return up;
    }
}