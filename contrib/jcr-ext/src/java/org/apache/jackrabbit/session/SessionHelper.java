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
package org.apache.jackrabbit.session;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;

import org.apache.jackrabbit.name.QName;

/**
 * Helper class for accessing the namespaces and node types associated
 * with a JCR Session.
 */
public final class SessionHelper {

    /** The session to which this frontend instance is bound. */
    private final Session session;

    /**
     * Creates a helper for accessing the given session.
     *
     * @param session current session
     */
    public SessionHelper(Session session) {
        this.session = session;
    }

    /**
     * Returns the prefixed JCR name that represents the given
     * qualified name.
     *
     * @param name qualified name
     * @return prefixed JCR name
     * @throws IllegalStateException if the namespace does not exist
     * @throws RuntimeException if a repository error occurs
     */
    public String getName(QName name)
            throws IllegalStateException, RuntimeException {
        try {
            String prefix = session.getNamespacePrefix(name.getNamespaceURI());
            if (prefix.length() > 0) {
                return prefix + ":" + name.getLocalName();
            } else {
                return name.getLocalName();
            }
        } catch (NamespaceException e) {
            throw new IllegalStateException("Expected namespace not found. " + e.getMessage());
        } catch (RepositoryException e) {
            throw new RuntimeException("Unexpected repository error", e);
        }
    }

    /**
     * Returns the named node type. This method is used to convert
     * qualified node type names to actual NodeType objects of the current
     * node type manager. The returned node type is acquired using the standard
     * JCR API starting with <code>getWorkspace().getNodeTypeManager()</code>
     * from the current session.
     *
     * @param name qualified node type name
     * @return named node type
     * @throws IllegalStateException if the named node type does not exist
     * @throws RuntimeException      if a repository error occurs
     */
    public NodeType getNodeType(QName name)
            throws IllegalStateException, RuntimeException {
        return getNodeType(getName(name));
    }

    /**
     * Returns the named node type. This method is used to convert
     * node type names to actual NodeType objects of the current node
     * type manager. The returned node type is acquired using the standard
     * JCR API starting with <code>getWorkspace().getNodeTypeManager()</code>
     * from the current session.
     *
     * @param name node type name
     * @return named node type
     * @throws IllegalStateException if the named node type does not exist
     * @throws RuntimeException      if a repository error occurs
     */
    public NodeType getNodeType(String name)
            throws IllegalStateException, RuntimeException {
        try {
            return
                session.getWorkspace().getNodeTypeManager().getNodeType(name);
        } catch (NoSuchNodeTypeException e) {
            throw new IllegalStateException("Expected node type not found" + e.getMessage());
        } catch (RepositoryException e) {
            throw new RuntimeException("Unexpected repository error", e);
        }
    }

    /**
     * Compares session helpers for equality. Two session helpers are equal
     * if they refer to the same session.
     *
     * @param that the object to compare this one to
     * @return <code>true</code> if the objects are equal,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        } else if (that instanceof SessionHelper) {
            return session.equals(((SessionHelper) that).session);
        } else {
            return false;
        }
    }

    /**
     * Calculates the session helper hash code. This method simply returns
     * the hash code of the underlying session.
     *
     * @return session hash code
     * @see Object#hashCode()
     */
    public int hashCode() {
        return session.hashCode();
    }

}
