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
package org.apache.jackrabbit.core.nodetype.compact;

import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.PropDef;
import org.apache.jackrabbit.core.nodetype.NodeDef;
import org.apache.jackrabbit.core.nodetype.ValueConstraint;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;

import javax.jcr.NamespaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.version.OnParentVersionAction;
import java.io.Writer;
import java.io.IOException;

/**
 * Prints node type defs in a compact notation
 * Print Format:
 * [ex:NodeType] > ex:ParentType1, ex:ParentType2
 * orderable mixin
 *   - ex:property (STRING) primary mandatory autocreated protected multiple VERSION
 *     = 'default1', 'default2'
 *     < 'constraint1', 'constraint2'
 *   + ex:node (ex:reqType1, ex:reqType2) = ex:defaultType mandatory autocreated protected multiple VERSION
 */
public class CompactNodeTypeDefWriter {

    /**
     * the indention string
     */
    private final static String INDENT = "  ";

    /**
     * the current namespace resolver
     */
    private final NamespaceResolver resolver;

    /**
     * the underlying writer
     */
    private final Writer out;

    /**
     *
     * @param out
     * @param r
     */
    public CompactNodeTypeDefWriter(Writer out, NamespaceResolver r) {
        this.out = out;
        this.resolver = r;
    }

    /**
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
    }

    /**
     * closes this writer but not the underlying stream
     *
     * @throws IOException
     */
    public void close() throws IOException {
        out.flush();
    }

    /**
     * putName
     */
    private void writeName(NodeTypeDef ntd) throws IOException {
        out.write("[");
        out.write(resolve(ntd.getName()));
        out.write("]");
    }

    /**
     * putSupertypes
     */
    private void writeSupertypes(NodeTypeDef ntd) throws IOException {
        QName[] sta = ntd.getSupertypes();
        if (sta == null) return;
        String delim=" > ";
        for (int i=0; i<sta.length; i++) {
            if (!sta[i].equals(QName.NT_BASE)) {
                out.write(delim);
                out.write(resolve(sta[i]));
                delim=", ";
            }
        }
    }

    /**
     * putOptions
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
     * putPropDefs
     */
    private void writePropDefs(NodeTypeDef ntd) throws IOException {
        PropDef[] pda = ntd.getPropertyDefs();
        for (int i = 0; i < pda.length; i++) {
            PropDef pd = pda[i];
            writePropDef(ntd, pd);
        }
    }

    /**
     * putNodeDefs
     */
    private void writeNodeDefs(NodeTypeDef ntd) throws IOException {
        NodeDef[] nda = ntd.getChildNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            NodeDef nd = nda[i];
            writeNodeDef(ntd, nd);
        }
    }

    /**
     * putPropDef
     * @param pd
     */
    private void writePropDef(NodeTypeDef ntd, PropDef pd) throws IOException {
        out.write("\n" + INDENT + "- ");
        writeItemDefName(pd.getName());
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
     * putDefaultValues
     * @param dva
     */
    private void writeDefaultValues(InternalValue[] dva) throws IOException {
        if (dva == null || dva.length == 0) return;
        String delim=" = '";
        for (int i = 0; i < dva.length; i++) {
            out.write(delim);
            try {
                out.write(escape(dva[i].toJCRValue(resolver).getString()));
            } catch (RepositoryException e) {
                out.write(escape(dva[i].toString()));
            }
            out.write("'");
            delim=", '";
        }
    }

    /**
     * putValueConstraints
     * @param vca
     */
    private void writeValueConstraints(ValueConstraint[] vca) throws IOException {
        if (vca == null || vca.length == 0) return;
        String vc = vca[0].getDefinition(resolver);
        out.write("\n" + INDENT + INDENT + "< '");
        out.write(escape(vc));
        out.write("'");
        for (int i = 1; i < vca.length; i++) {
            vc = vca[i].getDefinition(resolver);
            out.write(", '");
            out.write(escape(vc));
            out.write("'");
        }
    }

    /**
     * putNodeDef
     * @param nd
     */
    private void writeNodeDef(NodeTypeDef ntd, NodeDef nd) throws IOException {
        out.write("\n" + INDENT + "+ ");
        writeItemDefName(nd.getName());
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

    private void writeItemDefName(QName name) throws IOException {
        String s = resolve(name);
        // check for '-' and '+'
        if (s.indexOf('-') >= 0 || s.indexOf('+') >= 0) {
            out.write('\'');
            out.write(s);
            out.write('\'');
        } else {
            out.write(s);
        }
    }
    /**
     * putRequiredTypes
     * @param reqTypes
     */
    private void writeRequiredTypes(QName[] reqTypes) throws IOException {
        if (reqTypes != null && reqTypes.length > 0) {
            String delim = " (";
            for (int i = 0; i < reqTypes.length; i++) {
                out.write(delim);
                out.write(resolve(reqTypes[i]));
                delim=", ";
            }
            out.write(")");
        }
    }

    /**
     * putDefaultType
     * @param defType
     */
    private void writeDefaultType(QName defType) throws IOException {
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
    private String resolve(QName qname) {
        if (qname == null) {
            return "";
        }
        try {
            String prefix = resolver.getPrefix(qname.getNamespaceURI());
            if (prefix != null && !prefix.equals(QName.NS_EMPTY_PREFIX)) {
                prefix += ":";
            }
            return prefix + qname.getLocalName();
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
