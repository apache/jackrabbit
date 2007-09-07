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

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.jackrabbit.rmi.remote.RemoteXAResource;

/**
 * Local adapter for the JCR-RMI {@link RemoteXAResource} interface.
 *
 * @since 1.4
 */
public class ClientXAResource implements XAResource {

    /**
     * The adapted remote XA resource.
     */
    private RemoteXAResource remote;

    /**
     * Returns <code>true</code> if the given object is a local
     * adapter that refers to the same remote XA resource.
     * 
     * @see http://blogs.sun.com/fkieviet/entry/j2ee_jca_resource_adapters_the
     */
    public boolean isSameRM(XAResource xares) throws XAException {
        return xares instanceof ClientXAResource
            && remote == ((ClientXAResource) xares).remote;
    }

    /**
     * Creates a client adapter for the given remote XA resource.
     */
    public ClientXAResource(RemoteXAResource remote) {
        this.remote = remote;
    }

    private XAException getXAException(RemoteException e) {
        XAException exception = new XAException("Remote operation failed");
        exception.initCause(e);
        return exception;
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        try {
            remote.commit(xid, onePhase);
        } catch (RemoteException e) {
            throw getXAException(e);
        }
    }

    public void end(Xid xid, int flags) throws XAException {
        try {
            remote.end(xid, flags);
        } catch (RemoteException e) {
            throw getXAException(e);
        }
    }

    public void forget(Xid xid) throws XAException {
        try {
            remote.forget(xid);
        } catch (RemoteException e) {
            throw getXAException(e);
        }
    }

    public int getTransactionTimeout() throws XAException {
        try {
            return remote.getTransactionTimeout();
        } catch (RemoteException e) {
            throw getXAException(e);
        }
    }

    public int prepare(Xid xid) throws XAException {
        try {
            return remote.prepare(xid);
        } catch (RemoteException e) {
            throw getXAException(e);
        }
    }

    public Xid[] recover(int flag) throws XAException {
        try {
            return remote.recover(flag);
        } catch (RemoteException e) {
            throw getXAException(e);
        }
    }

    public void rollback(Xid xid) throws XAException {
        try {
            remote.rollback(xid);
        } catch (RemoteException e) {
            throw getXAException(e);
        }
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        try {
            return remote.setTransactionTimeout(seconds);
        } catch (RemoteException e) {
            throw getXAException(e);
        }
    }

    public void start(Xid xid, int flags) throws XAException {
        try {
            remote.start(xid, flags);
        } catch (RemoteException e) {
            throw getXAException(e);
        }
    }

}
