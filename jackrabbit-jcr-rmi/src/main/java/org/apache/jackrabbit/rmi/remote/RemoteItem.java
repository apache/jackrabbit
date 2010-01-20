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

/**
 * Remote version of the JCR {@link javax.jcr.Item Item} interface.
 * Used by the  {@link org.apache.jackrabbit.rmi.server.ServerItem ServerItem}
 * and {@link org.apache.jackrabbit.rmi.client.ClientItem ClientItem}
 * adapter base classes to provide transparent RMI access to remote items.
 * <p>
 * The methods in this interface are documented only with a reference
 * to a corresponding Item method. The remote object will simply forward
 * the method call to the underlying Item instance. Argument and return
 * values, as well as possible exceptions, are copied over the network.
 * Complex return values (Items and Nodes) are returned as remote references
 * to the corresponding remote interfaces. RMI errors are signaled with
 * RemoteExceptions.
 *
 * @see javax.jcr.Item
 * @see org.apache.jackrabbit.rmi.client.ClientItem
 * @see org.apache.jackrabbit.rmi.server.ServerItem
 */
public interface RemoteItem extends Remote {

    /**
     * Remote version of the
     * {@link javax.jcr.Item#getPath() Item.getPath()} method.
     *
     * @return item path
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    String getPath() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Item#getName() Item.getName()} method.
     *
     * @return item name
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    String getName() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Item#getAncestor(int) Item.getAncestor(int)} method.
     *
     * @param level ancestor level
     * @return ancestor item
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteItem getAncestor(int level)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Item#getParent() Item.getParent()} method.
     *
     * @return parent node
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    RemoteNode getParent() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Item#getDepth() Item.getDepth()} method.
     *
     * @return item depth
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    int getDepth() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Item#isNew() Item.isNew()} method.
     *
     * @return <code>true</code> if the item is new,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean isNew() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Item#isModified() Item.isModified()} method.
     *
     * @return <code>true</code> if the item is modified,
     *         <code>false</code> otherwise
     * @throws RemoteException on RMI errors
     */
    boolean isModified() throws RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Item#save() Item.save()} method.
     *
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void save() throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Item#refresh(boolean) Item.refresh(boolean)} method.
     *
     * @param keepChanges flag to keep transient changes
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void refresh(boolean keepChanges)
            throws RepositoryException, RemoteException;

    /**
     * Remote version of the
     * {@link javax.jcr.Item#remove() Item.remove()} method.
     *
     * @throws RepositoryException on repository errors
     * @throws RemoteException on RMI errors
     */
    void remove() throws RepositoryException, RemoteException;

}
