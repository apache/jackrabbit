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
package org.apache.jackrabbit.rmi.client;

import java.rmi.RemoteException;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.rmi.remote.RemoteVersion;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteVersion RemoteVersion}
 * interface. This class makes a remote version locally available using
 * the JCR {@link javax.jcr.version.Version Version} interface.
 *
 * @see javax.jcr.version.Version
 * @see org.apache.jackrabbit.rmi.remote.RemoteVersion
 */
public class ClientVersion extends ClientNode implements Version {

    /** The adapted remote version. */
    private RemoteVersion remote;

    /**
     * Creates a local adapter for the given remote version.
     *
     * @param session current session
     * @param remote  remote version
     * @param factory local adapter factory
     */
    public ClientVersion(Session session, RemoteVersion remote,
        LocalAdapterFactory factory) {
        super(session, remote, factory);
        this.remote = remote;
    }

    /**
     * Utility method for creating a version array for an array
     * of remote versions. The versions in the returned array
     * are created using the local adapter factory.
     * <p>
     * A <code>null</code> input is treated as an empty array.
     *
     * @param remotes remote versions
     * @return local version array
     */
    private Version[] getVersionArray(RemoteVersion[] remotes) {
        if (remotes != null) {
            Version[] versions = new Version[remotes.length];
            for (int i = 0; i < remotes.length; i++) {
                versions[i] = getFactory().getVersion(getSession(), remotes[i]);
            }
            return versions;
        } else {
            return new Version[0]; // for safety
        }
    }


    /** {@inheritDoc} */
    public Calendar getCreated() throws RepositoryException {
        try {
            return remote.getCreated();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Version[] getSuccessors() throws RepositoryException {
        try {
            return getVersionArray(remote.getSuccessors());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Version[] getPredecessors() throws RepositoryException {
        try {
            return getVersionArray(remote.getPredecessors());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public VersionHistory getContainingHistory() throws RepositoryException {
        try {
            return getFactory().getVersionHistory(getSession(), remote.getContainingHistory());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
   }

    /** {@inheritDoc} */
    public Node getFrozenNode() throws RepositoryException {
        try {
            return getFactory().getNode(getSession(), remote.getFrozenNode());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Version getLinearPredecessor() throws RepositoryException {
        try {
            RemoteVersion linearPredecessor = remote.getLinearPredecessor();
            if (linearPredecessor == null) {
                return null;
            } else {
                return getFactory().getVersion(getSession(), linearPredecessor);
            }
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Version getLinearSuccessor() throws RepositoryException {
        try {
            RemoteVersion linearSuccessor = remote.getLinearSuccessor();
            if (linearSuccessor == null) {
                return null;
            } else {
                return getFactory().getVersion(getSession(), linearSuccessor);
            }
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
}
