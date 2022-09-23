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
package org.apache.jackrabbit.core.virtual;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.util.WeakIdentityCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Collection;
import java.util.Map;

/**
 * This Class implements a virtual item state provider, in order to expose the
 * versions to the version storage.
 */
public abstract class AbstractVISProvider implements VirtualItemStateProvider, ItemStateListener {
    /**
     * the default logger
     */
    private static Logger log = LoggerFactory.getLogger(AbstractVISProvider.class);

    /**
     * the root node
     */
    private VirtualNodeState root = null;

    /**
     * the root node id
     */
    protected final NodeId rootNodeId;

    /**
     * the node type registry
     */
    protected final NodeTypeRegistry ntReg;

    /**
     * the cache node states. key=ItemId, value=ItemState
     */
    private final Map<NodeId, NodeState> nodes =
        // Using soft references instead of weak ones seems to have
        // some unexpected performance consequences, so for now it's
        // better to stick with weak references.
        new ReferenceMap<>(ReferenceStrength.HARD, ReferenceStrength.WEAK);

    /**
     * Listeners (weak references)
     */
    @SuppressWarnings("unchecked")
    private final transient Collection<ItemStateListener> listeners =
        new WeakIdentityCollection(5);

    /**
     * Creates an abstract virtual item state provider
     *
     * @param ntReg
     * @param rootNodeId
     */
    public AbstractVISProvider(NodeTypeRegistry ntReg, NodeId rootNodeId) {
        this.ntReg = ntReg;
        this.rootNodeId = rootNodeId;
    }

    /**
     * Creates the root node state.
     *
     * @return The virtual root node state.
     * @throws RepositoryException
     */
    protected abstract VirtualNodeState createRootNodeState() throws RepositoryException;

    //-----------------------------------------------------< ItemStateManager >

