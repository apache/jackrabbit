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
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.observation.ObservationManager;

import org.apache.jackrabbit.rmi.observation.Queue;
import org.apache.jackrabbit.rmi.observation.ServerEventListenerProxy;
import org.apache.jackrabbit.rmi.remote.RemoteEventCollection;
import org.apache.jackrabbit.rmi.remote.RemoteObservationManager;

/**
 * Remote adapter for the JCR
 * {@link javax.jcr.observation.ObservationManager ObservationManager} interface.
 * This class makes a local item available as an RMI service using
 * the {@link org.apache.jackrabbit.rmi.remote.RemoteObservationManager RemoteObservationManager}
 * interface.
 * <p>
 * This class works in conjunction with the
 * {@link org.apache.jackrabbit.rmi.client.ClientObservationManager} class to
 * implement the distributed the event listener registration described in
 * <a href="../observation/package.html"><code>observation</code></a> package
 * comment.
 *
 * @see javax.jcr.observation.ObservationManager
 * @see org.apache.jackrabbit.rmi.remote.RemoteObservationManager
 */
public class ServerObservationManager extends ServerObject implements
        RemoteObservationManager {

    /** The adapted local observation manager. */
    private ObservationManager observationManager;

    /**
     * The map of event listener proxies indexed by the unique identifier.
     */
	private Map<Long, ServerEventListenerProxy> proxyMap;

    /**
     * The queue to which event listener proxies post events to be reported
     * by the {@link #getNextEvent(long)} method.
     */
    private Queue queue;

    /**
     * Creates a remote adapter for the given local workspace.
     *
     * @param observationManager local observation manager
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerObservationManager(ObservationManager observationManager,
            RemoteAdapterFactory factory)
            throws RemoteException {
        super(factory);
        this.observationManager = observationManager;
    }

    /** {@inheritDoc} */
    public void addEventListener(long listenerId, int eventTypes,
        String absPath, boolean isDeep, String[] uuid, String[] nodeTypeName,
        boolean noLocal) throws RepositoryException, RemoteException {

        // find the proxy or create one
        ServerEventListenerProxy proxy;
        synchronized (this) {
            if (proxyMap == null) {
                proxyMap = new HashMap<Long, ServerEventListenerProxy>();
            }

            Long id = Long.valueOf(listenerId);
            proxy = proxyMap.get(id);
            if (proxy == null) {
                proxy = new ServerEventListenerProxy(getFactory(), listenerId,
                    getQueue());
                proxyMap.put(id, proxy);
            }
        }

        // register the proxy with the observation manager
        observationManager.addEventListener(proxy, eventTypes, absPath,
            isDeep, uuid, nodeTypeName, noLocal);
    }

    /** {@inheritDoc} */
    public void removeEventListener(long listenerId)
        throws RepositoryException, RemoteException {

        // try to find the proxy in the map
        ServerEventListenerProxy proxy;
        synchronized (this) {
            if (proxyMap == null) {
                return;
            }

            Long id = new Long(listenerId);
            proxy = (ServerEventListenerProxy) proxyMap.remove(id);
            if (proxy == null) {
                return;
            }
        }

        // register the proxy with the observation manager
        observationManager.removeEventListener(proxy);
    }

    /** {@inheritDoc} */
    public RemoteEventCollection getNextEvent(long timeout) throws RemoteException {
        // need the queue
        checkQueue();

        try {
            if (timeout < 0) {
                timeout = 0;
            }
            return (RemoteEventCollection) queue.get(timeout);
        } catch (InterruptedException ie) {
            // don't retry, but log
        }

        // did not get anything, fall back to nothing
        return null;
    }

    //---------- internal ------------------------------------------------------

    /**
     * Makes sure, the {@link #queue} field is assigned a value.
     */
    private synchronized void checkQueue() {
        if (queue == null) {
            queue = new Queue();
        }
    }

    /**
     * Returns the <code>Channel</code> allocating it if required.
     *
     * @return queue
     */
    private Queue getQueue() {
        checkQueue();
        return queue;
    }
}
