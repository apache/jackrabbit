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

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.SessionListener;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.core.persistence.IterablePersistenceManager;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
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
 * Garbage collector for DataStore. This implementation is iterates through all
 * nodes and reads the binary properties. To detect nodes that are moved while
 * the scan runs, event listeners are started. Like the well known garbage
 * collection in Java, the items that are still in use are marked. Currently
 * this achieved by updating the modified date of the entries. Newly added
 * entries are detected because the modified date is changed when they are
 * added.
 * <p>
 * Example code to run the data store garbage collection:
 * <pre>
 * GarbageCollector gc = ((SessionImpl)session).createDataStoreGarbageCollector();
 * gc.scan();
 * gc.stopScan();
 * gc.deleteUnused();
 * </pre>
 */
public class GarbageCollector {

    private ScanEventListener callback;

    private int sleepBetweenNodes;

    private int testDelay;

    private final DataStore store;

    private long startScanTimestamp;

    private final ArrayList listeners = new ArrayList();

    private final IterablePersistenceManager[] pmList;

    private final Session[] sessionList;
    private final SessionListener sessionListener;
    
    private final SynchronizedBoolean closed = new SynchronizedBoolean(false);

    private boolean persistenceManagerScan;

    // TODO It should be possible to stop and restart a garbage collection scan.

    /**
     * Create a new garbage collector.
     * This method is usually not called by the application, it is called
     * by SessionImpl.createDataStoreGarbageCollector().
     *
     * @param session the session that created this object
     * @param list the persistence managers
     * @param sessionList the sessions to access the workspaces
     */
    public GarbageCollector(SessionImpl session, IterablePersistenceManager[] list, Session[] sessionList) {
        RepositoryImpl rep = (RepositoryImpl) session.getRepository();
        store = rep.getDataStore();
        this.pmList = list;
        this.persistenceManagerScan = list != null;
        this.sessionList = sessionList;

        // Auto-close if the main session logs out
        this.sessionListener = new SessionListener() {
            public void loggedOut(SessionImpl session) {
            }
            public void loggingOut(SessionImpl session) {
                close();
            }
        };
        session.addListener(sessionListener);
    }

