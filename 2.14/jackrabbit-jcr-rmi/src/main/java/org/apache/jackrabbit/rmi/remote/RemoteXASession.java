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
package org.apache.jackrabbit.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

/**
 * Remote version of the {@link org.apache.jackrabbit.api.XASession}
 * interface.
 */
public interface RemoteXASession extends RemoteSession, Remote {

    /**
     * Remote version of the
     * {@link javax.transaction.xa.XAResource#commit(Xid, boolean)} method.
     */
    void commit(Xid xid, boolean onePhase) throws XAException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.transaction.xa.XAResource#end(Xid, int)} method.
     */
    void end(Xid xid, int flags) throws XAException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.transaction.xa.XAResource#forget(Xid)} method.
     */
    void forget(Xid xid) throws XAException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.transaction.xa.XAResource#getTransactionTimeout()} method.
     */
    int getTransactionTimeout() throws XAException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.transaction.xa.XAResource#prepare(Xid)} method.
     */
    int prepare(Xid xid) throws XAException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.transaction.xa.XAResource#recover(int)} method.
     */
    Xid[] recover(int flag) throws XAException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.transaction.xa.XAResource#rollback(Xid)} method.
     */
    void rollback(Xid xid) throws XAException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.transaction.xa.XAResource#setTransactionTimeout(int)} method.
     */
    boolean setTransactionTimeout(int seconds)
        throws XAException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.transaction.xa.XAResource#start(Xid, int)} method.
     */
    void start(Xid xid, int flags) throws XAException, RemoteException;

}
