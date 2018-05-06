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
 * Remote version of the JCR {@link javax.jcr.observation.ObservationManager ObservationManager}
 * interface. Used by the
 * {@link org.apache.jackrabbit.rmi.server.ServerObservationManager ServerObservationManager}
 * and
 * {@link org.apache.jackrabbit.rmi.client.ClientObservationManager ClientObservationManager}
 * adapter base classes to provide transparent RMI access to remote observation
 * managers.
 * <p>
 * See the <a href="../observation/package.html"><code>observation</code></a>
 * package comment for a description on how event listener registration and
 * notification is implemented.
 *
 * @see javax.jcr.observation.ObservationManager
 * @see org.apache.jackrabbit.rmi.client.ClientObservationManager
 * @see org.apache.jackrabbit.rmi.server.ServerObservationManager
 */
public interface RemoteObservationManager extends Remote {

    /**
     * Remote version of the
     * {@link javax.jcr.observation.ObservationManager#addEventListener(javax.jcr.observation.EventListener, int, String, boolean, String[], String[], boolean) ObservationManager.addEventListener()}
     * method. See class comment for an explanation on how the
     * <code>listenerId</code> is used.
     *
     * @param listenerId The identification of the listener on the client
     *      side to which events will be directed.
     * @param eventTypes The mask of event types to be sent to this listener.
     * @param absPath The root item defining a subtree for which events are to
     *      be delivered.
     * @param isDeep <code>true</code> if the events from the complete subtree
     *      rooted at <code>absPath</code> are to be sent or only for the item
     *      at the given path.
     * @param uuid An optional list of node UUIDs for which events are to be
     *      sent. If <code>null</code> this parameter is ignored.
     * @param nodeTypeName An optional list of node type names for which events
     *      are to be sent. If <code>null</code> this parameter is ignored.
     * @param noLocal <code>true</code> if only events are to be sent which do
     *      not originate from the session to which this instance belongs.
     *
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void addEventListener(long listenerId, int eventTypes,
        String absPath, boolean isDeep, String[] uuid,
        String[] nodeTypeName, boolean noLocal)
        throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.observation.ObservationManager#removeEventListener(javax.jcr.observation.EventListener) ObservationManager.removeEventListener()}
     * method. See class comment for an explanation on how the
     * <code>listenerId</code> is used.
     *
     * @param listenerId The identification of the listener on the client
     *      side to which events will be directed.
     *
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void removeEventListener(long listenerId)
        throws RepositoryException, RemoteException;

    /**
     * Returns the next event to be dispatched to registered event listeners. If
     * no event is available, this method blocks until one is available or until
     * the given timeout expires.
     *
     * @param timeout The time in milliseconds to wait for the next event
     *      available to be dispatched. If negative or zero, this method waits
     *      for ever.
     *
     * @return The {@link RemoteEventCollection} to be dispatched. <code>null</code> is
     *      returned if the method timed out waiting for an event to be
     *      dispatched.
     *
     * @throws RemoteException on RMI errors
     */
    RemoteEventCollection getNextEvent(long timeout)
        throws RemoteException;
}
