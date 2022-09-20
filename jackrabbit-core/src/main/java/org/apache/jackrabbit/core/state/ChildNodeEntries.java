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
package org.apache.jackrabbit.core.state;

import org.apache.commons.collections4.map.LinkedMap;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.util.EmptyLinkedMap;
import org.apache.jackrabbit.spi.Name;

import java.util.List;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Map;

/**
 * <code>ChildNodeEntries</code> represents an insertion-ordered
 * collection of <code>ChildNodeEntry</code>s that also maintains
 * the index values of same-name siblings on insertion and removal.
 */
class ChildNodeEntries implements Cloneable {

    /**
     * Insertion-ordered map of entries
     * (key=NodeId, value=entry)
     */
    private LinkedMap entries;

    /**
     * Map used for lookup by name
     * (key=name, value=either a single entry or a list of sns entries)
     */
    private Map<Name, Object> nameMap;

    /**
     * Indicates whether the entries and nameMap are shared with another
     * ChildNodeEntries instance.
     */
    private boolean shared;

    ChildNodeEntries() {
        init();
    }

    ChildNodeEntry get(NodeId id) {
        return (ChildNodeEntry) entries.get(id);
    }

    @SuppressWarnings("unchecked")
    List<ChildNodeEntry> get(Name nodeName) {
        Object obj = nameMap.get(nodeName);
        if (obj == null) {
            return Collections.emptyList();
        }
        if (obj instanceof List<?>) {
            // map entry is a list of siblings
            return Collections.unmodifiableList((List<ChildNodeEntry>) obj);
        } else {
            // map entry is a single child node entry
            return Collections.singletonList((ChildNodeEntry) obj);
        }
    }

