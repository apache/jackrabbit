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

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.log4j.Logger;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.io.PrintStream;
import java.util.*;

/**
 * Shared <code>ItemStateManager</code>. Caches objects returned from a
 * <code>PersistenceManager</code>. Objects returned by this item state
 * manager are shared among all sessions.
 */
public class SharedItemStateManager extends ItemStateCache
        implements ItemStateManager, ItemStateListener {

    /**
     * Logger instance
     */
    private static Logger log = Logger.getLogger(SharedItemStateManager.class);

    /**
     * Persistence Manager to use for loading and storing items
     */
    protected final PersistenceManager persistMgr;

    /**
     * Keep a hard reference to the root node state
     */
    private NodeState root;

    /**
     * Creates a new <code>SharedItemStateManager</code> instance.
     *
     * @param persistMgr
     * @param rootNodeUUID
     * @param ntReg
     */
    public SharedItemStateManager(PersistenceManager persistMgr,
                                  String rootNodeUUID,
                                  NodeTypeRegistry ntReg)
            throws ItemStateException {

        this.persistMgr = persistMgr;

        try {
            root = getNodeState(new NodeId(rootNodeUUID));
        } catch (NoSuchItemStateException e) {
            // create root node
            root = createRootNodeState(rootNodeUUID, ntReg);
        }
    }

    /**
     * Disposes this <code>SharedItemStateManager</code> and frees resources.
     */
    public void dispose() {
        // clear cache
        evictAll();
    }

    private NodeState createRootNodeState(String rootNodeUUID,
                                          NodeTypeRegistry ntReg)
            throws ItemStateException {

        NodeState rootState = createInstance(rootNodeUUID, Constants.REP_ROOT, null);

        // @todo FIXME need to manually setup root node by creating mandatory jcr:primaryType property
        NodeDefId nodeDefId = null;
        PropDefId propDefId = null;

        try {
            nodeDefId = new NodeDefId(ntReg.getRootNodeDef());
            // FIXME relies on definition of nt:base:
            // first property definition in nt:base is jcr:primaryType
            propDefId = new PropDefId(ntReg.getNodeTypeDef(Constants.NT_BASE).getPropertyDefs()[0]);
        } catch (NoSuchNodeTypeException nsnte) {
            String msg = "failed to create root node";
            log.debug(msg);
            throw new ItemStateException(msg, nsnte);
        }
        rootState.setDefinitionId(nodeDefId);

        QName propName = Constants.JCR_PRIMARYTYPE;
        rootState.addPropertyEntry(propName);

        PropertyState prop = createInstance(propName, rootNodeUUID);
        prop.setValues(new InternalValue[]{InternalValue.create(Constants.REP_ROOT)});
        prop.setType(PropertyType.NAME);
        prop.setMultiValued(false);
        prop.setDefinitionId(propDefId);

        ChangeLog changeLog = new ChangeLog();
        changeLog.added(rootState);
        changeLog.added(prop);

        persistMgr.store(changeLog);
        changeLog.persisted();

        return rootState;
    }

    /**
     * Dumps the state of this <code>SharedItemStateManager</code> instance
     * (used for diagnostic purposes).
     *
     * @param ps
     */
    public void dump(PrintStream ps) {
        ps.println("SharedItemStateManager (" + this + ")");
        ps.println();
        super.dump(ps);
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

        // load from persisted state
        NodeState state = persistMgr.load(id);
        state.setStatus(ItemState.STATUS_EXISTING);

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

        // load from persisted state
        PropertyState state = persistMgr.load(id);
        state.setStatus(ItemState.STATUS_EXISTING);

        // put it in cache
        cache(state);

        // register as listener
        state.addListener(this);
        return state;
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
    public synchronized boolean hasItemState(ItemId id) {
        if (isCached(id)) {
            return true;
        }

        try {
            if (id.denotesNode()) {
                return persistMgr.exists((NodeId) id);
            } else {
                return persistMgr.exists((PropertyId) id);
            }
        } catch (ItemStateException ise) {
            return false;
        }
    }

    /**
     * @see ItemStateManager#getNodeReferences
     */
    public synchronized NodeReferences getNodeReferences(NodeReferencesId id)
            throws NoSuchItemStateException, ItemStateException {

        NodeReferences refs;

        try {
            refs = persistMgr.load(id);
        } catch (NoSuchItemStateException nsise) {
            refs = new NodeReferences(id);
        }

        return refs;
    }

    //-------------------------------------------------------- other operations

    /**
     * Create a new node state instance
     *
     * @param uuid         uuid
     * @param nodeTypeName node type name
     * @param parentUUID   parent UUID
     * @return new node state instance
     */
    private NodeState createInstance(String uuid, QName nodeTypeName,
                                     String parentUUID) {

        NodeState state = persistMgr.createNew(new NodeId(uuid));
        state.setNodeTypeName(nodeTypeName);
        state.setParentUUID(parentUUID);
        state.setStatus(ItemState.STATUS_NEW);
        state.addListener(this);

        return state;
    }

    /**
     * Create a new node state instance
     * @param other other state associated with new instance
     * @return new node state instance
     */
    private ItemState createInstance(ItemState other) {
        if (other.isNode()) {
            NodeState ns = (NodeState) other;
            return createInstance(ns.getUUID(), ns.getNodeTypeName(), ns.getParentUUID());
        } else {
            PropertyState ps = (PropertyState) other;
            return createInstance(ps.getName(), ps.getParentUUID());
        }
    }

    /**
     * Create a new property state instance
     *
     * @param propName   property name
     * @param parentUUID parent UUID
     * @return new property state instance
     */
    PropertyState createInstance(QName propName, String parentUUID) {
        PropertyState state = persistMgr.createNew(
                new PropertyId(parentUUID, propName));
        state.setStatus(ItemState.STATUS_NEW);
        state.addListener(this);

        return state;
    }

    /**
     * Store modifications registered in a <code>ChangeLog</code>. The items
     * contained in the <tt>ChangeLog</tt> are not states returned by this
     * item state manager but rather must be reconnected to items provided
     * by this state manager.
     * @param local change log containing local items
     * @throws ItemStateException if an error occurs
     */
    public synchronized void store(ChangeLog local) throws ItemStateException {
        ChangeLog shared = new ChangeLog();

        /**
         * Validate modified references. Target node of references may
         * have been deleted in the meantime.
         */
        Iterator iter = local.modifiedRefs();
        while (iter.hasNext()) {
            NodeReferences refs = (NodeReferences) iter.next();
            NodeId id = new NodeId(refs.getUUID());

            if (refs.hasReferences()) {
                try {
                    if (local.get(id) == null && !hasItemState(id)) {
                        throw new NoSuchItemStateException();
                    }
                } catch (NoSuchItemStateException e) {
                    String msg = "Target node " + id + " of REFERENCE property does not exist";
                    throw new ItemStateException(msg);
                }
            }
            shared.modified(refs);
        }

        /**
         * Reconnect all items contained in the change log to their
         * respective shared item and add the shared items to a
         * new change log.
         */
        iter = local.addedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            state.connect(createInstance(state));
            shared.added(state.getOverlayedState());
        }
        iter = local.modifiedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            state.connect(getItemState(state.getId()));
            shared.modified(state.getOverlayedState());
        }
        iter = local.deletedStates();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            state.connect(getItemState(state.getId()));
            shared.deleted(state.getOverlayedState());
        }

        /* Push all changes from the local items to the shared items */
        local.push();

        /* Store items in the underlying persistence manager */
        persistMgr.store(shared);

        /* Let the shared item listeners know about the change */
        shared.persisted();
    }

    //----------------------------------------------------< ItemStateListener >

    /**
     * @see ItemStateListener#stateCreated
     */
    public void stateCreated(ItemState created) {
        cache(created);
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
