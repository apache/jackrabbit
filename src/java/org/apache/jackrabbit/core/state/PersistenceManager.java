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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.QName;

import java.util.Iterator;

/**
 * <code>PersistenceManager</code> ...
 */
public interface PersistenceManager {

    /**
     * Initialize this persistent manager.
     * @param context persistence manager context
     * @throws Exception if an error occurs
     */
    public void init(PMContext context) throws Exception;

    /**
     * Close this persistence manager. After having closed a persistence
     * manager, further operations on this object are treated as illegal
     * and throw
     * @throws Exception if an error occurs
     */
    public void close() throws Exception;

    /**
     * Create a new node state instance.
     * @param uuid          the UUID of the this node
     * @param nodeTypeName  node type of this node
     * @param parentUUID    the UUID of the parent node
     * @return node state instance.
     */
    public NodeState createNew(String uuid, QName nodeTypeName,
                               String parentUUID);

    /**
     * Create a new property state instance.
     * @param name          name of the property
     * @param parentUUID    the uuid of the parent node
     * @return property state instance.
     */
    public PropertyState createNew(QName name, String parentUUID);

    /**
     * Load the persistent members of a node state.
     * @param uuid uuid of the node to load
     * @return loaded node state
     * @throws NoSuchItemStateException if the item does not exist
     * @throws ItemStateException if an error occurs
     */
    public NodeState load(String uuid)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * Load the persistent members of a property state.
     * @param name name of the property
     * @param parentUUID the uuid of the parent node
     * @return loaded property state
     * @throws NoSuchItemStateException if the item does not exist
     * @throws ItemStateException if an error occurs
     */
    public PropertyState load(QName name, String parentUUID)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * Load the persistent members of a node references object.
     * @param targetId node target id
     * @return loaded references object
     * @throws NoSuchItemStateException if the item does not exist
     * @throws ItemStateException if an error occurs
     */
    public NodeReferences load(NodeId targetId)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * Determines if there's <code>ItemState</code> data for the given item.
     * @param id
     * @return
     * @throws ItemStateException
     */
    public boolean exists(ItemId id) throws ItemStateException;

    /**
     * Determines if there's <code>NodeReferences</code> data for
     * the given target id.
     *
     * @param targetId
     * @return
     * @throws ItemStateException
     */
    public boolean referencesExist(NodeId targetId) throws ItemStateException;

    /**
     * Save all modified states and node references, atomically.
     * @param states states that have been modified
     * @param refsIterator refs to store
     * @throws ItemStateException if an error occurs
     */
    public void store(Iterator states, Iterator refsIterator)
            throws ItemStateException;

}
