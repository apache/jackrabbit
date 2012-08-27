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

import java.rmi.RemoteException;
import java.util.Calendar;

import javax.jcr.RepositoryException;

/**
 * Remote version of the JCR {@link javax.jcr.version.Version Version} interface.
 * Used by the {@link org.apache.jackrabbit.rmi.server.ServerVersion ServerVersion}
 * and {@link org.apache.jackrabbit.rmi.client.ClientVersion ClientVersion}
 * adapters to provide transparent RMI access to remote versions.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding Version method. The remote object will simply forward
 * the method call to the underlying Version instance. Argument and return
 * values, as well as possible exceptions, are copied over the network.
 * Complex return values (like Versions) are returned as remote
 * references to the corresponding remote interfaces. Iterator values
 * are transmitted as object arrays. RMI errors are signaled with
 * RemoteExceptions.
 *
 * @see javax.jcr.version.Version
 * @see org.apache.jackrabbit.rmi.client.ClientVersion
 * @see org.apache.jackrabbit.rmi.server.ServerVersion
 */
public interface RemoteVersion extends RemoteNode {

    /**
     * Remote version of the
     * {@link javax.jcr.version.Version#getContainingHistory() Version.getContainingHistory()} method.
     *
     * @return a <code>RemoteVersionHistory</code> object.
     *
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
     RemoteVersionHistory getContainingHistory() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.Version#getCreated() Version.getCreated()} method.
     *
     * @return a <code>Calendar</code> object.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    Calendar getCreated() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.Version#getLinearSuccessor() Version.getLinearSuccessor()} method.
     *
     * @return a <code>RemoteVersion</code> or <code>null</code> if no linear
     *         successor exists.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     * @see RemoteVersionHistory#getAllLinearVersions
     */
    RemoteVersion getLinearSuccessor() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.Version#getSuccessors() Version.getSuccessors()} method.
     *
     * @return a <code>RemoteVersion</code> array.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteVersion[] getSuccessors() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.Version#getLinearPredecessor() Version.getLinearPredecessor()} method.
     *
     * @return a <code>RemoteVersion</code> or <code>null</code> if no linear
     *         predecessor exists.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     * @see RemoteVersionHistory#getAllLinearVersions
     */
    RemoteVersion getLinearPredecessor() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.Version#getPredecessors() Version.getPredecessors()} method.
     *
     * @return a <code>RemoteVersion</code> array.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteVersion[] getPredecessors() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.Version#getFrozenNode() Version.getFrozenNode()} method.
     *
     * @return a <code>RemoteNode</code> object.
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNode getFrozenNode() throws RepositoryException, RemoteException;
}
