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
package org.apache.jackrabbit.jcr2spi;

import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.version.VersionManager;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>VersionManagerImpl</code>...
 */
public class JcrVersionManager implements javax.jcr.version.VersionManager {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(JcrVersionManager.class);

    private final VersionManager vMgr;
    private final SessionImpl session;
    private final ItemManager itemManager;
    private final PathResolver resolver;

    /**
     * The ID of the activity currently in effect for the session this
     * manager has been created for.
     */
    private NodeId activityId;

    protected JcrVersionManager(SessionImpl session) {
        this.session = session;
        vMgr = session.getVersionStateManager();
        itemManager = session.getItemManager();
        resolver = session.getPathResolver();
    }

    //-----------------------------------------------------< VersionManager >---
    /**
     * @see javax.jcr.version.VersionManager#checkin(String)
     */
    public Version checkin(String absPath) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        session.checkIsAlive();
        
        Node n = itemManager.getNode(resolver.getQPath(absPath));
        return n.checkin();
    }

    /**
     * @see javax.jcr.version.VersionManager#checkout(String)
     */
    public void checkout(String absPath) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        session.checkIsAlive();

        Node n = itemManager.getNode(resolver.getQPath(absPath));
        n.checkout();
    }

    /**
     * @see javax.jcr.version.VersionManager#checkpoint(String)
     */
    public Version checkpoint(String absPath) throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        session.checkIsAlive();

        NodeImpl n = (NodeImpl) itemManager.getNode(resolver.getQPath(absPath));
        return n.checkpoint();
    }

    /**
     * @see javax.jcr.version.VersionManager#isCheckedOut(String)
     */
    public boolean isCheckedOut(String absPath) throws RepositoryException {
        session.checkIsAlive();

        Node n = itemManager.getNode(resolver.getQPath(absPath));
        return n.isCheckedOut();
    }

    /**
     * @see javax.jcr.version.VersionManager#getVersionHistory(String)
     */
    public VersionHistory getVersionHistory(String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkIsAlive();

        Node n = itemManager.getNode(resolver.getQPath(absPath));
        return n.getVersionHistory();
    }

    /**
     * @see javax.jcr.version.VersionManager#getBaseVersion(String)
     */
    public Version getBaseVersion(String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkIsAlive();

        Node n = itemManager.getNode(resolver.getQPath(absPath));
        return n.getBaseVersion();
    }

    /**
     * @see javax.jcr.version.VersionManager#restore(Version[], boolean)
     */
    public void restore(Version[] versions, boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        session.checkIsAlive();
        session.checkHasPendingChanges();

        NodeState[] versionStates = new NodeState[versions.length];
        for (int i = 0; i < versions.length; i++) {
            versionStates[i] = session.getVersionState(versions[i]);
        }
        vMgr.restore(versionStates, removeExisting);
    }

    /**
     * @see javax.jcr.version.VersionManager#restore(String, String, boolean)
     */
    public void restore(String absPath, String versionName, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        session.checkIsAlive();

        Node n = itemManager.getNode(resolver.getQPath(absPath));
        n.restore(versionName, removeExisting);
    }

    /**
     * @see javax.jcr.version.VersionManager#restore(Version, boolean)
     */
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        restore(new Version[]{version}, removeExisting);
    }

    /**
     * @see javax.jcr.version.VersionManager#restore(String, Version, boolean)
     */
    public void restore(String absPath, Version version, boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        session.checkIsAlive();
        // get parent
        int idx = absPath.lastIndexOf('/');
        String parent = idx == 0 ? "/" : absPath.substring(0, idx);
        String name = absPath.substring(idx + 1);
        Node n = itemManager.getNode(resolver.getQPath(parent));
        n.restore(version, name, removeExisting);
    }

    /**
     * @see javax.jcr.version.VersionManager#restoreByLabel(String, String, boolean)
     */
    public void restoreByLabel(String absPath, String versionLabel, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        session.checkIsAlive();

        Node n = itemManager.getNode(resolver.getQPath(absPath));
        n.restoreByLabel(versionLabel, removeExisting);
    }

    /**
     * @see javax.jcr.version.VersionManager#merge(String, String, boolean)
     */
    public NodeIterator merge(String absPath, String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        return merge(absPath, srcWorkspace, bestEffort, false);
    }

    /**
     * @see javax.jcr.version.VersionManager#merge(String, String, boolean, boolean)
     */
    public NodeIterator merge(String absPath, String srcWorkspace, boolean bestEffort, boolean isShallow) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        session.checkIsAlive();        

        NodeImpl n = (NodeImpl) itemManager.getNode(resolver.getQPath(absPath));
        n.checkIsWritable();
        session.checkHasPendingChanges();

        // if same workspace, ignore
        if (session.getWorkspace().getName().equals(srcWorkspace)) {
            return NodeIteratorAdapter.EMPTY;
        }
        // make sure the workspace exists and is accessible for this session.
        session.checkAccessibleWorkspace(srcWorkspace);
        
        Iterator<NodeId> failedIds = session.getVersionStateManager().merge((NodeState) n.getItemState(), srcWorkspace, bestEffort, isShallow);
        return new LazyItemIterator(itemManager, session.getHierarchyManager(), failedIds);
    }

    /**
     * @see javax.jcr.version.VersionManager#doneMerge(String, Version)
     */
    public void doneMerge(String absPath, Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        session.checkIsAlive();

        Node n = itemManager.getNode(resolver.getQPath(absPath));
        n.doneMerge(version);
    }

    /**
     * @see javax.jcr.version.VersionManager#cancelMerge(String, Version)
     */
    public void cancelMerge(String absPath, Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        session.checkIsAlive();                                  

        Node n = itemManager.getNode(resolver.getQPath(absPath));
        n.cancelMerge(version);
    }

    /**
     * @see javax.jcr.version.VersionManager#createConfiguration(String)
     */
    public Node createConfiguration(String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkIsAlive();

        NodeImpl n = (NodeImpl) itemManager.getNode(resolver.getQPath(absPath));
        NodeEntry entry = vMgr.createConfiguration((NodeState) n.getItemState());
        return (Node) itemManager.getItem(entry);
    }

    /**
     * @see javax.jcr.version.VersionManager#setActivity(Node)
     */
    public Node setActivity(Node activity) throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkIsAlive();
        session.checkSupportedOption(Repository.OPTION_ACTIVITIES_SUPPORTED);


        Node oldActivity = getActivity();
        if (activity == null) {
            activityId = null;
        } else {
            NodeImpl activityNode = getValidActivity(activity, "set");
            activityId = (NodeId) activityNode.getItemState().getId();
        }
        return oldActivity;
    }

    /**
     * @see javax.jcr.version.VersionManager#getActivity()
     */
    public Node getActivity() throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkIsAlive();
        session.checkSupportedOption(Repository.OPTION_ACTIVITIES_SUPPORTED);

        if (activityId == null) {
            return null;
        } else {
            try {
                return (Node) itemManager.getItem(session.getHierarchyManager().getNodeEntry(activityId));
            } catch (ItemNotFoundException e) {
                // the activity doesn't exist any more.
                log.warn("Activity node with id " + activityId + " doesn't exist any more.");
                activityId = null;
                return null;
            }
        }
    }

    /**
     * @see javax.jcr.version.VersionManager#createActivity(String)
     */
    public Node createActivity(String title) throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkIsAlive();

        NodeEntry entry = vMgr.createActivity(title);
        return (Node) itemManager.getItem(entry);
    }

    /**
     * @see javax.jcr.version.VersionManager#removeActivity(Node)
     */
    public void removeActivity(Node activityNode) throws UnsupportedRepositoryOperationException, RepositoryException {
        session.checkIsAlive();
        NodeImpl activity = getValidActivity(activityNode, "remove");

        NodeState nState = (NodeState) activity.getItemState();
        ItemId removeId = nState.getId();
        vMgr.removeActivity(nState);

        // if the removal succeeded, make sure there is no current activity
        // setting on this session, that points to the removed activity.
        if (activityId != null && activityId.equals(removeId)) {
            activityId = null;
        }
    }

    /**
     * @see javax.jcr.version.VersionManager#merge(Node)
     */
    public NodeIterator merge(Node activityNode) throws VersionException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        session.checkIsAlive();
        NodeImpl activity = getValidActivity(activityNode, "merge");
        Iterator<NodeId> failedIds = vMgr.mergeActivity((NodeState) activity.getItemState());
        return new LazyItemIterator(itemManager, session.getHierarchyManager(), failedIds);
    }

    /**
     * Assert that activity nodes passes to any of the activity methods have
     * been obtained from the session this version manager has been created for.
     * This is particularly important for workspace operations that are followed
     * by internal updated of modified items: The hierarchy entries invalidated
     * after successful completion of the operation must reside within scope
     * defined by this session.
     * <br>
     * In addition this method verifies that the passed node is of type nt:activity.
     *
     * @param activityNode
     * @param methodName
     * @return
     * @throws RepositoryException
     */
    private NodeImpl getValidActivity(Node activityNode, String methodName) throws UnsupportedRepositoryOperationException, RepositoryException {
        NodeImpl activity;
        if (session != activityNode.getSession()) {
            String msg = "Attempt to " +methodName+ " an activity node that has been retrieved by another session.";
            log.warn(msg);
            activity = (NodeImpl) session.getNodeByIdentifier(activityNode.getIdentifier());
        } else {
            activity = (NodeImpl) activityNode;
        }
        if (!activity.isNodeType(NameConstants.NT_ACTIVITY)) {
            throw new UnsupportedRepositoryOperationException("Given node is not an activity.");
        }
        return activity;
    }
}