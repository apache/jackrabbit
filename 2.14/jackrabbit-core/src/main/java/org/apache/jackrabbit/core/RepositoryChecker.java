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
package org.apache.jackrabbit.core;

import static org.apache.jackrabbit.core.RepositoryImpl.SYSTEM_ROOT_NODE_ID;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_BASEVERSION;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_ISCHECKEDOUT;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_PREDECESSORS;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_ROOTVERSION;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.JCR_VERSIONHISTORY;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_VERSIONABLE;
import static org.apache.jackrabbit.spi.commons.name.NameConstants.MIX_REFERENCEABLE;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.state.ChangeLog;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.version.InconsistentVersioningState;
import org.apache.jackrabbit.core.version.InternalVersion;
import org.apache.jackrabbit.core.version.InternalVersionHistory;
import org.apache.jackrabbit.core.version.InternalVersionManagerImpl;
import org.apache.jackrabbit.core.version.VersionHistoryInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool for checking for and optionally fixing consistency issues in a
 * repository. Currently this class only contains a simple versioning
 * recovery feature for
 * <a href="https://issues.apache.org/jira/browse/JCR-2551">JCR-2551</a>.
 */
class RepositoryChecker {

    /**
     * Logger instance.
     */
    private static final Logger log =
        LoggerFactory.getLogger(RepositoryChecker.class);

    private final PersistenceManager workspace;

    private final ChangeLog workspaceChanges;

    private final ChangeLog vworkspaceChanges;

    private final InternalVersionManagerImpl versionManager;

    // maximum size of changelog when running in "fixImmediately" mode
    private final static long CHUNKSIZE = 256;

    // number of nodes affected by pending changes
    private long dirtyNodes = 0;

    // total nodes checked, with problems
    private long totalNodes = 0;
    private long brokenNodes = 0;

    // start time
    private long startTime;

    public RepositoryChecker(PersistenceManager workspace,
            InternalVersionManagerImpl versionManager) {
        this.workspace = workspace;
        this.workspaceChanges = new ChangeLog();
        this.vworkspaceChanges = new ChangeLog();
        this.versionManager = versionManager;
    }

    public void check(NodeId id, boolean recurse, boolean fixImmediately)
            throws RepositoryException {

        log.info("Starting RepositoryChecker");

        startTime = System.currentTimeMillis();

        internalCheck(id, recurse, fixImmediately);

        if (fixImmediately) {
            internalFix(true);
        }

        log.info("RepositoryChecker finished; checked " + totalNodes
                + " nodes in " + (System.currentTimeMillis() - startTime)
                + "ms, problems found: " + brokenNodes);
    }

    private void internalCheck(NodeId id, boolean recurse,
            boolean fixImmediately) throws RepositoryException {
        try {
            log.debug("Checking consistency of node {}", id);
            totalNodes += 1;

            NodeState state = workspace.load(id);
            checkVersionHistory(state);

            if (fixImmediately && dirtyNodes > CHUNKSIZE) {
                internalFix(false);
            }

            if (recurse) {
                for (ChildNodeEntry child : state.getChildNodeEntries()) {
                    if (!SYSTEM_ROOT_NODE_ID.equals(child.getId())) {
                        internalCheck(child.getId(), recurse, fixImmediately);
                    }
                }
            }
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to access node " + id, e);
        }
    }

    private void fix(PersistenceManager pm, ChangeLog changes, String store,
            boolean verbose) throws RepositoryException {
        if (changes.hasUpdates()) {
            if (log.isWarnEnabled()) {
                log.warn("Fixing " + store + " inconsistencies: "
                        + changes.toString());
            }
            try {
                pm.store(changes);
                changes.reset();
            } catch (ItemStateException e) {
                String message = "Failed to fix " + store
                        + " inconsistencies (aborting)";
                log.error(message, e);
                throw new RepositoryException(message, e);
            }
        } else {
            if (verbose) {
                log.info("No " + store + " inconsistencies found");
            }
        }
    }

    public void fix() throws RepositoryException {
        internalFix(true);
    }

    private void internalFix(boolean verbose) throws RepositoryException {
        fix(workspace, workspaceChanges, "workspace", verbose);
        fix(versionManager.getPersistenceManager(), vworkspaceChanges,
                "versioning workspace", verbose);
        dirtyNodes = 0;
    }

