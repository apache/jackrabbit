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
import org.apache.jackrabbit.core.persistence.check.ConsistencyChecker;
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

public class ConsistencyCheckerImpl implements ConsistencyChecker {

    /** the default logger */
    private static Logger log = LoggerFactory.getLogger(ConsistencyCheckerImpl.class);

    private AbstractBundlePersistenceManager pm;
    
    private static final NameFactory NF = NameFactoryImpl.getInstance();
    
    public ConsistencyCheckerImpl(AbstractBundlePersistenceManager pm) {
        this.pm = pm;
    }

    public ConsistencyReport check(String[] uuids, boolean recursive, boolean fix, String lostNFoundId)
            throws RepositoryException {
        Set<ReportItem> reports = new HashSet<ReportItem>();

        long tstart = System.currentTimeMillis();
        int total = internalCheckConsistency(uuids, recursive, fix, reports, lostNFoundId);
        long elapsed = System.currentTimeMillis() - tstart;

        return new ConsistencyReportImpl(total, elapsed, reports);
    }
    
    private int internalCheckConsistency(String[] uuids, boolean recursive, boolean fix, Set<ReportItem> reports,
            String lostNFoundId) throws RepositoryException {
        int count = 0;
        int total = 0;
        Collection<NodePropBundle> modifications = new ArrayList<NodePropBundle>();
        Set<NodeId> orphaned = new HashSet<NodeId>();

        NodeId lostNFound = null;
        if (fix && lostNFoundId != null) {
            // do we have a "lost+found" node?
            try {
                NodeId tmpid = new NodeId(lostNFoundId);
                NodePropBundle lfBundle = pm.loadBundle(tmpid);
                if (lfBundle == null) {
                    log.error("specified 'lost+found' node does not exist");
                } else if (!NameConstants.NT_UNSTRUCTURED.equals(lfBundle.getNodeTypeName())) {
                    log.error("specified 'lost+found' node is not of type nt:unstructered");
                } else {
                    lostNFound = lfBundle.getId();
                }
            } catch (Exception ex) {
                log.error("finding 'lost+found' folder", ex);
            }
        }

        if (uuids == null) {
            try {
                List<NodeId> allIds = pm.getAllNodeIds(null, 0);
                total = allIds.size();
                
                for (NodeId id : allIds) {
                    try {
                        // parse and check bundle
                        NodePropBundle bundle = pm.loadBundle(id);
                        if (bundle == null) {
                            log.error("No bundle found for id '" + id + "'");
                        } else {
                            checkBundleConsistency(id, bundle, fix, modifications, lostNFound, orphaned, reports);

                            count++;
                            if (count % 1000 == 0) {
                                log.info(pm + ": checked " + count + "/" + (total == -1 ? "?" : total) + " bundles...");
                            }
                        }
                    } catch (ItemStateException e) {
                        // problem already logged (loadBundle called with
                        // logDetailedErrors=true)
                    }

                }
            } catch (ItemStateException ex) {
                throw new RepositoryException("getting nodeIds", ex);
            } finally {
                total = count;
            }
        } else {
            // check only given uuids, handle recursive flag

            // 1) convert uuid array to modifiable list
            // 2) for each uuid do
            //     a) load node bundle
            //     b) check bundle, store any bundle-to-be-modified in collection
            //     c) if recursive, add child uuids to list of uuids

            List<NodeId> idList = new ArrayList<NodeId>(uuids.length);
            // convert uuid string array to list of UUID objects
            for (int i = 0; i < uuids.length; i++) {
                try {
                    idList.add(new NodeId(uuids[i]));
                } catch (IllegalArgumentException e) {
                    log.error("Invalid id for consistency check, skipping: '" + uuids[i] + "': " + e);
                }
            }
            
            // iterate over UUIDs (including ones that are newly added inside the loop!)
            for (int i = 0; i < idList.size(); i++) {
                NodeId id = idList.get(i);
                try {
                    // load the node from the database
                    NodePropBundle bundle = pm.loadBundle(id);

                    if (bundle == null) {
                        log.error("No bundle found for id '" + id + "'");
                    }
                    else {
                        checkBundleConsistency(id, bundle, fix, modifications, lostNFound, orphaned, reports);

                        if (recursive) {
                            for (NodePropBundle.ChildNodeEntry entry : bundle.getChildNodeEntries()) {
                                idList.add(entry.getId());
                            }
                        }

                        count++;
                        if (count % 1000 == 0) {
                            log.info(pm + ": checked " + count + "/" + idList.size() + " bundles...");
                        }
                    }
                } catch (ItemStateException e) {
                    // problem already logged (loadBundle called with logDetailedErrors=true)
                }
            }

            total = idList.size();
        }

        // repair collected broken bundles
        if (fix && !modifications.isEmpty()) {
            log.info(pm + ": Fixing " + modifications.size() + " inconsistent bundle(s)...");
            for (NodePropBundle bundle : modifications) {
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

        if (fix && lostNFoundId != null && !orphaned.isEmpty()) {
            // do we have things to add to "lost+found"?
            try {
                NodePropBundle lfBundle = pm.loadBundle(lostNFound);
                if (lfBundle == null) {
                    log.error("specified 'lost+found' node does not exist");
                } else if (!NameConstants.NT_UNSTRUCTURED.equals(lfBundle.getNodeTypeName())) {
                    log.error("specified 'lost+found' node is not of type nt:unstructered");
                } else {
                    lfBundle.markOld();
                    for (NodeId orphan : orphaned) {
                        String nodeName = orphan + "-" + System.currentTimeMillis();
                        lfBundle.addChildNodeEntry(NF.create("", nodeName), orphan);
                    }
                    pm.storeBundle(lfBundle);
                    pm.evictBundle(lfBundle.getId());
                }
            } catch (Exception ex) {
                log.error("trying orphan adoption", ex);
            }
        }

        log.info(pm + ": checked " + count + "/" + total + " bundles.");

        return total;
    }

    /**
     * Checks a single bundle for inconsistencies, ie. inexistent child nodes
     * and inexistent parents.
     *
     * @param id node id for the bundle to check
     * @param bundle the bundle to check
     * @param fix if <code>true</code>, repair things that can be repaired
     * @param modifications if <code>fix == true</code>, collect the repaired
     * {@linkplain NodePropBundle bundles} here
     */
    private void checkBundleConsistency(NodeId id, NodePropBundle bundle,
                                          boolean fix, Collection<NodePropBundle> modifications,
                                          NodeId lostNFoundId, Set<NodeId> orphaned, Set<ReportItem> reports) {
        //log.info(name + ": checking bundle '" + id + "'");

        // skip all system nodes except root node
        if (id.toString().endsWith("babecafebabe")
                && !id.toString().equals("cafebabe-cafe-babe-cafe-babecafebabe")) {
            return;
        }

        // look at the node's children
        Collection<NodePropBundle.ChildNodeEntry> missingChildren = new ArrayList<NodePropBundle.ChildNodeEntry>();
        for (NodePropBundle.ChildNodeEntry entry : bundle.getChildNodeEntries()) {

            // skip check for system nodes (root, system root, version storage, node types)
            if (entry.getId().toString().endsWith("babecafebabe")) {
                continue;
            }

            try {
                // analyze child node bundles
                NodePropBundle child = pm.loadBundle(entry.getId());
                String message = null;
                if (child == null) {
                    message = "NodeState '" + id + "' references inexistent child" + " '"
                            + entry.getName() + "' with id " + "'" + entry.getId() + "'";
                    log.error(message);
                    missingChildren.add(entry);
                } else {
                    NodeId cp = child.getParentId();
                    if (cp == null) {
                        message = "ChildNode has invalid parent id: <null>";
                        log.error(message);
                    } else if (!cp.equals(id)) {
                        message = "ChildNode has invalid parent id: '" + cp + "' (instead of '" + id + "')";
                        log.error(message);
                    }
                }
                if (message != null) {
                    addMessage(reports, id, message);
                }
            } catch (ItemStateException e) {
                // problem already logged (loadBundle called with logDetailedErrors=true)
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
                    String message = "NodeState '" + id + "' references inexistent parent id '" + parentId + "'";
                    log.error(message);
                    addMessage(reports, id, message);
                    orphaned.add(id);
                    if (lostNFoundId != null) {
                        bundle.setParentId(lostNFoundId);
                        modifications.add(bundle);
                    }
                }
                else {
                    boolean found = false;

                    for (NodePropBundle.ChildNodeEntry entry : parentBundle.getChildNodeEntries()) {
                        if (entry.getId().equals(id)){
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        String message = "NodeState '" + id + "' is not referenced by its parent node '" + parentId + "'";
                        log.error(message);
                        addMessage(reports, id, message);

                        int l = (int) System.currentTimeMillis();
                        int r = new Random().nextInt();
                        int n = l + r;
                        String nodeName = Integer.toHexString(n);
                        parentBundle.addChildNodeEntry(NF.create("{}" + nodeName), id);
                        log.info("NodeState '" + id + "' adds itself to its parent node '" + parentId + "' with a new name '" + nodeName + "'");
                        modifications.add(parentBundle);
                    }
                }
            }
        } catch (ItemStateException e) {
            String message = "Error reading node '" + parentId + "' (parent of '" + id + "'): " + e;
            log.error(message);
            addMessage(reports, id, message);
        }
    }

    private void addMessage(Set<ReportItem> reports, NodeId id, String message) {
        if (reports != null) {
            reports.add(new ReportItemImpl(id.toString(), message));
        }
    }
}
