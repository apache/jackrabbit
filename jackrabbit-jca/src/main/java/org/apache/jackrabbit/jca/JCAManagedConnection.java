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

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class implements the managed connection for
 * this resource adapter.
 */
public class JCAManagedConnection
        implements ManagedConnection, ManagedConnectionMetaData {

    /**
     * The LocalTransactionAdapter wraps the internal XAResource and uses the XA Method's to
     * fulfill the LocalTransaction calls.
     */
    private static class LocalTransactionAdapter implements javax.resource.spi.LocalTransaction {

        /**
         * Internal {@link Xid} implementation.
         */
        class XidImpl implements Xid {
        
            private final byte[] globalTxId;
        
            public XidImpl(byte[] globalTxId) {
                this.globalTxId = globalTxId;
            }
        
            /**
             * {@inheritDoc}
             */
            public int getFormatId() {
                return 0;
            }
        
            /**
             * {@inheritDoc}
             */
            public byte[] getBranchQualifier() {
                return new byte[0];
            }
        
            /**
             * {@inheritDoc}
             */
            public byte[] getGlobalTransactionId() {
                return globalTxId;
            }
        }

        /**
         * Global static counter for the internal Xid's
         */
        private static AtomicInteger globalCounter = new AtomicInteger();

        private XAResource resource;
        private Xid xid;

        public LocalTransactionAdapter(XAResource xaResource) {
            this.resource = xaResource;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void begin() throws ResourceException {
            try {
                this.xid = new XidImpl(intToByteArray(globalCounter.getAndIncrement()));
                resource.start(xid, XAResource.TMNOFLAGS);
            } catch (XAException e) {
                throw new ResourceException(e.getMessage());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void commit() throws ResourceException {
            try {
                resource.end(xid, XAResource.TMSUCCESS);
                resource.commit(xid, true);
            } catch (XAException e) {
                throw new ResourceException(e.getMessage());
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void rollback() throws ResourceException {
            try {
                resource.end(xid, XAResource.TMFAIL);
                resource.rollback(xid);
            } catch (XAException e) {
                throw new ResourceException(e.getMessage());
            }
        }
        
        /**
         * Converts the given int (global transaction id) to a byte[]
         * 
         * @param value
         * @return byte[]
         */
        private static byte[] intToByteArray(int value) {
            byte[] b = new byte[4];
            for (int i = 0; i < 4; i++) {
                int offset = (b.length - 1 - i) * 8;
                b[i] = (byte) ((value >>> offset) & 0xFF);
            }
            return b;
        }
    }

    /**
     * Managed connection factory.
     */
    private final JCAManagedConnectionFactory mcf;

    /**
     * Connection request info.
     */
    private final JCAConnectionRequestInfo cri;

    /**
     * Session instance.
     */
    private Session session;

    /**
     * XAResource instance.
     */
    private XAResource xaResource;

    /**
     * Listeners.
     */
    private final LinkedList<ConnectionEventListener> listeners;

    /**
     * Handles.
     */
    private final LinkedList<JCASessionHandle> handles;

    /**
     * Log writer.
     */
    private PrintWriter logWriter;

    private LocalTransactionAdapter localTransactionAdapter;

    /**
     * Construct the managed connection.
     */
    public JCAManagedConnection(
            JCAManagedConnectionFactory mcf, JCAConnectionRequestInfo cri)
            throws ResourceException {
        this.mcf = mcf;
        this.cri = cri;
        this.session = openSession();
        this.listeners = new LinkedList<ConnectionEventListener>();
        this.handles = new LinkedList<JCASessionHandle>();
        if (this.mcf.getBindSessionToTransaction().booleanValue()) {
            this.xaResource = new TransactionBoundXAResource(this, (XAResource) session);
        } else {
            this.xaResource = (XAResource) session;
        }
        this.localTransactionAdapter = new LocalTransactionAdapter(xaResource);
    }

    /**
     * Create a new session.
     */
    private Session openSession() throws ResourceException {
        try {
            Session session = mcf.getRepository().login(
                    cri.getCredentials(), cri.getWorkspace());
            log("Created session (" + session + ")");
            return session;
        } catch (RepositoryException e) {
            log("Failed to create session", e);
            ResourceException exception = new ResourceException(
                    "Failed to create session: " + e.getMessage());
            exception.initCause(e);
            throw exception;
        }
    }

    /**
     * Return the managed connection factory.
     */
    public JCAManagedConnectionFactory getManagedConnectionFactory() {
        return mcf;
    }

    /**
     * Return the connection request info.
     */
    public JCAConnectionRequestInfo getConnectionRequestInfo() {
        return cri;
    }

    /**
     * Get the log writer.
     */
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    /**
     * Set the log writer.
     */
    public void setLogWriter(PrintWriter logWriter)
            throws ResourceException {
        this.logWriter = logWriter;
    }

    /**
     * Creates a new connection handle for the underlying physical
     * connection represented by the ManagedConnection instance.
     */
    public Object getConnection(Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        JCASessionHandle handle = new JCASessionHandle(this);
        addHandle(handle);
        return handle;
    }

    /**
     * Destroys the physical connection to the underlying resource manager.
     */
    public void destroy()
            throws ResourceException {
        this.session.logout();
        this.handles.clear();
    }

    /**
     * Application server calls this method to force any cleanup on
     * the ManagedConnection instance.
     */
    public void cleanup()
            throws ResourceException {
        synchronized (handles) {
            this.session.logout();
            this.session = openSession();
            this.handles.clear();
            if (this.mcf.getBindSessionToTransaction().booleanValue() && (this.xaResource instanceof TransactionBoundXAResource)) {
            	((TransactionBoundXAResource) this.xaResource).rebind((XAResource) session);
            } else {
            	this.xaResource = (XAResource) session;
            }
        }
    }

    /**
     * Used by the container to change the association of an
     * application-level connection handle with a ManagedConneciton instance.
     */
    public void associateConnection(Object connection)
            throws ResourceException {
        JCASessionHandle handle = (JCASessionHandle) connection;
        if (handle.getManagedConnection() != this) {
            handle.getManagedConnection().removeHandle(handle);
            handle.setManagedConnection(this);
            addHandle(handle);
        }
    }

    /**
     * Returns an javax.transaction.xa.XAresource instance.
     */
    public XAResource getXAResource()
            throws ResourceException {
        return this.xaResource;
    }

    /**
     * Returns an javax.resource.spi.LocalTransaction instance.
     */
    public LocalTransaction getLocalTransaction()
            throws ResourceException {
        return localTransactionAdapter;
    }

    /**
     * Gets the metadata information for this connection's underlying
     * EIS resource manager instance.
     */
    public ManagedConnectionMetaData getMetaData()
            throws ResourceException {
        return this;
    }

    /**
     * Close the handle.
     */
    public void closeHandle(JCASessionHandle handle) {
        if (handle != null) {
            removeHandle(handle);
            sendClosedEvent(handle);
        }
    }

    /**
     * Return the session.
     */
    public Session getSession(JCASessionHandle handle) {
        synchronized (handles) {
            if ((handles.size() > 0) && (handles.get(0) == handle)) {
                return session;
            } else {
                throw new java.lang.IllegalStateException("Inactive logical session handle called");
            }
        }
    }

    private String getDescriptor(String key) throws ResourceException {
        try {
            return mcf.getRepository().getDescriptor(key);
        } catch (RepositoryException e) {
            log("Failed to access the repository", e);
            ResourceException exception = new ResourceException(
                    "Failed to access the repository: " + e.getMessage());
            exception.initCause(e);
            throw exception;
        }
    }

    /**
     * Return the product name.
     */
    public String getEISProductName() throws ResourceException {
        return getDescriptor(Repository.REP_NAME_DESC);
    }

    /**
     * Return the product version.
     */
    public String getEISProductVersion() throws ResourceException {
        return getDescriptor(Repository.REP_VERSION_DESC);
    }

    /**
     * Return number of max connections.
     */
    public int getMaxConnections()
            throws ResourceException {
        return Integer.MAX_VALUE;
    }

    /**
     * Return the user name.
     */
    public String getUserName()
            throws ResourceException {
        return session.getUserID();
    }

    /**
     * Log a message.
     */
    public void log(String message) {
        log(message, null);
    }

    /**
     * Log a message.
     */
    public void log(String message, Throwable exception) {
        if (logWriter != null) {
            logWriter.println(message);

            if (exception != null) {
                exception.printStackTrace(logWriter);
            }
        }
    }

    /**
     * Adds a listener.
     */
    public void addConnectionEventListener(ConnectionEventListener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
    }

    /**
     * Remove a listener.
     */
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Send event.
     */
    private void sendEvent(ConnectionEvent event) {
        synchronized (listeners) {
            for (Iterator<ConnectionEventListener> i = listeners.iterator(); i.hasNext();) {
                ConnectionEventListener listener = i.next();

                switch (event.getId()) {
                    case ConnectionEvent.CONNECTION_CLOSED:
                        listener.connectionClosed(event);
                        break;
                    case ConnectionEvent.CONNECTION_ERROR_OCCURRED:
                        listener.connectionErrorOccurred(event);
                        break;
                    case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED:
                        listener.localTransactionCommitted(event);
                        break;
                    case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK:
                        listener.localTransactionRolledback(event);
                        break;
                    case ConnectionEvent.LOCAL_TRANSACTION_STARTED:
                        listener.localTransactionStarted(event);
                        break;
                    default:
                        // Unknown event, skip
                }
            }
        }
    }

    /**
     * Send event.
     */
    private void sendEvent(int type, Object handle, Exception cause) {
        ConnectionEvent event = new ConnectionEvent(this, type, cause);
        if (handle != null) {
            event.setConnectionHandle(handle);
        }

        sendEvent(event);
    }

    /**
     * Send connection closed event.
     */
    private void sendClosedEvent(JCASessionHandle handle) {
        sendEvent(ConnectionEvent.CONNECTION_CLOSED, handle, null);
    }

    /**
     * Send connection error event.
     */
    public void sendrrorEvent(JCASessionHandle handle, Exception cause) {
        sendEvent(ConnectionEvent.CONNECTION_ERROR_OCCURRED, handle, cause);
    }

    /**
     * Send transaction committed event.
     */
    public void sendTxCommittedEvent(JCASessionHandle handle) {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_COMMITTED, handle, null);
    }

    /**
     * Send transaction rolledback event.
     */
    public void sendTxRolledbackEvent(JCASessionHandle handle) {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK, handle, null);
    }

    /**
     * Send transaction started event.
     */
    public void sendTxStartedEvent(JCASessionHandle handle) {
        sendEvent(ConnectionEvent.LOCAL_TRANSACTION_STARTED, handle, null);
    }

    /**
     * Add a session handle.
     */
    private void addHandle(JCASessionHandle handle) {
        synchronized (handles) {
            handles.addFirst(handle);
        }
    }

    /**
     * Remove a session handle.
     */
    private void removeHandle(JCASessionHandle handle) {
        synchronized (handles) {
            handles.remove(handle);
        }
    }

    /**
     * Release handles.
     */
    void closeHandles() {
        synchronized (handles) {
            JCASessionHandle[] handlesArray = new JCASessionHandle[handles
                    .size()];
            handles.toArray(handlesArray);
            for (int i = 0; i < handlesArray.length; i++) {
                this.closeHandle(handlesArray[i]);
            }
        }
    }
}
