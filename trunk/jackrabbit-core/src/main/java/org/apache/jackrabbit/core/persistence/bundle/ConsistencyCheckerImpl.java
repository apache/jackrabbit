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
package org.apache.jackrabbit.core.persistence.bundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.cluster.ClusterException;
import org.apache.jackrabbit.core.cluster.Update;
import org.apache.jackrabbit.core.cluster.UpdateEventChannel;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.observation.EventState;
import org.apache.jackrabbit.core.persistence.check.ConsistencyCheckListener;
import org.apache.jackrabbit.core.persistence.check.ConsistencyReport;
import org.apache.jackrabbit.core.persistence.check.ConsistencyReportImpl;
import org.apache.jackrabbit.core.persistence.check.ReportItem;
import org.apache.jackrabbit.core.persistence.util.NodeInfo;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.DummyUpdateEventChannel;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsistencyCheckerImpl {

    private static Logger log = LoggerFactory.getLogger(ConsistencyCheckerImpl.class);

    /**
     * The number of nodes to fetch at once from the persistence manager. Defaults to 8kb
     */
    private static final int NODESATONCE = Integer.getInteger("org.apache.jackrabbit.checker.nodesatonce", 1024 * 8);

    /**
     * Attribute name used to store the size of the update.
     */
    private static final String ATTRIBUTE_UPDATE_SIZE = "updateSize";

    private final AbstractBundlePersistenceManager pm;
    private final ConsistencyCheckListener listener;
    private NodeId lostNFoundId;
    private UpdateEventChannel eventChannel = new DummyUpdateEventChannel();
    private Map<NodeId, NodePropBundle> bundles;
    private List<ConsistencyCheckerError> errors;
    private int nodeCount;
    private long elapsedTime;

    public ConsistencyCheckerImpl(AbstractBundlePersistenceManager pm, ConsistencyCheckListener listener,
                                  String lostNFoundId, final UpdateEventChannel eventChannel) {
        this.pm = pm;
        this.listener = listener;
        if (lostNFoundId != null) {
            this.lostNFoundId = new NodeId(lostNFoundId);
        }
        if (eventChannel != null) {
            this.eventChannel = eventChannel;
        }
    }

    /**
     * Check the database for inconsistencies.
     *
     * @param uuids a list of node identifiers to check or {@code null} in order to check all nodes
     * @param recursive  whether to recursively check the subtrees below the nodes identified by the provided uuids
     * @throws RepositoryException
     */
    public void check(String[] uuids, boolean recursive) throws RepositoryException {
        errors = new ArrayList<ConsistencyCheckerError>();
        long tstart = System.currentTimeMillis();
        nodeCount = internalCheckConsistency(uuids, recursive);
        elapsedTime = System.currentTimeMillis() - tstart;
    }

    /**
     * Do a double check on the errors found during {@link #check}.
     * Removes all false positives from the report.
     */
    public void doubleCheckErrors() {
        if (hasErrors()) {
            final Iterator<ConsistencyCheckerError> errorIterator = errors.iterator();
            while (errorIterator.hasNext()) {
                final ConsistencyCheckerError error = errorIterator.next();
                try {
                    if (!error.doubleCheck()) {
                        info(null, "False positive: " + error);
                        errorIterator.remove();
                    }
                } catch (ItemStateException e) {
                    error(null, "Failed to double check error: " + error, e);
                }
            }
        }
    }

    /**
     * Return the report of a consistency {@link #check} / {@link #doubleCheckErrors()} / {@link #repair}
     */
    public ConsistencyReport getReport() {
        final Set<ReportItem> reportItems = new HashSet<ReportItem>();
        if (hasErrors()) {
            for (ConsistencyCheckerError error : errors) {
                reportItems.add(error.getReportItem());
            }
        }
        return new ConsistencyReportImpl(nodeCount, elapsedTime, reportItems);
    }

    /**
     * Repair any errors found during a {@link #check}. Should be run after a {#check} and
     * (if needed) {@link #doubleCheckErrors}.
     *
     * @throws RepositoryException
     */
    public void repair() throws RepositoryException {
        checkLostNFound();
        bundles = new HashMap<NodeId, NodePropBundle>();
        if (hasRepairableErrors()) {
            boolean successful = false;
            final CheckerUpdate update = new CheckerUpdate();
            try {
                eventChannel.updateCreated(update);
                for (ConsistencyCheckerError error : errors) {
                    if (error.isRepairable()) {
                        try {
                            error.repair(update.getChanges());
                            info(null, "Repairing " + error);
                        } catch (ItemStateException e) {
                            error(null, "Failed to repair error: " + error, e);
                        }
                    }
                }

                final ChangeLog changes = update.getChanges();
                if (changes.hasUpdates()) {
                    eventChannel.updatePrepared(update);
                    for (NodePropBundle bundle : bundles.values()) {
                        storeBundle(bundle);
                    }
                    update.setAttribute(ATTRIBUTE_UPDATE_SIZE, changes.getUpdateSize());
                    successful = true;
                }
            } catch (ClusterException e) {
                throw new RepositoryException("Cannot create update", e);
            } finally {
                if (successful) {
                    eventChannel.updateCommitted(update, "checker@");
                } else {
                    eventChannel.updateCancelled(update);
                }
            }
        }
    }

    private boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    private boolean hasRepairableErrors() {
        if (hasErrors()) {
            for (ConsistencyCheckerError error : errors) {
                if (error.isRepairable()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void checkLostNFound() {
        if (lostNFoundId != null) {
            // do we have a "lost+found" node?
            try {
                NodePropBundle lfBundle = pm.loadBundle(lostNFoundId);
                if (lfBundle == null) {
                    error(lostNFoundId.toString(), "Specified 'lost+found' node does not exist");
                    lostNFoundId = null;
                } else if (!NameConstants.NT_UNSTRUCTURED.equals(lfBundle .getNodeTypeName())) {
                    error(lostNFoundId.toString(), "Specified 'lost+found' node is not of type nt:unstructured");
                    lostNFoundId = null;
                }
            } catch (Exception ex) {
                error(lostNFoundId.toString(), "finding 'lost+found' folder", ex);
                lostNFoundId = null;
            }
        } else {
            info(null, "No 'lost+found' node specified: orphans cannot be fixed");
        }
    }

    private int internalCheckConsistency(String[] uuids, boolean recursive) throws RepositoryException {
        int count = 0;

        if (uuids == null) {
            // check all nodes
            try {
                Map<NodeId, NodeInfo> batch = pm.getAllNodeInfos(null, NODESATONCE);
                Map<NodeId, NodeInfo> allInfos = batch;

                NodeId lastId = null;
                while (!batch.isEmpty()) {

                    for (Map.Entry<NodeId, NodeInfo> entry : batch.entrySet()) {
                        lastId = entry.getKey();

                        count++;
                        if (count % 1000 == 0) {
                            log.info(pm + ": loaded " + count + " infos...");
                        }

                    }

                    batch = pm.getAllNodeInfos(lastId, NODESATONCE);

                    allInfos.putAll(batch);
                }

                if (lastId == null) {
                    log.info("No nodes exists, skipping");
                } else if (pm.exists(lastId)) {
                    for (Map.Entry<NodeId, NodeInfo> entry : allInfos.entrySet()) {
                        checkBundleConsistency(entry.getKey(), entry.getValue(), allInfos);
                    }
                } else {
                    log.info("Failed to read all nodes, starting over");
                    internalCheckConsistency(uuids, recursive);
                }

            } catch (ItemStateException e) {
                throw new RepositoryException("Error loading nodes", e);
            } finally {
                NodeInfo.clearPool();
            }
        } else {
            // check only given uuids, handle recursive flag

            List<NodeId> idList = new ArrayList<NodeId>(uuids.length);
            for (final String uuid : uuids) {
                try {
                    idList.add(new NodeId(uuid));
                } catch (IllegalArgumentException e) {
                    error(uuid, "Invalid id for consistency check, skipping: '" + uuid + "': " + e);
                }
            }

            for (int i = 0; i < idList.size(); i++) {
                NodeId id = idList.get(i);
                try {
                    final NodePropBundle bundle = pm.loadBundle(id);
                    if (bundle == null) {
                        if (!isVirtualNode(id)) {
                            error(id.toString(), "No bundle found for id '" + id + "'");
                        }
                    } else {
                        checkBundleConsistency(id, new NodeInfo(bundle), Collections.<NodeId, NodeInfo>emptyMap());

                        if (recursive) {
                            for (NodePropBundle.ChildNodeEntry entry : bundle.getChildNodeEntries()) {
                                idList.add(entry.getId());
                            }
                        }

                        count++;
                        if (count % 1000 == 0 && listener == null) {
                            log.info(pm + ": checked " + count + "/" + idList.size() + " bundles...");
                        }
                    }
                } catch (ItemStateException ignored) {
                    // problem already logged
                }
            }
        }

        log.info(pm + ": checked " + count + " bundles.");

        return count;
    }

    /**
     * Checks a single bundle for inconsistencies, ie. inexistent child nodes, inexistent parents, and other
     * structural inconsistencies.
     *
     * @param nodeId node id for the bundle to check
     * @param nodeInfo the node info for the node to check
     * @param infos all the {@link NodeInfo}s loaded in the current batch
     */
    private void checkBundleConsistency(NodeId nodeId, NodeInfo nodeInfo, Map<NodeId, NodeInfo> infos) {

        // skip all virtual nodes
        if (!isRoot(nodeId) && isVirtualNode(nodeId)) {
            return;
        }

        if (listener != null) {
            listener.startCheck(nodeId.toString());
        }

        // check the children
        for (final NodeId childNodeId : nodeInfo.getChildren()) {

            if (isVirtualNode(childNodeId)) {
                continue;
            }

            NodeInfo childNodeInfo = infos.get(childNodeId);

            if (childNodeInfo == null) {
                addError(new MissingChild(nodeId, childNodeId));
            } else {
                if (!nodeId.equals(childNodeInfo.getParentId())) {
                    addError(new DisconnectedChild(nodeId, childNodeId, childNodeInfo.getParentId()));
                }
            }
        }

        // check the parent
        NodeId parentId = nodeInfo.getParentId();
        // skip root nodes
        if (parentId != null && !isRoot(nodeId)) {
            NodeInfo parentInfo = infos.get(parentId);

            if (parentInfo == null) {
                addError(new OrphanedNode(nodeId, parentId));
            } else {
                // if the parent exists, does it have a child node entry for us?
                boolean found = false;

                for (NodeId childNodeId : parentInfo.getChildren()) {
                    if (childNodeId.equals(nodeId)){
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    addError(new AbandonedNode(nodeId, parentId));
                }

            }
        }
    }

    protected boolean isVirtualNode(NodeId nodeId) {
        return nodeId.toString().endsWith("babecafebabe");
    }

    private boolean isRoot(NodeId nodeId) {
        return "cafebabe-cafe-babe-cafe-babecafebabe".equals(nodeId.toString());
    }

    private void addError(ConsistencyCheckerError error) {
        if (listener != null) {
            listener.report(error.getReportItem());
        }
        errors.add(error);
    }

    private void info(String id, String message) {
        if (this.listener == null) {
            String idstring = id == null ? "" : ("Node " + id + ": ");
            log.info(idstring + message);
        } else {
            listener.info(id, message);
        }
    }

    private void error(String id, String message) {
        if (this.listener == null) {
            String idstring = id == null ? "" : ("Node " + id + ": ");
            log.error(idstring + message);
        } else {
            listener.error(id, message);
        }
    }

    private void error(String id, String message, Throwable ex) {
        String idstring = id == null ? "" : ("Node " + id + ": ");
        log.error(idstring + message, ex);
        if (listener != null) {
            listener.error(id, message);
        }
    }

    private void storeBundle(NodePropBundle bundle) {
        try {
            bundle.markOld();
            bundle.setModCount((short) (bundle.getModCount()+1));
            pm.storeBundle(bundle);
            pm.evictBundle(bundle.getId());
        } catch (ItemStateException e) {
            log.error(pm + ": Error storing fixed bundle: " + e);
        }
    }

    private NodePropBundle getBundle(NodeId nodeId) throws ItemStateException {
        if (bundles.containsKey(nodeId)) {
            return bundles.get(nodeId);
        }
        return pm.loadBundle(nodeId);
    }

    private void saveBundle(NodePropBundle bundle) {
        bundles.put(bundle.getId(), bundle);
    }

    /**
     * A missing child is when the node referred to by a child node entry
     * does not exist.
     *
     * This type of error is repaired by removing the corrupted child node entry.
     */
    private class MissingChild extends ConsistencyCheckerError {

        private final NodeId childNodeId;

        private MissingChild(final NodeId nodeId, final NodeId childNodeId) {
            super(nodeId, "NodeState '" + nodeId + "' references inexistent child '" + childNodeId + "'");
            this.childNodeId = childNodeId;
        }

        @Override
        ReportItem.Type getType() {
            return ReportItem.Type.MISSING;
        }

        @Override
        boolean isRepairable() {
            return true;
        }

        @Override
        void doRepair(final ChangeLog changes) throws ItemStateException {
            final NodePropBundle bundle = getBundle(nodeId);
            final Iterator<NodePropBundle.ChildNodeEntry> entryIterator = bundle.getChildNodeEntries().iterator();
            while (entryIterator.hasNext()) {
                final NodePropBundle.ChildNodeEntry childNodeEntry = entryIterator.next();
                if (childNodeEntry.getId().equals(childNodeId)) {
                    entryIterator.remove();
                    saveBundle(bundle);
                    changes.modified(new NodeState(nodeId, null, null, ItemState.STATUS_EXISTING, false));
                }
            }
        }

        @Override
        boolean doubleCheck() throws ItemStateException {
            final NodePropBundle childBundle = pm.loadBundle(childNodeId);
            if (childBundle == null) {
                final NodePropBundle bundle = pm.loadBundle(nodeId);
                if (bundle != null) {
                    for (NodePropBundle.ChildNodeEntry entry : bundle.getChildNodeEntries()) {
                        if (entry.getId().equals(childNodeId)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    /**
     * A disconnected child is when a child node entry refers to a node
     * that exists, but that node actually has a different parent.
     *
     * This type of error is repaired by removing the corrupted child node entry.
     */
    private class DisconnectedChild extends ConsistencyCheckerError {

        private final NodeId childNodeId;

        DisconnectedChild(final NodeId nodeId, final NodeId childNodeId, final NodeId invalidParentId) {
            super(nodeId, "Node has invalid parent id: '" + invalidParentId + "' (instead of '" + nodeId + "')");
            this.childNodeId = childNodeId;
        }

        @Override
        ReportItem.Type getType() {
            return ReportItem.Type.DISCONNECTED;
        }

        @Override
        boolean isRepairable() {
            return true;
        }

        @Override
        void doRepair(final ChangeLog changes) throws ItemStateException {
            NodePropBundle bundle = getBundle(nodeId);
            final Iterator<NodePropBundle.ChildNodeEntry> entryIterator = bundle.getChildNodeEntries().iterator();
            while (entryIterator.hasNext()) {
                final NodePropBundle.ChildNodeEntry childNodeEntry = entryIterator.next();
                if (childNodeEntry.getId().equals(childNodeId)) {
                    entryIterator.remove();
                    saveBundle(bundle);
                    changes.modified(new NodeState(nodeId, null, null, ItemState.STATUS_EXISTING, false));
                    break;
                }
            }
        }

        @Override
        boolean doubleCheck() throws ItemStateException {
            final NodePropBundle childBundle = pm.loadBundle(childNodeId);
            if (childBundle != null && !childBundle.getParentId().equals(nodeId)) {
                final NodePropBundle bundle = pm.loadBundle(nodeId);
                if (bundle != null) {
                    // double check if the child node entry is still there
                    for (NodePropBundle.ChildNodeEntry entry : bundle.getChildNodeEntries()) {
                        if (entry.getId().equals(childNodeId)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    /**
     * An orphaned node is a node whose parent does not exist.
     *
     * This type of error is repaired by reattaching the orphan to
     * a special purpose 'lost and found' node.
     */
    private class OrphanedNode extends ConsistencyCheckerError {

        private final NodeId parentNodeId;

        OrphanedNode(final NodeId nodeId, final NodeId parentNodeId) {
            super(nodeId, "NodeState '" + nodeId + "' references inexistent parent id '" + parentNodeId + "'");
            this.parentNodeId = parentNodeId;
        }

        @Override
        ReportItem.Type getType() {
            return ReportItem.Type.ORPHANED;
        }

        @Override
        boolean isRepairable() {
            return lostNFoundId != null;
        }

        @Override
        void doRepair(final ChangeLog changes) throws ItemStateException {
            if (lostNFoundId != null) {
                final NodePropBundle bundle = getBundle(nodeId);
                final NodePropBundle lfBundle = getBundle(lostNFoundId);

                final String nodeName = nodeId + "-" + System.currentTimeMillis();
                final NameFactory nameFactory = NameFactoryImpl.getInstance();
                lfBundle.addChildNodeEntry(nameFactory.create("", nodeName), nodeId);
                bundle.setParentId(lostNFoundId);

                saveBundle(bundle);
                saveBundle(lfBundle);

                changes.modified(new NodeState(lostNFoundId, null, null, ItemState.STATUS_EXISTING, false));
                changes.modified(new NodeState(nodeId, null, null, ItemState.STATUS_EXISTING, false));
            }
        }

        @Override
        boolean doubleCheck() throws ItemStateException {
            final NodePropBundle parentBundle = pm.loadBundle(parentNodeId);
            if (parentBundle == null) {
                final NodePropBundle bundle = pm.loadBundle(nodeId);
                if (bundle != null) {
                    if (parentNodeId.equals(bundle.getParentId())) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * An abandoned node is a node that points to an existing node
     * as its parent, but that parent node does not have a corresponding
     * child node entry for the child.
     *
     * This type of error is repaired by adding the missing child node entry
     * to the parent.
     */
    private class AbandonedNode extends ConsistencyCheckerError {

        private final NodeId nodeId;
        private final NodeId parentNodeId;

        AbandonedNode(final NodeId nodeId, final NodeId parentNodeId) {
            super(nodeId, "NodeState '" + nodeId + "' is not referenced by its parent node '" + parentNodeId + "'");
            this.nodeId = nodeId;
            this.parentNodeId = parentNodeId;
        }

        @Override
        ReportItem.Type getType() {
            return ReportItem.Type.ABANDONED;
        }

        @Override
        boolean isRepairable() {
            return true;
        }

        @Override
        void doRepair(final ChangeLog changes) throws ItemStateException {
            final NodePropBundle parentBundle = getBundle(parentNodeId);

            parentBundle.addChildNodeEntry(createNodeName(), nodeId);

            saveBundle(parentBundle);
            changes.modified(new NodeState(parentNodeId, null, null, ItemState.STATUS_EXISTING, false));
        }

        private Name createNodeName() {
            int n = (int) System.currentTimeMillis() + new Random().nextInt();
            final String localName = Integer.toHexString(n);
            final NameFactory nameFactory = NameFactoryImpl.getInstance();
            return nameFactory.create("{}" + localName);
        }

        @Override
        boolean doubleCheck() throws ItemStateException {
            final NodePropBundle parentBundle = pm.loadBundle(parentNodeId);
            if (parentBundle != null) {
                for (NodePropBundle.ChildNodeEntry entry : parentBundle.getChildNodeEntries()) {
                    if (entry.getId().equals(nodeId)) {
                        return false;
                    }
                }
            }
            final NodePropBundle bundle = pm.loadBundle(nodeId);
            if (bundle != null) {
                if (parentNodeId.equals(bundle.getParentId())) {
                    return true;
                }
            }
            return false;
        }
    }

    private class CheckerUpdate implements Update {

        private final Map<String, Object> attributes = new HashMap<String, Object>();
        private final ChangeLog changeLog = new ChangeLog();
        private final long timestamp = System.currentTimeMillis();

        @Override
        public void setAttribute(final String name, final Object value) {
            attributes.put(name, value);
        }

        @Override
        public Object getAttribute(final String name) {
            return attributes.get(name);
        }

        @Override
        public ChangeLog getChanges() {
            return changeLog;
        }

        @Override
        public List<EventState> getEvents() {
            return Collections.emptyList();
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String getUserData() {
            return null;
        }
    }
}
