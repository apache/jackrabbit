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
import javax.jcr.ItemExistsException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.base.BaseWorkspace;
import org.xml.sax.ContentHandler;

/**
 * Lightweight implementation of the JCR Workspace interface.
 */
public class LiteWorkspace extends BaseWorkspace {

    /**
     * Creates a temporary workspace/session pair for the identified
     * workspace. The created temporary session can be used to bypass
     * the transient state of the current session.
     * <p>
     * Subclasses need to implement this method to make the default behaviour
     * of this base class functional. By default this method just throws
     * an {@link UnsupportedRepositoryOperationException UnsupportedRepositoryOperationException}.
     *
     * @param name workspace name
     * @return temporary workspace reference
     * @throws RepositoryException on repository errors
     */
    protected Workspace getWorkspace(String name) throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * Recursively copies the given source node into a child node of
     * the given destination parent node. The nodes need not be from
     * the same session or workspace.
     * <p>
     * This utility method is invoked by the default implementation of
     * the public {@link #copy(String, String, String) copy} method.
     *
     * @param srcNode source node
     * @param destParent destination parent node
     * @throws RepositoryException on repository errors
     */
    private void copy(Node srcNode, Node destParent)
            throws RepositoryException {
        Node destNode = destParent.addNode(srcNode.getName());

        PropertyIterator properties = srcNode.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            if (property.getDefinition().isMultiple()) {
                destNode.setProperty(property.getName(), property.getValues());
            } else {
                destNode.setProperty(property.getName(), property.getValue());
            }
        }

        NodeIterator children = srcNode.getNodes();
        while (children.hasNext()) {
            copy(children.nextNode(), destNode);
        }
    }

    /**
     * Copies the identified content subtree to the given location.
     * Implemented by creating temporary sessions for both this and
     * the source workspace and copying the content subtree recursively
     * item by item.
     * {@inheritDoc}
     */
    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath)
            throws NoSuchWorkspaceException, ConstraintViolationException,
            VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {
        Session srcSession = null;
        Session destSession = null;
        try {
            srcSession = getWorkspace(srcWorkspace).getSession();
            Item srcNode = srcSession.getItem(srcAbsPath);
            if (!srcNode.isNode()) {
                throw new PathNotFoundException(
                        "Invalid node path " + srcAbsPath);
            }

            destSession = getWorkspace(getName()).getSession();
            Item destParent = destSession.getItem(destAbsPath);
            if (!destParent.isNode()) {
                throw new PathNotFoundException(
                        "Invalid node path " + destAbsPath);
            }

            copy((Node) srcNode, (Node) destParent);
            destSession.save();
        } finally {
            try { srcSession.logout(); } catch (Exception e) { }
            try { destSession.logout(); } catch (Exception e) { }
        }
    }

    public ContentHandler getImportContentHandler(String parentAbsPath,
            int uuidBehavior) throws PathNotFoundException,
            ConstraintViolationException, VersionException, LockException,
            RepositoryException {
        Session session = null;
        try {
            session = getWorkspace(getName()).getSession();
            // TODO
            session.save();
            throw new UnsupportedRepositoryOperationException();
        } finally {
            try { session.logout(); } catch (Exception e) { }
        }
    }

}
