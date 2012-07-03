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
package org.apache.jackrabbit.core.data;

import org.apache.jackrabbit.api.management.DataStoreGarbageCollector;
import org.apache.jackrabbit.api.management.MarkEventListener;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;

/**
 * Garbage collector for DataStore. This implementation iterates through all
 * nodes and reads the binary properties. To detect nodes that are moved while
 * the scan runs, event listeners are started. Like the well known garbage
 * collection in Java, the items that are still in use are marked. Currently
 * this is achieved by updating the modified date of the entries. Newly added
 * entries are detected because the modified date is changed when they are
 * added.
 * <p>
 * Example code to run the data store garbage collection:
 * <pre>
 * JackrabbitRepositoryFactory jf = (JackrabbitRepositoryFactory) factory;
 * RepositoryManager m = jf.getRepositoryManager((JackrabbitRepository) repository);
 * GarbageCollector gc = m.createDataStoreGarbageCollector();
 * try {
 *     gc.mark();
 *     gc.sweep();
 * } finally {
 *     gc.close();
 * }
 * </pre>
 */
public class GarbageCollector implements DataStoreGarbageCollector {

    /** logger instance */
    static final Logger LOG = LoggerFactory.getLogger(GarbageCollector.class);

    private MarkEventListener callback;

    private long sleepBetweenNodes;

    protected int testDelay;

    private final DataStore store;

    private long startScanTimestamp;

    private final ArrayList<Listener> listeners = new ArrayList<Listener>();

    private final IterablePersistenceManager[] pmList;

    private final SessionImpl[] sessionList;

    private final AtomicBoolean closed = new AtomicBoolean();

    private boolean persistenceManagerScan;

    private volatile RepositoryException observationException;

    /**
     * Create a new garbage collector.
     * This method is usually not called by the application, it is called
     * by SessionImpl.createDataStoreGarbageCollector().
     *
     * @param dataStore the data store to be garbage-collected
     * @param list the persistence managers
     * @param sessionList the sessions to access the workspaces
     */
    public GarbageCollector(
            DataStore dataStore, IterablePersistenceManager[] list,
            SessionImpl[] sessionList) {
        this.store = dataStore;
        this.pmList = list;
        this.persistenceManagerScan = list != null;
        this.sessionList = sessionList;
    }

    public void setSleepBetweenNodes(long millis) {
        this.sleepBetweenNodes = millis;
    }

    public long getSleepBetweenNodes() {
        return sleepBetweenNodes;
    }

    /**
     * When testing the garbage collection, a delay is used instead of simulating concurrent access.
     *
     * @param testDelay the delay in milliseconds
     */
    public void setTestDelay(int testDelay) {
        this.testDelay = testDelay;
    }

    /**
     * @deprecated use setMarkEventListener().
     */
    public void setScanEventListener(ScanEventListener callback) {
        setMarkEventListener(callback);
    }

    public void setMarkEventListener(MarkEventListener callback) {
        this.callback = callback;
    }

    /**
     * @deprecated use mark().
     */
    public void scan() throws RepositoryException {
        mark();
    }

    public void mark() throws RepositoryException {
        if (store == null) {
            throw new RepositoryException("No DataStore configured.");
        }
        long now = System.currentTimeMillis();
        if (startScanTimestamp == 0) {
            startScanTimestamp = now;
            store.updateModifiedDateOnAccess(startScanTimestamp);
        }

        if (pmList == null || !persistenceManagerScan) {
            for (SessionImpl s : sessionList) {
                scanNodes(s);
            }
        } else {
            try {
                scanPersistenceManagers();
            } catch (ItemStateException e) {
                throw new RepositoryException(e);
            }
        }
    }

    private void scanNodes(SessionImpl session) throws RepositoryException {

        // add a listener to get 'moved' nodes
        Session clonedSession = session.createSession(session.getWorkspace().getName());
        listeners.add(new Listener(this, clonedSession));

        // adding a link to a BLOB updates the modified date
        // reading usually doesn't, but when scanning, it does
        recurse(session.getRootNode(), sleepBetweenNodes);
    }

    public void setPersistenceManagerScan(boolean allow) {
        persistenceManagerScan = allow;
    }

    public boolean isPersistenceManagerScan() {
        return persistenceManagerScan;
    }

    /**
     * @deprecated use isPersistenceManagerScan().
     */
    public boolean getPersistenceManagerScan() {
        return isPersistenceManagerScan();
    }

    private void scanPersistenceManagers() throws RepositoryException, ItemStateException {
        for (IterablePersistenceManager pm : pmList) {
            for (NodeId id : pm.getAllNodeIds(null, 0)) {
                if (callback != null) {
                    callback.beforeScanning(null);
                }
                try {
                    NodeState state = pm.load(id);
                    Set<Name> propertyNames = state.getPropertyNames();
                    for (Name name : propertyNames) {
                        PropertyId pid = new PropertyId(id, name);
                        PropertyState ps = pm.load(pid);
                        if (ps.getType() == PropertyType.BINARY) {
                            for (InternalValue v : ps.getValues()) {
                                // getLength will update the last modified date
                                // if the persistence manager scan is running
                                v.getLength();
                            }
                        }
                    }
                } catch (NoSuchItemStateException e) {
                    // the node may have been deleted or moved in the meantime
                    // ignore it
                }
            }
        }
    }

