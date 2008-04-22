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
package org.apache.jackrabbit.core.nodetype.compact;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.ValueConstraint;
import org.apache.jackrabbit.core.nodetype.ItemDef;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

/**
 * Prints node type defs in a compact notation
 * Print Format:
 * <ex = "http://apache.org/jackrabbit/example">
 * [ex:NodeType] > ex:ParentType1, ex:ParentType2
 * orderable mixin
 *   - ex:property (STRING) = 'default1', 'default2'
 *     primary mandatory autocreated protected multiple VERSION
 *     < 'constraint1', 'constraint2'
 *   + ex:node (ex:reqType1, ex:reqType2) = ex:defaultType
 *     mandatory autocreated protected multiple VERSION
 */
public class CompactNodeTypeDefWriter {

    /**
     * the indention string
     */
    private static final String INDENT = "  ";

    /**
     * the current namespace resolver
     */
    private final NamespaceResolver resolver;

    /**
     * the current name/path resolver
     */
    private final NamePathResolver npResolver;

    /**
     * the underlying writer
     */
    private Writer out;

    /**
     * special writer used for namespaces
     */
    private Writer nsWriter;

    /**
     * namespaces(prefixes) that are used
     */
    private HashSet usedNamespaces = new HashSet();

    /**
     * Creates a new nodetype writer
     *
     * @param out the underlying writer
     * @param r the namespace resolver
     * @param npResolver
     */
    public CompactNodeTypeDefWriter(Writer out, NamespaceResolver r, NamePathResolver npResolver) {
        this(out, r, npResolver, false);
    }

    /**
     * Creates a new nodetype writer
     *
     * @param out the underlaying writer
     * @param r the naespace resolver
     * @param npResolver
     * @param includeNS if <code>true</code> all used namespace decl. are also
     */
    public CompactNodeTypeDefWriter(Writer out, NamespaceResolver r, NamePathResolver npResolver, boolean includeNS) {
        this.resolver = r;
        this.npResolver = npResolver;
        if (includeNS) {
            this.out = new StringWriter();
            this.nsWriter = out;
        } else {
            this.out = out;
            this.nsWriter = null;
        }
    }

    /**
     * Writes the given list of NodeTypeDefs to the output writer including the
     * used namespaces.
     *
     * @param l
     * @param r
     * @param npResolver
     * @param out
     * @throws IOException
     */
    public static void write(List l, NamespaceResolver r, NamePathResolver npResolver, Writer out)
            throws IOException {
        CompactNodeTypeDefWriter w = new CompactNodeTypeDefWriter(out, r, npResolver, true);
        Iterator iter = l.iterator();
        while (iter.hasNext()) {
            NodeTypeDef def = (NodeTypeDef) iter.next();
            w.write(def);
        }
        w.close();
    }

    /**
     * Write one NodeTypeDef to this writer
     *
     * @param d
     * @throws IOException
     */
    public void write(NodeTypeDef d) throws IOException {
        writeName(d);
        writeSupertypes(d);
        writeOptions(d);
        writePropDefs(d);
        writeNodeDefs(d);
        out.write("\n\n");
    }

    /**
     * Flushes all pending write operations and Closes this writer. please note,
     * that the underlying writer remains open.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        if (nsWriter != null) {
            nsWriter.write("\n");
            out.close();
            nsWriter.write(((StringWriter) out).getBuffer().toString());
            out = nsWriter;
            nsWriter = null;
        }
        out.flush();
        out = null;
    }

    /**
     * write name
     */
    private void writeName(NodeTypeDef ntd) throws IOException {
        out.write("[");
        out.write(resolve(ntd.getName()));
        out.write("]");
    }

    /**
     * write supertypes
     */
    private void writeSupertypes(NodeTypeDef ntd) throws IOException {
        Name[] sta = ntd.getSupertypes();
        String delim = " > ";
        for (int i = 0; i < sta.length; i++) {
            out.write(delim);
            out.write(resolve(sta[i]));
            delim = ", ";
        }
    }

    /**
     * write options
     */
    private void writeOptions(NodeTypeDef ntd) throws IOException {
        if (ntd.hasOrderableChildNodes()) {
            out.write("\n" + INDENT);
            out.write("orderable");
            if (ntd.isMixin()) {
                out.write(" mixin");
            }
        } else if (ntd.isMixin()) {
            out.write("\n" + INDENT);
            out.write("mixin");
        }
    }

    /**
     * write prop defs
     */
    private void writePropDefs(NodeTypeDef ntd) throws IOException {
        PropDef[] pda = ntd.getPropertyDefs();
        for (int i = 0; i < pda.length; i++) {
            PropDef pd = pda[i];
            writePropDef(ntd, pd);
        }
    }

