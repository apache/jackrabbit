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

import org.apache.jackrabbit.core.QName;

import java.util.Iterator;

/**
 * Implementation <code>PersistenceManager</code> that handles some
 * concepts.
 */
public abstract class AbstractPersistenceManager implements PersistenceManager {

    /**
     * @see PersistenceManager#createNew
     */
    public NodeState createNew(String uuid, QName nodeTypeName,
                               String parentUUID) {
        return new NodeState(uuid, nodeTypeName, parentUUID,
                NodeState.STATUS_NEW, false);
    }

    /**
     * @see PersistenceManager#createNew
     */
    public PropertyState createNew(QName name, String parentUUID) {
        return new PropertyState(name, parentUUID,
                PropertyState.STATUS_NEW, false);
    }

    /**
     * Store modified states and node references, atomically.
     *
     * @param states       states that have been modified
     * @param refsIterator refs to store
     * @throws ItemStateException if an error occurs
     */
    public void store(Iterator states, Iterator refsIterator)
            throws ItemStateException {
        while (states.hasNext()) {
            ItemState state = (ItemState) states.next();
            if (state.isNode()) {
                NodeState ns = (NodeState) state;
                switch (state.getStatus()) {
                    case NodeState.STATUS_EXISTING_REMOVED:
                        destroy(ns);
                        break;
                    default:
                        store(ns);
                        break;
                }
            } else {
                PropertyState ps = (PropertyState) state;
                switch (state.getStatus()) {
                    case PropertyState.STATUS_EXISTING_REMOVED:
                        destroy(ps);
                        break;
                    default:
                        store(ps);
                        break;
                }
            }
        }

        while (refsIterator.hasNext()) {
            NodeReferences refs = (NodeReferences) refsIterator.next();
            switch (refs.getStatus()) {
                case NodeReferences.STATUS_DESTROYED:
                    destroy(refs);
                    break;
                default:
                    store(refs);
                    break;
            }
        }
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