    /**
     * Stop the observation listener if any are installed.
     */
    public void stopScan() throws RepositoryException {
        if (listeners.size() > 0) {
            for (Listener listener : listeners) {
                listener.stop();
            }
            listeners.clear();
        }
        checkObservationException();
    }

    /**
     * @deprecated use sweep().
     */
    public int deleteUnused() throws RepositoryException {
        return sweep();
    }

    public int sweep() throws RepositoryException {
        if (startScanTimestamp == 0) {
            throw new RepositoryException("scan must be called first");
        }
        stopScan();
        return store.deleteAllOlderThan(startScanTimestamp);
    }

    /**
     * Get the data store if one is used.
     *
     * @return the data store, or null
     */
    public DataStore getDataStore() {
        return store;
    }

    void recurse(final Node n, long sleep) throws RepositoryException {
        if (sleep > 0) {
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        if (callback != null) {
            callback.beforeScanning(n);
        }
        try {
            for (PropertyIterator it = n.getProperties(); it.hasNext();) {
                Property p = it.nextProperty();
                try {
                    if (p.getType() == PropertyType.BINARY) {
                        if (n.hasProperty("jcr:uuid")) {
                            rememberNode(n.getProperty("jcr:uuid").getString());
                        } else {
                            rememberNode(n.getPath());
                        }
                        if (p.isMultiple()) {
                            checkLengths(p.getLengths());
                        } else {
                        	checkLengths(p.getLength());
                        }
                    }
                } catch (InvalidItemStateException e) {
                    LOG.debug("Property removed concurrently - ignoring", e);
                }
            }
        } catch (InvalidItemStateException e) {
            LOG.debug("Node removed concurrently - ignoring", e);
        }
        try {
            for (NodeIterator it = n.getNodes(); it.hasNext();) {
                recurse(it.nextNode(), sleep);
            }
        } catch (InvalidItemStateException e) {
            LOG.debug("Node removed concurrently - ignoring", e);
        }
        checkObservationException();
    }

    private void rememberNode(String path) {
        // Do nothing at the moment
        // TODO It may be possible to delete some items early
        /*
         * To delete files early in the garbage collection scan, we could do
         * this:
         *
         * A) If garbage collection was run before, see if there a file with the
         * list of UUIDs ('uuids.txt').
         *
         * B) If yes, and if the checksum is ok, read all those nodes first (if
         * not so many). This updates the modified date of all old files that
         * are still in use. Afterwards, delete all files with an older modified
         * date than the last scan! Newer files, and files that are read have a
         * newer modification date.
         *
         * C) Delete the 'uuids.txt' file (in any case).
         *
         * D) Iterate (recurse) through all nodes and properties like now. If a
         * node has a binary property, store the UUID of the node in the file
         * ('uuids.txt'). Also store the time when the scan started.
         *
         * E) Checksum and close the file.
         *
         * F) Like now, delete files with an older modification date than this
         * scan.
         *
         * We can't use node path for this, UUIDs are required as nodes could be
         * moved around.
         *
         * This mechanism requires that all data stores update the last modified
         * date when calling addRecord and that record already exists.
         *
         */
    }

    private void checkLengths(long... lengths) throws RepositoryException {
        for (long length : lengths) {
            if (length == -1) {
                throw new RepositoryException("mark failed to access a property");
            }
        }
    }

    public void close() {
        if (!closed.getAndSet(true)) {
            try {
                stopScan();
            } catch (RepositoryException e) {
                LOG.warn("An error occured when stopping the event listener", e);
            }
            for (Session s : sessionList) {
                s.logout();
            }
        }
    }

    private void checkObservationException() throws RepositoryException {
        RepositoryException e = observationException;
        if (e != null) {
            observationException = null;
            String message = "Exception while processing concurrent events";
            LOG.warn(message, e);
            e = new RepositoryException(message, e);
        }
    }

    void onObservationException(Exception e) {
        if (e instanceof RepositoryException) {
            observationException = (RepositoryException) e;
        } else {
            observationException = new RepositoryException(e);
        }
    }

    /**
     * Auto-close in case the application didn't call it explicitly.
     */
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * Event listener to detect moved nodes.
     * A SynchronousEventListener is used to make sure this method is called before the main iteration ends.
     */
    class Listener implements SynchronousEventListener {

        private final GarbageCollector gc;
        private final Session session;
        private final ObservationManager manager;

        Listener(GarbageCollector gc, Session session)
                throws UnsupportedRepositoryOperationException,
                RepositoryException {
            this.gc = gc;
            this.session = session;
            Workspace ws = session.getWorkspace();
            manager = ws.getObservationManager();
            manager.addEventListener(this, Event.NODE_MOVED, "/", true, null,
                    null, false);
        }

        void stop() throws RepositoryException {
            manager.removeEventListener(this);
            session.logout();
        }

        public void onEvent(EventIterator events) {
            if (testDelay > 0) {
                try {
                    Thread.sleep(testDelay);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
            while (events.hasNext()) {
                Event event = events.nextEvent();
                try {
                    String path = event.getPath();
                    try {
                        Item item = session.getItem(path);
                        if (item.isNode()) {
                            Node n = (Node) item;
                            recurse(n, testDelay);
                        }
                    } catch (PathNotFoundException e) {
                        // ignore
                    }
                } catch (Exception e) {
                    gc.onObservationException(e);
                    try {
                        stop();
                    } catch (RepositoryException e2) {
                        LOG.warn("Exception removing the observation listener - ignored", e2);
                    }
                }
            }
        }
    }

}
