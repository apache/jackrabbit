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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.PropertyType;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class provides a simple structure to hold the nodestate and related
 * propertystate data.
 */
public class NodePropBundle {

    /**
     * default logger
     */
    private static Logger log = LoggerFactory.getLogger(NodePropBundle.class);

    /**
     * the node id
     */
    private final NodeId id;

    /**
     * the parent node id
     */
    private NodeId parentId;

    /**
     * the nodetype name
     */
    private Name nodeTypeName;

    /**
     * the mixintype names
     */
    private Set<Name> mixinTypeNames;

    /**
     * the child node entries
     */
    private LinkedList<NodePropBundle.ChildNodeEntry> childNodeEntries = new LinkedList<NodePropBundle.ChildNodeEntry>();

    /**
     * the properties
     */
    private HashMap<Name, PropertyEntry> properties = new HashMap<Name, PropertyEntry>();

    /**
     * flag that indicates if this bundle is new
     */
    private boolean isNew = true;

    /**
     * flag that indicates if this bundle is referenceable
     */
    private boolean isReferenceable;

    /**
     * the mod count
     */
    private short modCount;

    /**
     * the size
     */
    private long size;
    
    /**
     * Shared set, consisting of the parent ids of this shareable node. This
     * entry is <code>null</code> if this node is not shareable.
     */
    private Set<NodeId> sharedSet;

    /**
     * Creates a "new" bundle with the given id
     *
     * @param id the node id
     */
    public NodePropBundle(NodeId id) {
        this.id = id;
    }

    /**
     * Creates a bundle from the given state
     *
     * @param state the node state
     */
    public NodePropBundle(NodeState state) {
        this(state.getNodeId());
        update(state);
    }

    /**
     * Updates this bundle with values from the given state.
     * @param state the node state
     */
    public void update(NodeState state) {
        if (!id.equals(state.getNodeId())) {
            // sanity check
            throw new IllegalArgumentException("Not allowed to update foreign state.");
        }
        parentId = state.getParentId();
        nodeTypeName = state.getNodeTypeName();
        mixinTypeNames = state.getMixinTypeNames();
        isReferenceable = state.hasPropertyName(NameConstants.JCR_UUID);
        modCount = state.getModCount();
        List<org.apache.jackrabbit.core.state.ChildNodeEntry> list = state.getChildNodeEntries();
        childNodeEntries.clear();
        for (org.apache.jackrabbit.core.state.ChildNodeEntry cne : list) {
            addChildNodeEntry(cne.getName(), cne.getId());
        }
        sharedSet = state.getSharedSet();
    }

    /**
     * Creates a node state from the values of this bundle
     * @param pMgr the persistence manager
     * @return the new nodestate
     */
    public NodeState createNodeState(PersistenceManager pMgr) {
        NodeState state = pMgr.createNew(id);
        state.setParentId(parentId);
        state.setNodeTypeName(nodeTypeName);
        state.setMixinTypeNames(mixinTypeNames);
        state.setModCount(modCount);
        for (ChildNodeEntry e : childNodeEntries) {
            state.addChildNodeEntry(e.getName(), e.getId());
        }
        state.setPropertyNames(properties.keySet());

        // add fake property entries
        state.addPropertyName(NameConstants.JCR_PRIMARYTYPE);
        if (mixinTypeNames.size() > 0) {
            state.addPropertyName(NameConstants.JCR_MIXINTYPES);
        }
        // uuid is special...only if 'referenceable'
        if (isReferenceable) {
            state.addPropertyName(NameConstants.JCR_UUID);
        }
        for (NodeId nodeId : sharedSet) {
            state.addShare(nodeId);
        }
        return state;
    }

    /**
     * Checks if this bundle is new.
     * @return <code>true</code> if this bundle is new;
     *         <code>false</code> otherwise.
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * Marks this bundle as 'not new'.
     */
    public void markOld() {
        isNew = false;
    }

    /**
     * Returns the node id of this bundle
     * @return the node id of this bundle
     */
    public NodeId getId() {
        return id;
    }

