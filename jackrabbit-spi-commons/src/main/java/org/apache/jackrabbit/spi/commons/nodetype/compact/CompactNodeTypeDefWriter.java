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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.version.OnParentVersionAction;

import org.apache.jackrabbit.commons.cnd.Lexer;
import org.apache.jackrabbit.commons.query.qom.Operator;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QNodeTypeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueConstraint;
import org.apache.jackrabbit.spi.commons.QNodeTypeDefinitionImpl;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.SessionNamespaceResolver;
import org.apache.jackrabbit.spi.commons.nodetype.InvalidConstraintException;
import org.apache.jackrabbit.spi.commons.nodetype.constraint.ValueConstraint;
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
    private final Set<String> usedNamespaces = new HashSet<String>();

    /**
     * Creates a new nodetype writer based on a session
     *
     * @param out the underlaying writer
     * @param s repository session
     * @param includeNS if <code>true</code> all used namespace decl. are also
     *                  written to the writer
     */
    public CompactNodeTypeDefWriter(Writer out, Session s, boolean includeNS) {
        this(out, new SessionNamespaceResolver(s), new DefaultNamePathResolver(s), includeNS);
    }

    /**
     * Creates a new nodetype writer based on a namespace resolver
     *
     * @param out the underlaying writer
     * @param r the naespace resolver
     * @param includeNS if <code>true</code> all used namespace decl. are also
     *                  written to the writer
     */
    public CompactNodeTypeDefWriter(Writer out, NamespaceResolver r, boolean includeNS) {
        this(out, r, new DefaultNamePathResolver(r), includeNS);
    }

    /**
     * Creates a new nodetype writer that does not include namepsaces.
     *
     * @param out the underlaying writer
     * @param r the naespace resolver
     * @param npResolver name-path resolver
     */
    public CompactNodeTypeDefWriter(Writer out,
                                    NamespaceResolver r,
                                    NamePathResolver npResolver) {
        this(out, r, npResolver, false);
    }

    /**
     * Creates a new nodetype writer
     *
     * @param out the underlying writer
     * @param r the namespace resolver
     * @param npResolver name-path resolver
     * @param includeNS if <code>true</code> all used namespace decl. are also
     *                  written to the writer
     */
    public CompactNodeTypeDefWriter(Writer out,
                                    NamespaceResolver r,
                                    NamePathResolver npResolver,
                                    boolean includeNS) {
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
     * Writes the given list of QNodeTypeDefinition to the output writer including the
     * used namespaces.
     *
     * @param defs collection of definitions
     * @param r namespace resolver
     * @param npResolver name-path resolver
     * @param out output writer
     * @throws IOException if an I/O error occurs
     */
    public static void write(Collection<? extends QNodeTypeDefinition> defs,
                             NamespaceResolver r,
                             NamePathResolver npResolver,
                             Writer out)
            throws IOException {
        CompactNodeTypeDefWriter w = new CompactNodeTypeDefWriter(out, r, npResolver, true);
        for (QNodeTypeDefinition def : defs) {
            w.write(def);
        }
        w.close();
    }

    /**
     * Write one QNodeTypeDefinition to this writer
     *
     * @param ntd node type definition
     * @throws IOException if an I/O error occurs
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
     * Write a collection of QNodeTypeDefinitions to this writer
     *
     * @param defs node type definitions
     * @throws IOException if an I/O error occurs
     */
    public void write(Collection<? extends QNodeTypeDefinition> defs) throws IOException {
        for (QNodeTypeDefinition def : defs) {
            write(def);
        }
    }

    /**
     * Write one NodeTypeDefinition to this writer
     *
     * @param nt node type definition
     * @throws IOException if an I/O error occurs
     */
    public void write(NodeTypeDefinition nt) throws IOException {
        try {
            write(new QNodeTypeDefinitionImpl(nt, npResolver, QValueFactoryImpl.getInstance()));
        } catch (RepositoryException e) {
            throw new IOException("Error during internal conversion of nodetype definition:" + e.toString());
        }
    }

    /**
     * Flushes all pending write operations and Closes this writer. please note,
     * that the underlying writer remains open.
     *
     * @throws IOException if an I/O error occurs
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
     * @param ntd node type definition
     * @throws IOException if an I/O error occurs
     */
    private void writeName(QNodeTypeDefinition ntd) throws IOException {
        out.write("[");
        out.write(resolve(ntd.getName()));
        out.write("]");
    }

    /**
     * write supertypes
     * @param ntd node type definition
     * @throws IOException if an I/O error occurs
     */
    private void writeSupertypes(QNodeTypeDefinition ntd) throws IOException {
        // get ordered list of supertypes, omitting nt:Base
        TreeSet<Name> supertypes = new TreeSet<Name>();
        for (Name name : ntd.getSupertypes()) {
            if (!name.equals(NameConstants.NT_BASE)) {
                supertypes.add(name);
            }
        }
        if (!supertypes.isEmpty()) {
            String delim = " > ";
            for (Name name : supertypes) {
                out.write(delim);
                out.write(resolve(name));
                delim = ", ";
            }
        }
    }

    /**
     * write options
     * @param ntd node type definition
     * @throws IOException if an I/O error occurs
     */
    private void writeOptions(QNodeTypeDefinition ntd) throws IOException {
        List<String> options = new LinkedList<String>();
        if (ntd.isAbstract()) {
            options.add(Lexer.ABSTRACT[0]);
        }
        if (ntd.hasOrderableChildNodes()) {
            options.add(Lexer.ORDERABLE[0]);
        }
        if (ntd.isMixin()) {
            options.add(Lexer.MIXIN[0]);
        }
        if (!ntd.isQueryable()) {
            options.add(Lexer.NOQUERY[0]);
        }
        if (ntd.getPrimaryItemName() != null) {
            options.add(Lexer.PRIMARYITEM[0]);
            options.add(resolve(ntd.getPrimaryItemName()));
        }
        for (int i = 0; i < options.size(); i++) {
            if (i == 0) {
                out.write("\n" + INDENT);
            } else {
                out.write(" ");
            }
            out.write(options.get(i));
        }
    }

    /**
     * write prop defs
     * @param ntd node type definition
     * @throws IOException if an I/O error occurs
     */
    private void writePropDefs(QNodeTypeDefinition ntd) throws IOException {
        for (QPropertyDefinition pd : ntd.getPropertyDefs()) {
            writePropDef(pd);
        }
    }

    /**
     * write node defs
     * @param ntd node type definition
     * @throws IOException if an I/O error occurs
     */
    private void writeNodeDefs(QNodeTypeDefinition ntd) throws IOException {
        for (QNodeDefinition nd : ntd.getChildNodeDefs()) {
            writeNodeDef(nd);
        }
    }

    /**
     * write prop def
     * @param pd property definition
     * @throws IOException if an I/O error occurs
     */
    private void writePropDef(QPropertyDefinition pd) throws IOException {
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
        if (!pd.isFullTextSearchable()) {
            out.write(" nofulltext");
        }
        if (!pd.isQueryOrderable()) {
            out.write(" noqueryorder");
        }
        String[] qops = pd.getAvailableQueryOperators();
        if (qops != null && qops.length > 0) {
            List<String> opts = new ArrayList<String>(Arrays.asList(qops));
            List<String> defaultOps = Arrays.asList(Operator.getAllQueryOperators());
            if (!opts.containsAll(defaultOps)) {
                out.write(" queryops '");
                String delim = "";
                for (String opt: opts) {
                    out.write(delim);
                    delim= ", ";
                    if (opt.equals(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO)) {
                        out.write(Lexer.QUEROPS_EQUAL);
                    } else if (opt.equals(QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO)) {
                        out.write(Lexer.QUEROPS_NOTEQUAL);
                    } else if (opt.equals(QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN)) {
                        out.write(Lexer.QUEROPS_GREATERTHAN);
                    } else if (opt.equals(QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO)) {
                        out.write(Lexer.QUEROPS_GREATERTHANOREQUAL);
                    } else if (opt.equals(QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN)) {
                        out.write(Lexer.QUEROPS_LESSTHAN);
                    } else if (opt.equals(QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO)) {
                        out.write(Lexer.QUEROPS_LESSTHANOREQUAL);
                    } else if (opt.equals(QueryObjectModelConstants.JCR_OPERATOR_LIKE)) {
                        out.write(Lexer.QUEROPS_LIKE);
                    }
                }
                out.write("'");
            }
        }
        writeValueConstraints(pd.getValueConstraints(), pd.getRequiredType());
    }

    /**
     * write default values
     * @param dva default value
     * @throws IOException if an I/O error occurs
     */
    private void writeDefaultValues(QValue[] dva) throws IOException {
        if (dva != null && dva.length > 0) {
            String delim = " = '";
            for (QValue value : dva) {
                out.write(delim);
                try {
                    String str = ValueFormat.getJCRString(value, npResolver);
                    out.write(escape(str));
                } catch (RepositoryException e) {
                    out.write(escape(value.toString()));
                }
                out.write("'");
                delim = ", '";
            }
        }
    }

    /**
     * write value constraints
     * @param vca value constraint
     * @param type value type
     * @throws IOException if an I/O error occurs
     */
    private void writeValueConstraints(QValueConstraint[] vca, int type) throws IOException {
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

    /**
     * Converts the constraint to a jcr value
     * @param vc value constraint string
     * @param type value type
     * @return converted value
     */
    private String convertConstraint(QValueConstraint vc, int type) {
        try {
            ValueConstraint c = ValueConstraint.create(type, vc.getString());
            return c.getDefinition(npResolver);
        } catch (InvalidConstraintException e) {
            // ignore -> return unconverted constraint
            return vc.getString();
        }
    }

    /**
     * write node def
     *
     * @param nd node definition
     * @throws IOException if an I/O error occurs
     */
    private void writeNodeDef(QNodeDefinition nd) throws IOException {
        out.write("\n" + INDENT + "+ ");

        Name name = nd.getName();
        if (name.equals(NameConstants.ANY_NAME)) {
            out.write('*');
        } else {
            writeItemDefName(name);
        }
        writeRequiredTypes(nd.getRequiredPrimaryTypes());
        writeDefaultType(nd.getDefaultPrimaryType());
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
     * @param name name
     * @throws IOException if an I/O error occurs
     */
    private void writeItemDefName(Name name) throws IOException {
        out.write(resolve(name));
    }
    /**
     * write required types
     * @param reqTypes required type names
     * @throws IOException if an I/O error occurs
     */
    private void writeRequiredTypes(Name[] reqTypes) throws IOException {
        if (reqTypes != null && reqTypes.length > 0) {
            String delim = " (";
            for (Name reqType : reqTypes) {
                out.write(delim);
                out.write(resolve(reqType));
                delim = ", ";
            }
            out.write(")");
        }
    }

    /**
     * write default types
     * @param defType default type name
     * @throws IOException if an I/O error occurs
     */
    private void writeDefaultType(Name defType) throws IOException {
        if (defType != null && !defType.getLocalName().equals("*")) {
            out.write(" = ");
            out.write(resolve(defType));
        }
    }

    /**
     * resolve
     * @param name name to resolve
     * @return the resolved name
     * @throws IOException if an I/O error occurs
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
     * @param s string
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
