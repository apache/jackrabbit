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
 * 
 */
public class PersistenceCopier {

    private final PersistenceManager source;

    private final PersistenceManager target;

    private final Set<NodeId> exclude = new HashSet<NodeId>();

    public PersistenceCopier(
            PersistenceManager source, PersistenceManager target) {
        this.source = source;
        this.target = target;
    }

    public void excludeNode(NodeId id) {
        exclude.add(id);
    }

    public void copy(NodeId id) throws ItemStateException {
        if (!exclude.contains(id)) {
            NodeState node = source.load(id);

            for (ChildNodeEntry entry : node.getChildNodeEntries()) {
                copy(entry.getId());
            }

            copy(node);
        }
    }

    private void copy(NodeState sourceNode) throws ItemStateException {
        ChangeLog changes = new ChangeLog();

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

        target.store(changes);
    }

}
