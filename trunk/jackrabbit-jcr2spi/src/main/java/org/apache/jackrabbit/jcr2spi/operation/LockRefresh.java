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

import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.UnsupportedRepositoryOperationException;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.spi.NodeId;

import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * <code>LockRefresh</code>...
 */
public class LockRefresh extends AbstractOperation {

    private final NodeState nodeState;

    private LockRefresh(NodeState nodeState) {
        this.nodeState = nodeState;

        // NOTE: affected-states only needed for transient modifications
    }

    //----------------------------------------------------------< Operation >---
    /**
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws RepositoryException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException {
        assert status == STATUS_PENDING;
        visitor.visit(this);
    }

    /**
     * @see Operation#persisted()
     */
    public void persisted() {
        assert status == STATUS_PENDING;
        status = STATUS_PERSISTED;
        // nothing to do.
    }

    //----------------------------------------< Access Operation Parameters >---
    public NodeId getNodeId() throws RepositoryException {
        return nodeState.getNodeId();
    }

    //------------------------------------------------------------< Factory >---
    /**
     *
     * @param nodeState
     * @return
     */
    public static Operation create(NodeState nodeState) {
        Operation lck = new LockRefresh(nodeState);
        return lck;
    }
}