/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import javax.jcr.lock.LockException;

/**
 * Remote version of the JCR {@link javax.jcr.lock.Lock} interface.
 * Used by the  {@link org.apache.jackrabbit.rmi.server.ServerLock ServerLock}
 * and {@link org.apache.jackrabbit.rmi.client.ClientLock ClientLock}
 * adapter classes to provide transparent RMI access to remote locks.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding Lock method. The remote object will simply forward
 * the method call to the underlying Lock instance. Return values and
 * possible exceptions are copied over the network. RMI errors are signalled
 * with RemoteExceptions.
 *
 * @author Jukka Zitting
 * @see javax.jcr.lock.Lock
 * @see org.apache.jackrabbit.rmi.client.ClientLock
 * @see org.apache.jackrabbit.rmi.server.ServerLock
 */
public interface RemoteLock extends Remote {

    /**
     * @see javax.jcr.lock.Lock#getLockOwner()
     * @throws RemoteException on RMI exceptions
     */
    public String getLockOwner() throws RemoteException;

    /**
     * @see javax.jcr.lock.Lock#isDeep()
     * @throws RemoteException on RMI exceptions
     */
    public boolean isDeep() throws RemoteException;

    /**
     * @see javax.jcr.lock.Lock#getLockToken()
     * @throws RemoteException on RMI exceptions
     */
    public String getLockToken() throws RemoteException;

    /**
     * @see javax.jcr.lock.Lock#isLive()
     * @throws RemoteException on RMI exceptions
     */
    public boolean isLive() throws RemoteException;

    /**
     * @see javax.jcr.lock.Lock#refresh()
     * @throws RemoteException on RMI exceptions
     */
    public void refresh() throws LockException, RepositoryException,
        RemoteException;

}
