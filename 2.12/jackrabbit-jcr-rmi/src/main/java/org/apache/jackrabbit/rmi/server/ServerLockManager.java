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
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.lock.LockManager;

import org.apache.jackrabbit.rmi.remote.RemoteLock;
import org.apache.jackrabbit.rmi.remote.RemoteLockManager;

public class ServerLockManager extends ServerObject
        implements RemoteLockManager {

    /** The adapted local lock manager. */
    private LockManager manager;

    public ServerLockManager(LockManager manager, RemoteAdapterFactory factory)
            throws RemoteException {
        super(factory);
        this.manager = manager;
    }

    public String[] getLockTokens() throws RepositoryException {
        try {
            return manager.getLockTokens();
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public void addLockToken(String lockToken) throws RepositoryException {
        try {
            manager.addLockToken(lockToken);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public void removeLockToken(String lockToken) throws RepositoryException {
        try {
            manager.removeLockToken(lockToken);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public boolean isLocked(String absPath) throws RepositoryException {
        try {
            return manager.isLocked(absPath);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public boolean holdsLock(String absPath) throws RepositoryException {
        try {
            return manager.holdsLock(absPath);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public RemoteLock getLock(String absPath)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteLock(manager.getLock(absPath));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public RemoteLock lock(
            String absPath, boolean isDeep, boolean isSessionScoped,
            long timeoutHint, String ownerInfo)
            throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteLock(manager.lock(
                    absPath, isDeep, isSessionScoped, timeoutHint, ownerInfo));
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

    public void unlock(String absPath) throws RepositoryException {
        try {
            manager.unlock(absPath);
        } catch (RepositoryException e) {
            throw getRepositoryException(e);
        }
    }

}
