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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.cluster.WorkspaceRecord.CreateWorkspaceAction;
import org.apache.jackrabbit.core.config.ClusterConfig;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.journal.AbstractJournal;
import org.apache.jackrabbit.core.journal.InstanceRevision;
import org.apache.jackrabbit.core.journal.Journal;
import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.Record;
import org.apache.jackrabbit.core.journal.RecordConsumer;
import org.apache.jackrabbit.core.journal.RecordProducer;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.version.InternalVersionManagerImpl;
import org.apache.jackrabbit.core.xml.ClonedInputSource;
import org.apache.jackrabbit.spi.PrivilegeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import EDU.oswego.cs.dl.util.concurrent.Latch;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

/**
 * Default clustered node implementation.
 */
public class ClusterNode implements Runnable,
        NamespaceEventChannel, NodeTypeEventChannel, RecordConsumer,
        ClusterRecordProcessor, WorkspaceEventChannel, PrivilegeEventChannel  {

    /**
     * System property specifying a node id to use.
     */
    public static final String SYSTEM_PROPERTY_NODE_ID = "org.apache.jackrabbit.core.cluster.node_id";

    /**
     * Producer identifier.
     */
    private static final String PRODUCER_ID = "JR";

    /**
     * Status constant.
     */
    private static final int NONE = 0;

    /**
     * Status constant.
     */
    private static final int STARTED = 1;

    /**
     * Status constant.
     */
    private static final int STOPPED = 2;

    /**
     * Audit logger.
     */
    private static Logger auditLogger = LoggerFactory.getLogger("org.apache.jackrabbit.core.audit");

    /**
     * Default Logger.
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
     * Synchronization delay, in milliseconds.
     */
    private long syncDelay;

    /**
     * Stop delay, in milliseconds.
     */
    private long stopDelay;

    /**
     * Journal used.
     */
    private Journal journal;

    /**
     * Synchronization thread.
     */
    private Thread syncThread;

    /**
     * Mutex used when syncing.
     */
    private final Mutex syncLock = new Mutex();

    /**
     * Update counter, used in displaying the number of updates in audit log.
     */
    private final AtomicInteger updateCount = new AtomicInteger();

    /**
     * Latch used to communicate a stop request to the synchronization thread.
     */
    private final Latch stopLatch = new Latch();

    /**
     * Sync counter, used to avoid repeated sync() calls from piling up.
     * Only updated within the critical section guarded by {@link #syncLock}.
     *
     * @since Apache Jackrabbit 1.6
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1753">JCR-1753</a>
     */
    private AtomicInteger syncCount = new AtomicInteger();

    /**
     * Status flag, one of {@link #NONE}, {@link #STARTED} or {@link #STOPPED}.
     */
    private int status;

    /**
     * Map of available lock listeners, indexed by workspace name.
     */
    private final Map<String, LockEventListener> wspLockListeners = new HashMap<String, LockEventListener>();

    /**
     * Map of available update listeners, indexed by workspace name.
     */
    private final Map<String, UpdateEventListener> wspUpdateListeners = new HashMap<String, UpdateEventListener>();

    /**
     * Versioning update listener.
     */
    private UpdateEventListener versionUpdateListener;

    /**
     * Namespace listener.
     */
    private NamespaceEventListener namespaceListener;

    /**
     * Create workspace listener
     */
    private WorkspaceListener createWorkspaceListener;

    /**
     * Node type listener.
     */
    private NodeTypeEventListener nodeTypeListener;

    /**
     * Privilege listener.
     */
    private PrivilegeEventListener privilegeListener;

    /**
     * Instance revision manager.
     */
    private InstanceRevision instanceRevision;

    /**
     * Our record producer.
     */
    private RecordProducer producer;

    /**
     * Record deserializer.
     */
    private ClusterRecordDeserializer deserializer = new ClusterRecordDeserializer();
    
    /**
     * Flag indicating whether sync is manual.
     */
    private boolean disableAutoSync;

    /**
     * Initialize this cluster node.
     *
     * @param clusterContext The cluster context.
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
        clusterNodeId = cc.getId();
        syncDelay = cc.getSyncDelay();
        stopDelay = cc.getStopDelay();

        try {
            journal = cc.getJournal(clusterContext.getNamespaceResolver());
            instanceRevision = journal.getInstanceRevision();
            journal.register(this);
            producer = journal.getProducer(PRODUCER_ID);
        } catch (RepositoryException e) {
            throw new ClusterException(
                    "Cluster initialization failed: " + this, e);
        } catch (JournalException e) {
            throw new ClusterException(
                    "Journal initialization failed: " + this, e);
        }
    }

    /**
     * Set the stop delay, i.e. number of millseconds to wait for the
     * synchronization thread to stop.
     *
     * @param stopDelay stop delay in milliseconds
     */
    public void setStopDelay(long stopDelay) {
        this.stopDelay = stopDelay;
    }

    /**
     * Return the stop delay.
     *
     * @return stop delay
     * @see #setStopDelay(long)
     */
    public long getStopDelay() {
        return stopDelay;
    }
    
    /**
     * Disable periodic background synchronization. Used for testing purposes, only.
     */
    protected void disableAutoSync() {
        disableAutoSync = true;
    }

    /**
     * Starts this cluster node.
     *
     * @throws ClusterException if an error occurs
     */
    public synchronized void start() throws ClusterException {
        if (status == NONE) {
            syncOnStartup();

            if (!disableAutoSync) {
                Thread t = new Thread(this, "ClusterNode-" + clusterNodeId);
                t.setDaemon(true);
                t.start();
                syncThread = t;
            }
            status = STARTED;
        }
    }

    /**
     * Run loop that will sync this node after some delay.
     */
    public void run() {
        for (;;) {
            try {
                if (stopLatch.attempt(syncDelay)) {
                    break;
                }
            } catch (InterruptedException e) {
                String msg = "Interrupted while waiting for stop latch.";
                log.warn(msg);
            }
            try {
                sync();
            } catch (ClusterException e) {
                String msg = "Periodic sync of journal failed: " + e.getMessage();
                log.error(msg, e);
            } catch (Exception e) {
                String msg = "Unexpected exception while syncing of journal: " + e.getMessage();
                log.error(msg, e);
            } catch (Error e) {
                String msg = "Unexpected error while syncing of journal: " + e.getMessage();
                log.error(msg, e);
                throw e;
            }
        }
    }

    /** 
     * Synchronize contents from journal.
     * 
     * @param startup indicates if the cluster node is syncing on startup 
     *        or does a normal sync.
     * @throws ClusterException if an error occurs
     */
    private void internalSync(boolean startup) throws ClusterException {
        int count = syncCount.get();

        try {
            syncLock.acquire();
        } catch (InterruptedException e) {
            String msg = "Interrupted while waiting for mutex.";
            throw new ClusterException(msg);
        }

        try {
            // JCR-1753: Only synchronize if no other thread already did so
            // while we were waiting to acquire the syncLock.
            if (count == syncCount.get()) {
                syncCount.incrementAndGet();
                journal.sync(startup);
            }
        } catch (JournalException e) {
            throw new ClusterException(e.getMessage(), e.getCause());
        } finally {
            syncLock.release();
        }

    }

    /**
     * Synchronize contents from journal.
     *
     * @throws ClusterException if an error occurs
     */
    public void sync() throws ClusterException {
        internalSync(false);
    }

    /**
     * Synchronize contents from journal when a {@link ClusterNode} starts up.
     *
     * @throws ClusterException if an error occurs
     */
    public void syncOnStartup() throws ClusterException {
        internalSync(true);
    }

    /**
     * Stops this cluster node.
     */
    public synchronized void stop() {
        if (status != STOPPED) {
            status = STOPPED;

            stopLatch.release();

            // Give synchronization thread some time to finish properly before
            // closing down the journal (see JCR-1553)
            if (syncThread != null) {
                try {
                    syncThread.join(stopDelay);
                } catch (InterruptedException e) {
                    String msg = "Interrupted while joining synchronization thread.";
                    log.warn(msg);
                }
            }
            if (journal != null) {
                journal.close();
            }
            if (instanceRevision != null) {
                instanceRevision.close();
            }
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
     * Return the journal created by this cluster node.
     *
     * @return journal
     */
    public Journal getJournal() {
        return journal;
    }

    //-----------------------------------------------< NamespaceEventListener >

    /**
     * {@inheritDoc}
     */
    public void remapped(String oldPrefix, String newPrefix, String uri) {
        if (status != STARTED) {
            log.info("not started: namespace operation ignored.");
            return;
        }
        ClusterRecord record = null;
        boolean succeeded = false;

        try {
            record = new NamespaceRecord(oldPrefix, newPrefix, uri, producer.append());
            record.write();
            record.update();
            setRevision(record.getRevision());
            succeeded = true;
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while creating log entry.";
            log.error(msg, e);
        } finally {
            if (!succeeded && record != null) {
                record.cancelUpdate();
            }
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
        if (status != STARTED) {
            log.info("not started: nodetype operation ignored.");
            return;
        }
        ClusterRecord record = null;
        boolean succeeded = false;

        try {
            record = new NodeTypeRecord(ntDefs, true, producer.append());
            record.write();
            record.update();
            setRevision(record.getRevision());
            succeeded = true;
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while creating log entry.";
            log.error(msg, e);
        } finally {
            if (!succeeded && record != null) {
                record.cancelUpdate();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void reregistered(QNodeTypeDefinition ntDef) {
        if (status != STARTED) {
            log.info("not started: nodetype operation ignored.");
            return;
        }
        ClusterRecord record = null;
        boolean succeeded = false;

        try {
            record = new NodeTypeRecord(ntDef, producer.append());
            record.write();
            record.update();
            setRevision(record.getRevision());
            succeeded = true;
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while creating log entry.";
            log.error(msg, e);
        } finally {
            if (!succeeded && record != null) {
                record.cancelUpdate();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unregistered(Collection qnames) {
        if (status != STARTED) {
            log.info("not started: nodetype operation ignored.");
            return;
        }
        ClusterRecord record = null;
        boolean succeeded = false;

        try {
            record = new NodeTypeRecord(qnames, false, producer.append());
            record.write();
            record.update();
            setRevision(record.getRevision());
            succeeded = true;
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while creating log entry.";
            log.error(msg, e);
        } finally {
            if (!succeeded && record != null) {
                record.cancelUpdate();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setListener(NodeTypeEventListener listener) {
        nodeTypeListener = listener;
    }

    //----------------------------------------------< PrivilegeEventChannel >---
    /**
     * {@inheritDoc}
     * @see PrivilegeEventChannel#registeredPrivileges(java.util.Collection)
     */
    public void registeredPrivileges(Collection<PrivilegeDefinition> definitions) {
        if (status != STARTED) {
            log.info("not started: nodetype operation ignored.");
            return;
        }
        ClusterRecord record = null;
        boolean succeeded = false;

        try {
            record = new PrivilegeRecord(definitions, producer.append());
            record.write();
            record.update();
            setRevision(record.getRevision());
            succeeded = true;
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while creating log entry.";
            log.error(msg, e);
        } finally {
            if (!succeeded && record != null) {
                record.cancelUpdate();
            }
        }
    }

    public void setListener(PrivilegeEventListener listener) {
        privilegeListener = listener;
    }

    //--------------------------------------------------------------------------
    /**
     * Workspace update channel.
     */
    class WorkspaceUpdateChannel implements UpdateEventChannel {

        /**
         * Attribute name used to store record.
         */
        private static final String ATTRIBUTE_RECORD = "record";

        /**
         * Attribute name used to store the size of the update.
         */
        private static final String ATTRIBUTE_UPDATE_SIZE = "updateSize";

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
        public void updateCreated(Update update) throws ClusterException {
            if (status != STARTED) {
                log.info("not started: update create ignored.");
                return;
            }
            try {
                Record record = producer.append();
                update.setAttribute(ATTRIBUTE_RECORD, record);
            } catch (JournalException e) {
                String msg = "Unable to create log entry: " + e.getMessage();
                throw new ClusterException(msg, e);
            } catch (Throwable e) {
                String msg = "Unexpected error while creating log entry: "
                        + e.getMessage();
                throw new ClusterException(msg, e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void updatePrepared(Update update) throws ClusterException {
            if (status != STARTED) {
                log.info("not started: update prepare ignored.");
                return;
            }
            Record record = (Record) update.getAttribute(ATTRIBUTE_RECORD);
            if (record == null) {
                String msg = "No record created.";
                log.warn(msg);
                return;
            }

            List<EventState> events = update.getEvents();
            ChangeLog changes = update.getChanges();
            boolean succeeded = false;

            try {
                ChangeLogRecord clr = new ChangeLogRecord(changes, events,
                        record, workspace, update.getTimestamp(),
                        update.getUserData());
                clr.write();
                succeeded = true;
            } catch (JournalException e) {
                String msg = "Unable to create log entry: " + e.getMessage();
                throw new ClusterException(msg, e);
            } catch (Throwable e) {
                String msg = "Unexpected error while preparing log entry.";
                throw new ClusterException(msg, e);
            } finally {
                if (!succeeded) {
                    record.cancelUpdate();
                    update.setAttribute(ATTRIBUTE_RECORD, null);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void updateCommitted(Update update, String path) {
            Record record = (Record) update.getAttribute(ATTRIBUTE_RECORD);
            if (record == null) {
                if (status == STARTED) {
                    log.warn("No record prepared.");
                } else {
                    log.info("not started: update commit ignored.");
                }
                return;
            }
            try {

                long recordRevision = record.getRevision();
                setRevision(recordRevision);

                long journalUpdateSize = record.update();

                log.debug("Stored record '{}' to Journal ({})", recordRevision, journalUpdateSize);

                Object updateSizeValue = update.getAttribute(ATTRIBUTE_UPDATE_SIZE);
                long updateSize = updateSizeValue != null? (Long)updateSizeValue : 0;
                updateCount.compareAndSet(Integer.MAX_VALUE, 0);

               	auditLogger.info("[{}] {} {} ({})", new Object[]{updateCount.incrementAndGet(), 
                        record.getRevision(), path, updateSize});

            } catch (JournalException e) {
                String msg = "Unable to commit log entry.";
                log.error(msg, e);
            } catch (Throwable e) {
                String msg = "Unexpected error while committing log entry.";
                log.error(msg, e);
            } finally {
                update.setAttribute(ATTRIBUTE_RECORD, null);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void updateCancelled(Update update) {
            Record record = (Record) update.getAttribute(ATTRIBUTE_RECORD);
            if (record != null) {
                record.cancelUpdate();
                update.setAttribute(ATTRIBUTE_RECORD, null);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void setListener(UpdateEventListener listener) {
            if (workspace == null) {
                versionUpdateListener = listener;
                if (journal instanceof AbstractJournal &&
                        versionUpdateListener instanceof InternalVersionManagerImpl) {
                    ((AbstractJournal) journal).setInternalVersionManager(
                            (InternalVersionManagerImpl) versionUpdateListener);
                }
            } else {
                wspUpdateListeners.remove(workspace);
                if (listener != null) {
                    wspUpdateListeners.put(workspace, listener);
                }
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
        public ClusterOperation create(NodeId nodeId, boolean deep, String owner) {
            if (status != STARTED) {
                log.info("not started: lock operation ignored.");
                return null;
            }
            try {
                ClusterRecord record = new LockRecord(nodeId, deep, owner,
                        producer.append(), workspace);
                return new DefaultClusterOperation(ClusterNode.this, record);
            } catch (JournalException e) {
                String msg = "Unable to create log entry: " + e.getMessage();
                log.error(msg);
                return null;
            } catch (Throwable e) {
                String msg = "Unexpected error while creating log entry.";
                log.error(msg, e);
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        public ClusterOperation create(NodeId nodeId) {
            if (status != STARTED) {
                log.info("not started: unlock operation ignored.");
                return null;
            }
            try {
                ClusterRecord record = new LockRecord(nodeId, producer.append(),
                        workspace);
                return new DefaultClusterOperation(ClusterNode.this, record);
            } catch (JournalException e) {
                String msg = "Unable to create log entry: " + e.getMessage();
                log.error(msg);
                return null;
            } catch (Throwable e) {
                String msg = "Unexpected error while creating log entry.";
                log.error(msg, e);
                return null;
            }
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

    //-------------------------------------------------------< RecordConsumer >

    /**
     * {@inheritDoc}
     */
    public String getId() {
        return PRODUCER_ID;
    }

    /**
     * {@inheritDoc}
     */
    public long getRevision() {
        try {
            return instanceRevision.get();
        } catch (JournalException e) {
            log.warn("Unable to return current revision.", e);
            return Long.MAX_VALUE;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void consume(Record record) {
        log.info("Processing revision: " + record.getRevision());

        try {
            deserializer.deserialize(record).process(this);
        } catch (JournalException e) {
            String msg = "Unable to read revision '" + record.getRevision() + "'.";
            log.error(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setRevision(long revision) {
        try {
            instanceRevision.set(revision);
        } catch (JournalException e) {
            log.warn("Unable to set current revision to " + revision + ".", e);
        }
    }

    //--------------------------------------------------- ClusterRecordProcessor

    /**
     * {@inheritDoc}
     */
    public void process(ChangeLogRecord record) {
        String workspace = record.getWorkspace();

        UpdateEventListener listener = null;
        if (workspace != null) {
            listener = wspUpdateListeners.get(workspace);
            if (listener == null) {
                try {
                    clusterContext.updateEventsReady(workspace);
                } catch (RepositoryException e) {
                    String msg = "Error making update listener for workspace " +
                            workspace + " online: " + e.getMessage();
                    log.warn(msg);
                }
                listener = wspUpdateListeners.get(workspace);
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
            List<EventState> eventStates = record.getEvents();

            String path = getFirstUserId(eventStates)
                    + "@" + workspace
                    + ":" + EventState.getCommonPath(eventStates, null);

            updateCount.compareAndSet(Integer.MAX_VALUE, 0);
           	auditLogger.info("[{}] {} {}", new Object[]{updateCount.incrementAndGet(), 
                    record.getRevision(), path});

            listener.externalUpdate(record.getChanges(), eventStates,
                    record.getTimestamp(), record.getUserData());
        } catch (RepositoryException e) {
            String msg = "Unable to deliver update events: " + e.getMessage();
            log.error(msg);
            if (e.getCause() instanceof IllegalStateException) {
                throw (IllegalStateException) e.getCause();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void process(LockRecord record) {
        String workspace = record.getWorkspace();

        LockEventListener listener = wspLockListeners.get(workspace);
        if (listener == null) {
            try {
                clusterContext.lockEventsReady(workspace);
            } catch (RepositoryException e) {
                String msg = "Unable to make lock listener for workspace " +
                        workspace + " online: " + e.getMessage();
                log.warn(msg);
            }
            listener = wspLockListeners.get(workspace);
            if (listener ==  null) {
                String msg = "Lock channel unavailable for workspace: " + workspace;
                log.error(msg);
                return;
            }
        }
        try {
            if (record.isLock()) {
                listener.externalLock(record.getNodeId(), record.isDeep(),
                        record.getOwner());
            } else {
                listener.externalUnlock(record.getNodeId());
            }
        } catch (RepositoryException e) {
            String msg = "Unable to deliver lock event: " + e.getMessage();
            log.error(msg);
            if (e.getCause() instanceof IllegalStateException) {
                throw (IllegalStateException) e.getCause();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void process(NamespaceRecord record) {
        if (namespaceListener == null) {
            String msg = "Namespace listener unavailable.";
            log.error(msg);
            return;
        }
        try {
            namespaceListener.externalRemap(record.getOldPrefix(),
                    record.getNewPrefix(), record.getUri());
        } catch (RepositoryException e) {
            String msg = "Unable to deliver namespace operation: " + e.getMessage();
            log.error(msg);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void process(NodeTypeRecord record) {
        if (nodeTypeListener == null) {
            String msg = "NodeType listener unavailable.";
            log.error(msg);
            return;
        }
        Collection coll = record.getCollection();
        try {
            switch (record.getOperation()) {
            case NodeTypeRecord.REGISTER:
                nodeTypeListener.externalRegistered(coll);
                break;
            case NodeTypeRecord.UNREGISTER:
                nodeTypeListener.externalUnregistered(coll);
                break;
            case NodeTypeRecord.REREGISTER:
                QNodeTypeDefinition ntd = (QNodeTypeDefinition) coll.iterator().next();
                nodeTypeListener.externalReregistered(ntd);
                break;
            }
        } catch (InvalidNodeTypeDefException e) {
            String msg = "Unable to deliver node type operation: " + e.getMessage();
            log.error(msg);
        } catch (RepositoryException e) {
            String msg = "Unable to deliver node type operation: " + e.getMessage();
            log.error(msg);
        }
    }

    public void process(PrivilegeRecord record) {
        if (privilegeListener == null) {
            String msg = "Privilege listener unavailable.";
            log.error(msg);
            return;
        }
        try {
            privilegeListener.externalRegisteredPrivileges(record.getDefinitions());
        } catch (RepositoryException e) {
            String msg = "Unable to deliver privilege registration operation: " + e.getMessage();
            log.error(msg);
        }
    }

    public void process(WorkspaceRecord record) {
        if (createWorkspaceListener == null) {
            String msg = "Create Workspace listener unavailable.";
            log.error(msg);
            return;
        }
        try {
            if (record.getActionType() == WorkspaceRecord.CREATE_WORKSPACE_ACTION_TYPE) {
                CreateWorkspaceAction action = record.getCreateWorkspaceAction();
                createWorkspaceListener.externalWorkspaceCreated(record.getWorkspace(), action.getInputSource());
            }
        } catch (RepositoryException e) {
            String msg = "Unable to create workspace: "
                    + e.getMessage();
            log.error(msg);
        }
    }

    // -----------------------------------------------< CreateWorkspaceChannel >

    public void setListener(WorkspaceListener listener) {
        createWorkspaceListener = listener;
    }

    public void workspaceCreated(String workspaceName,
            ClonedInputSource inputSource) {
        if (status != STARTED) {
            log.info("not started: namespace operation ignored.");
            return;
        }
        ClusterRecord record = null;
        boolean succeeded = false;

        try {
            record = new WorkspaceRecord(workspaceName, inputSource, producer.append());
            record.write();
            record.update();
            setRevision(record.getRevision());
            succeeded = true;
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while creating log entry.";
            log.error(msg, e);
        } finally {
            if (!succeeded && record != null) {
                record.cancelUpdate();
            }
        }
    }

    /**
     * Invoked when a cluster operation has ended. If <code>successful</code>,
     * attempts to fill the journal record and update it, otherwise cancels
     * the update.
     *
     * @param operation cluster operation
     * @param successful <code>true</code> if the operation was successful and
     *                   the journal record should be updated;
     *                   <code>false</code> to revoke changes
     */
    public void ended(DefaultClusterOperation operation, boolean successful) {
        ClusterRecord record = operation.getRecord();
        boolean succeeded = false;

        try {
            if (successful) {
                record.write();
                record.update();
                setRevision(record.getRevision());
                succeeded = true;
            }
        } catch (JournalException e) {
            String msg = "Unable to create log entry: " + e.getMessage();
            log.error(msg);
        } catch (Throwable e) {
            String msg = "Unexpected error while creating log entry.";
            log.error(msg, e);
        } finally {
            if (!succeeded) {
                record.cancelUpdate();
            }
        }
    }

    private String getFirstUserId(List<EventState> eventStates) {
        if (eventStates == null || eventStates.isEmpty()) {
            return "";
        }
        return eventStates.get(0).getUserId();
    }
}
