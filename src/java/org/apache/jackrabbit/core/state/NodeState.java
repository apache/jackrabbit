/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.nodetype.NodeDefId;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

/**
 * <code>NodeState</code> represents the state of a <code>Node</code>.
 */
public class NodeState extends ItemState {

    static final long serialVersionUID = -1785389681811057946L;

    /**
     * List of parent UUIDs: there's <i>one</i> entry for every parent although
     * a parent might have more than one child entries refering to <i>this</i>
     * node state.
     * <p/>
     * Furthermore:
     * <p/>
     * <code>parentUUIDs.contains(super.parentUUID) == true</code>
     */
    protected List parentUUIDs = new ArrayList();

    protected String uuid;
    protected QName nodeTypeName;
    protected Set mixinTypeNames = new HashSet();
    protected NodeDefId defId;

    // insertion-ordered collection of ChildNodeEntry objects
    protected ChildNodeEntries childNodeEntries = new ChildNodeEntries();
    // insertion-ordered collection of PropertyEntry objects
    protected List propertyEntries = new ArrayList();

    /**
     * Package private constructor
     *
     * @param overlayedState the backing node state being overlayed
     * @param initialStatus  the initial status of the node state object
     */
    protected NodeState(NodeState overlayedState, int initialStatus) {
        super(overlayedState, initialStatus);

        copy(overlayedState);
    }

    /**
     * Package private constructor
     *
     * @param uuid          the UUID of the this node
     * @param nodeTypeName  node type of this node
     * @param parentUUID    the UUID of the parent node
     * @param initialStatus the initial status of the node state object
     */
    protected NodeState(String uuid, QName nodeTypeName, String parentUUID, int initialStatus) {
        super(parentUUID, new NodeId(uuid), initialStatus);
        if (parentUUID != null) {
            parentUUIDs.add(parentUUID);
        }
        this.nodeTypeName = nodeTypeName;
        this.uuid = uuid;
    }

    /**
     * @see ItemState#copy
     */
    protected void copy(ItemState state) {
        super.copy(state);

        NodeState nodeState = (NodeState) state;
        nodeTypeName = nodeState.getNodeTypeName();
        mixinTypeNames.clear();
        mixinTypeNames.addAll(nodeState.getMixinTypeNames());
        defId = nodeState.getDefinitionId();
        uuid = nodeState.getUUID();
        parentUUIDs.clear();
        parentUUIDs.addAll(nodeState.getParentUUIDs());
        propertyEntries.clear();
        propertyEntries.addAll(nodeState.getPropertyEntries());
        childNodeEntries.removeAll();
        childNodeEntries.addAll(nodeState.getChildNodeEntries());
    }

    //-------------------------------------------------------< public methods >
    /**
     * Determines if this item state represents a node.
     *
     * @return always true
     * @see ItemState#isNode
     */
    public final boolean isNode() {
        return true;
    }

    /**
     * Returns the name of this node's node type.
     *
     * @return the name of this node's node type.
     */
    public QName getNodeTypeName() {
        return nodeTypeName;
    }

    /**
     * Returns the names of this node's mixin types.
     *
     * @return a set of the names of this node's mixin types.
     */
    public synchronized Set getMixinTypeNames() {
        return Collections.unmodifiableSet(mixinTypeNames);
    }

    /**
     * Sets the names of this node's mixin types.
     *
     * @param names set of names of mixin types
     */
    public synchronized void setMixinTypeNames(Set names) {
        mixinTypeNames.clear();
        mixinTypeNames.addAll(names);
    }

    /**
     * Returns the id of the definition applicable to this node state.
     *
     * @return the id of the definition
     */
    public NodeDefId getDefinitionId() {
        return defId;
    }

    /**
     * Sets the id of the definition applicable to this node state.
     *
     * @param defId the id of the definition
     */
    public void setDefinitionId(NodeDefId defId) {
        this.defId = defId;
    }

    /**
     * Returns the UUID of the repository node this node state is representing.
     *
     * @return the UUID
     */
    public String getUUID() {
        return uuid;
    }

    /**
     * Returns the UUIDs of the parent <code>NodeState</code>s or <code>null</code>
     * if either this item state represents the root node or this item state is
     * 'free floating', i.e. not attached to the repository's hierarchy.
     *
     * @return the UUIDs of the parent <code>NodeState</code>s
     * @see #addParentUUID
     * @see #removeParentUUID
     */
    public synchronized List getParentUUIDs() {
        return Collections.unmodifiableList(parentUUIDs);
    }

