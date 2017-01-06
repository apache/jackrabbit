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

import javax.jcr.Repository;

import org.apache.jackrabbit.rmi.remote.RemoteXASession;
import org.apache.jackrabbit.rmi.remote.SerializableXid;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteXASession RemoteXASession}
 * interface.
 *
 * @since 1.4
 */
public class ClientXASession extends ClientSession implements XAResource {

    /**
     * The adapted remote transaction enabled session.
     */
    private RemoteXASession remote;

    /**
     * Creates a client adapter for the given remote session which is
     * transaction enabled.
     */
    public ClientXASession(
            Repository repository, RemoteXASession remote,
            LocalAdapterFactory factory) {
        super(repository, remote, factory);
        this.remote = remote;
    }

    /**
     * Returns <code>true</code> if the given object is a local
     * adapter that refers to the same remote XA resource.
     *
     * @see <a href="http://blogs.sun.com/fkieviet/entry/j2ee_jca_resource_adapters_the">http://blogs.sun.com/fkieviet/entry/j2ee_jca_resource_adapters_the</a>
     */
    public boolean isSameRM(XAResource xares) throws XAException {
        return xares instanceof ClientXASession
            && remote == ((ClientXASession) xares).remote;
    }

    private XAException getXAException(RemoteException e) {
        XAException exception = new XAException("Remote operation failed");
        exception.initCause(e);
        return exception;
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        try {
            remote.commit(new SerializableXid(xid), onePhase);
        } catch (RemoteException e) {
            throw getXAException(e);
        }
    }

    public void end(Xid xid, int flags) throws XAException {
        try {
            remote.end(new SerializableXid(xid), flags);
        } catch (RemoteException e) {
            throw getXAException(e);
        }
    }

    public void forget(Xid xid) throws XAException {
        try {
            remote.forget(new SerializableXid(xid));
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
            return remote.prepare(new SerializableXid(xid));
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
            remote.rollback(new SerializableXid(xid));
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
            remote.start(new SerializableXid(xid), flags);
        } catch (RemoteException e) {
            throw getXAException(e);
        }
    }

}
