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

import org.apache.xerces.util.XMLChar;

import javax.jcr.NamespaceException;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Qualified name. A qualified name is a combination of a namespace URI
 * and a local part. Instances of this class are used by Jackrabbit to
 * represent the names of JCR content items and other objects.
 * <p/>
 * A qualified name is immutable once created, although the prefixed JCR
 * name representation of the qualified name can change depending on the
 * namespace mappings in effect.
 *
 * <h2>String representations</h2>
 * <p>
 * The prefixed JCR name format of a qualified name is specified by
 * JSR 170 as follows:
 * <pre>
 *
 *  name ::= simplename | prefixedname
 *  simplename ::= onecharsimplename |
 *                 twocharsimplename |
 *                 threeormorecharname
 *  prefixedname ::= prefix ':' localname
 *  localname ::= onecharlocalname |
 *                twocharlocalname |
 *                threeormorecharname
 *  onecharsimplename ::= (* Any Unicode character except:
 *                     '.', '/', ':', '[', ']', '*',
 *                     ''', '"', '|' or any whitespace
 *                     character *)
 *  twocharsimplename ::= '.' onecharsimplename |
 *                        onecharsimplename '.' |
 *                        onecharsimplename onecharsimplename
 *  onecharlocalname ::= nonspace
 *  twocharlocalname ::= nonspace nonspace
 *  threeormorecharname ::= nonspace string nonspace
 *  prefix ::= (* Any valid XML Name *)
 *  string ::= char | string char
 *  char ::= nonspace | ' '
 *  nonspace ::= (* Any Unicode character except:
 *                  '/', ':', '[', ']', '*',
 *                  ''', '"', '|' or any whitespace
 *                  character *)
 * </pre>
 * <p>
 * In addition to the prefixed JCR name format, a qualified name can also
 * be represented using the format "<code>{namespaceURI}localPart</code>".
 */
public class QName implements Cloneable, Comparable, Serializable {

    /** Serialization UID of this class. */
    static final long serialVersionUID = -2712313010017755368L;

    public static final QName[] EMPTY_ARRAY = new QName[0];

