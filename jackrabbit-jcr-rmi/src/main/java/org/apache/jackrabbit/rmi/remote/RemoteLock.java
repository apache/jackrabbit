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
package org.apache.jackrabbit.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

/**
 * Remote version of the JCR {@link javax.jcr.lock.Lock} interface.
 * Used by the  {@link org.apache.jackrabbit.rmi.server.ServerLock ServerLock}
 * and {@link org.apache.jackrabbit.rmi.client.ClientLock ClientLock}
 * adapter classes to provide transparent RMI access to remote locks.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding Lock method. The remote object will simply forward
 * the method call to the underlying Lock instance. Return values and
 * possible exceptions are copied over the network. RMI errors are signaled
 * with RemoteExceptions.
 *
 * @see javax.jcr.lock.Lock
 * @see org.apache.jackrabbit.rmi.client.ClientLock
 * @see org.apache.jackrabbit.rmi.server.ServerLock
 */
public interface RemoteLock extends Remote {

    /**
     * Remote version of the
     * {@link javax.jcr.lock.Lock#getNode() Lock.getNode()} method.
     *
     * @return remote node
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     * @since JCR-RMI 2.0
     */
    RemoteNode getNode() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.lock.Lock#getLockOwner() Lock.getLockOwner()} method.
     *
     * @return lock owner
     * @throws RemoteException on RMI errors
     */
    String getLockOwner() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.lock.Lock#isDeep() Lock.isDeep()} method.
     *
     * @return <code>true</code> if the lock is deep,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean isDeep() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.lock.Lock#getLockToken() Lock.getLockToken()} method.
     *
     * @return lock token
     * @throws RemoteException on RMI errors
     */
    String getLockToken() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.lock.Lock#isLive() Lock.isLive()} method.
     *
     * @return <code>true</code> if the lock is live,
     *         <code>false</code> otherwise
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    boolean isLive() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.lock.Lock#refresh() Lock.refresh()} method.
     *
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void refresh() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.lock.Lock#isSessionScoped()} () Lock.isSessionScoped()} method.
     *
     * @return <code>true</code> if the lock is live,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean isSessionScoped() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.lock.Lock#getSecondsRemaining()} () Lock.getSecondsRemaining()} method.
     *
     * @return the number of seconds remaining until this lock times out.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
	long getSecondsRemaining() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.lock.Lock#isLockOwningSession()} () Lock.isLockOwningSession()} method.
     *
     * @return a <code>boolean</code>.
     * @throws RemoteException on RMI errors
     */
	boolean isLockOwningSession() throws RemoteException;


}