    /**
     * Returns the parent id of this bundle
     * @return the parent id of this bundle
     */
    public NodeId getParentId() {
        return parentId;
    }

    /**
     * Sets the parent id
     * @param parentId the parent id
     */
    public void setParentId(NodeId parentId) {
        this.parentId = parentId;
    }

    /**
     * Returns the nodetype name of this bundle
     * @return the nodetype name of this bundle
     */
    public Name getNodeTypeName() {
        return nodeTypeName;
    }

    /**
     * Sets the node type name
     * @param nodeTypeName the nodetype name
     */
    public void setNodeTypeName(Name nodeTypeName) {
        this.nodeTypeName = nodeTypeName;
    }

    /**
     * Returns the mixin type names of this bundle.
     * @return the mixin type names of this bundle.
     */
    public Set<Name> getMixinTypeNames() {
        return mixinTypeNames;
    }

    /**
     * Sets the mixin type names
     * @param mixinTypeNames the mixin type names
     */
    public void setMixinTypeNames(Set<Name> mixinTypeNames) {
        this.mixinTypeNames = mixinTypeNames;
    }

    /**
     * Checks if this bundle is referenceable.
     * @return <code>true</code> if this bundle is referenceable;
     *         <code>false</code> otherwise.
     */
    public boolean isReferenceable() {
        return isReferenceable;
    }

    /**
     * Sets the is referenceable flag on this bundle
     * @param referenceable the ref. flag
     */
    public void setReferenceable(boolean referenceable) {
        isReferenceable = referenceable;
    }

    /**
     * Returns the mod count.
     *
     * @return the mod count.
     */
    public short getModCount() {
        return modCount;
    }

    /**
     * Sets the mod count
     * 
     * @param modCount the mod count
     */
    public void setModCount(short modCount) {
        this.modCount = modCount;
    }

    /**
     * Returns the list of the child node entries.
     * @return the list of the child node entries.
     */
    public List<NodePropBundle.ChildNodeEntry> getChildNodeEntries() {
        return childNodeEntries;
    }

    /**
     * Adds a child node entry.
     * @param name the name of the entry.
     * @param id the id of the entry
     */
    public void addChildNodeEntry(Name name, NodeId id) {
        childNodeEntries.add(new ChildNodeEntry(name, id));
    }

    /**
     * Adds a new property entry
     * @param entry the enrty to add
     */
    public void addProperty(PropertyEntry entry) {
        assert !NameConstants.JCR_PRIMARYTYPE.equals(entry.getName());
        assert !NameConstants.JCR_UUID.equals(entry.getName());
        properties.put(entry.getName(), entry);
    }

    /**
     * Creates a property entry from the given state and adds it.
     *
     * @param state the property state
     * @param blobStore BLOB store from where to delete previous property value
     */
    public void addProperty(PropertyState state, BLOBStore blobStore) {
        PropertyEntry old =
            properties.put(state.getName(), new PropertyEntry(state));
        if (old != null) {
            old.destroy(blobStore);
        }
    }

    /**
     * Checks if this bundle has a property
     * @param name the name of the property
     * @return <code>true</code> if the property exists;
     *         <code>false</code> otherwise.
     */
    public boolean hasProperty(Name name) {
        return properties.containsKey(name);
    }

    /**
     * Returns a set of the property names.
     * @return a set of the property names.
     */
    public Set<Name> getPropertyNames() {
        return properties.keySet();
    }

    /**
     * Returns a collection of property entries.
     * @return a collection of property entries.
     */
    public Collection<PropertyEntry> getPropertyEntries() {
        return properties.values();
    }

    /**
     * Returns the property entry with the given name.
     * @param name the name of the property entry
     * @return the desired property entry or <code>null</code>
     */
    public PropertyEntry getPropertyEntry(Name name) {
        return properties.get(name);
    }

    /**
     * Removes all property entries
     *
     * @param blobStore BLOB store from where to delete property values
     */
    public void removeAllProperties(BLOBStore blobStore) {
        for (Name name : new HashSet<Name>(properties.keySet())) {
            removeProperty(name, blobStore);
        }
    }

