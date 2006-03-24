/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
import java.util.*;

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
    private final static String INDENT = "  ";

    /**
     * The list of NodeTypeDefs to write
     */
    private final List nodeTypeDefList;

    /**
     * the current namespace resolver
     */
    private final NamespaceResolver resolver;

    /**
     * the underlying writer
     */
    private final Writer out;

    /**
     * The namespaces to be written out.
     */
    private final Map namespaceMap = new HashMap();

    /**
     *
     * @param l
     * @param r
     * @param w
     */
    public CompactNodeTypeDefWriter(List l, NamespaceResolver r, Writer w) throws NamespaceException {
        nodeTypeDefList = l;
        out = w;
        resolver = r;
        buildNamespaceMap();
    }

    /**
     *
     * @throws IOException
     */
    public void write() throws IOException, NamespaceException {
        for (Iterator i = namespaceMap.entrySet().iterator(); i.hasNext();){
            Map.Entry e = (Map.Entry)i.next();
            String prefix = (String)e.getKey();
            String uri = (String)e.getValue();
            out.write("<");
            out.write(prefix);
            out.write(" = \"");
            out.write(uri);
            out.write("\">\n");
        }
        for (Iterator i = nodeTypeDefList.iterator(); i.hasNext();){
            NodeTypeDef ntd = (NodeTypeDef)i.next();
            writeName(ntd);
            writeSupertypes(ntd);
            writeOptions(ntd);
            writePropDefs(ntd);
            writeChildNodeDefs(ntd);
        }
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
     * buildNamespaceMap
     */
    private void buildNamespaceMap() throws NamespaceException {
        for (Iterator i = nodeTypeDefList.iterator(); i.hasNext();){
            NodeTypeDef ntd = (NodeTypeDef)i.next();
            addNamespace(ntd.getName());
            addNamespace(ntd.getSupertypes());
            PropDef[] pda = ntd.getPropertyDefs();
            for (int j = 0; j < pda.length; j++){
                PropDef pd = pda[j];
                addNamespace(pd.getName());
            }

            NodeDef[] nda = ntd.getChildNodeDefs();
            for (int j = 0; j < nda.length; j++){
                NodeDef nd = nda[j];
                addNamespace(nd.getName());
                addNamespace(nd.getRequiredPrimaryTypes());
                addNamespace(nd.getDefaultPrimaryType());
            }
        }
    }

    private void addNamespace(QName qn) throws NamespaceException {
        String uri = qn.getNamespaceURI();
        String prefix = resolver.getPrefix(uri);
        namespaceMap.put(prefix, uri);
    }

    private void addNamespace(QName[] qna) throws NamespaceException {
        for(int i = 0; i < qna.length; i++){
            QName qn = qna[i];
            addNamespace(qn);
        }
    }

    /**
     * writeName
     */
    private void writeName(NodeTypeDef ntd) throws IOException, NamespaceException {
        out.write("[");
        out.write(resolve(ntd.getName()));
        out.write("]");
    }

    /**
     * writeSupertypes
     */
    private void writeSupertypes(NodeTypeDef ntd) throws IOException, NamespaceException {
        QName[] sta = ntd.getSupertypes();
        if (sta == null) return;
        String delim=" > ";
        for (int i=0; i<sta.length; i++) {
            if (!sta[i].equals(QName.NT_BASE)) {
                out.write(delim);
                out.write(resolve(sta[i]));
                delim = ", ";
            }
        }
    }

    /**
     * writeOptions
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
     * writePropDefs
     */
    private void writePropDefs(NodeTypeDef ntd) throws IOException, NamespaceException {
        PropDef[] pda = ntd.getPropertyDefs();
        for (int i = 0; i < pda.length; i++) {
            PropDef pd = pda[i];
            writePropDef(ntd, pd);
        }
    }

    /**
     * writeNodeDefs
     */
    private void writeChildNodeDefs(NodeTypeDef ntd) throws IOException, NamespaceException {
        NodeDef[] nda = ntd.getChildNodeDefs();
        for (int i = 0; i < nda.length; i++) {
            NodeDef nd = nda[i];
            writeNodeDef(ntd, nd);
        }
    }

    /**
     * writePropDef
     * @param pd
     */
    private void writePropDef(NodeTypeDef ntd, PropDef pd) throws IOException, NamespaceException {
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
     * writeDefaultValues
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
     * writeValueConstraints
     * @param vca
     */
    private void writeValueConstraints(ValueConstraint[] vca) throws IOException {
        if (vca == null || vca.length == 0) return;
        String vc = vca[0].getDefinition(resolver);
        out.write(" < '");
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
     * writeNodeDef
     * @param nd
     */
    private void writeNodeDef(NodeTypeDef ntd, NodeDef nd) throws IOException, NamespaceException {
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

    private void writeItemDefName(QName name) throws IOException, NamespaceException {
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
    private void writeRequiredTypes(QName[] reqTypes) throws IOException, NamespaceException {
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
    private void writeDefaultType(QName defType) throws IOException, NamespaceException {
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
    private String resolve(QName qname) throws NamespaceException {
        if (qname == null) {
            return "";
        }
        String prefix = resolver.getPrefix(qname.getNamespaceURI());
        if (prefix != null && !prefix.equals(QName.NS_EMPTY_PREFIX)) {
            prefix += ":";
        }
        return prefix + qname.getLocalName();
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
