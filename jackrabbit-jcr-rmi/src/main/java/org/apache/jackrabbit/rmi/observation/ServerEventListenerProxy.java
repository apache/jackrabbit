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
package org.apache.jackrabbit.rmi.observation;

import java.rmi.RemoteException;

import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.jackrabbit.rmi.remote.RemoteEventCollection;
import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ServerEventListenerProxy</code> class is the server-side event
 * listener proxy registered on behalf of a client-side event listener identified
 * with the unique identifier.
 * <p>
 * The term <i>Server</i> in this class indicates, that this is a server-side
 * class. In contrast to the classes in the
 * {@link org.apache.jackrabbit.rmi.server} package, this class neither extends
 * the {@link org.apache.jackrabbit.rmi.server.ServerObject} class nor does it
 * implement any of the remote interfaces in the
 * {@link org.apache.jackrabbit.rmi.remote} package because it only is
 * instantiated to be used on the server side.
 * <p>
 * See the package overview for an explanation of the mechanisms implemented for
 * event dispatching.
 */
public class ServerEventListenerProxy implements EventListener {

    /** logger */
    private static final Logger log =
        LoggerFactory.getLogger(ServerEventListenerProxy.class);

    /** The factory used to convert event iterators to remote events */
    private final RemoteAdapterFactory factory;

    /**
     * The unique indentifier of the client-side event listener on whose
     * behalf this listener proxy is registered.
     */
    private final long listenerId;

    /**
     * The queue to which remote events are queue for them to be picked up
     * by calls to the
     * {@link org.apache.jackrabbit.rmi.remote.RemoteObservationManager#getNextEvent(long)}
     * method.
     */
    private final Queue queue;

    /**
     * Creates a new instance of this listener proxy.
     *
     * @param factory The {@link RemoteAdapterFactory} used to convert the
     *      {@link EventIterator} instances to {@link RemoteEventCollection} objects.
     * @param listenerId The unique identifier of the client-side event listener
     *      on whose behalf this proxy works.
     * @param queue The sink to which events to be dispatched to the client are
     *      queued to be picked up.
     */
    public ServerEventListenerProxy(RemoteAdapterFactory factory,
            long listenerId, Queue queue) {
        this.factory = factory;
        this.listenerId = listenerId;
        this.queue = queue;
    }

    /**
     * Converts the {@link javax.jcr.observation.Event} instances in the given
     * iterator to an instance of {@link RemoteEventCollection} for them to be dispatched
     * to the client-side event listener.
     *
     * @param events The {@link javax.jcr.observation.Event Events} to be
     *      dispatched.
     */
    public void onEvent(EventIterator events) {
        try {
            RemoteEventCollection remoteEvent = factory.getRemoteEvent(listenerId, events);
            queue.put(remoteEvent);
        } catch (RemoteException re) {
            Throwable t = (re.getCause() == null) ? re : re.getCause();
            log.error("Problem creating remote event for " + listenerId, t);
        }
    }

    //---------- Object overwrite ----------------------------------------------

    /**
     * Returns the client-side listener identifier as its hash code.
     *
     * @return hash code
     */
    public int hashCode() {
        return (int) listenerId;
    }

    /**
     * Returns <code>true</code> if <code>obj</code> is either the same as this
     * or a proxy for the same client-side listener, which is identicated by the
     * same listener identifier.
     *
     * @param obj The object to compare to.
     *
     * @return <code>true</code> if <code>obj</code> is the same or a proxy for
     *      the same client-side listener.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof ServerEventListenerProxy) {
            return listenerId == ((ServerEventListenerProxy) obj).listenerId;
        } else {
            return false;
        }
    }

    /**
     * Returns the a string representation of this instance, which is an
     * indication of this class's name and the unique identifier of the real
     * event listener.
     *
     * @return string representation
     */
    public String toString() {
        return "EventListenerProxy: listenerId=" + listenerId;
    }
}
