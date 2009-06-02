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
import org.apache.jackrabbit.jcr2spi.lock.LockStateManager;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;

import javax.jcr.lock.LockManager;
import javax.jcr.lock.LockException;
import javax.jcr.RepositoryException;
import javax.jcr.Node;

/**
 * <code>JcrLockManager</code>...
 */
public class JcrLockManager implements LockManager {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(JcrLockManager.class);

    private final LockStateManager lockStateMgr;
    private final ItemManager itemManager;
    private final PathResolver resolver;

    protected JcrLockManager(SessionImpl session) {
        lockStateMgr = session.getLockStateManager();
        itemManager = session.getItemManager();
        resolver = session.getPathResolver();
    }

    //--------------------------------------------------------< LockManager >---
    /**
     * @see javax.jcr.lock.LockManager#getLock(String)
     */
    public javax.jcr.lock.Lock getLock(String absPath) throws LockException, RepositoryException {
        Node n = itemManager.getNode(resolver.getQPath(absPath));
        return n.getLock();
    }

    /**
     * @see javax.jcr.lock.LockManager#isLocked(String)
     */
    public boolean isLocked(String absPath) throws RepositoryException {
        Node n = itemManager.getNode(resolver.getQPath(absPath));
        return n.isLocked();
    }

    /**
     * @see javax.jcr.lock.LockManager#holdsLock(String)
     */
    public boolean holdsLock(String absPath) throws RepositoryException {
        Node n = itemManager.getNode(resolver.getQPath(absPath));
        return n.holdsLock();
    }

    /**
     * @see javax.jcr.lock.LockManager#lock(String, boolean, boolean, long, String)
     */
    public javax.jcr.lock.Lock lock(String absPath, boolean isDeep, boolean isSessionScoped, long timeoutHint, String ownerInfo) throws RepositoryException {
        Node n = itemManager.getNode(resolver.getQPath(absPath));
        return ((NodeImpl) n).lock(isDeep, isSessionScoped, timeoutHint, ownerInfo);
    }

    /**
     * @see javax.jcr.lock.LockManager#unlock(String)
     */
    public void unlock(String absPath) throws LockException, RepositoryException {
        Node n = itemManager.getNode(resolver.getQPath(absPath));
        n.unlock();
    }

    /**
     * Returns the lock tokens present on the <code>SessionInfo</code> this
     * manager has been created with.
     *
     * @see javax.jcr.lock.LockManager#getLockTokens()
     */
    public String[] getLockTokens() throws RepositoryException {
        return lockStateMgr.getLockTokens();
    }

    /**
     * Delegates this call to {@link WorkspaceManager#addLockToken(String)}.
     * If this succeeds this method will inform all locks stored in the local
     * map in order to give them the chance to update their lock information.
     *
     * @see javax.jcr.lock.LockManager#addLockToken(String)
     */
    public void addLockToken(String lt) throws LockException, RepositoryException {
        lockStateMgr.addLockToken(lt);
    }

    /**
     * If the lock addressed by the token is session-scoped, this method will
     * throw a LockException, such as defined by JSR170 v.1.0.1 for
     * {@link javax.jcr.Session#removeLockToken(String)}.<br>Otherwise the call is
     * delegated to {@link WorkspaceManager#removeLockToken(String)}.
     * All locks stored in the local lock map are notified by the removed
     * token in order have them updated their lock information.
     *
     * @see javax.jcr.lock.LockManager#removeLockToken(String)
     */
    public void removeLockToken(String lt) throws LockException, RepositoryException {
        lockStateMgr.removeLockToken(lt);
    }

}