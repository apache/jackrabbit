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

import org.apache.jackrabbit.core.state.TransactionContext;
import org.apache.jackrabbit.core.state.TransactionException;

/**
 * Listener on a transaction. Will receive notifications about commit
 * and rollback actions.
 *
 * @see org.apache.jackrabbit.core.state.TransactionContext
 */
public interface TransactionListener {

    /**
     * Transaction was committed
     *
     * @param tx transaction that was committed
     */
    public void transactionCommitted(TransactionContext tx)
            throws TransactionException;

    /**
     * Transaction was rolled back
     *
     * @param tx transaction that was rolled back
     */
    public void transactionRolledBack(TransactionContext tx)
            throws TransactionException;
}
