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

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.name.QName;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * <code>NodeState</code> represents the state of a <code>Node</code>.
 */
public class NodeState extends ItemState {

    /** Serialization UID of this class. */
    static final long serialVersionUID = -764076390011517389L;

    /** the uuid of this node */
    protected String uuid;

    /** the name of this node's primary type */
    protected QName nodeTypeName;

    /** the names of this node's mixin types */
    protected Set mixinTypeNames = new HashSet();

    /** id of this node's definition */
    protected NodeDefId defId;

    /** insertion-ordered collection of ChildNodeEntry objects */
    protected ChildNodeEntries childNodeEntries = new ChildNodeEntries();

    /** set of property names (QName objects) */
    protected Set propertyNames = new HashSet();

    /**
     * Listeners (weak references)
     */
    private final transient ReferenceMap listeners =
            new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK);

    /**
     * Constructor
     *
     * @param overlayedState the backing node state being overlayed
     * @param initialStatus  the initial status of the node state object
     * @param isTransient    flag indicating whether this state is transient or not
     */
    public NodeState(NodeState overlayedState, int initialStatus,
                     boolean isTransient) {
        super(initialStatus, isTransient);

        connect(overlayedState);
        pull();
    }

    /**
     * Constructor
     *
     * @param uuid          the UUID of the this node
     * @param nodeTypeName  node type of this node
     * @param parentUUID    the UUID of the parent node
     * @param initialStatus the initial status of the node state object
     * @param isTransient   flag indicating whether this state is transient or not
     */
    public NodeState(String uuid, QName nodeTypeName, String parentUUID,
                     int initialStatus, boolean isTransient) {
        super(parentUUID, new NodeId(uuid), initialStatus, isTransient);

        this.nodeTypeName = nodeTypeName;
        this.uuid = uuid;
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized void copy(ItemState state) {
        synchronized (state) {
            super.copy(state);

            NodeState nodeState = (NodeState) state;
            nodeTypeName = nodeState.getNodeTypeName();
            mixinTypeNames = new HashSet(nodeState.getMixinTypeNames());
            defId = nodeState.getDefinitionId();
            uuid = nodeState.getUUID();
            propertyNames = new HashSet(nodeState.getPropertyNames());
            childNodeEntries = new ChildNodeEntries();
            childNodeEntries.addAll(nodeState.getChildNodeEntries());
        }
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
     * Determines if there are any child node entries.
     *
     * @return <code>true</code> if there are child node entries,
     *         <code>false</code> otherwise.
     */
    public boolean hasChildNodeEntries() {
        return !childNodeEntries.isEmpty();
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
     * specified <code>uuid</code>.
     *
     * @param uuid UUID of the child node
     * @return <code>true</code> if there is a <code>ChildNodeEntry</code> with
     *         the specified <code>name</code>.
     */
    public synchronized boolean hasChildNodeEntry(String uuid) {
        return childNodeEntries.get(uuid) != null;
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
     * Determines if there is a property entry with the specified
     * <code>QName</code>.
     *
     * @param propName <code>QName</code> object specifying a property name
     * @return <code>true</code> if there is a property entry with the specified
     *         <code>QName</code>.
     */
    public synchronized boolean hasPropertyName(QName propName) {
        return propertyNames.contains(propName);
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
     * Returns the <code>ChildNodeEntry</code> with the specified uuid or
     * <code>null</code> if there's no such entry.
     *
     * @param uuid UUID of the child node
     * @return the <code>ChildNodeEntry</code> with the specified uuid or
     *         <code>null</code> if there's no such entry.
     * @see #addChildNodeEntry
     * @see #removeChildNodeEntry
     */
    public synchronized ChildNodeEntry getChildNodeEntry(String uuid) {
        return childNodeEntries.get(uuid);
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
        return childNodeEntries;
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
        return childNodeEntries.get(nodeName);
    }

    /**
     * Adds a new <code>ChildNodeEntry</code>.
     *
     * @param nodeName <code>QName</code> object specifying the name of the new entry.
     * @param uuid     UUID the new entry is refering to.
     * @return the newly added <code>ChildNodeEntry</code>
     */
    public synchronized ChildNodeEntry addChildNodeEntry(QName nodeName,
                                                         String uuid) {
        ChildNodeEntry entry = childNodeEntries.add(nodeName, uuid);
        notifyNodeAdded(entry);
        return entry;
    }

    /**
     * Renames a new <code>ChildNodeEntry</code>.
     *
     * @param oldName <code>QName</code> object specifying the entry's old name
     * @param index 1-based index if there are same-name child node entries
     * @param newName <code>QName</code> object specifying the entry's new name
     * @return <code>true</code> if the entry was sucessfully renamed;
     *         otherwise <code>false</code>
     */
    public synchronized boolean renameChildNodeEntry(QName oldName, int index,
                                                     QName newName) {
        ChildNodeEntry oldEntry = childNodeEntries.remove(oldName, index);
        if (oldEntry != null) {
            ChildNodeEntry newEntry =
                    childNodeEntries.add(newName, oldEntry.getUUID());
            notifyNodeAdded(newEntry);
            notifyNodeRemoved(oldEntry);
            return true;
        }
        return false;
    }

    /**
     * Removes a <code>ChildNodeEntry</code>.
     *
     * @param nodeName <code>ChildNodeEntry</code> object specifying a node name
     * @param index    1-based index if there are same-name child node entries
     * @return <code>true</code> if the specified child node entry was found
     *         in the list of child node entries and could be removed.
     */
    public synchronized boolean removeChildNodeEntry(QName nodeName, int index) {
        ChildNodeEntry entry = childNodeEntries.remove(nodeName, index);
        if (entry != null) {
            notifyNodeRemoved(entry);
        }
        return entry != null;
    }

    /**
     * Removes a <code>ChildNodeEntry</code>.
     *
     * @param uuid UUID of the entry to be removed
     * @return <code>true</code> if the specified child node entry was found
     *         in the list of child node entries and could be removed.
     */
    public synchronized boolean removeChildNodeEntry(String uuid) {
        ChildNodeEntry entry = childNodeEntries.remove(uuid);
        if (entry != null) {
            notifyNodeRemoved(entry);
        }
        return entry != null;
    }

    /**
     * Removes all <code>ChildNodeEntry</code>s.
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
        notifyNodesReplaced();
    }

    /**
     * Returns the names of this node's properties as a set of
     * <code>QNames</code> objects.
     *
     * @return set of <code>QNames</code> objects
     * @see #addPropertyName
     * @see #removePropertyName
     */
    public synchronized Set getPropertyNames() {
        return Collections.unmodifiableSet(propertyNames);
    }

    /**
     * Adds a property name entry.
     *
     * @param propName <code>QName</code> object specifying the property name
     */
    public synchronized void addPropertyName(QName propName) {
        propertyNames.add(propName);
    }

    /**
     * Removes a property name entry.
     *
     * @param propName <code>QName</code> object specifying the property name
     * @return <code>true</code> if the specified property name was found
     *         in the list of property name entries and could be removed.
     */
    public synchronized boolean removePropertyName(QName propName) {
        return propertyNames.remove(propName);
    }

    /**
     * Removes all property name entries.
     */
    public synchronized void removeAllPropertyNames() {
        propertyNames.clear();
    }

    /**
     * Sets the set of <code>QName</code> objects denoting the
     * properties of this node.
     */
    public synchronized void setPropertyNames(Set propNames) {
        propertyNames.clear();
        propertyNames.addAll(propNames);
    }

    /**
     * Set the node type name. Needed for deserialization and should therefore
     * not change the internal status.
     *
     * @param nodeTypeName node type name
     */
    public synchronized void setNodeTypeName(QName nodeTypeName) {
        this.nodeTypeName = nodeTypeName;
    }

    //---------------------------------------------------------< diff methods >
    /**
     * Returns a set of <code>QName</code>s denoting those properties that
     * do not exist in the overlayed node state but have been added to
     * <i>this</i> node state.
     *
     * @return set of <code>QName</code>s denoting the properties that have
     *         been added.
     */
    public synchronized Set getAddedPropertyNames() {
        if (!hasOverlayedState()) {
            return Collections.unmodifiableSet(propertyNames);
        }

        NodeState other = (NodeState) getOverlayedState();
        HashSet set = new HashSet(propertyNames);
        set.removeAll(other.propertyNames);
        return set;
    }

    /**
     * Returns a list of child node entries that do not exist in the overlayed
     * node state but have been added to <i>this</i> node state.
     *
     * @return list of added child node entries
     */
    public synchronized List getAddedChildNodeEntries() {
        if (!hasOverlayedState()) {
            return childNodeEntries;
        }

        NodeState other = (NodeState) getOverlayedState();
        return childNodeEntries.removeAll(other.childNodeEntries);
    }

    /**
     * Returns a set of <code>QName</code>s denoting those properties that
     * exist in the overlayed node state but have been removed from
     * <i>this</i> node state.
     *
     * @return set of <code>QName</code>s denoting the properties that have
     *         been removed.
     */
    public synchronized Set getRemovedPropertyNames() {
        if (!hasOverlayedState()) {
            return Collections.EMPTY_SET;
        }

        NodeState other = (NodeState) getOverlayedState();
        HashSet set = new HashSet(other.propertyNames);
        set.removeAll(propertyNames);
        return set;
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
        return other.childNodeEntries.removeAll(childNodeEntries);
    }

    /**
     * Returns a list of child node entries that exist both in <i>this</i> node
     * state and in the overlayed node state but have been reordered.
     * <p/>
     * The list may include only the minimal set of nodes that have been
     * reordered. That is, even though a certain number of nodes have changed
     * their absolute position the list may include less that this number of
     * nodes.
     * <p/>
     * Example:<br/>
     * Initial state:
     * <pre>
     *  + node1
     *  + node2
     *  + node3
     * </pre>
     * After reorder:
     * <pre>
     *  + node2
     *  + node3
     *  + node1
     * </pre>
     * All nodes have changed their absolute position. The returned list however
     * may only return that <code>node1</code> has been reordered (from the
     * first position to the end).
     *
     * @return list of reordered child node enties.
     */
    public synchronized List getReorderedChildNodeEntries() {
        if (!hasOverlayedState()) {
            return Collections.EMPTY_LIST;
        }

        List others = new ArrayList();
        others.addAll(((NodeState) getOverlayedState()).getChildNodeEntries());

        List ours = new ArrayList();
        ours.addAll(childNodeEntries);

        // do a lazy init
        List reordered = null;
        // remove added nodes from 'our' entries
        ours.removeAll(getAddedChildNodeEntries());
        // remove all removed nodes from 'other' entries
        others.removeAll(getRemovedChildNodeEntries());
        // both entry lists now contain the set of nodes that have not
        // been removed or added, but they may have changed their position.
        for (int i = 0; i < ours.size();) {
            ChildNodeEntry entry = (ChildNodeEntry) ours.get(i);
            ChildNodeEntry other = (ChildNodeEntry) others.get(i);
            if (!entry.getUUID().equals(other.getUUID())) {
                if (reordered == null) {
                    reordered = new ArrayList();
                }
                // Note that this check will not necessarily find the
                // minimal reorder operations required to convert the overlayed
                // child node entries into the current.

                // is there a next entry?
                if (i + 1 < ours.size()) {
                    // if entry is the next in the other list then probably
                    // the other entry at position <code>i</code> was reordered
                    if (entry.getUUID().equals(((ChildNodeEntry) others.get(i + 1)).getUUID())) {
                        // scan for the uuid of the other entry in our list
                        for (int j = i; j < ours.size(); j++) {
                            if (((ChildNodeEntry) ours.get(j)).getUUID().equals(other.uuid)) {
                                // found it
                                entry = (ChildNodeEntry) ours.get(j);
                                break;
                            }
                        }
                    }
                }

                reordered.add(entry);
                // remove the entry from both lists
                // entries > i are already cleaned
                for (int j = i; j < ours.size(); j++) {
                    if (((ChildNodeEntry) ours.get(j)).getUUID().equals(entry.getUUID())) {
                        ours.remove(j);
                    }
                }
                for (int j = i; j < ours.size(); j++) {
                    if (((ChildNodeEntry) others.get(j)).getUUID().equals(entry.getUUID())) {
                        others.remove(j);
                    }
                }
                // if a reorder has been detected index <code>i</code> is not
                // incremented because entries will be shifted when the
                // reordered entry is removed.
            } else {
                // no reorder, move to next child entry
                i++;
            }
        }
        if (reordered == null) {
            return Collections.EMPTY_LIST;
        } else {
            return reordered;
        }
    }

    //--------------------------------------------------< ItemState overrides >
    /**
     * {@inheritDoc}
     *
     * If the listener passed is at the same time a <code>NodeStateListener</code>
     * we add it to our list of specialized listeners.
     */
    public void addListener(ItemStateListener listener) {
        if (listener instanceof NodeStateListener) {
            synchronized (listeners) {
                if (!listeners.containsKey(listener)) {
                    listeners.put(listener, listener);
                }
            }
        }
        super.addListener(listener);
    }

    /**
     * {@inheritDoc}
     *
     * If the listener passed is at the same time a <code>NodeStateListener</code>
     * we remove it from our list of specialized listeners.
     */
    public void removeListener(ItemStateListener listener) {
        if (listener instanceof NodeStateListener) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }
        super.removeListener(listener);
    }

    //-------------------------------------------------< misc. helper methods >
    /**
     * Notify the listeners that a child node entry has been added
     */
    protected void notifyNodeAdded(ChildNodeEntry added) {
        synchronized (listeners) {
            MapIterator iter = listeners.mapIterator();
            while (iter.hasNext()) {
                NodeStateListener l = (NodeStateListener) iter.next();
                if (l != null) {
                    l.nodeAdded(this, added.getName(),
                            added.getIndex(), added.getUUID());
                }
            }
        }
    }

    /**
     * Notify the listeners that the child node entries have been replaced
     */
    protected void notifyNodesReplaced() {
        synchronized (listeners) {
            MapIterator iter = listeners.mapIterator();
            while (iter.hasNext()) {
                NodeStateListener l = (NodeStateListener) iter.next();
                if (l != null) {
                    l.nodesReplaced(this);
                }
            }
        }
    }

    /**
     * Notify the listeners that a child node entry has been removed
     */
    protected void notifyNodeRemoved(ChildNodeEntry removed) {
        synchronized (listeners) {
            MapIterator iter = listeners.mapIterator();
            while (iter.hasNext()) {
                NodeStateListener l = (NodeStateListener) iter.next();
                if (l != null) {
                    l.nodeRemoved(this, removed.getName(),
                            removed.getIndex(), removed.getUUID());
                }
            }
        }
    }

    //-------------------------------------------------< Serializable support >
    private void writeObject(ObjectOutputStream out) throws IOException {
        // delegate to default implementation
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        // delegate to default implementation
        in.defaultReadObject();
    }

    //--------------------------------------------------------< inner classes >
    /**
     * <code>ChildNodeEntries</code> represents an insertion-ordered
     * collection of <code>ChildNodeEntry</code>s that also maintains
     * the index values of same-name siblings on insertion and removal.
     * <p/>
     * <code>ChildNodeEntries</code> also provides an unmodifiable
     * <code>List</code> view.
     */
    private static class ChildNodeEntries implements List, Serializable {

        // insertion-ordered map of entries (key=uuid, value=entry)
        LinkedMap entries;
        // map used for lookup by name (key=name, value=1st same-name sibling entry)
        Map nameMap;

        ChildNodeEntries() {
            entries = new LinkedMap();
            nameMap = new HashMap();
        }

        ChildNodeEntry add(QName nodeName, String uuid) {
            ChildNodeEntry sibling = (ChildNodeEntry) nameMap.get(nodeName);
            while (sibling != null && sibling.getNextSibling() != null) {
                sibling = sibling.getNextSibling();
            }

            int index = (sibling == null) ? 1 : sibling.getIndex() + 1;

            ChildNodeEntry entry = new ChildNodeEntry(nodeName, uuid, index);
            if (sibling == null) {
                nameMap.put(nodeName, entry);
            } else {
                sibling.setNextSibling(entry);
            }
            entries.put(uuid, entry);

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
            entries.clear();
            nameMap.clear();
        }

        ChildNodeEntry remove(String uuid) {
            ChildNodeEntry entry = (ChildNodeEntry) entries.get(uuid);
            if (entry != null) {
                return remove(entry.getName(), entry.getIndex());
           }
            return entry;
        }

        public ChildNodeEntry remove(ChildNodeEntry entry) {
            return remove(entry.getName(), entry.getIndex());
        }

        public ChildNodeEntry remove(QName nodeName, int index) {
            if (index < 1) {
                throw new IllegalArgumentException("index is 1-based");
            }

            ChildNodeEntry sibling = (ChildNodeEntry) nameMap.get(nodeName);
            ChildNodeEntry prevSibling = null;
            while (sibling != null) {
                if (sibling.getIndex() == index) {
                    break;
                }
                prevSibling = sibling;
                sibling = sibling.getNextSibling();
            }
            if (sibling == null) {
                return null;
            }

            // remove from entries list
            entries.remove(sibling.getUUID());

            // update linked list of siblings & name map entry
            if (prevSibling != null) {
                prevSibling.setNextSibling(sibling.getNextSibling());
            } else {
                // the head is removed from the linked siblings list,
                // update name map
                if (sibling.getNextSibling() == null) {
                    nameMap.remove(nodeName);
                } else {
                    nameMap.put(nodeName, sibling.getNextSibling());
                }
            }
            // update indices of subsequent same-name siblings
            ChildNodeEntry nextSibling = sibling.getNextSibling();
            while (nextSibling != null) {
                nextSibling.decIndex();
                nextSibling = nextSibling.getNextSibling();
            }

            return sibling;
        }

        List get(QName nodeName) {
            ChildNodeEntry sibling = (ChildNodeEntry) nameMap.get(nodeName);
            if (sibling == null) {
                return Collections.EMPTY_LIST;
            }
            List siblings = new ArrayList();
            while (sibling != null) {
                siblings.add(sibling);
                sibling = sibling.getNextSibling();
            }
            return siblings;
        }

        ChildNodeEntry get(String uuid) {
            return (ChildNodeEntry) entries.get(uuid);
        }

        ChildNodeEntry get(QName nodeName, int index) {
            if (index < 1) {
                throw new IllegalArgumentException("index is 1-based");
            }

            ChildNodeEntry sibling = (ChildNodeEntry) nameMap.get(nodeName);
            while (sibling != null) {
                if (sibling.getIndex() == index) {
                    return sibling;
                }
                sibling = sibling.getNextSibling();
            }
            return null;
        }

        /**
         * Returns a list of <code>ChildNodeEntry</code>s who do only exist in
         * <code>this</code> but not in <code>other</code>
         * <p/>
         * Note that two entries are considered identical in this context if
         * they have the same name and uuid, i.e. the index is disregarded
         * whereas <code>ChildNodeEntry.equals(Object)</code> also compares
         * the index.
         *
         * @param other entries to be removed
         * @return a new list of those entries that do only exist in
         *         <code>this</code> but not in <code>other</code>
         */
        List removeAll(ChildNodeEntries other) {
            if (entries.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            if (other.isEmpty()) {
                return this;
            }

            List result = new ArrayList();
            Iterator iter = iterator();
            while (iter.hasNext()) {
                ChildNodeEntry entry = (ChildNodeEntry) iter.next();
                ChildNodeEntry otherEntry = (ChildNodeEntry) other.get(entry.uuid);
                if (otherEntry == null
                        || !entry.getName().equals(otherEntry.getName())) {
                    result.add(entry);
                }
            }

            return result;
        }

        //-------------------------------------------< unmodifiable List view >
        public boolean contains(Object o) {
            if (o instanceof ChildNodeEntry) {
                return entries.containsKey(((ChildNodeEntry) o).uuid);
            } else {
                return false;
            }
        }

        public boolean containsAll(Collection c) {
            Iterator iter = c.iterator();
            while (iter.hasNext()) {
                if (!contains(iter.next())) {
                    return false;
                }
            }
            return true;
        }

        public Object get(int index) {
            return entries.getValue(index);
        }

        public int indexOf(Object o) {
            if (o instanceof ChildNodeEntry) {
                return entries.indexOf(((ChildNodeEntry) o).uuid);
            } else {
                return -1;
            }
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }

        public int lastIndexOf(Object o) {
            // entries are unique
            return indexOf(o);
        }

        public Iterator iterator() {
            return new OrderedMapIterator(entries.asList().listIterator(), entries);
        }

        public ListIterator listIterator() {
            return new OrderedMapIterator(entries.asList().listIterator(), entries);
        }

        public ListIterator listIterator(int index) {
            return new OrderedMapIterator(entries.asList().listIterator(index), entries);
        }

        public int size() {
            return entries.size();
        }

        public List subList(int fromIndex, int toIndex) {
            // @todo FIXME does not fulfil the contract of List.subList(int,int)
            return Collections.unmodifiableList(new ArrayList(this).subList(fromIndex, toIndex));
        }

        public Object[] toArray() {
            ChildNodeEntry[] array = new ChildNodeEntry[size()];
            return toArray(array);
        }

        public Object[] toArray(Object a[]) {
            if (!a.getClass().getComponentType().isAssignableFrom(ChildNodeEntry.class)) {
                throw new ArrayStoreException();
            }
            if (a.length < size()) {
                a = new ChildNodeEntry[size()];
            }
            MapIterator iter = entries.mapIterator();
            int i = 0;
            while (iter.hasNext()) {
                iter.next();
                a[i] = entries.getValue(i);
                i++;
            }
            while (i < a.length) {
                a[i++] = null;
            }
            return a;
        }

        public void add(int index, Object element) {
            throw new UnsupportedOperationException();
        }

        public boolean add(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(int index, Collection c) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public Object remove(int index) {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public Object set(int index, Object element) {
            throw new UnsupportedOperationException();
        }

        //---------------------------------------------< Serializable support >
        private void writeObject(ObjectOutputStream out) throws IOException {
            // important: fields must be written in same order as they are
            // read in readObject(ObjectInputStream)
            out.writeInt(size()); // count
            for (Iterator iter = iterator(); iter.hasNext();) {
                NodeState.ChildNodeEntry entry =
                        (NodeState.ChildNodeEntry) iter.next();
                //out.writeObject(entry.getName());   // name
                out.writeUTF(entry.getName().toString());   // name
                out.writeUTF(entry.getUUID());  // uuid
            }
        }

        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            entries = new LinkedMap();
            nameMap = new HashMap();
            // important: fields must be read in same order as they are
            // written in writeObject(ObjectOutputStream)
            int count = in.readInt();   // count
            for (int i = 0; i < count; i++) {
                //QName name = (QName) in.readObject();    // name
                QName name = QName.valueOf(in.readUTF());    // name
                String s = in.readUTF();   // uuid
                add(name, s);
            }
        }

        //----------------------------------------------------< inner classes >
        class OrderedMapIterator implements ListIterator {

            final ListIterator keyIter;
                final Map entries;

            OrderedMapIterator(ListIterator keyIter, Map entries) {
                this.keyIter = keyIter;
                this.entries = entries;
            }

            public boolean hasNext() {
                return keyIter.hasNext();
            }

            public Object next() {
                return entries.get(keyIter.next());
            }

            public boolean hasPrevious() {
                return keyIter.hasPrevious();
            }

            public int nextIndex() {
                return keyIter.nextIndex();
            }

            public Object previous() {
                return entries.get(keyIter.previous());
            }

            public int previousIndex() {
                return keyIter.previousIndex();
            }

            public void add(Object o) {
                throw new UnsupportedOperationException();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public void set(Object o) {
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * <code>ChildNodeEntry</code> specifies the name, index (in the case of
     * same-name siblings) and the UUID of a child node entry.
     */
    public static class ChildNodeEntry {

        private QName name;
        private int index; // 1-based index for same-name siblings
        private String uuid;
        private ChildNodeEntry nextSibling;

        private ChildNodeEntry(QName name, String uuid, int index) {
            if (name == null) {
                throw new IllegalArgumentException("name can not be null");
            }
            this.name = name;

            if (uuid == null) {
                throw new IllegalArgumentException("uuid can not be null");
            }
            this.uuid = uuid;

            if (index < 1) {
                throw new IllegalArgumentException("index is 1-based");
            }
            this.index = index;

            nextSibling = null;
        }

        public String getUUID() {
            return uuid;
        }

        public QName getName() {
            return name;
        }

        public int getIndex() {
            return index;
        }

        public ChildNodeEntry getNextSibling() {
            return nextSibling;
        }

        void setNextSibling(ChildNodeEntry nextSibling) {
            if (nextSibling != null && !nextSibling.getName().equals(name)) {
                throw new IllegalArgumentException("not a same-name sibling entry");
            }

            this.nextSibling = nextSibling;
        }

        int incIndex() {
            return ++index;
        }

        int decIndex() {
            if (index == 1) {
                throw new IndexOutOfBoundsException();
            }
            return --index;
        }

        //---------------------------------------< java.lang.Object overrides >
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

        /**
         * Returns zero to satisfy the Object equals/hashCode contract.
         * This class is mutable and not meant to be used as a hash key.
         *
         * @return always zero
         * @see Object#hashCode()
         */
        public int hashCode() {
            return 0;
        }
    }
}
