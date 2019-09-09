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

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

/**
 * Remote version of the JCR {@link javax.jcr.Workspace Workspace} interface.
 * Used by the
 * {@link org.apache.jackrabbit.rmi.server.ServerWorkspace ServerWorkspace}
 * and
 * {@link org.apache.jackrabbit.rmi.client.ClientWorkspace ClientWorkspace}
 * adapters to provide transparent RMI access to remote workspaces.
 * <p>
 * Most of the methods in this interface are documented only with a reference
 * to a corresponding Workspace method. In these cases the remote object
 * will simply forward the method call to the underlying Workspace instance.
 * Complex return values like namespace registries and other objects are
 * returned as remote references to the corresponding remote interface. Simple
 * return values and possible exceptions are copied over the network
 * to the client. RMI errors are signaled with RemoteExceptions.
 *
 * @see javax.jcr.Workspace
 * @see org.apache.jackrabbit.rmi.client.ClientWorkspace
 * @see org.apache.jackrabbit.rmi.server.ServerWorkspace
 */
public interface RemoteWorkspace extends Remote {

    /**
     * Remote version of the
     * {@link javax.jcr.Workspace#getName() Workspace.getName()} method.
     *
     * @return workspace name
     * @throws RemoteException on RMI errors
     */
    String getName() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Workspace#copy(String,String) Workspace.copy(String,String)}
     * method.
     *
     * @param from source path
     * @param to destination path
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void copy(String from, String to)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Workspace#copy(String,String,String) Workspace.copy(String,String,String)}
     * method.
     *
     * @param workspace source workspace
     * @param from source path
     * @param to destination path
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void copy(String workspace, String from, String to)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Workspace#clone(String,String,String,boolean) Workspace.clone(String,String,String,boolean)}
     * method.
     *
     * @param workspace source workspace
     * @param from source path
     * @param to destination path
     * @param removeExisting flag to remove existing items
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void clone(String workspace, String from, String to, boolean removeExisting)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Workspace#move(String,String) Workspace.move(String,String)}
     * method.
     *
     * @param from source path
     * @param to destination path
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void move(String from, String to)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Workspace#getNodeTypeManager() Workspace.getNodeTypeManager()}
     * method.
     *
     * @return node type manager
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNodeTypeManager getNodeTypeManager()
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Workspace#getNamespaceRegistry() Workspace.getNamespaceRegistry()}
     * method.
     *
     * @return namespace registry
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNamespaceRegistry getNamespaceRegistry()
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Workspace#getQueryManager() Workspace.getQueryManager()}
     * method.
     *
     * @return query manager
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteQueryManager getQueryManager()
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Workspace#getObservationManager() Workspace.getObservationManager()}
     * method.
     *
     * @return observation manager
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteObservationManager getObservationManager()
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Workspace#getAccessibleWorkspaceNames() Workspace.getAccessibleWorkspaceNames()}
     * method.
     *
     * @return accessible workspace names
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    String[] getAccessibleWorkspaceNames()
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Workspace#importXML(String,java.io.InputStream,int) Workspace.importXML(String,InputStream,int)}
     * method.
     *
     * @param path node path
     * @param xml imported XML document
     * @param uuidBehaviour uuid behaviour flag
     * @throws IOException on IO errors
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void importXML(String path, byte[] xml, int uuidBehaviour)
            throws IOException, RepositoryException, RemoteException;

    void createWorkspace(String name, String source)
            throws RepositoryException, RemoteException;

    void deleteWorkspace(String name)
            throws RepositoryException, RemoteException;

    RemoteLockManager getLockManager()
        throws RepositoryException, RemoteException;

    RemoteVersionManager getVersionManager()
        throws RepositoryException, RemoteException;

}
