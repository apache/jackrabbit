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

import org.apache.commons.collections.ReferenceMap;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.QName;
import org.apache.log4j.Logger;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Local <code>ItemStateManager</code> that isolates changes to
 * persistent states from other clients.
 */
public class LocalItemStateManager extends ItemStateCache
        implements ItemStateManager, ItemStateListener, TransactionListener {

    /**
     * Logger instance
     */
    private static Logger log = Logger.getLogger(LocalItemStateManager.class);

    /**
     * Known attribute name
     */
    private static final String ATTRIBUTE_STATES = "ItemStates";

    /**
     * Known attribute name
     */
    private static final String ATTRIBUTE_REFS = "NodeReferences";

    /**
     * Shared item state manager
     */
    private final SharedItemStateManager sharedStateMgr;

    /**
     * Currently associated transaction
     */
    private TransactionContext tx;

    /**
     * A cache for <code>NodeReferences</code> objects.
     */
    private Map refsCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);

    /**
     * Creates a new <code>LocalItemStateManager</code> instance.
     *
     * @param sharedStateMgr shared state manager
     */
    public LocalItemStateManager(SharedItemStateManager sharedStateMgr) {
        this.sharedStateMgr = sharedStateMgr;
    }

    /**
     * Disposes this <code>LocalItemStateManager</code> and frees resources.
     */
    public void dispose() {
        // clear cache
        evictAll();

        refsCache.clear();
    }

    /**
     * @param id
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    protected NodeState getNodeState(NodeId id)
            throws NoSuchItemStateException, ItemStateException {

        // check cache
        if (isCached(id)) {
            return (NodeState) retrieve(id);
        }

        // load from parent manager and wrap
        NodeState state = (NodeState) sharedStateMgr.getItemState(id);
        state = new NodeState(state, state.getStatus(), false);

        // put it in cache
        cache(state);

        // register as listener
        state.addListener(this);
        return state;
    }

    /**
     * @param id
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    protected PropertyState getPropertyState(PropertyId id)
            throws NoSuchItemStateException, ItemStateException {

        // check cache
        if (isCached(id)) {
            return (PropertyState) retrieve(id);
        }

        // load from parent manager and wrap
        PropertyState state = (PropertyState) sharedStateMgr.getItemState(id);
        state = new PropertyState(state, state.getStatus(), false);

        // put it in cache
        cache(state);

        // register as listener
        state.addListener(this);
        return state;
    }

    /**
     * Set transaction context
     */
    public void setTransactionContext(TransactionContext tx) {
        dispose();

        if (tx != null) {
            if (tx.getAttribute(ATTRIBUTE_STATES) == null) {
                tx.setAttribute(ATTRIBUTE_STATES, new ArrayList());
                tx.setAttribute(ATTRIBUTE_REFS, new ArrayList());
                tx.addListener(this);
            } else {
                List states = (List) tx.getAttribute(ATTRIBUTE_STATES);
                for (int i = 0; i < states.size(); i++) {
                    cache((ItemState) states.get(i));
                }

                List refsCollection = (List) tx.getAttribute(ATTRIBUTE_REFS);
                for (int i = 0; i < refsCollection.size(); i++) {
                    NodeReferences refs = (NodeReferences) states.get(i);
                    refsCache.put(refs.getTargetId(), refs);
                }
            }
        }
        this.tx = tx;
    }

    /**
     * Dumps the state of this <code>LocalItemStateManager</code> instance
     * (used for diagnostic purposes).
     *
     * @param ps
     */
    public void dump(PrintStream ps) {
        ps.println("LocalItemStateManager (" + this + ")");
        ps.println();
        super.dump(ps);
    }

    //-----------------------------------------------------< ItemStateManager >

    /**
     * @see ItemStateManager#getItemState(ItemId)
     */
    public synchronized ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        if (id.denotesNode()) {
            return getNodeState((NodeId) id);
        } else {
            return getPropertyState((PropertyId) id);
        }
    }

    /**
     * @see ItemStateManager#hasItemState(ItemId)
     */
    public boolean hasItemState(ItemId id) {
        if (isCached(id)) {
            return true;
        }
        return sharedStateMgr.hasItemState(id);
    }

    /**
     * @see ItemStateManager#getNodeReferences
     */
    public synchronized NodeReferences getNodeReferences(NodeId targetId)
            throws NoSuchItemStateException, ItemStateException {

        if (refsCache.containsKey(targetId)) {
            return (NodeReferences) refsCache.get(targetId);
        }

        NodeReferences refs = sharedStateMgr.getNodeReferences(targetId);
        refs = new NodeReferences(refs);

        refsCache.put(targetId, refs);

        return refs;
    }

    /**
     * @see ItemStateManager#beginUpdate
     */
    public UpdateOperation beginUpdate() throws ItemStateException {
        return new Update();
    }

    /**
     * End an update operation
     */
    private void endUpdate(List states, List refsCollection)
            throws ItemStateException {

        for (int i = 0; i < states.size(); i++) {
            ItemState state = (ItemState) states.get(i);
            state.connect(getOrCreateOverlayed(state));
            state.push();
        }
        for (int i = 0; i < refsCollection.size(); i++) {
            NodeReferences refs = (NodeReferences) refsCollection.get(i);
            refs.connect(getOrCreateOverlayed(refs));
            refs.push();
        }
        sharedStateMgr.store(states, refsCollection);
    }

    /**
     * Return the item state inside the shared item state manager
     * corresponding to a given item state.
     */
    private ItemState getOrCreateOverlayed(ItemState state)
            throws ItemStateException {

        switch (state.getStatus()) {
            case ItemState.STATUS_NEW:
                if (state.isNode()) {
                    NodeState ns = (NodeState) state;
                    return sharedStateMgr.createInstance(ns.getUUID(),
                            ns.getNodeTypeName(), ns.getParentUUID());
                } else {
                    PropertyState ps = (PropertyState) state;
                    return sharedStateMgr.createInstance(ps.getName(),
                            ps.getParentUUID());
                }
            default:
                return sharedStateMgr.getItemState(state.getId());
        }
    }

    /**
     * Return the references object inside the shared item state manager
     * corresponding to a references object.
     */
    private NodeReferences getOrCreateOverlayed(NodeReferences refs)
            throws ItemStateException {

        switch (refs.getStatus()) {
            case NodeReferences.STATUS_NEW:
                return new NodeReferences(refs.getTargetId());

            default:
                return sharedStateMgr.getNodeReferences(refs.getTargetId());
        }
    }

    //--------------------------------------------------< TransactionListener >

    /**
     * @see TransactionListener#transactionCommitted
     */
    public void transactionCommitted(TransactionContext tx)
            throws TransactionException {

        List states = (List) tx.getAttribute(ATTRIBUTE_STATES);
        List refsCollection = (List) tx.getAttribute(ATTRIBUTE_REFS);

        try {
            endUpdate(states, refsCollection);
        } catch (ItemStateException e) {
            throw new TransactionException("Unable to end update.", e);
        }
    }

    /**
     * @see TransactionListener#transactionRolledBack
     */
    public void transactionRolledBack(TransactionContext tx) {
    }

    //------------------------------------------------------< UpdateOperation >

    class Update implements UpdateOperation {

        /**
         * Modified states
         */
        private final List states = new ArrayList();

        /**
         * Modified references
         */
        private final List refsCollection = new ArrayList();

        /**
         * @see UpdateOperation#createNew
         */
        public NodeState createNew(String uuid, QName nodeTypeName,
                                   String parentUUID) {

            NodeState state = new NodeState(uuid, nodeTypeName, parentUUID,
                    ItemState.STATUS_NEW, false);

            cache(state);
            state.addListener(LocalItemStateManager.this);

            return state;
        }

        /**
         * @see UpdateOperation#createNew
         */
        public PropertyState createNew(QName propName, String parentUUID) {
            PropertyState state = new PropertyState(propName, parentUUID,
                    ItemState.STATUS_NEW, false);

            cache(state);
            state.addListener(LocalItemStateManager.this);

            return state;
        }

        /**
         * @see UpdateOperation#store
         */
        public void store(ItemState state) {
            state.disconnect();
            states.add(state);
            // notify listeners that the specified instance has been modified
            state.notifyStateUpdated();
        }

        /**
         * @see UpdateOperation#store
         */
        public void store(NodeReferences refs) {
            refs.disconnect();
            refsCollection.add(refs);
        }

        /**
         * @see UpdateOperation#destroy
         */
        public void destroy(ItemState state) {
            state.disconnect();
            state.setStatus(ItemState.STATUS_EXISTING_REMOVED);
            states.add(state);
            // notify listeners that the specified instance has been marked 'removed'
            state.notifyStateDestroyed();
        }

        /**
         * @see UpdateOperation#end
         */
        public void end() throws ItemStateException {
            if (tx != null) {
                List txStates = (List) tx.getAttribute(ATTRIBUTE_STATES);
                List txRefs = (List) tx.getAttribute(ATTRIBUTE_REFS);

                txStates.addAll(states);
                txRefs.addAll(refsCollection);
            } else {
                endUpdate(states, refsCollection);
            }
        }
    }

    //----------------------------------------------------< ItemStateListener >
    /**
     * @see ItemStateListener#stateCreated
     */
    public void stateCreated(ItemState created) {
        // not interested
    }

    /**
     * @see ItemStateListener#stateModified
     */
    public void stateModified(ItemState modified) {
        // not interested
    }

    /**
     * @see ItemStateListener#stateDestroyed
     */
    public void stateDestroyed(ItemState destroyed) {
        destroyed.removeListener(this);
        evict(destroyed.getId());
    }

    /**
     * @see ItemStateListener#stateDiscarded
     */
    public void stateDiscarded(ItemState discarded) {
        discarded.removeListener(this);
        evict(discarded.getId());
    }
}
