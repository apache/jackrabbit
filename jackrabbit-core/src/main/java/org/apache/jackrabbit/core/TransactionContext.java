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

import javax.transaction.xa.XAException;
import javax.transaction.Status;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the transaction on behalf of the component that wants to
 * explictely demarcate transcation boundaries. After having been prepared,
 * starts a thread that rolls back the transaction if some time passes without
 * any further action. This will guarantee that global objects locked by one
 * of the resources' {@link InternalXAResource#prepare} method, are eventually
 * unlocked.
 */
public class TransactionContext implements Runnable {

    /**
     * Logger instance.
     */
    private static final Logger log = LoggerFactory.getLogger(TransactionContext.class);

    /**
     * Transactional resources.
     */
    private final InternalXAResource[] resources;

    /**
     * Timeout, in seconds.
     */
    private final int timeout;

    /**
     * Transaction attributes.
     */
    private final Map attributes = new HashMap();

    /**
     * Status.
     */
    private int status;

    /**
     * Create a new instance of this class.
     * @param resources transactional resources
     * @param timeout timeout, in seconds
     */
    public TransactionContext(InternalXAResource[] resources, int timeout) {
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
        status = Status.STATUS_PREPARING;
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
        status = Status.STATUS_PREPARED;

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

        // start rollback thread in case the commit is never issued
        new Thread(this, "RollbackThread").start();
    }

    /**
     * Commit the transaction identified by this context. Commits changes on
     * all resources. If some resource reports an error on commit,
     * automatically rollback changes on all other resources. Throw
     * exception at the end if some commit failed.
     * @throws XAException if an error occurs
     */
    public synchronized void commit() throws XAException {
        if (status == Status.STATUS_ROLLEDBACK) {
            throw new XAException(XAException.XA_RBTIMEOUT);
        }
        status = Status.STATUS_COMMITTING;
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
        status = Status.STATUS_COMMITTED;

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
        if (status == Status.STATUS_ROLLEDBACK) {
            throw new XAException(XAException.XA_RBTIMEOUT);
        }
        status = Status.STATUS_ROLLING_BACK;
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
        status = Status.STATUS_ROLLEDBACK;

        if (errors != 0) {
            throw new XAException(XAException.XA_RBOTHER);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Waits for the amount of time specified as transaction timeout. After
     * this time has elapsed, rolls back the transaction if still prepared
     * and marks the transaction rolled back.
     */
    public void run() {
        try {
            Thread.sleep(timeout * 1000);
        } catch (InterruptedException e) {
            /* ignore */
        }

        synchronized (this) {
            if (status == Status.STATUS_PREPARED) {
                try {
                    rollback();
                } catch (XAException e) {
                    /* ignore */
                }
                log.warn("Transaction rolled back because timeout expired.");
            }
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
}
