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
package org.apache.jackrabbit.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.util.Timer;

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the transaction on behalf of the component that wants to
 * explicitly demarcate transaction boundaries. After having been prepared,
 * schedules a task that rolls back the transaction if some time passes without
 * any further action. This will guarantee that global objects locked by one
 * of the resources' {@link InternalXAResource#prepare} method, are eventually
 * unlocked.
 */
public class TransactionContext extends Timer.Task {

    /**
     * Logger instance.
     */
    private static final Logger log = LoggerFactory.getLogger(TransactionContext.class);

    private static final int STATUS_PREPARING = 1;
    private static final int STATUS_PREPARED = 2;
    private static final int STATUS_COMMITTING = 3;
    private static final int STATUS_COMMITTED = 4;
    private static final int STATUS_ROLLING_BACK = 5;
    private static final int STATUS_ROLLED_BACK = 6;

    /**
     * The per thread associated Xid
     */
    private static final ThreadLocal<Xid> CURRENT_XID = new ThreadLocal<Xid>();

    /**
     * Create a global timer for all transaction contexts.
     */
    private static final Timer TIMER = new Timer(true);

    /**
     * Transactional resources.
     */
    private final InternalXAResource[] resources;

    /**
     * Timeout, in seconds.
     */
    private final int timeout;

    /**
    * The Xid
    */
   private final Xid xid;

    /**
     * Transaction attributes.
     */
    private final Map<String, Object> attributes = new HashMap<String, Object>();

    /**
     * Status.
     */
    private int status;

    /**
     * Flag indicating whether the association is currently suspended.
     */
    private boolean suspended;

    /**
     * Create a new instance of this class.
     * @param xid associated xid
     * @param resources transactional resources
     * @param timeout timeout, in seconds
     */
    public TransactionContext(Xid xid, InternalXAResource[] resources, int timeout) {
        this.xid = xid;
        this.resources = resources;
        this.timeout = timeout;
    }

    /**
     * Set an attribute on this transaction. If the value specified is
     * <code>null</code>, it is semantically equivalent to
     * {@link #removeAttribute}.
     *
     * @param name  attribute name
     * @param value attribute value
     */
    public void setAttribute(String name, Object value) {
        if (value == null) {
            removeAttribute(name);
        }
        attributes.put(name, value);
    }

    /**
     * Return an attribute value on this transaction.
     *
     * @param name attribute name
     * @return attribute value, <code>null</code> if no attribute with that
     *         name exists
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Remove an attribute on this transaction.
     *
     * @param name attribute name
     */
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    /**
     * Prepare the transaction identified by this context. Prepares changes on
     * all resources. If some resource reports an error on prepare,
     * automatically rollback changes on all other resources. Throw exception
     * at the end if errors were found.
     * @throws XAException if an error occurs
     */
    public synchronized void prepare() throws XAException {
        bindCurrentXid();
        status = STATUS_PREPARING;
        beforeOperation();

        TransactionException txe = null;
        for (int i = 0; i < resources.length; i++) {
            try {
                resources[i].prepare(this);
            } catch (TransactionException e) {
                txe = e;
                break;
            }
        }

        afterOperation();
        status = STATUS_PREPARED;

        if (txe != null) {
            // force immediate rollback on error.
            try {
                rollback();
            } catch (XAException e) {
                /* ignore */
            }
            XAException e = new XAException(XAException.XA_RBOTHER);
            e.initCause(txe);
            throw e;
        }

        // start rollback task in case the commit is never issued
        TIMER.schedule(this, timeout * 1000, Integer.MAX_VALUE);
    }

