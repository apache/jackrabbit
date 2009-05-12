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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.version.VersionManager;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;

import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.AccessDeniedException;
import javax.jcr.MergeException;
import javax.jcr.Node;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.lock.LockException;

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
        Node n = itemManager.getNode(resolver.getQPath(absPath));
        return n.checkin();
    }

    /**
     * @see javax.jcr.version.VersionManager#checkout(String)
     */
    public void checkout(String absPath) throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
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
        Node n = itemManager.getNode(resolver.getQPath(absPath));
        return n.isCheckedOut();
    }

    /**
     * @see javax.jcr.version.VersionManager#getVersionHistory(String)
     */
    public VersionHistory getVersionHistory(String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
        Node n = itemManager.getNode(resolver.getQPath(absPath));
        return n.getVersionHistory();
    }

    /**
     * @see javax.jcr.version.VersionManager#getBaseVersion(String)
     */
    public Version getBaseVersion(String absPath) throws UnsupportedRepositoryOperationException, RepositoryException {
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
        Node n = itemManager.getNode(resolver.getQPath(absPath));
        n.restore(version, removeExisting);
    }

    /**
     * @see javax.jcr.version.VersionManager#restoreByLabel(String, String, boolean)
     */
    public void restoreByLabel(String absPath, String versionLabel, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
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
        // TODO: improve
        NodeImpl n = (NodeImpl) itemManager.getNode(resolver.getQPath(absPath));
        return n.merge(srcWorkspace, bestEffort, isShallow);
    }

    /**
     * @see javax.jcr.version.VersionManager#doneMerge(String, Version)
     */
    public void doneMerge(String absPath, Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        Node n = itemManager.getNode(resolver.getQPath(absPath));
        n.doneMerge(version);
    }

    /**
     * @see javax.jcr.version.VersionManager#cancelMerge(String, Version)
     */
    public void cancelMerge(String absPath, Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        Node n = itemManager.getNode(resolver.getQPath(absPath));
        n.cancelMerge(version);
    }

    /**
     * @see javax.jcr.version.VersionManager#createConfiguration(String, Version)
     */
    public Node createConfiguration(String absPath, Version baseline) throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO
        throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
    }

    /**
     * @see javax.jcr.version.VersionManager#setActivity(Node)
     */
    public Node setActivity(Node activity) throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO
        throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
    }

    /**
     * @see javax.jcr.version.VersionManager#getActivity()
     */
    public Node getActivity() throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO
        throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
    }

    /**
     * @see javax.jcr.version.VersionManager#createActivity(String)
     */
    public Node createActivity(String title) throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO
        throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
    }

    /**
     * @see javax.jcr.version.VersionManager#removeActivity(String)
     */
    public Node removeActivity(String title) throws UnsupportedRepositoryOperationException, RepositoryException {
        // TODO
        throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
    }

    /**
     * @see javax.jcr.version.VersionManager#merge(Node)
     */
    public NodeIterator merge(Node activityNode) throws VersionException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        // TODO
       throw new UnsupportedOperationException("JCR-2104: JSR 283 Versioning. Implementation missing");
    }
}