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

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.AbstractNodeData;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.NodeImpl;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.nodetype.ConstraintViolationException;
import java.util.Calendar;
import java.util.List;

/**
 * Base implementation of the {@link javax.jcr.version.Version} interface.
 */
public class VersionImpl extends NodeImpl implements Version {

    /**
     * Logger instance.
     */
    private static Logger log = LoggerFactory.getLogger(VersionImpl.class);

    /**
     * Create a new instance of this class.
     * @param itemMgr item manager
     * @param sessionContext component context of the associated session
     * @param data node data
     */
    public VersionImpl(
            ItemManager itemMgr, SessionContext sessionContext,
            AbstractNodeData data) {
        super(itemMgr, sessionContext, data);
    }

    /**
     * Returns the internal version. Subclass responsibility.
     * @return internal version
     * @throws RepositoryException if the internal version is not available
     */
    protected InternalVersion getInternalVersion() throws RepositoryException {
        SessionImpl session = sessionContext.getSessionImpl();
        InternalVersion version =
                session.getInternalVersionManager().getVersion((NodeId) id);
        if (version == null) {
            throw new InvalidItemStateException(id + ": the item does not exist anymore");
        }
        return version;
    }

    /**
     * {@inheritDoc}
     */
    public Calendar getCreated() throws RepositoryException {
        return getInternalVersion().getCreated();
    }

    /**
     * {@inheritDoc}
     */
    public javax.jcr.version.Version[] getSuccessors() throws RepositoryException {
        // need to wrap it around proper node
        List<InternalVersion> suc = getInternalVersion().getSuccessors();
        Version[] ret = new Version[suc.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = (Version) sessionContext.getSessionImpl().getNodeById(
                    suc.get(i).getId());
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public javax.jcr.version.Version[] getPredecessors() throws RepositoryException {
        // need to wrap it around proper node
        InternalVersion[] pred = getInternalVersion().getPredecessors();
        Version[] ret = new Version[pred.length];
        for (int i = 0; i < pred.length; i++) {
            ret[i] = (Version) sessionContext.getSessionImpl().getNodeById(pred[i].getId());
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public Version getLinearSuccessor() throws RepositoryException {
        // get base version. this can certainly be optimized
        SessionImpl session = sessionContext.getSessionImpl();
        InternalVersionHistory vh = ((VersionHistoryImpl) getContainingHistory())
                .getInternalVersionHistory();
        Node vn = session.getNodeById(vh.getVersionableId());
        InternalVersion base = ((VersionImpl) vn.getBaseVersion()).getInternalVersion();

        InternalVersion suc = getInternalVersion().getLinearSuccessor(base);
        return suc == null ? null : (Version) session.getNodeById(suc.getId());
    }

    /**
     * {@inheritDoc}
     */
    public javax.jcr.version.Version getLinearPredecessor() throws RepositoryException {
        InternalVersion pred = getInternalVersion().getLinearPredecessor();
        return pred == null ? null : (Version) sessionContext.getSessionImpl().getNodeById(pred.getId());
    }

    /**
     * {@inheritDoc}
     */
    public javax.jcr.version.VersionHistory getContainingHistory() throws RepositoryException {
        return (VersionHistory) getParent();
    }

    /**
     * Returns the frozen node of this version
     *
     * @return the internal frozen node
     * @throws javax.jcr.RepositoryException if an error occurs
     */
    public InternalFrozenNode getInternalFrozenNode() throws RepositoryException {
        return getInternalVersion().getFrozenNode();
    }

    /**
     * {@inheritDoc}
     */
    public Node getFrozenNode() throws RepositoryException {
        return sessionContext.getSessionImpl().getNodeById(getInternalVersion().getFrozenNodeId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSame(Item otherItem) {
        if (otherItem instanceof VersionImpl) {
            // since all versions live in the same workspace, we can compare the uuids
            try {
                InternalVersion other = ((VersionImpl) otherItem).getInternalVersion();
                return other.getId().equals(getInternalVersion().getId());
            } catch (RepositoryException e) {
                log.warn("Unable to retrieve internal version objects: " + e.getMessage());
                log.debug("Stack dump:", e);
            }
        }
        return false;
    }

    /**
     * Checks if this version is more recent than the given version <code>v</code>.
     * A version is more recent if and only if it is a successor (or a successor
     * of a successor, etc., to any degree of separation) of the compared one.
     *
     * @param v the version to check
     * @return <code>true</code> if the version is more recent;
     *         <code>false</code> otherwise.
     * @throws RepositoryException if a repository error occurs
     */
    public boolean isMoreRecent(VersionImpl v) throws RepositoryException {
        return getInternalVersion().isMoreRecent(v.getInternalVersion());
    }

    /**
     * Checks if this is the root version.
     * @return <code>true</code> if this version is the root version;
     *         <code>false</code> otherwise.
     * @throws RepositoryException if a repository error occurs
     */
    public boolean isRootVersion() throws RepositoryException {
        return getInternalVersion().isRootVersion();
    }

    //--------------------------------------< Overwrite "protected" methods >---


    /**
     * Always throws a {@link javax.jcr.nodetype.ConstraintViolationException} since this node
     * is protected.
     *
     * @throws javax.jcr.nodetype.ConstraintViolationException
     */
    @Override
    public void update(String srcWorkspaceName) throws ConstraintViolationException {
        String msg = "update operation not allowed: " + this;
        log.debug(msg);
        throw new ConstraintViolationException(msg);
    }

    /**
     * Always throws a {@link javax.jcr.nodetype.ConstraintViolationException} since this node
     * is protected.
     *
     * @throws javax.jcr.nodetype.ConstraintViolationException
     */
    @Override
    public NodeIterator merge(String srcWorkspace, boolean bestEffort)
            throws ConstraintViolationException {
        String msg = "merge operation not allowed: " + this;
        log.debug(msg);
        throw new ConstraintViolationException(msg);
    }

    //--------------------------------------------------------------< Object >

    /**
     * Return a string representation of this version node for diagnostic
     * purposes.
     *
     * @return "version node /path/to/item"
     */
    public String toString() {
        return "version " + super.toString();
    }

}
