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
 * TODO
 */
public class Name {

    public static Name parseJCRName(Session session, String name)
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

    private final String namespace;

    private final String name;

    public Name(String namespace, String name) {
        this.namespace = namespace;
        this.name = name;
    }

    public String getNamespaceURI() {
        return namespace;
    }

    public String getLocalPart() {
        return name;
    }

    public String toJCRName(Session session)
            throws NamespaceException, RepositoryException {
        String prefix = session.getNamespacePrefix(namespace);
        return prefix + ":" + name;
    }

    public boolean equals(Object object) {
        if (object instanceof Name) {
            Name qname = (Name) object;
            return namespace.equals(qname.namespace) && name.equals(qname.name);
        } else {
            return false;
        }
    }

}
