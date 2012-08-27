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

import javax.jcr.RepositoryException;

/**
 * Remote version of the JC
 * {@link javax.jcr.version.VersionHistory VersionHistory} interface. Used by
 * the
 * {@link org.apache.jackrabbit.rmi.server.ServerVersionHistory ServerVersionHistory}
 * and
 * {@link org.apache.jackrabbit.rmi.client.ClientVersionHistory ClientVersionHistory}
 * adapters to provide transparent RMI access to remote version histories.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding VersionHistory method. The remote object will simply
 * forward the method call to the underlying VersionHistory instance. Argument
 * and return values, as well as possible exceptions, are copied over the
 * network. Complex return values (like Versions) are returned as remote
 * references to the corresponding remote interfaces. Iterator values
 * are transmitted as object arrays. RMI errors are signaled with
 * RemoteExceptions.
 *
 * @see javax.jcr.version.Version
 * @see org.apache.jackrabbit.rmi.client.ClientVersionHistory
 * @see org.apache.jackrabbit.rmi.server.ServerVersionHistory
 */
public interface RemoteVersionHistory extends RemoteNode {

    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#getVersionableUUID()}  VersionHistory.getVersionableUUID()}
     * method.
     *
     * @return the uuid of the versionable node
     * @throws RepositoryException if an error occurs.
     * @throws RemoteException on RMI errors
     * @deprecated As of JCR 2.0, {@link #getVersionableIdentifier} should be
     *             used instead.
     */
    String getVersionableUUID() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#getVersionableIdentifier()}  VersionHistory.getVersionableIdentifier()}
     * method.
     *
     * @return the identifier of the versionable node
     * @throws RepositoryException if an error occurs.
     * @throws RemoteException on RMI errors
     */
    String getVersionableIdentifier() throws RepositoryException, RemoteException;
	
    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#getRootVersion() VersionHistory.getRootVersion()}
     * method.
     *
     * @return a <code>Version</code> object.
     * @throws RepositoryException if an error occurs.
     * @throws RemoteException on RMI errors
     */
    RemoteVersion getRootVersion() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#getAllLinearVersions() VersionHistory.getAllLinearVersions()}
     * method.
     *
     * @return linear remote versions
     * @throws RepositoryException if an error occurs.
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getAllLinearVersions() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#getAllVersions() VersionHistory.getAllVersions()}
     * method.
     *
     * @return remote versions
     * @throws RepositoryException if an error occurs.
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getAllVersions() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#getAllLinearFrozenNodes() VersionHistory.getAllLinearFrozenNodes()}
     * method.
     *
     * @return linear remote frozen nodes
     * @throws RepositoryException if an error occurs.
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getAllLinearFrozenNodes() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#getAllFrozenNodes() VersionHistory.getAllFrozenNodes()}
     * method.
     *
     * @return remote frozen nodes
     * @throws RepositoryException if an error occurs.
     * @throws RemoteException on RMI errors
     */
    RemoteIterator getAllFrozenNodes() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#getVersion(String) VersionHistory.getVersion(String)}
     * method.
     *
     * @param versionName a version name
     * @return a <code>Version</code> object.
     * @throws RepositoryException if an error occurs.
     * @throws RemoteException on RMI errors
     */
    RemoteVersion getVersion(String versionName)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#getVersionByLabel(String) VersionHistory.getVersionByLabel(String)}
     * method.
     *
     * @param label a version label
     * @return a <code>Version</code> object.
     * @throws RepositoryException if an error occurs.
     * @throws RemoteException on RMI errors
     */
    RemoteVersion getVersionByLabel(String label)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#addVersionLabel(String, String, boolean)
     * VersionHistory.addVersionLabel(String, String, boolean)}
     * method.
     *
     * @param versionName the name of the version to which the label is to be added.
     * @param label the label to be added.
     * @param moveLabel if <code>true</code>, then if <code>label</code> is already assigned to a version in
     * this version history, it is moved to the new version specified; if <code>false</code>, then attempting
     * to assign an already used label will throw a <code>VersionException</code>.
     *
     * @throws RepositoryException if another error occurs.
     * @throws RemoteException on RMI errors
     */
    void addVersionLabel(String versionName, String label, boolean moveLabel)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#removeVersionLabel(String) VersionHistory.removeVersionLabel(String)}
     * method.
     *
     * @param label a version label
     * @throws RepositoryException if another error occurs.
     * @throws RemoteException on RMI errors
     */
    void removeVersionLabel(String label)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#hasVersionLabel(String) VersionHistory.hasVersionLabel(String)}
     * method.
     *
     * @param label a version label
     * @return a <code>boolean</code>
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    boolean hasVersionLabel(String label) throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#hasVersionLabel(javax.jcr.version.Version, String) hasVersionLabel(Version, String)}
     * method.
     *
     * @param versionUUID The UUID of the version whose labels are to be returned.
     * @param label a version label
     * @return a <code>boolean</code>.
     * @throws RepositoryException if another error occurs.
     * @throws RemoteException on RMI errors
     */
    boolean hasVersionLabel(String versionUUID, String label)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#getVersionLabels() VersionHistory.getVersionLabels()}
     * method.
     *
     * @return a <code>String</code> array containing all the labels of the version history
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    String[] getVersionLabels() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#getVersionLabels(javax.jcr.version.Version) VersionHistory.getVersionLabels(Version)}
     * method.
     *
     * @param versionUUID The UUID of the version whose labels are to be returned.
     * @return a <code>String</code> array containing all the labels of the given version
     * @throws RepositoryException if another error occurs.
     * @throws RemoteException on RMI errors
     */
    String[] getVersionLabels(String versionUUID)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.version.VersionHistory#removeVersion(String) VersionHistory.removeVersion(String)}
     * method.
     *
     * @param versionName the name of a version in this version history.
     * @throws RepositoryException if another error occurs.
     * @throws RemoteException on RMI errors
     */
    void removeVersion(String versionName)
            throws RepositoryException, RemoteException;

}
