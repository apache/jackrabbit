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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;

import org.apache.jackrabbit.rmi.remote.RemoteLock;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteLock RemoteLock}
 * interface. This class makes a remote lock locally available using
 * the JCR {@link javax.jcr.lock.Lock Lock} interface.
 *
 * @see javax.jcr.lock.Lock
 * @see org.apache.jackrabbit.rmi.remote.RemoteLock
 */
public class ClientLock extends ClientObject implements Lock {

    /** Current session. */
    private Session session;

    /** The adapted remote lock. */
    private RemoteLock remote;

    /**
     * Creates a local adapter for the given remote lock.
     *
     * @param session current session
     * @param remote remote lock
     * @param factory local adapter factory
     */
    public ClientLock(Session session, RemoteLock remote, LocalAdapterFactory factory) {
        super(factory);
        this.session = session;
        this.remote = remote;
    }

    /** {@inheritDoc} */
    public Node getNode() {
        try {
            return getNode(session, remote.getNode());
        } catch (RemoteException e) {
            throw new RemoteRuntimeException(e);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public String getLockOwner() {
        try {
            return remote.getLockOwner();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isDeep() {
        try {
            return remote.isDeep();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getLockToken() {
        try {
            return remote.getLockToken();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isLive() throws RepositoryException {
        try {
            return remote.isLive();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public void refresh() throws RepositoryException {
        try {
            remote.refresh();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isSessionScoped() {
        try {
            return remote.isSessionScoped();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public long getSecondsRemaining() throws RepositoryException {
        try {
            return remote.getSecondsRemaining();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isLockOwningSession() {
        try {
            return remote.isLockOwningSession();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }
}
