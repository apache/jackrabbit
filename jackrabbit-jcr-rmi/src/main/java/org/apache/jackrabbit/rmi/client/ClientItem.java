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

import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.rmi.remote.RemoteItem;

/**
 * Local adapter for the JCR-RMI
 * {@link org.apache.jackrabbit.rmi.remote.RemoteItem RemoteItem}
 * interface. This class makes a remote item locally available using
 * the JCR {@link javax.jcr.Item Item} interface. Used mainly as the
 * base class for the
 * {@link org.apache.jackrabbit.rmi.client.ClientProperty ClientProperty}
 * and
 * {@link org.apache.jackrabbit.rmi.client.ClientNode ClientNode} adapters.
 *
 * @see javax.jcr.Item
 * @see org.apache.jackrabbit.rmi.remote.RemoteItem
 */
public class ClientItem extends ClientObject implements Item {

    /** Current session. */
    private Session session;

    /** The adapted remote item. */
    private RemoteItem remote;

    /**
     * Creates a local adapter for the given remote item.
     *
     * @param session current session
     * @param remote  remote item
     * @param factory local adapter factory
     */
    public ClientItem(Session session, RemoteItem remote,
            LocalAdapterFactory factory) {
        super(factory);
        this.session = session;
        this.remote = remote;
    }

    /**
     * Returns the current session without contacting the remote item.
     *
     * {@inheritDoc}
     */
    public Session getSession() {
        return session;
    }

    /** {@inheritDoc} */
    public String getPath() throws RepositoryException {
        try {
            return remote.getPath();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public String getName() throws RepositoryException {
        try {
            return remote.getName();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Item getAncestor(int level) throws RepositoryException {
        try {
            return getItem(getSession(), remote.getAncestor(level));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public Node getParent() throws RepositoryException {
        try {
            return getNode(getSession(), remote.getParent());
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public int getDepth() throws RepositoryException {
        try {
            return remote.getDepth();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /**
     * Returns false by default without contacting the remote item.
     * This method should be overridden by {@link Node Node} subclasses.
     *
     * {@inheritDoc}
     *
     * @return false
     */
    public boolean isNode() {
        return false;
    }

    /** {@inheritDoc} */
    public boolean isNew() {
        try {
            return remote.isNew();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /** {@inheritDoc} */
    public boolean isModified() {
        try {
            return remote.isModified();
        } catch (RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    /**
     * Checks whether this instance represents the same repository item as
     * the given other instance. A simple heuristic is used to first check
     * some generic conditions (null values, instance equality, type equality),
     * after which the <em>item paths</em> are compared to determine sameness.
     * A RuntimeException is thrown if the item paths cannot be retrieved.
     *
     * {@inheritDoc}
     *
     * @see Item#getPath()
     */
    public boolean isSame(Item item) throws RepositoryException {
        if (item == null) {
            return false;
        } else if (equals(item)) {
            return true;
        } else if (isNode() == item.isNode()) {
            return getPath().equals(item.getPath());
        } else {
            return false;
        }
    }

    /**
     * Accepts the visitor to visit this item. {@link Node Node} and
     * {@link Property Property} subclasses should override this method
     * to call the appropriate {@link ItemVisitor ItemVisitor} methods,
     * as the default implementation does nothing.
     *
     * {@inheritDoc}
     */
    public void accept(ItemVisitor visitor) throws RepositoryException {
    }

    /** {@inheritDoc} */
    public void save() throws RepositoryException {
        try {
            remote.save();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void refresh(boolean keepChanges) throws RepositoryException {
        try {
            remote.refresh(keepChanges);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    /** {@inheritDoc} */
    public void remove() throws RepositoryException {
        try {
            remote.remove();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

}
