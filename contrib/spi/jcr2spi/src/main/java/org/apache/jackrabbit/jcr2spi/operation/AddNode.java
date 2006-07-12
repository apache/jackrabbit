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
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.QName;
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
import java.util.List;

/**
 * <code>AddNode</code>...
 */
public class AddNode extends AbstractOperation {

    private static Logger log = LoggerFactory.getLogger(AddNode.class);

    private final NodeId parentId;
    private final QName nodeName;
    private final QName nodeTypeName;
    private final NodeId id;

    private AddNode(NodeId parentId, QName nodeName, QName nodeTypeName, NodeId id) {
        this.parentId = parentId;
        this.nodeName = nodeName;
        this.nodeTypeName = nodeTypeName;
        this.id = id;
        addAffectedItemId(parentId);
        addAffectedItemId(id);
    }

    //----------------------------------------------------------< Operation >---
    /**
     *
     * @param visitor
     */
    public void accept(OperationVisitor visitor) throws LockException, ConstraintViolationException, AccessDeniedException, ItemExistsException, NoSuchNodeTypeException, UnsupportedRepositoryOperationException, VersionException, RepositoryException {
        visitor.visit(this);
    }

    //----------------------------------------< Access Operation Parameters >---
    public NodeId getParentId() {
        return parentId;
    }

    public QName getNodeName() {
        return nodeName;
    }

    public QName getNodeTypeName() {
        return nodeTypeName;
    }

    public String getUuid() {
        return (id != null) ? id.getUUID() : null;
    }

    //------------------------------------------------------------< Factory >---

    public static Operation create(NodeState parentState, QName nodeName,
                                   QName nodeTypeName, NodeId id) {
        AddNode an = new AddNode(parentState.getNodeId(), nodeName, nodeTypeName, id);
        return an;
    }

    public static NodeId getLastCreated(NodeState parentState, QName nodeName) {
        // TODO: check if this really retrieves the child state that was created before
        // problem: we don't know the id of the nodestate in advance -> retrieval of new state not possible.
        List cne = parentState.getChildNodeEntries(nodeName);
        NodeId childId = ((NodeState.ChildNodeEntry)cne.get(cne.size()-1)).getId();
        return childId;
    }
}