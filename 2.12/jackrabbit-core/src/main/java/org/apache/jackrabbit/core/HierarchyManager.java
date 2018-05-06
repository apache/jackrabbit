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
package org.apache.jackrabbit.core;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

/**
 * The <code>HierarchyManager</code> interface ...
 */
public interface HierarchyManager {

    /**
     * Resolves a path into an item id.
     * <p>
     * If there is both a node and a property at the specified path, this method
     * will return the id of the node.
     * <p>
     * Note that, for performance reasons, this method returns <code>null</code>
     * rather than throwing a <code>PathNotFoundException</code> if there's no
     * item to be found at <code>path</code>.
     *
     * @deprecated As of JSR 283, a <code>Path</code> doesn't anymore uniquely
     * identify an <code>Item</code>, therefore {@link #resolveNodePath(Path)} and
     * {@link #resolvePropertyPath(Path)} should be used instead.
     *
     * @param path path to resolve
     * @return item id referred to by <code>path</code> or <code>null</code>
     *         if there's no item at <code>path</code>.
     * @throws RepositoryException if an error occurs
     */
    ItemId resolvePath(Path path) throws RepositoryException;

    /**
     * Resolves a path into a node id.
     * <p>
     * Note that, for performance reasons, this method returns <code>null</code>
     * rather than throwing a <code>PathNotFoundException</code> if there's no
     * node to be found at <code>path</code>.
     *
     * @param path path to resolve
     * @return node id referred to by <code>path</code> or <code>null</code>
     *         if there's no node at <code>path</code>.
     * @throws RepositoryException if an error occurs
     */
    NodeId resolveNodePath(Path path) throws RepositoryException;

    /**
     * Resolves a path into a property id.
     * <p>
     * Note that, for performance reasons, this method returns <code>null</code>
     * rather than throwing a <code>PathNotFoundException</code> if there's no
     * property to be found at <code>path</code>.
     *
     * @param path path to resolve
     * @return property id referred to by <code>path</code> or <code>null</code>
     *         if there's no property at <code>path</code>.
     * @throws RepositoryException if an error occurs
     */
    PropertyId resolvePropertyPath(Path path) throws RepositoryException;

    /**
     * Returns the path to the given item.
     * @param id
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    Path getPath(ItemId id) throws ItemNotFoundException, RepositoryException;

    /**
     * Returns the name of the specified item.
     * @param id id of item whose name should be returned
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    Name getName(ItemId id) throws ItemNotFoundException, RepositoryException;

    /**
     * Returns the name of the specified item, with the given parent id. If the
     * given item is not shareable, this is identical to {@link #getName(ItemId)}.
     *
     * @param id node id
     * @param parentId parent node id
     * @return name
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    Name getName(NodeId id, NodeId parentId)
            throws ItemNotFoundException, RepositoryException;

    /**
     * Returns the depth of the specified item which is equivalent to
     * <code>getPath(id).getAncestorCount()</code>. The depth reflects the
     * absolute hierarchy level.
     *
     * @param id item id
     * @return the depth of the specified item
     * @throws ItemNotFoundException if the specified <code>id</code> does not
     *                               denote an existing item.
     * @throws RepositoryException   if another error occurs
     */
    int getDepth(ItemId id) throws ItemNotFoundException, RepositoryException;

    /**
     * Returns the depth of the specified descendant relative to the given
     * ancestor. If <code>ancestorId</code> and <code>descendantId</code>
     * denote the same item 0 is returned. If <code>ancestorId</code> does not
     * denote an ancestor -1 is returned.
     *
     * @param ancestorId ancestor id
     * @param descendantId descendant id
     * @return the relative depth; -1 if <code>ancestorId</code> does not
     *         denote an ancestor of the item denoted by <code>descendantId</code>
     *         (or itself).
     * @throws ItemNotFoundException if either of the specified id's does not
     *                               denote an existing item.
     * @throws RepositoryException   if another error occurs
     */
    int getRelativeDepth(NodeId ancestorId, ItemId descendantId)
            throws ItemNotFoundException, RepositoryException;

    /**
     * Determines whether the node with the specified <code>nodeId</code>
     * is an ancestor of the item denoted by the given <code>itemId</code>.
     * This is equivalent to
     * <code>getPath(nodeId).isAncestorOf(getPath(itemId))</code>.
     *
     * @param nodeId node id
     * @param itemId item id
     * @return <code>true</code> if the node with the specified
     *         <code>nodeId</code> is an ancestor of the item denoted by the
     *         given <code>itemId</code>; <code>false</code> otherwise
     * @throws ItemNotFoundException if any of the specified id's does not
     *                               denote an existing item.
     * @throws RepositoryException   if another error occurs
     */
    boolean isAncestor(NodeId nodeId, ItemId itemId)
            throws ItemNotFoundException, RepositoryException;

    //------------------------------------------- operation with shareable nodes

    /**
     * Determines whether the node with the specified <code>ancestor</code>
     * is a share ancestor of the item denoted by the given <code>descendant</code>.
     * This is <code>true</code> for two nodes <code>A</code>, <code>B</code>
     * if either:
     * <ul>
     * <li><code>A</code> is a (proper) ancestor of <code>B</code></li>
     * <li>there is a non-empty sequence of nodes <code>N<sub>1</sub></code>,...
     * ,<code>N<sub>k</sub></code> such that <code>A</code>=
     * <code>N<sub>1</sub></code> and <code>B</code>=<code>N<sub>k</sub></code>
     * and <code>N<sub>i</sub></code> is the parent or a share-parent of
     * <code>N<sub>i+1</sub></code> (for every <code>i</code> in <code>1</code>
     * ...<code>k-1</code>.</li>
     * </ul>
     *
     * @param ancestor node id
     * @param descendant item id
     * @return <code>true</code> if the node denoted by <code>ancestor</code>
     *         is a share ancestor of the item denoted by <code>descendant</code>,
     *         <code>false</code> otherwise
     * @throws ItemNotFoundException if any of the specified id's does not
     *                               denote an existing item.
     * @throws RepositoryException   if another error occurs
     */
    boolean isShareAncestor(NodeId ancestor, NodeId descendant)
            throws ItemNotFoundException, RepositoryException;

    /**
     * Returns the depth of the specified share-descendant relative to the given
     * share-ancestor. If <code>ancestor</code> and <code>descendant</code>
     * denote the same item, <code>0</code> is returned. If <code>ancestor</code>
     * does not denote an share-ancestor <code>-1</code> is returned.
     *
     * @param ancestorId ancestor id
     * @param descendantId descendant id
     * @return the relative depth; <code>-1</code> if <code>ancestor</code> does
     *         not denote a share-ancestor of the item denoted by <code>descendant</code>
     *         (or itself).
     * @throws ItemNotFoundException if either of the specified id's does not
     *                               denote an existing item.
     * @throws RepositoryException   if another error occurs
     */
    int getShareRelativeDepth(NodeId ancestorId, ItemId descendantId)
            throws ItemNotFoundException, RepositoryException;
}
