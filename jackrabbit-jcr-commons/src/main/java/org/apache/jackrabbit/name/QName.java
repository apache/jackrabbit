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
package org.apache.jackrabbit.name;

import java.io.Serializable;

/**
 * Qualified name. A qualified name is a combination of a namespace URI
 * and a local part. Instances of this class are used to internally represent
 * the names of JCR content items and other objects within a content repository.
 * <p>
 * A qualified name is immutable once created, although the prefixed JCR
 * name representation of the qualified name can change depending on the
 * namespace mappings in effect.
 * <p>
 * This class also contains a number of common namespace and qualified name
 * constants for the namespaces and names specified by the JCR specification.
 *
 * <h2>String representations</h2>
 * <p>
 * The prefixed JCR name format of a qualified name is specified by
 * section 4.6 of the the JCR 1.0 specification (JSR 170) as follows:
 * <pre>
 * name                ::= simplename | prefixedname
 * simplename          ::= onecharsimplename |
 *                         twocharsimplename |
 *                         threeormorecharname
 * prefixedname        ::= prefix ':' localname
 * localname           ::= onecharlocalname |
 *                         twocharlocalname |
 *                         threeormorecharname
 * onecharsimplename   ::= (* Any Unicode character except:
 *                            '.', '/', ':', '[', ']', '*',
 *                            ''', '"', '|' or any whitespace
 *                            character *)
 * twocharsimplename   ::= '.' onecharsimplename |
 *                         onecharsimplename '.' |
 *                         onecharsimplename onecharsimplename
 * onecharlocalname    ::= nonspace
 * twocharlocalname    ::= nonspace nonspace
 * threeormorecharname ::= nonspace string nonspace
 * prefix              ::= (* Any valid XML Name *)
 * string              ::= char | string char
 * char                ::= nonspace | ' '
 * nonspace            ::= (* Any Unicode character except:
 *                            '/', ':', '[', ']', '*',
 *                            ''', '"', '|' or any whitespace
 *                            character *)
 * </pre>
 * <p>
 * In addition to the prefixed JCR name format, a qualified name can also
 * be represented using the format "<code>{namespaceURI}localPart</code>".
 *
 * @deprecated Use the Name and NameFactory interfaces from
 *             the org.apache.jackrabbit.spi package of
 *             the jackrabbit-spi component. A default implementation
 *             is available as
 *             the org.apache.jackrabbit.spi.commons.name.NameFactoryImpl
 *             class in the jackrabbit-spi-commons component.
 */
public final class QName implements Cloneable, Comparable, Serializable {

    //------------------------------------------< namespace related constants >

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

    //------------------------------------------< general item name constants >

    /**
     * Extra QName for the root node
     */
    public static final QName ROOT = new QName(NS_DEFAULT_URI,"");

    /**
     * jcr:system
     */
    public static final QName JCR_SYSTEM = new QName(NS_JCR_URI, "system");

    /**
     * jcr:nodeTypes
     */
    public static final QName JCR_NODETYPES = new QName(NS_JCR_URI, "nodeTypes");

    /**
     * jcr:uuid
     */
    public static final QName JCR_UUID = new QName(NS_JCR_URI, "uuid");

    /**
     * jcr:primaryType
     */
    public static final QName JCR_PRIMARYTYPE = new QName(NS_JCR_URI, "primaryType");

    /**
     * jcr:mixinTypes
     */
    public static final QName JCR_MIXINTYPES = new QName(NS_JCR_URI, "mixinTypes");

    /**
     * jcr:created
     */
    public static final QName JCR_CREATED = new QName(NS_JCR_URI, "created");

    /**
     * jcr:lastModified
     */
    public static final QName JCR_LASTMODIFIED = new QName(NS_JCR_URI, "lastModified");

    /**
     * jcr:encoding
     */
    public static final QName JCR_ENCODING = new QName(NS_JCR_URI, "encoding");

    /**
     * jcr:mimeType
     */
    public static final QName JCR_MIMETYPE = new QName(NS_JCR_URI, "mimeType");

    /**
     * jcr:data
     */
    public static final QName JCR_DATA = new QName(NS_JCR_URI, "data");

    /**
     * jcr:content
     */
    public static final QName JCR_CONTENT = new QName(NS_JCR_URI, "content");

    //--------------------------------------< xml related item name constants >

    /**
     * jcr:root (dummy name for root node used in XML serialization)
     */
    public static final QName JCR_ROOT = new QName(NS_JCR_URI, "root");

    /**
     * jcr:xmltext
     */
    public static final QName JCR_XMLTEXT = new QName(NS_JCR_URI, "xmltext");

