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
 * The <code>Type</code> class encapsulates the lock type as defined by RFC 2518.
 */
public class Type {

    private static Map types = new HashMap();

    public static final Type WRITE = Type.create(DavConstants.XML_WRITE, DavConstants.NAMESPACE);

    private final String name;
    private final Namespace namespace;

    /**
     * Private constructor.
     *
     * @param name
     * @param namespace
     */
    private Type(String name, Namespace namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    /**
     * Returns the Xml representation of this lock <code>Type</code>.
     *
     * @return Xml representation
     */
    public Element toXml() {
        return new Element(name, namespace);
    }

    /**
     * Create a <code>Type</code> object from the given Xml element.
     *
     * @param lockType
     * @return <code>Type</code> object.
     */
    public static Type create(Element lockType) {
        if (lockType == null) {
            throw new IllegalArgumentException("'null' is not valid lock type entry.");
        }
        return create(lockType.getName(), lockType.getNamespace());
    }

    /**
     * Create a <code>Type</code> object from the given name and namespace.
     *
     * @param name
     * @param namespace
     * @return <code>Type</code> object.
     */
    public static Type create(String name, Namespace namespace) {
	String key = "{" + namespace.getURI() + "}" + name;
        if (types.containsKey(key)) {
            return (Type) types.get(key);
        } else {
            Type type = new Type(name, namespace);
            types.put(key, type);
            return type;
        }
    }

    /**
     * Returns <code>true</code> if this Type is equal to the given one.
     *
     * @param obj
     * @return
     */
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj instanceof Type) {
	    Type other = (Type) obj;
	    return name.equals(other.name) && namespace.equals(other.namespace);
	}
	return false;
    }
}