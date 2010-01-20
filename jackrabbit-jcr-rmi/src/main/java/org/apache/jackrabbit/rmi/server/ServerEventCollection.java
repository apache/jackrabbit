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

import org.apache.jackrabbit.rmi.remote.RemoteEventCollection;

/**
 * The <code>ServerEventCollection</code> class implements the
 * {@link org.apache.jackrabbit.rmi.remote.RemoteEventCollection}event to
 * actually sent the server-side event to the client.
 * <p>
 * This class does not directly relate to any JCR class because beside the list
 * of events the unique identifier of the client-side listener has to be
 * provided such that the receiving listener may be identified on the
 * client-side.
 */
public class ServerEventCollection extends ServerObject implements
        RemoteEventCollection {

    /** The unique identifier of the receiving listener */
    private final long listenerId;

    /**
     * The list of
     * {@link org.apache.jackrabbit.rmi.remote.RemoteEventCollection.RemoteEvent}.
     */
    private final RemoteEvent[] events;

    /**
     * Creates an instance of this class.
     *
     * @param listenerId The unique identifier of the client-side listener to
     *            which the events should be sent.
     * @param events The list of {@link RemoteEvent remote events}.
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    ServerEventCollection(
            long listenerId, RemoteEvent[] events, RemoteAdapterFactory factory)
            throws RemoteException {
        super(factory);

        this.listenerId = listenerId;
        this.events = events;
    }

    /** {@inheritDoc} */
    public long getListenerId() {
        return listenerId;
    }

    /** {@inheritDoc} */
    public RemoteEvent[] getEvents() {
        return events;
    }

    /**
     * Server side implementation of the {@link RemoteEvent} interface.
     *
     * {@inheritDoc}
     */
    public static class ServerEvent extends ServerObject implements RemoteEvent {

        /** Event type */
        private final int type;

        /** Item path */
        private final String path;

        /** User identifier */
        private final String userID;

        /**
         * Creates an instance of this class.
         * @param type The event type.
         * @param path The absolute path to the underlying item.
         * @param userId The userID of the originating session.
         * @param factory remote adapter factory
         * @throws RemoteException on RMI errors
         */
        ServerEvent(
                int type, String path, String userId, RemoteAdapterFactory factory)
                throws RemoteException {
            super(factory);
            this.type = type;
            this.path = path;
            this.userID = userId;
        }

        /** {@inheritDoc} */
        public String getPath() {
            return path;
        }

        /** {@inheritDoc} */
        public int getType() {
            return type;
        }

        /** {@inheritDoc} */
        public String getUserID() {
            return userID;
        }
    }
}
