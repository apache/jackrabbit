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
package org.apache.jackrabbit.rmi.server;

import java.rmi.RemoteException;
import java.util.Calendar;

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.jackrabbit.rmi.remote.RemoteVersion;
import org.apache.jackrabbit.rmi.remote.RemoteVersionHistory;

/**
 * Remote adapter for the JCR {@link javax.jcr.version.Version Version} interface.
 * This class makes a local version available as an RMI service using
 * the {@link org.apache.jackrabbit.rmi.remote.RemoteVersion RemoteVersion}
 * interface.
 *
 * @see javax.jcr.version.Version
 * @see org.apache.jackrabbit.rmi.remote.RemoteVersion
 */
public class ServerVersion extends ServerNode implements RemoteVersion {

    /** The adapted local version. */
    private Version version;

    /**
     * Creates a remote adapter for the given local version.
     *
     * @param version local version
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerVersion(Version version, RemoteAdapterFactory factory)
            throws RemoteException {
        super(version, factory);
        this.version = version;
    }

    /**
     * Utility method for creating an array of remote references for
     * local versions. The remote references are created using the
     * remote adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty array.
     *
     * @param versions local version array
     * @return remote version array
     * @throws RemoteException on RMI errors
     */
    private RemoteVersion[] getRemoteVersionArray(Version[] versions)
            throws RemoteException {
        if (versions != null) {
            RemoteVersion[] remotes = new RemoteVersion[versions.length];
            for (int i = 0; i < remotes.length; i++) {
                remotes[i] = getFactory().getRemoteVersion(versions[i]);
            }
            return remotes;
        } else {
            return new RemoteVersion[0]; // for safety
        }
    }

//  This is only available after 0.16.2
//    /** {@inheritDoc} */
//    public RemoteVersionHistory getContainingHistory() throws RepositoryException {
//        try {
//            return getFactory().getRemoteVersionHistory(version.getContainingHistory());
//        } catch (RepositoryException ex) {
//            throw getRepositoryException(ex);
//        }
//    }

    /** {@inheritDoc} */
    public Calendar getCreated() throws RepositoryException {
        try {
            return version.getCreated();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteVersion[] getSuccessors() throws RepositoryException, RemoteException {
        try {
            return getRemoteVersionArray(version.getSuccessors());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteVersion[] getPredecessors() throws RepositoryException, RemoteException {
        try {
            return getRemoteVersionArray(version.getPredecessors());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteVersionHistory getContainingHistory() throws RepositoryException, RemoteException {
        try {
            return getFactory().getRemoteVersionHistory(version.getContainingHistory());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
}
