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

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;

import java.io.IOException;
import java.util.ArrayList;

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
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;

/**
 * Garbage collector for DataStore. This implementation is iterates through all
 * nodes and reads the binary properties. To detect nodes that are moved while
 * the scan runs, event listeners are started. Like the well known garbage
 * collection in Java, the items that are still in use are marked. Currently
 * this achived by updating the modified date of the entries. Newly added
 * entries are detected because the modified date is changed when they are
 * added.
 * 
 */
public class GarbageCollector {

    private final ScanEventListener callback;

    private final int sleepBetweenNodes;
    
    private int testDelay;

    private DataStore store;

    private long startScanTimestamp;

    private ArrayList listeners = new ArrayList();

    // TODO It should be possible to stop and restart a garbage collection scan.
    // TODO It may be possible to delete files early, see rememberNode()

    /**
     * Create a new garbage collector. 
     * To display the progress, a callback object may be used.
     * 
     * @param callback if set, this is called while scanning
     * @param sleepBetweenNodes the number of milliseconds to sleep in the main scan loop (0 if the scan should run at full speed)
     */
    public GarbageCollector(ScanEventListener callback, int sleepBetweenNodes) {
        this.sleepBetweenNodes = sleepBetweenNodes;
        this.callback = callback;
    }
    
    /**
     * When testing the garbage collection, a delay is used instead of simulating concurrent access.
     *  
     * @param testDelay the delay in milliseconds
     */
    public void setTestDelay(int testDelay) {
        this.testDelay = testDelay;
    }

    public void scan(Session session) throws RepositoryException,
            IllegalStateException, IOException {
        long now = System.currentTimeMillis();
        if (startScanTimestamp == 0) {
            RepositoryImpl rep = (RepositoryImpl) session.getRepository();
            store = rep.getDataStore();
            startScanTimestamp = now;
            store.updateModifiedDateOnRead(startScanTimestamp);
        }

        // add a listener to get 'new' nodes
        // actually, new nodes are not the problem, but moved nodes
        listeners.add(new Listener(session));

        // adding a link to a BLOB updates the modified date
        // reading usually doesn't, but when scanning, it does
        recurse(session.getRootNode(), sleepBetweenNodes);
    }

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
            Property p = it.nextProperty();
            if (p.getType() == PropertyType.BINARY) {
                if (n.hasProperty("jcr:uuid")) {
                    rememberNode(n.getProperty("jcr:uuid").getString());
                } else {
                    rememberNode(n.getPath());
                }
                if (p.getDefinition().isMultiple()) {
                    Value[] list = p.getValues();
                    for (int i = 0; i < list.length; i++) {
                        list[i].getStream().close();
                    }
                } else {
                    p.getStream().close();
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
         */
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
