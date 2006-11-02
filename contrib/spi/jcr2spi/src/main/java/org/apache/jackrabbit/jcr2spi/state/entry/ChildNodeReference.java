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
package org.apache.jackrabbit.jcr2spi.state.entry;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateFactory;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;

/**
 * <code>ChildNodeReference</code> implements common functionality for child
 * node entry implementations.
 */
public abstract class ChildNodeReference extends ChildItemReference implements ChildNodeEntry {

    /**
     * Creates a new <code>ChildNodeEntry</code> for an already initialized
     * child node state. The child node must already be attached to its parent.
     *
     * @param child     the child node state.
     * @param isf       the item state factory to re-create node states.
     * @param idFactory the <code>IdFactory</code> to create new ItemIds
     * @return
     */
    public static ChildNodeEntry create(NodeState child, ItemStateFactory isf,
                                        IdFactory idFactory) {
        ChildNodeEntry cne;
        if (child.getUUID() == null) {
            cne = new PathElementReference(child, isf, idFactory);
        } else {
            cne = new UUIDReference(child, isf);
        }
        return cne;
    }

    /**
     * Creates a <code>ChildNodeEntry</code> instance based on
     *  <code>nodeName</code> and an optional <code>uuid</code>.
     *
     * @param parent
     * @param childName
     * @param childUUID
     * @param isf
     * @param idFactory
     * @return
     */
    public static ChildNodeEntry create(NodeState parent, QName childName,
                                        String childUUID, ItemStateFactory isf,
                                        IdFactory idFactory) {
        if (childUUID == null) {
            return new PathElementReference(parent, childName, isf, idFactory);
        } else {
            return new UUIDReference(parent, idFactory.createNodeId(childUUID), isf, childName);
        }
    }

    /**
     * Creates a new <code>ChildNodeReference</code> with the given parent
     * <code>NodeState</code>.
     *
     * @param parent the <code>NodeState</code> that owns this child node
     *               reference.
     * @param name   the name of the child item.
     * @param isf    the item state factory to create the item state.
     */
    ChildNodeReference(NodeState parent, QName name, ItemStateFactory isf) {
        super(parent, name, isf);
    }


    /**
     * Creates a new <code>ChildNodeReference</code> with the given parent
     * <code>NodeState</code> and an already initialized child node state.
     *
     * @param child  the child node state.
     * @param isf    the item state factory to re-create the node state.
     */
    ChildNodeReference(NodeState child, ItemStateFactory isf) {
        super(child.getParent(), child, child.getQName(), isf);
    }

    /**
     * @inheritDoc
     * @see ChildNodeEntry#getIndex()
     */
    public int getIndex() {
        return parent.getChildNodeIndex(this);
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
