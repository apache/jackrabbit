/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.observation.DelegatingObservationDispatcher;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.virtual.VirtualItemStateProvider;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.state.PersistenceManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeReferences;
import org.apache.jackrabbit.core.state.NodeReferencesId;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.MalformedPathException;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionException;
import javax.jcr.version.Version;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This Class implements a VersionManager.
 */
public class VersionManagerImpl extends AbstractVersionManager
        /*implements EventStateCollectionFactory*/ {

    /**
     * the default logger
     */
    private static Logger log = LoggerFactory.getLogger(VersionManager.class);

    /**
     * The path to the version storage: /jcr:system/jcr:versionStorage
     */
    private static final Path VERSION_STORAGE_PATH;

    static {
        try {
            Path.PathBuilder builder = new Path.PathBuilder();
            builder.addRoot();
            builder.addLast(QName.JCR_SYSTEM);
            builder.addLast(QName.JCR_VERSIONSTORAGE);
            VERSION_STORAGE_PATH = builder.getPath();
        } catch (MalformedPathException e) {
            // will not happen. path is always valid
            throw new InternalError("Cannot initialize path");
        }
    }

    /**
     * The persistence manager for the versions
     */
    private final PersistenceManager pMgr;

    /**
     * The file system for this version manager
     */
    private final FileSystem fs;

    /**
     * the shared state manager for the version storage
     */
    private SharedItemStateManager sharedStateMgr;

    /**
     * the virtual item state provider that exposes the version storage
     */
    private final VersionItemStateProvider versProvider;

    /**
     * the node type manager
     */
    private NodeTypeRegistry ntReg;

    /**
     * the dynamic event state collection factory
     */
    private final DynamicESCFactory escFactory;

    /**
     * Map of returned items. this is kept for invalidating
     */
    private ReferenceMap versionItems = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);

    /**
     * Creates a new version manager
     *
     */
    public VersionManagerImpl(PersistenceManager pMgr, FileSystem fs,
                              NodeTypeRegistry ntReg,
                              DelegatingObservationDispatcher obsMgr, NodeId rootId,
                              NodeId rootParentId) throws RepositoryException {
        try {
            this.pMgr = pMgr;
            this.fs = fs;
            this.ntReg = ntReg;
            this.escFactory = new DynamicESCFactory(obsMgr);

            // need to store the version storage root directly into the persistence manager
            if (!pMgr.exists(rootId)) {
                NodeState root = pMgr.createNew(rootId);
                root.setParentId(rootParentId);
                root.setDefinitionId(ntReg.getEffectiveNodeType(QName.REP_SYSTEM).getApplicableChildNodeDef(
                        QName.JCR_VERSIONSTORAGE, QName.REP_VERSIONSTORAGE).getId());
                root.setNodeTypeName(QName.REP_VERSIONSTORAGE);
                PropertyState pt = pMgr.createNew(new PropertyId(rootId, QName.JCR_PRIMARYTYPE));
                pt.setDefinitionId(ntReg.getEffectiveNodeType(QName.REP_SYSTEM).getApplicablePropertyDef(
                        QName.JCR_PRIMARYTYPE, PropertyType.NAME, false).getId());
                pt.setMultiValued(false);
                pt.setType(PropertyType.NAME);
                pt.setValues(new InternalValue[]{InternalValue.create(QName.REP_VERSIONSTORAGE)});
                root.addPropertyName(pt.getName());
                ChangeLog cl = new ChangeLog();
                cl.added(root);
                cl.added(pt);
                pMgr.store(cl);
            }
            sharedStateMgr =
                    new VersionItemStateManager(pMgr, rootId, ntReg);
            stateMgr = new LocalItemStateManager(sharedStateMgr, escFactory);
            NodeState nodeState = (NodeState) stateMgr.getItemState(rootId);
            historyRoot = new NodeStateEx(stateMgr, ntReg, nodeState, QName.JCR_VERSIONSTORAGE);

            // create the virtual item state provider
            versProvider = new VersionItemStateProvider(this, sharedStateMgr);

        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public VirtualItemStateProvider getVirtualItemStateProvider() {
        return versProvider;
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws Exception {
        pMgr.close();
        fs.close();
    }

    /**
     * Returns the event state collection factory.
     * @return the event state collection factory.
     */
    public DynamicESCFactory getEscFactory() {
        return escFactory;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public VersionHistory createVersionHistory(Session session, final NodeState node)
            throws RepositoryException {

        InternalVersionHistory history = (InternalVersionHistory)
                escFactory.doSourced((SessionImpl) session, new SourcedTarget(){
            public Object run() throws RepositoryException {
                return createVersionHistory(node);
            }
        });

        if (history == null) {
            throw new VersionException("History already exists for node " + node.getNodeId());
        }
        return (VersionHistory) ((SessionImpl) session).getNodeById(history.getId());
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasItem(NodeId id) {
        return versionItems.containsKey(id) || stateMgr.hasItemState(id);
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized InternalVersionItem getItem(NodeId id)
            throws RepositoryException {

        try {
            InternalVersionItem item = (InternalVersionItem) versionItems.get(id);
            if (item == null) {
                if (stateMgr.hasItemState(id)) {
                    NodeState state = (NodeState) stateMgr.getItemState(id);
                    NodeStateEx pNode = new NodeStateEx(stateMgr, ntReg, state, null);
                    NodeId parentId = pNode.getParentId();
                    InternalVersionItem parent =
                            (parentId != null) ? getItem(parentId) : null;
                    QName ntName = state.getNodeTypeName();
                    if (ntName.equals(QName.NT_FROZENNODE)) {
                        item = new InternalFrozenNodeImpl(this, pNode, parent);
                    } else if (ntName.equals(QName.NT_VERSIONEDCHILD)) {
                        item = new InternalFrozenVHImpl(this, pNode, parent);
                    } else if (ntName.equals(QName.NT_VERSION)) {
                        item = ((InternalVersionHistory) parent).getVersion(id);
                    } else if (ntName.equals(QName.NT_VERSIONHISTORY)) {
                        item = new InternalVersionHistoryImpl(this, pNode);
                    } else {
                        //return null;
                    }
                }
                if (item != null) {
                    versionItems.put(id, item);
                }
            }
            return item;
        } catch (ItemStateException e) {
            throw new RepositoryException(e);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public Version checkin(final NodeImpl node) throws RepositoryException {
        InternalVersion version = (InternalVersion)
                escFactory.doSourced((SessionImpl) node.getSession(), new SourcedTarget(){
            public Object run() throws RepositoryException {
                String histUUID = node.getProperty(QName.JCR_VERSIONHISTORY).getString();
                return checkin((InternalVersionHistoryImpl) 
                        getVersionHistory(NodeId.valueOf(histUUID)), node);
            }
        });

        return (AbstractVersion)
                ((SessionImpl) node.getSession()).getNodeById(version.getId());
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public void removeVersion(VersionHistory history, final QName name)
            throws VersionException, RepositoryException {

        final AbstractVersionHistory historyImpl = (AbstractVersionHistory) history;
        if (!historyImpl.hasNode(name)) {
            throw new VersionException("Version with name " + name.toString()
                    + " does not exist in this VersionHistory");
        }

        escFactory.doSourced((SessionImpl) history.getSession(), new SourcedTarget(){
            public Object run() throws RepositoryException {
                InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl)
                        historyImpl.getInternalVersionHistory();
                removeVersion(vh, name);
                return null;
            }
        });
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This method must not be synchronized since it could cause deadlocks with
     * item-reading listeners in the observation thread.
     */
    public Version setVersionLabel(final VersionHistory history,
                                   final QName version, final QName label,
                                   final boolean move)
            throws RepositoryException {

        InternalVersion v = (InternalVersion)
                escFactory.doSourced((SessionImpl) history.getSession(), new SourcedTarget(){
            public Object run() throws RepositoryException {
                InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl)
                        ((AbstractVersionHistory) history).getInternalVersionHistory();
                return setVersionLabel(vh, version, label, move);
            }
        });

        if (v == null) {
            return null;
        } else {
            return (Version)
                    ((SessionImpl) history.getSession()).getNodeByUUID(v.getId().getUUID());
        }
    }

    /**
     * Invoked by some external source to indicate that some items in the
     * versions tree were updated. Version manager should flush its own
     * caches.
     * @param items items updated
     */
    public void itemsUpdated(Collection items) {
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            itemUpdated((InternalVersionItem) iter.next());
        }
    }

    /**
     * Update internal version item. Version histories are reloaded if possible.
     * Matching items are removed from the cache.
     * @param item item updated
     */
    private void itemUpdated(InternalVersionItem item) {
        InternalVersionItem cached = (InternalVersionItem) versionItems.remove(item.getId());
        if (cached != null) {
            if (cached instanceof InternalVersionHistoryImpl) {
                InternalVersionHistoryImpl vh = (InternalVersionHistoryImpl) cached;
                try {
                    vh.reload();
                    versionItems.put(vh.getId(), vh);
                } catch (RepositoryException e) {
                    log.warn("Unable to update version history: " + e.toString());
                }
            }
        }
    }

    public boolean setNodeReferences(NodeReferences refs) {
        try {
            InternalVersionItem item = getItem(refs.getTargetId());
            setItemReferences(item, refs.getReferences());
            return true;
        } catch (RepositoryException e) {
            log.error("Error while setting references: " + e.toString());
            return false;
        }
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
            if (!hasItem(id.getParentId())) {
                refs.add(id);
            }
        }
        internalSetItemReferences(item, refs);
    }

    /**
     * {@inheritDoc}
     */
    protected List getItemReferences(InternalVersionItem item) {
        try {
            NodeReferences refs = pMgr.load(new NodeReferencesId(item.getId()));
            return refs.getReferences();
        } catch (ItemStateException e) {
            // ignore
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * {@inheritDoc}
     */
    private void internalSetItemReferences(InternalVersionItem item, List references) {
        try {
            ChangeLog log = new ChangeLog();
            NodeReferences refs = new NodeReferences(new NodeReferencesId(item.getId()));
            refs.addAllReferences(references);
            log.modified(refs);
            pMgr.store(log);
        } catch (ItemStateException e) {
            log.error("Error while storing", e);
        }
    }

    /**
     * returns the id of the version history root node
     *
     * @return the id of the version history root node
     */
    NodeId getHistoryRootId() {
        return historyRoot.getState().getNodeId();
    }

    /**
     * Return the shared item state manager.
     */
    SharedItemStateManager getSharedStateMgr() {
        return sharedStateMgr;
    }

    //--------------------------------------------------------< inner classes >
    /**
     * Spezialized SharedItemStateManager that filters out NodeReferences to
     * non-versioning states.
     */
    class VersionItemStateManager extends SharedItemStateManager {

        public VersionItemStateManager(PersistenceManager persistMgr,
                                       NodeId rootNodeId,
                                       NodeTypeRegistry ntReg)
                throws ItemStateException {
            super(persistMgr, rootNodeId, ntReg, false);
        }

        protected void checkReferentialIntegrity(ChangeLog changes)
                throws ReferentialIntegrityException, ItemStateException {
            // only store VV-type references and NV-type references

            // check whether targets of modified node references exist
            for (Iterator iter = changes.modifiedRefs(); iter.hasNext();) {
                NodeReferences refs = (NodeReferences) iter.next();
                NodeId id = refs.getTargetId();
                // no need to check existence of target if there are no references
                if (refs.hasReferences()) {
                    if (!changes.has(id) && !hasItemState(id)) {
                        // remove references
                        iter.remove();
                    }
                }
            }
        }

    }

    public static final class DynamicESCFactory implements EventStateCollectionFactory {

        /**
         * the observation manager
         */
        private DelegatingObservationDispatcher obsMgr;

        /**
         * the current event source
         */
        private SessionImpl source;


        /**
         * Creates a new event state collection factory
         * @param obsMgr
         */
        public DynamicESCFactory(DelegatingObservationDispatcher obsMgr) {
            this.obsMgr = obsMgr;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * This object uses one instance of a <code>LocalItemStateManager</code>
         * to update data on behalf of many sessions. In order to maintain the
         * association between update operation and session who actually invoked
         * the update, an internal event source is used.
         */
        public synchronized EventStateCollection createEventStateCollection()
                throws RepositoryException {
            if (source == null) {
                throw new RepositoryException("Unknown event source.");
            }
            return createEventStateCollection(source);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * This object uses one instance of a <code>LocalItemStateManager</code>
         * to update data on behalf of many sessions. In order to maintain the
         * association between update operation and session who actually invoked
         * the update, an internal event source is used.
         */
        public EventStateCollection createEventStateCollection(SessionImpl source) {
            return obsMgr.createEventStateCollection(source, VERSION_STORAGE_PATH);
        }

        /**
         * Executes the given runnable using the given event source.
         *
         * @param eventSource
         * @param runnable
         * @throws RepositoryException
         */
        public synchronized Object doSourced(SessionImpl eventSource, SourcedTarget runnable)
                throws RepositoryException {
            this.source = eventSource;
            try {
                return runnable.run();
            } finally {
                this.source = null;
            }
        }
    }

    private abstract class SourcedTarget {
        public abstract Object run() throws RepositoryException;
    }
}