    /**
     * The reqular expression pattern used to validate and parse
     * qualified names.
     * <p>
     * The pattern contains the following groups:
     * <ul>
     * <li>group 1 is namespace prefix incl. delimiter (colon)
     * <li>group 2 is namespace prefix excl. delimiter (colon)
     * <li>group 3 is localName
     * </ul>
     */
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "(([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?):)?"
            + "([^ /:\\[\\]*'\"|](?:[^/:\\[\\]*'\"|]*[^ /:\\[\\]*'\"|])?)");

    /**
     * Matcher instance as thread-local.
     */
    private static final ThreadLocal NAME_MATCHER = new ThreadLocal() {
        protected Object initialValue() {
            return NAME_PATTERN.matcher("dummy");
        }
    };

    /** The memorized hash code of this qualified name. */
    private transient int hash;

    /** The memorized string representation of this qualified name. */
    private transient String string;

    /** The internalized namespace URI of this qualified name. */
    protected final String namespaceURI;

    /** The internalized local part of this qualified name. */
    protected final String localName;

    /**
     * Creates a new qualified name with the given namespace URI and
     * local part.
     * <p/>
     * Note that the format of the local part is not validated. The format
     * can be checked by calling {@link #checkFormat(String)}.
     *
     * @param namespaceURI namespace uri
     * @param localName local part
     */
    public QName(String namespaceURI, String localName) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException("invalid namespaceURI specified");
        }
        // an empty localName is valid though (e.g. the root node name)
        if (localName == null) {
            throw new IllegalArgumentException("invalid localName specified");
        }
        // internalize both namespaceURI and localName to improve performance
        // of QName comparisons
        this.namespaceURI = namespaceURI.intern();
        this.localName = localName.intern();
        hash = 0;
    }

    //------------------------------------------------------< factory methods >

    /**
     * Parses the given prefixed JCR name into a qualified name using the
     * given namespace resolver.
     *
     * @param rawName prefixed JCR name
     * @param resolver namespace resolver
     * @return qualified name
     * @throws IllegalNameException if the given name is not a valid JCR name
     * @throws UnknownPrefixException if the JCR name prefix does not resolve
     */
    public static QName fromJCRName(String rawName, NamespaceResolver resolver)
            throws IllegalNameException, UnknownPrefixException {
        if (resolver == null) {
            throw new NullPointerException("resolver must not be null");
        }

        if (rawName == null || rawName.length() == 0) {
            throw new IllegalNameException("empty name");
        }

        // parts[0]: prefix
        // parts[1]: localName
        String[] parts = parse(rawName);

        String uri;
        try {
            uri = resolver.getURI(parts[0]);
        } catch (NamespaceException nse) {
            throw new UnknownPrefixException(parts[0]);
        }

        return new QName(uri, parts[1]);
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
    public static QName valueOf(String s) throws IllegalArgumentException {
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
        parse(jcrName);
    }

    /**
     * Parses the <code>jcrName</code> and returns an array of two strings:
     * the first array element contains the prefix (or empty string),
     * the second the local name.
     *
     * @param jcrName the name to be parsed
     * @return An array holding two strings: the first array element contains
     *         the prefix (or empty string), the second the local name.
     * @throws IllegalNameException If <code>jcrName</code> is not a valid
     *                              JCR-style name.
     */
    public static String[] parse(String jcrName) throws IllegalNameException {
        if (jcrName == null || jcrName.length() == 0) {
            throw new IllegalNameException("empty name");
        }

        if (".".equals(jcrName) || "..".equals(jcrName)) {
            // illegal syntax for name
            throw new IllegalNameException("'" + jcrName + "' is not a valid name");
        }

        String prefix;
        String localName;


        Matcher matcher = (Matcher) NAME_MATCHER.get();
        matcher.reset(jcrName);
        if (matcher.matches()) {
            // check for prefix (group 1)
            if (matcher.group(1) != null) {
                // prefix specified
                // group 2 is namespace prefix excl. delimiter (colon)
                prefix = matcher.group(2);
                // check if the prefix is a valid XML prefix
                if (!XMLChar.isValidNCName(prefix)) {
                    // illegal syntax for prefix
                    throw new IllegalNameException("'" + jcrName
                            + "' is not a valid name: illegal prefix");
                }
            } else {
                // no prefix specified
                prefix = "";
            }

            // group 3 is localName
            localName = matcher.group(3);
        } else {
            // illegal syntax for name
            throw new IllegalNameException("'" + jcrName + "' is not a valid name");
        }

        return new String[]{prefix, localName};
    }

    //-------------------------------------------------------< public methods >
    /**
     * Returns the local part of the qualified name.
     *
     * @return local name
     */
    public String getLocalName() {
        return localName;
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
     * Returns the qualified name in the prefixed JCR name format.
     * The namespace URI is mapped to a prefix using the given
     * namespace resolver.
     *
     * @param resolver namespace resolver
     * @return prefixed name
     * @throws NoPrefixDeclaredException if the namespace can not be resolved
     */
    public String toJCRName(NamespaceResolver resolver)
            throws NoPrefixDeclaredException {
        StringBuffer sb = new StringBuffer();
        toJCRName(resolver, sb);
        return sb.toString();
    }

    /**
     * Appends the qualified name in the prefixed JCR name format to the given
     * string buffer. The namespace URI is mapped to a prefix using the given
     * namespace resolver.
     *
     * @param resolver namespace resolver
     * @param buf      string buffer where the prefixed JCR name should be
     *                 appended to
     * @throws NoPrefixDeclaredException if the namespace can not be resolved
     * @see #toJCRName(NamespaceResolver)
     */
    public void toJCRName(NamespaceResolver resolver, StringBuffer buf)
            throws NoPrefixDeclaredException {
        // prefix
        String prefix;
        try {
            prefix = resolver.getPrefix(namespaceURI);
        } catch (NamespaceException nse) {
            throw new NoPrefixDeclaredException("no prefix declared for URI: "
                    + namespaceURI);
        }
        if (prefix.length() == 0) {
            // default prefix (empty string)
        } else {
            buf.append(prefix);
            buf.append(':');
        }
        // name
        buf.append(localName);
    }

    /**
     * Returns the string representation of this <code>QName</code> in the
     * following format:
     * <p/>
     * <code><b>{</b>namespaceURI<b>}</b>localName</code>
     *
     * @return the string representation of this <code>QName</code>.
     * @see #valueOf(String)
     */
    public String toString() {
        // QName is immutable, we can store the string representation
        if (string == null) {
            string = '{' + namespaceURI + '}' + localName;
        }
        return string;
    }

    /**
     * Compares two qualified names for equality. Returns <code>true</code>
     * if the given object is a qualified name and has the same namespace URI
     * and local part as this qualified name.
     *
     * @param obj the object to compare this qualified name with
     * @return <code>true</code> if the object is equal to this qualified name,
     *         <code>false</code> otherwise
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof QName) {
            QName other = (QName) obj;
            // localName & namespaceURI are internalized,
            // we only have to compare their references
            return localName == other.localName
                    && namespaceURI == other.namespaceURI;
        }
        return false;
    }

    /**
     * Returns the hash code of this qualified name. The hash code is
     * computed from the namespace URI and local part of the qualified
     * name and memorized for better performance.
     *
     * @return hash code
     * @see Object#hashCode()
     */
    public int hashCode() {
        // QName is immutable, we can store the computed hash code value
        int h = hash;
        if (h == 0) {
            h = 17;
            h = 37 * h + namespaceURI.hashCode();
            h = 37 * h + localName.hashCode();
            hash = h;
        }
        return h;
    }

    /**
     * Creates a clone of this qualified name.
     * Overriden in order to make <code>clone()</code> public.
     *
     * @return a clone of this instance
     * @throws CloneNotSupportedException never thrown
     * @see Object#clone()
     */
    public Object clone() throws CloneNotSupportedException {
        // QName is immutable, no special handling required
        return super.clone();
    }

    /**
     * Compares two qualified names.
     *
     * @param o the object to compare this qualified name with
     * @return comparison result
     * @throws ClassCastException if the given object is not a qualified name
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(Object o) throws ClassCastException {
        QName other = (QName) o;
        int result = namespaceURI.compareTo(other.namespaceURI);
        return (result != 0) ? result : localName.compareTo(other.localName);
    }
}
