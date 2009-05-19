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
package org.apache.jackrabbit.core.persistence;

import java.util.HashSet;
import java.util.Set;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;

/**
 * Tool for copying item states from one persistence manager to another.
 * Used for backing up or migrating repository content.
 *
 * @since Apache Jackrabbit 1.6
 */
public class PersistenceCopier {

    /**
     * Source persistence manager.
     */
    private final PersistenceManager source;

    /**
     * Target persistence manager.
     */
    private final PersistenceManager target;

    /**
     * Identifiers of the nodes that have already been copied or that
     * should explicitly not be copied. Used to avoid duplicate copies
     * of shareable nodes and to avoid trying to copy "missing" nodes
     * like the virtual "/jcr:system" node.
     */
    private final Set<NodeId> exclude = new HashSet<NodeId>();

    /**
     * Creates a tool for copying content from one persistence manager
     * to another.
     *
     * @param source source persistence manager
     * @param target target persistence manager
     */
    public PersistenceCopier(
            PersistenceManager source, PersistenceManager target) {
        this.source = source;
        this.target = target;
    }

    /**
     * Explicitly exclude the identified node from being copied. Used for
     * excluding virtual nodes like "/jcr:system" from the copy process.
     *
     * @param id identifier of the node to be excluded
     */
    public void excludeNode(NodeId id) {
        exclude.add(id);
    }

    /**
     * Recursively copies the identified node and all its descendants.
     * Explicitly excluded nodes and nodes that have already been copied
     * are automatically skipped.
     *
     * @param id identifier of the node to be copied
     * @throws ItemStateException if the copy operation fails
     */
    public void copy(NodeId id) throws ItemStateException {
        if (!exclude.contains(id)) {
            NodeState node = source.load(id);

            for (ChildNodeEntry entry : node.getChildNodeEntries()) {
                copy(entry.getId());
            }

            copy(node);
            exclude.add(id);
        }
    }

    /**
     * Copies the given node state and all associated property states
     * to the target persistence manager.
     *
     * @param sourceNode source node state
     * @throws ItemStateException if the copy operation fails
     */
    private void copy(NodeState sourceNode) throws ItemStateException {
        ChangeLog changes = new ChangeLog();

        // Copy the node state
        NodeState targetNode = target.createNew(sourceNode.getNodeId());
        targetNode.setParentId(sourceNode.getParentId());
        targetNode.setDefinitionId(sourceNode.getDefinitionId());
        targetNode.setNodeTypeName(sourceNode.getNodeTypeName());
        targetNode.setMixinTypeNames(sourceNode.getMixinTypeNames());
        targetNode.setPropertyNames(sourceNode.getPropertyNames());
        targetNode.setChildNodeEntries(sourceNode.getChildNodeEntries());
        if (target.exists(targetNode.getNodeId())) {
            changes.modified(targetNode);
        } else {
            changes.added(targetNode);
        }

        // Copy all associated property states
        for (Name name : sourceNode.getPropertyNames()) {
            PropertyId id = new PropertyId(sourceNode.getNodeId(), name);
            PropertyState sourceState = source.load(id);
            PropertyState targetState = target.createNew(id);
            targetState.setDefinitionId(sourceState.getDefinitionId());
            targetState.setType(sourceState.getType());
            targetState.setMultiValued(sourceState.isMultiValued());
            // TODO: Copy binaries?
            targetState.setValues(sourceState.getValues());
            if (target.exists(targetState.getPropertyId())) {
                changes.modified(targetState);
            } else {
                changes.added(targetState);
            }
        }

        // TODO: Copy node references?

        // Persist the copied states
        target.store(changes);
    }

}
