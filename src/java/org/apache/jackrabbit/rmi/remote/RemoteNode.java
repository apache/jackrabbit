/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import javax.jcr.Value;


/**
 * Remote version of the JCR {@link javax.jcr.Node Node} interface.
 * Used by the {@link org.apache.jackrabbit.rmi.server.ServerNode ServerNode}
 * and {@link org.apache.jackrabbit.rmi.client.ClientNode ClientNode}
 * adapters to provide transparent RMI access to remote nodes.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding Node method. The remote object will simply forward
 * the method call to the underlying Node instance. Argument and return
 * values, as well as possible exceptions, are copied over the network.
 * Compex return values (like Nodes and Properties) are returned as remote
 * references to the corresponding remote interfaces. Iterator values
 * are transmitted as object arrays. RMI errors are signalled with
 * RemoteExceptions.
 * <p>
 * Note that only two generic setProperty methods are included in this
 * interface. Clients should implement the type-specific setProperty
 * methods by wrapping the argument values into generic Value objects
 * and calling the generic setProperty methods. Note also that the
 * Value objects must be serializable and implemented using classes
 * available on both the client and server side. The
 * {@link org.apache.jackrabbit.rmi.remote.SerialValue SerialValue}
 * decorator utility provides a convenient way to satisfy these
 * requirements.
 *
 * @author Jukka Zitting
 * @see javax.jcr.Node
 * @see org.apache.jackrabbit.rmi.client.ClientNode
 * @see org.apache.jackrabbit.rmi.server.ServerNode
 */
public interface RemoteNode extends RemoteItem {

    /**
     * @see javax.jcr.Node#addNode(java.lang.String)
     * @throws RemoteException on RMI exceptions
     */
    RemoteNode addNode(String path) throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#addNode(java.lang.String, java.lang.String)
     * @throws RemoteException on RMI exceptions
     */
    RemoteNode addNode(String path, String type)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#getProperty(java.lang.String)
     * @throws RemoteException on RMI exceptions
     */
    RemoteProperty getProperty(String path)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#getProperties()
     * @throws RemoteException on RMI exceptions
     */
    RemoteProperty[] getProperties()
            throws RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Node#getProperties(java.lang.String)
     * @throws RemoteException on RMI exceptions
     */
    RemoteProperty[] getProperties(String pattern)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#getPrimaryItem()
     * @throws RemoteException on RMI exceptions
     */
    RemoteItem getPrimaryItem() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#getUUID()
     * @throws RemoteException on RMI exceptions
     */
    String getUUID() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#getReferences()
     * @throws RemoteException on RMI exceptions
     */
    RemoteProperty[] getReferences()
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#getNodes()
     * @throws RemoteException on RMI exceptions
     */
    RemoteNode[] getNodes() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#getNodes(java.lang.String)
     * @throws RemoteException on RMI exceptions
     */
    RemoteNode[] getNodes(String pattern)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#hasNode(java.lang.String)
     * @throws RemoteException on RMI exceptions
     */
    boolean hasNode(String path) throws RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Node#hasProperty(java.lang.String)
     * @throws RemoteException on RMI exceptions
     */
    boolean hasProperty(String path)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#hasNodes()
     * @throws RemoteException on RMI exceptions
     */
    boolean hasNodes() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#hasProperties()
     * @throws RemoteException on RMI exceptions
     */
    boolean hasProperties() throws RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Node#getPrimaryNodeType()
     * @throws RemoteException on RMI exceptions
     */
    RemoteNodeType getPrimaryNodeType()
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#getMixinNodeTypes()
     * @throws RemoteException on RMI exceptions
     */
    RemoteNodeType[] getMixinNodeTypes()
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#isNodeType(java.lang.String)
     * @throws RemoteException on RMI exceptions
     */
    boolean isNodeType(String type) throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#getNode(java.lang.String)
     * @throws RemoteException on RMI exceptions
     */
    RemoteNode getNode(String path) throws RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Node#orderBefore(java.lang.String, java.lang.String)
     * @throws RemoteException on RMI exceptions
     */
    void orderBefore(String src, String dst)
            throws RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value)
     * @throws RemoteException on RMI exceptions
     */
    RemoteProperty setProperty(String name, Value value)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value[])
     * @throws RemoteException on RMI exceptions
     */
    RemoteProperty setProperty(String name, Value[] value)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#addMixin(java.lang.String)
     * @throws RemoteException on RMI exceptions
     */
    void addMixin(String name) throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#removeMixin(java.lang.String)
     * @throws RemoteException on RMI exceptions
     */
    void removeMixin(String name) throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#canAddMixin(java.lang.String)
     * @throws RemoteException on RMI exceptions
     */
    boolean canAddMixin(String name)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#getDefinition()
     * @throws RemoteException on RMI exceptions
     */
    RemoteNodeDef getDefinition() throws RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Node#checkout()
     * @throws RemoteException on RMI exceptions
     */
    void checkout() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#update(java.lang.String)
     * @throws RemoteException on RMI exceptions
     */
    void update(String workspace) throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#merge(java.lang.String, boolean)
     * @throws RemoteException on RMI exceptions
     */
    void merge(String workspace, boolean bestEffort)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#getCorrespondingNodePath(java.lang.String)
     * @throws RemoteException on RMI exceptions
     */
    String getCorrespondingNodePath(String workspace)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#getIndex()
     * @throws RemoteException on RMI exceptions
     */
    int getIndex() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#restore(java.lang.String, boolean)
     * @throws RemoteException on RMI exceptions
     */
    void restore(String version, boolean removeExisting)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#restoreByLabel(java.lang.String, boolean)
     * @throws RemoteException on RMI exceptions
     */
    void restoreByLabel(String label, boolean removeExisting)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#unlock()
     * @throws RemoteException on RMI exceptions
     */
    void unlock() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#holdsLock()
     * @throws RemoteException on RMI exceptions
     */
    boolean holdsLock() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#isLocked()
     * @throws RemoteException on RMI exceptions
     */
    boolean isLocked() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#isCheckedOut()
     * @throws RemoteException on RMI exceptions
     */
    boolean isCheckedOut() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value[], int)
     * @throws RemoteException on RMI exceptions
     */
    RemoteProperty setProperty(String name, Value[] values, int type)
            throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Node#lock(boolean, boolean)
     * @throws RemoteException on RMI exceptions
     */
    RemoteLock lock(boolean isDeep, boolean isSessionScoped)
            throws RepositoryException, RemoteException;
    
    /**
     * @see javax.jcr.Node#getLock()
     * @throws RemoteException on RMI exceptions
     */
    RemoteLock getLock() throws RepositoryException, RemoteException;

}
