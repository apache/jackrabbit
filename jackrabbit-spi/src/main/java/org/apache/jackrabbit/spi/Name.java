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
package org.apache.jackrabbit.spi;

import java.io.Serializable;

/**
 * A <code>Name</code> is a combination of a namespace URI and a local part.
 * Instances of this class are used to internally represent the names of JCR
 * content items and other objects within a content repository.
 * <p/>
 * A <code>Name</code> is immutable once created.
 * <p/>
 * The String representation of a <code>Name</code> object must be in the
 * format "<code>{namespaceURI}localPart</code>".
 * <p/>
 * An implementation of the <code>Name</code> interface must implement the
 * {@link Object#equals(Object)} method such that two Name objects are equal if
 * both the namespace URI and the local part are equal.
 */
public interface Name extends Comparable, Cloneable, Serializable {

    // default namespace (empty uri)
    public static final String NS_EMPTY_PREFIX = "";
    public static final String NS_DEFAULT_URI = "";

    // reserved namespace for repository internal node types
    public static final String NS_REP_PREFIX = "rep";
    public static final String NS_REP_URI = "internal";

    // reserved namespace for items defined by built-in node types
    public static final String NS_JCR_PREFIX = "jcr";
    public static final String NS_JCR_URI = "http://www.jcp.org/jcr/1.0";

    // reserved namespace for built-in primary node types
    public static final String NS_NT_PREFIX = "nt";
    public static final String NS_NT_URI = "http://www.jcp.org/jcr/nt/1.0";

    // reserved namespace for built-in mixin node types
    public static final String NS_MIX_PREFIX = "mix";
    public static final String NS_MIX_URI = "http://www.jcp.org/jcr/mix/1.0";

    // reserved namespace used in the system view XML serialization format
    public static final String NS_SV_PREFIX = "sv";
    public static final String NS_SV_URI = "http://www.jcp.org/jcr/sv/1.0";

    // reserved namespaces that must not be redefined and should not be used
    public static final String NS_XML_PREFIX = "xml";
    public static final String NS_XML_URI = "http://www.w3.org/XML/1998/namespace";
    public static final String NS_XMLNS_PREFIX = "xmlns";
    public static final String NS_XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    /**
     * Empty array of <code>Name</code>
     */
    public static final Name[] EMPTY_ARRAY = new Name[0];

    /**
     * Returns the local part of this <code>Name</code> object.
     *
     * @return local name
     */
    public String getLocalName();

    /**
     * Returns the namespace URI of this <code>Name</code> object.
     *
     * @return namespace URI
     */
    public String getNamespaceURI();
}
