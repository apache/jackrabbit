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
package org.apache.jackrabbit.spi.rmi.server;

import org.apache.jackrabbit.spi.rmi.remote.RemoteSessionInfo;
import org.apache.jackrabbit.spi.SessionInfo;

import java.rmi.RemoteException;

/**
 * <code>ServerSessionInfo</code> implements the server side of a
 * <code>SessionInfo</code>.
 */
class ServerSessionInfo extends ServerObject implements RemoteSessionInfo {

    /**
     * The local session info.
     */
    private final SessionInfo sessionInfo;

    /**
     * Creates a new server session info based on the given SPI session info.
     *
     * @param sessionInfo the SPI session info.
     */
    public ServerSessionInfo(SessionInfo sessionInfo) throws RemoteException {
        this.sessionInfo = sessionInfo;
    }

    /**
     * @return the underlying session info instance.
     */
    SessionInfo getSessionInfo() {
        return sessionInfo;
    }

    //------------------------------< RemoteSessionInfo >-----------------------

    public String getUserID() throws RemoteException {
        return sessionInfo.getUserID();
    }

    public String getWorkspaceName() throws RemoteException {
        return sessionInfo.getWorkspaceName();
    }

    public String[] getLockTokens() throws RemoteException {
        return sessionInfo.getLockTokens();
    }

    public void addLockToken(String lockToken) throws RemoteException {
        sessionInfo.addLockToken(lockToken);
    }

    public void removeLockToken(String lockToken) throws RemoteException {
        sessionInfo.removeLockToken(lockToken);
    }

    public String getLastEventBundleId() throws RemoteException {
        return sessionInfo.getLastEventBundleId();
    }

    public void setLastEventBundleId(String eventBundleId)
            throws RemoteException {
        sessionInfo.setLastEventBundleId(eventBundleId);
    }
}
