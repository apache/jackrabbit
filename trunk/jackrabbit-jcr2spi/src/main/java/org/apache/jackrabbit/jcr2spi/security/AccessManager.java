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
package org.apache.jackrabbit.jcr2spi.security;

import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.apache.jackrabbit.jcr2spi.state.NodeState;

import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;

/**
 * The <code>AccessManager</code> can be queried to determines whether permission
 * is granted to perform a specific action on a specific item.
 */
public interface AccessManager {

    /**
     * predefined action constants
     */
    public String READ_ACTION = javax.jcr.Session.ACTION_READ;
    public String REMOVE_ACTION = javax.jcr.Session.ACTION_REMOVE;
    public String ADD_NODE_ACTION = javax.jcr.Session.ACTION_ADD_NODE;
    public String SET_PROPERTY_ACTION = javax.jcr.Session.ACTION_SET_PROPERTY;

    public String[] READ = new String[] {READ_ACTION};
    public String[] REMOVE = new String[] {REMOVE_ACTION};

    /**
     * Determines whether the specified <code>permissions</code> are granted
     * on the item with the specified path.
     *
     * @param parentState The node state of the next existing ancestor.
     * @param relPath The relative path pointing to the non-existing target item.
     * @param actions An array of actions that need to be checked.
     * @return <code>true</code> if the actions are granted; otherwise <code>false</code>
     * @throws ItemNotFoundException if the target item does not exist
     * @throws RepositoryException if another error occurs
     */
    boolean isGranted(NodeState parentState, Path relPath, String[] actions) throws ItemNotFoundException, RepositoryException;

    /**
     * Determines whether the specified <code>permissions</code> are granted
     * on the item with the specified path.
     *
     * @param itemState
     * @param actions An array of actions that need to be checked.
     * @return <code>true</code> if the actions are granted; otherwise <code>false</code>
     * @throws ItemNotFoundException if the target item does not exist
     * @throws RepositoryException if another error occurs
     */
     boolean isGranted(ItemState itemState, String[] actions) throws ItemNotFoundException, RepositoryException;


    /**
     * Returns true if the existing item with the given <code>ItemId</code> can
     * be read.
     *
     * @param itemState
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    boolean canRead(ItemState itemState) throws ItemNotFoundException, RepositoryException;

    /**
     * Returns true if the existing item state can be removed.
     *
     * @param itemState
     * @return
     * @throws ItemNotFoundException
     * @throws RepositoryException
     */
    boolean canRemove(ItemState itemState) throws ItemNotFoundException, RepositoryException;

    /**
     * Determines whether the subject of the current context is granted access
     * to the given workspace.
     *
     * @param workspaceName name of workspace
     * @return <code>true</code> if the subject of the current context is
     * granted access to the given workspace; otherwise <code>false</code>.
     * @throws NoSuchWorkspaceException if a workspace with the given name does not exist.
     * @throws RepositoryException if another error occurs
     */
    boolean canAccess(String workspaceName) throws NoSuchWorkspaceException, RepositoryException;
}
