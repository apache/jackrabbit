/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.core;

import javax.jcr.NamespaceException;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <code>QName</code> represents the qualified name of a repository item
 * (i.e. <code>Node</code> or <code>Property</code>) or a node type.
 */
public class QName implements Cloneable, Comparable, Serializable {

    static final long serialVersionUID = -2712313010017755368L;

    /**
     * Pattern used to validate and parse name:<p>
     * <ul>
     * <li>group 1 is namespace prefix incl. delimiter (colon)
     * <li>group 2 is namespace prefix excl. delimiter (colon)
     * <li>group 3 is localName
     * </ul>
     */
    private static final Pattern NAME_PATTERN = Pattern.compile("(([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?):)?([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?)");

    private transient int hash;
    private transient String string;

    protected final String namespaceURI;
    protected final String localName;

    /**
     * Creates a new <code>QName</code> instance with the given <code>namespaceURI</code>
     * and <code>localName</code>.
     *
     * @param namespaceURI
     * @param localName
     */
    public QName(String namespaceURI, String localName) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException("invalid namespaceURI specified");
        }
        // an empty localName is valid though (e.g. the root node name)
        if (localName == null) {
            throw new IllegalArgumentException("invalid localName specified");
        }
        this.namespaceURI = namespaceURI;
        this.localName = localName;
        hash = 0;
    }

    //------------------------------------------------------< factory methods >
    /**
     * @param rawName
     * @param resolver
     * @return
     * @throws IllegalNameException
     * @throws UnknownPrefixException
     */
    public static QName fromJCRName(String rawName, NamespaceResolver resolver)
            throws IllegalNameException, UnknownPrefixException {
        if (rawName == null || rawName.length() == 0) {
            throw new IllegalNameException("empty name");
        }

        String prefix = null;
        String localName = null;

        Matcher matcher = NAME_PATTERN.matcher(rawName);
        if (matcher.matches()) {
            // check for prefix (group 1)
            if (matcher.group(1) != null) {
                // prefix specified
                // group 2 is namespace prefix excl. delimiter (colon)
                prefix = matcher.group(2);
            } else {
                // no prefix specified
                prefix = "";
            }

            // group 3 is localName
            localName = matcher.group(3);
        } else {
            // illegal syntax for name
            throw new IllegalNameException("'" + rawName + "' is not a valid name");
        }

        String uri;
        try {
            uri = resolver.getURI(prefix);
        } catch (NamespaceException nse) {
            throw new UnknownPrefixException(prefix);
        }

        return new QName(uri, localName);
    }

    /**
     * Returns a <code>QName</code> holding the value of the specified
     * string. The string must be in the format returned by the
     * <code>QName.toString()</code> method, i.e.
     * <p/>
     * <code><b>{</b>namespaceURI<b>}</b>localName</code>
     *
     * @param s a <code>String</code> containing the <code>QName</code>
     *          representation to be parsed.
     * @return the <code>QName</code> represented by the argument
     * @throws IllegalArgumentException if the specified string can not be parsed
     *                                  as a <code>QName</code>.
     * @see #toString()
     */
    public static QName valueOf(String s) {
        if ("".equals(s) || s == null) {
            throw new IllegalArgumentException("invalid QName literal");
        }

        if (s.charAt(0) == '{') {
            int i = s.indexOf('}');

            if (i == -1) {
                throw new IllegalArgumentException("invalid QName literal");
            }

            if (i == s.length() - 1) {
                throw new IllegalArgumentException("invalid QName literal");
            } else {
                return new QName(s.substring(1, i), s.substring(i + 1));
            }
        } else {
            throw new IllegalArgumentException("invalid QName literal");
        }
    }

    //------------------------------------------------------< utility methods >
    /**
     * Checks if <code>jcrName</code> is a valid JCR-style name.
     *
     * @param jcrName the name to be checked
     * @throws IllegalNameException If <code>jcrName</code> is not a valid
     *                              JCR-style name.
     */
    public static void checkFormat(String jcrName) throws IllegalNameException {
        if (jcrName == null || jcrName.length() == 0) {
            throw new IllegalNameException("empty name");
        }

        Matcher matcher = NAME_PATTERN.matcher(jcrName);
        if (!matcher.matches()) {
            // illegal syntax for name
            throw new IllegalNameException("'" + jcrName + "' is not a valid name");
        }
    }

    //-------------------------------------------------------< public methods >
    /**
     * @return
     */
    public String getLocalName() {
        return localName;
    }

    /**
     * @return
     */
    public String getNamespaceURI() {
        return namespaceURI;
    }

    /**
     * @param resolver
     * @return
     * @throws NoPrefixDeclaredException
     */
    public String toJCRName(NamespaceResolver resolver) throws NoPrefixDeclaredException {
        StringBuffer sb = new StringBuffer();
        // prefix
        String prefix;
        try {
            prefix = resolver.getPrefix(namespaceURI);
        } catch (NamespaceException nse) {
            throw new NoPrefixDeclaredException("no prefix declared for URI: " + namespaceURI);
        }
        if (prefix.length() == 0) {
            // default prefix (empty string)
        } else {
            sb.append(prefix);
            sb.append(':');
        }
        // name
        sb.append(localName);
        return sb.toString();
    }

    /**
     * Returns the string representation of this <code>QName</code> in the
     * following format:
     * <p/>
     * <code><b>{</b>namespaceURI<b>}</b>localName</code>
     *
     * @return the string representation of this <code>QName</code>.
     * @see #valueOf(java.lang.String)
     */
    public String toString() {
        // QName is immutable, we can store the string representation
        if (string == null) {
            string = '{' + namespaceURI + '}' + localName;
        }
        return string;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof QName) {
            QName other = (QName) obj;
            return namespaceURI.equals(other.namespaceURI)
                    && localName.equals(other.localName);
        }
        return false;
    }

    public int hashCode() {
        // QName is immutable, we can store the computed hash code value
        if (hash == 0) {
            hash = namespaceURI.hashCode() + 313 * localName.hashCode();
        }
        return hash;
    }

    /**
     * Creates a clone of this <code>QName</code>.
     * Overriden in order to make <code>clone()</code> public.
     *
     * @return a clone of this instance
     * @throws CloneNotSupportedException
     */
    public Object clone() throws CloneNotSupportedException {
        // QName is immutable, no special handling required
        return super.clone();
    }

    public int compareTo(Object o) {
        return toString().compareTo(((QName) o).toString());
    }
}
