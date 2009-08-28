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
package org.apache.jackrabbit.core;

import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;

/**
 * Data object referenced by different <code>ItemImpl</code> instances that
 * all represent the same item, i.e. items having the same <code>ItemId</code>.
 */
public abstract class ItemData {

    /** Associated item id */
    private final ItemId id;

    /** Associated item state */
    private ItemState state;

    /** Associated item definition */
    private ItemDefinition definition;

    /** Status */
    private int status;

    /** The item manager */
    private ItemManager itemMgr;

    /**
     * Create a new instance of this class.
     *
     * @param state item state
     * @param itemMgr item manager
     */
    protected ItemData(ItemState state, ItemManager itemMgr) {
        this.id = state.getId();
        this.state = state;
        this.itemMgr = itemMgr;
        this.status = ItemImpl.STATUS_NORMAL;
    }

    /**
     * Create a new instance of this class.
     *
     * @param id item id
     */
    protected ItemData(ItemId id) {
        this.id = id;
        this.status = ItemImpl.STATUS_NORMAL;
    }

    /**
     * Return the associated item state.
     *
     * @return item state
     */
    public ItemState getState() {
        return state;
    }

    /**
     * Set the associated item state.
     *
     * @param state item state
     */
    protected void setState(ItemState state) {
        this.state = state;
    }

    /**
     * Return the associated item definition.
     *
     * @return item definition
     * @throws RepositoryException if the definition cannot be retrieved.
     */
    public ItemDefinition getDefinition() throws RepositoryException {
        if (definition == null && itemMgr != null) {
            if (isNode()) {
                definition = itemMgr.getDefinition((NodeState) state);
            } else {
                definition = itemMgr.getDefinition((PropertyState) state);
            }
        }
        return definition;
    }

    /**
     * Set the associated item definition.
     *
     * @param definition item definition
     */
    protected void setDefinition(ItemDefinition definition) {
        this.definition = definition;
    }

    /**
     * Return the status.
     *
     * @return status
     */
    public int getStatus() {
        return status;
    }

    /**
     * Set the status.
     *
     * @param status
     */
    protected void setStatus(int status) {
        this.status = status;
    }

    /**
     * Return a flag indicating whether item is a node.
     *
     * @return <code>true</code> if this item is a node;
     *         <code>false</code> otherwise.
     */
    public boolean isNode() {
        return false;
    }

    /**
     * Return the id associated with this item.
     *
     * @return item id
     */
    public ItemId getId() {
        return id;
    }

    /**
     * Return the parent id of this item.
     *
     * @return parent id
     */
    public NodeId getParentId() {
        return getState().getParentId();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return getId().toString();
    }
}
