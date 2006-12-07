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
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.ItemStateFactory;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;

/**
 * <code>PathElementReference</code> implements a {@link ChildNodeEntry} based
 * on a {@link Path.PathElement}.
 */
class PathElementReference extends ChildNodeReference implements ChildNodeEntry {

    /**
     * IdFactory to create an ItemId based on the parent NodeId
     */
    private final IdFactory idFactory;

    /**
     * Creates a new <code>PathElementReference</code>
     *
     * @param parent    the <code>ItemState</code> that owns this child item
     *                  reference.
     * @param name      the name of the child node.
     * @param isf       the item state factory to create the node state.
     * @param idFactory the <code>IdFactory</code> to create new ItemIds
     */
    PathElementReference(NodeState parent, QName name,
                         ItemStateFactory isf, IdFactory idFactory) {
        super(parent, name, isf);
        this.idFactory = idFactory;
    }

    /**
     * Creates a new <code>PathElementReference</code> with the given parent
     * <code>NodeState</code> and an already initialized child node state.
     *
     * @param child  the child node state.
     * @param isf    the item state factory to re-create the node state.
     * @param idFactory the <code>IdFactory</code> to create new ItemIds
     */
    PathElementReference(NodeState child, ItemStateFactory isf,
                         IdFactory idFactory) {
        super(child, isf);
        this.idFactory = idFactory;
    }

    /**
     * @inheritDoc
     * @see ChildItemReference#doResolve()
     * <p/>
     * Returns a <code>NodeState</code>.
     */
    protected ItemState doResolve()
            throws NoSuchItemStateException, ItemStateException {
        return isf.createNodeState(getId(), parent);
    }

    /**
     * @inheritDoc
     * @see ChildNodeEntry#getId()
     */
    public NodeId getId() {
        return idFactory.createNodeId(parent.getNodeId(), Path.create(getName(), getIndex()));
    }

    /**
     * This implementation always returns <code>null</code>.
     * @inheritDoc
     * @see ChildNodeEntry#getUniqueID()
     */
    public String getUniqueID() {
        return null;
    }
}
