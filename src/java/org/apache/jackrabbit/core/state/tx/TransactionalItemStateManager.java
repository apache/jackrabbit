/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.core.state.tx;

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.state.*;
import org.apache.log4j.Logger;

/**
 * Delivers node and item states that will participate in a transaction.
 */
public class TransactionalItemStateManager extends ItemStateCache
        implements PersistentItemStateProvider, ItemStateListener {

    /**
     * Logger instance
     */
    private static Logger log = Logger.getLogger(TransactionalItemStateManager.class);

    /**
     * Controlling transaction
     */
    private final Transaction tx;

    /**
     * Store to use
     */
    private final TransactionalStore store;

    /**
     * Create a new instance of this class.
     *
     * @param tx    controlling transaction
     * @param store store to use when loading elements that have not yet
     *              been cached
     */
    public TransactionalItemStateManager(Transaction tx,
                                         TransactionalStore store) {
        this.tx = tx;
        this.store = store;
    }

    //-------------------------------------------< PersistentItemStateProvider >

    /**
     * @see PersistentItemStateProvider#createNodeState
     */
    public PersistentNodeState createNodeState(String uuid, QName nodeTypeName,
                                               String parentUUID)
            throws ItemStateException {

        NodeId id = new NodeId(uuid);
        if (isCached(id)) {
            String msg = "A node state instance with id " + id + " already exists.";
            log.error(msg);
            throw new ItemStateException(msg);
        }

        PersistentNodeState state = createState(id);
        state.setNodeTypeName(nodeTypeName);
        state.setParentUUID(parentUUID);
        cache(state);

        return state;
    }

    /**
     * @see PersistentItemStateProvider#createPropertyState
     */
    public PersistentPropertyState createPropertyState(String parentUUID,
                                                       QName propName)
            throws ItemStateException {

        PropertyId id = new PropertyId(parentUUID, propName);
        if (isCached(id)) {
            String msg = "A property state instance with id " + id + " already exists.";
            log.error(msg);
            throw new ItemStateException(msg);
        }

        PersistentPropertyState state = createState(id);
        cache(state);

        return state;
    }

    /**
     * @see PersistentItemStateProvider#getItemState
     *      <p/>
     *      Calls the transactional store to retrieve the shared, read-only item.
     *      Wraps this item with a transactional extension that will intercept
     *      calls to {@link PersistableItemState#store} and
     *      {@link PersistableItemState#destroy}.
     */
    public synchronized ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {
        if (isCached(id)) {
            return retrieve(id);
        }

        ItemState state = store.load(id);
        if (state.isNode()) {
            state = createState((PersistentNodeState) state);
        } else {
            state = createState((PersistentPropertyState) state);
        }
        cache(state);
        state.addListener(this);

        return state;
    }

    /**
     * @see PersistentItemStateProvider#hasItemState
     */
    public boolean hasItemState(ItemId id) {
        // try shortcut first
        if (isCached(id)) {
            return true;
        }
        try {
            getItemState(id);
            return true;
        } catch (ItemStateException e) {
            return false;
        }
    }

    /**
     * @see PersistentItemStateProvider#getItemStateInAttic
     */
    public ItemState getItemStateInAttic(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        throw new NoSuchItemStateException(id.toString());
    }

    /**
     * @see PersistentItemStateProvider#hasItemStateInAttic
     */
    public boolean hasItemStateInAttic(ItemId id) {
        return false;
    }

    /**
     * Create a persistent node state instance given its id.
     *
     * @param id node id
     */
    private PersistentNodeState createState(NodeId id) {
        return new TransactionalNodeState(id.getUUID(), tx);
    }

    /**
     * Create a persistent node state instance given its overlaid state.
     *
     * @param state overlaid state
     */
    private PersistentNodeState createState(PersistentNodeState state) {
        return new TransactionalNodeState(state, tx);
    }

    /**
     * Create a persistent property state instance given its id.
     *
     * @param id property id
     */
    private PersistentPropertyState createState(PropertyId id) {
        return new TransactionalPropertyState(id.getName(),
                id.getParentUUID(), tx);
    }

    /**
     * Create a persistent property state instance given its overlaid state.
     *
     * @param state overlaid state
     */
    private PersistentPropertyState createState(PersistentPropertyState state) {
        return new TransactionalPropertyState(state, tx);
    }

    //-----------------------------------------------------< ItemStateListener >

    /**
     * @see ItemStateListener#stateCreated
     *      <p/>
     *      Invoked when the state of some freshly created item has been stored
     *      for the first time.
     */
    public void stateCreated(ItemState created) {
    }

    /**
     * @see ItemStateListener#stateModified
     *      <p/>
     *      Invoked when the state of some item has been stored. The state is no
     *      longer connected to the actual persistent state, but will continue
     *      its life in the transaction.
     */
    public void stateModified(ItemState modified) {
    }

    /**
     * @see ItemStateListener#stateDestroyed
     *      <p/>
     *      Invoked when the state of some item has been destroyed. The state is no
     *      longer connected to the actual persistent state, but will continue
     *      its life in the transaction.
     */
    public void stateDestroyed(ItemState destroyed) {
        destroyed.removeListener(this);
    }

    /**
     * @see ItemStateListener#stateDiscarded
     */
    public void stateDiscarded(ItemState discarded) {
        discarded.removeListener(this);
    }
}