    /**
     * Removes the proprty with the given name from this bundle.
     *
     * @param name the name of the property
     * @param blobStore BLOB store from where to delete the property value
     */
    public void removeProperty(Name name, BLOBStore blobStore) {
        PropertyEntry pe = properties.remove(name);
        if (pe != null) {
            pe.destroy(blobStore);
        }
    }
    
    /**
     * Sets the shared set of this bundle.
     * @return the shared set of this bundle.
     */
    public Set<NodeId> getSharedSet() {
        return sharedSet;
    }

    /**
     * Sets the shared set.
     * @param sharedSet shared set
     */
    public void setSharedSet(Set<NodeId> sharedSet) {
        this.sharedSet = sharedSet;
    }

    /**
     * Returns the approx. size of this bundle.
     * @return the approx. size of this bundle.
     */
    public long getSize() {
        // add some internal memory
        //  + shallow size: 64
        //  + properties
        //    + shallow size: 40
        //    + N * property entry: 218 + values + blobids
        //  + childnodes
        //    + shallow size: 24
        //    + N * 24 + 160 + 44 + name.length
        //  + mixintypes names
        //    + shallow size: 16
        //    + N * QNames
        //  + nodetype name:
        //    + shallow size: 24
        //      + string: 20 + length
        //  + parentId: 160
        //  + id: 160
        return 500 + size + 300 * (childNodeEntries.size() + properties.size() + 3);
    }

    /**
     * Sets the data size of this bundle
     * @param size the data size
     */
    public void setSize(long size) {
        this.size = size;
    }

