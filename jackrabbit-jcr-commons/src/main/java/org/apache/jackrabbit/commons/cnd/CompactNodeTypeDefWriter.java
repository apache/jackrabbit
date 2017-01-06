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
package org.apache.jackrabbit.commons.cnd;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.query.qom.Operator;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.util.Text;

import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.qom.QueryObjectModelConstants;
import javax.jcr.version.OnParentVersionAction;
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

/**
 * Prints node type defs in a compact notation
 * Print Format:
 * &lt;ex = "http://apache.org/jackrabbit/example"&gt;
 * [ex:NodeType] &gt; ex:ParentType1, ex:ParentType2
 * orderable mixin
 *   - ex:property (STRING) = 'default1', 'default2'
 *     primary mandatory autocreated protected multiple VERSION
 *     &lt; 'constraint1', 'constraint2'
 *   + ex:node (ex:reqType1, ex:reqType2) = ex:defaultType
 *     mandatory autocreated protected multiple VERSION
 */
public class CompactNodeTypeDefWriter {

    /**
     * the indention string
     */
    private static final String INDENT = "  ";

    private static final String ANY = "*";

    /**
     * Helper to retrieve the namespace URI for a given prefix.
     */
    private final NamespaceMapping nsMapping;

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
     * @param out the underlying writer
     * @param session repository session
     * @param includeNS if <code>true</code> all used namespace decl. are also
     *                  written to the writer
     */
    public CompactNodeTypeDefWriter(Writer out, final Session session, boolean includeNS) {
        this(out, new DefaultNamespaceMapping(session), includeNS);
    }

