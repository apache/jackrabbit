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
        return manager.getLockTokens();
    }

    public void addLockToken(String lockToken) throws RepositoryException {
        manager.addLockToken(lockToken);
    }

    public void removeLockToken(String lockToken) throws RepositoryException {
        manager.removeLockToken(lockToken);
    }

    public boolean isLocked(String absPath) throws RepositoryException {
        return manager.isLocked(absPath);
    }

    public boolean holdsLock(String absPath) throws RepositoryException {
        return manager.holdsLock(absPath);
    }

    public RemoteLock getLock(String absPath)
            throws RepositoryException, RemoteException {
        return getFactory().getRemoteLock(manager.getLock(absPath));
    }

    public RemoteLock lock(
            String absPath, boolean isDeep, boolean isSessionScoped,
            long timeoutHint, String ownerInfo)
            throws RepositoryException, RemoteException {
        return getFactory().getRemoteLock(manager.lock(
                absPath, isDeep, isSessionScoped, timeoutHint, ownerInfo));
    }

    public void unlock(String absPath) throws RepositoryException {
        manager.unlock(absPath);
    }

}
