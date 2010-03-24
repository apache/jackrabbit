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
package org.apache.jackrabbit.rmi.client;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockManager;

import org.apache.jackrabbit.rmi.remote.RemoteLockManager;

public class ClientLockManager extends ClientObject implements LockManager {

    /** The current session. */
    private Session session;

    private RemoteLockManager remote;

    public ClientLockManager(
            Session session, RemoteLockManager remote,
            LocalAdapterFactory factory) {
        super(factory);
        this.session = session;
        this.remote = remote;
    }

    public String[] getLockTokens() throws RepositoryException {
        try {
            return remote.getLockTokens();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public void addLockToken(String lockToken) throws RepositoryException {
        try {
            remote.addLockToken(lockToken);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public void removeLockToken(String lockToken) throws RepositoryException {
        try {
            remote.removeLockToken(lockToken);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public Lock getLock(String absPath) throws RepositoryException {
        try {
            return getFactory().getLock(session, remote.getLock(absPath));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public boolean holdsLock(String absPath) throws RepositoryException {
        try {
            return remote.holdsLock(absPath);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public boolean isLocked(String absPath) throws RepositoryException {
        try {
            return remote.isLocked(absPath);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public Lock lock(
            String absPath, boolean isDeep, boolean isSessionScoped,
            long timeoutHint, String ownerInfo) throws RepositoryException {
        try {
            return getFactory().getLock(session, remote.lock(
                    absPath, isDeep, isSessionScoped, timeoutHint, ownerInfo));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public void unlock(String absPath) throws RepositoryException {
        try {
            remote.unlock(absPath);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

}
