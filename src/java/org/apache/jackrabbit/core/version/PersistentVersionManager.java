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

import org.apache.jackrabbit.core.*;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.*;

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
    public InternalVersionHistory createVersionHistory(NodeImpl node)
            throws RepositoryException;

    /**
     * returns the internal version history for the id
     *
     * @param histId the id of the history
     * @return
     * @throws RepositoryException
     */
    public InternalVersionHistory getVersionHistory(String histId)
            throws RepositoryException;

    /**
     * Checks if the versionhistory for the given id exists
     *
     * @param histId
     * @return
     */
    public boolean hasVersionHistory(String histId);

    /**
     * returns an iterator over the external ids of the version histories
     *
     * @return
     * @throws RepositoryException
     */
    public Iterator getVersionHistoryIds() throws RepositoryException;

    /**
     * returns the number of version histories
     *
     * @return
     * @throws RepositoryException
     */
    public int getNumVersionHistories() throws RepositoryException;

    /**
     * returns the internal version for the id
     *
     * @param versionId
     * @return
     * @throws RepositoryException
     */
    public InternalVersion getVersion(String histId, String versionId)
            throws RepositoryException;

    /**
     * returns the version with the given id
     *
     * @param versionId
     * @return
     * @throws RepositoryException
     */
    public InternalVersion getVersion(String versionId)
            throws RepositoryException;

    /**
     * Checks if the version with the given id exists
     *
     * @param versionId
     * @return
     */
    public boolean hasVersion(String versionId);

    /**
     * checks, if the item with the given external id exists
     * @param externalId
     * @return
     */
    public boolean hasItem(String externalId);

    /**
     * returns the item referred by the external id
     * @param externalId
     * @return
     * @throws RepositoryException
     */
    public InternalVersionItem getItemByExternal(String externalId)
            throws RepositoryException;

    /**
     * returns the item referred by the internal id
     * @param internalId
     * @return
     * @throws RepositoryException
     */
    public InternalVersionItem getItemByInternal(String internalId)
            throws RepositoryException;

    /**
     * Checks in a node
     *
     * @param node
     * @return
     * @throws RepositoryException
     * @see Node#checkin()
     */
    public InternalVersion checkin(NodeImpl node) throws RepositoryException;

}
