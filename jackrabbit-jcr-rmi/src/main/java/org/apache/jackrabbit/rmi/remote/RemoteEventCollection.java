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
         * Returns the event type.
         *
         * @return event type
         * @throws RemoteException on RMI errors
         */
        int getType() throws RemoteException;

        /**
         * Returns the absolute path of the underlying item.
         *
         * @return item path
         * @throws RemoteException on RMI errors
         */
        String getPath() throws RemoteException;

        /**
         * Returns the userID of the session causing this event.
         *
         * @return user identifier
         * @throws RemoteException on RMI errors
         */
        String getUserID() throws RemoteException;
    }

}
