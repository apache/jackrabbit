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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import java.util.Iterator;
import java.util.List;

/**
 * This interface defines the version manager. It gives access to the underlaying
 * persistence layer of the versioning.
 */
public interface VersionManager {
    /**
     * returns the virtual item state provider that exposes the internal versions
     * as items.
     *
     * @param base
     * @return
     */
    public VirtualItemStateProvider getVirtualItemStateProvider(ItemStateManager base);

    /**
     * Creates a new version history. This action is needed either when creating
     * a new 'mix:versionable' node or when adding the 'mix:versionalbe' mixin
     * to a node.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public VersionHistory createVersionHistory(NodeImpl node) throws RepositoryException;

    /**
     * invokes the checkin() on the persistent version manager and remaps the
     * newly created version objects.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public Version checkin(NodeImpl node) throws RepositoryException;

    //-----------------------------------------------------< internal stuff >---

    /**
     * Checks if the version history with the given id exists
     *
     * @param id
     * @return
     */
    public boolean hasVersionHistory(String id);

    /**
     * Returns the version history with the given id
     *
     * @param id
     * @return
     * @throws RepositoryException
     */
    public InternalVersionHistory getVersionHistory(String id) throws RepositoryException;

    /**
     * Returns the number of version histories
     *
     * @return
     * @throws RepositoryException
     */
    public int getNumVersionHistories() throws RepositoryException;

    /**
     * Returns an iterator over all ids of {@link InternalVersionHistory}s.
     *
     * @return
     * @throws RepositoryException
     */
    public Iterator getVersionHistoryIds() throws RepositoryException;

    /**
     * Checks if the version with the given id exists
     *
     * @param id
     * @return
     */
    boolean hasVersion(String id);

    /**
     * Returns the version with the given id
     *
     * @param id
     * @return
     * @throws RepositoryException
     */
    InternalVersion getVersion(String id) throws RepositoryException;

    /**
     * checks, if the node with the given id exists
     * todo: move probably to VersionManagerImpl
     *
     * @param id
     * @return
     */
    public boolean hasItem(String id);

    /**
     * Returns the version item with the given id
     * todo: move probably to VersionManagerImpl
     *
     * @param id
     * @return
     * @throws RepositoryException
     */
    public InternalVersionItem getItem(String id) throws RepositoryException;

    /**
     * Returns the references that exist to this version item
     * todo: move probably to VersionManagerImpl
     *
     * @param item
     * @return a collection of property ids
     */
    public List getItemReferences(InternalVersionItem item);

    /**
     * Informs this version manager that the references to one of its
     * items has changed.
     * todo: move probably to VersionManagerImpl
     *
     * @param item the version item that is referenced
     * @param references the collection of PropertyIds that references the item
     */
    public void setItemReferences(InternalVersionItem item, List references);

    /**
     * Close this version manager. After having closed a persistence
     * manager, further operations on this object are treated as illegal
     * and throw
     *
     * @throws Exception if an error occurs
     */
    public void close() throws Exception;
}
