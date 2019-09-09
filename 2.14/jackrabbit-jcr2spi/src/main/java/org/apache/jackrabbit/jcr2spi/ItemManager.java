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

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;

/**
 * There's one <code>ItemManager</code> instance per <code>Session</code>
 * instance. It is the factory for <code>Node</code> and <code>Property</code>
 * instances.
 * <p>
 * The <code>ItemManager</code>'s responsibilities are:
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
 * <p>
 * If the parent <code>Session</code> is an <code>XASession</code>, there is
 * one <code>ItemManager</code> instance per started global transaction.
 */
public interface ItemManager {

    /**
     * Disposes this <code>ItemManager</code> and frees resources.
     */
    public void dispose();

    /**
     * Checks if the node with the given path exists.
     *
     * @param path path to the node to be checked
     * @return true if the specified item exists
     * @throws RepositoryException
     */
    public boolean nodeExists(Path path) throws RepositoryException;

    /**
     * Checks if the property with the given path exists.
     *
     * @param path path to the property to be checked
     * @return true if the specified item exists
     * @throws RepositoryException
     */
    public boolean propertyExists(Path path) throws RepositoryException;

    /**
     * Checks if the item for given HierarchyEntry exists.
     *
     * @param hierarchyEntry
     * @return true if the specified item exists
      @throws RepositoryException
     */
    public boolean itemExists(HierarchyEntry hierarchyEntry) throws RepositoryException;

    /**
     *
     * @param path
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public Node getNode(Path path) throws PathNotFoundException, RepositoryException;

    /**
     *
     * @param path
     * @return
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public Property getProperty(Path path) throws PathNotFoundException, RepositoryException;

    /**
     *
     * @param hierarchyEntry
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public Item getItem(HierarchyEntry hierarchyEntry) throws ItemNotFoundException, RepositoryException;

    /**
     *
     * @param parentEntry
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public boolean hasChildNodes(NodeEntry parentEntry) throws ItemNotFoundException, RepositoryException;

    /**
     *
     * @param parentEntry
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public NodeIterator getChildNodes(NodeEntry parentEntry) throws ItemNotFoundException, RepositoryException;

    /**
     *
     * @param parentEntry
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public boolean hasChildProperties(NodeEntry parentEntry) throws ItemNotFoundException, RepositoryException;

    /**
     *
     * @param parentEntry
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    public PropertyIterator getChildProperties(NodeEntry parentEntry) throws ItemNotFoundException, RepositoryException;
}
