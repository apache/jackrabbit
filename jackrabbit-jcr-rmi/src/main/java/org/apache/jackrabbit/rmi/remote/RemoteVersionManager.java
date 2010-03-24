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

public interface RemoteVersionManager extends Remote {

//    void cancelMerge(String absPath, Version version)
//        throws RepositoryException, RemoteException;

    RemoteVersion checkin(String absPath)
        throws RepositoryException, RemoteException;

    void checkout(String absPath) throws RepositoryException, RemoteException;

    RemoteVersion checkpoint(String absPath)
        throws RepositoryException, RemoteException;

    RemoteNode createActivity(String title)
        throws RepositoryException, RemoteException;

    RemoteNode createConfiguration(String absPath)
        throws RepositoryException, RemoteException;

//    void doneMerge(String absPath, Version version)
//        throws RepositoryException, RemoteException;

    RemoteNode getActivity() throws RepositoryException, RemoteException;

    RemoteVersion getBaseVersion(String absPath)
        throws RepositoryException, RemoteException;

    RemoteVersionHistory getVersionHistory(String absPath)
        throws RepositoryException, RemoteException;

    boolean isCheckedOut(String absPath)
        throws RepositoryException, RemoteException;

//    NodeIterator merge(Node activityNode)
//        throws RepositoryException, RemoteException;

    RemoteIterator merge(
        String absPath, String srcWorkspace, boolean bestEffort)
        throws RepositoryException, RemoteException;

    RemoteIterator merge(
        String absPath, String srcWorkspace, boolean bestEffort,
        boolean isShallow)
        throws RepositoryException, RemoteException;

//    void removeActivity(Node activityNode)
//        throws RepositoryException, RemoteException;

//    void restore(Version[] versions, boolean removeExisting)
//        throws RepositoryException, RemoteException;

//    void restore(Version version, boolean removeExisting)
//        throws RepositoryException, RemoteException;

    void restore(
        String absPath, String versionName, boolean removeExisting)
        throws RepositoryException, RemoteException;

//    void restore(String absPath, Version version, boolean removeExisting)
//        throws RepositoryException, RemoteException;

    void restoreByLabel(
        String absPath, String versionLabel, boolean removeExisting)
        throws RepositoryException, RemoteException;

//    Node setActivity(Node activity) throws RepositoryException, RemoteException;

}
