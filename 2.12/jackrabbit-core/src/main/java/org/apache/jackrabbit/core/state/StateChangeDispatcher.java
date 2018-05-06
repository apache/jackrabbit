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

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.Name;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Component that holds references to listeners interested in changes to item
 * states and dispatches notifications.
 */
public class StateChangeDispatcher {

    /**
     * Simple item state listeners.
     * A copy on write array list is used so that no synchronization is required.
     */
    private final Collection<WeakReference<ItemStateListener>> listeners =
        new CopyOnWriteArrayList<WeakReference<ItemStateListener>>();

    /**
     * Node state listeners
     * A copy on write array list is used so that no synchronization is required.
     */
    private final Collection<WeakReference<NodeStateListener>> nsListeners =
        new CopyOnWriteArrayList<WeakReference<NodeStateListener>>();

    /**
     * Add an <code>ItemStateListener</code>.
     * @param listener the new listener to be informed on modifications
     */
    public void addListener(ItemStateListener listener) {
        assert getReference(listeners, listener) == null;
        listeners.add(new WeakReference<ItemStateListener>(listener));

        if (listener instanceof NodeStateListener) {
            NodeStateListener nsListener = (NodeStateListener) listener;
            assert getReference(nsListeners, nsListener) == null;
            nsListeners.add(new WeakReference<NodeStateListener>(nsListener));
        }
    }

    private <T> Reference<T> getReference(Collection< ? extends Reference<T>> coll, ItemStateListener listener) {
        for (Reference<T> ref : coll) {
            Object o = ref.get();
            if (o == listener) {
                return ref;
            } else if (o == null) {
                // clean up unreferenced objects
                coll.remove(ref);
            }
        }
        return null;
    }

    /**
     * Remove an <code>ItemStateListener</code>
     * @param listener an existing listener
     */
    public void removeListener(ItemStateListener listener) {
        if (listener instanceof NodeStateListener) {
            nsListeners.remove(getReference(nsListeners, listener));
        }
        listeners.remove(getReference(listeners, listener));
    }

    /**
     * Notify listeners about changes to some state.
     * @param created created state.
     */
    public void notifyStateCreated(ItemState created) {
        for (Reference<ItemStateListener> ref : listeners) {
            ItemStateListener l = ref.get();
            if (l != null) {
                l.stateCreated(created);
            }
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param modified modified state.
     */
    public void notifyStateModified(ItemState modified) {
        for (Reference<ItemStateListener> ref : listeners) {
            ItemStateListener l = ref.get();
            if (l != null) {
                l.stateModified(modified);
            }
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param destroyed destroyed state.
     */
    public void notifyStateDestroyed(ItemState destroyed) {
        for (Reference<ItemStateListener> ref : listeners) {
            ItemStateListener l = ref.get();
            if (l != null) {
                l.stateDestroyed(destroyed);
            }
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param discarded discarded state.
     */
    public void notifyStateDiscarded(ItemState discarded) {
        for (Reference<ItemStateListener> ref : listeners) {
            ItemStateListener l = ref.get();
            if (l != null) {
                l.stateDiscarded(discarded);
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
    public void notifyNodeAdded(NodeState state, Name name, int index, NodeId id) {
        for (Reference<NodeStateListener> ref : nsListeners) {
            NodeStateListener n = ref.get();
            if (n != null) {
                n.nodeAdded(state, name, index, id);
            }
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param state node state that changed
     */
    public void notifyNodesReplaced(NodeState state) {
        for (Reference<NodeStateListener> ref : nsListeners) {
            NodeStateListener n = ref.get();
            if (n != null) {
                n.nodesReplaced(state);
            }
        }
    }

    /**
     * Notify listeners about changes to some state.
     * @param state node state that changed
     */
    public void notifyNodeModified(NodeState state) {
        for (Reference<NodeStateListener> ref : nsListeners) {
            NodeStateListener n = ref.get();
            if (n != null) {
                n.nodeModified(state);
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
    public void notifyNodeRemoved(NodeState state, Name name, int index, NodeId id) {
        for (Reference<NodeStateListener> ref : nsListeners) {
            NodeStateListener n = ref.get();
            if (n != null) {
                n.nodeRemoved(state, name, index, id);
            }
        }
    }

}
