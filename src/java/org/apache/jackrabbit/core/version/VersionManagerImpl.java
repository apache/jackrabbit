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
package org.apache.jackrabbit.core.version;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.Path;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.observation.DelegatingObservationDispatcher;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.state.ItemStateManager;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.log4j.Logger;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyIterator;
import javax.jcr.NodeIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * This Class implements a VersionManager. It more or less acts as proxy
 * between the virtual item state manager that exposes the version to the
 * version storage ({@link VersionItemStateProvider}) and the persistent
 * version manager.
 */
public class VersionManagerImpl implements VersionManager, Constants {

    /**
     * the default logger
     */
    private static Logger log = Logger.getLogger(VersionManager.class);

    /**
     * The root node UUID for the version storage
     */
    private final String VERSION_STORAGE_NODE_UUID;

    /**
     * The root parent node UUID for the version storage
     */
    private final String VERSION_STORAGE_PARENT_NODE_UUID;

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
    private NodeTypeRegistry ntReg;

    /**
     * the observation manager
     */
    private DelegatingObservationDispatcher obsMgr;

    /**
     * Creates a bew vesuion manager
     *
     * @param vMgr
     */
    public VersionManagerImpl(PersistentVersionManager vMgr, NodeTypeRegistry ntReg,
                              DelegatingObservationDispatcher obsMgr, String rootUUID,
                              String rootParentUUID) {
        this.vMgr = vMgr;
        this.ntReg = ntReg;
        this.obsMgr = obsMgr;
        this.VERSION_STORAGE_NODE_UUID = rootUUID;
        this.VERSION_STORAGE_PARENT_NODE_UUID = rootParentUUID;
    }

    /**
     * returns the virtual item state provider that exposes the internal versions
     * as items.
     *
     * @param base
     * @return
     */
    public synchronized VirtualItemStateProvider getVirtualItemStateProvider(ItemStateManager base) {
        if (virtProvider == null) {
            try {
                // init the definition id mgr
                virtProvider = new VersionItemStateProvider(this, ntReg, VERSION_STORAGE_NODE_UUID, VERSION_STORAGE_PARENT_NODE_UUID);
            } catch (Exception e) {
                // todo: better error handling
                log.error("Error while initializing virtual items.", e);
                throw new IllegalStateException(e.toString());
            }
        }
        return virtProvider;
    }

