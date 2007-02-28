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
package org.apache.jackrabbit.spi.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * <code>RemoteSessionInfo</code>...
 */
public interface RemoteSessionInfo extends Remote {

    /**
     * Remote version of {@link org.apache.jackrabbit.spi.SessionInfo#addLockToken(String)}.
     */
    public String getUserID() throws RemoteException;

    /**
     * Remote version of {@link org.apache.jackrabbit.spi.SessionInfo#addLockToken(String)}.
     */
    public String getWorkspaceName() throws RemoteException;

    /**
     * Remote version of {@link org.apache.jackrabbit.spi.SessionInfo#addLockToken(String)}.
     */
    public String[] getLockTokens() throws RemoteException;

    /**
     * Remote version of {@link org.apache.jackrabbit.spi.SessionInfo#addLockToken(String)}.
     */
    public void addLockToken(String lockToken) throws RemoteException;

    /**
     * Remote version of {@link org.apache.jackrabbit.spi.SessionInfo#removeLockToken(String)}.
     */
    public void removeLockToken(String lockToken) throws RemoteException;

    /**
     * Remote version of {@link org.apache.jackrabbit.spi.SessionInfo#getLastEventBundleId()}.
     */
    public String getLastEventBundleId() throws RemoteException;

    /**
     * Remote version of {@link org.apache.jackrabbit.spi.SessionInfo#setLastEventBundleId(String)}.
     */
    public void setLastEventBundleId(String eventBundleId) throws RemoteException;

}
