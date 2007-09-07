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

import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

/**
 * The <code>HierarchyManager</code> interface ...
 */
public interface HierarchyManager {

    /**
     * Resolves a path into an item id.
     * <p/>
     * Note that, for performance reasons, this method returns <code>null</code>
     * rather than throwing a <code>PathNotFoundException</code> if there's no
     * item to be found at <code>path</code>.
     *  
     * @param path path to resolve
     * @return item id refered to by <code>path</code> or <code>null</code>
     *         if there's no item at <code>path</code>.
     * @throws RepositoryException if an error occurs
     */
    ItemId resolvePath(Path path) throws RepositoryException;

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
    QName getName(ItemId id) throws ItemNotFoundException, RepositoryException;

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
     *         given <code>itemId</code; <code>false</code> otherwise
     * @throws ItemNotFoundException if any of the specified id's does not
     *                               denote an existing item.
     * @throws RepositoryException   if another error occurs
     */
    boolean isAncestor(NodeId nodeId, ItemId itemId)
            throws ItemNotFoundException, RepositoryException;
}
