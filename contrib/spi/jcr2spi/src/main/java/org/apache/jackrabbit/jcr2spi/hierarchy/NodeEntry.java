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
package org.apache.jackrabbit.jcr2spi.hierarchy;

import org.apache.jackrabbit.jcr2spi.state.NoSuchItemStateException;
import org.apache.jackrabbit.jcr2spi.state.ItemStateException;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.Event;

import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.PathNotFoundException;
import java.util.Iterator;
import java.util.List;
import java.util.Collection;

/**
 * <code>NodeEntry</code>...
 */
public interface NodeEntry extends HierarchyEntry {

    /**
     * @return the <code>NodeId</code> of this child node entry.
     */
    public NodeId getId();

    /**
     * @return the unique ID of the node state which is referenced by this
     * child node entry or <code>null</code> if the node state cannot be
     * identified with a unique ID.
     */
    public String getUniqueID();

    /**
     *
     * @param uniqueID
     */
    public void setUniqueID(String uniqueID);

    /**
     * @return the index of this child node entry to suppport same-name siblings.
     * If the index of this entry cannot be determined
     * {@link org.apache.jackrabbit.name.Path#INDEX_UNDEFINED} is returned.
     */
    public int getIndex();

    /**
     * @return the referenced <code>NodeState</code>.
     * @throws NoSuchItemStateException if the <code>NodeState</code> does not
     * exist anymore.
     * @throws ItemStateException If an error occurs while retrieving the
     * <code>NodeState</code>.
     */
    public NodeState getNodeState() throws NoSuchItemStateException, ItemStateException;

    /**
     * Traverse the tree below this entry and return the child entry matching
     * the given path. If that entry has not been loaded yet, try to do so.
     * NOTE: In contrast to 'getNodeEntry', getNodeEntries, getPropertyEntry
     * and getPropertyEntries this method may return invalid entries, i.e.
     * entries connected to a removed or stale ItemState.
     *
     * @param path
     * @return the entry at the given path.
     */
    public HierarchyEntry getDeepEntry(Path path) throws PathNotFoundException, RepositoryException;

    /**
     * Determines if there is a valid <code>NodeEntry</code> with the
     * specified <code>nodeName</code>.
     *
     * @param nodeName <code>QName</code> object specifying a node name
     * @return <code>true</code> if there is a <code>NodeEntry</code> with
     * the specified <code>nodeName</code>.
     */
    public boolean hasNodeEntry(QName nodeName);

    /**
     * Determines if there is a valid <code>NodeEntry</code> with the
     * specified <code>name</code> and <code>index</code>.
     *
     * @param nodeName  <code>QName</code> object specifying a node name.
     * @param index 1-based index if there are same-name child node entries.
     * @return <code>true</code> if there is a <code>NodeEntry</code> with
     * the specified <code>name</code> and <code>index</code>.
     */
    public boolean hasNodeEntry(QName nodeName, int index);

    /**
     * Returns the valid <code>NodeEntry</code> with the specified name
     * and index or <code>null</code> if there's no matching entry.
     *
     * @param nodeName <code>QName</code> object specifying a node name.
     * @param index 1-based index if there are same-name child node entries.
     * @return The <code>NodeEntry</code> with the specified name and index
     * or <code>null</code> if there's no matching entry.
     */
    public NodeEntry getNodeEntry(QName nodeName, int index);

    /**
     * Returns the <code>NodeEntry</code> with the specified
     * <code>NodeId</code> or <code>null</code> if there's no matching
     * entry.
     *
     * @param childId the id of the child entry.
     * @return the <code>NodeEntry</code> with the specified
     * <code>NodeId</code> or <code>null</code> if there's no matching entry.
     */
    public NodeEntry getNodeEntry(NodeId childId);

    /**
     * Returns a unmodifiable iterator of <code>NodeEntry</code> objects
     * denoting the the valid child NodeEntries present on this <code>NodeEntry</code>.
     *
     * @return iterator of <code>NodeEntry</code> objects
     */
    public Iterator getNodeEntries();

    /**
     * Returns a unmodifiable List of <code>NodeEntry</code>s with the
     * specified name.
     *
     * @param nodeName name of the child node entries that should be returned
     * @return list of <code>NodeEntry</code> objects
     */
    public List getNodeEntries(QName nodeName);

    /**
     * Adds a new child NodeEntry to this entry.
     *
     * @param nodeName
     * @param uniqueID
     * @return the new <code>NodeEntry</code>
     */
    public NodeEntry addNodeEntry(QName nodeName, String uniqueID, int index);

    /**
     * Adds a new, transient child <code>NodeEntry</code>
     *
     * @return
     */
    public NodeState addNewNodeEntry(QName nodeName, String uniqueID, QName primaryNodeType, QNodeDefinition definition) throws ItemExistsException;

    /**
     * @param newName
     * @param newParent
     * @return
     */
    public NodeEntry moveNodeEntry(NodeState childState, QName newName, NodeEntry newParent) throws RepositoryException;

    /**
     * Determines if there is a property entry with the specified <code>QName</code>.
     *
     * @param propName <code>QName</code> object specifying a property name
     * @return <code>true</code> if there is a property entry with the specified
     * <code>QName</code>.
     */
    public boolean hasPropertyEntry(QName propName);

    /**
     * Returns the valid <code>PropertyEntry</code> with the specified name
     * or <code>null</code> if no matching entry exists.
     *
     * @param propName <code>QName</code> object specifying a property name.
     * @return The <code>PropertyEntry</code> with the specified name or
     * <code>null</code> if no matching entry exists.
     */
    public PropertyEntry getPropertyEntry(QName propName);

    /**
     * Returns an unmodifiable Iterator over those children that represent valid
     * PropertyEntries.
     *
     * @return
     */
    public Iterator getPropertyEntries();

    /**
     *
     * @param propName
     * @return
     */
    public PropertyEntry addPropertyEntry(QName propName) throws ItemExistsException;

    /**
     * Adds property entries for the given <code>QName</code>s. It depends on
     * the status of this <code>NodeEntry</code>, how conflicts are resolved
     * and whether or not existing entries that are missing in the iterator
     * get removed.
     *
     * @param propNames
     * @throws ItemExistsException
     */
    public void addPropertyEntries(Collection propNames) throws ItemExistsException;

    /**
     *
     * @param propName
     * @return
     */
    public PropertyState addNewPropertyEntry(QName propName, QPropertyDefinition definition) throws ItemExistsException;

    /**
     * Reorders this NodeEntry before the sibling entry specified by the given
     * <code>beforeEntry</code>.
     *
     * @param beforeEntry the child node where to insert the node before. If
     * <code>null</code> this entry is moved to the end of its parents child node entries.
     * @return true if the reordering was successful. False if either of the
     * given entry does not exist in the listed child entries..
     */
    public boolean orderBefore(NodeEntry beforeEntry) ;

    /**
     * The parent entry of a external event gets informed about the modification.
     * Note, that {@link Event#getParentId()} of the given childEvent must point
     * to this <code>NodeEntry</code>.
     *
     * @param childEvent
     */
    public void refresh(Event childEvent) ;
}