    /**
     * jcr:xmlcharacters
     */
    public static final QName JCR_XMLCHARACTERS = new QName(NS_JCR_URI, "xmlcharacters");

    //-----------------------------------------< query related name constants >

    /**
     * jcr:score
     */
    public static final QName JCR_SCORE = new QName(NS_JCR_URI, "score");

    /**
     * jcr:path
     */
    public static final QName JCR_PATH = new QName(NS_JCR_URI, "path");

    /**
     * jcr:statement
     */
    public static final QName JCR_STATEMENT = new QName(NS_JCR_URI, "statement");

    /**
     * jcr:language
     */
    public static final QName JCR_LANGUAGE = new QName(NS_JCR_URI, "language");

    //----------------------------------< locking related item name constants >

    /**
     * jcr:lockOwner
     */
    public static final QName JCR_LOCKOWNER = new QName(NS_JCR_URI, "lockOwner");

    /**
     * jcr:lockIsDeep
     */
    public static final QName JCR_LOCKISDEEP = new QName(NS_JCR_URI, "lockIsDeep");

    //-------------------------------< versioning related item name constants >

    /**
     * jcr:versionStorage
     */
    public static final QName JCR_VERSIONSTORAGE = new QName(NS_JCR_URI, "versionStorage");

    /**
     * jcr:mergeFailed
     */
    public static final QName JCR_MERGEFAILED = new QName(NS_JCR_URI, "mergeFailed");

    /**
     * jcr:frozenNode
     */
    public static final QName JCR_FROZENNODE = new QName(NS_JCR_URI, "frozenNode");

    /**
     * jcr:frozenUuid
     */
    public static final QName JCR_FROZENUUID = new QName(NS_JCR_URI, "frozenUuid");

    /**
     * jcr:frozenPrimaryType
     */
    public static final QName JCR_FROZENPRIMARYTYPE = new QName(NS_JCR_URI, "frozenPrimaryType");

    /**
     * jcr:frozenMixinTypes
     */
    public static final QName JCR_FROZENMIXINTYPES = new QName(NS_JCR_URI, "frozenMixinTypes");

    /**
     * jcr:predecessors
     */
    public static final QName JCR_PREDECESSORS = new QName(NS_JCR_URI, "predecessors");

    /**
     * jcr:versionLabels
     */
    public static final QName JCR_VERSIONLABELS = new QName(NS_JCR_URI, "versionLabels");

    /**
     * jcr:successors
     */
    public static final QName JCR_SUCCESSORS = new QName(NS_JCR_URI, "successors");

    /**
     * jcr:isCheckedOut
     */
    public static final QName JCR_ISCHECKEDOUT = new QName(NS_JCR_URI, "isCheckedOut");

    /**
     * jcr:versionHistory
     */
    public static final QName JCR_VERSIONHISTORY = new QName(NS_JCR_URI, "versionHistory");

    /**
     * jcr:baseVersion
     */
    public static final QName JCR_BASEVERSION = new QName(NS_JCR_URI, "baseVersion");

    /**
     * jcr:childVersionHistory
     */
    public static final QName JCR_CHILDVERSIONHISTORY = new QName(NS_JCR_URI, "childVersionHistory");

    /**
     * jcr:rootVersion
     */
    public static final QName JCR_ROOTVERSION = new QName(NS_JCR_URI, "rootVersion");

    /**
     * jcr:versionableUuid
     */
    public static final QName JCR_VERSIONABLEUUID = new QName(NS_JCR_URI, "versionableUuid");

    //--------------------------------< node type related item name constants >

    /**
     * jcr:nodeTypeName
     */
    public static final QName JCR_NODETYPENAME = new QName(NS_JCR_URI, "nodeTypeName");

    /**
     * jcr:hasOrderableChildNodes
     */
    public static final QName JCR_HASORDERABLECHILDNODES = new QName(NS_JCR_URI, "hasOrderableChildNodes");

    /**
     * jcr:isMixin
     */
    public static final QName JCR_ISMIXIN = new QName(NS_JCR_URI, "isMixin");

    /**
     * jcr:supertypes
     */
    public static final QName JCR_SUPERTYPES = new QName(NS_JCR_URI, "supertypes");

    /**
     * jcr:propertyDefinition
     */
    public static final QName JCR_PROPERTYDEFINITION = new QName(NS_JCR_URI, "propertyDefinition");

    /**
     * jcr:name
     */
    public static final QName JCR_NAME = new QName(NS_JCR_URI, "name");

    /**
     * jcr:mandatory
     */
    public static final QName JCR_MANDATORY = new QName(NS_JCR_URI, "mandatory");

    /**
     * jcr:protected
     */
    public static final QName JCR_PROTECTED = new QName(NS_JCR_URI, "protected");

