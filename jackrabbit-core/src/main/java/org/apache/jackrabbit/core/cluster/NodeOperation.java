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
package org.apache.jackrabbit.core.cluster;

import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.value.InternalValue;

/**
 * Describes a journal record for a node change.
 */
abstract class NodeOperation extends ItemOperation {

    /**
     * Node id.
     */
    private NodeId id;

    /**
     * Node definition id.
     */
    private NodeDefId definitionId;

    /**
     * Creates a new instance of this class. Takes an operation type as paramter.
     *
     * @param operationType operation type
     */
    protected NodeOperation(int operationType) {
        super(operationType);
    }

    /**
     * Creates a new instance of a known subclass.
     *
     * @param operationType operation type
     * @return instance of this class
     */
    public static NodeOperation create(int operationType) {
        switch (operationType) {
            case ADDED:
                return new NodeAddedOperation();
            case MODIFIED:
                return new NodeModifiedOperation();
            case DELETED:
                return new NodeDeletedOperation();
            default:
                throw new IllegalArgumentException("Unknown operation type: " + operationType);
        }
    }

    /**
     * Return a flag indicating whether the node id is contained in this record.
     *
     * @return <code>true</code> if the node id is contained;
     *         <code>false</code> otherwise.
     */
    public boolean hasId() {
        return id != null;
    }

    /**
     * Return the node id.
     *
     * @return node id
     */
    public NodeId getId() {
        return id;
    }

    /**
     * Set the node id.
     *
     * @param id node id
     */
    public void setId(NodeId id) {
        this.id = id;
    }

    /**
     * Return the definition id.
     *
     * @return definition id
     */
    public NodeDefId getDefintionId() {
        return definitionId;
    }

    /**
     * Return a flag indicating whether the definition id is contained in this record.
     *
     * @return <code>true</code> if the definition id is contained;
     *         <code>false</code> otherwise.
     */
    public boolean hasDefinitionId() {
        return definitionId != null;
    }

    /**
     * Set the definition id.
     *
     * @param defintionId definition id
     */
    public void setDefintionId(NodeDefId defintionId) {
        this.definitionId = defintionId;
    }
}
