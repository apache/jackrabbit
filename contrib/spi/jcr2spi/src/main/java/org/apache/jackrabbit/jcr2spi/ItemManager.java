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
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;

import javax.jcr.PathNotFoundException;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;

/**
 * <code>ItemManager</code>...
 */
public interface ItemManager extends ItemLifeCycleListener {

    // DIFF JR: removed 'getRootNode' shortcut
    
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
     * @param id id of the item to be checked
     * @return true if the specified item exists
     */
    public boolean itemExists(ItemId id);

    /**
     * @param path
     * @return
     * @throws javax.jcr.PathNotFoundException
     * @throws javax.jcr.AccessDeniedException
     * @throws javax.jcr.RepositoryException
     */
    public ItemImpl getItem(Path path)
            throws PathNotFoundException, AccessDeniedException, RepositoryException;

    /**
     * @param id
     * @return
     * @throws javax.jcr.RepositoryException
     */
    public ItemImpl getItem(ItemId id)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException;

    /**
     *
     * @param parentId
     * @return
     * @throws ItemNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    public boolean hasChildNodes(NodeId parentId) throws ItemNotFoundException,
        AccessDeniedException, RepositoryException;

    /**
     *
     * @param parentId
     * @return
     * @throws ItemNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    public NodeIterator getChildNodes(NodeId parentId)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException;

    /**
     *
     * @param parentId
     * @return
     * @throws ItemNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    public boolean hasChildProperties(NodeId parentId)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException;

    /**
     *
     * @param parentId
     * @return
     * @throws ItemNotFoundException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    public PropertyIterator getChildProperties(NodeId parentId)
            throws ItemNotFoundException, AccessDeniedException, RepositoryException;
}