    private void checkVersionHistory(NodeState node) {

        String message = null;
        NodeId nid = node.getNodeId();
        boolean isVersioned = node.hasPropertyName(JCR_VERSIONHISTORY);

        NodeId vhid = null;

        try {
            String type = isVersioned ? "in-use" : "candidate";

            log.debug("Checking " + type + " version history of node {}", nid);

            String intro = "Removing references to an inconsistent " + type
                    + " version history of node " + nid;

            message = intro + " (getting the VersionInfo)";
            VersionHistoryInfo vhi = versionManager.getVersionHistoryInfoForNode(node);
            if (vhi != null) {
                // get the version history's node ID as early as possible
                // so we can attempt a fixup even when the next call fails
                vhid = vhi.getVersionHistoryId();
            }

            message = intro + " (getting the InternalVersionHistory)";

            InternalVersionHistory vh = null;

            try {
                vh = versionManager.getVersionHistoryOfNode(nid);
            }
            catch (ItemNotFoundException ex) {
                // it's ok if we get here if the node didn't claim to be versioned
                if (isVersioned) {
                    throw ex;
                }
            }

            if (vh == null) {
                if (isVersioned) {
                    message = intro + "getVersionHistoryOfNode returned null";
                    throw new InconsistentVersioningState(message);    
                }
            } else { 
                vhid = vh.getId();

                // additional checks, see JCR-3101

                message = intro + " (getting the version names failed)";
                Name[] versionNames = vh.getVersionNames();
                boolean seenRoot = false;

                for (Name versionName : versionNames) {
                    seenRoot |= JCR_ROOTVERSION.equals(versionName);

                    log.debug("Checking version history of node {}, version {}", nid, versionName);

                    message = intro + " (getting version " + versionName + "  failed)";
                    InternalVersion v = vh.getVersion(versionName);

                    message = intro + "(frozen node of root version " + v.getId() + " missing)";
                    if (null == v.getFrozenNode()) {
                        throw new InconsistentVersioningState(message);
                    }
                }

                if (!seenRoot) {
                    message = intro + " (root version is missing)";
                    throw new InconsistentVersioningState(message);
                }
            }
        } catch (InconsistentVersioningState e) {
            log.info(message, e);
            NodeId nvhid = e.getVersionHistoryNodeId();
            if (nvhid != null) {
                if (vhid != null && !nvhid.equals(vhid)) {
                    log.error("vhrid returned with InconsistentVersioningState does not match the id we already had: "
                            + vhid + " vs " + nvhid);
                }
                vhid = nvhid; 
            }
            removeVersionHistoryReferences(node, vhid);
        } catch (Exception e) {
            log.info(message, e);
            removeVersionHistoryReferences(node, vhid);
        }
    }

    // un-versions the node, and potentially moves the version history away
    private void removeVersionHistoryReferences(NodeState node,  NodeId vhid) {

        dirtyNodes += 1;
        brokenNodes += 1;

        NodeState modified =
            new NodeState(node, NodeState.STATUS_EXISTING_MODIFIED, true);

        Set<Name> mixins = new HashSet<Name>(node.getMixinTypeNames());
        if (mixins.remove(MIX_VERSIONABLE)) {
            // we are keeping jcr:uuid, so we need to make sure the type info stays valid
            mixins.add(MIX_REFERENCEABLE);
            modified.setMixinTypeNames(mixins);
        }

        removeProperty(modified, JCR_VERSIONHISTORY);
        removeProperty(modified, JCR_BASEVERSION);
        removeProperty(modified, JCR_PREDECESSORS);
        removeProperty(modified, JCR_ISCHECKEDOUT);

        workspaceChanges.modified(modified);

        if (vhid != null) {
            // attempt to rename the version history, so it doesn't interfere with
            // a future attempt to put the node under version control again 
            // (see JCR-3115)

            log.info("trying to rename version history of node " + node.getId());

            NameFactory nf = NameFactoryImpl.getInstance();

            // Name of VHR in parent folder is ID of versionable node
            Name vhrname = nf.create(Name.NS_DEFAULT_URI, node.getId().toString());

            try {
                NodeState vhrState = versionManager.getPersistenceManager().load(vhid);
                NodeState vhrParentState = versionManager.getPersistenceManager().load(vhrState.getParentId());

                if (vhrParentState.hasChildNodeEntry(vhrname)) {
                    NodeState modifiedParent = (NodeState) vworkspaceChanges.get(vhrState.getParentId());
                    if (modifiedParent == null) {
                        modifiedParent = new NodeState(vhrParentState, NodeState.STATUS_EXISTING_MODIFIED, true);
                    }

                    Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    String appendme = String.format(" (disconnected by RepositoryChecker on %04d%02d%02dT%02d%02d%02dZ)",
                            now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH),
                            now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND));
                    modifiedParent.renameChildNodeEntry(vhid,
                            nf.create(vhrname.getNamespaceURI(), vhrname.getLocalName() + appendme));

                    vworkspaceChanges.modified(modifiedParent);
                }
                else {
                    log.info("child node entry " + vhrname + " for version history not found inside parent folder.");
                }
            } catch (Exception ex) {
                log.error("while trying to rename the version history", ex);
            }
        }
    }

    private void removeProperty(NodeState node, Name name) {
        if (node.hasPropertyName(name)) {
            node.removePropertyName(name);
            try {
                workspaceChanges.deleted(workspace.load(
                        new PropertyId(node.getNodeId(), name)));
            } catch (ItemStateException ignoe) {
            }
        }
    }

}
