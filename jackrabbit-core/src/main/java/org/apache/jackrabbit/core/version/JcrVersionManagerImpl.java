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
package org.apache.jackrabbit.core.version;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.PropertyType;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.ItemValidator;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.api.jsr283.version.Version;
import org.apache.jackrabbit.api.jsr283.version.VersionHistory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.uuid.UUID;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Implementation of the {@link org.apache.jackrabbit.api.jsr283.version.VersionManager}.
 * <p/>
 * Note: For a cleaner architecture, we should probably rename the existing classes
 * that implement the internal version manager, and name this VersionManagerImpl.
 */
public class JcrVersionManagerImpl implements org.apache.jackrabbit.api.jsr283.version.VersionManager {

    /**
     * default logger
     */
    private static final Logger log = LoggerFactory.getLogger(JcrVersionManagerImpl.class);

    /**
     * workspace session
     */
    private final SessionImpl session;

    /**
     * Creates a new version manager for the given session
     * @param session workspace sesion
     */
    public JcrVersionManagerImpl(SessionImpl session) {
        this.session = session;
    }

    /**
     * {@inheritDoc}
     */
    public Version checkin(String absPath) throws RepositoryException {
        return (Version) session.getNode(absPath).checkin();
    }

    /**
     * {@inheritDoc}
     */
    public void checkout(String absPath) throws RepositoryException {
        session.getNode(absPath).checkout();
    }

    /**
     * {@inheritDoc}
     */
    public Version checkpoint(String absPath) throws RepositoryException {
        // this is not quite correct, since the entire checkpoint operation
        // should be atomic
        Node node = session.getNode(absPath);
        Version v = (Version) node.checkin();
        node.checkout();
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCheckedOut(String absPath) throws RepositoryException {
        return session.getNode(absPath).isCheckedOut();
    }

    /**
     * {@inheritDoc}
     */
    public VersionHistory getVersionHistory(String absPath)
            throws RepositoryException {
        return (VersionHistory) session.getNode(absPath).getVersionHistory();
    }

    /**
     * {@inheritDoc}
     */
    public Version getBaseVersion(String absPath)
            throws RepositoryException {
        return (Version) session.getNode(absPath).getBaseVersion();
    }

    /**
     * {@inheritDoc}
     */
    public void restore(Version[] versions, boolean removeExisting)
            throws RepositoryException {
        session.getWorkspace().restore(versions, removeExisting);
    }

    /**
     * {@inheritDoc}
     */
    public void restore(String absPath, String versionName,
                        boolean removeExisting)
            throws RepositoryException {
        session.getNode(absPath).restore(versionName, removeExisting);
    }

    /**
     * {@inheritDoc}
     */
    public void restore(Version version, boolean removeExisting)
            throws RepositoryException {
        session.getWorkspace().restore(new Version[]{version}, removeExisting);
    }

    /**
     * {@inheritDoc}
     */
    public void restore(String absPath, Version version, boolean removeExisting)
            throws RepositoryException {
        session.getNode(absPath).restore(version, removeExisting);
    }

    /**
     * {@inheritDoc}
     */
    public void restoreByLabel(String absPath, String versionLabel,
                               boolean removeExisting)
            throws RepositoryException {
        session.getNode(absPath).restoreByLabel(versionLabel, removeExisting);
    }

    /**
     * {@inheritDoc}
     */
    public NodeIterator merge(String absPath, String srcWorkspace,
                              boolean bestEffort)
            throws RepositoryException {
        return ((NodeImpl) session.getNode(absPath))
                .merge(srcWorkspace, bestEffort, false);
    }

    /**
     * {@inheritDoc}
     */
    public NodeIterator merge(String absPath, String srcWorkspace,
                              boolean bestEffort, boolean isShallow)
            throws RepositoryException {
        return ((NodeImpl) session.getNode(absPath))
                .merge(srcWorkspace, bestEffort, isShallow);
    }

    /**
     * {@inheritDoc}
     */
    public void doneMerge(String absPath, Version version)
            throws RepositoryException {
        session.getNode(absPath).doneMerge(version);
    }

    /**
     * {@inheritDoc}
     */
    public void cancelMerge(String absPath, Version version)
            throws RepositoryException {
        session.getNode(absPath).cancelMerge(version);
    }

    /**
     * {@inheritDoc}
     */
    public Node createConfiguration(String absPath, Version baseline)
            throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("comming soon...");
    }

    /**
     * {@inheritDoc}
     */
    public Node setActivity(Node activity) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("comming soon...");
    }

    /**
     * {@inheritDoc}
     */
    public Node getActivity() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("comming soon...");
    }

    /**
     * {@inheritDoc}
     */
    public Node createActivity(String title) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("comming soon...");
    }

    /**
     * {@inheritDoc}
     */
    public Node removeActivity(String title) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("comming soon...");
    }

    /**
     * {@inheritDoc}
     */
    public NodeIterator merge(Node activityNode) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("comming soon...");
    }
}