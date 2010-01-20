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

import javax.jcr.Session;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.jackrabbit.rmi.remote.RemoteXASession;
import org.apache.jackrabbit.rmi.remote.SerializableXid;

/**
 * Remote adapter for XA-enabled sessions.
 *
 * @since 1.4
 */
public class ServerXASession extends ServerSession implements RemoteXASession {

    /**
     * The adapted local XA resource
     */
    private final XAResource resource;

    /**
     * Creates a remote adapter for the given local, transaction enabled,
     * session.
     */
    public ServerXASession(
            Session session, XAResource resource, RemoteAdapterFactory factory)
            throws RemoteException {
        super(session, factory);
        this.resource = resource;
    }

    private static XAException getXAException(XAException e) {
        return new XAException(e.getMessage());
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        try {
            resource.commit(xid, onePhase);
        } catch (XAException e) {
            throw getXAException(e);
        }
    }

    public void end(Xid xid, int flags) throws XAException {
        try {
            resource.end(xid, flags);
        } catch (XAException e) {
            throw getXAException(e);
        }
    }

    public void forget(Xid xid) throws XAException {
        try {
            resource.forget(xid);
        } catch (XAException e) {
            throw getXAException(e);
        }
    }

    public int getTransactionTimeout() throws XAException {
        try {
            return resource.getTransactionTimeout();
        } catch (XAException e) {
            throw getXAException(e);
        }
    }

    public int prepare(Xid xid) throws XAException {
        try {
            return resource.prepare(xid);
        } catch (XAException e) {
            throw getXAException(e);
        }
    }

    public Xid[] recover(int flag) throws XAException {
        try {
            Xid[] xids = resource.recover(flag);
            for (int i = 0; i < xids.length; i++) {
                xids[i] = new SerializableXid(xids[i]);
            }
            return xids;
        } catch (XAException e) {
            throw getXAException(e);
        }
    }

    public void rollback(Xid xid) throws XAException {
        try {
            resource.rollback(xid);
        } catch (XAException e) {
            throw getXAException(e);
        }
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        try {
            return resource.setTransactionTimeout(seconds);
        } catch (XAException e) {
            throw getXAException(e);
        }
    }

    public void start(Xid xid, int flags) throws XAException {
        try {
            resource.start(xid, flags);
        } catch (XAException e) {
            throw getXAException(e);
        }
    }

}
