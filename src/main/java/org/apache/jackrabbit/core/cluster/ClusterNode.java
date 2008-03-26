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
import org.apache.jackrabbit.core.config.JournalConfig;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.journal.Journal;
import org.apache.jackrabbit.core.journal.RecordConsumer;
import org.apache.jackrabbit.core.journal.Record;
import org.apache.jackrabbit.core.journal.JournalException;
import org.apache.jackrabbit.core.journal.FileRevision;
import org.apache.jackrabbit.core.nodetype.InvalidNodeTypeDefException;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.observation.EventStateCollection;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.io.File;

/**
 * Default clustered node implementation.
 */
public class ClusterNode implements Runnable,
        NamespaceEventChannel, NodeTypeEventChannel, RecordConsumer  {

    /**
     * System property specifying a node id to use.
     */
    public static final String SYSTEM_PROPERTY_NODE_ID = "org.apache.jackrabbit.core.cluster.node_id";

    /**
     * Revision counter parameter name.
     */
    private static final String REVISION_NAME = "revision";

    /**
     * Used for padding short string representations.
     */
    private static final String SHORT_PADDING = "0000";

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
     * Bit indicating this is a registration operation.
     */
    private static final int NTREG_REGISTER = 0;

    /**
     * Bit indicating this is a reregistration operation.
     */
    private static final int NTREG_REREGISTER = (1 << 30);

    /**
     * Bit indicating this is an unregistration operation.
     */
    private static final int NTREG_UNREGISTER = (1 << 31);

    /**
     * Mask used in node type registration operations.
     */
    private static final int NTREG_MASK = (NTREG_REREGISTER | NTREG_UNREGISTER);

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
     * Synchronization delay, in milliseconds.
     */
    private long syncDelay;

    /**
     * Journal used.
     */
    private Journal journal;

    /**
     * Mutex used when syncing.
     */
    private final Mutex syncLock = new Mutex();

    /**
     * Status flag, one of {@link #NONE}, {@link #STARTED} or {@link #STOPPED}.
     */
    private int status;

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
     * Instance revision file.
     */
    private FileRevision instanceRevision;

    /**
     * Workspace name used when consuming records.
     */
    private String workspace;

    /**
     * Change log used when consuming records.
     */
    private ChangeLog changeLog;

    /**
     * List of recorded events; used when consuming records.
     */
    private List events;

    /**
     * Last used session for event sources.
     */
    private Session lastSession;

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

        JournalConfig jc = cc.getJournalConfig();

        String revisionName = jc.getParameters().getProperty(REVISION_NAME);
        if (revisionName == null) {
            String msg = "Revision not specified.";
            throw new ClusterException(msg);
        }
        try {
            instanceRevision = new FileRevision(new File(revisionName));

            journal = (Journal) jc.newInstance();
            journal.init(clusterNodeId, clusterContext.getNamespaceResovler());
            journal.register(this);
        } catch (ConfigurationException e) {
            throw new ClusterException(e.getMessage(), e.getCause());
        } catch (JournalException e) {
            throw new ClusterException(e.getMessage(), e.getCause());
        }
    }

    /**
     * Starts this cluster node.
     *
     * @throws ClusterException if an error occurs
     */
    public synchronized void start() throws ClusterException {
        if (status == NONE) {
            sync();

            Thread t = new Thread(this, "ClusterNode-" + clusterNodeId);
            t.setDaemon(true);
            t.start();

            status = STARTED;
        }
    }

    /**
     * Run loop that will sync this node after some delay.
     */
    public void run() {
        for (;;) {
            synchronized (this) {
                try {
                    wait(syncDelay);
                } catch (InterruptedException e) {}

                if (status == STOPPED) {
                    return;
                }
            }
            try {
                sync();
            } catch (ClusterException e) {
                String msg = "Periodic sync of journal failed: " + e.getMessage();
                log.error(msg, e);
            } catch (Exception e) {
                String msg = "Unexpected error while syncing of journal: " + e.getMessage();
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
        } catch (JournalException e) {
            throw new ClusterException(e.getMessage(), e.getCause());
        } finally {
            syncLock.release();
        }
    }

    /**
     * Stops this cluster node.
     */
    public synchronized void stop() {
        if (status == STARTED) {
            status = STOPPED;

            journal.close();
            instanceRevision.close();
            
            notifyAll();
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

    /**
     * Return the instance id to be used for this node in the cluster.
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

    //-----------------------------------------------< NamespaceEventListener >

    /**
     * {@inheritDoc}
     */
    public void remapped(String oldPrefix, String newPrefix, String uri) {
        if (status != STARTED) {
            log.info("not started: namespace operation ignored.");
            return;
        }
        Record record = null;
        boolean succeeded = false;

        try {
            record = journal.getProducer(PRODUCER_ID).append();
            record.writeString(null);
            write(record, oldPrefix, newPrefix, uri);
            record.writeChar('\0');
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
        Record record = null;
        boolean succeeded = false;

        try {
            record = journal.getProducer(PRODUCER_ID).append();
            record.writeString(null);
            write(record, ntDefs, true);
            record.writeChar('\0');
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
    public void reregistered(NodeTypeDef ntDef) {
        if (status != STARTED) {
            log.info("not started: nodetype operation ignored.");
            return;
        }
        Record record = null;
        boolean succeeded = false;

        try {
            record = journal.getProducer(PRODUCER_ID).append();
            record.writeString(null);
            write(record, ntDef);
            record.writeChar('\0');
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
        Record record = null;
        boolean succeeded = false;

        try {
            record = journal.getProducer(PRODUCER_ID).append();
            record.writeString(null);
            write(record, qnames, false);
            record.writeChar('\0');
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

    /**
     * Workspace update channel.
     */
    class WorkspaceUpdateChannel implements UpdateEventChannel {

        /**
         * Attribute name used to store record.
         */
        private static final String ATTRIBUTE_RECORD = "record";

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
        public void updateCreated(Update update) {
            if (status != STARTED) {
                log.info("not started: update create ignored.");
                return;
            }
            try {
                Record record = journal.getProducer(PRODUCER_ID).append();
                update.setAttribute(ATTRIBUTE_RECORD, record);
            } catch (JournalException e) {
                String msg = "Unable to create log entry.";
                log.error(msg, e);
            } catch (Throwable e) {
                String msg = "Unexpected error while creating log entry.";
                log.error(msg, e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void updatePrepared(Update update) {
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

            EventStateCollection events = update.getEvents();
            ChangeLog changes = update.getChanges();
            boolean succeeded = false;

            try {
                record.writeString(workspace);
                write(record, changes, events);
                record.writeChar('\0');
                succeeded = true;
            } catch (JournalException e) {
                String msg = "Unable to create log entry: " + e.getMessage();
                log.error(msg);
            } catch (Throwable e) {
                String msg = "Unexpected error while preparing log entry.";
                log.error(msg, e);
            } finally {
                if (!succeeded && record != null) {
                    record.cancelUpdate();
                    update.setAttribute(ATTRIBUTE_RECORD, null);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void updateCommitted(Update update) {
            if (status != STARTED) {
                log.info("not started: update commit ignored.");
                return;
            }
            Record record = (Record) update.getAttribute(ATTRIBUTE_RECORD);
            if (record == null) {
                String msg = "No record prepared.";
                log.warn(msg);
                return;
            }
            try {
                record.update();
                setRevision(record.getRevision());
                log.info("Appended revision: " + record.getRevision());
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
            if (status != STARTED) {
                log.info("not started: update cancel ignored.");
                return;
            }
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
                Record record = journal.getProducer(PRODUCER_ID).append();
                return new LockOperation(ClusterNode.this, workspace, record,
                        nodeId, deep, owner);
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
                Record record = journal.getProducer(PRODUCER_ID).append();
                return new LockOperation(ClusterNode.this, workspace, record,
                        nodeId);
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

    /**
     * Invoked when a record starts.
     *
     * @param workspace workspace, may be <code>null</code>
     */
    private void start(String workspace) {
        this.workspace = workspace;

        changeLog = new ChangeLog();
        events = new ArrayList();
    }

    /**
     * Process an update operation.
     *
     * @param operation operation to process
     */
    private void process(ItemOperation operation) {
        operation.apply(changeLog);
    }

    /**
     * Process an event.
     *
     * @param event event
     */
    private void process(EventState event) {
        events.add(event);
    }

    /**
     * Process a lock operation.
     *
     * @param nodeId node id
     * @param isDeep flag indicating whether lock is deep
     * @param owner lock owner
     */
    private void process(NodeId nodeId, boolean isDeep, String owner) {
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
     * Process an unlock operation.
     *
     * @param nodeId node id
     */
    private void process(NodeId nodeId) {
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
     * Process a namespace operation.
     *
     * @param oldPrefix old prefix. if <code>null</code> this is a fresh mapping
     * @param newPrefix new prefix. if <code>null</code> this is an unmap operation
     * @param uri uri to map prefix to
     */
    private void process(String oldPrefix, String newPrefix, String uri) {
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
     * Process one or more node type registrations.
     *
     * @param c collection of node type definitions, if this is a register
     *          operation; collection of <code>Name</code>s if this is
     *          an unregister operation
     * @param register <code>true</code>, if this is a register operation;
     *                 <code>false</code> otherwise
     */
    private void process(Collection c, boolean register) {
        if (nodeTypeListener == null) {
            String msg = "NodeType listener unavailable.";
            log.error(msg);
            return;
        }
        try {
            if (register) {
                nodeTypeListener.externalRegistered(c);
            } else {
                nodeTypeListener.externalUnregistered(c);
            }
        } catch (InvalidNodeTypeDefException e) {
            String msg = "Unable to deliver node type operation: " + e.getMessage();
            log.error(msg);
        } catch (RepositoryException e) {
            String msg = "Unable to deliver node type operation: " + e.getMessage();
            log.error(msg);
        }
    }

    /**
     * Process a node type re-registration.
     *
     * @param ntDef node type definition
     */
    private void process(NodeTypeDef ntDef) {
        if (nodeTypeListener == null) {
            String msg = "NodeType listener unavailable.";
            log.error(msg);
            return;
        }
        try {
            nodeTypeListener.externalReregistered(ntDef);
        } catch (InvalidNodeTypeDefException e) {
            String msg = "Unable to deliver node type operation: " + e.getMessage();
            log.error(msg);
        } catch (RepositoryException e) {
            String msg = "Unable to deliver node type operation: " + e.getMessage();
            log.error(msg);
        }
    }

    /**
     * Invoked when a record ends.
     */
    private void end() {
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

        String workspace = null;

        try {
            workspace = record.readString();
            start(workspace);

            for (;;) {
                char c = record.readChar();
                if (c == '\0') {
                    break;
                }
                if (c == 'N') {
                    NodeOperation operation = NodeOperation.create(record.readByte());
                    operation.setId(record.readNodeId());
                    process(operation);
                } else if (c == 'P') {
                    PropertyOperation operation = PropertyOperation.create(record.readByte());
                    operation.setId(record.readPropertyId());
                    process(operation);
                } else if (c == 'E') {
                    int type = record.readByte();
                    NodeId parentId = record.readNodeId();
                    Path parentPath = record.readPath();
                    NodeId childId = record.readNodeId();
                    Path.Element childRelPath = record.readPathElement();
                    Name ntName = record.readQName();

                    Set mixins = new HashSet();
                    int mixinCount = record.readInt();
                    for (int i = 0; i < mixinCount; i++) {
                        mixins.add(record.readQName());
                    }
                    String userId = record.readString();
                    process(createEventState(type, parentId, parentPath, childId,
                            childRelPath, ntName, mixins, userId));
                } else if (c == 'L') {
                    NodeId nodeId = record.readNodeId();
                    boolean isLock = record.readBoolean();
                    if (isLock) {
                        boolean isDeep = record.readBoolean();
                        String owner = record.readString();
                        process(nodeId, isDeep, owner);
                    } else {
                        process(nodeId);
                    }
                } else if (c == 'S') {
                    String oldPrefix = record.readString();
                    String newPrefix = record.readString();
                    String uri = record.readString();
                    process(oldPrefix, newPrefix, uri);
                } else if (c == 'T') {
                    int size = record.readInt();
                    int opcode = size & NTREG_MASK;
                    size &= ~NTREG_MASK;

                    switch (opcode) {
                        case NTREG_REGISTER:
                            HashSet ntDefs = new HashSet();
                            for (int i = 0; i < size; i++) {
                                ntDefs.add(record.readNodeTypeDef());
                            }
                            process(ntDefs, true);
                            break;
                        case NTREG_REREGISTER:
                            process(record.readNodeTypeDef());
                            break;
                        case NTREG_UNREGISTER:
                            HashSet ntNames = new HashSet();
                            for (int i = 0; i < size; i++) {
                                ntNames.add(record.readQName());
                            }
                            process(ntNames, false);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown opcode: " + opcode);
                    }
                } else {
                    throw new IllegalArgumentException("Unknown entry type: " + c);
                }
            }
            end();

        } catch (JournalException e) {
            String msg = "Unable to read revision '" + record.getRevision() + "'.";
            log.error(msg, e);
        } catch (IllegalArgumentException e) {
            String msg = "Error while processing revision " +
                    record.getRevision() + ": " + e.getMessage();
            log.error(msg);
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

    /**
     * Create an event state.
     *
     * @param type event type
     * @param parentId parent id
     * @param parentPath parent path
     * @param childId child id
     * @param childRelPath child relative path
     * @param ntName ndoe type name
     * @param userId user id
     * @return event
     */
    private EventState createEventState(int type, NodeId parentId, Path parentPath,
                                        NodeId childId, Path.Element childRelPath,
                                        Name ntName, Set mixins, String userId) {
        switch (type) {
            case Event.NODE_ADDED:
                return EventState.childNodeAdded(parentId, parentPath, childId, childRelPath,
                        ntName, mixins, getOrCreateSession(userId), true);
            case Event.NODE_REMOVED:
                return EventState.childNodeRemoved(parentId, parentPath, childId, childRelPath,
                        ntName, mixins, getOrCreateSession(userId), true);
            case Event.PROPERTY_ADDED:
                return EventState.propertyAdded(parentId, parentPath, childRelPath,
                        ntName, mixins, getOrCreateSession(userId), true);
            case Event.PROPERTY_CHANGED:
                return EventState.propertyChanged(parentId, parentPath, childRelPath,
                        ntName, mixins, getOrCreateSession(userId), true);
            case Event.PROPERTY_REMOVED:
                return EventState.propertyRemoved(parentId, parentPath, childRelPath,
                        ntName, mixins, getOrCreateSession(userId), true);
            default:
                String msg = "Unexpected event type: " + type;
                throw new IllegalArgumentException(msg);
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

    //-----------------------------------------------< Record writing methods >

    private static void write(Record record, ChangeLog changeLog, EventStateCollection esc)
            throws JournalException {

        Iterator deletedStates = changeLog.deletedStates();
        while (deletedStates.hasNext()) {
            ItemState state = (ItemState) deletedStates.next();
            if (state.isNode()) {
                write(record, NodeDeletedOperation.create((NodeState) state));
            } else {
                write(record, PropertyDeletedOperation.create((PropertyState) state));
            }
        }
        Iterator modifiedStates = changeLog.modifiedStates();
        while (modifiedStates.hasNext()) {
            ItemState state = (ItemState) modifiedStates.next();
            if (state.isNode()) {
                write(record, NodeModifiedOperation.create((NodeState) state));
            } else {
                write(record, PropertyModifiedOperation.create((PropertyState) state));
            }
        }
        Iterator addedStates = changeLog.addedStates();
        while (addedStates.hasNext()) {
            ItemState state = (ItemState) addedStates.next();
            if (state.isNode()) {
                write(record, NodeAddedOperation.create((NodeState) state));
            } else {
                write(record, PropertyAddedOperation.create((PropertyState) state));
            }
        }

        Iterator events = esc.getEvents().iterator();
        while (events.hasNext()) {
            EventState event = (EventState) events.next();
            write(record, event);
        }
    }

    private static void write(Record record, String oldPrefix, String newPrefix, String uri)
            throws JournalException {

        record.writeChar('S');
        record.writeString(oldPrefix);
        record.writeString(newPrefix);
        record.writeString(uri);
    }

    private static void write(Record record, Collection c, boolean register)
            throws JournalException {

        record.writeChar('T');

        int size = c.size();
        if (!register) {
            size |= NTREG_UNREGISTER;
        }
        record.writeInt(size);

        Iterator iter = c.iterator();
        while (iter.hasNext()) {
            if (register) {
                record.writeNodeTypeDef((NodeTypeDef) iter.next());
            } else {
                record.writeQName((Name) iter.next());
            }
        }
    }

    private static void write(Record record, NodeTypeDef ntDef)
            throws JournalException {

        record.writeChar('T');

        int size = 1;
        size |= NTREG_REREGISTER;
        record.writeInt(size);

        record.writeNodeTypeDef(ntDef);
    }

    private static void write(Record record, PropertyOperation operation)
            throws JournalException {

        record.writeChar('P');
        record.writeByte(operation.getOperationType());
        record.writePropertyId(operation.getId());
    }

    private static void write(Record record, NodeOperation operation)
            throws JournalException {

        record.writeChar('N');
        record.writeByte(operation.getOperationType());
        record.writeNodeId(operation.getId());
    }

    /**
     * Log an event. Subclass responsibility.
     *
     * @param event event to log
     */
    private static void write(Record record, EventState event)
            throws JournalException {

        record.writeChar('E');
        record.writeByte(event.getType());
        record.writeNodeId(event.getParentId());
        record.writePath(event.getParentPath());
        record.writeNodeId(event.getChildId());
        record.writePathElement(event.getChildRelPath());
        record.writeQName(event.getNodeType());

        Set mixins = event.getMixinNames();
        record.writeInt(mixins.size());
        Iterator iter = mixins.iterator();
        while (iter.hasNext()) {
            record.writeQName((Name) iter.next());
        }
        record.writeString(event.getUserId());
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
    public void ended(AbstractClusterOperation operation, boolean successful) {
        Record record = operation.getRecord();
        boolean succeeded = false;

        try {
            if (successful) {
                record = operation.getRecord();
                record.writeString(operation.getWorkspace());
                operation.write();
                record.writeChar('\0');
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
}