    /**
     * Creates a new nodetype writer based on a session
     *
     * @param out the underlying writer
     * @param nsMapping the mapping from prefix to namespace URI.
     * @param includeNS if <code>true</code> all used namespace decl. are also
     *                  written to the writer
     */
    public CompactNodeTypeDefWriter(Writer out, NamespaceMapping nsMapping, boolean includeNS) {
        this.nsMapping = nsMapping;
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
     * @param session session
     * @param out output writer
     * @throws java.io.IOException if an I/O error occurs
     */
    public static void write(Collection<NodeTypeDefinition> defs,
                             Session session, Writer out) throws IOException {
        CompactNodeTypeDefWriter w = new CompactNodeTypeDefWriter(out, session, true);
        for (NodeTypeDefinition def : defs) {
            w.write(def);
        }
        w.close();
    }

    /**
     * Writes the given list of QNodeTypeDefinition to the output writer
     * including the used namespaces.
     *
     * @param defs collection of definitions
     * @param nsMapping the mapping from prefix to namespace URI.
     * @param out output writer
     * @throws java.io.IOException if an I/O error occurs
     */
    public static void write(Collection<NodeTypeDefinition> defs,
                             NamespaceMapping nsMapping, Writer out) throws IOException {
        CompactNodeTypeDefWriter w = new CompactNodeTypeDefWriter(out, nsMapping, true);
        for (NodeTypeDefinition def : defs) {
            w.write(def);
        }
        w.close();
    }

    /**
     * Write one NodeTypeDefinition to this writer
     *
     * @param ntd node type definition
     * @throws IOException if an I/O error occurs
     */
    public void write(NodeTypeDefinition ntd) throws IOException {
        writeName(ntd);
        writeSupertypes(ntd);
        writeOptions(ntd);
        PropertyDefinition[] pdefs = ntd.getDeclaredPropertyDefinitions();
        if (pdefs != null) {
            for (PropertyDefinition pd : pdefs) {
                writePropDef(pd);
            }
        }
        NodeDefinition[] ndefs = ntd.getDeclaredChildNodeDefinitions();
        if (ndefs != null) {
            for (NodeDefinition nd : ndefs) {
                writeNodeDef(nd);
            }
        }
        out.write("\n\n");
    }

    /**
     * Write a namespace declaration to this writer. Note, that this method
     * has no effect if there is no extra namespace write present.
     *
     * @param prefix  namespace prefix
     * @throws IOException  if an I/O error occurs
     */
    public void writeNamespaceDeclaration(String prefix) throws IOException {
        if (nsWriter != null && !usedNamespaces.contains(prefix)) {
            usedNamespaces.add(prefix);
            nsWriter.write("<'");
            nsWriter.write(prefix);
            nsWriter.write("'='");
            nsWriter.write(escape(nsMapping.getNamespaceURI(prefix)));
            nsWriter.write("'>\n");
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

    //------------------------------------------------------------< private >---
    /**
     * write name
     * @param ntd node type definition
     * @throws IOException if an I/O error occurs
     */
    private void writeName(NodeTypeDefinition ntd) throws IOException {
        out.write(Lexer.BEGIN_NODE_TYPE_NAME);
        writeJcrName(ntd.getName());
        out.write(Lexer.END_NODE_TYPE_NAME);
    }

    /**
     * Write the super type definitions.
     * 
     * @param ntd node type definition
     * @throws IOException if an I/O error occurs
     */
    private void writeSupertypes(NodeTypeDefinition ntd) throws IOException {
        // get ordered list of supertypes, omitting nt:base
        TreeSet<String> supertypes = new TreeSet<String>();
        for (String name : ntd.getDeclaredSupertypeNames()) {
            if (!name.equals(JcrConstants.NT_BASE)) {
                supertypes.add(name);
            }
        }
        if (!supertypes.isEmpty()) {
            String delim = " " + Lexer.EXTENDS + " ";
            for (String name : supertypes) {
                out.write(delim);
                writeJcrName(name);
                delim = ", ";
            }
        }
    }

    /**
     * Write the options
     * 
     * @param ntd node type definition
     * @throws IOException if an I/O error occurs
     */
    private void writeOptions(NodeTypeDefinition ntd) throws IOException {
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
        String pin = ntd.getPrimaryItemName();
        if (pin != null) {
            options.add(Lexer.PRIMARYITEM[0]);
        }
        for (int i = 0; i < options.size(); i++) {
            if (i == 0) {
                out.write("\n" + INDENT);
            } else {
                out.write(" ");
            }
            out.write(options.get(i));
        }

        if (pin != null) {
            out.write(" ");
            writeJcrName(pin);            
        }
    }

    /**
     * Write a property definition
     * 
     * @param pd property definition
     * @throws IOException if an I/O error occurs
     */
    private void writePropDef(PropertyDefinition pd) throws IOException {
        out.write("\n" + INDENT + Lexer.PROPERTY_DEFINITION + " ");

        writeJcrName(pd.getName());
        out.write(" ");
        out.write(Lexer.BEGIN_TYPE);
        out.write(PropertyType.nameFromValue(pd.getRequiredType()).toLowerCase());
        out.write(Lexer.END_TYPE);
        writeDefaultValues(pd.getDefaultValues());
        if (pd.isMandatory()) {
            out.write(" ");
            out.write(Lexer.MANDATORY[0]);
        }
        if (pd.isAutoCreated()) {
            out.write(" ");
            out.write(Lexer.AUTOCREATED[0]);
        }
        if (pd.isProtected()) {
            out.write(" ");
            out.write(Lexer.PROTECTED[0]);
        }
        if (pd.isMultiple()) {
            out.write(" ");
            out.write(Lexer.MULTIPLE[0]);
        }
        if (pd.getOnParentVersion() != OnParentVersionAction.COPY) {
            out.write(" ");
            out.write(OnParentVersionAction.nameFromValue(pd.getOnParentVersion()).toLowerCase());
        }
        if (!pd.isFullTextSearchable()) {
            out.write(" ");
            out.write(Lexer.NOFULLTEXT[0]);
        }
        if (!pd.isQueryOrderable()) {
            out.write(" ");
            out.write(Lexer.NOQUERYORDER[0]);
        }
        String[] qops = pd.getAvailableQueryOperators();
        if (qops != null && qops.length > 0) {
            List<String> opts = new ArrayList<String>(Arrays.asList(qops));
            List<String> defaultOps = Arrays.asList(Operator.getAllQueryOperators());
            if (!opts.containsAll(defaultOps)) {
                out.write(" ");
                out.write(Lexer.QUERYOPS[0]);
                out.write(" '");
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
     * Write default values
     * 
     * @param dva default value
     * @throws IOException if an I/O error occurs
     */
    private void writeDefaultValues(Value[] dva) throws IOException {
        if (dva != null && dva.length > 0) {
            String delim = " = '";
            for (Value value : dva) {
                out.write(delim);
                try {
                    out.write(escape(value.getString()));
                } catch (RepositoryException e) {
                    out.write(escape(value.toString()));
                }
                out.write("'");
                delim = ", '";
            }
        }
    }

    /**
     * Write the value constraints
     *
     * @param constraints the value constraints
     * @param type value type
     * @throws IOException if an I/O error occurs
     */
    private void writeValueConstraints(String[] constraints, int type) throws IOException {
        if (constraints != null && constraints.length > 0) {
            out.write(" ");
            out.write(Lexer.CONSTRAINT);
            out.write(" '");
            out.write(escape(constraints[0]));
            out.write("'");
            for (int i = 1; i < constraints.length; i++) {
                out.write(", '");
                out.write(escape(constraints[i]));
                out.write("'");
            }
        }
    }

    /**
     * Write a child node definition
     *
     * @param nd node definition
     * @throws IOException if an I/O error occurs
     */
    private void writeNodeDef(NodeDefinition nd) throws IOException {
        out.write("\n" + INDENT + Lexer.CHILD_NODE_DEFINITION + " ");

        writeJcrName(nd.getName());
        writeRequiredTypes(nd.getRequiredPrimaryTypeNames());
        writeDefaultType(nd.getDefaultPrimaryTypeName());
        if (nd.isMandatory()) {
            out.write(" ");
            out.write(Lexer.MANDATORY[0]);
        }
        if (nd.isAutoCreated()) {
            out.write(" ");
            out.write(Lexer.AUTOCREATED[0]);
        }
        if (nd.isProtected()) {
            out.write(" ");
            out.write(Lexer.PROTECTED[0]);
        }
        if (nd.allowsSameNameSiblings()) {
            out.write(" ");
            out.write(Lexer.MULTIPLE[0]);
        }
        if (nd.getOnParentVersion() != OnParentVersionAction.COPY) {
            out.write(" ");
            out.write(OnParentVersionAction.nameFromValue(nd.getOnParentVersion()).toLowerCase());
        }
    }
    
    /**
     * write required types
     * @param reqTypes required type names
     * @throws IOException if an I/O error occurs
     */
    private void writeRequiredTypes(String[] reqTypes) throws IOException {
        if (reqTypes != null && reqTypes.length > 0) {
            String delim = " " + Lexer.BEGIN_TYPE;
            for (String reqType : reqTypes) {
                out.write(delim);
                writeJcrName(reqType);
                delim = ", ";
            }
            out.write(Lexer.END_TYPE);
        }
    }

    /**
     * write default types
     * @param defType default type name
     * @throws IOException if an I/O error occurs
     */
    private void writeDefaultType(String defType) throws IOException {
        if (defType != null && !defType.equals("*")) {
            out.write(" = ");
            writeJcrName(defType);
        }
    }

    /**
     * Write the name and updated the namespace declarations if needed.
     * 
     * @param name name to write
     * @throws IOException if an I/O error occurs
     */
    private void writeJcrName(String name) throws IOException {
        if (name == null) {
            return;
        }

        String prefix = Text.getNamespacePrefix(name);
        if (!prefix.equals(NamespaceRegistry.PREFIX_EMPTY)) {
            // update namespace declaration
            writeNamespaceDeclaration(prefix);
            prefix += ":";
        }

        String localName = Text.getLocalName(name);
        String encLocalName = (ANY.equals(localName)) ? ANY : ISO9075.encode(Text.getLocalName(name));
        String resolvedName = prefix + encLocalName;

        // check for '-' and '+'
        boolean quotesNeeded = (name.indexOf('-') >= 0 || name.indexOf('+') >= 0);
        if (quotesNeeded) {
            out.write("'");
            out.write(resolvedName);
            out.write("'");
        } else {
            out.write(resolvedName);
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

    /**
     * Map namespace prefixes such as present in a qualified JCR name to
     * the corresponding namespace URI.
     */
    public interface NamespaceMapping {

        String getNamespaceURI(String prefix);
    }

    /**
     * Default implementation using <code>Session</code> to determine
     * the namespace URI.
     */
    private static class DefaultNamespaceMapping implements NamespaceMapping {

        private final Session session;

        private DefaultNamespaceMapping(Session session) {
            this.session = session;
        }

        public String getNamespaceURI(String prefix) {
            try {
                return session.getNamespaceURI(prefix);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }
    }
}