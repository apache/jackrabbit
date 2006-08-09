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

import org.apache.jackrabbit.name.QName;

/**
 * <code>ChildNodeReference</code> implements common functionality for child
 * node entry implementations.
 */
abstract class ChildNodeReference extends ChildItemReference implements ChildNodeEntry {

    /**
     * Creates a new <code>ChildNodeReference</code> with the given parent
     * <code>NodeState</code>.
     *
     * @param parent the <code>NodeState</code> that owns this child node
     *               reference.
     * @param name   the name of the child item.
     * @param isf    the item state factory to create the item state.
     */
    public ChildNodeReference(NodeState parent, QName name, ItemStateFactory isf) {
        super(parent, name, isf);
    }

    /**
     * Creates a new <code>ChildNodeReference</code> with the given parent
     * <code>NodeState</code> and an already initialized child node state.
     *
     * @param child  the child node state.
     * @param isf    the item state factory to re-create the node state.
     */
    public ChildNodeReference(NodeState child, ItemStateFactory isf) {
        super(child.getParent(), child, null, isf); // TODO: get name from child instead of null
    }

    /**
     * @inheritDoc
     * @see ChildNodeEntry#getIndex()
     */
    public int getIndex() {
        return parent.getChildNodeIndex(getName(), this);
    }

    /**
     * @inheritDoc
     * @see ChildNodeEntry#getName()
     */
    public QName getName() {
        return name;
    }

    /**
     * @inheritDoc
     * @see ChildNodeEntry#getNodeState()
     */
    public NodeState getNodeState()
            throws NoSuchItemStateException, ItemStateException {
        return (NodeState) resolve();
    }

    /**
     * @inheritDoc
     * @see ChildNodeEntry#isAvailable()
     */
    public boolean isAvailable() {
        return isResolved();
    }
}
