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
package org.apache.jackrabbit.jcr.core.state;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.jcr.core.*;
import org.apache.jackrabbit.jcr.core.nodetype.NodeDefId;
import org.apache.jackrabbit.jcr.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.jcr.core.nodetype.PropDefId;

import javax.jcr.PropertyType;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.io.PrintStream;
import java.util.Iterator;

/**
 * <code>PersistentItemStateManager</code> ...
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.23 $, $Date: 2004/08/25 17:32:18 $
 */
public class PersistentItemStateManager extends ItemStateCache
	implements ItemStateProvider, ItemStateListener {

    private static Logger log = Logger.getLogger(PersistentItemStateManager.class);

    private final PersistenceManager persistMgr;

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

    private PersistentNodeState createPersistentRootNodeState(String rootNodeUUID,
							      NodeTypeRegistry ntReg)
	    throws ItemStateException {
	PersistentNodeState rootState = createNodeState(rootNodeUUID, NodeTypeRegistry.NT_UNSTRUCTURED, null);

	// @todo FIXME need to manually setup root node by creating mandatory jcr:primaryType property
	NodeDefId nodeDefId = null;
	PropDefId propDefId = null;

	try {
	    // FIXME relies on definition of nt:unstructured and nt:base:
	    // first (and only) child node definition in nt:unstructured is applied to root node
	    nodeDefId = new NodeDefId(ntReg.getNodeTypeDef(NodeTypeRegistry.NT_UNSTRUCTURED).getChildNodeDefs()[0]);
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
	prop.setValues(new InternalValue[]{InternalValue.create(NodeTypeRegistry.NT_UNSTRUCTURED)});
	prop.setType(PropertyType.NAME);
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

    //----------------------------------------------------< ItemStateProvider >
    /**
     * @see ItemStateProvider#getItemState(ItemId)
     */
    public ItemState getItemState(ItemId id)
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
	try {
	    getItemState(id);
	    return true;
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

    //---------------< methods for listing and retrieving ItemState instances >
    /**
     * @param id
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    synchronized PersistentNodeState getNodeState(NodeId id)
	    throws NoSuchItemStateException, ItemStateException {
	// check cache
	if (isCached(id)) {
	    return (PersistentNodeState) retrieve(id);
	}
	// load from persisted state
	PersistentNodeState state = persistMgr.loadNodeState(id.getUUID());
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
    synchronized PersistentPropertyState getPropertyState(PropertyId id)
	    throws NoSuchItemStateException, ItemStateException {
	// check cache
	if (isCached(id)) {
	    return (PersistentPropertyState) retrieve(id);
	}
	// load from persisted state
	PersistentPropertyState state = persistMgr.loadPropertyState(id.getParentUUID(), id.getName());
	state.setStatus(ItemState.STATUS_EXISTING);
	// put it in cache
	cache(state);
	// register as listener
	state.addListener(this);
	return state;
    }

    //-------------------------< methods for creating new ItemState instances >
    /**
     * Creates a <code>PersistentNodeState</code> instance representing new,
     * i.e. not yet existing state. Call <code>{@link PersistentNodeState#store()}</code>
     * on the returned object to make it persistent.
     *
     * @param uuid
     * @param nodeTypeName
     * @param parentUUID
     * @return
     * @throws ItemStateException
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

	PersistentNodeState state = persistMgr.createNodeStateInstance(uuid, nodeTypeName);
	state.setParentUUID(parentUUID);
	// put it in cache
	cache(state);
	// register as listener
	state.addListener(this);
	return state;
    }

    /**
     * Creates a <code>PersistentPropertyState</code> instance representing new,
     * i.e. not yet existing state. Call <code>{@link PersistentPropertyState#store()}</code>
     * on the returned object to make it persistent.
     *
     * @param parentUUID
     * @param propName
     * @return
     * @throws ItemStateException
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

	PersistentPropertyState state = persistMgr.createPropertyStateInstance(propName, parentUUID);
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