    /**
     * {@inheritDoc}
     */
    public boolean hasItemState(ItemId id) {
        if (id instanceof NodeId) {
            if (nodes.containsKey(id)) {
                return true;
            } else if (id.equals(rootNodeId)) {
                return true;
            } else {
                return internalHasNodeState((NodeId) id);
            }
        } else {
            return internalHasPropertyState((PropertyId) id);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {

        if (id instanceof NodeId) {
            ItemState s;
            if (nodes.containsKey(id)) {
                s = nodes.get(id);
            } else if (id.equals(rootNodeId)) {
                s = getRootState();
            } else {
                s = cache(internalGetNodeState((NodeId) id));
            }
            return s;
        } else {
            return internalGetPropertyState((PropertyId) id);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NodeReferences getNodeReferences(NodeId id)
            throws NoSuchItemStateException, ItemStateException {
        throw new NoSuchItemStateException(id.toString());
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNodeReferences(NodeId id) {
        return false;
    }

    //-------------------------------------------< VirtualItemStateProvider >---

    /**
     * {@inheritDoc}
     */
    public boolean isVirtualRoot(ItemId id) {
        return id.equals(rootNodeId);
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getVirtualRootId() {
        return rootNodeId;
    }

    /**
     * {@inheritDoc}
     */
    public NodeId[] getVirtualRootIds() {
        return new NodeId[]{rootNodeId};
    }

    /**
     * Returns the root state
     *
     * @return the root state
     * @throws ItemStateException If the root node state does not exist and its
     * creation fails.
     */
    public synchronized NodeState getRootState() throws ItemStateException {
        try {
            if (root == null) {
                root = createRootNodeState();
            }
            return root;
        } catch (RepositoryException e) {
            throw new ItemStateException("Error creating root node state", e);
        }
    }

    /**
     * Discards all virtual item states and prepares for the root state
     * to be recreated when next accessed.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-2617">JCR-2617</a>
     */
    protected synchronized void discardAll() {
        if (root != null) {
            discardTree(root);
            nodes.clear();
            root = null;
        }
    }

    /**
     * Recursively discards all the properties and nodes in the subtree
     * rooted at the given node state.
     *
     * @param state root of the subtree to be discarded
     */
    private void discardTree(NodeState state) {
        for (Name name : state.getPropertyNames()) {
            try {
                getItemState(new PropertyId(state.getNodeId(), name)).discard();
            } catch (ItemStateException e) {
                log.warn("Unable to discard virtual property " + name, e);
            }
        }
        for (ChildNodeEntry entry : state.getChildNodeEntries()) {
            try {
                discardTree((NodeState) getItemState(entry.getId()));
            } catch (ItemStateException e) {
                log.warn("Unable to discard virtual node " + entry.getId(), e);
            }
        }
        state.discard();
    }

    /**
     * Checks if this provide has the node state of the given node id
     *
     * @param id
     * @return <code>true</code> if it has the node state
     */
    protected abstract boolean internalHasNodeState(NodeId id);

    /**
     * Retrieves the node state with the given node id
     *
     * @param id
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    protected abstract VirtualNodeState internalGetNodeState(NodeId id)
            throws NoSuchItemStateException, ItemStateException;

    /**
     * Checks if this provider has the property state of the given id.
     *
     * @param id
     * @return <code>true</code> if it has the property state
     */
    protected boolean internalHasPropertyState(PropertyId id) {

        try {
            // get parent state
            NodeState parent = (NodeState) getItemState(id.getParentId());

            // handle some default prop states
            if (parent instanceof VirtualNodeState) {
                return parent.hasPropertyName(id.getName());
            }
        } catch (ItemStateException e) {
            // ignore
        }
        return false;
    }

    /**
     * Retrieves the property state for the given id
     *
     * @param id
     * @return
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     */
    protected VirtualPropertyState internalGetPropertyState(PropertyId id)
            throws NoSuchItemStateException, ItemStateException {

        // get parent state
        NodeState parent = (NodeState) getItemState(id.getParentId());

        // handle some default prop states
        if (parent instanceof VirtualNodeState) {
            return ((VirtualNodeState) parent).getProperty(id.getName());
        }
        throw new NoSuchItemStateException(id.toString());
    }

    /**
     * {@inheritDoc}
     */
    public VirtualPropertyState createPropertyState(VirtualNodeState parent,
                                                    Name name, int type,
                                                    boolean multiValued)
            throws RepositoryException {
        PropertyId id = new PropertyId(parent.getNodeId(), name);
        VirtualPropertyState prop = new VirtualPropertyState(id);
        prop.setType(type);
        prop.setMultiValued(multiValued);
        return prop;
    }

    /**
     * {@inheritDoc}
     */
    public VirtualNodeState createNodeState(VirtualNodeState parent, Name name,
                                            NodeId id, Name nodeTypeName)
            throws RepositoryException {

        assert id != null;

        // create a new node state
        VirtualNodeState state = new VirtualNodeState(this, parent.getNodeId(), id, nodeTypeName, Name.EMPTY_ARRAY);

        cache(state);
        return state;
    }

    /**
     * {@inheritDoc}
     */
    public void addListener(ItemStateListener listener) {
        synchronized (listeners) {
            assert (!listeners.contains(listener));
            listeners.add(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeListener(ItemStateListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * returns the node type manager
     *
     * @return the node type manager
     */
    protected NodeTypeRegistry getNodeTypeRegistry() {
        return ntReg;
    }

    /**
     * adds the node state to the cache
     *
     * @param state
     * @return The same state.
     */
    protected NodeState cache(NodeState state) {
        if (state != null) {
            nodes.put(state.getNodeId(), state);
            log.debug("item added to cache. size=" + nodes.size());
        }
        return state;
    }

    /**
     * removes the node state from the cache
     *
     * @param id
     */
    protected void evict(NodeId id) {
        nodes.remove(id);
    }

    /**
     * invalidates the item
     *
     * @param id
     * @param recursive
     */
    public void invalidateItem(ItemId id, boolean recursive) {
        VirtualNodeState state = id.equals(rootNodeId) ? root : (VirtualNodeState) nodes.get(id);
        if (state != null) {
            if (recursive) {
                VirtualPropertyState[] props = state.getProperties();
                for (int i = 0; i < props.length; i++) {
                    props[i].notifyStateUpdated();
                }
                for (ChildNodeEntry pe : state.getChildNodeEntries()) {
                    invalidateItem(pe.getId(), true);
                }
            }
            state.notifyStateUpdated();
        }
    }

    /**
     * retrieves the property definition for the given constraints
     *
     * @param parent The parent node state.
     * @param propertyName The name of the property.
     * @param type
     * @param multiValued
     * @return
     * @throws RepositoryException
     */
    protected QPropertyDefinition getApplicablePropertyDef(NodeState parent, Name propertyName,
                                               int type, boolean multiValued)
            throws RepositoryException {
        return getEffectiveNodeType(parent).getApplicablePropertyDef(propertyName, type, multiValued);
    }

    /**
     * Retrieves the node definition for the given constraints.
     *
     * @param parent The parent state.
     * @param nodeName
     * @param nodeTypeName
     * @return
     * @throws RepositoryException
     */
    protected QNodeDefinition getApplicableChildNodeDef(NodeState parent, Name nodeName, Name nodeTypeName)
            throws RepositoryException {
        return getEffectiveNodeType(parent).getApplicableChildNodeDef(
                nodeName, nodeTypeName, getNodeTypeRegistry());
    }

    /**
     * Returns the effective (i.e. merged and resolved) node type representation
     * of this node's primary and mixin node types.
     *
     * @return the effective node type
     * @throws RepositoryException
     */
    protected EffectiveNodeType getEffectiveNodeType(NodeState parent) throws RepositoryException {
        try {
            NodeTypeRegistry ntReg = getNodeTypeRegistry();
            return ntReg.getEffectiveNodeType(
                    parent.getNodeTypeName(), parent.getMixinTypeNames());
        } catch (NodeTypeConflictException ntce) {
            String msg = "internal error: failed to build effective node type for node " + parent.getNodeId();
            throw new RepositoryException(msg, ntce);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateCreated(ItemState created) {
        ItemStateListener[] la;
        synchronized (listeners) {
            la = listeners.toArray(new ItemStateListener[listeners.size()]);
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].stateCreated(created);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateModified(ItemState modified) {
        ItemStateListener[] la;
        synchronized (listeners) {
            la = listeners.toArray(new ItemStateListener[listeners.size()]);
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].stateModified(modified);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateDestroyed(ItemState destroyed) {
        if (destroyed.isNode() && destroyed.getId().equals(rootNodeId)) {
            try {
                root = createRootNodeState();
            } catch (RepositoryException e) {
                // ignore
            }
        }
        evict((NodeId) destroyed.getId());

        ItemStateListener[] la;
        synchronized (listeners) {
            la = listeners.toArray(new ItemStateListener[listeners.size()]);
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].stateDestroyed(destroyed);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stateDiscarded(ItemState discarded) {
        if (discarded.isNode() && discarded.getId().equals(rootNodeId)) {
            try {
                root = createRootNodeState();
            } catch (RepositoryException e) {
                // ignore
            }
        }
        evict((NodeId) discarded.getId());

        ItemStateListener[] la;
        synchronized (listeners) {
            la = listeners.toArray(new ItemStateListener[listeners.size()]);
        }
        for (int i = 0; i < la.length; i++) {
            if (la[i] != null) {
                la[i].stateDiscarded(discarded);
            }
        }
    }
}