    /**
     * jcr:requiredType
     */
    public static final QName JCR_REQUIREDTYPE = new QName(NS_JCR_URI, "requiredType");

    /**
     * jcr:onParentVersion
     */
    public static final QName JCR_ONPARENTVERSION = new QName(NS_JCR_URI, "onParentVersion");

    /**
     * jcr:primaryItemName
     */
    public static final QName JCR_PRIMARYITEMNAME = new QName(NS_JCR_URI, "primaryItemName");

    /**
     * jcr:multiple
     */
    public static final QName JCR_MULTIPLE = new QName(NS_JCR_URI, "multiple");

    /**
     * jcr:valueConstraints
     */
    public static final QName JCR_VALUECONSTRAINTS = new QName(NS_JCR_URI, "valueConstraints");

    /**
     * jcr:defaultValues
     */
    public static final QName JCR_DEFAULTVALUES = new QName(NS_JCR_URI, "defaultValues");

    /**
     * jcr:autoCreated
     */
    public static final QName JCR_AUTOCREATED = new QName(NS_JCR_URI, "autoCreated");

    /**
     * jcr:childNodeDefinition
     */
    public static final QName JCR_CHILDNODEDEFINITION = new QName(NS_JCR_URI, "childNodeDefinition");

    /**
     * jcr:sameNameSiblings
     */
    public static final QName JCR_SAMENAMESIBLINGS = new QName(NS_JCR_URI, "sameNameSiblings");

    /**
     * jcr:defaultPrimaryType
     */
    public static final QName JCR_DEFAULTPRIMARYTYPE = new QName(NS_JCR_URI, "defaultPrimaryType");

    /**
     * jcr:requiredPrimaryTypes
     */
    public static final QName JCR_REQUIREDPRIMARYTYPES = new QName(NS_JCR_URI, "requiredPrimaryTypes");

    //---------------------------------------------< node type name constants >

    /**
     * rep:root
     */
    public static final QName REP_ROOT = new QName(NS_REP_URI, "root");

    /**
     * rep:system
     */
    public static final QName REP_SYSTEM = new QName(NS_REP_URI, "system");

    /**
     * rep:versionStorage
     */
    public static final QName REP_VERSIONSTORAGE = new QName(NS_REP_URI, "versionStorage");

    /**
     * rep:versionStorage
     */
    public static final QName REP_NODETYPES = new QName(NS_REP_URI, "nodeTypes");

    /**
     * nt:unstructured
     */
    public static final QName NT_UNSTRUCTURED = new QName(NS_NT_URI, "unstructured");

    /**
     * nt:base
     */
    public static final QName NT_BASE = new QName(NS_NT_URI, "base");

    /**
     * nt:hierarchyNode
     */
    public static final QName NT_HIERARCHYNODE = new QName(NS_NT_URI, "hierarchyNode");

    /**
     * nt:resource
     */
    public static final QName NT_RESOURCE = new QName(NS_NT_URI, "resource");

    /**
     * nt:file
     */
    public static final QName NT_FILE = new QName(NS_NT_URI, "file");

    /**
     * nt:folder
     */
    public static final QName NT_FOLDER = new QName(NS_NT_URI, "folder");

    /**
     * nt:query
     */
    public static final QName NT_QUERY = new QName(NS_NT_URI, "query");

    /**
     * mix:referenceable
     */
    public static final QName MIX_REFERENCEABLE = new QName(NS_MIX_URI, "referenceable");

    /**
     * mix:referenceable
     */
    public static final QName MIX_LOCKABLE = new QName(NS_MIX_URI, "lockable");

    /**
     * mix:versionable
     */
    public static final QName MIX_VERSIONABLE = new QName(NS_MIX_URI, "versionable");

    /**
     * nt:versionHistory
     */
    public static final QName NT_VERSIONHISTORY = new QName(NS_NT_URI, "versionHistory");

    /**
     * nt:version
     */
    public static final QName NT_VERSION = new QName(NS_NT_URI, "version");

    /**
     * nt:versionLabels
     */
    public static final QName NT_VERSIONLABELS = new QName(NS_NT_URI, "versionLabels");

    /**
     * nt:versionedChild
     */
    public static final QName NT_VERSIONEDCHILD = new QName(NS_NT_URI, "versionedChild");

    /**
     * nt:frozenNode
     */
    public static final QName NT_FROZENNODE = new QName(NS_NT_URI, "frozenNode");

    /**
     * nt:nodeType
     */
    public static final QName NT_NODETYPE = new QName(NS_NT_URI, "nodeType");

    /**
     * nt:propertyDefinition
     */
    public static final QName NT_PROPERTYDEFINITION = new QName(NS_NT_URI, "propertyDefinition");

