/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the transaction on behalf of the component that wants to
 * explictely demarcate transcation boundaries.
 */
public class TransactionContext {

    /**
     * Transaction listeners
     */
    private final List listeners = new ArrayList();

    /**
     * Transaction attributes
     */
    private final Map attributes = new HashMap();

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
     * Add a transaction listener. This listener will be invoked when the
     * transaction is either committed or rolled back.
     *
     * @param listener listener to add
     */
    public void addListener(TransactionListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a transaction listener previously added with {@link #addListener}
     *
     * @param listener listener to remove
     */
    public void removeListener(TransactionListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Notify listeners that transaction was committed. Since there is at most
     * one commit and one rollback event to be reported, the listeners can
     * safely be cleared at the same time.
     */
    void notifyCommitted() {
        TransactionListener[] al;

        synchronized (listeners) {
            al = new TransactionListener[listeners.size()];
            listeners.toArray(al);
            listeners.clear();
        }

        for (int i = 0; i < al.length; i++) {
            al[i].transactionCommitted(this);
        }
    }

    /**
     * Notify listeners that transaction was rolled back. Since there is at most
     * one commit and one rollback event to be reported, the listeners can
     * safely be cleared at the same time.
     */
    void notifyRolledBack() {
        TransactionListener[] al;

        synchronized (listeners) {
            al = new TransactionListener[listeners.size()];
            listeners.toArray(al);
            listeners.clear();
        }

        for (int i = 0; i < al.length; i++) {
            al[i].transactionRolledBack(this);
        }
    }
}
