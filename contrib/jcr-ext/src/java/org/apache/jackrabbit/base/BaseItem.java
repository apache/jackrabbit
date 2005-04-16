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
package org.apache.jackrabbit.base;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 * TODO
 */
public class BaseItem implements Item {
    
    private Item item;

    protected BaseItem() {
        this.item = null;
    }

    protected BaseItem(Item item) {
        this.item = item;
    }

    protected void setProxyItem(Item item) {
        this.item = item;
    }

    /** {@inheritDoc} */
    public String getPath() throws RepositoryException {
        if (item != null) {
            return item.getPath();
        } else {
            try {
                Node parent = getParent();
                String path = parent.getPath();
                if (path.equals("/")) {
                    return path + getName();
                } else {
                    return path + "/" + getName();
                }
            } catch (ItemNotFoundException e) {
                return "/";
            }
        }
    }

    /** {@inheritDoc} */
    public String getName() throws RepositoryException {
        if (item != null) {
            return item.getName();
        } else {
            throw new UnsupportedRepositoryOperationException();
        }
    }

    /** {@inheritDoc} */
    public Item getAncestor(int depth) throws ItemNotFoundException,
            AccessDeniedException, RepositoryException {
        if (item != null) {
            return item.getAncestor(depth);
        } else {
            int thisDepth = getDepth();
            if (depth >= 0 || depth < thisDepth) {
                return getParent().getAncestor(depth);
            } else if (thisDepth == depth) {
                return this;
            } else {
                throw new ItemNotFoundException(
                        "Invalid ancestor depth " + depth);
            }
        }
    }

    /** {@inheritDoc} */
    public Node getParent() throws ItemNotFoundException,
            AccessDeniedException, RepositoryException {
        if (item != null) {
            return item.getParent();
        } else {
            throw new UnsupportedRepositoryOperationException();
        }
    }

    /** {@inheritDoc} */
    public int getDepth() throws RepositoryException {
        if (item != null) {
            return item.getDepth();
        } else {
            try {
                return getParent().getDepth() + 1;
            } catch (ItemNotFoundException e) {
                return 0;
            }
        }
    }

    /** {@inheritDoc} */
    public Session getSession() throws RepositoryException {
        if (item != null) {
            return item.getSession();
        } else {
            throw new UnsupportedRepositoryOperationException();
        }
    }

    /** {@inheritDoc} */
    public boolean isNode() {
        if (item != null) {
            return item.isNode();
        } else {
            return this instanceof Node;
        }
    }

    /** {@inheritDoc} */
    public boolean isNew() {
        if (item != null) {
            return item.isNew();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /** {@inheritDoc} */
    public boolean isModified() {
        if (item != null) {
            return item.isModified();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /** {@inheritDoc} */
    public boolean isSame(Item otherItem) {
        try {
            if (item != null) {
                return item.isSame(otherItem);
            } else if (otherItem == this) {
                return true;
            } else {
                return getPath().equals(otherItem.getPath());
            }
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    /** {@inheritDoc} */
    public void accept(ItemVisitor visitor) throws RepositoryException {
        if (item != null) {
            item.accept(visitor);
        } else {
            throw new UnsupportedRepositoryOperationException();
        }
    }

    /** {@inheritDoc} */
    public void save() throws AccessDeniedException,
            ConstraintViolationException, InvalidItemStateException,
            ReferentialIntegrityException, VersionException, LockException,
            RepositoryException {
        if (item != null) {
            item.save();
        } else {
            throw new UnsupportedRepositoryOperationException();
        }
    }

    /** {@inheritDoc} */
    public void refresh(boolean keepChanges) throws InvalidItemStateException,
            RepositoryException {
        if (item != null) {
            item.refresh(keepChanges);
        } else {
            throw new UnsupportedRepositoryOperationException();
        }
    }

    /** {@inheritDoc} */
    public void remove() throws VersionException, LockException,
            RepositoryException {
        if (item != null) {
            item.remove();
        } else {
            throw new UnsupportedRepositoryOperationException();
        }
    }

}
