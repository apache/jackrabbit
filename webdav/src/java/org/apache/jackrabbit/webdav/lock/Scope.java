/*
 * Copyright 2005 The Apache Software Foundation.
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
package org.apache.jackrabbit.webdav.lock;

import org.jdom.Element;
import org.jdom.Namespace;
import org.apache.jackrabbit.webdav.DavConstants;

import java.util.*;

/**
 * The <code>Scope</code> class abstracts the lock scope as defined by RFC 2518.
 */
public class Scope {

    private static final Map scopes = new HashMap();

    public static final Scope EXCLUSIVE = Scope.create(DavConstants.XML_EXCLUSIVE, DavConstants.NAMESPACE);
    public static final Scope SHARED = Scope.create(DavConstants.XML_SHARED, DavConstants.NAMESPACE);

    private final String name;
    private final Namespace namespace;

    /**
     * Private constructor
     *
     * @param name
     * @param namespace
     */
    private Scope(String name, Namespace namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    /**
     * Return the Xml representation of the lock scope object as present in
     * the LOCK request and response body and in the {@link LockDiscovery}.
     *
     * @return Xml representation
     */
    public Element toXml() {
        return new Element(name, namespace);
    }

    /**
     * Create a <code>Scope</code> object from the given Xml element.
     *
     * @param lockScope
     * @return Scope object.
     */
    public static Scope create(Element lockScope) {
        if (lockScope == null) {
            throw new IllegalArgumentException("'null' is not valid lock scope entry.");
        }
        return create(lockScope.getName(), lockScope.getNamespace());
    }

    /**
     * Create a <code>Scope</code> object from the given name and namespace.
     *
     * @param name
     * @param namespace
     * @return Scope object.
     */
    public static Scope create(String name, Namespace namespace) {
	String key = "{" + namespace.getURI() + "}" + name;
        if (scopes.containsKey(key)) {
            return (Scope) scopes.get(key);
        } else {
            Scope scope = new Scope(name, namespace);
            scopes.put(key, scope);
            return scope;
        }
    }

    /**
     * Returns <code>true</code> if this Scope is equal to the given one.
     *
     * @param obj
     * @return
     */
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj instanceof Scope) {
	    Scope other = (Scope) obj;
	    return name.equals(other.name) && namespace.equals(other.namespace);
	}
	return false;
    }

}