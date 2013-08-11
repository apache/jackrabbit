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
package org.apache.jackrabbit.core.retention;

import org.apache.commons.io.IOUtils;
import javax.jcr.retention.Hold;
import javax.jcr.retention.RetentionPolicy;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.PropertyImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.observation.SynchronousEventListener;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.name.PathMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * <code>RetentionEvaluatorImpl</code>...
 */
public class RetentionRegistryImpl implements RetentionRegistry, SynchronousEventListener {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(RetentionRegistryImpl.class);
    /**
     * Name of the file storing the existing retention/holds
     */
    private static final String FILE_NAME = "retention";

    private final PathMap<RetentionPolicyImpl> retentionMap =
        new PathMap<RetentionPolicyImpl>();

    private final PathMap<List<HoldImpl>> holdMap = new PathMap<List<HoldImpl>>();

    private final SessionImpl session;
    private final FileSystemResource retentionFile;

    private long holdCnt;
    private long retentionCnt;

    private boolean initialized;

    public RetentionRegistryImpl(SessionImpl session, FileSystem fs) throws RepositoryException {
        this.session = session;
        this.retentionFile = new FileSystemResource(fs, FileSystem.SEPARATOR + FILE_NAME);

        // start listening to added/changed or removed holds and retention policies.
        Workspace wsp = session.getWorkspace();
        // register event listener to be informed about new/removed holds and
        // retention policies.
        int types = Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED | Event.PROPERTY_CHANGED;
        String[] ntFilter = new String[] {session.getJCRName(RetentionManagerImpl.REP_RETENTION_MANAGEABLE)};
        wsp.getObservationManager().addEventListener(this, types, "/", true, null, ntFilter, false);

        // populate the retentionMap and the holdMap with the effective
        // holds and retention policies present within the content.
        try {
            readRetentionFile();
        } catch (FileSystemException e) {
            throw new RepositoryException("Error while reading retention/holds from '" + retentionFile.getPath() + "'", e);
        } catch (IOException e) {
            throw new RepositoryException("Error while reading retention/holds from '" + retentionFile.getPath() + "'", e);
        }
        initialized = true;
    }

