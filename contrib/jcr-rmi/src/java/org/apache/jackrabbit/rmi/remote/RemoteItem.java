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

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;

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
 * Compex return values (Items and Nodes) are returned as remote references
 * to the corresponding remote interfaces. RMI errors are signalled with
 * RemoteExceptions.
 *
 * @author Jukka Zitting
 * @see javax.jcr.Item
 * @see org.apache.jackrabbit.rmi.client.ClientItem
 * @see org.apache.jackrabbit.rmi.server.ServerItem
 */
public interface RemoteItem extends Remote {

    /**
     * @see javax.jcr.Item#getPath()
     * @throws RemoteException on RMI exceptions
     */
    public String getPath() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Item#getName()
     * @throws RemoteException on RMI exceptions
     */
    public String getName() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Item#getAncestor(int)
     * @throws RemoteException on RMI exceptions
     */
    public RemoteItem getAncestor(int level) throws ItemNotFoundException,
        AccessDeniedException, RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Item#getParent()
     * @throws RemoteException on RMI exceptions
     */
    public RemoteNode getParent() throws ItemNotFoundException,
        AccessDeniedException, RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Item#getDepth()
     * @throws RemoteException on RMI exceptions
     */
    public int getDepth() throws RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Item#isNew()
     * @throws RemoteException on RMI exceptions
     */
    public boolean isNew() throws RemoteException;

    /**
     * @see javax.jcr.Item#isModified()
     * @throws RemoteException on RMI exceptions
     */
    public boolean isModified() throws RemoteException;

    /**
     * @see javax.jcr.Item#save()
     * @throws RemoteException on RMI exceptions
     */
    public void save() throws AccessDeniedException, LockException,
            ConstraintViolationException, InvalidItemStateException,
            ReferentialIntegrityException, RepositoryException,
            RemoteException;

    /**
     * @see javax.jcr.Item#refresh(boolean)
     * @throws RemoteException on RMI exceptions
     */
    public void refresh(boolean keepChanges) throws InvalidItemStateException,
            RepositoryException, RemoteException;

    /**
     * @see javax.jcr.Item#remove()
     * @throws RemoteException on RMI exceptions
     */
    public void remove() throws RepositoryException, RemoteException;

}
