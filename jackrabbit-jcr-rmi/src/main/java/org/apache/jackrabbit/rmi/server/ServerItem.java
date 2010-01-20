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

import javax.jcr.Item;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.remote.RemoteItem;
import org.apache.jackrabbit.rmi.remote.RemoteNode;

/**
 * Remote adapter for the JCR {@link javax.jcr.Item Item} interface.
 * This class makes a local item available as an RMI service using
 * the {@link org.apache.jackrabbit.rmi.remote.RemoteItem RemoteItem}
 * interface. Used mainly as the base class for the
 * {@link org.apache.jackrabbit.rmi.server.ServerProperty ServerProperty}
 * and {@link org.apache.jackrabbit.rmi.server.ServerNode ServerNode}
 * adapters.
 *
 * @see javax.jcr.Item
 * @see org.apache.jackrabbit.rmi.remote.RemoteItem
 */
public class ServerItem extends ServerObject implements RemoteItem {

    /** The adapted local item. */
    private Item item;

    /**
     * Creates a remote adapter for the given local item.
     *
     * @param item    local item to be adapted
     * @param factory remote adapter factory
     * @throws RemoteException on RMI errors
     */
    public ServerItem(Item item, RemoteAdapterFactory factory)
            throws RemoteException {
        super(factory);
        this.item = item;
    }

    /** {@inheritDoc} */
    public String getPath() throws RepositoryException, RemoteException {
        try {
            return item.getPath();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getName() throws RepositoryException, RemoteException {
        try {
            return item.getName();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void save() throws RepositoryException, RemoteException {
        try {
            item.save();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteItem getAncestor(int level)
            throws RepositoryException, RemoteException {
        try {
            return getRemoteItem(item.getAncestor(level));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public int getDepth() throws RepositoryException, RemoteException {
        try {
            return item.getDepth();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public RemoteNode getParent() throws RepositoryException, RemoteException {
        try {
            return getRemoteNode(item.getParent());
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isModified() throws RemoteException {
        return item.isModified();
    }

    /** {@inheritDoc} */
    public boolean isNew() throws RemoteException {
        return item.isNew();
    }

    /** {@inheritDoc} */
    public void refresh(boolean keepChanges)
            throws RepositoryException, RemoteException {
        try {
            item.refresh(keepChanges);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void remove() throws RepositoryException, RemoteException {
        try {
            item.remove();
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

}