    /**
     * Read the file system resource containing the node ids of those nodes
     * contain holds/retention policies and populate the 2 path maps.
     *
     * If an entry in the retention file doesn't have a corresponding entry
     * (either rep:hold property or rep:retentionPolicy property at the
     * node identified by the node id entry) or doesn't correspond to an existing
     * node, that entry will be ignored. Upon {@link #close()} of this
     * manager, the file will be updated to reflect the actual set of holds/
     * retentions present and effective in the content.
     *
     * @throws IOException
     * @throws FileSystemException
     */
    private void readRetentionFile() throws IOException, FileSystemException {
        if (retentionFile.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(retentionFile.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    NodeId nodeId = NodeId.valueOf(line);
                    try {
                        NodeImpl node = (NodeImpl) session.getItemManager().getItem(nodeId);
                        Path nodePath = node.getPrimaryPath();

                        if (node.hasProperty(RetentionManagerImpl.REP_HOLD)) {
                            PropertyImpl prop = node.getProperty(RetentionManagerImpl.REP_HOLD);
                            addHolds(nodePath, prop);
                        }
                        if (node.hasProperty(RetentionManagerImpl.REP_RETENTION_POLICY)) {
                            PropertyImpl prop = node.getProperty(RetentionManagerImpl.REP_RETENTION_POLICY);
                            addRetentionPolicy(nodePath, prop);
                        }
                    } catch (RepositoryException e) {
                        // node doesn't exist any more or hold/retention has been removed.
                        // ignore. upon close() the file will not contain the given nodeId
                        // any more.
                        log.warn("Unable to read retention policy / holds from node '" + nodeId + "': " + e.getMessage());
                    }
                }
            } finally {
                IOUtils.closeQuietly(reader);
            }
        }
    }

    /**
     * Write back the file system resource containing the node ids of those
     * nodes containing holds and/or retention policies. Each node id is
     * present only once.
     */
    private void writeRetentionFile() {
        final Set<NodeId> nodeIds = new HashSet<NodeId>();

        // first look for nodes containing holds
        holdMap.traverse(new PathMap.ElementVisitor<List<HoldImpl>>() {
            public void elementVisited(PathMap.Element<List<HoldImpl>> element) {
                List<HoldImpl> holds = element.get();
                if (!holds.isEmpty()) {
                    nodeIds.add(holds.get(0).getNodeId());
                }
            }
        }, false);

        // then collect ids of nodes having an retention policy
        retentionMap.traverse(new PathMap.ElementVisitor<RetentionPolicyImpl>() {
            public void elementVisited(PathMap.Element<RetentionPolicyImpl> element) {
                nodeIds.add(element.get().getNodeId());
            }
        }, false);

        if (!nodeIds.isEmpty()) {
            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(new OutputStreamWriter(retentionFile.getOutputStream()));
                for (Iterator<NodeId> it = nodeIds.iterator(); it.hasNext();) {
                    writer.write(it.next().toString());
                    if (it.hasNext()) {
                        writer.newLine();
                    }
                }
            } catch (FileSystemException fse) {
                log.error("Error while saving locks to '" + retentionFile.getPath() + "': " + fse.getMessage());
            } catch (IOException ioe) {
                log.error("Error while saving locks to '" + retentionFile.getPath() + "': " + ioe.getMessage());
            } finally {
                IOUtils.closeQuietly(writer);
            }
        }
    }

    public void close()  {
        writeRetentionFile();
        initialized = false;
    }

    private void addHolds(Path nodePath, PropertyImpl p) throws RepositoryException {
        synchronized (holdMap) {
            HoldImpl[] holds = HoldImpl.createFromProperty(p, ((PropertyId) p.getId()).getParentId());
            holdMap.put(nodePath, Arrays.asList(holds));
            holdCnt++;
        }
    }

    private void removeHolds(Path nodePath) {
        synchronized (holdMap) {
            PathMap.Element<List<HoldImpl>> el = holdMap.map(nodePath, true);
            if (el != null) {
                el.remove();
                holdCnt--;
            } // else: no entry for holds on nodePath (should not occur)
        }
    }

    private void addRetentionPolicy(Path nodePath, PropertyImpl p) throws RepositoryException {
        synchronized (retentionMap) {
            RetentionPolicyImpl rp = new RetentionPolicyImpl(
                    p.getString(), ((PropertyId) p.getId()).getParentId(), session);
            retentionMap.put(nodePath, rp);
            retentionCnt++;
        }
    }

    private void removeRetentionPolicy(Path nodePath) {
        synchronized (retentionMap) {
            PathMap.Element<RetentionPolicyImpl> el =
                retentionMap.map(nodePath, true);
            if (el != null) {
                el.remove();
                retentionCnt--;
            } // else: no entry for holds on nodePath (should not occur)
        }
    }

    //--------------------------------------------------< RetentionRegistry >---
    /**
     * @see RetentionRegistry#hasEffectiveHold(org.apache.jackrabbit.spi.Path,boolean)
     */
    public boolean hasEffectiveHold(Path nodePath, boolean checkParent) throws RepositoryException {
        if (!initialized) {
            throw new IllegalStateException("Not initialized.");
        }
        if (holdCnt <= 0) {
            return false;
        }
        PathMap.Element<List<HoldImpl>> element = holdMap.map(nodePath, false);
        List<HoldImpl> holds = element.get();
        if (holds != null) {
            if (element.hasPath(nodePath)) {
                // one or more holds on the specified path
                return true;
            } else if (checkParent && !nodePath.denotesRoot() &&
                    element.hasPath(nodePath.getAncestor(1))) {
                // hold present on the parent node without checking for being
                // a deep hold.
                // this required for removal of a node that can be inhibited
                // by a hold on the node itself, by a hold on the parent or
                // by a deep hold on any ancestor.
                return true;
            } else {
                for (Hold hold : holds) {
                    if (hold.isDeep()) {
                        return true;
                    }
                }
            }
        }
        // no hold at path or no deep hold on parent.
        return false;
    }

    /**
     * @see RetentionRegistry#hasEffectiveRetention(org.apache.jackrabbit.spi.Path,boolean)
     */
    public boolean hasEffectiveRetention(Path nodePath, boolean checkParent) throws RepositoryException {
        if (!initialized) {
            throw new IllegalStateException("Not initialized.");
        }
        if (retentionCnt <= 0) {
            return false;
        }
        RetentionPolicy rp = null;
        PathMap.Element<RetentionPolicyImpl> element = retentionMap.map(nodePath, true);
        if (element != null) {
            rp = element.get();
        }
        if (rp == null && checkParent && (!nodePath.denotesRoot())) {
            element = retentionMap.map(nodePath.getAncestor(1), true);
            if (element != null) {
                rp = element.get();
            }
        }
        return rp != null;
    }

    //-------------------------------------------< SynchronousEventListener >---
    /**
     * @param events Events reporting hold/retention policy changes.
     */
    public void onEvent(EventIterator events) {
        while (events.hasNext()) {
            Event ev = events.nextEvent();
            try {
                Path evPath = session.getQPath(ev.getPath());
                Path nodePath = evPath.getAncestor(1);
                Name propName = evPath.getName();

                if (RetentionManagerImpl.REP_HOLD.equals(propName)) {
                    // hold changes
                    switch (ev.getType()) {
                        case Event.PROPERTY_ADDED:
                        case Event.PROPERTY_CHANGED:
                            // build the Hold objects from the rep:hold property
                            // and put them into the hold map.
                            PropertyImpl p = (PropertyImpl) session.getProperty(ev.getPath());
                            addHolds(nodePath, p);
                            break;
                        case Event.PROPERTY_REMOVED:
                            // all holds present on this node were remove
                            // -> remove the corresponding entry in the holdMap.
                            removeHolds(nodePath);
                            break;
                    }
                } else if (RetentionManagerImpl.REP_RETENTION_POLICY.equals(propName)) {
                    // retention policy changes
                    switch (ev.getType()) {
                        case Event.PROPERTY_ADDED:
                        case Event.PROPERTY_CHANGED:
                            // build the RetentionPolicy objects from the rep:retentionPolicy property
                            // and put it into the retentionMap.
                            PropertyImpl p = (PropertyImpl) session.getProperty(ev.getPath());
                            addRetentionPolicy(nodePath, p);
                            break;
                        case Event.PROPERTY_REMOVED:
                            // retention policy present on this node was remove
                            // -> remove the corresponding entry in the retentionMap.
                            removeRetentionPolicy(nodePath);
                            break;
                    }
                }
                // else: not interested in any other property -> ignore.

            } catch (RepositoryException e) {
                log.warn("Internal error while processing event. {}", e.getMessage());
                // ignore.
            }
        }
    }
}
