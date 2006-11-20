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

import org.apache.jackrabbit.jcr2spi.state.NodeState;

import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

/**
 * <code>LockOperation</code>...
 */
public class LockOperation extends AbstractOperation {

    private final NodeState nodeState;
    private final boolean isDeep;
    private final boolean isSessionScoped;

    private LockOperation(NodeState nodeState, boolean isDeep, boolean isSessionScoped) {
        this.nodeState = nodeState;
        this.isDeep = isDeep;
        this.isSessionScoped = isSessionScoped;

        this.addAffectedItemState(nodeState);
    }

    //----------------------------------------------------------< Operation >---
    /**
     * @see Operation#accept(OperationVisitor)
     */
    public void accept(OperationVisitor visitor) throws RepositoryException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException {
        visitor.visit(this);
    }

    /**
     * @see Operation#persisted()
     */
    public void persisted() {
        // TODO
    }

    //----------------------------------------< Access Operation Parameters >---
    public NodeState getNodeState() {
        return nodeState;
    }

    public boolean isDeep() {
        return isDeep;
    }

    public boolean isSessionScoped() {
        return isSessionScoped;
    }

    //------------------------------------------------------------< Factory >---
    /**
     *
     * @param nodeState
     * @param isDeep
     * @return
     */
    public static Operation create(NodeState nodeState, boolean isDeep, boolean isSessionScoped) {
        Operation lck = new LockOperation(nodeState, isDeep, isSessionScoped);
        return lck;
    }
}