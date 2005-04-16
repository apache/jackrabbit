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
package org.apache.jackrabbit.lite;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.base.BaseItem;
import org.apache.jackrabbit.name.Name;

/**
 * TODO
 */
public class LiteItem extends BaseItem {

    private final Session session;

    private final Node parent;

    private final Name name;

    protected LiteItem(Session session, Node parent, Name name) {
        this.session = session;
        this.parent = parent;
        this.name = name;
    }

    public Session getSession() throws RepositoryException {
        return session;
    }

    public String getName() throws RepositoryException {
        return name.toJCRName(getSession());
    }

    public Node getParent() throws ItemNotFoundException,
            AccessDeniedException, RepositoryException {
        return parent;
    }

    public boolean isModified() {
        return false;
    }

    public boolean isNew() {
        return false;
    }

    public boolean isSame(Item otherItem) {
        try {
            return getPath().equals(otherItem.getPath());
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public void refresh(boolean keepChanges) throws InvalidItemStateException,
            RepositoryException {
        // do nothing
    }

    public void remove() throws VersionException, LockException,
            RepositoryException {
        // do nothing
    }

    public void save() throws AccessDeniedException,
            ConstraintViolationException, InvalidItemStateException,
            ReferentialIntegrityException, VersionException, LockException,
            RepositoryException {
        // do nothing
    }

}
