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
import javax.jcr.ItemNotFoundException;
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
     * Returns the ID that must be used for resolving this entry OR loading its
     * children entries from the persistent layer. This is the same as
     * <code>getId()</code> unless this entry or any of its ancestors has been
     * transiently moved.
     *
     * @return
     * @see #getId()
     */
    public NodeId getWorkspaceId();

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
     * @throws ItemNotFoundException if the <code>NodeState</code> does not
     * exist.
     * @throws RepositoryException If an error occurs while retrieving the
     * <code>NodeState</code>.
     */
    public NodeState getNodeState() throws ItemNotFoundException, RepositoryException;

    /**
     * Traverse the tree below this entry and return the child entry matching
     * the given path. If that entry has not been loaded yet, try to do so.
     * NOTE: In contrast to 'getNodeEntry', getNodeEntries, getPropertyEntry
     * and getPropertyEntries this method may return invalid entries, i.e.
     * entries connected to a removed or stale ItemState.
     *
     * @param path
     * @return the entry at the given path.
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public HierarchyEntry getDeepEntry(Path path) throws PathNotFoundException, RepositoryException;

    /**
     * Traverse the tree below this entry and return the child entry matching
     * the given 'workspacePath', i.e. transient modifications and new entries
     * are ignored.<p/>
     * If no matching entry can be found, <code>null</code> is return.
     *
     * @param workspacePath
     * @return matching entry or <code>null</code>.
     */
    public HierarchyEntry lookupDeepEntry(Path workspacePath);

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
     * @throws RepositoryException If an unexpected error occurs.
     */
    public NodeEntry getNodeEntry(QName nodeName, int index) throws RepositoryException;

    /**
     * Returns the valid <code>NodeEntry</code> with the specified name
     * and index or <code>null</code> if there's no matching entry. If
     * <code>loadIfNotFound</code> is true, the implementation must make
     * sure, that it's list of child entries is up to date and eventually
     * try to load the node entry.
     *
     * @param nodeName <code>QName</code> object specifying a node name.
     * @param index 1-based index if there are same-name child node entries.
     * @param loadIfNotFound
     * @return The <code>NodeEntry</code> with the specified name and index
     * or <code>null</code> if there's no matching entry.
     * @throws RepositoryException If an unexpected error occurs.
     */
    public NodeEntry getNodeEntry(QName nodeName, int index, boolean loadIfNotFound) throws RepositoryException;

    /**
     * Returns a unmodifiable iterator of <code>NodeEntry</code> objects
     * denoting the the valid child NodeEntries present on this <code>NodeEntry</code>.
     *
     * @return iterator of <code>NodeEntry</code> objects
     * @throws RepositoryException If an unexpected error occurs.
     */
    public Iterator getNodeEntries() throws RepositoryException;

    /**
     * Returns a unmodifiable List of <code>NodeEntry</code>s with the
     * specified name.
     *
     * @param nodeName name of the child node entries that should be returned
     * @return list of <code>NodeEntry</code> objects
     * @throws RepositoryException If an unexpected error occurs.
     */
    public List getNodeEntries(QName nodeName) throws RepositoryException;

    /**
     * Adds a new child NodeEntry to this entry.
     *
     * @param nodeName
     * @param uniqueID
     * @return the new <code>NodeEntry</code>
     * @throws RepositoryException If an unexpected error occurs.
     */
    public NodeEntry addNodeEntry(QName nodeName, String uniqueID, int index) throws RepositoryException;

    /**
     * Adds a new, transient child <code>NodeEntry</code>
     *
     * @param nodeName
     * @param uniqueID
     * @param primaryNodeType
     * @param definition
     * @return
     * @throws RepositoryException If an error occurs.
     */
    public NodeState addNewNodeEntry(QName nodeName, String uniqueID, QName primaryNodeType, QNodeDefinition definition) throws RepositoryException;

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
     * @throws RepositoryException If an unexpected error occurs.
     */
    public PropertyEntry getPropertyEntry(QName propName) throws RepositoryException;

    /**
     * Returns the valid <code>PropertyEntry</code> with the specified name
     * or <code>null</code> if no matching entry exists.  If
     * <code>loadIfNotFound</code> is true, the implementation must make
     * sure, that it's list of property entries is up to date and eventually
     * try to load the property entry with the given name.
     *
     * @param propName <code>QName</code> object specifying a property name.
     * @param loadIfNotFound
     * @return The <code>PropertyEntry</code> with the specified name or
     * <code>null</code> if no matching entry exists.
     * @throws RepositoryException If an unexpected error occurs.
     */
    public PropertyEntry getPropertyEntry(QName propName,  boolean loadIfNotFound) throws RepositoryException;

    /**
     * Returns an unmodifiable Iterator over those children that represent valid
     * PropertyEntries.
     *
     * @return
     */
    public Iterator getPropertyEntries();

    /**
     * Add an existing <code>PropertyEntry</code> with the given name.
     * Please note the difference to {@link #addNewPropertyEntry(QName, QPropertyDefinition)}
     * which adds a new, transient entry.
     *
     * @param propName
     * @return the <code>PropertyEntry</code>
     * @throws ItemExistsException if a child item exists with the given name
     * @throws RepositoryException if an unexpected error occurs.
     */
    public PropertyEntry addPropertyEntry(QName propName) throws ItemExistsException, RepositoryException;

    /**
     * Adds property entries for the given <code>QName</code>s. It depends on
     * the status of this <code>NodeEntry</code>, how conflicts are resolved
     * and whether or not existing entries that are missing in the iterator
     * get removed.
     *
     * @param propNames
     * @throws ItemExistsException
     * @throws RepositoryException if an unexpected error occurs.
     */
    public void addPropertyEntries(Collection propNames) throws ItemExistsException, RepositoryException;

    /**
     * Add a new, transient <code>PropertyEntry</code> to this <code>NodeEntry</code>
     * and return the <code>PropertyState</code> associated with the new entry.
     *
     * @param propName
     * @return The PropertyState associated with the new property entry.
     * @throws ItemExistsException
     * @throws RepositoryException if an unexpected error occurs.
     */
    public PropertyState addNewPropertyEntry(QName propName, QPropertyDefinition definition) throws ItemExistsException, RepositoryException;

    /**
     * Reorders this NodeEntry before the sibling entry specified by the given
     * <code>beforeEntry</code>.
     *
     * @param beforeEntry the child node where to insert the node before. If
     * <code>null</code> this entry is moved to the end of its parents child node entries.
     * @throws RepositoryException If an unexpected error occurs.
     */
    public void orderBefore(NodeEntry beforeEntry) throws RepositoryException;

    /**
     * Moves this <code>NodeEntry</code> as new child entry of the
     * <code>NodeEntry</code> identified by <code>newParent</code> and/or renames
     * it to <code>newName</code>. If <code>transientMove</code> is true, an
     * implementation must make sure, that reverting this modification by calling
     * {@link HierarchyEntry#revert()} on the common ancestor of both parents
     * moves this NodeEntry back and resets the name to its original value.
     *
     * @param newName
     * @param newParent
     * @return the moved entry
     * @throws RepositoryException If the entry to be moved is not a child of this
     * NodeEntry or if an unexpected error occurs.
     */
    public NodeEntry move(QName newName, NodeEntry newParent, boolean transientMove) throws RepositoryException;

    /**
     * @return true if this <code>NodeEntry</code> is transiently moved.
     */
    public boolean isTransientlyMoved();

    /**
     * The parent entry of a external event gets informed about the modification.
     * Note, that {@link Event#getParentId()} of the given childEvent must point
     * to this <code>NodeEntry</code>.
     *
     * @param childEvent
     */
    public void refresh(Event childEvent) ;
}
