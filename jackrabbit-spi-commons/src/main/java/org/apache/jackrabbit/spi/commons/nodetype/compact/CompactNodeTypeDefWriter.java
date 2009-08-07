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
package org.apache.jackrabbit.spi.commons.nodetype.compact;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.value.QValueFactoryImpl;
import org.apache.jackrabbit.spi.commons.value.ValueFormat;
import org.apache.jackrabbit.util.ISO9075;

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
     * the current value factory
     */
    private final ValueFactory valueFactory;

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
    private final HashSet usedNamespaces = new HashSet();

    /**
     * Creates a new nodetype writer
     *
     * @param out the underlying writer
     * @param r the namespace resolver
     * @param npResolver
     * @param valueFactory
     */
    public CompactNodeTypeDefWriter(Writer out, NamespaceResolver r, NamePathResolver npResolver,
            ValueFactory valueFactory) {
        this(out, r, npResolver, valueFactory, false);
    }

    /**
     * Creates a new nodetype writer
     *
     * @param out the underlaying writer
     * @param r the naespace resolver
     * @param npResolver
     * @param valueFactory
     * @param includeNS if <code>true</code> all used namespace decl. are also
     */
    public CompactNodeTypeDefWriter(Writer out, NamespaceResolver r, NamePathResolver npResolver,
            ValueFactory valueFactory, boolean includeNS) {
        this.resolver = r;
        this.npResolver = npResolver;
        this.valueFactory = valueFactory;
        if (includeNS) {
            this.out = new StringWriter();
            this.nsWriter = out;
        } else {
            this.out = out;
            this.nsWriter = null;
        }
    }

    /**
     * Writes the given list of QNodeTypeDefinition to the output writer including the
     * used namespaces.
     *
     * @param l
     * @param r
     * @param npResolver
     * @param valueFactory
     * @param out
     * @throws IOException
     */
    public static void write(List l, NamespaceResolver r, NamePathResolver npResolver,
            ValueFactory valueFactory, Writer out)
            throws IOException {
        CompactNodeTypeDefWriter w = new CompactNodeTypeDefWriter(out, r, npResolver, valueFactory, true);
        Iterator iter = l.iterator();
        while (iter.hasNext()) {
            QNodeTypeDefinition def = (QNodeTypeDefinition) iter.next();
            w.write(def);
        }
        w.close();
    }

    /**
     * Write one QNodeTypeDefinition to this writer
     *
     * @param ntd
     * @throws IOException
     */
    public void write(QNodeTypeDefinition ntd) throws IOException {
        writeName(ntd);
        writeSupertypes(ntd);
        writeOptions(ntd);
        writePropDefs(ntd);
        writeNodeDefs(ntd);
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
    private void writeName(QNodeTypeDefinition ntd) throws IOException {
        out.write("[");
        out.write(resolve(ntd.getName()));
        out.write("]");
    }

    /**
     * write supertypes
     */
    private void writeSupertypes(QNodeTypeDefinition ntd) throws IOException {
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
    private void writeOptions(QNodeTypeDefinition ntd) throws IOException {
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
    private void writePropDefs(QNodeTypeDefinition ntd) throws IOException {
        QPropertyDefinition[] pda = ntd.getPropertyDefs();
        for (int i = 0; i < pda.length; i++) {
            QPropertyDefinition pd = pda[i];
            writePropDef(ntd, pd);
        }
    }

    /**
     * write node defs
     */
    private void writeNodeDefs(QNodeTypeDefinition ntd) throws IOException {
        QNodeDefinition[] nda = ntd.getChildNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            QNodeDefinition nd = nda[i];
            writeNodeDef(ntd, nd);
        }
    }

    /**
     * write prop def
     * @param pd
     */
    private void writePropDef(QNodeTypeDefinition ntd, QPropertyDefinition pd) throws IOException {
        out.write("\n" + INDENT + "- ");

        Name name = pd.getName();
        if (name.equals(NameConstants.ANY_NAME)) {
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
        writeValueConstraints(pd.getValueConstraints(), pd.getRequiredType());
    }

    /**
     * write default values
     * @param dva
     */
    private void writeDefaultValues(QValue[] dva) throws IOException {
        if (dva != null && dva.length > 0) {
            String delim = " = '";
            for (int i = 0; i < dva.length; i++) {
                out.write(delim);
                try {
                    Value v = ValueFormat.getJCRValue(dva[i], npResolver, valueFactory);
                    out.write(escape(v.getString()));
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
    private void writeValueConstraints(String[] vca, int type) throws IOException {
        if (vca != null && vca.length > 0) {
            String vc = convertConstraint(vca[0], type);
            out.write(" < '");
            out.write(escape(vc));
            out.write("'");
            for (int i = 1; i < vca.length; i++) {
                vc = convertConstraint(vca[i], type);
                out.write(", '");
                out.write(escape(vc));
                out.write("'");
            }
        }
    }

    private String convertConstraint(String vc, int type) {
        if (type == PropertyType.REFERENCE || type == PropertyType.NAME || type == PropertyType.PATH) {
            if (type == PropertyType.REFERENCE)
                type = PropertyType.NAME;

            try {
                QValue qv = QValueFactoryImpl.getInstance().create(vc, type);
                vc = ValueFormat.getJCRValue(qv, npResolver, valueFactory).getString();
            }
            catch (RepositoryException e) {
                // ignore -> return unconverted constraint
            }
        }

        return vc;
    }

    /**
     * write node def
     *
     * @param nd
     */
    private void writeNodeDef(QNodeTypeDefinition ntd, QNodeDefinition nd) throws IOException {
        out.write("\n" + INDENT + "+ ");

        Name name = nd.getName();
        if (name.equals(NameConstants.ANY_NAME)) {
            out.write('*');
        } else {
            writeItemDefName(name);
        }
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
        out.write(resolve(name));
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
     * @param name
     * @return the resolved name
     */
    private String resolve(Name name) throws IOException {
        if (name == null) {
            return "";
        }
        try {
            String prefix = resolver.getPrefix(name.getNamespaceURI());
            if (prefix != null && !prefix.equals(Name.NS_EMPTY_PREFIX)) {
                // check for writing namespaces
                if (nsWriter != null) {
                    if (!usedNamespaces.contains(prefix)) {
                        usedNamespaces.add(prefix);
                        nsWriter.write("<'");
                        nsWriter.write(prefix);
                        nsWriter.write("'='");
                        nsWriter.write(escape(name.getNamespaceURI()));
                        nsWriter.write("'>\n");
                    }
                }
                prefix += ":";
            }

            String encLocalName = ISO9075.encode(name.getLocalName());
            String resolvedName = prefix + encLocalName;

            // check for '-' and '+'
            if (resolvedName.indexOf('-') >= 0 || resolvedName.indexOf('+') >= 0) {
                return "'" + resolvedName + "'";
            } else {
                return resolvedName;
            }

        } catch (NamespaceException e) {
            return name.toString();
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
