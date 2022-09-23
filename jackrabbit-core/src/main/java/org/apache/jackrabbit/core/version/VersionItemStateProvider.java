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
package org.apache.jackrabbit.core.version;

import javax.jcr.RepositoryException;

import org.apache.commons.collections4.map.AbstractReferenceMap.ReferenceStrength;
import org.apache.commons.collections4.map.ReferenceMap;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.core.virtual.VirtualNodeState;
import org.apache.jackrabbit.core.virtual.VirtualPropertyState;
import org.apache.jackrabbit.spi.Name;

/**
 * This Class implements a virtual item state provider.
 */
class VersionItemStateProvider implements VirtualItemStateProvider, ItemStateListener {

    /**
     * The root node UUID for the version storage
     */
    private final NodeId historyRootId;

    /**
     * The root node UUID for the activity storage
     */
    private final NodeId activitiesRootId;

    /**
     * The item state manager directly on the version persistence mgr
     */
    private final VersionItemStateManager stateMgr;

    /**
     * Map of returned items. this is kept for invalidating
     */
    private ReferenceMap<ItemId, ItemState> items = new ReferenceMap<>(ReferenceStrength.HARD, ReferenceStrength.WEAK);

    /**
     * Creates a new version manager
     *
     */
    public VersionItemStateProvider(NodeId historyRootId,
                                    NodeId activitiesRootId,
                                    VersionItemStateManager stateMgr) {
        this.historyRootId = historyRootId;
        this.activitiesRootId = activitiesRootId;
        this.stateMgr = stateMgr;

        stateMgr.addListener(this);
    }

    //------------------------------------------< VirtualItemStateProvider >---

    /**
     * @inheritDoc
     */
    public boolean isVirtualRoot(ItemId id) {
        return id.equals(historyRootId) || id.equals(activitiesRootId);
    }

    /**
     * @inheritDoc
     */
    public NodeId getVirtualRootId() {
        return historyRootId;
    }

    /**
     * @inheritDoc
     */
    public NodeId[] getVirtualRootIds() {
        return new NodeId[]{historyRootId, activitiesRootId};
    }

    /**
     * @inheritDoc
     */
    public VirtualPropertyState createPropertyState(VirtualNodeState parent,
                                                    Name name, int type,
                                                    boolean multiValued)
            throws RepositoryException {
        throw new IllegalStateException("InternalVersionManager should never create a VirtualPropertyState");
    }

    /**
     * @inheritDoc
     */
    public VirtualNodeState createNodeState(VirtualNodeState parent, Name name,
                                            NodeId id, Name nodeTypeName)
            throws RepositoryException {
        throw new IllegalStateException("InternalVersionManager should never create a VirtualNodeState");
    }

    /**
     * @inheritDoc
     */
    public synchronized ItemState getItemState(ItemId id)
            throws NoSuchItemStateException, ItemStateException {
        ItemState item = items.get(id);
        if (item == null) {
            item = stateMgr.getItemState(id);
            items.put(id, item);
        }
        return item;
    }

    /**
     * @inheritDoc
     */
    public boolean setNodeReferences(ChangeLog references) {
        return stateMgr.setNodeReferences(references);
    }

    /**
     * @inheritDoc
     */
    public boolean hasItemState(ItemId id) {
        return items.get(id) != null || stateMgr.hasItemState(id);
    }

    /**
     * @inheritDoc
     */
    public NodeReferences getNodeReferences(NodeId id)
            throws NoSuchItemStateException, ItemStateException {
        return stateMgr.getNodeReferences(id);
    }

    /**
     * @inheritDoc
     */
    public boolean hasNodeReferences(NodeId id) {
        return stateMgr.hasNodeReferences(id);
    }

    /**
     * {@inheritDoc}
     */
    public void addListener(ItemStateListener listener) {
        stateMgr.addListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    public void removeListener(ItemStateListener listener) {
        stateMgr.removeListener(listener);
    }

    //-------------------------------------------------< ItemStateListener >---

    /**
     * @inheritDoc
     */
    public void stateCreated(ItemState created) {
        // ignore
    }

    /**
     * @inheritDoc
     */
    public void stateModified(ItemState modified) {
        // ignore
    }

    /**
     * @inheritDoc
     */
    public void stateDestroyed(ItemState destroyed) {
        items.remove(destroyed.getId());
    }

    /**
     * @inheritDoc
     */
    public void stateDiscarded(ItemState discarded) {
        items.remove(discarded.getId());
    }
}
