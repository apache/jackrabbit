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
     * A cache for <code>NodeReferences</code> objects.
     */
    private Map refsCache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);

    /**
     * Creates a new <code>DefaultItemStateManager</code> instance.
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

        NodeState rootState = createInstance(rootNodeUUID, NodeTypeRegistry.REP_ROOT, null);

        // @todo FIXME need to manually setup root node by creating mandatory jcr:primaryType property
        NodeDefId nodeDefId = null;
        PropDefId propDefId = null;

        try {
            nodeDefId = new NodeDefId(ntReg.getRootNodeDef());
            // FIXME relies on definition of nt:base:
            // first property definition in nt:base is jcr:primaryType
            propDefId = new PropDefId(ntReg.getNodeTypeDef(NodeTypeRegistry.NT_BASE).getPropertyDefs()[0]);
        } catch (NoSuchNodeTypeException nsnte) {
            String msg = "failed to create root node";
            log.error(msg, nsnte);
            throw new ItemStateException(msg, nsnte);
        }
        rootState.setDefinitionId(nodeDefId);

        QName propName = new QName(NamespaceRegistryImpl.NS_JCR_URI, "primaryType");
        rootState.addPropertyEntry(propName);

        PropertyState prop = createInstance(propName, rootNodeUUID);
        prop.setValues(new InternalValue[]{InternalValue.create(NodeTypeRegistry.REP_ROOT)});
        prop.setType(PropertyType.NAME);
        prop.setMultiValued(false);
        prop.setDefinitionId(propDefId);

        ArrayList states = new ArrayList();
        states.add(rootState);
        states.add(prop);

        // do persist root node (incl. properties)
        store(states, Collections.EMPTY_LIST);

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
        NodeState state = persistMgr.load(id.getUUID());
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
        PropertyState state = persistMgr.load(id.getName(), id.getParentUUID());
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
    public boolean hasItemState(ItemId id) {
        if (isCached(id)) {
            return true;
        }

        try {
            return persistMgr.exists(id);
        } catch (ItemStateException ise) {
            return false;
        }
    }

    /**
     * @see ItemStateManager#getNodeReferences
     */
    public synchronized NodeReferences getNodeReferences(NodeId targetId)
            throws NoSuchItemStateException, ItemStateException {

        if (refsCache.containsKey(targetId)) {
            return (NodeReferences) refsCache.get(targetId);
        }

        NodeReferences refs;

        try {
            refs = persistMgr.load(targetId);
        } catch (NoSuchItemStateException nsise) {
            refs = new NodeReferences(targetId);
        }

        refsCache.put(targetId, refs);
        return refs;
    }

    /**
     * @see ItemStateManager#beginUpdate
     */
    public UpdateOperation beginUpdate() throws ItemStateException {
        throw new ItemStateException("Update not available.");
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
    NodeState createInstance(String uuid, QName nodeTypeName,
                             String parentUUID) {

        NodeState state = persistMgr.createNew(uuid, nodeTypeName, parentUUID);
        state.setStatus(ItemState.STATUS_NEW);
        state.addListener(this);

        return state;
    }

    /**
     * Create a new property state instance
     *
     * @param propName   property name
     * @param parentUUID parent UUID
     * @return new property state instance
     */
    PropertyState createInstance(QName propName, String parentUUID) {
        PropertyState state = persistMgr.createNew(propName, parentUUID);
        state.setStatus(ItemState.STATUS_NEW);
        state.addListener(this);

        return state;
    }

    /**
     * Store modified states and node references, atomically.
     *
     * @param states         states that have been modified
     * @param refsCollection collection of refs to store
     * @throws ItemStateException if an error occurs
     */
    void store(Collection states, Collection refsCollection)
            throws ItemStateException {

        persistMgr.store(states.iterator(), refsCollection.iterator());

        Iterator iter = states.iterator();
        while (iter.hasNext()) {
            ItemState state = (ItemState) iter.next();
            int status = state.getStatus();
            // @todo FIXME need to notify listeners on underlying (shared) state
            if (state.getOverlayedState() != null) {
                state = state.getOverlayedState();
            }
            switch (status) {
                case ItemState.STATUS_NEW:
                    state.notifyStateCreated();
                    state.setStatus(ItemState.STATUS_EXISTING);
                    break;

                case ItemState.STATUS_EXISTING_REMOVED:
                    state.notifyStateDestroyed();
                    state.discard();
                    break;

                default:
                    state.notifyStateUpdated();
                    state.setStatus(ItemState.STATUS_EXISTING);
                    break;
            }
        }
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
