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

import org.apache.jackrabbit.util.WeakIdentityCollection;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.core.NodeId;

import java.util.Collection;

/**
 * Component that holds weak references to listeners interested in changes to item states and dispatches notifications.
 */
public class StateChangeDispatcher {

    /**
     * Simple item state listeners (weak references)
     */
    private final transient Collection listeners = new WeakIdentityCollection(5);

    /**
     * Node state listeners (weak references)
     */
    private final transient Collection nsListeners = new WeakIdentityCollection(5);

    /**
     * Add an <code>ItemStateListener</code>.
     * @param listener the new listener to be informed on modifications
     */
    public void addListener(ItemStateListener listener) {
        synchronized (listeners) {
            assert (!listeners.contains(listener));
            listeners.add(listener);
        }
        if (listener instanceof NodeStateListener) {
            synchronized (nsListeners) {
                assert (!nsListeners.contains(listener));
                nsListeners.add(listener);
            }
        }
    }

    /**
     * Remove an <code>ItemStateListener</code>
     * @param listener an existing listener
     */
    public void removeListener(ItemStateListener listener) {
        if (listener instanceof NodeStateListener) {
            synchronized (nsListeners) {
                nsListeners.remove(listener);
            }
        }
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param created created state.
     */
    public void notifyStateCreated(ItemState created) {
        ItemStateListener[] la;
        synchronized (listeners) {
            la = (ItemStateListener[]) listeners.toArray(new ItemStateListener[listeners.size()]);
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].stateCreated(created);
            }
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param modified modified state.
     */
    public void notifyStateModified(ItemState modified) {
        ItemStateListener[] la;
        synchronized (listeners) {
            la = (ItemStateListener[]) listeners.toArray(new ItemStateListener[listeners.size()]);
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].stateModified(modified);
            }
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param destroyed destroyed state.
     */
    public void notifyStateDestroyed(ItemState destroyed) {
        ItemStateListener[] la;
        synchronized (listeners) {
            la = (ItemStateListener[]) listeners.toArray(new ItemStateListener[listeners.size()]);
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].stateDestroyed(destroyed);
            }
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param discarded discarded state.
     */
    public void notifyStateDiscarded(ItemState discarded) {
        ItemStateListener[] la;
        synchronized (listeners) {
            la = (ItemStateListener[]) listeners.toArray(new ItemStateListener[listeners.size()]);
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].stateDiscarded(discarded);
            }
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param state node state that changed
     * @param name  name of node that was added
     * @param index index of new node
     * @param id    id of new node
     */
    public void notifyNodeAdded(NodeState state, QName name, int index, NodeId id) {
        // small optimization as there are only a few clients interested in node state modifications
        if (!nsListeners.isEmpty()) {
            NodeStateListener[] la;
            synchronized (nsListeners) {
                la = (NodeStateListener[]) nsListeners.toArray(new NodeStateListener[nsListeners.size()]);
            }
            for (int i = 0; i < la.length; i++) {
                if (la[i] != null) {
                    la[i].nodeAdded(state, name, index, id);
                }
            }
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param state node state that changed
     */
    public void notifyNodesReplaced(NodeState state) {
        // small optimization as there are only a few clients interested in node state modifications
        if (!nsListeners.isEmpty()) {
            NodeStateListener[] la;
            synchronized (nsListeners) {
                la = (NodeStateListener[]) nsListeners.toArray(new NodeStateListener[nsListeners.size()]);
            }
            for (int i = 0; i < la.length; i++) {
                if (la[i] != null) {
                    la[i].nodesReplaced(state);
                }
            }
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param state node state that changed
     */
    public void notifyNodeModified(NodeState state) {
        // small optimization as there are only a few clients interested in node state modifications
        if (!nsListeners.isEmpty()) {
            NodeStateListener[] la;
            synchronized (nsListeners) {
                la = (NodeStateListener[]) nsListeners.toArray(new NodeStateListener[nsListeners.size()]);
            }
            for (int i = 0; i < la.length; i++) {
                if (la[i] != null) {
                    la[i].nodeModified(state);
                }
            }
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param state node state that changed
     * @param name  name of node that was added
     * @param index index of new node
     * @param id    id of new node
     */
    public void notifyNodeRemoved(NodeState state, QName name, int index, NodeId id) {
        // small optimization as there are only a few clients interested in node state modifications
        if (!nsListeners.isEmpty()) {
            NodeStateListener[] la;
            synchronized (nsListeners) {
                la = (NodeStateListener[]) nsListeners.toArray(new NodeStateListener[nsListeners.size()]);
            }
            for (int i = 0; i < la.length; i++) {
                if (la[i] != null) {
                    la[i].nodeRemoved(state, name, index, id);
                }
            }
        }
    }
}