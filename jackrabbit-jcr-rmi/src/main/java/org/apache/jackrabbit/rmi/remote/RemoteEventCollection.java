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
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * The <code>RemoteEventCollection</code> class serves as a container for
 * notifications sent to registered event listeners. Instances of this class are
 * created by the server-side event listener proxies and sent to the client-side
 * event poller. On the client-side the enclosed list of events is then sent to
 * the listener identified by the contained listener identifier.
 */
public interface RemoteEventCollection extends Remote {

    /**
     * Returns unique identifier of the client-side listener to which the
     * enclosed events should be sent.
     *
     * @return unique listener identifier
     * @throws RemoteException on RMI errors
     */
    long getListenerId() throws RemoteException;

    /**
     * Returns the list of events to be sent to the client-side listener
     * identified by {@link #getListenerId()}.
     *
     * @return list of events
     * @throws RemoteException on RMI errors
     */
    RemoteEvent[] getEvents() throws RemoteException;

    /**
     * The <code>RemoteEvent</code> class provides an encapsulation of single
     * events in an event list sent to a registered listener.
     */
    public static interface RemoteEvent extends Remote {
        /**
         * Remote version of the
         * {@link javax.jcr.observation.Event#getType() Event.getType()} method.
         *
         * @return the type of this event.
         * @throws RemoteException on RMI errors
         */
        int getType() throws RemoteException;

        /**
         * Remote version of the
         * {@link javax.jcr.observation.Event#getPath() Event.getPath()} method.
         *
         * @return the absolute path associated with this event or
         *         <code>null</code>.
         * @throws RepositoryException on repository errors
         * @throws RemoteException on RMI errors
         */
        String getPath() throws RepositoryException, RemoteException;

        /**
         * Remote version of the
         * {@link javax.jcr.observation.Event#getUserID() Event.getUserID()} method.
         *
         * @return the user ID.
         * @throws RemoteException on RMI errors
         */
        String getUserID() throws RemoteException;

        /**
         * Remote version of the
         * {@link javax.jcr.observation.Event#getIdentifier() Event.getIdentifier()} method.
         *
         * @return the identifier associated with this event or <code>null</code>.
         * @throws RepositoryException on repository errors
         * @throws RemoteException on RMI errors
         */
        String getIdentifier() throws RepositoryException, RemoteException;

        /**
         * Remote version of the
         * {@link javax.jcr.observation.Event#getInfo() Event.getInfo()} method.
         *
         * @return A <code>Map</code> containing parameter information for instances
         *         of a <code>NODE_MOVED</code> event.
         * @throws RepositoryException on repository errors
         * @throws RemoteException on RMI errors
         */
        Map getInfo() throws RepositoryException, RemoteException;

        /**
         * Remote version of the
         * {@link javax.jcr.observation.Event#getUserData() Event.getUserData()} method.
         *
         * @return The user data string.
         * @throws RepositoryException on repository errors
         * @throws RemoteException on RMI errors
         */
        String getUserData() throws RepositoryException, RemoteException;

        /**
         * Remote version of the
         * {@link javax.jcr.observation.Event#getDate() Event.getDate()} method.
         *
         * @return the date when the change was persisted that caused this event.
         * @throws RepositoryException on repository errors
         * @throws RemoteException on RMI errors
         */
        long getDate() throws RepositoryException, RemoteException;
    }
}
