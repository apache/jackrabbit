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
package org.apache.jackrabbit.core.xml;

import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.jackrabbit.value.ValueHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A <code>DocViewSAXEventGenerator</code> instance can be used to generate
 * SAX events representing the serialized form of an item in Document View XML.
 */
public class DocViewSAXEventGenerator extends AbstractSAXEventGenerator {

    private static Logger log = LoggerFactory.getLogger(DocViewSAXEventGenerator.class);

    public static final String CDATA_TYPE = "CDATA";

    // used to temporarily store properties of a node
    private final List props;

    /**
     * Constructor
     *
     * @param node           the node state which should be serialized
     * @param noRecurse      if true, only <code>node</code> and its properties
     *                       will be serialized; otherwise the entire hierarchy
     *                       starting with <code>node</code> will be serialized.
     * @param skipBinary     flag governing whether binary properties are to be
     *                       serialized.
     * @param contentHandler the content handler to feed the SAX events to
     * @throws RepositoryException if an error occurs
     */
    public DocViewSAXEventGenerator(Node node, boolean noRecurse,
                                    boolean skipBinary,
                                    ContentHandler contentHandler)
            throws RepositoryException {
        super(node, noRecurse, skipBinary, contentHandler);

        props = new ArrayList();
    }

    private Name getQName(String rawName) throws RepositoryException {
        try {
            return resolver.getQName(rawName);
        } catch (NameException e) {
            // should never get here...
            String msg = "internal error: failed to resolve namespace mappings";
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void entering(Node node, int level)
            throws RepositoryException, SAXException {
        // nop
    }

    /**
     * {@inheritDoc}
     */
    protected void enteringProperties(Node node, int level)
            throws RepositoryException, SAXException {
        // reset list of properties
        props.clear();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * See also {@link DocViewImportHandler#startElement(String, String, String, org.xml.sax.Attributes)}
     * regarding special handling of multi-valued properties on import.
     */
    protected void leavingProperties(Node node, int level)
            throws RepositoryException, SAXException {
        String name = node.getName();
        if (name.equals(jcrXMLText)) {
            // the node represents xml character data
            Iterator iter = props.iterator();
            while (iter.hasNext()) {
                Property prop = (Property) iter.next();
                String propName = prop.getName();
                if (propName.equals(jcrXMLCharacters)) {
                    // assume jcr:xmlcharacters is single-valued
                    char[] chars = prop.getValue().getString().toCharArray();
                    contentHandler.characters(chars, 0, chars.length);
                }
            }
        } else {
            // regular node

            // element name
            String elemName;
            if (node.getDepth() == 0) {
                // root node needs a name
                elemName = jcrRoot;
            } else {
                // encode node name to make sure it's a valid xml name
                elemName = ISO9075.encode(name);
            }

            // attributes (properties)
            AttributesImpl attrs = new AttributesImpl();
            addNamespacePrefixes(level, attrs);
            Iterator iter = props.iterator();
            while (iter.hasNext()) {
                Property prop = (Property) iter.next();
                String propName = prop.getName();

                if (prop.getDefinition().isMultiple()) {
                    // todo proper multi-value serialization support
                    // skip multi-valued properties for the time being
                    // until a way of properly handling/detecting multi-valued
                    // properties on re-import is found (see JCR-325);
                    // see also DocViewImportHandler#startElement()

                    // skipping multi-valued properties entirely is legal
                    // according to "6.4.2.5 Multi-value Properties" of the
                    // jsr-170 specification
                    continue;
                }

                // attribute name (encode property name to make sure it's a valid xml name)
                String attrName = ISO9075.encode(propName);
                Name qName = getQName(attrName);

                // attribute value
                if (prop.getType() == PropertyType.BINARY && skipBinary) {
                    // add empty attribute
                    attrs.addAttribute(qName.getNamespaceURI(),
                            qName.getLocalName(), attrName, CDATA_TYPE, "");
                } else {
                    StringBuffer attrValue = new StringBuffer();
                    // serialize single-valued property
                    attrValue.append(ValueHelper.serialize(prop.getValue(), false));
                    attrs.addAttribute(qName.getNamespaceURI(),
                            qName.getLocalName(), attrName, CDATA_TYPE,
                            attrValue.toString());
                }
            }

            // start element (node)
            Name qName = getQName(elemName);
            contentHandler.startElement(qName.getNamespaceURI(),
                    qName.getLocalName(), elemName, attrs);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void leaving(Node node, int level)
            throws RepositoryException, SAXException {
        String name = node.getName();
        if (name.equals(jcrXMLText)) {
            // the node represents xml character data
            // (already processed in leavingProperties(NodeImpl, int)
            return;
        }
        // element name
        String elemName;
        if (node.getDepth() == 0) {
            // root node needs a name
            elemName = jcrRoot;
        } else {
            // encode node name to make sure it's a valid xml name
            elemName = ISO9075.encode(name);
        }

        // end element (node)
        Name qName = getQName(elemName);
        contentHandler.endElement(qName.getNamespaceURI(), qName.getLocalName(),
                elemName);
    }

    /**
     * {@inheritDoc}
     */
    protected void entering(Property prop, int level)
            throws RepositoryException, SAXException {
        props.add(prop);
    }

    /**
     * {@inheritDoc}
     */
    protected void leaving(Property prop, int level)
            throws RepositoryException, SAXException {
        // nop
    }
}
