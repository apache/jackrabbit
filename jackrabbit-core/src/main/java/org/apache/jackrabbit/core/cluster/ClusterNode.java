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
package org.apache.jackrabbit.core.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.core.config.ClusterConfig;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

import javax.jcr.observation.Event;
import javax.jcr.Session;
import javax.jcr.RepositoryException;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;

/**
 * Default clustered node implementation.
 */
public class ClusterNode implements Runnable, UpdateEventChannel,
        NamespaceEventChannel, NodeTypeEventChannel  {

    /**
     * System property specifying a node id to use.
     */
    public static final String SYSTEM_PROPERTY_NODE_ID = "org.apache.jackrabbit.core.cluster.node_id";

    /**
     * Used for padding short string representations.
     */
    private static final String SHORT_PADDING = "0000";

    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(ClusterNode.class);

    /**
     * Cluster context.
     */
    private ClusterContext clusterContext;

    /**
     * Cluster node id.
     */
    private String clusterNodeId;

    /**
     * Synchronization delay, in seconds.
     */
    private int syncDelay;

    /**
     * Journal used.
     */
    private Journal journal;

    /**
     * Mutex used when syncing.
     */
    private final Mutex syncLock = new Mutex();

    /**
     * Flag indicating whether this cluster node is stopped.
     */
    private boolean stopped;

    /**
     * Map of available lock listeners, indexed by workspace name.
     */
    private final Map wspLockListeners = new HashMap();

    /**
     * Map of available update listeners, indexed by workspace name.
     */
    private final Map wspUpdateListeners = new HashMap();

    /**
     * Versioning update listener.
     */
    private UpdateEventListener versionUpdateListener;

    /**
     * Namespace listener.
     */
    private NamespaceEventListener namespaceListener;

    /**
     * Node type listener.
     */
    private NodeTypeEventListener nodeTypeListener;

    /**
     * Initialize this cluster node.
     *
     * @throws ClusterException if an error occurs
     */
    public void init(ClusterContext clusterContext) throws ClusterException {
        this.clusterContext = clusterContext;

        init();
    }

    /**
     * Initialize this cluster node (overridable).
     *
     * @throws ClusterException if an error occurs
     */
    protected void init() throws ClusterException {
        ClusterConfig cc = clusterContext.getClusterConfig();
        clusterNodeId = getClusterNodeId(cc.getId());
        syncDelay = cc.getSyncDelay();

        try {
            journal = (Journal) cc.getJournalConfig().newInstance();
            journal.init(clusterNodeId, new SyncListener(), clusterContext.getNamespaceResovler());
        } catch (ConfigurationException e) {
            throw new ClusterException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Starts this cluster node.
     *
     * @throws ClusterException if an error occurs
     */
    public synchronized void start() throws ClusterException {
        sync();

        Thread t = new Thread(this, "ClusterNode-" + clusterNodeId);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Run loop that will sync this node after some delay.
     */
    public void run() {
        for (;;) {
            synchronized (this) {
                try {
                    wait(syncDelay * 1000);
                } catch (InterruptedException e) {}

                if (stopped) {
                    return;
                }
            }
            try {
                sync();
            } catch (ClusterException e) {
                String msg = "Periodic sync of journal failed: " + e.getMessage();
                log.error(msg);
            }
        }
    }

    /**
     * Synchronize contents from journal.
     *
     * @throws ClusterException if an error occurs
     */
    public void sync() throws ClusterException {
        try {
            syncLock.acquire();
        } catch (InterruptedException e) {
            String msg = "Interrupted while waiting for mutex.";
            throw new ClusterException(msg);
        }
        try {
            journal.sync();
        } finally {
            syncLock.release();
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void stop() {
        stopped = true;

        notifyAll();
    }

    /**
     * Called when a node has been locked.
     *
     * @param workspace workspace name
     * @param nodeId node id
     * @param deep flag indicating whether lock is deep
     * @param owner lock owner
     */
    private void locked(String workspace, NodeId nodeId, boolean deep, String owner) {
        try {
            journal.begin(workspace);
            journal.log(nodeId, deep, owner);
            journal.prepare();
            journal.commit();
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while creating log entry.";
            log.error(msg, e);
        }
    }

    /**
     * Called when a node has been unlocked.
     *
     * @param workspace workspace name
     * @param nodeId node id
     */
    private void unlocked(String workspace, NodeId nodeId) {
        try {
            journal.begin(workspace);
            journal.log(nodeId);
            journal.prepare();
            journal.commit();
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while creating log entry.";
            log.error(msg, e);
        }
    }

    /**
     * Create an {@link UpdateEventChannel} for some workspace.
     *
     * @param workspace workspace name
     * @return lock event channel
     */
    public UpdateEventChannel createUpdateChannel(String workspace) {
        return new WorkspaceUpdateChannel(workspace);
    }

    /**
     * Create a {@link LockEventChannel} for some workspace.
     *
     * @param workspace workspace name
     * @return lock event channel
     */
    public LockEventChannel createLockChannel(String workspace) {
        return new WorkspaceLockChannel(workspace);
    }

    /**
     * Return the instance id to be used for this node in the cluster.
     *
     * @param id configured id, <code>null</code> to take random id
     */
    private String getClusterNodeId(String id) {
        if (id == null) {
            id = System.getProperty(SYSTEM_PROPERTY_NODE_ID);
            if (id == null) {
                id = toHexString((short) (Math.random() * (Short.MAX_VALUE - Short.MIN_VALUE)));
            }
        }
        return id;
    }

    /**
     * Return a zero-padded short string representation.
     *
     * @param n short
     * @return string representation
     */
    private static String toHexString(short n) {
        String s = Integer.toHexString(n);
        int padlen = SHORT_PADDING.length() - s.length();
        if (padlen < 0) {
            s = s.substring(-padlen);
        } else if (padlen > 0) {
            s = SHORT_PADDING.substring(0, padlen) + s;
        }
        return s;
    }

    //--------------------------------------------------< UpdateEventListener >

    /**
     * {@inheritDoc}
     * <p/>
     * Invoked when an update has been created inside versioning. Delegate
     * to common method with <code>null</code> workspace.
     */
    public void updateCreated(ChangeLog changes, EventStateCollection esc) {
        updateCreated(null, changes, esc);
    }

    /**
     * Called when an a update operation has been created.
     *
     * @param workspace workspace to use when writing journal entry
     * @param changes changes
     * @param esc events as they will be delivered on success
     */
    private void updateCreated(String workspace, ChangeLog changes, EventStateCollection esc) {
        try {
            journal.begin(workspace);
            journal.log(changes, esc);
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while creating log entry.";
            log.error(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updatePrepared() {
        try {
            journal.prepare();
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while preparing log entry.";
            log.error(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateCommitted() {
        try {
            journal.commit();
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while committing log entry.";
            log.error(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateCancelled() {
        try {
            journal.cancel();
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while cancelling log entry.";
            log.error(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Invoked to set the update event listener responsible for delivering versioning events.
     */
    public void setListener(UpdateEventListener listener) {
        versionUpdateListener = listener;
    }

    //-----------------------------------------------< NamespaceEventListener >

    /**
     * {@inheritDoc}
     */
    public void remapped(String oldPrefix, String newPrefix, String uri) {
        try {
            journal.begin(null);
            journal.log(oldPrefix, newPrefix, uri);
            journal.prepare();
            journal.commit();
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while creating log entry.";
            log.error(msg, e);
        }
    }

    public void setListener(NamespaceEventListener listener) {
        namespaceListener = listener;
    }

    //------------------------------------------------< NodeTypeEventListener >

    /**
     * {@inheritDoc}
     */
    public void registered(Collection ntDefs) {
        try {
            journal.begin(null);
            journal.log(ntDefs);
            journal.prepare();
            journal.commit();
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while creating log entry.";
            log.error(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setListener(NodeTypeEventListener listener) {
        nodeTypeListener = listener;
    }


    /**
     * Workspace update channel.
     */
    class WorkspaceUpdateChannel implements UpdateEventChannel {

        /**
         * Workspace name.
         */
        private final String workspace;

        /**
         * Create a new instance of this class.
         *
         * @param workspace workspace name
         */
        public WorkspaceUpdateChannel(String workspace) {
            this.workspace = workspace;
        }

        /**
         * {@inheritDoc}
         */
        public void updateCreated(ChangeLog changes, EventStateCollection esc) {
            ClusterNode.this.updateCreated(workspace, changes, esc);
        }

        /**
         * {@inheritDoc}
         */
        public void updatePrepared() {
            ClusterNode.this.updatePrepared();
        }

        /**
         * {@inheritDoc}
         */
        public void updateCommitted() {
            ClusterNode.this.updateCommitted();
        }

        /**
         * {@inheritDoc}
         */
        public void updateCancelled() {
            ClusterNode.this.updateCancelled();
        }

        /**
         * {@inheritDoc}
         */
        public void setListener(UpdateEventListener listener) {
            wspUpdateListeners.remove(workspace);
            if (listener != null) {
                wspUpdateListeners.put(workspace, listener);
            }
        }
    }

    /**
     * Workspace lock channel.
     */
    class WorkspaceLockChannel implements LockEventChannel {

        /**
         * Workspace name.
         */
        private final String workspace;

        /**
         * Create a new instance of this class.
         *
         * @param workspace workspace name
         */
        public WorkspaceLockChannel(String workspace) {
            this.workspace = workspace;
        }

        /**
         * {@inheritDoc}
         */
        public void locked(NodeId nodeId, boolean deep, String owner) {
            ClusterNode.this.locked(workspace, nodeId, deep, owner);
        }

        /**
         * {@inheritDoc}
         */
        public void unlocked(NodeId nodeId) {
            ClusterNode.this.unlocked(workspace, nodeId);
        }

        /**
         * {@inheritDoc}
         */
        public void setListener(LockEventListener listener) {
            wspLockListeners.remove(workspace);
            if (listener != null) {
                wspLockListeners.put(workspace, listener);
            }
        }
    }

    /**
     * Sync listener on journal.
     */
    class SyncListener implements RecordProcessor {

        /**
         * Workspace name.
         */
        private String workspace;

        /**
         * Change log.
         */
        private ChangeLog changeLog;

        /**
         * List of recorded events.
         */
        private List events;

        /**
         * Last used session for event sources.
         */
        private Session lastSession;

        /**
         * {@inheritDoc}
         */
        public void start(String workspace) {
            this.workspace = workspace;

            changeLog = new ChangeLog();
            events = new ArrayList();
        }

        /**
         * {@inheritDoc}
         */
        public void process(ItemOperation operation) {
            operation.apply(changeLog);
        }

        /**
         * {@inheritDoc}
         */
        public void process(int type, NodeId parentId, Path parentPath, NodeId childId,
                            Path.PathElement childRelPath, QName ntName, Set mixins, String userId) {

            EventState event = null;

            switch (type) {
                case Event.NODE_ADDED:
                    event = EventState.childNodeAdded(parentId, parentPath, childId, childRelPath,
                            ntName, mixins, getOrCreateSession(userId));
                    break;
                case Event.NODE_REMOVED:
                    event = EventState.childNodeRemoved(parentId, parentPath, childId, childRelPath,
                            ntName, mixins, getOrCreateSession(userId));
                    break;
                case Event.PROPERTY_ADDED:
                    event = EventState.propertyAdded(parentId, parentPath, childRelPath,
                            ntName, mixins, getOrCreateSession(userId));
                    break;
                case Event.PROPERTY_CHANGED:
                    event = EventState.propertyChanged(parentId, parentPath, childRelPath,
                            ntName, mixins, getOrCreateSession(userId));
                    break;
                case Event.PROPERTY_REMOVED:
                    event = EventState.propertyRemoved(parentId, parentPath, childRelPath,
                            ntName, mixins, getOrCreateSession(userId));
                    break;
                default:
                    String msg = "Unexpected event type: " + type;
                    log.warn(msg);
            }
            events.add(event);
        }

        /**
         * {@inheritDoc}
         */
        public void process(NodeId nodeId, boolean isDeep, String owner) {
            LockEventListener listener = (LockEventListener) wspLockListeners.get(workspace);
            if (listener == null) {
                try {
                    clusterContext.lockEventsReady(workspace);
                } catch (RepositoryException e) {
                    String msg = "Unable to make lock listener for workspace " +
                            workspace + " online: " + e.getMessage();
                    log.warn(msg);
                }
                listener = (LockEventListener) wspLockListeners.get(workspace);
                if (listener ==  null) {
                    String msg = "Lock channel unavailable for workspace: " + workspace;
                    log.error(msg);
                    return;
                }
            }
            try {
                listener.externalLock(nodeId, isDeep, owner);
            } catch (RepositoryException e) {
                String msg = "Unable to deliver lock event: " + e.getMessage();
                log.error(msg);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void process(NodeId nodeId) {
            LockEventListener listener = (LockEventListener) wspLockListeners.get(workspace);
            if (listener == null) {
                try {
                    clusterContext.lockEventsReady(workspace);
                } catch (RepositoryException e) {
                    String msg = "Unable to make lock listener for workspace " +
                            workspace + " online: " + e.getMessage();
                    log.warn(msg);
                }
                listener = (LockEventListener) wspLockListeners.get(workspace);
                if (listener ==  null) {
                    String msg = "Lock channel unavailable for workspace: " + workspace;
                    log.error(msg);
                    return;
                }
            }
            try {
                listener.externalUnlock(nodeId);
            } catch (RepositoryException e) {
                String msg = "Unable to deliver lock event: " + e.getMessage();
                log.error(msg);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void process(String oldPrefix, String newPrefix, String uri) {
            if (namespaceListener == null) {
                String msg = "Namespace listener unavailable.";
                log.error(msg);
                return;
            }
            try {
                namespaceListener.externalRemap(oldPrefix, newPrefix, uri);
            } catch (RepositoryException e) {
                String msg = "Unable to deliver namespace operation: " + e.getMessage();
                log.error(msg);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void process(Collection ntDefs) {
            if (nodeTypeListener == null) {
                String msg = "NodeType listener unavailable.";
                log.error(msg);
                return;
            }
            try {
                nodeTypeListener.externalRegistered(ntDefs);
            } catch (InvalidNodeTypeDefException e) {
                String msg = "Unable to deliver node type operation: " + e.getMessage();
                log.error(msg);
            } catch (RepositoryException e) {
                String msg = "Unable to deliver node type operation: " + e.getMessage();
                log.error(msg);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void end() {
            UpdateEventListener listener = null;
            if (workspace != null) {
                listener = (UpdateEventListener) wspUpdateListeners.get(workspace);
                if (listener == null) {
                    try {
                        clusterContext.updateEventsReady(workspace);
                    } catch (RepositoryException e) {
                        String msg = "Error making update listener for workspace " +
                                workspace + " online: " + e.getMessage();
                        log.warn(msg);
                    }
                    listener = (UpdateEventListener) wspUpdateListeners.get(workspace);
                    if (listener ==  null) {
                        String msg = "Update listener unavailable for workspace: " + workspace;
                        log.error(msg);
                        return;
                    }
                }
            } else {
                if (versionUpdateListener != null) {
                    listener = versionUpdateListener;
                } else {
                    String msg = "Version update listener unavailable.";
                    log.error(msg);
                    return;
                }
            }
            try {
                listener.externalUpdate(changeLog, events);
            } catch (RepositoryException e) {
                String msg = "Unable to deliver update events: " + e.getMessage();
                log.error(msg);
            }
        }

        /**
         * Return a session matching a certain user id.
         *
         * @param userId user id
         * @return session
         */
        private Session getOrCreateSession(String userId) {
            if (lastSession == null || !lastSession.getUserID().equals(userId)) {
                lastSession = new ClusterSession(userId);
            }
            return lastSession;
        }
    }
}
