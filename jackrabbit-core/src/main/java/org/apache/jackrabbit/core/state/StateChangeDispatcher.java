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

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;

/**
 * Component that holds references to listeners interested in changes to item
 * states and dispatches notifications.
 */
public class StateChangeDispatcher {

    /**
     * Simple item state listeners.
     * A copy on write array list is used so that no synchronization is required.
     */
    private final Collection listeners = new CopyOnWriteArrayList();

    /**
     * Node state listeners
     * A copy on write array list is used so that no synchronization is required.
     */
    private final transient Collection nsListeners = new CopyOnWriteArrayList();

    /**
     * Add an <code>ItemStateListener</code>.
     * @param listener the new listener to be informed on modifications
     */
    public void addListener(ItemStateListener listener) {
        assert getReference(listeners, listener) == null;
        listeners.add(new WeakReference(listener));

        if (listener instanceof NodeStateListener) {
            assert getReference(nsListeners, listener) == null;
            nsListeners.add(new WeakReference(listener));
        }
    }
    
    private Reference getReference(Collection coll, ItemStateListener listener) {
        Iterator iter = coll.iterator();
        while (iter.hasNext()) {
            Reference ref = (Reference) iter.next();
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
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            Reference ref = (Reference) iter.next();
            ItemStateListener l = (ItemStateListener) ref.get();
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
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            Reference ref = (Reference) iter.next();
            ItemStateListener l = (ItemStateListener) ref.get();
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
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            Reference ref = (Reference) iter.next();
            ItemStateListener l = (ItemStateListener) ref.get();
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
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            Reference ref = (Reference) iter.next();
            ItemStateListener l = (ItemStateListener) ref.get();
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
        Iterator iter = nsListeners.iterator();
        while (iter.hasNext()) {
            Reference ref = (Reference) iter.next();
            NodeStateListener n = (NodeStateListener) ref.get();
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
        Iterator iter = nsListeners.iterator();
        while (iter.hasNext()) {
            Reference ref = (Reference) iter.next();
            NodeStateListener n = (NodeStateListener) ref.get();
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
        Iterator iter = nsListeners.iterator();
        while (iter.hasNext()) {
            Reference ref = (Reference) iter.next();
            NodeStateListener n = (NodeStateListener) ref.get();
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
        Iterator iter = nsListeners.iterator();
        while (iter.hasNext()) {
            Reference ref = (Reference) iter.next();
            NodeStateListener n = (NodeStateListener) ref.get();
            if (n != null) {
                n.nodeRemoved(state, name, index, id);
            }               
        }
    }

}
