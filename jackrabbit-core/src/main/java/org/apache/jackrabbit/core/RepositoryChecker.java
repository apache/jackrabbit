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

import java.util.HashSet;
import java.util.Set;

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
import org.apache.jackrabbit.core.version.InternalVersionManager;
import org.apache.jackrabbit.spi.Name;
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

    private final InternalVersionManager versionManager;

    public RepositoryChecker(
            PersistenceManager workspace,
            InternalVersionManager versionManager) {
        this.workspace = workspace;
        this.workspaceChanges = new ChangeLog();
        this.versionManager = versionManager;
    }

    public void check(NodeId id, boolean recurse)
            throws RepositoryException {
        try {
            log.debug("Checking consistency of node {}", id);
            NodeState state = workspace.load(id);
            checkVersionHistory(state);

            if (recurse) {
                for (ChildNodeEntry child : state.getChildNodeEntries()) {
                    if (!SYSTEM_ROOT_NODE_ID.equals(child.getId())) {
                        check(child.getId(), recurse);
                    }
                }
            }
        } catch (ItemStateException e) {
            throw new RepositoryException("Unable to access node " + id, e);
        }
    }

    public void fix() throws RepositoryException {
        if (workspaceChanges.hasUpdates()) {
            log.warn("Fixing repository inconsistencies");
            try {
                workspace.store(workspaceChanges);
            } catch (ItemStateException e) {
                e.printStackTrace();
                throw new RepositoryException(
                        "Failed to fix workspace inconsistencies", e);
            }
        } else {
            log.info("No repository inconsistencies found");
        }
    }

    private void checkVersionHistory(NodeState node) {
        if (node.hasPropertyName(JCR_VERSIONHISTORY)) {
            String message = null;
            NodeId nid = node.getNodeId();

            try {
                log.debug("Checking version history of node {}", nid);

                message = "Removing references to a missing version history of node " + nid;
                InternalVersionHistory vh = versionManager.getVersionHistoryOfNode(nid);

                // additional checks, see JCR-3101
                String intro = "Removing references to an inconsistent version history of node "
                    + nid;

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
                        throw new InconsistentVersioningState("frozen node of "
                                + v.getId() + " is missing.");
                    }
                }

                if (!seenRoot) {
                    message = intro + " (root version is missing)";
                    throw new InconsistentVersioningState("root version of " + nid +" is missing.");
                }
            } catch (Exception e) {
                log.info(message, e);
                removeVersionHistoryReferences(node);
            }
        }
    }

    private void removeVersionHistoryReferences(NodeState node) {
        NodeState modified =
            new NodeState(node, NodeState.STATUS_EXISTING_MODIFIED, true);

        Set<Name> mixins = new HashSet<Name>(node.getMixinTypeNames());
        if (mixins.remove(MIX_VERSIONABLE)) {
            modified.setMixinTypeNames(mixins);
        }

        removeProperty(modified, JCR_VERSIONHISTORY);
        removeProperty(modified, JCR_BASEVERSION);
        removeProperty(modified, JCR_PREDECESSORS);
        removeProperty(modified, JCR_ISCHECKEDOUT);

        workspaceChanges.modified(modified);
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