    /**
     * Commit the transaction identified by this context. Commits changes on
     * all resources. If some resource reports an error on commit,
     * automatically rollback changes on all other resources. Throw
     * exception at the end if some commit failed.
     * @throws XAException if an error occurs
     */
    public synchronized void commit() throws XAException {
        if (status == STATUS_ROLLED_BACK) {
            throw new XAException(XAException.XA_RBTIMEOUT);
        }
        bindCurrentXid();
        status = STATUS_COMMITTING;
        beforeOperation();

        TransactionException txe = null;
        for (int i = 0; i < resources.length; i++) {
            InternalXAResource resource = resources[i];
            if (txe != null) {
                try {
                    resource.rollback(this);
                } catch (TransactionException e) {
                    log.warn("Unable to rollback changes on " + resource, e);
                }
            } else {
                try {
                    resource.commit(this);
                } catch (TransactionException e) {
                    txe = e;
                }
            }
        }
        afterOperation();
        status = STATUS_COMMITTED;

        // cancel the rollback task
        cancel();
        cleanCurrentXid();

        if (txe != null) {
            XAException e = new XAException(XAException.XA_RBOTHER);
            e.initCause(txe);
            throw e;
        }
    }

    /**
     * Rollback the transaction identified by this context. Rolls back changes
     * on all resources. Throws exception at the end if errors were found.
     * @throws XAException if an error occurs
     */
    public synchronized void rollback() throws XAException {
        if (status == STATUS_ROLLED_BACK) {
            throw new XAException(XAException.XA_RBTIMEOUT);
        }
        bindCurrentXid();
        status = STATUS_ROLLING_BACK;
        beforeOperation();

        int errors = 0;
        for (int i = 0; i < resources.length; i++) {
            InternalXAResource resource = resources[i];
            try {
                resource.rollback(this);
            } catch (TransactionException e) {
                log.warn("Unable to rollback changes on " + resource, e);
                errors++;
            }
        }
        afterOperation();
        status = STATUS_ROLLED_BACK;

        // cancel the rollback task
        cancel();
        cleanCurrentXid();

        if (errors != 0) {
            throw new XAException(XAException.XA_RBOTHER);
        }
    }

    /**
     * Rolls back the transaction if still prepared and marks the transaction
     * rolled back.
     */
    public void run() {
        synchronized (this) {
            if (status == STATUS_PREPARED) {
                try {
                    rollback();
                } catch (XAException e) {
                    /* ignore */
                }
                log.warn("Transaction rolled back because timeout expired.");
            }
            // cancel the rollback task
            cancel();
        }
    }

    /**
     * Invoke all of the registered resources' {@link InternalXAResource#beforeOperation}
     * methods.
     */
    private void beforeOperation() {
        for (int i = 0; i < resources.length; i++) {
            resources[i].beforeOperation(this);
        }
    }

    /**
     * Invoke all of the registered resources' {@link InternalXAResource#afterOperation}
     * methods.
     */
    private void afterOperation() {
        for (int i = 0; i < resources.length; i++) {
            resources[i].afterOperation(this);
        }
    }

    /**
     * Return a flag indicating whether the association is suspended.
     *
     * @return <code>true</code> if the association is suspended;
     *         <code>false</code> otherwise
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * Set a flag indicating whether the association is suspended.
     *
     * @param suspended flag whether that the association is suspended.
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    /**
     * Helper Method to bind the {@link Xid} associated with this {@link TransactionContext}
     * to the {@link #CURRENT_XID} ThreadLocal.
     */
    private void bindCurrentXid() {
        CURRENT_XID.set(xid);
    }

    /**
     * Helper Method to clean the {@link Xid} associated with this {@link TransactionContext}
     * from the {@link #CURRENT_XID} ThreadLocal.
     */
    private void cleanCurrentXid() {
        CURRENT_XID.set(null);
    }

    /**
     * Returns the {@link Xid} bind to the {@link #CURRENT_XID} ThreadLocal
     * @return current Xid or null
     */
    public static Xid getCurrentXid() {
        return CURRENT_XID.get();
    }

    /**
     * Helper Method to check if the given {@link Xid} has the same globalTransactionId
     * as the current {@link Xid} bind to the {@link #CURRENT_XID} ThreadLocal
     * @param xid Xid to check
     * @param fallback if either the given {@link Xid} or the current {@link Xid} is null we can not check if they
     *        are same, the fallback value will be returned
     * @return true if the same otherwise false
     */
    public static boolean isCurrentXid(Xid xid, boolean fallback) {
        Xid currentXid = CURRENT_XID.get();
        return fallback ? true : (currentXid == null || xid == null) ? fallback : Arrays.equals(xid.getGlobalTransactionId(), currentXid.getGlobalTransactionId());
    }
}
