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
package org.apache.jackrabbit.jcr2spi.state;

import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.name.QName;

/**
 * <code>UUIDReference</code> implements a {@link ChildNodeEntry} based on a
 * <code>NodeId</code> with just a UUID and no relative path component.
 */
class UUIDReference extends ChildNodeReference implements ChildNodeEntry {

    /**
     * The <code>NodeId</code> with just a UUID that references the child node.
     */
    private final NodeId childId;

    /**
     * Creates a new <code>UUIDReference</code>.
     *
     * @param parent  the <code>NodeState</code> that owns this child node
     *                reference.
     * @param childId the id of the referenced <code>NodeState</code>. This id
     *                must not have a relative path component.
     * @param name    the name of the child node.
     * @param isf     the item state factory to create the node state.
     * @throws IllegalArgumentException if <code>childId</code> has a relative
     *                                  path component.
     */
    public UUIDReference(NodeState parent, NodeId childId, ItemStateFactory isf, QName name) {
        super(parent, name, isf);
        if (childId.getRelativePath() == null) {
            throw new IllegalArgumentException("childId must not contain a relative path");
        }
        this.childId = childId;
    }

    /**
     * Creates a new <code>UUIDReference</code> with the given parent
     * <code>NodeState</code> and an already initialized child node state.
     *
     * @param child     the child node state.
     * @param isf       the item state factory to re-create the node state.
     * @throws IllegalArgumentException if the id of <code>child</code> has a
     *                                  relative path component.
     */
    public UUIDReference(NodeState child, ItemStateFactory isf) {
        super(child, isf);
        this.childId = child.getNodeId();
        if (childId.getRelativePath() == null) {
            throw new IllegalArgumentException("id of child must not contain a relative path");
        }
    }

    /**
     * @inheritDoc
     * @see ChildItemReference#doResolve()
     * <p/>
     * Returns a <code>NodeState</code>.
     */
    protected ItemState doResolve()
            throws NoSuchItemStateException, ItemStateException {
        return isf.createNodeState(childId, getParent());
    }

    /**
     * @inheritDoc
     * @see ChildNodeEntry#getId()
     */
    public NodeId getId() {
        return childId;
    }

    /**
     * This implementation always returns a non-null value.
     * @inheritDoc
     * @see ChildNodeEntry#getUUID()
     */
    public String getUUID() {
        return childId.getUUID();
    }
}