    /**
     * Adds the specified UUID to the list of parent UUIDs of this node state.
     *
     * @param uuid the UUID of the parent node
     * @see #getParentUUIDs
     * @see #removeParentUUID
     */
    public synchronized void addParentUUID(String uuid) {
        parentUUIDs.add(uuid);
    }

    /**
     * Removes the specified UUID from the list of parent UUIDs of this node state.
     *
     * @param uuid the UUID of the parent node
     * @return <code>true</code> if the specified UUID was contained in the set
     *         of parent UUIDs and could be removed.
     * @see #getParentUUIDs
     * @see #addParentUUID
     */
    public synchronized boolean removeParentUUID(String uuid) {
        if (parentUUID.equals(uuid)) {
            parentUUID = null;
        }
        boolean removed = parentUUIDs.remove(uuid);
        if (parentUUID == null) {
            // change primary parent
            if (!parentUUIDs.isEmpty()) {
                parentUUID = (String) parentUUIDs.iterator().next();
            }
        }
        return removed;
    }

    /**
     * Removes all parent UUIDs of this node state.
     */
    public synchronized void removeAllParentUUIDs() {
        parentUUIDs.clear();
        parentUUID = null;
    }

    /**
     * Sets the UUIDs of the parent <code>NodeState</code>s.
     */
    public synchronized void setParentUUIDs(List uuids) {
        parentUUIDs.clear();
        parentUUIDs.addAll(uuids);
    }