    /**
     * Close this version manager. After having closed a persistence
     * manager, further operations on this object are treated as illegal
     * and throw
     *
     * @throws Exception if an error occurs
     */
    public void close() throws Exception {
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
        virtProvider.invalidateItem(new NodeId(VERSION_STORAGE_NODE_UUID));
        VersionHistoryImpl vh = (VersionHistoryImpl) node.getSession().getNodeByUUID(history.getId());

        // generate observation events
        List events = new ArrayList();
        recursiveAdd(events, (NodeImpl) vh.getParent(), vh);
        obsMgr.dispatch(events, (SessionImpl) node.getSession());

        return vh;
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
     *
     * @param id
     * @return
     */
    public boolean hasItem(String id) {
        return vMgr.hasItem(id);
    }

    /**
     * Returns the version item with the given id
     *
     * @param id
     * @return
     * @throws RepositoryException
     */
    public InternalVersionItem getItem(String id) throws RepositoryException {
        return vMgr.getItem(id);
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
        SessionImpl session = (SessionImpl) node.getSession();
        InternalVersion version = vMgr.checkin(node);
        virtProvider.invalidateItem(new NodeId(version.getVersionHistory().getId()));
        VersionImpl v = (VersionImpl) session.getNodeByUUID(version.getId());

        // generate observation events
        List events = new ArrayList();
        recursiveAdd(events, (NodeImpl) v.getParent(), v);
        obsMgr.dispatch(events, session);

        return v;
    }

    /**
     * Removes the specified version from the history
     *
     * @param history the version history from where to remove the version.
     * @param name the name of the version to remove.
     * @throws VersionException if the version <code>history</code> does
     *  not have a version with <code>name</code>.
     * @throws RepositoryException if any other error occurs.
     */
    public void removeVersion(VersionHistory history, QName name)
            throws RepositoryException {
        if (!((VersionHistoryImpl) history).hasNode(name)) {
            throw new VersionException("Version with name " + name.toString()
                    + " does not exist in this VersionHistory");
        }
        // generate observation events
        SessionImpl session = (SessionImpl) history.getSession();
        VersionImpl version = (VersionImpl) ((VersionHistoryImpl) history).getNode(name);
        List events = new ArrayList();
        recursiveRemove(events, (NodeImpl) history, version);

        InternalVersionHistory vh = ((VersionHistoryImpl) history).getInternalVersionHistory();
        vh.removeVersion(name);

        virtProvider.invalidateItem(new NodeId(vh.getId()));
        obsMgr.dispatch(events, session);
    }

    /**
     * {@inheritDoc}
     */
    public Version setVersionLabel(VersionHistory history, QName version,
                                   QName label, boolean move)
            throws RepositoryException {
        SessionImpl session = (SessionImpl) history.getSession();

        InternalVersionHistory vh = ((VersionHistoryImpl) history).getInternalVersionHistory();
        NodeImpl labelNode = ((VersionHistoryImpl) history).getNode(JCR_VERSIONLABELS);
        InternalVersion v = vh.setVersionLabel(version, label, move);

        // collect observation events
        List events = new ArrayList();
        if (version == null && v != null) {
            // label removed
            events.add(EventState.propertyRemoved(
                    labelNode.internalGetUUID(),
                    labelNode.getPrimaryPath(),
                    Path.PathElement.fromString(label.toString()),
                    (NodeTypeImpl) labelNode.getPrimaryNodeType(),
                    labelNode.getSession()
            ));
        } else if (v == null) {
            // label added
            events.add(EventState.propertyAdded(
                    labelNode.internalGetUUID(),
                    labelNode.getPrimaryPath(),
                    Path.PathElement.fromString(label.toString()),
                    (NodeTypeImpl) labelNode.getPrimaryNodeType(),
                    labelNode.getSession()
            ));
        } else {
            // label modified
            events.add(EventState.propertyChanged(
                    labelNode.internalGetUUID(),
                    labelNode.getPrimaryPath(),
                    Path.PathElement.fromString(label.toString()),
                    (NodeTypeImpl) labelNode.getPrimaryNodeType(),
                    labelNode.getSession()
            ));
        }
        virtProvider.invalidateItem(new NodeId(vh.getId()));
        obsMgr.dispatch(events, session);
        return v == null ? null : (VersionImpl) session.getNodeByUUID(v.getId());
    }

    /**
     * Adds a subtree of itemstates as 'added' to a list of events
     *
     * @param events
     * @param parent
     * @param node
     * @throws RepositoryException
     */
    private void recursiveAdd(List events, NodeImpl parent, NodeImpl node)
            throws RepositoryException {

        events.add(EventState.childNodeAdded(
                parent.internalGetUUID(),
                parent.getPrimaryPath(),
                node.internalGetUUID(),
                node.getPrimaryPath().getNameElement(),
                (NodeTypeImpl) parent.getPrimaryNodeType(),
                node.getSession()
        ));

        PropertyIterator iter = node.getProperties();
        while (iter.hasNext()) {
            PropertyImpl prop = (PropertyImpl) iter.nextProperty();
            events.add(EventState.propertyAdded(
                    node.internalGetUUID(),
                    node.getPrimaryPath(),
                    prop.getPrimaryPath().getNameElement(),
                    (NodeTypeImpl) node.getPrimaryNodeType(),
                    node.getSession()
            ));
        }
        NodeIterator niter = node.getNodes();
        while (niter.hasNext()) {
            NodeImpl n = (NodeImpl) niter.nextNode();
            recursiveAdd(events, node, n);
        }
    }

    /**
     * Adds a subtree of itemstates as 'removed' to a list of events
     *
     * @param events
     * @param parent
     * @param node
     * @throws RepositoryException
     */
    private void recursiveRemove(List events, NodeImpl parent, NodeImpl node)
            throws RepositoryException {

        events.add(EventState.childNodeRemoved(
                parent.internalGetUUID(),
                parent.getPrimaryPath(),
                node.internalGetUUID(),
                node.getPrimaryPath().getNameElement(),
                (NodeTypeImpl) parent.getPrimaryNodeType(),
                node.getSession()
        ));
        NodeIterator niter = node.getNodes();
        while (niter.hasNext()) {
            NodeImpl n = (NodeImpl) niter.nextNode();
            recursiveRemove(events, node, n);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List getItemReferences(InternalVersionItem item) {
        return vMgr.getItemReferences(item);
    }

    /**
     * {@inheritDoc}
     */
    public void setItemReferences(InternalVersionItem item, List references) {
        // filter out version storage intern ones
        ArrayList refs = new ArrayList();
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            PropertyId id = (PropertyId) iter.next();
            if (!vMgr.hasItem(id.getParentUUID())) {
                refs.add(id);
            }
        }
        vMgr.setItemReferences(item, refs);
    }
}
