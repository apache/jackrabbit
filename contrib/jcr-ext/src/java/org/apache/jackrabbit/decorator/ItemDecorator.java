/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.decorator;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * TODO
 */
public class ItemDecorator implements Item {
    
    private DecoratorFactory factory;
    
    private Session session;
    
    private Item item;

    /**
     * Returns the decorated session through which this item decorator
     * was acquired.
     * 
     * @return decorated session
     */
    public Session getSession() throws RepositoryException {
        return session;
    }

    /** {@inheritDoc} */
    public String getPath() throws RepositoryException {
        return item.getPath();
    }

    /** {@inheritDoc} */
    public String getName() throws RepositoryException {
        return item.getName();
    }

    /** {@inheritDoc} */
    public Item getAncestor(int depth) throws ItemNotFoundException,
            AccessDeniedException, RepositoryException {
        Item ancestor = item.getAncestor(depth);
        return factory.getItemDecorator(session, ancestor);
    }

    /** {@inheritDoc} */
    public Node getParent() throws ItemNotFoundException,
            AccessDeniedException, RepositoryException {
        Node parent = item.getParent();
        return factory.getNodeDecorator(session, parent);
    }

    /** {@inheritDoc} */
    public int getDepth() throws RepositoryException {
        return item.getDepth();
    }

    /** {@inheritDoc} */
    public boolean isNode() {
        return item.isNode();
    }

    /** {@inheritDoc} */
    public boolean isNew() {
        return item.isNew();
    }

    /** {@inheritDoc} */
    public boolean isModified() {
        return item.isModified();
    }

    /** {@inheritDoc} */
    public boolean isSame(Item otherItem) {
        // TODO Auto-generated method stub
        return false;
    }

    /** {@inheritDoc} */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        item.accept(visitor);
    }

    /** {@inheritDoc} */
    public void save() throws AccessDeniedException,
            ConstraintViolationException, InvalidItemStateException,
            ReferentialIntegrityException, VersionException, LockException,
            RepositoryException {
        item.save();
    }

    /** {@inheritDoc} */
    public void refresh(boolean keepChanges) throws InvalidItemStateException,
            RepositoryException {
        item.refresh(keepChanges);
    }

    /** {@inheritDoc} */
    public void remove() throws VersionException, LockException,
            RepositoryException {
        item.remove();
    }

}
