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
import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.NodeId;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.AccessDeniedException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionException;
import javax.jcr.lock.LockException;

/**
 * <code>AddNode</code>...
 */
public class AddNode extends AbstractOperation {

    private static Logger log = LoggerFactory.getLogger(AddNode.class);

    private final NodeId parentId;
    private final NodeState parentState;
    private final QName nodeName;
    private final QName nodeTypeName;
    private final String uuid;

    private AddNode(NodeState parentState, QName nodeName, QName nodeTypeName, String uuid) {
        this.parentId = parentState.getNodeId();
        this.parentState = parentState;
        this.nodeName = nodeName;
        this.nodeTypeName = nodeTypeName;
        this.uuid = uuid;

        addAffectedItemState(parentState);
    }

    //----------------------------------------------------------< Operation >---
    /**
     *
     * @param visitor
     */
    public void accept(OperationVisitor visitor) throws LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        visitor.visit(this);
    }

    /**
     * Throws UnsupportedOperationException
     *
     * @see Operation#persisted(CacheBehaviour)
     * @param cacheBehaviour
     */
    public void persisted(CacheBehaviour cacheBehaviour) {
        throw new UnsupportedOperationException("persisted() not implemented for transient modification.");
    }
    //----------------------------------------< Access Operation Parameters >---
    public NodeId getParentId() {
        return parentId;
    }

    public NodeState getParentState() {
        return parentState;
    }

    public QName getNodeName() {
        return nodeName;
    }

    public QName getNodeTypeName() {
        return nodeTypeName;
    }

    public String getUuid() {
        return uuid;
    }

    //------------------------------------------------------------< Factory >---

    public static Operation create(NodeState parentState, QName nodeName,
                                   QName nodeTypeName, String uuid) {
        AddNode an = new AddNode(parentState, nodeName, nodeTypeName, uuid);
        return an;
    }
}