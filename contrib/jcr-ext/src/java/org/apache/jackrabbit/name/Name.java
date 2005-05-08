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
package org.apache.jackrabbit.name;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Qualified name. Instance of this immutable class are used to
 * represent qualified names. A qualified name consists of a
 * namespace URI and a local part.
 */
public final class Name {

    /** Namespace URI of the qualified name. */
    private final String namespaceURI;

    /** Local part of the qualified name. */
    private final String localPart;

    /**
     * Creates a qualified name instance.
     *
     * @param namespace namespace URI
     * @param name      local part
     */
    public Name(String namespace, String name) {
        this.namespaceURI = namespace;
        this.localPart = name;
    }

    /**
     * Returns the namespace URI of the qualified name.
     *
     * @return namespace URI
     */
    public String getNamespaceURI() {
        return namespaceURI;
    }

    /**
     * Returns the local part of the qualified name.
     *
     * @return local part
     */
    public String getLocalPart() {
        return localPart;
    }

    /**
     * Compares for equality. Two qualified names are equal if they have
     * the same namespace URI and the same local part.
     *
     * @param object the object to compare to
     * @return <code>true</code> if the given object is equal to this one,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object instanceof Name) {
            Name that = (Name) object;
            return namespaceURI.equals(that.namespaceURI)
                && localPart.equals(that.localPart);
        } else {
            return false;
        }
    }

    /**
     * Calculates the hash code of the qualified name.
     *
     * @return hash code
     * @see Object#hashCode()
     */
    public int hashCode() {
        int code = 17;
        code = 37 * code + namespaceURI.hashCode();
        code = 37 * code + localPart.hashCode();
        return code;
    }

    /**
     * Returns a string representation of the qualified name.
     *
     * @return string representation
     * @see Object#toString()
     */
    public String toString() {
        return "{" + namespaceURI + "}" + localPart;
    }

    /**
     * Parses the given prefixed JCR name into a qualified name instance.
     * The namespace prefix is resolved using the current session.
     *
     * @param session current session
     * @param name    prefixed JCR name
     * @return qualified name
     * @throws NamespaceException  if the namespace prefix is not registered
     * @throws RepositoryException if another error occurs
     */
    public static Name fromJCRName(Session session, String name)
            throws NamespaceException, RepositoryException {
        int p = name.indexOf(':');
        if (p != -1) {
            String prefix = name.substring(0, p);
            name = name.substring(p + 1);
            return new Name(session.getNamespaceURI(prefix), name);
        } else {
            return new Name(session.getNamespaceURI(""), name);
        }
    }

    /**
     * Returns the prefixed JCR name representation of the qualified name.
     * The namespace prefix is retrieved from the current session.
     *
     * @param session current session
     * @return prefixed JCR name
     * @throws NamespaceException  if the namespace URI is not registered
     * @throws RepositoryException if another error occurs
     */
    public String toJCRName(Session session)
            throws NamespaceException, RepositoryException {
        String prefix = session.getNamespacePrefix(namespaceURI);
        return prefix + ":" + localPart;
    }

}
