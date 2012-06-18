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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.persistence.check.ConsistencyCheckListener;
import org.apache.jackrabbit.core.persistence.check.ConsistencyReport;
import org.apache.jackrabbit.core.persistence.check.ConsistencyReportImpl;
import org.apache.jackrabbit.core.persistence.check.ReportItem;
import org.apache.jackrabbit.core.persistence.check.ReportItemImpl;
import org.apache.jackrabbit.core.persistence.util.NodeInfo;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsistencyCheckerImpl {

    /** the default logger */
    private static Logger log = LoggerFactory.getLogger(ConsistencyCheckerImpl.class);

    private final AbstractBundlePersistenceManager pm;

    private final ConsistencyCheckListener listener;

    private static final NameFactory NF = NameFactoryImpl.getInstance();

    /**
     * The number of nodes to fetch at once from the persistence manager. Defaults to 8kb
     */
    private static final int NODESATONCE = Integer.getInteger("org.apache.jackrabbit.checker.nodesatonce", 1024 * 8);

    /**
     * Whether to load all node infos before checking or to check nodes as they are loaded.
     * The former is magnitudes faster but requires more memory.
     */
    private static final boolean CHECKAFTERLOADING = Boolean.getBoolean("org.apache.jackrabbit.checker.checkafterloading");

    public ConsistencyCheckerImpl(AbstractBundlePersistenceManager pm,
            ConsistencyCheckListener listener) {
        this.pm = pm;
        this.listener = listener;
    }

    public ConsistencyReport check(String[] uuids, boolean recursive,
            boolean fix, String lostNFoundId) throws RepositoryException {
        Set<ReportItem> reports = new HashSet<ReportItem>();

        long tstart = System.currentTimeMillis();
        int total = internalCheckConsistency(uuids, recursive, fix, reports, lostNFoundId);
        long elapsed = System.currentTimeMillis() - tstart;

        return new ConsistencyReportImpl(total, elapsed, reports);
    }

    private int internalCheckConsistency(String[] uuids, boolean recursive,
            boolean fix, Set<ReportItem> reports, String lostNFoundId)
            throws RepositoryException {
        int count = 0;

        NodeId lostNFound = null;
        if (fix) {
            if (lostNFoundId != null) {
                // do we have a "lost+found" node?
                try {
                    NodeId tmpid = new NodeId(lostNFoundId);
                    NodePropBundle lfBundle = pm.loadBundle(tmpid);
                    if (lfBundle == null) {
                        error(lostNFoundId, "Specified 'lost+found' node does not exist");
                    } else if (!NameConstants.NT_UNSTRUCTURED.equals(lfBundle .getNodeTypeName())) {
                        error(lostNFoundId, "Specified 'lost+found' node is not of type nt:unstructured");
                    } else {
                        lostNFound = lfBundle.getId();
                    }
                } catch (Exception ex) {
                    error(lostNFoundId, "finding 'lost+found' folder", ex);
                }
            } else {
                log.info("No 'lost+found' node specified: orphans cannot be fixed");
            }
        }

        if (uuids == null) {
            try {
                Map<NodeId, NodeInfo> batch = pm.getAllNodeInfos(null, NODESATONCE);
                Map<NodeId, NodeInfo> allInfos = batch;

                while (!batch.isEmpty()) {
                    NodeId lastId = null;

                    for (Map.Entry<NodeId, NodeInfo> entry : batch.entrySet()) {
                        NodeId id = entry.getKey();
                        lastId = id;

                        count++;
                        if (count % 1000 == 0) {
                            log.info(pm + ": loaded " + count + " infos...");
                        }

                        if (!CHECKAFTERLOADING) {
                            // check immediately
                            NodeInfo nodeInfo = entry.getValue();
                            checkBundleConsistency(id, nodeInfo, fix, lostNFound, reports, batch);
                        }
                    }

                    batch = pm.getAllNodeInfos(lastId, NODESATONCE);

                    if (CHECKAFTERLOADING) {
                        allInfos.putAll(batch);
                    }
                }

                if (CHECKAFTERLOADING) {
                    // check info
                    for (Map.Entry<NodeId, NodeInfo> entry : allInfos.entrySet()) {
                        checkBundleConsistency(entry.getKey(), entry.getValue(), fix, lostNFound, reports, allInfos);
                    }
                }
            } catch (ItemStateException ex) {
                throw new RepositoryException("getting nodeIds", ex);
            }
        } else {
            // check only given uuids, handle recursive flag

            // 1) convert uuid array to modifiable list
            // 2) for each uuid do
            // a) load node bundle
            // b) check bundle, store any bundle-to-be-modified in collection
            // c) if recursive, add child uuids to list of uuids

            List<NodeId> idList = new ArrayList<NodeId>(uuids.length);
            // convert uuid string array to list of UUID objects
            for (int i = 0; i < uuids.length; i++) {
                try {
                    idList.add(new NodeId(uuids[i]));
                } catch (IllegalArgumentException e) {
                    error(uuids[i],
                            "Invalid id for consistency check, skipping: '"
                                    + uuids[i] + "': " + e);
                }
            }

            // iterate over UUIDs (including ones that are newly added inside
            // the loop!)
            for (int i = 0; i < idList.size(); i++) {
                NodeId id = idList.get(i);
                try {
                    // load the node from the database
                    NodePropBundle bundle = pm.loadBundle(id);

                    if (bundle == null) {
                        if (!isVirtualNode(id)) {
                            error(id.toString(), "No bundle found for id '"
                                    + id + "'");
                        }
                    } else {
                        checkBundleConsistency(id, new NodeInfo(bundle), fix, lostNFound,
                                reports, Collections.<NodeId, NodeInfo>emptyMap());

                        if (recursive) {
                            for (NodePropBundle.ChildNodeEntry entry : bundle
                                    .getChildNodeEntries()) {
                                idList.add(entry.getId());
                            }
                        }

                        count++;
                        if (count % 1000 == 0 && listener == null) {
                            log.info(pm + ": checked " + count + "/"
                                    + idList.size() + " bundles...");
                        }
                    }
                } catch (ItemStateException e) {
                    // problem already logged (loadBundle called with
                    // logDetailedErrors=true)
                }
            }
        }

        log.info(pm + ": checked " + count + " bundles.");

        // clear the NodeId pool
        NodeInfo.clearPool();

        return count;
    }

    /**
     * Checks a single bundle for inconsistencies, ie. inexistent child nodes, inexistent parents, and other
     * structural inconsistencies.
     *
     * @param id node id for the bundle to check
     * @param nodeInfo the node info for the node to check
     * @param fix if <code>true</code>, repair things that can be repaired
     * {@linkplain org.apache.jackrabbit.core.persistence.util.NodePropBundle bundles} here
     * @param infos all the {@link NodeInfo}s loaded in the current batch
     */
    private void checkBundleConsistency(NodeId id, NodeInfo nodeInfo,
                                        boolean fix, NodeId lostNFoundId,
                                        Set<ReportItem> reports, Map<NodeId, NodeInfo> infos) {
        // log.info(name + ": checking bundle '" + id + "'");

        // skip all virtual nodes
        if (isVirtualNode(id)) {
            return;
        }

        if (listener != null) {
            listener.startCheck(id.toString());
        }

        // look at the node's children
        Collection<NodePropBundle.ChildNodeEntry> missingChildren = new ArrayList<NodePropBundle.ChildNodeEntry>();
        Collection<NodePropBundle.ChildNodeEntry> disconnectedChildren = new ArrayList<NodePropBundle.ChildNodeEntry>();

        NodePropBundle bundle = null;

        for (final NodeId childNodeId : nodeInfo.getChildren()) {

            // skip check for system nodes (root, system root, version storage,
            // node types)
            if (childNodeId.toString().endsWith("babecafebabe")) {
                continue;
            }

            try {
                // analyze child node bundles
                NodePropBundle childBundle = null;
                NodeInfo childNodeInfo = infos.get(childNodeId);

                String message = null;
                // does the child exist?
                if (childNodeInfo == null) {
                    // try to load the bundle
                    childBundle = pm.loadBundle(childNodeId);
                    if (childBundle == null) {
                        // the child indeed does not exist
                        // double check whether we still exist and the child entry is still there
                        if (bundle == null) {
                            bundle = pm.loadBundle(id);
                        }
                        if (bundle != null) {
                            NodePropBundle.ChildNodeEntry childNodeEntry = null;
                            for (NodePropBundle.ChildNodeEntry entry : bundle.getChildNodeEntries()) {
                                if (entry.getId().equals(childNodeId)) {
                                    childNodeEntry = entry;
                                    break;
                                }
                            }
                            if (childNodeEntry != null) {
                                message = "NodeState '" + id + "' references inexistent child '" + childNodeId + "'";
                                log.error(message);
                                missingChildren.add(childNodeEntry);
                            }
                        } else {
                            return;
                        }
                    } else {
                        // exists after all
                        childNodeInfo = new NodeInfo(childBundle);
                    }
                }
                if (childNodeInfo != null) {
                    // if the child exists does it reference the current node as its parent?
                    NodeId cp = childNodeInfo.getParentId();
                    if (!id.equals(cp)) {
                        // double check whether the child still has a different parent
                        if (childBundle == null) {
                            childBundle = pm.loadBundle(childNodeId);
                        }
                        if (childBundle != null && !childBundle.getParentId().equals(id)) {
                            // double check if we still exist
                            if (bundle == null) {
                                bundle = pm.loadBundle(id);
                            }
                            if (bundle != null) {
                                // double check if the child node entry is still there
                                NodePropBundle.ChildNodeEntry childNodeEntry = null;
                                for (NodePropBundle.ChildNodeEntry entry : bundle.getChildNodeEntries()) {
                                    if (entry.getId().equals(childNodeId)) {
                                        childNodeEntry = entry;
                                        break;
                                    }
                                }
                                if (childNodeEntry != null) {
                                    // indeed we have a disconnected child
                                    message = "ChildNode has invalid parent id: '" + cp + "' (instead of '" + id + "')";
                                    log.error(message);
                                    disconnectedChildren.add(childNodeEntry);
                                }
                            } else {
                                return;
                            }

                        }
                    }
                }
                if (message != null) {
                    addMessage(reports, id, message);
                }
            } catch (ItemStateException e) {
                // problem already logged (loadBundle called with
                // logDetailedErrors=true)
                addMessage(reports, id, e.getMessage());
            }
        }
        // remove child node entry (if fixing is enabled)
        if (fix && (!missingChildren.isEmpty() || !disconnectedChildren.isEmpty())) {
            for (NodePropBundle.ChildNodeEntry entry : missingChildren) {
                bundle.getChildNodeEntries().remove(entry);
            }
            for (NodePropBundle.ChildNodeEntry entry : disconnectedChildren) {
                bundle.getChildNodeEntries().remove(entry);
            }
            fixBundle(bundle);
        }

        // check parent reference
        NodeId parentId = nodeInfo.getParentId();
        try {
            // skip root nodes (that point to itself)
            if (parentId != null && !id.toString().endsWith("babecafebabe")) {
                NodePropBundle parentBundle = null;
                NodeInfo parentInfo = infos.get(parentId);

                // does the parent exist?
                if (parentInfo == null) {
                    // try to load the bundle
                    parentBundle = pm.loadBundle(parentId);
                    if (parentBundle == null) {
                        // indeed the parent doesn't exist
                        // double check whether we still exist and the parent is still the same\
                        if (bundle == null) {
                            bundle = pm.loadBundle(id);
                        }
                        if (bundle != null) {
                            if (parentId.equals(bundle.getParentId())) {
                                // indeed we have an orphaned node
                                String message = "NodeState '" + id + "' references inexistent parent id '" + parentId + "'";
                                log.error(message);
                                addMessage(reports, id, message);
                                if (fix && lostNFoundId != null) {
                                    // add a child to lost+found
                                    NodePropBundle lfBundle = pm.loadBundle(lostNFoundId);
                                    lfBundle.markOld();
                                    String nodeName = id + "-" + System.currentTimeMillis();
                                    lfBundle.addChildNodeEntry(NF.create("", nodeName), id);
                                    pm.storeBundle(lfBundle);
                                    pm.evictBundle(lostNFoundId);

                                    // set lost+found parent
                                    bundle.setParentId(lostNFoundId);
                                    fixBundle(bundle);
                                }
                            }
                        } else {
                            return;
                        }
                    } else {
                        // parent exists after all
                        parentInfo = new NodeInfo(parentBundle);
                    }
                }
                if (parentInfo != null) {
                    // if the parent exists, does it have a child node entry for us?
                    boolean found = false;

                    for (NodeId childNodeId : parentInfo.getChildren()) {
                        if (childNodeId.equals(id)){
                            found = true;
                            break;
                        }
                    }

                    if (!found && parentBundle == null) {
                        // double check the parent
                        parentBundle = pm.loadBundle(parentId);
                        if (parentBundle != null) {
                            for (NodePropBundle.ChildNodeEntry entry : parentBundle.getChildNodeEntries()) {
                                if (entry.getId().equals(id)) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!found) {
                        // double check whether we still exist and the parent id is still the same
                        if (bundle == null) {
                            bundle = pm.loadBundle(id);
                        }
                        if (bundle != null) {
                            if (parentId.equals(bundle.getParentId())) {
                                // indeed we have an abandoned node
                                String message = "NodeState '" + id
                                        + "' is not referenced by its parent node '"
                                        + parentId + "'";
                                log.error(message);
                                addMessage(reports, id, message);
                                if (fix) {
                                    int l = (int) System.currentTimeMillis();
                                    int r = new Random().nextInt();
                                    int n = l + r;
                                    String nodeName = Integer.toHexString(n);
                                    parentBundle.addChildNodeEntry(NF.create("{}" + nodeName), id);
                                    log.info("NodeState '" + id
                                            + "' adds itself to its parent node '"
                                            + parentId + "' with a new name '" + nodeName
                                            + "'");
                                    fixBundle(parentBundle);
                                }
                            }
                        } else {
                            return;
                        }
                    }

                }
            }
        } catch (ItemStateException e) {
            String message = "Error reading node '" + parentId
                    + "' (parent of '" + id + "'): " + e;
            log.error(message);
            addMessage(reports, id, message);
        }
    }

    /**
     * @return whether the id is for a virtual node (not needing checking)
     */
    private boolean isVirtualNode(NodeId id) {
        String s = id.toString();
        if ("cafebabe-cafe-babe-cafe-babecafebabe".equals(s)) {
            // root node isn't virtual
            return false;
        }
        else {
            // all other system nodes are
            return s.endsWith("babecafebabe");
        }
    }


    private void addMessage(Set<ReportItem> reports, NodeId id, String message) {

        if (reports != null || listener != null) {
            ReportItem ri = new ReportItemImpl(id.toString(), message);

            if (reports != null) {
                reports.add(ri);
            }
            if (listener != null) {
                listener.report(ri);
            }
        }
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

    private void fixBundle(NodePropBundle bundle) {
        try {
            log.info(pm + ": Fixing bundle '" + bundle.getId() + "'");
            bundle.markOld(); // use UPDATE instead of INSERT
            pm.storeBundle(bundle);
            pm.evictBundle(bundle.getId());
        } catch (ItemStateException e) {
            log.error(pm + ": Error storing fixed bundle: " + e);
        }
    }
}
