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
package org.apache.jackrabbit.ntdoc.model;

import java.util.*;

/**
 * This class defines the node type set.
 */
public final class NodeTypeSet {
    /**
     * Node types.
     */
    private final Map nodeTypes;

    /**
     * Construct the node type set.
     */
    public NodeTypeSet() {
        this.nodeTypes = new TreeMap();
    }

    /**
     * Return node types.
     */
    public NodeType[] getNodeTypes() {
        return getNodeTypes(true, true);
    }

    /**
     * Return node types.
     */
    public NodeType[] getMixinNodeTypes() {
        return getNodeTypes(true, false);
    }

    /**
     * Return node types.
     */
    public NodeType[] getConcreteNodeTypes() {
        return getNodeTypes(false, true);
    }

    /**
     * Return node types.
     */
    public NodeType[] getNodeTypes(boolean mixin, boolean concrete) {
        ArrayList list = new ArrayList();

        for (Iterator i = this.nodeTypes.values().iterator(); i.hasNext();) {
            NodeType nt = (NodeType) i.next();
            if (nt.isMixin() && mixin) {
                list.add(nt);
            } else if (!nt.isMixin() && concrete) {
                list.add(nt);
            }
        }

        Collections.sort(list);
        return (NodeType[]) list.toArray(new NodeType[list.size()]);
    }

    /**
     * Return node type by name.
     */
    public NodeType getNodeType(String name) {
        if (name != null) {
            return (NodeType) this.nodeTypes.get(name);
        } else {
            return null;
        }
    }

    /**
     * Add node type.
     */
    public void addNodeType(NodeType nodeType) {
        if (nodeType != null) {
            this.nodeTypes.put(nodeType.getName(), nodeType);
            nodeType.setNodeTypeSet(this);
        }
    }

    /**
     * Add node types.
     */
    public void addNodeTypes(NodeType[] nodeTypes) {
        for (int i = 0; i < nodeTypes.length; i++) {
            addNodeType(nodeTypes[i]);
        }
    }

    /**
     * Add node types.
     */
    public void addNodeTypes(NodeTypeSet nodeTypes) {
        addNodeTypes(nodeTypes.getNodeTypes());
    }

    /**
     * Return the size of set.
     */
    public int getSize() {
        return this.nodeTypes.size();
    }

    /**
     * Find all inherited nodes.
     */
    public NodeType[] getInheritedTypes(NodeType type) {
        Set set = new TreeSet();
        findInheritedTypes(type, set);
        return (NodeType[]) set.toArray(new NodeType[set.size()]);
    }

    /**
     * Find all inherited nodes.
     */
    private void findInheritedTypes(NodeType type, Set nodes) {
        String[] supertypes = type.getSuperTypes();
        for (int i = 0; i < supertypes.length; i++) {
            NodeType tmp = getNodeType(supertypes[i]);
            if ((tmp != null) && !nodes.contains(tmp)) {
                nodes.add(tmp);
                findInheritedTypes(tmp, nodes);
            }
        }
    }

    /**
     * Return the node type at index.
     */
    public NodeType getNodeType(int index) {
        NodeType[] list = getNodeTypes();
        if ((index >= 0) && (index < list.length)) {
            return list[index];
        } else {
            return null;
        }
    }
}
