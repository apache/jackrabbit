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
package org.apache.jackrabbit.core.persistence;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.ChangeLog;

/**
 * Persistence manager interface. Persistence managers are
 * internal Jackrabbit components that handle the persistent
 * storage of content nodes and properties. A persistence
 * manager knows how to retrieve the persistent states of
 * content items and how to atomically save a set of changes
 * to the persistent state.
 * <p>
 * Each workspace of a Jackrabbit content repository uses separate
 * persistence manager to store the content in that workspace. Also
 * the Jackrabbit version handler uses a separate persistence manager.
 * The persistence managers in use are configured in the Jackrabbit
 * XML configuration files. The configured persistence managers are
 * instantiated and initialized using the JavaBeans conventions.
 *
 * <h2>Persistence manager life cycle</h2>
 * <p>
 * The life cycle of a persistence manager instance contains four phases:
 * <ol>
 *   <li>Instantiation, where the instance is created and possible
 *       configuration properties are set using the JavaBean conventions.
 *       During this phase the persistence manager should not attempt to
 *       reference any external resources.
 *   <li>Initialization, where the {@link #init(PMContext) init} method
 *       is invoked to bind the persistence manager with a given
 *       {@link PMContext context}.
 *   <li>Normal usage, where the various create, load, exists, and store
 *       methods of the persistence manager are used to manage the
 *       persistent content items.
 *   <li>Closing, where the {@link #close() close} method is invoked
 *       to close the persistence manager and release all acquired
 *       resources.
 * </ol>
 *
 * <h2>Concurrency</h2>
 * <p>
 * A persistence manager instance should be thread-safe and guarantee that
 * the {@link #store(ChangeLog)} method calls are atomic. Any load() or
 * exists() calls started after a store() call has returned must access
 * the updated content. The client accessing a persistence manager must
 * guarantee that no two concurrent {@link #store(ChangeLog)} calls will
 * modify the same items.
 */
public interface PersistenceManager {

    /**
     * Initializes the persistence manager. The persistence manager is
     * permanently bound to the given context, and any required external
     * resources are acquired.
     * <p>
     * An appropriate exception is thrown if the persistence manager
     * initialization fails for whatever reason. In this case the
     * state of the persistence manager is undefined and the instance
     * should be discarded.
     *
     * @param context persistence manager context
     * @throws Exception if the persistence manager initialization failed
     */
    void init(PMContext context) throws Exception;

    /**
     * Closes the persistence manager. The consistency of the persistent
     * storage is guaranteed and all acquired resources are released.
     * It is an error to invoke any methods on a closed persistence manager,
     * and implementations are free to enforce this constraint by throwing
     * IllegalStateExceptions in such cases.
     * <p>
     * An appropriate exception is thrown if the persistence manager
     * could not be closed properly. In this case the state of the
     * persistence manager is undefined and the instance should be
     * discarded.
     *
     * @throws Exception if the persistence manager failed to close properly
     */
    void close() throws Exception;

    /**
     * Creates a new node state instance with the given id.
     *
     * @param id node id
     * @return node state instance
     */
    NodeState createNew(NodeId id);

    /**
     * Creates a new property state instance with the given id.
     *
     * @param id property id
     * @return property state instance
     */
    PropertyState createNew(PropertyId id);

    /**
     * Load the persistent members of a node state.
     *
     * @param id node id
     * @return loaded node state
     * @throws NoSuchItemStateException if the node state does not exist
     * @throws ItemStateException if another error occurs
     */
    NodeState load(NodeId id)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * Load the persistent members of a property state.
     *
     * @param id property id
     * @return loaded property state
     * @throws NoSuchItemStateException if the property state does not exist
     * @throws ItemStateException if another error occurs
     */
    PropertyState load(PropertyId id)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * Load the persisted references to the node with the given identifier.
     *
     * @param id reference target node id
     * @throws NoSuchItemStateException if the target node does not exist
     * @throws ItemStateException if another error occurs
     */
    NodeReferences loadReferencesTo(NodeId id)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * Checks whether the identified node exists.
     *
     * @param id node id
     * @return <code>true</code> if the node exists,
     *         <code>false</code> otherwise
     * @throws ItemStateException on persistence manager errors
     */
    boolean exists(NodeId id) throws ItemStateException;

    /**
     * Checks whether the identified property exists.
     *
     * @param id property id
     * @return <code>true</code> if the property exists,
     *         <code>false</code> otherwise
     * @throws ItemStateException on persistence manager errors
     */
    boolean exists(PropertyId id) throws ItemStateException;

    /**
     * Checks whether references of the identified target node exist.
     *
     * @param targetId target node id
     * @return <code>true</code> if the references exist,
     *         <code>false</code> otherwise
     * @throws ItemStateException on persistence manager errors
     */
    boolean existsReferencesTo(NodeId targetId) throws ItemStateException;

    /**
     * Atomically saves the given set of changes.
     *
     * @param changeLog change log containing states that were changed
     * @throws ItemStateException if the changes could not be saved
     */
    void store(ChangeLog changeLog) throws ItemStateException;

    /**
     * Perform a consistency check of the data. An example are non-existent
     * nodes referenced in a child node entry. The existence of this feature and
     * the scope of the implementation can vary in different PersistenceManager
     * implementations.
     *
     * @param uuids
     *            list of UUIDs of nodes to be checked. if null, all nodes will
     *            be checked
     * @param recursive
     *            if true, the tree(s) below the given node(s) will be traversed
     *            and checked as well
     * @param fix
     *            if true, any problems found that can be repaired will be
     *            repaired. if false, no data will be modified, instead all
     *            inconsistencies will only get logged
     */
    void checkConsistency(String[] uuids, boolean recursive, boolean fix);

}
