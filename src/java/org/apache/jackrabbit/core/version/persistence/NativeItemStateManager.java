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
package org.apache.jackrabbit.core.version.persistence;

import org.apache.jackrabbit.core.state.*;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.NodeDefId;
import org.apache.jackrabbit.core.nodetype.PropDefId;
import org.apache.jackrabbit.core.*;
import org.apache.log4j.Logger;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.PropertyType;

/**
 * This Class implements...
 *
 * @author tripod
 * @version $Revision:$, $Date:$
 */
public class NativeItemStateManager extends ItemStateCache
        implements UpdatableItemStateManager, ItemStateListener {

    /**
     * Logger instance
     */
    private static Logger log = Logger.getLogger(NativeItemStateManager.class);

    /**
     * Persistence Manager to use for loading and storing items
     */
    protected final PersistenceManager persistMgr;

    /**
     * Keep a hard reference to the root node state
     */
    private NodeState root;

    /**
     * Flag indicating whether this item state manager is in edit mode
     */
    private boolean editMode;

    /**
     * Change log
     */
    private ChangeLog changeLog;

    /**
     * Creates a new <code>DefaultItemStateManager</code> instance.
     *
     * @param persistMgr
     * @param rootNodeUUID
     * @param ntReg
     */
    public NativeItemStateManager(PersistenceManager persistMgr,
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
            log.debug(msg);
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

        ChangeLog changeLog = new ChangeLog();
        changeLog.added(rootState);
        changeLog.added(prop);
        store(changeLog);

        return rootState;
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
    public boolean hasItemState(ItemId id) {
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

    /**
     * @see UpdatableItemStateManager#edit
     */
    public void edit() throws ItemStateException {
        if (editMode) {
            throw new ItemStateException("Already in edit mode.");
        }
        editMode = true;

        changeLog = new ChangeLog();
    }

    /**
     * @see UpdatableItemStateManager#createNew
     */
    public NodeState createNew(String uuid, QName nodeTypeName,
                               String parentUUID) {

        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        return createInstance(uuid, nodeTypeName, parentUUID);
    }

    /**
     * @see UpdatableItemStateManager#createNew
     */
    public PropertyState createNew(QName propName, String parentUUID) {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        return createInstance(propName, parentUUID);
    }

    /**
     * @see UpdatableItemStateManager#store
     */
    public void store(ItemState state) {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        changeLog.modified(state);
    }

    /**
     * @see UpdatableItemStateManager#store
     */
    public void store(NodeReferences refs) {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        changeLog.modified(refs);
    }

    /**
     * @see UpdatableItemStateManager#destroy
     */
    public void destroy(ItemState state) {
        if (!editMode) {
            throw new IllegalStateException("Not in edit mode");
        }
        changeLog.deleted(state);
    }

    /**
     * @see UpdatableItemStateManager#cancel
     */
    public void cancel() {
        editMode = false;

        changeLog.discard();
    }

    /**
     * @see UpdatableItemStateManager#update
     */
    public void update() throws ItemStateException {
        store(changeLog);

        editMode = false;
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
        cache(state);
        return state;
    }

    /**
     * Create a new property state instance
     *
     * @param propName   property name
     * @param parentUUID parent UUID
     * @return new property state instance
     */
    private PropertyState createInstance(QName propName, String parentUUID) {
        PropertyState state = persistMgr.createNew(new PropertyId(parentUUID, propName));
        state.setStatus(ItemState.STATUS_NEW);
        state.addListener(this);
        cache(state);
        return state;
    }

    /**
     * Save all states and node references, atomically.
     * @param changeLog change log containing states that were changed
     * @throws ItemStateException if an error occurs
     */
    private synchronized void store(ChangeLog changeLog)
            throws ItemStateException {

        persistMgr.store(changeLog);
        changeLog.persisted();
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