    /**
     * write node defs
     */
    private void writeNodeDefs(NodeTypeDef ntd) throws IOException {
        NodeDef[] nda = ntd.getChildNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            NodeDef nd = nda[i];
            writeNodeDef(ntd, nd);
        }
    }

    /**
     * write prop def
     * @param pd
     */
    private void writePropDef(NodeTypeDef ntd, PropDef pd) throws IOException {
        out.write("\n" + INDENT + "- ");


        Name name = pd.getName();
        if (name.equals(ItemDef.ANY_NAME)) {
            out.write('*');
        } else {
            writeItemDefName(name);
        }
        out.write(" (");
        out.write(PropertyType.nameFromValue(pd.getRequiredType()).toLowerCase());
        out.write(")");
        writeDefaultValues(pd.getDefaultValues());
        out.write(ntd.getPrimaryItemName() != null && ntd.getPrimaryItemName().equals(pd.getName()) ? " primary" : "");
        if (pd.isMandatory()) {
            out.write(" mandatory");
        }
        if (pd.isAutoCreated()) {
            out.write(" autocreated");
        }
        if (pd.isProtected()) {
            out.write(" protected");
        }
        if (pd.isMultiple()) {
            out.write(" multiple");
        }
        if (pd.getOnParentVersion() != OnParentVersionAction.COPY) {
            out.write(" ");
            out.write(OnParentVersionAction.nameFromValue(pd.getOnParentVersion()).toLowerCase());
        }
        writeValueConstraints(pd.getValueConstraints());
    }

    /**
     * write default values
     * @param dva
     */
    private void writeDefaultValues(InternalValue[] dva) throws IOException {
        if (dva != null && dva.length > 0) {
            String delim = " = '";
            for (int i = 0; i < dva.length; i++) {
                out.write(delim);
                try {
                    out.write(escape(dva[i].toJCRValue(npResolver).getString()));
                } catch (RepositoryException e) {
                    out.write(escape(dva[i].toString()));
                }
                out.write("'");
                delim = ", '";
            }
        }
    }

    /**
     * write value constraints
     * @param vca
     */
    private void writeValueConstraints(ValueConstraint[] vca) throws IOException {
        if (vca != null && vca.length > 0) {
            String vc = vca[0].getDefinition(npResolver);
            out.write(" < '");
            out.write(escape(vc));
            out.write("'");
            for (int i = 1; i < vca.length; i++) {
                vc = vca[i].getDefinition(npResolver);
                out.write(", '");
                out.write(escape(vc));
                out.write("'");
            }
        }
    }

    /**
     * write node def
     * @param nd
     */
    private void writeNodeDef(NodeTypeDef ntd, NodeDef nd) throws IOException {
        out.write("\n" + INDENT + "+ ");

        Name name = nd.getName();
        writeItemDefName(name);
        writeRequiredTypes(nd.getRequiredPrimaryTypes());
        writeDefaultType(nd.getDefaultPrimaryType());
        out.write(ntd.getPrimaryItemName() != null && ntd.getPrimaryItemName().equals(nd.getName()) ? " primary" : "");
        if (nd.isMandatory()) {
            out.write(" mandatory");
        }
        if (nd.isAutoCreated()) {
            out.write(" autocreated");
        }
        if (nd.isProtected()) {
            out.write(" protected");
        }
        if (nd.allowsSameNameSiblings()) {
            out.write(" multiple");
        }
        if (nd.getOnParentVersion() != OnParentVersionAction.COPY) {
            out.write(" ");
            out.write(OnParentVersionAction.nameFromValue(nd.getOnParentVersion()).toLowerCase());
        }
    }

    /**
     * Write item def name
     * @param name
     * @throws IOException
     */
    private void writeItemDefName(Name name) throws IOException {
        if (name.equals(ItemDef.ANY_NAME)) {
            out.write('*');
        } else {
            out.write(resolve(name));
        }
    }
    /**
     * write required types
     * @param reqTypes
     */
    private void writeRequiredTypes(Name[] reqTypes) throws IOException {
        if (reqTypes != null && reqTypes.length > 0) {
            String delim = " (";
            for (int i = 0; i < reqTypes.length; i++) {
                out.write(delim);
                out.write(resolve(reqTypes[i]));
                delim = ", ";
            }
            out.write(")");
        }
    }

    /**
     * write default types
     * @param defType
     */
    private void writeDefaultType(Name defType) throws IOException {
        if (defType != null && !defType.getLocalName().equals("*")) {
            out.write(" = ");
            out.write(resolve(defType));
        }
    }

    /**
     * resolve
     * @param qname
     * @return the resolved name
     */
    private String resolve(Name qname) throws IOException {
        if (qname == null) {
            return "";
        }
        try {
            String prefix = resolver.getPrefix(qname.getNamespaceURI());
            // check for writing namespaces
            if (nsWriter != null) {
                if (!usedNamespaces.contains(prefix)) {
                    usedNamespaces.add(prefix);
                    nsWriter.write("<'");
                    nsWriter.write(prefix);
                    nsWriter.write("'='");
                    nsWriter.write(escape(qname.getNamespaceURI()));
                    nsWriter.write("'>\n");
                }
            }
            if (!prefix.equals(Name.NS_EMPTY_PREFIX)) {
                prefix += ":";
            }

            String encLocalName = ISO9075.encode(qname.getLocalName());
            String resolvedName = prefix + encLocalName;

            // check for '-' and '+'
            if (resolvedName.indexOf('-') >= 0 || resolvedName.indexOf('+') >= 0) {
                return "'" + resolvedName + "'";
            } else {
                return resolvedName;
            }

        } catch (NamespaceException e) {
            return qname.toString();
        }
    }

    /**
     * escape
     * @param s
     * @return the escaped string
     */
    private String escape(String s) {
        StringBuffer sb = new StringBuffer(s);
        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '\\') {
                sb.insert(i, '\\');
                i++;
            } else if (sb.charAt(i) == '\'') {
                sb.insert(i, '\'');
                i++;
            }
        }
        return sb.toString();
    }
}
