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

import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.observation.ObservationManagerImpl;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import java.io.PrintStream;

/**
 * Local <code>ItemStateManager</code> that isolates changes to
 * persistent states from other clients.
 */
public class LocalItemStateManager extends ItemStateCache
        implements UpdatableItemStateManager, ItemStateListener {

    /**
     * Logger instance
     */
    private static Logger log = Logger.getLogger(LocalItemStateManager.class);

    /**
     * Shared item state manager
     */
    protected final SharedItemStateManager sharedStateMgr;

    /**
     * Local WorkspaceImpl instance.
     */
    protected final WorkspaceImpl wspImpl;

    /**
     * Flag indicating whether this item state manager is in edit mode
     */
    private boolean editMode;

    /**
     * Change log
     */
    private final ChangeLog changeLog = new ChangeLog();

    /**
     * Creates a new <code>LocalItemStateManager</code> instance.
     * todo LocalItemStateManager without a wspImpl will not generate observation events!
     *
     * @param sharedStateMgr shared state manager
     * @param wspImpl        the workspace instance where this item state manager
     *                       belongs to, or <code>null</code> if this item state manager is not
     *                       associated with a workspace. This is the case for the version item state
     *                       manager. Version item states are not associated with a specific workspace
     *                       instance.
     */
    public LocalItemStateManager(SharedItemStateManager sharedStateMgr, WorkspaceImpl wspImpl) {
        this.sharedStateMgr = sharedStateMgr;
        this.wspImpl = wspImpl;
    }

    /**
     * Disposes this <code>LocalItemStateManager</code> and frees resources.
     */
    public void dispose() {
        // clear cache
        evictAll();
    }

    /**
     * Retrieve a node state from the parent shared state manager and
     * wraps it into a intermediate object that helps us handle local
     * modifications.
     *
     * @param id node id
     * @return node state
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    protected NodeState getNodeState(NodeId id)
            throws NoSuchItemStateException, ItemStateException {

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
     * Retrieve a property state from the parent shared state manager and
     * wraps it into a intermediate object that helps us handle local
     * modifications.
     *
     * @param id property id
     * @return property state
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
     * {@inheritDoc}
     */
    public synchronized ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        // check change log
        ItemState state = changeLog.get(id);
        if (state != null) {
            return state;
        }

        // check cache
        if (isCached(id)) {
            return retrieve(id);
        }

        // regular behaviour
        if (id.denotesNode()) {
            return getNodeState((NodeId) id);
        } else {
            return getPropertyState((PropertyId) id);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasItemState(ItemId id) {

        // check items in change log
        try {
            ItemState state = changeLog.get(id);
            if (state != null) {
                return true;
            }
        } catch (NoSuchItemStateException e) {
            return false;
        }

        // check cache
        if (isCached(id)) {
            return true;
        }

        // regular behaviour
        return sharedStateMgr.hasItemState(id);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized NodeReferences getNodeReferences(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException {

        // check change log
        NodeReferences refs = changeLog.get(id);
        if (refs != null) {
            return refs;
        }

        return sharedStateMgr.getNodeReferences(id);
    }

    //--------------------------------------------< UpdatableItemStateManager >
    /**
     * {@inheritDoc}
     */
    public void edit() throws ItemStateException {
        if (editMode) {
            throw new ItemStateException("Already in edit mode");
        }
        changeLog.reset();

        editMode = true;
    }

    /**
     * {@inheritDoc}
     */
    public NodeState createNew(String uuid, QName nodeTypeName, String parentUUID) {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }

        NodeState state = new NodeState(uuid, nodeTypeName, parentUUID,
                ItemState.STATUS_NEW, false);
        changeLog.added(state);
        return state;
    }

    /**
     * {@inheritDoc}
     */
    public PropertyState createNew(QName propName, String parentUUID) {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        PropertyState state = new PropertyState(propName, parentUUID,
                ItemState.STATUS_NEW, false);
        changeLog.added(state);
        return state;
    }

    /**
     * {@inheritDoc}
     */
    public void store(ItemState state) {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        changeLog.modified(state);
    }

    /**
     * {@inheritDoc}
     */
    public void store(NodeReferences refs) {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        changeLog.modified(refs);
    }

    /**
     * {@inheritDoc}
     */
    public void destroy(ItemState state) {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        changeLog.deleted(state);
    }

    /**
     * {@inheritDoc}
     */
    public void cancel() {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        changeLog.undo(sharedStateMgr);

        editMode = false;
    }

    /**
     * {@inheritDoc}
     */
    public void update() throws ItemStateException {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        update(changeLog);
        changeLog.reset();

        editMode = false;
    }

    /**
     * End an update operation. Fetch the states and references from
     * the parent (shared) item manager, reconnect them to the items
     * collected in our (local) change log and overwrite the shared
     * items with our copies.
     *
     * @param changeLog change log containing local states and references
     */
    protected void update(ChangeLog changeLog)
            throws ItemStateException {

        try {
            sharedStateMgr.store(changeLog,
                    (wspImpl != null) ? (ObservationManagerImpl) wspImpl.getObservationManager() : null);
        } catch (RepositoryException e) {
            // should never get here
            String msg = "ObservationManager unavailable";
            log.error(msg);
            throw new ItemStateException(msg, e);
        }

        changeLog.persisted();
    }

    //----------------------------------------------------< ItemStateListener >
    /**
     * {@inheritDoc}
     */
    public void stateCreated(ItemState created) {
    }

    /**
     * {@inheritDoc}
     */
    public void stateModified(ItemState modified) {
    }

    /**
     * {@inheritDoc}
     */
    public void stateDestroyed(ItemState destroyed) {
        destroyed.removeListener(this);
        evict(destroyed.getId());
    }

    /**
     * {@inheritDoc}
     */
    public void stateDiscarded(ItemState discarded) {
        discarded.removeListener(this);
        evict(discarded.getId());
    }
}
