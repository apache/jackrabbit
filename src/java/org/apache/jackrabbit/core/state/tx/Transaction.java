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
package org.apache.jackrabbit.core.state.tx;

/**
 * Represents the transaction on behalf of the component that wants to
 * explictely demarcate transcation boundaries.
 */
public interface Transaction {

    /**
     * Known attribute name
     */
    public static final String ATTRIBUTE_TX_ID = "TX_ID";

    /**
     * Known attribute name
     */
    public static final String ATTRIBUTE_XID = "XID";

    /**
     * Prepare this transaction. Prepares all changes to items contained in the
     * transaction. After having prepared a transaction, no more changes to
     * the underlying items may occur, until either {@link #commit} or
     * {@link #rollback} has been invoked.
     *
     * @throws TransactionException if an error occurs
     */
    public void prepare() throws TransactionException;

    /**
     * Commit this transaction. Commits all changes to items contained in the
     * transaction. After having successfully committed the transaction, it
     * may no longer be used.
     *
     * @throws TransactionException if an error occurs
     */
    public void commit() throws TransactionException;

    /**
     * Rollback this transaction. Rollbacks all changes to items contained in
     * the transaction. After having successfully rolled back the transaction,
     * it may no longer be used.
     *
     * @throws TransactionException if an error occurs
     */
    public void rollback() throws TransactionException;

    /**
     * Set the only possible outcome for this transaction to rollback.
     *
     * @throws TransactionException if an error occurs
     */
    public void setRollbackOnly() throws TransactionException;

    /**
     * Set an attribute on this transaction. If the value specified is
     * <code>null</code>, it is semantically equivalent to
     * {@link #removeAttribute}.
     *
     * @param name  attribute name
     * @param value attribute value
     */
    public void setAttribute(String name, Object value);

    /**
     * Return an attribute value on this transaction.
     *
     * @param name attribute name
     * @return attribute value, <code>null</code> if no attribute with that
     *         name exists
     */
    public Object getAttribute(String name);

    /**
     * Remove an attribute on this transaction.
     *
     * @param name attribute name
     */
    public void removeAttribute(String name);

    /**
     * Add a transaction listener. This listener will be invoked when the
     * transaction is either committed or rolled back.
     *
     * @param listener listener to add
     */
    public void addListener(TransactionListener listener);

    /**
     * Remove a transaction listener previously added with {@link #addListener}
     *
     * @param listener listener to remove
     */
    public void removeListener(TransactionListener listener);

}
