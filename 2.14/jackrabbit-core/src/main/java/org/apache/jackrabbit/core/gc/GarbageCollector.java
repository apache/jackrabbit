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
package org.apache.jackrabbit.core.gc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

import org.apache.jackrabbit.api.management.DataStoreGarbageCollector;
import org.apache.jackrabbit.api.management.MarkEventListener;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.data.DataStore;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.persistence.util.NodeInfo;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
	private class ScanNodeIdListTask implements Callable<Void> {

        private int split;
        private List<NodeId> nodeList;
        private PersistenceManager pm;
        private int pmCount;

        public ScanNodeIdListTask(int split, List<NodeId> nodeList, PersistenceManager pm, int pmCount) {
            this.split = split;
            this.nodeList = nodeList;
            this.pm = pm;
            this.pmCount = pmCount;
        }

        public Void call() throws Exception {
            scanNodeIdList(split, nodeList, pm, pmCount);
            return null;
        }

    }

    /** logger instance */
    static final Logger LOG = LoggerFactory.getLogger(GarbageCollector.class);

    /**
     * The number of nodes to fetch at once from the persistence manager. Defaults to 8kb
     */
    private static final int NODESATONCE = Integer.getInteger("org.apache.jackrabbit.garbagecollector.nodesatonce", 1024 * 8);

    /**
     * Set this System Property to true to speed up the node traversing in a binary focused repository.
     * See JCR-3708
     */
    private static final boolean NODE_ID_SCAN = Boolean.getBoolean("org.apache.jackrabbit.garbagecollector.node_id.scan");

    private MarkEventListener callback;

    private long sleepBetweenNodes;

    private long minSplitSize = 100000;

    private int concurrentThreadSize = 3;

    protected int testDelay;

    private final DataStore store;

    private long startScanTimestamp;

    private final ArrayList<Listener> listeners = new ArrayList<Listener>();

    private final IterablePersistenceManager[] pmList;

    private final SessionImpl[] sessionList;

    private final AtomicBoolean closed = new AtomicBoolean();
    
    private final RepositoryContext context;

    private boolean persistenceManagerScan;

    private volatile RepositoryException observationException;

    /**
     * Create a new garbage collector.
     * This method is usually not called by the application, it is called
     * by SessionImpl.createDataStoreGarbageCollector().
     * 
     * @param context repository context
     * @param dataStore the data store to be garbage-collected
     * @param list the persistence managers
     * @param sessionList the sessions to access the workspaces
     */
    
    public GarbageCollector(RepositoryContext context, 
            DataStore dataStore, IterablePersistenceManager[] list,
            SessionImpl[] sessionList) {
        this.context = context;
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

    public long getMinSplitSize() {
        return minSplitSize;
    }

    public void setMinSplitSize(long minSplitSize) {
        this.minSplitSize = minSplitSize;
    }

    public int getConcurrentThreadSize() {
        return concurrentThreadSize;
    }

    public void setConcurrentThreadSize(int concurrentThreadSize) {
        this.concurrentThreadSize = concurrentThreadSize;
    }

    /**
     * When testing the garbage collection, a delay is used instead of simulating concurrent access.
     *
     * @param testDelay the delay in milliseconds
     */
    public void setTestDelay(int testDelay) {
        this.testDelay = testDelay;
    }

    public void setMarkEventListener(MarkEventListener callback) {
        this.callback = callback;
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
                if (!NODE_ID_SCAN) {
                    scanPersistenceManagersByNodeInfos();
                } else {
                    scanPersistenceManagersByNodeIds();
                }
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

    private void scanPersistenceManagersByNodeInfos() throws RepositoryException, ItemStateException {
        int pmCount = 0;
        for (IterablePersistenceManager pm : pmList) {
            pmCount++;
            int count = 0;
            Map<NodeId,NodeInfo> batch = pm.getAllNodeInfos(null, NODESATONCE);
            while (!batch.isEmpty()) {
                NodeId lastId = null;
                for (NodeInfo info : batch.values()) {
                    count++;
                    if (count % 1000 == 0) {
                        LOG.debug(pm.toString() + " ("+pmCount + "/" + pmList.length + "): analyzed " + count + " nodes...");
                    }
                    lastId = info.getId();
                    if (callback != null) {
                        callback.beforeScanning(null);
                    }
                    if (info.hasBlobsInDataStore()) {
                        try {
                            NodeState state = pm.load(info.getId());
                            Set<Name> propertyNames = state.getPropertyNames();
                            for (Name name : propertyNames) {
                                PropertyId pid = new PropertyId(info.getId(), name);
                                PropertyState ps = pm.load(pid);
                                if (ps.getType() == PropertyType.BINARY) {
                                    for (InternalValue v : ps.getValues()) {
                                        // getLength will update the last modified date
                                        // if the persistence manager scan is running
                                        v.getLength();
                                    }
                                }
                            }
                        } catch (NoSuchItemStateException ignored) {
                            // the node may have been deleted in the meantime
                        }
                    }
                }
                batch = pm.getAllNodeInfos(lastId, NODESATONCE);
            }
        }
        NodeInfo.clearPool();
    }

    private void scanPersistenceManagersByNodeIds() throws RepositoryException, ItemStateException {
        int pmCount = 0;
        for (IterablePersistenceManager pm : pmList) {
            pmCount++;
            List<NodeId> allNodeIds = pm.getAllNodeIds(null, 0);
            int overAllCount = allNodeIds.size();
            if (overAllCount > minSplitSize) {
                final int splits = getConcurrentThreadSize();
                ExecutorService executorService = Executors.newFixedThreadPool(splits);
                try {
                    Set<Future<Void>> futures = new HashSet<Future<Void>>();
                    List<List<NodeId>> lists = splitIntoParts(allNodeIds, splits);
                    LOG.debug(splits + " concurrent Threads will be started. Split Size: " + lists.get(0).size()+" Total Size: " + overAllCount);
                    for (int i = 0; i < splits; i++) {
                        List<NodeId> subList = lists.get(i);
                        futures.add(executorService.submit(new ScanNodeIdListTask(i + 1, subList, pm, pmCount)));
                    }
                    for (Future<Void> future : futures) {
                        future.get();
                    }
                } catch (Exception e) {
                    throw new RepositoryException(e);
                } finally {
                    executorService.shutdown();
                }
            } else {
                scanNodeIdList(0, allNodeIds, pm, pmCount);
            }
        }
    }
    
    private void scanNodeIdList(int split, List<NodeId> nodeList, PersistenceManager pm, int pmCount) throws RepositoryException, ItemStateException {
        int count = 0;
        for (NodeId id : nodeList) {
            count++;
            if (count % 1000 == 0) {
                if (split > 0) {
                    LOG.debug("[Split " + split + "] " + pm.toString() + " (" + pmCount + "/" + pmList.length + "): analyzed " + count + " nodes [" + nodeList.size() + "]...");
                } else {
                    LOG.debug(pm.toString() + " (" + pmCount + "/" + pmList.length + "): analyzed " + count + " nodes [" + nodeList.size() + "]...");
                }
            }
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

    private <T> List<List<T>> splitIntoParts(List<T> ls, int parts) {
        final List<List<T>> listParts = new ArrayList<List<T>>();
        final int chunkSize = ls.size() / parts;
        int leftOver = ls.size() % parts;
        int iTake = chunkSize;

        for (int i = 0, iT = ls.size(); i < iT; i += iTake) {
            if (leftOver > 0) {
                leftOver--;
                iTake = chunkSize + 1;
            } else {
                iTake = chunkSize;
            }
            listParts.add(new ArrayList<T>(ls.subList(i, Math.min(iT, i + iTake))));
        }
        return listParts;
    }

    /**
     * Reset modifiedDateOnAccess to 0 and stop the observation 
     * listener if any are installed.
     */
    public void stopScan() throws RepositoryException {
         // reset updateModifiedDateOnAccess to OL
        store.updateModifiedDateOnAccess(0L);
        
        if (listeners.size() > 0) {
            for (Listener listener : listeners) {
                listener.stop();
            }
            listeners.clear();
        }
        checkObservationException();
        context.setGcRunning(false);
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

    private static void checkLengths(long... lengths) throws RepositoryException {
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