    /**
     * Set the delay between scanning items.
     * The main scan loop sleeps this many milliseconds after
     * scanning a node. The default is 0, meaning the scan should run at full speed.
     *
     * @param millis the number of milliseconds to sleep
     */
    public void setSleepBetweenNodes(int millis) {
        this.sleepBetweenNodes = millis;
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
     * Set the event listener. If set, the event listener will be called
     * for each item that is scanned. This mechanism can be used
     * to display the progress.
     *
     * @param callback if set, this is called while scanning
     */
    public void setScanEventListener(ScanEventListener callback) {
        this.callback = callback;
    }

    /**
     * Scan the repository. The garbage collector will iterate over all nodes in the repository
     * and update the last modified date. If all persistence managers implement the
     * IterablePersistenceManager interface, this mechanism will be used; if not, the garbage
     * collector will scan the repository using the JCR API starting from the root node.
     *
     * @throws RepositoryException
     * @throws IllegalStateException
     * @throws IOException
     * @throws ItemStateException
     */
    public void scan() throws RepositoryException,
            IllegalStateException, IOException, ItemStateException {
        if (store == null) {
            throw new RepositoryException("No DataStore configured.");
        }
        long now = System.currentTimeMillis();
        if (startScanTimestamp == 0) {
            startScanTimestamp = now;
            store.updateModifiedDateOnAccess(startScanTimestamp);
        }

        if (pmList == null || !persistenceManagerScan) {
            for (int i = 0; i < sessionList.length; i++) {
                scanNodes(sessionList[i]);
            }
        } else {
            scanPersistenceManagers();
        }
    }

    private void scanNodes(Session session)
        throws RepositoryException, IllegalStateException, IOException {

        // add a listener to get 'new' nodes
        // actually, new nodes are not the problem, but moved nodes
        listeners.add(new Listener(session));

        // adding a link to a BLOB updates the modified date
        // reading usually doesn't, but when scanning, it does
        recurse(session.getRootNode(), sleepBetweenNodes);
    }

    /**
     * Enable or disable using the IterablePersistenceManager interface
     * to scan the items. This is important for clients that need
     * the complete Node implementation in the ScanEventListener
     * callback.
     *
     * @param allow true if using the IterablePersistenceManager interface is allowed
     */
    public void setPersistenceManagerScan(boolean allow) {
        persistenceManagerScan = allow;
    }

    /**
     * Check if using the IterablePersistenceManager interface is allowed.
     *
     * @return true if using IterablePersistenceManager is possible.
     */
    public boolean getPersistenceManagerScan() {
        return persistenceManagerScan;
    }

    private void scanPersistenceManagers() throws ItemStateException, RepositoryException {
        for (int i = 0; i < pmList.length; i++) {
            IterablePersistenceManager pm = pmList[i];
            Iterator it = pm.getAllNodeIds(null, 0);
            while (it.hasNext()) {
                NodeId id = (NodeId) it.next();
                if (callback != null) {
                    callback.beforeScanning(null);
                }
                try {
                    NodeState state = pm.load(id);
                    Set propertyNames = state.getPropertyNames();
                    for (Iterator nameIt = propertyNames.iterator(); nameIt
                            .hasNext();) {
                        Name name = (Name) nameIt.next();
                        PropertyId pid = new PropertyId(id, name);
                        PropertyState ps = pm.load(pid);
                        if (ps.getType() == PropertyType.BINARY) {
                            InternalValue[] values = ps.getValues();
                            for (int j = 0; j < values.length; j++) {
                                values[j].getBLOBFileValue().getLength();
                            }
                        }
                    }
                } catch (NoSuchItemStateException e) {
                    // the node may have been deleted or moved in the meantime
                    // ignore it
                }
                if (callback != null) {
                    callback.afterScanning(null);
                }
            }
        }
    }

    /**
     * The repository was scanned. This method will stop the observation
     * listener.
     */
    public void stopScan() throws RepositoryException {
        checkScanStarted();
        for (int i = 0; i < listeners.size(); i++) {
            Listener listener = (Listener) listeners.get(i);
            try {
                listener.stop();
            } catch (Exception e) {
                throw new RepositoryException(e);
            }
        }
        listeners.clear();
    }

    /**
     * Delete all unused items in the data store.
     * 
     * @return the number of deleted items
     */
    public int deleteUnused() throws RepositoryException {
        checkScanStarted();
        checkScanStopped();
        return store.deleteAllOlderThan(startScanTimestamp);
    }

    private void checkScanStarted() throws RepositoryException {
        if (startScanTimestamp == 0) {
            throw new RepositoryException("scan must be called first");
        }
    }

    private void checkScanStopped() throws RepositoryException {
        if (listeners.size() > 0) {
            throw new RepositoryException("stopScan must be called first");
        }
    }

    /**
     * Get the data store if one is used.
     * 
     * @return the data store, or null
     */
    public DataStore getDataStore() {
        return store;
    }

    private void recurse(final Node n, int sleep) throws RepositoryException,
            IllegalStateException, IOException {
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
        for (PropertyIterator it = n.getProperties(); it.hasNext();) {
            PropertyImpl p = (PropertyImpl) it.nextProperty();
            if (p.getType() == PropertyType.BINARY) {
                if (n.hasProperty("jcr:uuid")) {
                    rememberNode(n.getProperty("jcr:uuid").getString());
                } else {
                    rememberNode(n.getPath());
                }
                if (p.isMultiple()) {
                    p.getLengths();
                } else {
                    p.getLength();
                }
            }
        }
        if (callback != null) {
            callback.afterScanning(n);
        }
        for (NodeIterator it = n.getNodes(); it.hasNext();) {
            recurse(it.nextNode(), sleep);
        }
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
    
    /**
     * Cleanup resources used internally by this instance.
     */
    public void close() {
        if (!closed.set(true)) {
            for (int i = 0; i < sessionList.length; i++) {
                sessionList[i].logout();
            }
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

        private final Session session;

        private final ObservationManager manager;

        private Exception lastException;

        Listener(Session session)
                throws UnsupportedRepositoryOperationException,
                RepositoryException {
            this.session = session;
            Workspace ws = session.getWorkspace();
            manager = ws.getObservationManager();
            manager.addEventListener(this, Event.NODE_ADDED, "/", true, null,
                    null, false);
        }

        void stop() throws Exception {
            if (lastException != null) {
                throw lastException;
            }
            manager.removeEventListener(this);
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
                    lastException = e;
                }
            }
        }
    }

}
