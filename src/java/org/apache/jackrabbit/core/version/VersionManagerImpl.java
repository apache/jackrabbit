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
package org.apache.jackrabbit.core.version;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.core.*;
import org.apache.jackrabbit.core.state.ItemStateProvider;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.Version;
import java.util.Iterator;

/**
 * This Class implements a VersionManager. It more or less acts as proxy
 * between the virtual item state manager that exposes the version to the
 * version storage ({@link VersionItemStateProvider}) and the persistent
 * version manager.
 */
public class VersionManagerImpl implements VersionManager {

    /**
     * the default logger
     */
    private static Logger log = Logger.getLogger(VersionManager.class);
    /**
     * The version manager of the internal versions
     */
    private final PersistentVersionManager vMgr;
    /**
     * The virtual item manager that exposes the versions to the content
     */
    private VersionItemStateProvider virtProvider;
    /**
     * the node type manager
     */
    private NodeTypeManagerImpl ntMgr;


    /**
     * Creates a bew vesuion manager
     * @param vMgr
     */
    public VersionManagerImpl(PersistentVersionManager vMgr) {
        this.vMgr = vMgr;
    }

    /**
     * returns the virtual item state provider that exposes the internal versions
     * as items.
     *
     * @param base
     * @return
     */
    public synchronized VirtualItemStateProvider getVirtualItemStateProvider(SessionImpl session, ItemStateProvider base) {
        if (virtProvider == null) {
            try {
                // init the definition id mgr
                ntMgr = session.getNodeTypeManager();
                ntMgr.getNodeType(NodeTypeRegistry.NT_BASE).getApplicablePropertyDef(ItemImpl.PROPNAME_PRIMARYTYPE, PropertyType.NAME, false).unwrap();
                // check, if workspace of session has history root
                NodeImpl systemRoot = ((RepositoryImpl) session.getRepository()).getSystemRootNode(session);
                if (!systemRoot.hasNode(VersionManager.NODENAME_HISTORY_ROOT)) {
                    // if not exist, create
                    systemRoot.addNode(VersionManager.NODENAME_HISTORY_ROOT, NodeTypeRegistry.NT_UNSTRUCTURED);
                }
                systemRoot.save();
                String rootId = systemRoot.getNode(VersionManager.NODENAME_HISTORY_ROOT).internalGetUUID();

                NodeState virtRootState = (NodeState) base.getItemState(new NodeId(rootId));
                virtProvider = new VersionItemStateProvider(this, ntMgr, rootId, virtRootState.getParentUUID());
            } catch (Exception e) {
                // todo: better error handling
                log.error("Error while initializing virtual items.", e);
                throw new IllegalStateException(e.toString());
            }
        }
        return virtProvider;
    }

    /**
     * returns the node type manager
     * @return
     */
    NodeTypeManagerImpl getNodeTypeManager() {
        return ntMgr;
    }

    /**
     * Creates a new version history. This action is needed either when creating
     * a new 'mix:versionable' node or when adding the 'mix:versionalbe' mixin
     * to a node.
     *
     * @param node
     * @return
     * @throws javax.jcr.RepositoryException
     */
    public VersionHistory createVersionHistory(NodeImpl node) throws RepositoryException {
        InternalVersionHistory history = vMgr.createVersionHistory(node);
        return (VersionHistory) node.getSession().getNodeByUUID(history.getId());
    }

    /**
     * Returns the base version of the given node. assuming mix:versionable
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public Version getBaseVersion(NodeImpl node) throws RepositoryException {
        String histUUID = node.getProperty(VersionManager.PROPNAME_VERSION_HISTORY).getString();
        InternalVersionHistory history = vMgr.getVersionHistory(histUUID);
        InternalVersion version = history.getVersion(node.getProperty(PROPNAME_BASE_VERSION).getString());
        return version == null ? null : (Version) node.getSession().getNodeByUUID(version.getId());
    }

    /**
     * Returns the version history for the given node. assuming mix:versionable
     * and version history set in property
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public VersionHistory getVersionHistory(NodeImpl node)
            throws RepositoryException {
        String histUUID = node.getProperty(VersionManager.PROPNAME_VERSION_HISTORY).getString();
        InternalVersionHistory history = vMgr.getVersionHistory(histUUID);
        return (VersionHistory) node.getSession().getNodeByUUID(history.getId());
    }

    /**
     * Checks if the version history with the given id exists
     *
     * @param id
     * @return
     */
    public boolean hasVersionHistory(String id) {
        return vMgr.hasVersionHistory(id);
    }

    /**
     * Returns the version history with the given id
     *
     * @param id
     * @return
     * @throws RepositoryException
     */
    public InternalVersionHistory getVersionHistory(String id) throws RepositoryException {
        return vMgr.getVersionHistory(id);
    }

    /**
     * Returns the number of version histories
     *
     * @return
     * @throws RepositoryException
     */
    public int getNumVersionHistories() throws RepositoryException {
        return vMgr.getNumVersionHistories();
    }

    /**
     * Returns an iterator over all ids of {@link InternalVersionHistory}s.
     *
     * @return
     * @throws RepositoryException
     */
    public Iterator getVersionHistoryIds() throws RepositoryException {
        return vMgr.getVersionHistoryIds();
    }

    /**
     * Checks if the version with the given id exists
     *
     * @param id
     * @return
     */
    public boolean hasVersion(String id) {
        return vMgr.hasVersion(id);
    }

    /**
     * Returns the version with the given id
     *
     * @param id
     * @return
     * @throws RepositoryException
     */
    public InternalVersion getVersion(String id) throws RepositoryException {
        return vMgr.getVersion(id);
    }

    /**
     * checks, if the node with the given id exists
     * @param id
     * @return
     */
    public boolean hasItem(String id) {
        return vMgr.hasItem(id);
    }

    /**
     * Returns the version item with the given id
     * @param id
     * @return
     * @throws RepositoryException
     */
    public InternalVersionItem getItem(String id) throws RepositoryException {
        return vMgr.getItemByExternal(id);
    }

    /**
     * invokes the checkin() on the persistent version manager and remaps the
     * newly created version objects.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    public Version checkin(NodeImpl node) throws RepositoryException {
        InternalVersion version = vMgr.checkin(node);
        return (Version) node.getSession().getNodeByUUID(version.getId());
    }


}
