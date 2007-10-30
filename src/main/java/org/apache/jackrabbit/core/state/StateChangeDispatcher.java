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
package org.apache.jackrabbit.core.state;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.spi.Name;

import java.util.Collection;
import java.util.Iterator;

/**
 * Component that holds references to listeners interested in changes to item
 * states and dispatches notifications.
 */
public class StateChangeDispatcher {

    /**
     * Simple item state listeners
     */
    private final Collection listeners = new CopyOnWriteArrayList();

    /**
     * Node state listeners
     */
    private final transient Collection nsListeners = new CopyOnWriteArrayList();

    /**
     * Add an <code>ItemStateListener</code>.
     * @param listener the new listener to be informed on modifications
     */
    public void addListener(ItemStateListener listener) {
        assert (!listeners.contains(listener));
        listeners.add(listener);

        if (listener instanceof NodeStateListener) {
            assert (!nsListeners.contains(listener));
            nsListeners.add(listener);
        }
    }

    /**
     * Remove an <code>ItemStateListener</code>
     * @param listener an existing listener
     */
    public void removeListener(ItemStateListener listener) {
        if (listener instanceof NodeStateListener) {
            nsListeners.remove(listener);
        }
        listeners.remove(listener);
    }

    /**
     * Notify listeners about changes to some state.
     * @param created created state.
     */
    public void notifyStateCreated(ItemState created) {
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            ((ItemStateListener) iter.next()).stateCreated(created);
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param modified modified state.
     */
    public void notifyStateModified(ItemState modified) {
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            ((ItemStateListener) iter.next()).stateModified(modified);
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param destroyed destroyed state.
     */
    public void notifyStateDestroyed(ItemState destroyed) {
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            ((ItemStateListener) iter.next()).stateDestroyed(destroyed);
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param discarded discarded state.
     */
    public void notifyStateDiscarded(ItemState discarded) {
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            ((ItemStateListener) iter.next()).stateDiscarded(discarded);
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param state node state that changed
     * @param name  name of node that was added
     * @param index index of new node
     * @param id    id of new node
     */
    public void notifyNodeAdded(NodeState state, Name name, int index, NodeId id) {
        Iterator iter = nsListeners.iterator();
        while (iter.hasNext()) {
            ((NodeStateListener) iter.next()).nodeAdded(state, name, index, id);
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param state node state that changed
     */
    public void notifyNodesReplaced(NodeState state) {
        Iterator iter = nsListeners.iterator();
        while (iter.hasNext()) {
            ((NodeStateListener) iter.next()).nodesReplaced(state);
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param state node state that changed
     */
    public void notifyNodeModified(NodeState state) {
        Iterator iter = nsListeners.iterator();
        while (iter.hasNext()) {
            ((NodeStateListener) iter.next()).nodeModified(state);
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param state node state that changed
     * @param name  name of node that was added
     * @param index index of new node
     * @param id    id of new node
     */
    public void notifyNodeRemoved(NodeState state, Name name, int index, NodeId id) {
        Iterator iter = nsListeners.iterator();
        while (iter.hasNext()) {
            ((NodeStateListener) iter.next()).nodeRemoved(state, name, index, id);
        }
    }
}