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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
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
     * Target data store, possibly <code>null</code>.
     */
    private final DataStore store;

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
     * @param store target data store
     */
    public PersistenceCopier(
            PersistenceManager source,  PersistenceManager target,
            DataStore store) {
        this.source = source;
        this.target = target;
        this.store = store;
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
     * @throws RepositoryException if the copy operation fails
     */
    public void copy(NodeId id) throws RepositoryException {
        if (!exclude.contains(id)) {
            try {
                NodeState node = source.load(id);

                for (ChildNodeEntry entry : node.getChildNodeEntries()) {
                    copy(entry.getId());
                }

                copy(node);
                exclude.add(id);
            } catch (ItemStateException e) {
                throw new RepositoryException("Unable to copy " + id, e);
            }
        }
    }

    /**
     * Copies the given node state and all associated property states
     * to the target persistence manager.
     *
     * @param sourceNode source node state
     * @throws RepositoryException if the copy operation fails
     */
    private void copy(NodeState sourceNode) throws RepositoryException {
        try {
            ChangeLog changes = new ChangeLog();

            // Copy the node state
            NodeState targetNode = target.createNew(sourceNode.getNodeId());
            targetNode.setParentId(sourceNode.getParentId());
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
                targetState.setType(sourceState.getType());
                targetState.setMultiValued(sourceState.isMultiValued());
                InternalValue[] values = sourceState.getValues();

                // special case copy of binary values
                if (sourceState.getType() == PropertyType.BINARY) {
                    InternalValue[] convertedValues = new InternalValue[values.length];
                    for (int i = 0; i < values.length; i++) {
                        try (InputStream stream = values[i].getStream()) {
                            convertedValues[i] = InternalValue.create(stream, store);
                        }
                    }
                    targetState.setValues(convertedValues);
                } else {
                    targetState.setValues(values);
                }

                if (target.exists(targetState.getPropertyId())) {
                    changes.modified(targetState);
                } else {
                    changes.added(targetState);
                }
            }

            // Copy all node references
            if (source.existsReferencesTo(sourceNode.getNodeId())) {
                changes.modified(source.loadReferencesTo(sourceNode.getNodeId()));
            } else if (target.existsReferencesTo(sourceNode.getNodeId())) {
                NodeReferences references =
                    target.loadReferencesTo(sourceNode.getNodeId());
                references.clearAllReferences();
                changes.modified(references);
            }

            // Persist the copied states
            target.store(changes);
        } catch (IOException e) {
            throw new RepositoryException(
                    "Unable to copy binary values of " + sourceNode, e);
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to copy " + sourceNode, e);
        }
    }

}
