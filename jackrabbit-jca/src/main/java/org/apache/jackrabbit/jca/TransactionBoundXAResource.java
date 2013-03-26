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

public class TransactionBoundXAResource implements XAResource {

    private XAResource xaResource;

    private JCAManagedConnection connection;

    private boolean ending;

    public TransactionBoundXAResource(JCAManagedConnection connection,
            XAResource xaResource) {
        super();
        this.xaResource = xaResource;
        this.connection = connection;
    }
    
    /**
     * There is a one-to-one Relation between this TransactionBoundXAResource
     * and the JCAManagedConnection so the used XAResource must be in sync when it is changed in the
     * JCAManagedConnection.
     * @param res
     */
    protected void rebind(XAResource res) {
        this.xaResource = res;
    }

    public void commit(Xid arg0, boolean arg1) throws XAException {
        xaResource.commit(arg0, arg1);
    }

    public void end(Xid arg0, int arg1) throws XAException {
        if (!ending) {
            this.ending = true;
            try {
                xaResource.end(arg0, arg1);
            } finally {
            	if(arg1 != XAResource.TMSUSPEND) {
            		this.connection.closeHandles();
            	}
            }
            // reuse the XAResource
            this.ending = false;
        }
    }

    public void forget(Xid arg0) throws XAException {
        xaResource.forget(arg0);
    }

    public int getTransactionTimeout() throws XAException {
        return xaResource.getTransactionTimeout();
    }

    public boolean isSameRM(XAResource arg0) throws XAException {
        return xaResource.isSameRM(arg0);
    }

    public int prepare(Xid arg0) throws XAException {
        return xaResource.prepare(arg0);
    }

    public Xid[] recover(int arg0) throws XAException {
        return xaResource.recover(arg0);
    }

    public void rollback(Xid arg0) throws XAException {
        xaResource.rollback(arg0);
    }

    public boolean setTransactionTimeout(int arg0) throws XAException {
        return xaResource.setTransactionTimeout(arg0);
    }

    public void start(Xid arg0, int arg1) throws XAException {
        xaResource.start(arg0, arg1);
    }

}
