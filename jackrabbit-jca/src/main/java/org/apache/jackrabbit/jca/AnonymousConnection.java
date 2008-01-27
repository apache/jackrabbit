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
package org.apache.jackrabbit.jca;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import javax.security.auth.Subject;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.ResourceException;
import java.io.PrintWriter;

/**
 * Implements a <code>ManagedConnection</code> for an anonymous user,
 * where no <code>ConnectionRequestInfo</code> has been specified.
 *
 * @see JCAManagedConnectionFactory#createManagedConnection
 */
public class AnonymousConnection implements ManagedConnection, XAResource {

    /**
     * Default transaction timeout, in seconds.
     */
    private static final int DEFAULT_TX_TIMEOUT = 5;

    /**
     * Timeout explicitely set.
     */
    private int timeout;

    /**
     * Log writer.
     */
    private PrintWriter logWriter;

    //------------------------------------------------------- ManagedConnection

    /**
     * {@inheritDoc}
     */
    public XAResource getXAResource() throws ResourceException {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void cleanup() throws ResourceException {
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() throws ResourceException {
    }

    /**
     * {@inheritDoc}
     */
    public void setLogWriter(PrintWriter logWriter) throws ResourceException {
        this.logWriter = logWriter;
    }

    /**
     * {@inheritDoc}
     */
    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    /**
     * {@inheritDoc}
     */
    public void addConnectionEventListener(ConnectionEventListener listener) {
        // ignored
    }

    /**
     * {@inheritDoc}
     */
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        // ignored
    }

    //--------------------------------------------------------- not implemented

    public Object getConnection(Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {

        String msg = "No connection allowed for anonymous user.";
        throw new UnsupportedOperationException(msg);
    }

    public void associateConnection(Object o) throws ResourceException {
        String msg = "Associating a connection not supported.";
        throw new UnsupportedOperationException(msg);
    }

    public LocalTransaction getLocalTransaction() throws ResourceException {
        String msg = "Local transactions not supported.";
        throw new UnsupportedOperationException(msg);
    }

    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        String msg = "Retrieving meta data not supported.";
        throw new UnsupportedOperationException(msg);
    }

    //-------------------------------------------------------------- XAResource

    /**
     * {@inheritDoc}
     */
    public Xid[] recover(int flags) throws XAException {
        return new Xid[0];
    }

    /**
     * {@inheritDoc}
     */
    public int getTransactionTimeout() throws XAException {
        return timeout == 0 ? DEFAULT_TX_TIMEOUT : timeout;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setTransactionTimeout(int timeout) throws XAException {
        this.timeout = timeout;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSameRM(XAResource xares) throws XAException {
        return xares instanceof AnonymousConnection;
    }

    //--------------------------------------------------------- not implemented

    public void start(Xid xid, int flags) throws XAException {
        throw new XAException(XAException.XAER_RMFAIL);
    }

    public void end(Xid xid, int flags) throws XAException {
        throw new XAException(XAException.XAER_NOTA);
    }

    public void forget(Xid xid) throws XAException {
        throw new XAException(XAException.XAER_NOTA);
    }

    public int prepare(Xid xid) throws XAException {
        throw new XAException(XAException.XAER_NOTA);
    }

    public void commit(Xid xid, boolean arg1) throws XAException {
        throw new XAException(XAException.XAER_NOTA);
    }

    public void rollback(Xid xid) throws XAException {
        throw new XAException(XAException.XAER_NOTA);
    }
}