    //--------------------------------------------------------------< Object >

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(id);
        builder.append("(");
        builder.append(parentId);
        builder.append(",");
        builder.append(nodeTypeName);
        for (Name mixin : mixinTypeNames) {
            builder.append(",");
            builder.append(mixin);
        }
        if (isReferenceable) {
            builder.append(",referenceable");
        }
        builder.append(") = ");
        if (!sharedSet.isEmpty()) {
            builder.append(sharedSet);
            builder.append(" ");
        }
        builder.append(properties.values());
        builder.append(" ");
        builder.append(childNodeEntries);
        return builder.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof NodePropBundle) {
            NodePropBundle that = (NodePropBundle) object;
            return equalNullSafe(id, that.id)
                && equalNullSafe(parentId, that.parentId)
                && equalNullSafe(nodeTypeName, that.nodeTypeName)
                && equalNullSafe(mixinTypeNames, that.mixinTypeNames)
                && isReferenceable == that.isReferenceable
                && equalNullSafe(sharedSet, that.sharedSet)
                && equalNullSafe(properties, that.properties)
                && equalNullSafe(childNodeEntries, that.childNodeEntries);
        }
        return false;
    }
    
    private static boolean equalNullSafe(Object a, Object b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.equals(b);
    }

    //-----------------------------------------------------< ChildNodeEntry >---

    /**
     * Helper class for a child node entry
     */
    public static class ChildNodeEntry {

        /**
         * the name of the entry
         */
        private final Name name;

        /**
         * the id of the entry
         */
        private final NodeId id;

        /**
         * Creates a new entry with the given name and id
         * @param name the name
         * @param id the id
         */
        public ChildNodeEntry(Name name, NodeId id) {
            this.name = name;
            this.id = id;
        }

        /**
         * Returns the name.
         * @return the name.
         */
        public Name getName() {
            return name;
        }

        /**
         * Returns the id.
         * @return the id.
         */
        public NodeId getId() {
            return id;
        }

        //----------------------------------------------------------< Object >

        public String toString() {
            return name + " => " + id;
        }

        public boolean equals(Object object) {
            if (object instanceof ChildNodeEntry) {
                ChildNodeEntry that = (ChildNodeEntry) object;
                return name.equals(that.name) && id.equals(that.id);
            } else {
                return false;
            }
        }

    }

    //------------------------------------------------------< PropertyEntry >---

    /**
     * Helper class for a property enrty
     */
    public static class PropertyEntry {

        /**
         * The property id
         */
        private final PropertyId id;

        /**
         * the internal value
         */
        private InternalValue[] values;

        /**
         * the property type
         */
        private int type;

        /**
         * the multivalued flag
         */
        private boolean multiValued;

        /**
         * the blob ids
         */
        private String[] blobIds;

        /**
         * the mod count
         */
        private short modCount;

        /**
         * Creates a new property entry with the given id.
         * @param id the id
         */
        public PropertyEntry(PropertyId id) {
            this.id = id;
        }

        /**
         * Creates a new property entry and initialized it with values from
         * the given property state.
         * @param state the source property state.
         */
        public PropertyEntry(PropertyState state) {
            this((PropertyId) state.getId());
            values = state.getValues();
            type = state.getType();
            multiValued = state.isMultiValued();
            if (!multiValued && values.length != 1) {
                throw new IllegalArgumentException("Non-multi-valued property with values.length " + values.length);
            }
            modCount = state.getModCount();
            if (type == PropertyType.BINARY) {
                blobIds = new String[values.length];
            }
        }

        /**
         * Returns the property id.
         * @return the property id.
         */
        public PropertyId getId() {
            return id;
        }

        /**
         * Returns the property name
         * @return the property name
         */
        public Name getName() {
            return id.getName();
        }

        /**
         * Retruns the internal values
         * @return the internal values
         */
        public InternalValue[] getValues() {
            return values;
        }

        /**
         * Sets the internal values.
         * @param values the internal values.
         */
        public void setValues(InternalValue[] values) {
            this.values = values;
        }

        /**
         * Returns the type.
         * @return the type.
         */
        public int getType() {
            return type;
        }

        /**
         * Sets the type
         * @param type the type
         */
        public void setType(int type) {
            this.type = type;
        }

        /**
         * Returns the multivalued flag.
         * @return the multivalued flag.
         */
        public boolean isMultiValued() {
            return multiValued;
        }

        /**
         * Sets the multivalued flag.
         * @param multiValued the multivalued flag
         */
        public void setMultiValued(boolean multiValued) {
            this.multiValued = multiValued;
        }

        /**
         * Returns the n<sup>th</sup> blob id.
         * @param n the index of the blob id
         * @return the blob id
         */
        public String getBlobId(int n) {
            return blobIds[n];
        }

        /**
         * Sets the blob ids
         * @param blobIds the blobids
         */
        public void setBlobIds(String[] blobIds) {
            this.blobIds = blobIds;
        }

        /**
         * Sets the n<sup>th</sup> blobid
         * @param blobId the blob id
         * @param n the index of the blob id
         */
        public void setBlobId(String blobId, int n) {
            blobIds[n] = blobId;
        }

        /**
         * Returns the mod count.
         * @return the mod count.
         */
        public short getModCount() {
            return modCount;
        }

        /**
         * Sets the mod count
         * @param modCount the mod count
         */
        public void setModCount(short modCount) {
            this.modCount = modCount;
        }

        /**
         * Destroys this property state and deletes temporary blob file values.
         * @param blobStore the blobstore that will destroy the blobs
         */
        private void destroy(BLOBStore blobStore) {
            // delete blobs if needed
            for (int i = 0; blobIds != null && i < blobIds.length; i++) {
                if (blobIds[i] != null) {
                    try {
                        blobStore.remove(blobIds[i]);
                        log.debug("removed blob {}", blobIds[i]);
                    } catch (Exception e) {
                        log.error("Ignoring error while removing blob " + blobIds[i], e);
                    }
                }
            }
        }

        //----------------------------------------------------------< Object >

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(id);
            builder.append("(");
            builder.append(PropertyType.nameFromValue(type));
            if (multiValued) {
                builder.append(",multiple");
            }
            builder.append(") = ");
            builder.append(Arrays.toString(values));
            return builder.toString();
        }

        public boolean equals(Object object) {
            if (object instanceof PropertyEntry) {
                PropertyEntry that = (PropertyEntry) object;
                return id.equals(that.id)
                    && type == that.type
                    && multiValued == that.multiValued
                    && Arrays.equals(values, that.values);
            } else {
                return false;
            }
        }

    }

}