    /**
     * Determines if there is a <code>ChildNodeEntry</code> with the
     * specified <code>name</code>.
     *
     * @param name <code>QName</code> object specifying a node name
     * @return <code>true</code> if there is a <code>ChildNodeEntry</code> with
     *         the specified <code>name</code>.
     */
    public synchronized boolean hasChildNodeEntry(QName name) {
        Iterator iter = childNodeEntries.iterator();
        while (iter.hasNext()) {
            ChildNodeEntry entry = (ChildNodeEntry) iter.next();
            if (name.equals(entry.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if there is a <code>ChildNodeEntry</code> with the
     * specified <code>name</code> and <code>index</code>.
     *
     * @param name  <code>QName</code> object specifying a node name
     * @param index 1-based index if there are same-name child node entries
     * @return <code>true</code> if there is a <code>ChildNodeEntry</code> with
     *         the specified <code>name</code> and <code>index</code>.
     */
    public synchronized boolean hasChildNodeEntry(QName name, int index) {
        if (index < 1) {
            throw new IllegalArgumentException("index is 1-based");
        }
        Iterator iter = childNodeEntries.iterator();
        int count = 0;
        while (iter.hasNext()) {
            ChildNodeEntry entry = (ChildNodeEntry) iter.next();
            if (name.equals(entry.getName())) {
                if (++count == index) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if there is a <code>PropertyEntry</code> with the
     * specified <code>QName</code>.
     *
     * @param propName <code>QName</code> object specifying a property name
     * @return <code>true</code> if there is a <code>PropertyEntry</code> with
     *         the specified <code>QName</code>.
     */
    public synchronized boolean hasPropertyEntry(QName propName) {
        PropertyEntry entry = new PropertyEntry(propName);
        return propertyEntries.contains(entry);
    }

    /**
     * Returns the <code>PropertyEntry</code> with the specified name or
     * <code>null</code> if there's no such entry.
     *
     * @param propName <code>QName</code> object specifying a property name
     * @return the <code>PropertyEntry</code> with the specified name or
     *         <code>null</code> if there's no such entry.
     */
    public synchronized PropertyEntry getPropertyEntry(QName propName) {
        Iterator iter = propertyEntries.iterator();
        while (iter.hasNext()) {
            PropertyEntry entry = (PropertyEntry) iter.next();
            if (propName.equals(entry.getName())) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Returns the <code>ChildNodeEntry</code> with the specified name and index
     * or <code>null</code> if there's no such entry.
     *
     * @param nodeName <code>QName</code> object specifying a node name
     * @param index    1-based index if there are same-name child node entries
     * @return the <code>ChildNodeEntry</code> with the specified name and index
     *         or <code>null</code> if there's no such entry.
     */
    public synchronized ChildNodeEntry getChildNodeEntry(QName nodeName, int index) {
        if (index < 1) {
            throw new IllegalArgumentException("index is 1-based");
        }
        Iterator iter = childNodeEntries.iterator();
        int count = 0;
        while (iter.hasNext()) {
            ChildNodeEntry entry = (ChildNodeEntry) iter.next();
            if (nodeName.equals(entry.getName())) {
                if (++count == index) {
                    return entry;
                }
            }
        }
        return null;
    }

    /**
     * Returns a list of <code>ChildNodeEntry</code> objects denoting the
     * child nodes of this node.
     *
     * @return list of <code>ChildNodeEntry</code> objects
     * @see #addChildNodeEntry
     * @see #removeChildNodeEntry
     */
    public synchronized List getChildNodeEntries() {
        return childNodeEntries.entries();
    }

    /**
     * Returns a list of <code>ChildNodeEntry</code> objects denoting the
     * child nodes of this node that refer to the specified UUID.
     *
     * @param uuid UUID of a child node state.
     * @return list of <code>ChildNodeEntry</code> objects
     * @see #addChildNodeEntry
     * @see #removeChildNodeEntry
     */
    public synchronized List getChildNodeEntries(String uuid) {
        ArrayList list = new ArrayList();
        Iterator iter = childNodeEntries.iterator();
        while (iter.hasNext()) {
            ChildNodeEntry entry = (ChildNodeEntry) iter.next();
            if (entry.getUUID().equals(uuid)) {
                list.add(entry);
            }
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns a list of <code>ChildNodeEntry</code>s with the specified name.
     *
     * @param nodeName name of the child node entries that should be returned
     * @return list of <code>ChildNodeEntry</code> objects
     * @see #addChildNodeEntry
     * @see #removeChildNodeEntry
     */
    public synchronized List getChildNodeEntries(QName nodeName) {
        ArrayList list = new ArrayList();
        Iterator iter = childNodeEntries.iterator();
        while (iter.hasNext()) {
            ChildNodeEntry entry = (ChildNodeEntry) iter.next();
            if (entry.getName().equals(nodeName)) {
                list.add(entry);
            }
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Adds a new <code>ChildNodeEntry<code>.
     *
     * @param nodeName <code>QName<code> object specifying the name of the new entry.
     * @param uuid     UUID the new entry is refering to.
     * @return the newly added <code>ChildNodeEntry<code>
     */
    public synchronized ChildNodeEntry addChildNodeEntry(QName nodeName, String uuid) {
        return childNodeEntries.add(nodeName, uuid);
    }

    /**
     * Removes a <code>ChildNodeEntry<code>.
     *
     * @param nodeName <code>ChildNodeEntry<code> object specifying a node name
     * @param index    1-based index if there are same-name child node entries
     * @return <code>true</code> if the specified child node entry was found
     *         in the list of child node entries and could be removed.
     */
    public synchronized boolean removeChildNodeEntry(QName nodeName, int index) {
        return childNodeEntries.remove(nodeName, index);
    }

    /**
     * Removes all <code>ChildNodeEntry<code>s.
     */
    public synchronized void removeAllChildNodeEntries() {
        childNodeEntries.removeAll();
    }

    /**
     * Sets the list of <code>ChildNodeEntry</code> objects denoting the
     * child nodes of this node.
     */
    public synchronized void setChildNodeEntries(List nodeEntries) {
        childNodeEntries.removeAll();
        childNodeEntries.addAll(nodeEntries);
    }

    /**
     * Returns a list of <code>PropertyEntry</code> objects denoting the
     * properties of this node.
     *
     * @return list of <code>PropertyEntry</code> objects
     * @see #addPropertyEntry
     * @see #removePropertyEntry
     */
    public synchronized List getPropertyEntries() {
        return Collections.unmodifiableList(propertyEntries);
    }

    /**
     * Adds a <code>PropertyEntry<code>.
     *
     * @param propName <code>QName<code> object specifying the property name
     */
    public synchronized void addPropertyEntry(QName propName) {
        PropertyEntry entry = new PropertyEntry(propName);
        propertyEntries.add(entry);
    }

    /**
     * Removes a <code>PropertyEntry<code>.
     *
     * @param propName <code>QName<code> object specifying the property name
     * @return <code>true</code> if the specified property entry was found
     *         in the list of property entries and could be removed.
     */
    public synchronized boolean removePropertyEntry(QName propName) {
        PropertyEntry entry = new PropertyEntry(propName);
        int pos = propertyEntries.indexOf(entry);
        if (pos == -1) {
            return false;
        } else {
            propertyEntries.remove(pos);
            return true;
        }
    }

    /**
     * Removes all <code>PropertyEntry<code>s.
     */
    public synchronized void removeAllPropertyEntries() {
        propertyEntries.clear();
    }

    /**
     * Sets the list of <code>PropertyEntry</code> objects denoting the
     * properties of this node.
     */
    public synchronized void setPropertyEntries(List propEntries) {
        propertyEntries.clear();
        propertyEntries.addAll(propEntries);
    }

    //---------------------------------------------------------< diff methods >
    /**
     * Returns a list of parent UUID's, that do not exist in the overlayed node
     * state but have been added to <i>this</i> node state.
     *
     * @return list of added parent UUID's
     */
    public synchronized List getAddedParentUUIDs() {
        if (!hasOverlayedState()) {
            return Collections.EMPTY_LIST;
        }

        ArrayList list = new ArrayList(parentUUIDs);

        NodeState other = (NodeState) getOverlayedState();
        Iterator i = other.parentUUIDs.iterator();
        while (i.hasNext()) {
            list.remove(i.next());
        }

        return list;
    }

    /**
     * Returns a list of property entries, that do not exist in the overlayed
     * node state but have been added to <i>this</i> node state.
     *
     * @return list of added property entries
     */
    public synchronized List getAddedPropertyEntries() {
        if (!hasOverlayedState()) {
            return Collections.unmodifiableList(propertyEntries);
        }

        ArrayList list = new ArrayList(propertyEntries);

        NodeState other = (NodeState) getOverlayedState();
        Iterator i = other.propertyEntries.iterator();
        while (i.hasNext()) {
            list.remove(i.next());
        }

        return list;
    }

    /**
     * Returns a list of child node entries, that do not exist in the overlayed
     * node state but have been added to <i>this</i> node state.
     *
     * @return list of added child node entries
     */
    public synchronized List getAddedChildNodeEntries() {
        if (!hasOverlayedState()) {
            return Collections.unmodifiableList(childNodeEntries.entries());
        }

        ArrayList list = new ArrayList(childNodeEntries.entries());

        NodeState other = (NodeState) getOverlayedState();
        Iterator i = other.childNodeEntries.entries().iterator();
        while (i.hasNext()) {
            list.remove(i.next());
        }

        return list;
    }

    /**
     * Returns a list of parent UUID's, that exist in the overlayed node state
     * but have been removed from <i>this</i> node state.
     *
     * @return list of removed parent UUID's
     */
    public synchronized List getRemovedParentUUIDs() {
        if (!hasOverlayedState()) {
            return Collections.EMPTY_LIST;
        }

        NodeState other = (NodeState) getOverlayedState();
        ArrayList list = new ArrayList(other.parentUUIDs);

        Iterator i = parentUUIDs.iterator();
        while (i.hasNext()) {
            list.remove(i.next());
        }

        return list;
    }

    /**
     * Returns a list of property entries, that exist in the overlayed node state
     * but have been removed from <i>this</i> node state.
     *
     * @return list of removed property entries
     */
    public synchronized List getRemovedPropertyEntries() {
        if (!hasOverlayedState()) {
            return Collections.EMPTY_LIST;
        }

        NodeState other = (NodeState) getOverlayedState();
        ArrayList list = new ArrayList(other.propertyEntries);

        Iterator i = propertyEntries.iterator();
        while (i.hasNext()) {
            list.remove(i.next());
        }

        return list;
    }

    /**
     * Returns a list of child node entries, that exist in the overlayed node state
     * but have been removed from <i>this</i> node state.
     *
     * @return list of removed child node entries
     */
    public synchronized List getRemovedChildNodeEntries() {
        if (!hasOverlayedState()) {
            return Collections.EMPTY_LIST;
        }

        NodeState other = (NodeState) getOverlayedState();
        ArrayList list = new ArrayList(other.childNodeEntries.entries());

        Iterator i = childNodeEntries.entries().iterator();
        while (i.hasNext()) {
            list.remove(i.next());
        }

        return list;
    }

    //--------------------------------------------------< ItemState overrides >

    /**
     * Sets the UUID of the parent <code>NodeState</code>.
     *
     * @param parentUUID the parent <code>NodeState</code>'s UUID or <code>null</code>
     *                   if either this item state should represent the root node or this item state
     *                   should be 'free floating', i.e. detached from the repository's hierarchy.
     */
    public synchronized void setParentUUID(String parentUUID) {
        // @todo is this correct?
        if (parentUUID != null && !parentUUIDs.contains(parentUUID)) {
            parentUUIDs.add(parentUUID);
        }
        this.parentUUID = parentUUID;
    }

    //-------------------------------------------------< Serializable support >
    private void writeObject(ObjectOutputStream out) throws IOException {
        // delegate to default implementation
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        // delegate to default implementation
        in.defaultReadObject();
    }

    //--------------------------------------------------------< inner classes >
    /**
     * <code>ChildNodeEntries</code> represents an insertion-ordered
     * collection of <code>ChildNodeEntry</code>s that also maintains
     * the index values of same-name siblings on insertion and removal.
     */
    private static class ChildNodeEntries implements Serializable {

        // insertion-ordered collection of entries
        List entries;
        // mapping from names to list of same-name sibling entries
        Map names;

        ChildNodeEntries() {
            entries = new ArrayList();
            names = new HashMap();
        }

        ChildNodeEntry add(QName nodeName, String uuid) {
            List siblings = (List) names.get(nodeName);
            if (siblings == null) {
                siblings = new ArrayList();
                names.put(nodeName, siblings);
            }

            int index = siblings.size() + 1;

            ChildNodeEntry entry = new ChildNodeEntry(nodeName, uuid, index);
            siblings.add(entry);
            entries.add(entry);

            return entry;
        }

        void addAll(List entriesList) {
            Iterator iter = entriesList.iterator();
            while (iter.hasNext()) {
                ChildNodeEntry entry = (ChildNodeEntry) iter.next();
                // delegate to add(QName, String) to maintain consistency
                add(entry.getName(), entry.getUUID());
            }
        }

        public void removeAll() {
            names.clear();
            entries.clear();
        }

        public boolean remove(ChildNodeEntry entry) {
            return remove(entry.getName(), entry.getIndex());
        }

        public boolean remove(QName nodeName, int index) {
            if (index < 1) {
                throw new IllegalArgumentException("index is 1-based");
            }
            List siblings = (List) names.get(nodeName);
            if (siblings == null) {
                return false;
            }
            if (index > siblings.size()) {
                return false;
            }
            // remove from siblings list
            ChildNodeEntry removedEntry = (ChildNodeEntry) siblings.remove(index - 1);
            // remove from entries list
            entries.remove(removedEntry);

            if (siblings.size() == 0) {
                // short cut
                names.remove(nodeName);
                return true;
            }

            // update indices of subsequent same-name siblings
            for (int i = index - 1; i < siblings.size(); i++) {
                ChildNodeEntry oldEntry = (ChildNodeEntry) siblings.get(i);
                ChildNodeEntry newEntry = new ChildNodeEntry(nodeName, oldEntry.getUUID(), oldEntry.getIndex() - 1);
                // overwrite old entry with updated entry in siblings list
                siblings.set(i, newEntry);
                // overwrite old entry with updated entry in entries list
                entries.set(entries.indexOf(oldEntry), newEntry);
            }

            return true;
        }

        Iterator iterator() {
            return entries.iterator();
        }

        List entries() {
            return Collections.unmodifiableList(entries);
        }
    }

    /**
     * base class for <code>PropertyEntry</code> and <code>ChildNodeEntry</code>
     */
    private abstract static class ChildEntry implements Serializable {
        protected QName name;

        protected ChildEntry(QName name) {
            this.name = name;
        }

        public QName getName() {
            return name;
        }
    }

    /**
     * <code>PropertyEntry</code> specifies the name of a property entry.
     */
    public static class PropertyEntry extends ChildEntry {

        private int hash = 0;

        private PropertyEntry(QName propName) {
            super(propName);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof PropertyEntry) {
                PropertyEntry other = (PropertyEntry) obj;
                return name.equals(other.name);
            }
            return false;
        }

        public String toString() {
            return name.toString();
        }

        public int hashCode() {
            // PropertyEntry is immutable, we can store the computed hash code value
            if (hash == 0) {
                hash = name.hashCode();
            }
            return hash;
        }
    }

    /**
     * <code>ChildNodeEntry</code> specifies the name, index (in the case of
     * same-name siblings) and the UUID of a child node entry.
     */
    public static class ChildNodeEntry extends ChildEntry {

        private int hash = 0;

        private int index; // 1-based index for same-name siblings
        private String uuid;

        public ChildNodeEntry(QName nodeName, String uuid, int index) {
            super(nodeName);

            if (uuid == null) {
                throw new IllegalArgumentException("uuid can not be null");
            }
            this.uuid = uuid;

            if (index < 1) {
                throw new IllegalArgumentException("index is 1-based");
            }
            this.index = index;
        }

        public String getUUID() {
            return uuid;
        }

        public int getIndex() {
            return index;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ChildNodeEntry) {
                ChildNodeEntry other = (ChildNodeEntry) obj;
                return (name.equals(other.name) && uuid.equals(other.uuid)
                        && index == other.index);
            }
            return false;
        }

        public String toString() {
            return name.toString() + "[" + index + "] -> " + uuid;
        }

        public int hashCode() {
            // ChildNodeEntry is immutable, we can store the computed hash code value
            int h = hash;
            if (h == 0) {
                h = 17;
                h = 37 * h + name.hashCode();
                h = 37 * h + uuid.hashCode();
                h = 37 * h + index;
                hash = h;
            }
            return h;
        }
    }
}
