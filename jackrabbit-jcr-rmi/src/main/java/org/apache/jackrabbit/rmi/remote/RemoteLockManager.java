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

import javax.jcr.RepositoryException;

public interface RemoteLockManager extends Remote {

    String[] getLockTokens() throws RepositoryException, RemoteException;

    void addLockToken(String lockToken)
        throws RepositoryException, RemoteException;

    void removeLockToken(String lockToken)
        throws RepositoryException, RemoteException;

    RemoteLock getLock(String absPath)
        throws RepositoryException, RemoteException;

    boolean holdsLock(String absPath)
        throws RepositoryException, RemoteException;

    boolean isLocked(String absPath)
        throws RepositoryException, RemoteException;

    RemoteLock lock(
        String absPath, boolean isDeep, boolean isSessionScoped,
        long timeoutHint, String ownerInfo)
        throws RepositoryException, RemoteException;

    void unlock(String absPath) throws RepositoryException, RemoteException;

}
