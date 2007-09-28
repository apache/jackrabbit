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
package org.apache.jackrabbit.spi.rmi.client;

import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.rmi.remote.RemoteSessionInfo;

import java.rmi.RemoteException;

/**
 * <code>ClientSessionInfo</code> wraps a remote session info and exposes it
 * as a SPI {@link org.apache.jackrabbit.spi.SessionInfo}.
 */
class ClientSessionInfo implements SessionInfo {

    /**
     * The underlying remote session info.
     */
    private final RemoteSessionInfo remoteSessionInfo;

    /**
     * Creates a new session info based on a remote session info.
     *
     * @param remoteSessionInfo the remote session info.
     */
    public ClientSessionInfo(RemoteSessionInfo remoteSessionInfo) {
        this.remoteSessionInfo = remoteSessionInfo;
    }

    /**
     * @return the underlying remote session info.
     */
    RemoteSessionInfo getRemoteSessionInfo() {
        return remoteSessionInfo;
    }

    /**
     * {@inheritDoc}
     * @throws RemoteRuntimeException if an RMI error occurs.
     */
    public String getUserID() {
        try {
            return remoteSessionInfo.getUserID();
        } catch (RemoteException e) {
            throw new RemoteRuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     * @throws RemoteRuntimeException if an RMI error occurs.
     */
    public String getWorkspaceName() {
        try {
            return remoteSessionInfo.getWorkspaceName();
        } catch (RemoteException e) {
            throw new RemoteRuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     * @throws RemoteRuntimeException if an RMI error occurs.
     */
    public String[] getLockTokens() {
        try {
            return remoteSessionInfo.getLockTokens();
        } catch (RemoteException e) {
            throw new RemoteRuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     * @throws RemoteRuntimeException if an RMI error occurs.
     */
    public void addLockToken(String lockToken) {
        try {
            remoteSessionInfo.addLockToken(lockToken);
        } catch (RemoteException e) {
            throw new RemoteRuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     * @throws RemoteRuntimeException if an RMI error occurs.
     */
    public void removeLockToken(String lockToken) {
        try {
            remoteSessionInfo.removeLockToken(lockToken);
        } catch (RemoteException e) {
            throw new RemoteRuntimeException(e);
        }
    }
}
