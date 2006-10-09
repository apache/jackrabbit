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
 * This class implements the node type.
 */
public final class NodeType
        implements Comparable {
    /**
     * Name of type.
     */
    private final String name;

    /**
     * Namespace.
     */
    private String namespace;

    /**
     * Super types.
     */
    private final HashSet superTypes;

    /**
     * Mixin.
     */
    private boolean mixin;

    /**
     * Orderable.
     */
    private boolean orderable;

    /**
     * List of child items.
     */
    private final List itemDefs;

    /**
     * Node type set.
     */
    private NodeTypeSet nodeTypeSet;

    /**
     * Construct the node type.
     */
    public NodeType(String name) {
        this.name = name;
        this.itemDefs = new ArrayList();
        this.superTypes = new HashSet();
    }

    /**
     * Return the name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Return the namespace URI.
     */
    public String getNamespace() {
        return this.namespace != null ? this.namespace : "";
    }

    /**
     * Set the namespace URI.
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Return the super types.
     */
    public String[] getSuperTypes() {
        if (!isMixin()) {
            addSuperType("nt:base");
        }

        return (String[]) this.superTypes.toArray(new String[this.superTypes.size()]);
    }

    /**
     * Set the super types.
     */
    public void setSuperTypes(String[] superTypes) {
        setSuperTypes(Arrays.asList(superTypes));
    }

    /**
     * Set the super types.
     */
    public void setSuperTypes(Collection superTypes) {
        this.superTypes.clear();
        this.superTypes.addAll(superTypes != null ? superTypes : Collections.EMPTY_LIST);
    }

    /**
     * Add a super type.
     */
    public void addSuperType(String superType) {
        this.superTypes.add(superType);
    }

    /**
     * Return true if mixin type.
     */
    public boolean isMixin() {
        return this.mixin;
    }

    /**
     * Set true if mixin type.
     */
    public void setMixin(boolean mixin) {
        this.mixin = mixin;
    }

    /**
     * Return true if it has orderable child nodes.
     */
    public boolean isOrderable() {
        return this.orderable;
    }

    /**
     * Set true if it has orderable child nodes.
     */
    public void setOrderable(boolean orderable) {
        this.orderable = orderable;
    }

    /**
     * Return the primary item name.
     */
    public ItemDef getPrimaryItem() {
        for (Iterator i = this.itemDefs.iterator(); i.hasNext();) {
            ItemDef def = (ItemDef) i.next();
            if (def.isPrimary()) {
                return def;
            }
        }

        return null;
    }

    /**
     * Return the primary item name.
     */
    public String getPrimaryItemName() {
        ItemDef def = getPrimaryItem();
        return def != null ? def.getName() : "";
    }

    /**
     * Add item def.
     */
    public void addItemDef(ItemDef def) {
        this.itemDefs.add(def);
        def.setDeclaringNodeType(this);
    }

    /**
     * Find all inherited nodes.
     */
    public NodeType[] getInheritedTypes() {
        return this.nodeTypeSet.getInheritedTypes(this);
    }

    /**
     * Return item def list.
     */
    private List getItemDefList(boolean properties, boolean nodes) {
        return getItemDefList(this.itemDefs, properties, nodes);
    }

    /**
     * Return inherited item def list.
     */
    private List getInheritedItemDefList(boolean properties, boolean nodes) {
        ArrayList list = new ArrayList();
        NodeType[] types = getInheritedTypes();

        for (int i = 0; i < types.length; i++) {
            list.addAll(Arrays.asList(types[i].getItemDefs()));
        }

        return getItemDefList(list, properties, nodes);
    }

    /**
     * Return item def list.
     */
    private List getItemDefList(List source, boolean properties, boolean nodes) {
        ArrayList list = new ArrayList();
        for (Iterator i = source.iterator(); i.hasNext();) {
            ItemDef def = (ItemDef) i.next();

            if (properties && (def instanceof PropertyDef)) {
                list.add(def);
            }

            if (nodes && (def instanceof NodeDef)) {
                list.add(def);
            }
        }

        Collections.sort(list);
        return list;
    }

    /**
     * Return item defs.
     */
    public ItemDef[] getItemDefs() {
        List list = getItemDefList(true, true);
        return (ItemDef[]) list.toArray(new ItemDef[list.size()]);
    }

    /**
     * Return property defs.
     */
    public PropertyDef[] getPropertyDefs() {
        List list = getItemDefList(true, false);
        return (PropertyDef[]) list.toArray(new PropertyDef[list.size()]);
    }

    /**
     * Return node defs.
     */
    public NodeDef[] getNodeDefs() {
        List list = getItemDefList(false, true);
        return (NodeDef[]) list.toArray(new NodeDef[list.size()]);
    }

    /**
     * Return inherited item defs.
     */
    public ItemDef[] getInheritedItemDefs() {
        List list = getInheritedItemDefList(true, true);
        return (ItemDef[]) list.toArray(new ItemDef[list.size()]);
    }

    /**
     * Return inherited property defs.
     */
    public PropertyDef[] getInheritedPropertyDefs() {
        List list = getInheritedItemDefList(true, false);
        return (PropertyDef[]) list.toArray(new PropertyDef[list.size()]);
    }

    /**
     * Return inherited node defs.
     */
    public NodeDef[] getInheritedNodeDefs() {
        List list = getInheritedItemDefList(false, true);
        return (NodeDef[]) list.toArray(new NodeDef[list.size()]);
    }

    /**
     * Return the node type set.
     */
    public NodeTypeSet getNodeTypeSet() {
        return this.nodeTypeSet;
    }

    /**
     * Set the node type set.
     */
    public void setNodeTypeSet(NodeTypeSet nodeTypeSet) {
        this.nodeTypeSet = nodeTypeSet;
    }

    /**
     * Compare to.
     */
    public int compareTo(Object o) {
        return this.name.compareTo(((NodeType) o).name);
    }
}
