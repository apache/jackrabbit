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

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;

import java.util.Iterator;

/**
 * Implementation <code>PersistenceManager</code> that handles some
 * concepts.
 */
public abstract class AbstractPersistenceManager implements PersistenceManager {

    /**
     * {@inheritDoc}
     */
    public NodeState createNew(NodeId id) {
        return new NodeState(id, null, null, NodeState.STATUS_NEW, false);
    }

    /**
     * {@inheritDoc}
     */
    public PropertyState createNew(PropertyId id) {
        return new PropertyState(id, PropertyState.STATUS_NEW, false);
    }

    /**
     * Right now, this iterates over all items in the changelog and
     * calls the individual methods that handle single item states
     * or node references objects. Properly implemented, this method
     * should ensure that changes are either written completely to
     * the underlying persistence layer, or not at all.
     *
     * {@inheritDoc}
     */
    public synchronized void store(ChangeLog changeLog) throws ItemStateException {
        Iterator iter = changeLog.deletedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            if (state.isNode()) {
                destroy((NodeState) state);
            } else {
                destroy((PropertyState) state);
            }
        }
        iter = changeLog.addedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            if (state.isNode()) {
                store((NodeState) state);
            } else {
                store((PropertyState) state);
            }
        }
        iter = changeLog.modifiedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            if (state.isNode()) {
                store((NodeState) state);
            } else {
                store((PropertyState) state);
            }
        }
        iter = changeLog.modifiedRefs();
        while (iter.hasNext()) {
            NodeReferences refs = (NodeReferences) iter.next();
            if (refs.hasReferences()) {
                store(refs);
            } else {
                if (exists(refs.getId())) {
                    destroy(refs);
                }
            }
        }
    }

    /**
     * This implementation does nothing.
     *
     * {@inheritDoc}
     */
    public void checkConsistency(String[] uuids, boolean recursive, boolean fix) {
    }

    /**
     * Store a node state. Subclass responsibility.
     *
     * @param state node state to store
     * @throws ItemStateException if an error occurs
     */
    protected abstract void store(NodeState state) throws ItemStateException;

    /**
     * Store a property state. Subclass responsibility.
     *
     * @param state property state to store
     * @throws ItemStateException if an error occurs
     */
    protected abstract void store(PropertyState state) throws ItemStateException;

    /**
     * Store a references object. Subclass responsibility.
     *
     * @param refs references object to store
     * @throws ItemStateException if an error occurs
     */
    protected abstract void store(NodeReferences refs) throws ItemStateException;

    /**
     * Destroy a node state. Subclass responsibility.
     *
     * @param state node state to destroy
     * @throws ItemStateException if an error occurs
     */
    protected abstract void destroy(NodeState state) throws ItemStateException;

    /**
     * Destroy a property state. Subclass responsibility.
     *
     * @param state property state to destroy
     * @throws ItemStateException if an error occurs
     */
    protected abstract void destroy(PropertyState state) throws ItemStateException;

    /**
     * Destroy a node references object. Subclass responsibility.
     *
     * @param refs node references object to destroy
     * @throws ItemStateException if an error occurs
     */
    protected abstract void destroy(NodeReferences refs)
            throws ItemStateException;
}
