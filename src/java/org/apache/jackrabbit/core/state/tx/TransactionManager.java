/*
 * Copyright 2004 The Apache Software Foundation.
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

import javax.transaction.xa.Xid;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the transaction manager to be used by a component that will
 * itself control the boundaries of a transaction.<p>
 * When starting transactions for operations that directly use
 * item states and where a <code>PersistentItemStateProvider</code> is
 * used, this is how to allocate a transaction and associate it with
 * the provider:
 * <pre>
 * // given provider
 * PersistentItemStateProvider provider;
 * // given transaction manager
 * TransactionManager txManager;
 * <p/>
 * // create a store to use when committing changes
 * TransactionalStore store = new DefaultTransactionalStore(provider);
 * // allocate transaction
 * Transaction tx = txManager.beginTransaction(store);
 * // create a new provider to use
 * PersistentItemStateProvider txProvider;
 * txProvider = new TransactionalItemStateManager(tx, store);
 * <p/>
 * // now operate on the new provider that is transactional
 * ...
 * <p/>
 * // commit transaction, which will automatically save all changes
 * tx.commit();
 * </pre>
 */
public class TransactionManager implements TransactionListener {

    /**
     * Directory for transactions
     */
    private final File directory;

    /**
     * Global transactions
     */
    private final Map txGlobal = new HashMap();

    /**
     * Transaction ID counter
     */
    private int txIdCounter;

    /**
     * Create a new instance of this class. Takes the parent directory
     * of all transactions as parameter.
     */
    public TransactionManager(File directory) throws IOException {
        this.directory = directory;

        directory.mkdirs();
        if (!directory.isDirectory()) {
            throw new IOException("Unable to create transaction manager directory");
        }
    }

    /**
     * Begin a new transaction, given a transactional store.
     *
     * @param store store to use when committing changes
     * @return transaction that may be prepared, committed and rolled back
     * @throws TransactionException if the transaction can not be created
     */
    public Transaction beginTransaction(TransactionalStore store)
            throws TransactionException {

        String txId = nextTxId();

        Transaction tx = new TransactionImpl(txIdToDirectory(txId), store);
        tx.setAttribute(Transaction.ATTRIBUTE_TX_ID, txId);
        tx.addListener(this);

        return tx;
    }

    /**
     * Begin a new global transaction. The object returned is at the same time
     * a {@link TransactionalItemStateManager}
     *
     * @return transaction
     */
    public Transaction beginTransaction(Xid xid, TransactionalStore store)
            throws TransactionException {

        String txId = nextTxId();

        Transaction tx = new TransactionImpl(txIdToDirectory(txId), store);
        tx.setAttribute(Transaction.ATTRIBUTE_TX_ID, txId);
        tx.setAttribute(Transaction.ATTRIBUTE_XID, xid);
        tx.addListener(this);
        txGlobal.put(xid, tx);

        return tx;
    }

    /**
     * Return a global transaction known to this transaction manager. The object
     * returned is at the same time a {@link TransactionalItemStateManager}
     *
     * @param xid XID
     * @return transaction, <code>null</code> if the transaction is unknown
     */
    public Transaction getTransaction(Xid xid) {
        return (Transaction) txGlobal.get(xid);
    }

    //---------------------------------------------------< TransactionListener >

    /**
     * @see TransactionListener#transactionCommitted
     *      <p/>
     *      Remove this transaction if this is a global transaction.
     */
    public void transactionCommitted(Transaction tx) {
        Xid xid = (Xid) tx.getAttribute(Transaction.ATTRIBUTE_XID);
        if (xid != null) {
            txGlobal.remove(xid);
        }
    }

    /**
     * @see TransactionListener#transactionCommitted
     *      <p/>
     *      Remove this transaction if this is a global transaction.
     */
    public void transactionRolledBack(Transaction tx) {
        Xid xid = (Xid) tx.getAttribute(Transaction.ATTRIBUTE_XID);
        if (xid != null) {
            txGlobal.remove(xid);
        }
    }

    /**
     * Return the next available transaction Id
     */
    private String nextTxId() {
        int txId = ++txIdCounter;

        char[] rep = new char[8];
        for (int i = 0; i < rep.length; i++) {
            int b = txId & 0x0F;
            rep[rep.length - i - 1] = Integer.toHexString(b).charAt(0);
            txId >>>= 4;
        }
        return new String(rep);
    }

    /**
     * Return the directory used for some transaction.
     *
     * @param txId transaction id
     * @return directory to use
     */
    private File txIdToDirectory(String txId) {
        return new File(directory, txId);
    }
}