    @SuppressWarnings("unchecked")
    ChildNodeEntry get(Name nodeName, int index) {
        if (index < 1) {
            throw new IllegalArgumentException("index is 1-based");
        }

        Object obj = nameMap.get(nodeName);
        if (obj == null) {
            return null;
        }
        if (obj instanceof List<?>) {
            // map entry is a list of siblings
            List<ChildNodeEntry> siblings = (List<ChildNodeEntry>) obj;
            if (index <= siblings.size()) {
                return siblings.get(index - 1);
            }
        } else {
            // map entry is a single child node entry
            if (index == 1) {
                return (ChildNodeEntry) obj;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    ChildNodeEntry add(Name nodeName, NodeId id) {
        ensureModifiable();
        List<ChildNodeEntry> siblings = null;
        int index = 0;
        Object obj = nameMap.get(nodeName);
        if (obj != null) {
            if (obj instanceof List<?>) {
                // map entry is a list of siblings
                siblings = (List<ChildNodeEntry>) obj;
                if (siblings.size() > 0) {
                    // reuse immutable Name instance from 1st same name sibling
                    // in order to help gc conserving memory
                    nodeName = siblings.get(0).getName();
                }
            } else {
                // map entry is a single child node entry,
                // convert to siblings list
                siblings = new ArrayList<ChildNodeEntry>();
                siblings.add((ChildNodeEntry) obj);
                nameMap.put(nodeName, siblings);
            }
            index = siblings.size();
        }

        index++;

        ChildNodeEntry entry = new ChildNodeEntry(nodeName, id, index);
        if (siblings != null) {
            siblings.add(entry);
        } else {
            nameMap.put(nodeName, entry);
        }
        entries.put(id, entry);

        return entry;
    }

    void addAll(List<ChildNodeEntry> entriesList) {
        for (ChildNodeEntry entry : entriesList) {
            // delegate to add(Name, String) to maintain consistency
            add(entry.getName(), entry.getId());
        }
    }

    // The index may have changed because of changes by another session. Use remove(NodeId id)
    // instead    
    @Deprecated
    @SuppressWarnings("unchecked")
    public ChildNodeEntry remove(Name nodeName, int index) {
        if (index < 1) {
            throw new IllegalArgumentException("index is 1-based");
        }

        ensureModifiable();
        Object obj = nameMap.get(nodeName);
        if (obj == null) {
            return null;
        }

        if (obj instanceof ChildNodeEntry) {
            // map entry is a single child node entry
            if (index != 1) {
                return null;
            }
            ChildNodeEntry removedEntry = (ChildNodeEntry) obj;
            nameMap.remove(nodeName);
            entries.remove(removedEntry.getId());
            return removedEntry;
        }

        // map entry is a list of siblings
        List<ChildNodeEntry> siblings = (List<ChildNodeEntry>) obj;
        if (index > siblings.size()) {
            return null;
        }

        // remove from siblings list
        ChildNodeEntry removedEntry = siblings.remove(index - 1);
        // remove from ordered entries map
        entries.remove(removedEntry.getId());

        // update indices of subsequent same-name siblings
        for (int i = index - 1; i < siblings.size(); i++) {
            ChildNodeEntry oldEntry = siblings.get(i);
            ChildNodeEntry newEntry = new ChildNodeEntry(nodeName, oldEntry.getId(), oldEntry.getIndex() - 1);
            // overwrite old entry with updated entry in siblings list
            siblings.set(i, newEntry);
            // overwrite old entry with updated entry in ordered entries map
            entries.put(newEntry.getId(), newEntry);
        }

        // clean up name lookup map if necessary
        if (siblings.size() == 0) {
            // no more entries with that name left:
            // remove from name lookup map as well
            nameMap.remove(nodeName);
        } else if (siblings.size() == 1) {
            // just one entry with that name left:
            // discard siblings list and update name lookup map accordingly
            nameMap.put(nodeName, siblings.get(0));
        }

        // we're done
        return removedEntry;
    }

    /**
     * Removes the child node entry refering to the node with the given id.
     *
     * @param id id of node whose entry is to be removed.
     * @return the removed entry or <code>null</code> if there is no such entry.
     */
    ChildNodeEntry remove(NodeId id) {
        ChildNodeEntry entry = (ChildNodeEntry) entries.get(id);
        if (entry != null) {
            return remove(entry.getName(), entry.getIndex());
        }
        return entry;
    }

    /**
     * Removes the given child node entry.
     *
     * @param entry entry to be removed.
     * @return the removed entry or <code>null</code> if there is no such entry.
     */
    public ChildNodeEntry remove(ChildNodeEntry entry) {
        return remove(entry.getId());
    }

    /**
     * Removes all child node entries
     */
    public void removeAll() {
        init();
    }

    /**
     * Returns a list of <code>ChildNodeEntry</code>s who do only exist in
     * <code>this</code> but not in <code>other</code>.
     * <p>
     * Note that two entries are considered identical in this context if
     * they have the same name and uuid, i.e. the index is disregarded
     * whereas <code>ChildNodeEntry.equals(Object)</code> also compares
     * the index.
     *
     * @param other entries to be removed
     * @return a new list of those entries that do only exist in
     *         <code>this</code> but not in <code>other</code>
     */
    List<ChildNodeEntry> removeAll(ChildNodeEntries other) {
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        if (other.isEmpty()) {
            return list();
        }

        List<ChildNodeEntry> result = new ArrayList<ChildNodeEntry>();
        for (Object e : entries.values()) {
            ChildNodeEntry entry = (ChildNodeEntry) e;
            ChildNodeEntry otherEntry = other.get(entry.getId());
            if (entry == otherEntry) {
                continue;
            }
            if (otherEntry == null
                    || !entry.getName().equals(otherEntry.getName())) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * Returns a list of <code>ChildNodeEntry</code>s who do exist in
     * <code>this</code> <i>and</i> in <code>other</code>.
     * <p>
     * Note that two entries are considered identical in this context if
     * they have the same name and uuid, i.e. the index is disregarded
     * whereas <code>ChildNodeEntry.equals(Object)</code> also compares
     * the index.
     *
     * @param other entries to be retained
     * @return a new list of those entries that do exist in
     *         <code>this</code> <i>and</i> in <code>other</code>
     */
    List<ChildNodeEntry> retainAll(ChildNodeEntries other) {
        if (entries.isEmpty()
                || other.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChildNodeEntry> result = new ArrayList<ChildNodeEntry>();
        for (Object e : entries.values()) {
            ChildNodeEntry entry = (ChildNodeEntry) e;
            ChildNodeEntry otherEntry = other.get(entry.getId());
            if (entry == otherEntry) {
                result.add(entry);
            } else if (otherEntry != null
                    && entry.getName().equals(otherEntry.getName())) {
                result.add(entry);
            }
        }
        return result;
    }

    //-----------------------------------------------< unmodifiable List view >

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public List<ChildNodeEntry> list() {
        return new ArrayList<ChildNodeEntry>(entries.values());
    }

    public List<ChildNodeEntry> getRenamedEntries(ChildNodeEntries that) {
        List<ChildNodeEntry> renamed = Collections.emptyList();
        for (Object e : entries.values()) {
            ChildNodeEntry entry = (ChildNodeEntry) e;
            ChildNodeEntry other = that.get(entry.getId());
            if (other != null && !entry.getName().equals(other.getName())) {
                // child node entry with same id but different name exists in
                // overlaid and this state => renamed entry detected
                if (renamed.isEmpty()) {
                    renamed = new ArrayList<ChildNodeEntry>();
                }
                renamed.add(entry);
            }
        }
        return renamed;
    }

    public int size() {
        return entries.size();
    }

    //-------------------------------------------< java.lang.Object overrides >
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ChildNodeEntries) {
            ChildNodeEntries other = (ChildNodeEntries) obj;
            return (nameMap.equals(other.nameMap)
                    && entries.equals(other.entries)
                    && shared == other.shared);
        }
        return false;
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

    //----------------------------------------------------< Cloneable support >

    /**
     * Returns a shallow copy of this <code>ChildNodeEntries</code> instance;
     * the entries themselves are not cloned.
     *
     * @return a shallow copy of this instance.
     */
    protected Object clone() {
        try {
            ChildNodeEntries clone = (ChildNodeEntries) super.clone();
            if (nameMap != Collections.EMPTY_MAP) {
                clone.shared = true;
                shared = true;
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            // never happens, this class is cloneable
            throw new InternalError();
        }
    }

    //-------------------------------------------------------------< internal >

    /**
     * Initializes the name and entries map with unmodifiable empty instances.
     */
    private void init() {
        nameMap = Collections.emptyMap();
        entries = EmptyLinkedMap.INSTANCE;
        shared = false;
    }

    /**
     * Ensures that the {@link #nameMap} and {@link #entries} map are
     * modifiable.
     */
    @SuppressWarnings("unchecked")
    private void ensureModifiable() {
        if (nameMap == Collections.EMPTY_MAP) {
            nameMap = new HashMap<Name, Object>();
            entries = new LinkedMap();
        } else if (shared) {
            entries = (LinkedMap) entries.clone();
            nameMap = new HashMap<Name, Object>(nameMap);
            for (Map.Entry<Name, Object> entry : nameMap.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof List<?>) {
                    entry.setValue(new ArrayList<ChildNodeEntry>(
                            (List<ChildNodeEntry>) value));
                }
            }
            shared = false;
        }
    }

}
