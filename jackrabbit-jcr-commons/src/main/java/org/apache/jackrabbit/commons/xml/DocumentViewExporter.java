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
package org.apache.jackrabbit.commons.xml;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.commons.NamespaceHelper;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.value.ValueHelper;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Document view exporter.
 *
 * @since Jackrabbit JCR Commons 1.5
 */
public class DocumentViewExporter extends Exporter {

    /**
     * Creates a document view exporter.
     *
     * @param session current session
     * @param handler SAX event handler for the export
     * @param recurse whether to recursively export the whole subtree
     * @param binary whether to export binary values
     */
    public DocumentViewExporter(
            Session session, ContentHandler handler,
            boolean recurse, boolean binary) {
        super(session, handler, recurse, binary);
    }

    /**
     * Exports the given node either as XML characters (if it's an
     * <code>xml:text</code> node) or as an XML element with properties
     * mapped to XML attributes.
     */
    protected void exportNode(String uri, String local, Node node)
            throws RepositoryException, SAXException {
        if (NamespaceHelper.JCR.equals(uri) && "xmltext".equals(local)) {
            try {
                // assume jcr:xmlcharacters is single-valued
                Property property =
                    node.getProperty(helper.getJcrName("jcr:xmlcharacters"));
                char[] ch = property.getString().toCharArray();
                characters(ch, 0, ch.length);
            } catch (PathNotFoundException e) {
                // jcr:xmlcharacters not found, ignore this node
            }
        } else {
            // attributes (properties)
            exportProperties(node);

            // encode node name to make sure it's a valid xml name
            String encoded = ISO9075.encode(local);
            startElement(uri, encoded);
            exportNodes(node);
            endElement(uri, encoded);
        }
    }

    /**
     * Maps the given single-valued property to an XML attribute.
     */
    protected void exportProperty(String uri, String local, Value value)
            throws RepositoryException {
        // TODO: Serialized names and paths should use XML namespace mappings
        String attribute = ValueHelper.serialize(value, false);
        addAttribute(uri, ISO9075.encode(local), attribute);
    }

    /**
     * Does nothing. Multi-valued properties are skipped for the time being
     * until a way of properly handling/detecting multi-valued properties
     * on re-import is found. Skipping multi-valued properties entirely is
     * legal according to "6.4.2.5 Multi-value Properties" of the JSR 170
     * specification.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-325">JCR-325</a>
     */
    protected void exportProperty(
            String uri, String local, int type, Value[] values) {
        // TODO: proper multi-value serialization support
    }

}
