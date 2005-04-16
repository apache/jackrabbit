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
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.lite.nodetype.LiteNodeDef;

/**
 * TODO
 */
public class LiteRootNode extends LiteNode {

    protected LiteRootNode(Session session) {
        super(session,
                new LiteNodeDef(session, null, null, 0, false, false, false),
                null);
    }

    
    public void addMixin(String mixinName) throws NoSuchNodeTypeException,
            VersionException, ConstraintViolationException, LockException,
            RepositoryException {
        throw new ConstraintViolationException(
                "Unable to change root node type");
    }

    public boolean canAddMixin(String mixinName) throws RepositoryException {
        return false;
    }

    public String getName() throws RepositoryException {
        return "";
    }

    public String getPath() throws RepositoryException {
        return "/";
    }


    public int getDepth() throws RepositoryException {
        return 0;
    }

    public Node getParent() throws ItemNotFoundException,
            AccessDeniedException, RepositoryException {
        throw new ItemNotFoundException("The root node has no parent");
    }

    public boolean hasProperties() throws RepositoryException {
        return false;
    }

    public void removeMixin(String mixinName) throws NoSuchNodeTypeException,
            VersionException, ConstraintViolationException, LockException,
            RepositoryException {
        throw new RepositoryException("The root node is not modifiable");
    }

    public Property setProperty(String name, Value value)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        throw new RepositoryException("The root node is not modifiable");
    }

    public Property setProperty(String name, Value[] values, int type)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        throw new RepositoryException("The root node is not modifiable");
    }

    public Property setProperty(String name, Value[] values)
            throws ValueFormatException, VersionException, LockException,
            RepositoryException {
        throw new RepositoryException("The root node is not modifiable");
    }

    public Item getAncestor(int depth) throws ItemNotFoundException,
            AccessDeniedException, RepositoryException {
        if (depth == 0) {
            return this;
        } else {
            throw new ItemNotFoundException("Invalid ancestor depth " + depth);
        }
    }

}
