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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.NodeState;

import javax.jcr.PathNotFoundException;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.Item;

/**
 * There's one <code>ItemManager</code> instance per <code>Session</code>
 * instance. It is the factory for <code>Node</code> and <code>Property</code>
 * instances.
 * <p/>
 * The <code>ItemManager</code>'s responsabilities are:
 * <ul>
 * <li>providing access to <code>Item</code> instances by <code>ItemState</code>
 * whereas <code>Node</code> and <code>Item</code> are only providing relative access.
 * <li>returning the instance of an existing <code>Node</code> or <code>Property</code>,
 * given its absolute path.
 * <li>creating the per-session instance of a <code>Node</code>
 * or <code>Property</code> that doesn't exist yet and needs to be created first.
 * <li>guaranteeing that there aren't multiple instances representing the same
 * <code>Node</code> or <code>Property</code> associated with the same
 * <code>Session</code> instance.
 * <li>maintaining a cache of the item instances it created.
 * </ul>
 * <p/>
 * If the parent <code>Session</code> is an <code>XASession</code>, there is
 * one <code>ItemManager</code> instance per started global transaction.
 */
public interface ItemManager extends ItemLifeCycleListener {

    /**
     * Disposes this <code>ItemManager</code> and frees resources.
     */
    public void dispose();

    /**
     * Checks if the item with the given path exists.
     *
     * @param path path to the item to be checked
     * @return true if the specified item exists
     */
    public boolean itemExists(Path path);

    /**
     * Checks if the item with the given id exists.
     *
     * @param itemState state of the item to be checked
     * @return true if the specified item exists
     */
    public boolean itemExists(ItemState itemState);


    /**
     *
     * @param path
     * @return The item for the given path.
     * @throws javax.jcr.PathNotFoundException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.RepositoryException
     */
    public Item getItem(Path path)
        throws PathNotFoundException, AccessDeniedException, RepositoryException;

    /**
     *
     * @param itemState
     * @return
     * @throws ItemNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    public Item getItem(ItemState itemState)
        throws ItemNotFoundException, AccessDeniedException, RepositoryException;

    /**
     *
     * @param parentState
     * @return
     * @throws ItemNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    public boolean hasChildNodes(NodeState parentState)
        throws ItemNotFoundException, AccessDeniedException, RepositoryException;

    /**
     *
     * @param parentState
     * @return
     * @throws ItemNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    public NodeIterator getChildNodes(NodeState parentState)
        throws ItemNotFoundException, AccessDeniedException, RepositoryException;

    /**
     *
     * @param parentState
     * @return
     * @throws ItemNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    public boolean hasChildProperties(NodeState parentState)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException;

    /**
     *
     * @param parentState
     * @return
     * @throws ItemNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    public PropertyIterator getChildProperties(NodeState parentState)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException;
}
