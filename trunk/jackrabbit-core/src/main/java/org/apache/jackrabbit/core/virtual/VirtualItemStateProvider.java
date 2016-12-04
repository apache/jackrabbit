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
package org.apache.jackrabbit.core.virtual;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.spi.Name;

/**
 * This Interface defines a virtual item state provider.
 */
public interface VirtualItemStateProvider extends ItemStateManager {

    /**
     * Checks if the id refers to the root of a virtual tree.
     *
     * @param id
     * @return <code>true</code> if it is the root
     */
    boolean isVirtualRoot(ItemId id);

    /**
     * Returns the id of the root node of the virtual tree.
     *
     * @return the id of the root node of the virtual tree.
     * @deprecated use {@link #getVirtualRootIds()} instead.
     */
    NodeId getVirtualRootId();

    /**
     * Returns the ids of the root nodes of the virtual tree.
     *
     * @return the ids of the roots node of the virtual tree.
     */
    NodeId[] getVirtualRootIds();

    /**
     * Creats a new virtual property state
     *
     * @param parent
     * @param name
     * @param type
     * @param multiValued
     * @return
     * @throws RepositoryException
     */
    VirtualPropertyState createPropertyState(VirtualNodeState parent,
                                             Name name, int type,
                                             boolean multiValued)
            throws RepositoryException;

    /**
     * Creates a new virtual node state
     *
     * @param parent
     * @param name
     * @param id (must not be null)
     * @param nodeTypeName
     * @return
     * @throws RepositoryException
     */
    VirtualNodeState createNodeState(VirtualNodeState parent, Name name,
                                     NodeId id, Name nodeTypeName)
        throws RepositoryException;

    /**
     * Informs this provider that the node references to some of its states
     * have changed.
     *
     * @param references collection of {@link NodeReferences} instances
     * @return <code>true</code> if the reference target is one of its items.
     */
    boolean setNodeReferences(ChangeLog references);


    /**
     * Add an <code>ItemStateListener</code>
     * @param listener the new listener to be informed on modifications
     */
    void addListener(ItemStateListener listener);

    /**
     * Remove an <code>ItemStateListener</code>
     *
     * @param listener an existing listener
     */
    void removeListener(ItemStateListener listener);

}