    /**
     * nt:childNodeDefinition
     */
    public static final QName NT_CHILDNODEDEFINITION = new QName(NS_NT_URI, "childNodeDefinition");

    //------------------------------------------< system view name constants >

    /**
     * sv:node
     */
    public static final QName SV_NODE = new QName(NS_SV_URI, "node");

    /**
     * sv:property
     */
    public static final QName SV_PROPERTY = new QName(NS_SV_URI, "property");

    /**
     * sv:value
     */
    public static final QName SV_VALUE = new QName(NS_SV_URI, "value");

    /**
     * sv:type
     */
    public static final QName SV_TYPE = new QName(NS_SV_URI, "type");

    /**
     * sv:name
     */
    public static final QName SV_NAME = new QName(NS_SV_URI, "name");

    /** Serialization UID of this class. */
    static final long serialVersionUID = -2712313010017755368L;

    public static final QName[] EMPTY_ARRAY = new QName[0];

    /** The memorized hash code of this qualified name. */
    private transient int hash;

    /** The memorized string representation of this qualified name. */
    private transient String string;

    /** The internalized namespace URI of this qualified name. */
    private final String namespaceURI;

    /** The local part of this qualified name. */
    private final String localName;

    /**
     * Creates a new qualified name with the given namespace URI and
     * local part.
     * <p/>
     * Note that the format of the local part is not validated. The format
     * can be checked by calling {@link NameFormat#checkFormat(String)}.
     *
     * @param namespaceURI namespace uri
     * @param localName local part
     * @throws IllegalArgumentException if <code>localName</code> is invalid.
     */
    public QName(String namespaceURI, String localName) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException("invalid namespaceURI specified");
        }
        // an empty localName is valid though (e.g. the root node name)
        if (localName == null) {
            throw new IllegalArgumentException("invalid localName specified");
        }
        // internalize namespaceURI to improve performance of QName comparisons.
        // Please note that we do *not* internalize localName since this could
        // blow perm space for large repositories
        this.namespaceURI = namespaceURI.intern();
        this.localName = localName;
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
     * @deprecated Use {@link NameFormat#parse(String, NamespaceResolver)} instead.
     */
    public static QName fromJCRName(String rawName, NamespaceResolver resolver)
            throws IllegalNameException, UnknownPrefixException {
        return NameFormat.parse(rawName, resolver);
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
     * JCR-style name.
     * @deprecated Use {@link NameFormat#checkFormat(String)} instead.
     */
    public static void checkFormat(String jcrName) throws IllegalNameException {
        NameFormat.checkFormat(jcrName);
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
     * @deprecated Use {@link NameFormat#format(QName, NamespaceResolver)}
     * instead.
     */
    public String toJCRName(NamespaceResolver resolver)
            throws NoPrefixDeclaredException {
        return NameFormat.format(this, resolver);
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
     * @deprecated Use {@link NameFormat#format(QName, NamespaceResolver, StringBuffer)}
     * instead.
     */
    public void toJCRName(NamespaceResolver resolver, StringBuffer buf)
            throws NoPrefixDeclaredException {
        NameFormat.format(this, resolver, buf);
    }

    //---------------------------------------------------------------< Object >
    /**
     * Returns the string representation of this <code>QName</code> in the
     * following format:
     * <p/>
     * <code><b>{</b>namespaceURI<b>}</b>localName</code>
     *
     * @return the string representation of this <code>QName</code>.
     * @see #valueOf(String)
     * @see Object#toString()
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
            // we can use == operator for namespaceURI since it is internalized
            return namespaceURI == other.namespaceURI
                    && localName.equals(other.localName);
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

    //------------------------------------------------------------< Cloneable >
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

    //-----------------------------------------------------------< Comparable >
    /**
     * Compares two qualified names.
     *
     * @param o the object to compare this qualified name with
     * @return comparison result
     * @throws ClassCastException if the given object is not a qualified name
     * @see Comparable#compareTo(Object)
     */
    public int compareTo(Object o) {
        if (this == o) {
            return 0;
        }

        QName other = (QName) o;
        // we can use == operator for namespaceURI since it is internalized
        if (namespaceURI == other.namespaceURI) {
            return localName.compareTo(other.localName);
        } else {
            return namespaceURI.compareTo(other.namespaceURI);
        }
    }

    //-------------------------------------------------< Serializable support >
    /**
     * Creates a new <code>QName</code> instance using the proper constructor
     * during deserialization in order to make sure that internalized strings
     * are used where appropriate.
     */
    private Object readResolve() {
        return new QName(namespaceURI, localName);
    }
}
