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
package org.apache.jackrabbit.core.nodetype;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.nodetype.xml.NodeTypeFormat;
import org.apache.jackrabbit.core.nodetype.xml.AdditionalNamespaceResolver;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * <code>NodeTypeDefStore</code> ...
 */
class NodeTypeDefStore {
    private static Logger log = Logger.getLogger(NodeTypeDefStore.class);

    private static final String ROOT_ELEMENT = "nodeTypes";

    // map of node type names and node type definitions
    private HashMap ntDefs;

    /**
     * Empty default constructor.
     */
    NodeTypeDefStore() {
        ntDefs = new HashMap();
    }

    /**
     * @param in
     * @throws IOException
     * @throws InvalidNodeTypeDefException
     * @throws RepositoryException
     */
    void load(InputStream in)
            throws IOException, InvalidNodeTypeDefException, RepositoryException {
        SAXBuilder builder = new SAXBuilder();
        Element root;
        try {
            Document doc = builder.build(in);
            root = doc.getRootElement();
        } catch (JDOMException jde) {
            String msg = "internal error: failed to parse persistent node type definitions";
            log.debug(msg);
            throw new RepositoryException(msg, jde);
        }

        // read definitions
        NamespaceResolver resolver = new AdditionalNamespaceResolver(root);
        Iterator iter =
            root.getChildren(NodeTypeFormat.NODETYPE_ELEMENT).iterator();
        while (iter.hasNext()) {
            NodeTypeFormat format =
                new NodeTypeFormat(resolver, (Element) iter.next());
            format.read();
            add(format.getNodeType());
        }
    }

    /**
     * @param out
     * @param nsReg
     * @throws IOException
     * @throws RepositoryException
     */
    void store(OutputStream out, NamespaceRegistryImpl nsReg)
            throws IOException, RepositoryException {
        Element root = new Element(ROOT_ELEMENT);

        // namespace declarations
        String[] prefixes = nsReg.getPrefixes();
        for (int i = 0; i < prefixes.length; i++) {
            String prefix = prefixes[i];
            if ("".equals(prefix)) {
                continue;
            }
            String uri = nsReg.getURI(prefix);
            root.addNamespaceDeclaration(Namespace.getNamespace(prefix, uri));
        }

        // node type definitions
        Iterator iter = all().iterator();
        while (iter.hasNext()) {
            NodeTypeFormat format =
                new NodeTypeFormat(nsReg, (NodeTypeDef) iter.next());
            format.write();
            root.addContent(format.getElement());
        }

        XMLOutputter serializer = new XMLOutputter(Format.getPrettyFormat());
        serializer.output(new Document(root), out);
    }

    /**
     * @param ntd
     */
    void add(NodeTypeDef ntd) {
        ntDefs.put(ntd.getName(), ntd);
    }

    /**
     * @param name
     * @return
     */
    boolean remove(QName name) {
        return ntDefs.remove(name) != null ? true : false;
    }

    /**
     *
     */
    void removeAll() {
        ntDefs.clear();
    }

    /**
     * @param name
     * @return
     */
    boolean contains(QName name) {
        return ntDefs.containsKey(name);
    }

    /**
     * @param name
     * @return
     */
    NodeTypeDef get(QName name) {
        return (NodeTypeDef) ntDefs.get(name);
    }

    /**
     * @return
     */
    Collection all() {
        return Collections.unmodifiableCollection(ntDefs.values());
    }

}
