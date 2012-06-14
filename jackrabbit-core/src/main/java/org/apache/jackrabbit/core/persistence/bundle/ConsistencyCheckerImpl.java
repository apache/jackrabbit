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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.persistence.check.ConsistencyCheckListener;
import org.apache.jackrabbit.core.persistence.check.ConsistencyReport;
import org.apache.jackrabbit.core.persistence.check.ConsistencyReportImpl;
import org.apache.jackrabbit.core.persistence.check.ReportItem;
import org.apache.jackrabbit.core.persistence.check.ReportItemImpl;
import org.apache.jackrabbit.core.persistence.util.NodePropBundle;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsistencyCheckerImpl {

    /** the default logger */
    private static Logger log = LoggerFactory
            .getLogger(ConsistencyCheckerImpl.class);

    private final AbstractBundlePersistenceManager pm;

    private final ConsistencyCheckListener listener;

    private static final NameFactory NF = NameFactoryImpl.getInstance();

    // process 64K nodes at once
    private static int NODESATONCE = 1024 * 64;

    public ConsistencyCheckerImpl(AbstractBundlePersistenceManager pm,
            ConsistencyCheckListener listener) {
        this.pm = pm;
        this.listener = listener;
    }

    public ConsistencyReport check(String[] uuids, boolean recursive,
            boolean fix, String lostNFoundId) throws RepositoryException {
        Set<ReportItem> reports = new HashSet<ReportItem>();

        long tstart = System.currentTimeMillis();
        int total = internalCheckConsistency(uuids, recursive, fix, reports,
                lostNFoundId);
        long elapsed = System.currentTimeMillis() - tstart;

        return new ConsistencyReportImpl(total, elapsed, reports);
    }

    private int internalCheckConsistency(String[] uuids, boolean recursive,
            boolean fix, Set<ReportItem> reports, String lostNFoundId)
            throws RepositoryException {
        int count = 0;
        Collection<NodePropBundle> modifications = new ArrayList<NodePropBundle>();
        Set<NodeId> orphaned = new HashSet<NodeId>();

        NodeId lostNFound = null;
        if (fix && lostNFoundId != null) {
            // do we have a "lost+found" node?
            try {
                NodeId tmpid = new NodeId(lostNFoundId);
                NodePropBundle lfBundle = pm.loadBundle(tmpid);
                if (lfBundle == null) {
                    error(lostNFoundId,
                            "specified 'lost+found' node does not exist");
                } else if (!NameConstants.NT_UNSTRUCTURED.equals(lfBundle
                        .getNodeTypeName())) {
                    error(lostNFoundId,
                            "specified 'lost+found' node is not of type nt:unstructured");
                } else {
                    lostNFound = lfBundle.getId();
                }
            } catch (Exception ex) {
                error(lostNFoundId, "finding 'lost+found' folder", ex);
            }
        }

        if (uuids == null) {
            try {
                List<NodeId> allIds = pm.getAllNodeIds(null, NODESATONCE);

                while (!allIds.isEmpty()) {
                    NodeId lastId = null;

                    for (NodeId id : allIds) {
                        lastId = id;
                        try {
                            // parse and check bundle
                            NodePropBundle bundle = pm.loadBundle(id);
                            if (bundle == null) {
                                error(id.toString(), "No bundle found for id '"
                                        + id + "'");
                            } else {
                                checkBundleConsistency(id, bundle, fix,
                                        modifications, lostNFound, orphaned,
                                        reports);

                                count++;
                                if (count % 1000 == 0 && listener == null) {
                                    log.info(pm + ": checked " + count
                                            + " bundles...");
                                }
                            }
                        } catch (ItemStateException e) {
                            // problem already logged (loadBundle called with
                            // logDetailedErrors=true)
                        }
                    }

                    if (!allIds.isEmpty()) {
                        allIds = pm.getAllNodeIds(lastId, NODESATONCE);
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
                        checkBundleConsistency(id, bundle, fix, modifications,
                                lostNFound, orphaned, reports);

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

        // repair collected broken bundles
        if (fix && !modifications.isEmpty()) {
            info(null, pm + ": Fixing " + modifications.size()
                    + " inconsistent bundle(s)...");
            for (NodePropBundle bundle : modifications) {
                try {
                    info(bundle.getId().toString(), pm + ": Fixing bundle '"
                            + bundle.getId() + "'");
                    bundle.markOld(); // use UPDATE instead of INSERT
                    pm.storeBundle(bundle);
                    pm.evictBundle(bundle.getId());
                } catch (ItemStateException e) {
                    error(bundle.getId().toString(), pm
                            + ": Error storing fixed bundle: " + e);
                }
            }
        }

        if (fix && lostNFoundId != null && !orphaned.isEmpty()) {
            // do we have things to add to "lost+found"?
            try {
                NodePropBundle lfBundle = pm.loadBundle(lostNFound);
                if (lfBundle == null) {
                    error(lostNFoundId, "specified 'lost+found' node does not exist");
                } else if (!NameConstants.NT_UNSTRUCTURED.equals(lfBundle
                        .getNodeTypeName())) {
                    error(lostNFoundId, "specified 'lost+found' node is not of type nt:unstructered");
                } else {
                    lfBundle.markOld();
                    for (NodeId orphan : orphaned) {
                        String nodeName = orphan + "-"
                                + System.currentTimeMillis();
                        lfBundle.addChildNodeEntry(NF.create("", nodeName),
                                orphan);
                    }
                    pm.storeBundle(lfBundle);
                    pm.evictBundle(lfBundle.getId());
                }
            } catch (Exception ex) {
                error(null, "trying orphan adoption", ex);
            }
        }

        log.info(pm + ": checked " + count + " bundles.");

        return count;
    }

    /**
     * Checks a single bundle for inconsistencies, ie. inexistent child nodes
     * and inexistent parents.
     * 
     * @param id
     *            node id for the bundle to check
     * @param bundle
     *            the bundle to check
     * @param fix
     *            if <code>true</code>, repair things that can be repaired
     * @param modifications
     *            if <code>fix == true</code>, collect the repaired
     *            {@linkplain NodePropBundle bundles} here
     */
    private void checkBundleConsistency(NodeId id, NodePropBundle bundle,
            boolean fix, Collection<NodePropBundle> modifications,
            NodeId lostNFoundId, Set<NodeId> orphaned, Set<ReportItem> reports) {
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
        for (NodePropBundle.ChildNodeEntry entry : bundle.getChildNodeEntries()) {

            final NodeId childNodeId = entry.getId();

            // skip check for system nodes (root, system root, version storage,
            // node types)
            if (childNodeId.toString().endsWith("babecafebabe")) {
                continue;
            }

            try {
                // analyze child node bundles
                final NodePropBundle childBundle = pm.loadBundle(childNodeId);
                String message = null;
                if (childBundle == null) {
                    // double check whether we still exist and the child entry is still there
                    bundle = pm.loadBundle(id);

                    if (bundle != null) {
                        boolean stillThere = false;
                        for (NodePropBundle.ChildNodeEntry entryRetry : bundle.getChildNodeEntries()) {
                            if (entryRetry.getId().equals(childNodeId)) {
                                stillThere = true;
                                break;
                            }
                        }
                        if (stillThere) {
                            message = "NodeState '" + id
                                    + "' references inexistent child" + " '"
                                    + entry.getName() + "' with id " + "'"
                                    + childNodeId + "'";
                            log.error(message);
                            missingChildren.add(entry);
                        }
                    }
                } else {
                    NodeId cp = childBundle.getParentId();
                    if (cp == null || !cp.equals(id)) {
                        // double check whether the child entry is still there
                        bundle = pm.loadBundle(id);
                        if (bundle != null) {
                            boolean stillThere = false;
                            for (NodePropBundle.ChildNodeEntry entryRetry : bundle.getChildNodeEntries()) {
                                if (entryRetry.getId().equals(childNodeId)) {
                                    stillThere = true;
                                    break;
                                }
                            }
                            if (stillThere) {
                                if (cp == null) {
                                    message = "ChildNode has invalid parent id: <null>";
                                    log.error(message);
                                } else if (!cp.equals(id)) {
                                    message = "ChildNode has invalid parent id: '" + cp
                                            + "' (instead of '" + id + "')";
                                    log.error(message);
                                }
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
        if (fix && !missingChildren.isEmpty()) {
            for (NodePropBundle.ChildNodeEntry entry : missingChildren) {
                bundle.getChildNodeEntries().remove(entry);
            }
            modifications.add(bundle);
        }

        // check parent reference
        NodeId parentId = bundle.getParentId();
        try {
            // skip root nodes (that point to itself)
            if (parentId != null && !id.toString().endsWith("babecafebabe")) {
                NodePropBundle parentBundle = pm.loadBundle(parentId);

                if (parentBundle == null) {
                    // double check whether we still exist and the parent is still the same
                    bundle = pm.loadBundle(id);
                    if (bundle != null) {
                        if (parentId.equals(bundle.getParentId())) {
                            String message = "NodeState '" + id
                                    + "' references inexistent parent id '" + parentId
                                    + "'";
                            log.error(message);
                            addMessage(reports, id, message);
                            orphaned.add(id);
                            if (lostNFoundId != null) {
                                bundle.setParentId(lostNFoundId);
                                modifications.add(bundle);
                            }
                        }
                    }
                } else {
                    boolean found = false;

                    for (NodePropBundle.ChildNodeEntry entry : parentBundle.getChildNodeEntries()) {
                        if (entry.getId().equals(id)) {
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        // double check whether we still exist and the parent is still the same
                        bundle = pm.loadBundle(id);
                        if (bundle != null) {
                            if (parentId.equals(bundle.getParentId())) {
                                String message = "NodeState '" + id
                                        + "' is not referenced by its parent node '"
                                        + parentId + "'";
                                log.error(message);
                                addMessage(reports, id, message);

                                int l = (int) System.currentTimeMillis();
                                int r = new Random().nextInt();
                                int n = l + r;
                                String nodeName = Integer.toHexString(n);
                                parentBundle.addChildNodeEntry(
                                        NF.create("{}" + nodeName), id);
                                log.info("NodeState '" + id
                                        + "' adds itself to its parent node '"
                                        + parentId + "' with a new name '" + nodeName
                                        + "'");
                                modifications.add(parentBundle);
                            }
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
}
