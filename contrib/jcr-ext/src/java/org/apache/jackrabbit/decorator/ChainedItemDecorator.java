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
public class ChainedItemDecorator implements Item {

    private ItemDecorator decorator;

    public ChainedItemDecorator(ItemDecorator decorator) {
        this.decorator = decorator;
    }

    /** {@inheritDoc} */
    public Session getSession() throws RepositoryException {
        return decorator.getSession();
    }

    /** {@inheritDoc} */
    public String getPath() throws RepositoryException {
        return decorator.getPath();
    }

    /** {@inheritDoc} */
    public String getName() throws RepositoryException {
        return decorator.getName();
    }

    /** {@inheritDoc} */
    public Item getAncestor(int depth) throws ItemNotFoundException,
            AccessDeniedException, RepositoryException {
        return decorator.getAncestor(depth);
    }

    /** {@inheritDoc} */
    public Node getParent() throws ItemNotFoundException,
            AccessDeniedException, RepositoryException {
        return decorator.getParent();
    }

    /** {@inheritDoc} */
    public int getDepth() throws RepositoryException {
        return decorator.getDepth();
    }

    /** {@inheritDoc} */
    public boolean isNode() {
        return decorator.isNode();
    }

    /** {@inheritDoc} */
    public boolean isNew() {
        return decorator.isNew();
    }

    /** {@inheritDoc} */
    public boolean isModified() {
        return decorator.isModified();
    }

    /** {@inheritDoc} */
    public boolean isSame(Item otherItem) {
        return decorator.isSame(otherItem);
    }

    /** {@inheritDoc} */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        decorator.accept(visitor);
    }

    /** {@inheritDoc} */
    public void save() throws AccessDeniedException,
            ConstraintViolationException, InvalidItemStateException,
            ReferentialIntegrityException, VersionException, LockException,
            RepositoryException {
        decorator.save();
    }

    /** {@inheritDoc} */
    public void refresh(boolean keepChanges) throws InvalidItemStateException,
            RepositoryException {
        decorator.refresh(keepChanges);
    }

    /** {@inheritDoc} */
    public void remove() throws VersionException, LockException,
            RepositoryException {
        decorator.remove();
    }

}
