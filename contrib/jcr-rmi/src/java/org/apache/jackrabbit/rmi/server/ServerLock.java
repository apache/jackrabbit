/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

import org.apache.jackrabbit.rmi.remote.RemoteLock;

/**
 * Remote adapter for the JCR {@link javax.jcr.lock.Lock Lock} interface.
 * This class makes a local lock available as an RMI service using
 * the {@link org.apache.jackrabbit.rmi.remote.RemoteLock RemoteLock}
 * interface.
 * 
 * @author Jukka Zitting
 * @see javax.jcr.lock.Lock
 * @see org.apache.jackrabbit.rmi.remote.RemoteLock
 */
public class ServerLock extends UnicastRemoteObject implements RemoteLock {
    
    /** The adapted local lock. */
    private Lock lock;
    
    /**
     * Creates a remote adapter for the given local lock.
     * 
     * @param lock local lock
     * @throws RemoteException on RMI errors
     */
    public ServerLock(Lock lock) throws RemoteException {
        this.lock = lock;
    }

    /** {@inheritDoc} */
    public String getLockOwner() throws RemoteException {
        return lock.getLockOwner();
    }

    /** {@inheritDoc} */
    public boolean isDeep() throws RemoteException {
        return lock.isDeep();
    }

    /** {@inheritDoc} */
    public String getLockToken() throws RemoteException {
        return lock.getLockToken();
    }

    /** {@inheritDoc} */
    public boolean isLive() throws RemoteException {
        return lock.isLive();
    }

    /** {@inheritDoc} */
    public void refresh() throws LockException, RepositoryException,
            RemoteException {
        lock.refresh();
    }

}
