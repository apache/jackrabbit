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

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;

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
     * @param id node id
     * @return node state instance.
     */
    public NodeState createNew(NodeId id);

    /**
     * Create a new property state instance.
     * @param id property id
     * @return property state instance.
     */
    public PropertyState createNew(PropertyId id);

    /**
     * Load the persistent members of a node state.
     * @param id node id
     * @return loaded node state
     * @throws NoSuchItemStateException if the item does not exist
     * @throws ItemStateException if an error occurs
     */
    public NodeState load(NodeId id)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * Load the persistent members of a property state.
     * @param id property id
     * @return loaded property state
     * @throws NoSuchItemStateException if the item does not exist
     * @throws ItemStateException if an error occurs
     */
    public PropertyState load(PropertyId id)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * Load the persistent members of a node references object.
     * @param id node target id
     * @throws NoSuchItemStateException if the item does not exist
     * @throws ItemStateException if an error occurs
     */
    public NodeReferences load(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * Determines if there's <code>NodeState</code> data
     * for the given item.
     * @param id
     * @return
     * @throws ItemStateException
     */
    public boolean exists(NodeId id) throws ItemStateException;

    /**
     * Determines if there's <code>PropertyState</code> data
     * for the given item.
     * @param id
     * @return
     * @throws ItemStateException
     */
    public boolean exists(PropertyId id) throws ItemStateException;

    /**
     * Determines if there's <code>NodeReferences</code> data for
     * the given target id.
     *
     * @param targetId
     * @return
     * @throws ItemStateException
     */
    public boolean exists(NodeReferencesId targetId) throws ItemStateException;

    /**
     * Save all states and node references, atomically.
     * @param changeLog change log containing states that were changed
     * @throws ItemStateException if an error occurs
     */
    public void store(ChangeLog changeLog) throws ItemStateException;
}
