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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.log4j.Logger;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.io.PrintStream;
import java.util.Iterator;

/**
 * <code>PersistentItemStateManager</code> ...
 */
public class PersistentItemStateManager extends ItemStateCache
        implements PersistentItemStateProvider, ItemStateListener {

    private static Logger log = Logger.getLogger(PersistentItemStateManager.class);

    protected final PersistenceManager persistMgr;


    // keep a hard reference to the root node state
    private PersistentNodeState root;

    /**
     * Creates a new <code>PersistentItemStateManager</code> instance.
     *
     * @param persistMgr
     * @param rootNodeUUID
     * @param ntReg
     */
    public PersistentItemStateManager(PersistenceManager persistMgr,
                                      String rootNodeUUID,
                                      NodeTypeRegistry ntReg)
            throws ItemStateException {
        this.persistMgr = persistMgr;

        try {
            root = getNodeState(new NodeId(rootNodeUUID));
        } catch (NoSuchItemStateException e) {
            // create root node
            root = createPersistentRootNodeState(rootNodeUUID, ntReg);
        }
    }

    /**
     * Disposes this <code>PersistentItemStateManager</code> and frees resources.
     */
    public void dispose() {
        // clear cache
        evictAll();
    }

    private PersistentNodeState createPersistentRootNodeState(String rootNodeUUID,
                                                              NodeTypeRegistry ntReg)
            throws ItemStateException {
        PersistentNodeState rootState = createNodeState(rootNodeUUID, NodeTypeRegistry.REP_ROOT, null);

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

        PersistentPropertyState prop = createPropertyState(rootNodeUUID, propName);
        prop.setValues(new InternalValue[]{InternalValue.create(NodeTypeRegistry.REP_ROOT)});
        prop.setType(PropertyType.NAME);
        prop.setMultiValued(false);
        prop.setDefinitionId(propDefId);

        rootState.store();
        prop.store();

        return rootState;
    }

    /**
     * Dumps the state of this <code>TransientItemStateManager</code> instance
     * (used for diagnostic purposes).
     *
     * @param ps
     */
    public void dump(PrintStream ps) {
        ps.println("PersistentItemStateManager (" + this + ")");
        ps.println();
        ps.println("entries in cache:");
        ps.println();
        Iterator iter = keys();
        while (iter.hasNext()) {
            ItemId id = (ItemId) iter.next();
            ItemState state = retrieve(id);
            dumpItemState(id, state, ps);
        }
    }

    private void dumpItemState(ItemId id, ItemState state, PrintStream ps) {
        ps.print(state.isNode() ? "Node: " : "Prop: ");
        switch (state.getStatus()) {
            case ItemState.STATUS_EXISTING:
                ps.print("[existing]           ");
                break;
            case ItemState.STATUS_EXISTING_MODIFIED:
                ps.print("[existing, modified] ");
                break;
            case ItemState.STATUS_EXISTING_REMOVED:
                ps.print("[existing, removed]  ");
                break;
            case ItemState.STATUS_NEW:
                ps.print("[new]                ");
                break;
            case ItemState.STATUS_STALE_DESTROYED:
                ps.print("[stale, destroyed]   ");
                break;
            case ItemState.STATUS_STALE_MODIFIED:
                ps.print("[stale, modified]    ");
                break;
            case ItemState.STATUS_UNDEFINED:
                ps.print("[undefined]          ");
                break;
        }
        ps.println(id + " (" + state + ")");
    }

    /**
     * Create a <code>PersistentNodeState</code> instance. May be overridden by
     * subclasses.
     *
     * @param uuid UUID
     * @return persistent node state instance
     */
    protected PersistentNodeState createNodeStateInstance(String uuid) {
        return new PersistentNodeState(uuid, persistMgr);
    }

    /**
     * Create a <code>PersistentPropertyState</code> instance. May be overridden
     * by subclasses.
     *
     * @param name       qualified name
     * @param parentUUID parent UUID
     * @return persistent property state instance
     */
    protected PersistentPropertyState createPropertyStateInstance(QName name,
                                                                  String parentUUID) {

        return new PersistentPropertyState(name, parentUUID, persistMgr);
    }

    /**
     * @param id
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    protected PersistentNodeState getNodeState(NodeId id)
            throws NoSuchItemStateException, ItemStateException {
        // check cache
        if (isCached(id)) {
            return (PersistentNodeState) retrieve(id);
        }
        // load from persisted state
        PersistentNodeState state = createNodeStateInstance(id.getUUID());
        persistMgr.load(state);
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
    protected PersistentPropertyState getPropertyState(PropertyId id)
            throws NoSuchItemStateException, ItemStateException {
        // check cache
        if (isCached(id)) {
            return (PersistentPropertyState) retrieve(id);
        }
        // load from persisted state
        PersistentPropertyState state = createPropertyStateInstance(id.getName(), id.getParentUUID());
        persistMgr.load(state);
        state.setStatus(ItemState.STATUS_EXISTING);
        // put it in cache
        cache(state);
        // register as listener
        state.addListener(this);
        return state;
    }

    //----------------------------------------------------< ItemStateProvider >
    /**
     * @see ItemStateProvider#getItemState(ItemId)
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
     * @see ItemStateProvider#hasItemState(ItemId)
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
     * @see ItemStateProvider#getItemStateInAttic(ItemId)
     */
    public ItemState getItemStateInAttic(ItemId id)
            throws NoSuchItemStateException, ItemStateException {
        // n/a
        throw new NoSuchItemStateException(id.toString());
    }

    /**
     * @see ItemStateProvider#hasItemStateInAttic(ItemId)
     */
    public boolean hasItemStateInAttic(ItemId id) {
        // n/a
        return false;
    }

    //------------------------------------------< PersistentItemStateProvider >
    /**
     * @see PersistentItemStateProvider#createNodeState
     */
    public synchronized PersistentNodeState createNodeState(String uuid, QName nodeTypeName, String parentUUID)
            throws ItemStateException {
        NodeId id = new NodeId(uuid);
        // check cache
        if (isCached(id)) {
            String msg = "there's already a node state instance with id " + id;
            log.error(msg);
            throw new ItemStateException(msg);
        }

        PersistentNodeState state = createNodeStateInstance(uuid);
        state.setNodeTypeName(nodeTypeName);
        state.setParentUUID(parentUUID);
        // put it in cache
        cache(state);
        // register as listener
        state.addListener(this);
        return state;
    }

    /**
     * @see PersistentItemStateProvider#createPropertyState
     */
    public synchronized PersistentPropertyState createPropertyState(String parentUUID, QName propName)
            throws ItemStateException {
        PropertyId id = new PropertyId(parentUUID, propName);
        // check cache
        if (isCached(id)) {
            String msg = "there's already a property state instance with id " + id;
            log.error(msg);
            throw new ItemStateException(msg);
        }

        PersistentPropertyState state = createPropertyStateInstance(propName, parentUUID);
        // put it in cache
        cache(state);
        // register as listener
        state.addListener(this);
        return state;
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
