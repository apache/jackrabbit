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
import org.apache.jackrabbit.core.state.UpdatableItemStateManager;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Iterator;
import java.util.List;

/**
 * This interface defines the access to the persistence layer of the
 * versioning. The way how the versions are stored may totaly differ from
 * the way they are exposed to the client.
 */
public interface PersistentVersionManager {

    /**
     * Creates a new Version History.
     *
     * @param node the node for which the version history is to be initialized
     * @return the newly created version history.
     * @throws RepositoryException
     */
    InternalVersionHistory createVersionHistory(NodeImpl node)
            throws RepositoryException;

    /**
     * returns the internal version history for the id
     *
     * @param histId the id of the history
     * @return
     * @throws RepositoryException
     */
    InternalVersionHistory getVersionHistory(String histId)
            throws RepositoryException;

    /**
     * Checks if the versionhistory for the given id exists
     *
     * @param histId
     * @return
     */
    boolean hasVersionHistory(String histId);

    /**
     * returns an iterator over the external ids of the version histories
     *
     * @return
     * @throws RepositoryException
     */
    Iterator getVersionHistoryIds() throws RepositoryException;

    /**
     * returns the number of version histories
     *
     * @return
     * @throws RepositoryException
     */
    int getNumVersionHistories() throws RepositoryException;

    /**
     * returns the internal version for the id
     *
     * @param versionId
     * @return
     * @throws RepositoryException
     */
    InternalVersion getVersion(String histId, String versionId)
            throws RepositoryException;

    /**
     * returns the version with the given id
     *
     * @param versionId
     * @return
     * @throws RepositoryException
     */
    InternalVersion getVersion(String versionId)
            throws RepositoryException;

    /**
     * Checks if the version with the given id exists
     *
     * @param versionId
     * @return
     */
    boolean hasVersion(String versionId);

    /**
     * checks, if the item with the given external id exists
     *
     * @param id
     * @return
     */
    boolean hasItem(String id);

    /**
     * returns the item referred by the id
     *
     * @param id
     * @return
     * @throws RepositoryException
     */
    InternalVersionItem getItem(String id)
            throws RepositoryException;

    /**
     * Checks in a node
     *
     * @param node
     * @return
     * @throws RepositoryException
     * @see Node#checkin()
     */
    InternalVersion checkin(NodeImpl node) throws RepositoryException;

    /**
     * Return the item state manager of this version manager
     * @return item state manager
     */
    UpdatableItemStateManager getItemStateMgr();

    /**
     * Returns the references that exist to this version item
     *
     * @param item
     * @return a collection of property ids
     */
    List getItemReferences(InternalVersionItem item);

    /**
     * Informs this version manager that the references to one of its
     * items has changed.
     *
     * @param item the version item that is referenced
     * @param references the collection of PropertyIds that references the item
     */
    void setItemReferences(InternalVersionItem item, List references);

    /**
     * Close this persistence version manager. After having closed a persistence
     * manager, further operations on this object are treated as illegal
     * and throw
     * @throws Exception if an error occurs
     */
    void close() throws Exception;
}
