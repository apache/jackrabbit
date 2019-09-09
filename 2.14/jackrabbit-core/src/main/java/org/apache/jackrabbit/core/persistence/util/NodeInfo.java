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
package org.apache.jackrabbit.core.persistence.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;

/**
 * Holds structural information about a node. Used by the consistency checker and garbage collector.
 */
public final class NodeInfo {

    /**
     * The same node id in a NodeInfo graph typically occurs three times: as a the id of the current
     * NodeInfo, as the parent to another NodeInfo, and as a child of another NodeInfo. In order to
     * minimize the memory footprint use an NodeId object pool.
     */
    private static final ConcurrentMap<NodeId,NodeId> nodeIdPool = new ConcurrentHashMap<NodeId, NodeId>(1000);

    /**
     * The node id
     */
    private final NodeId nodeId;

    /**
     * The parent node id
     */
    private final NodeId parentId;

    /**
     * The child ids
     */
    private List<NodeId> children;

    /**
     * Map of reference property names of this node with their node id values
     */
    private Map<Name, List<NodeId>> references;

    /**
     * Whether this node is referenceable or not
     */
    private boolean isReferenceable;

    /**
     * Whether this node has blob properties in data storage
     */
    private boolean hasBlobsInDataStore;

    /**
     * Create a new NodeInfo object from a bundle
     *
     * @param bundle the node bundle
     */
    public NodeInfo(final NodePropBundle bundle) {
        nodeId = getNodeId(bundle.getId());
        parentId = getNodeId(bundle.getParentId());

        List<NodePropBundle.ChildNodeEntry> childNodeEntries = bundle.getChildNodeEntries();
        if (!childNodeEntries.isEmpty()) {
            children = new ArrayList<NodeId>(childNodeEntries.size());
            for (NodePropBundle.ChildNodeEntry entry : bundle.getChildNodeEntries()) {
                children.add(getNodeId(entry.getId()));
            }
        } else {
            children = Collections.emptyList();
        }

        for (NodePropBundle.PropertyEntry entry : bundle.getPropertyEntries()) {
            if (entry.getType() == PropertyType.REFERENCE) {
                if (references == null) {
                    references = new HashMap<Name, List<NodeId>>(4);
                }
                List<NodeId> values = new ArrayList<NodeId>(entry.getValues().length);
                for (InternalValue value : entry.getValues()) {
                    values.add(getNodeId(value.getNodeId()));
                }
                references.put(entry.getName(), values);
            }
            else if (entry.getType() == PropertyType.BINARY) {
                for (InternalValue internalValue : entry.getValues()) {
                    if (internalValue.isInDataStore()) {
                        hasBlobsInDataStore = true;
                        break;
                    }
                }

            }
        }

        if (references == null) {
            references = Collections.emptyMap();
        }
        isReferenceable = bundle.isReferenceable();
    }

    /**
     * @return the node id of this node
     */
    public NodeId getId() {
        return nodeId;
    }

    /**
     * @return the parent id of this node
     */
    public NodeId getParentId() {
        return parentId;
    }

    /**
     * @return the child ids of this node
     */
    public List<NodeId> getChildren() {
        return children;
    }

    /**
     * @return the reference properties along with their node id values of this node
     */
    public Map<Name, List<NodeId>> getReferences() {
        return references;
    }

    /**
     * @return whether the node represented by this node info is referenceable
     */
    public boolean isReferenceable() {
        return isReferenceable;
    }

    /**
     * @return whether the node has blob properties that are inside the data storage
     */
    public boolean hasBlobsInDataStore() {
        return hasBlobsInDataStore;
    }

    /**
     * Simple pool implementation to minimize memory overhead from node id objects
     * @param nodeId  node id to cache
     * @return  the cached node id
     */
    private static NodeId getNodeId(NodeId nodeId) {
        if (nodeId == null) {
            return null;
        }
        NodeId cached = nodeIdPool.get(nodeId);
        if (cached == null) {
            cached = nodeIdPool.putIfAbsent(nodeId, nodeId);
            if (cached == null) {
                cached = nodeId;
            }
        }
        return cached;
    }

    /**
     * Clear the NodeId pool.
     */
    public static void clearPool() {
        nodeIdPool.clear();
    }
}
