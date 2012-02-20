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
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.observation.EventJournal;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;

import org.apache.jackrabbit.rmi.iterator.ArrayEventListenerIterator;
import org.apache.jackrabbit.rmi.observation.ClientEventPoll;
import org.apache.jackrabbit.rmi.remote.RemoteObservationManager;

/**
 * The <code>ClientObservationManager</code> class
 * <p>
 * This class uses an instance of the
 * {@link org.apache.jackrabbit.rmi.observation.ClientEventPoll} class for the
 * actual registration and event dispatching.
 * <p>
 * This class does not require the
 * {@link org.apache.jackrabbit.rmi.client.LocalAdapterFactory} and consequently
 * calls the base class constructor with a <code>null</code> factory.
 * <p>
 * See the {@link org.apache.jackrabbit.rmi.observation observation}
 * package comment for a description on how event listener registration and
 * notification is implemented.
 *
 * @see org.apache.jackrabbit.rmi.observation.ClientEventPoll
 */
public class ClientObservationManager extends ClientObject implements
        ObservationManager {

    /** The remote observation manager */
    private final RemoteObservationManager remote;

    /** The <code>Workspace</code> to which this observation manager belongs. */
    private final Workspace workspace;

    /** The ClientEventPoll class internally used for event dispatching */
    private ClientEventPoll poller;

    /**
     * Creates an instance of this class talking to the given remote observation
     * manager.
     *
     * @param remote The {@link RemoteObservationManager} backing this
     *      client-side observation manager.
     * @param workspace The <code>Workspace</code> instance to which this
     *      observation manager belongs.
     */
    public ClientObservationManager(Workspace workspace,
            RemoteObservationManager remote) {
        super(null);
        this.remote = remote;
        this.workspace = workspace;
    }

    /** {@inheritDoc} */
    public void addEventListener(EventListener listener, int eventTypes,
            String absPath, boolean isDeep, String[] uuid,
            String[] nodeTypeName, boolean noLocal)
            throws RepositoryException {
        try {
            long listenerId = getClientEventPoll().addListener(listener);
            remote.addEventListener(listenerId, eventTypes, absPath,
                isDeep, uuid, nodeTypeName, noLocal);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void removeEventListener(EventListener listener)
            throws RepositoryException {
        try {
            long id = getClientEventPoll().removeListener(listener);
            remote.removeEventListener(id);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public EventListenerIterator getRegisteredEventListeners() {
        EventListener[] listeners = (poller != null)
                ? poller.getListeners()
                : new EventListener[0];
        return new ArrayEventListenerIterator(listeners);
    }

    public EventJournal getEventJournal() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCR-3206");
    }

    public EventJournal getEventJournal(
            int eventTypes, String absPath, boolean isDeep,
            String[] uuid, String[] nodeTypeName) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCR-3206");
    }

    public void setUserData(String userData) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException("TODO: JCR-3206");
    }

    //---------- internal ------------------------------------------------------

    /**
     * Returns the {@link ClientEventPoll} instance used by this (client-side)
     * observation manager. This method creates the instance on the first call
     * and starts the poller thread to wait for remote events.
     *
     * @return poller instance
     */
    private synchronized ClientEventPoll getClientEventPoll() {
        if (poller == null) {
            poller = new ClientEventPoll(remote, workspace.getSession());
            poller.start();
        }
        return poller;
    }

}
