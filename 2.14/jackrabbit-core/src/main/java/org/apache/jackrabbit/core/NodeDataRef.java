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

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ItemState;

/**
 * Data object representing a node. Used for share-siblings of a shareable node
 * that is already loaded.
 */
class NodeDataRef extends AbstractNodeData {

    /** Referenced data object */
    private final AbstractNodeData data;

    /**
     * Create a new instance of this class.
     *
     * @param data data to reference
     * @param primaryParentId primary parent id
     */
    protected NodeDataRef(AbstractNodeData data, NodeId primaryParentId) {
        super(data.getId());

        this.data = data;

        setPrimaryParentId(primaryParentId);
    }

    /**
     * {@inheritDoc}
     *
     * This implementation returns the state of the referenced data object.
     */
    public ItemState getState() {
        return data.getState();
    }

    /**
     * {@inheritDoc}
     *
     * This implementation sets the state of the referenced data object.
     */
    protected void setState(ItemState state) {
        data.setState(state);
    }

    /**
     * {@inheritDoc}
     *
     * This implementation returns the definition of the referenced data object.
     * @throws RepositoryException if the definition cannot be retrieved.
     */
    public ItemDefinition getDefinition() throws RepositoryException {
        return data.getDefinition();
    }

    /**
     * {@inheritDoc}
     *
     * This implementation sets the definition of the referenced data object.
     */
    protected void setDefinition(ItemDefinition definition) {
        data.setDefinition(definition);
    }
}
