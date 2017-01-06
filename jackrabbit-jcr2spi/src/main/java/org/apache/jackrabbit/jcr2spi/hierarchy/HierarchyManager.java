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
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PropertyId;

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * <code>HierarchyManager</code>...
 */
public interface HierarchyManager {

    /**
     * Dispose this <code>HierarchyManager</code>
     */
    public void dispose();

    /**
     * @return the root entry.
     */
    public NodeEntry getRootEntry();

    /**
     * Lookup of <code>HierarchyEntry</code> by its workspace Id that may be different
     * if a entry (or any of its ancestors) has been transiently moved or
     * reordered.
     * <p>
     * If the Hierarchy already lists the entry with the given workspaceItemId it is
     * returned otherwise <code>null</code>. See {@link #getNodeEntry(NodeId)}
     * or {@link #getPropertyEntry(PropertyId)} for methods that resolves the
     * ItemId including lookup in the persistence layer if the entry has not been
     * loaded yet.
     *
     * @param workspaceItemId
     * @return the HierarchyEntry with the given <code>workspaceItemId</code>.
     */
    public HierarchyEntry lookup(ItemId workspaceItemId);

    /**
     * Lookup of <code>HierarchyEntry</code> by its workspace path that may be different
     * if a entry (or any of its ancestors) has been transiently moved or
     * reordered.
     * <p>
     * If the Hierarchy already lists the entry with the given path it is
     * returned otherwise <code>null</code>. See {@link #getNodeEntry(Path)}
     * or {@link #getPropertyEntry(Path)} for methods that resolves the path
     * including lookup in the persistence layer if the entry has not been loaded yet.
     *
     * @param workspacePath
     * @return the HierarchyEntry with the given <code>workspacePath</code>.
     */
    public HierarchyEntry lookup(Path workspacePath);

    /**
     * Resolves a itemId into a <code>HierarchyEntry</code>.
     *
     * @param nodeId
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public NodeEntry getNodeEntry(NodeId nodeId) throws ItemNotFoundException, RepositoryException;

    /**
     * Resolves a path into a <code>NodeEntry</code>.
     *
     * @param qPath
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public NodeEntry getNodeEntry(Path qPath) throws PathNotFoundException, RepositoryException;

    /**
     * Resolves a propertyId into a <code>PropertyEntry</code>.
     *
     * @param propertyId
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public PropertyEntry getPropertyEntry(PropertyId propertyId) throws ItemNotFoundException, RepositoryException;

    /**
     * Resolves a path into a <code>PropertyEntry</code>.
     *
     * @param qPath
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public PropertyEntry getPropertyEntry(Path qPath) throws PathNotFoundException, RepositoryException;

     /**
     * Retrieves the <code>NodeEntry</code> corresponding to the given
     * path and resolves it to the underlying <code>NodeState</code>.
     *
     * @param qPath
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public NodeState getNodeState(Path qPath) throws PathNotFoundException, RepositoryException;

     /**
     * Retrieves the <code>PropertyEntry</code> corresponding to the given
     * path and resolves it to the underlying <code>PropertyState</code>.
     *
     * @param qPath
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public PropertyState getPropertyState(Path qPath) throws PathNotFoundException, RepositoryException;

    /**
     * Returns the depth of the specified item. The depth reflects the
     * absolute hierarchy level.
     *
     * @param hierarchyEntry
     * @return the depth of the specified item
     * @throws RepositoryException if another error occurs
     */
    public int getDepth(HierarchyEntry hierarchyEntry) throws ItemNotFoundException, RepositoryException;

    /**
     * Returns the depth of the specified descendant relative to the given
     * ancestor. If <code>ancestor</code> and <code>descendant</code>
     * denote the same item 0 is returned. If <code>ancestor</code> does not
     * denote an ancestor -1 is returned.
     *
     * @param ancestor NodeEntry that must be an ancestor of the descendant
     * @param descendant HierarchyEntry
     * @return the relative depth; -1 if <code>ancestor</code> does not
     * denote an ancestor of the item denoted by <code>descendant</code>
     * (or itself).
     * @throws ItemNotFoundException If either of the specified id's does not
     * denote an existing item.
     * @throws RepositoryException If another error occurs.
     */
    public int getRelativeDepth(NodeEntry ancestor, HierarchyEntry descendant) throws ItemNotFoundException, RepositoryException;
}