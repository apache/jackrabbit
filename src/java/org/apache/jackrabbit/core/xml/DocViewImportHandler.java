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
package org.apache.jackrabbit.core.xml;

import org.apache.jackrabbit.core.BaseException;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.QName;
import org.apache.jackrabbit.core.util.ISO9075;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Stack;
import java.io.IOException;

/**
 * <code>DocViewImportHandler</code> processes Document View XML SAX events
 * and 'translates' them into <code>{@link Importer}</code> method calls.
 */
class DocViewImportHandler extends TargetImportHandler {

    private static Logger log = Logger.getLogger(DocViewImportHandler.class);

    /**
     * stack of NodeInfo instances; an instance is pushed onto the stack
     * in the startElement method and is popped from the stack in the
     * endElement method.
     */
    private final Stack stack = new Stack();
    // buffer used to merge adjacent character data
    private StringBufferValue textHandler = new StringBufferValue();

    /**
     * Constructs a new <code>DocViewImportHandler</code>.
     *
     * @param importer
     * @param nsContext
     */
    DocViewImportHandler(Importer importer, NamespaceResolver nsContext) {
        super(importer, nsContext);
    }

    /**
     * Translates character data encountered in
     * <code>{@link #characters(char[], int, int)}</code> into a
     * <code>jcr:xmltext</code> child node with a <code>jcr:xmlcharacters</code>
     * property.
     *
     * @param text
     * @throws SAXException
     */
    private void onTextNode(StringBufferValue text)
            throws SAXException {
        String s = textHandler.retrieve();
        if (s.trim().length() == 0) {
            // ignore whitespace-only character data
            log.debug("ignoring withespace character data: " + s);
            return;
        }
        try {
            Importer.NodeInfo node =
                    new Importer.NodeInfo(JCR_XMLTEXT, null, null, null);
            Importer.TextValue[] values = new Importer.TextValue[]{text};
            ArrayList props = new ArrayList();
            Importer.PropInfo prop =
                    new Importer.PropInfo(JCR_XMLCHARACTERS,
                            PropertyType.STRING, values);
            props.add(prop);
            // call Importer
            importer.startNode(node, props, nsContext);
            importer.endNode(node);
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    //-------------------------------------------------------< ContentHandler >
    /**
     * {@inheritDoc}
     */
    public void startDocument() throws SAXException {
        try {
            importer.start();
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes atts)
            throws SAXException {
        if (textHandler != null && textHandler.length() > 0) {
            // there is character data that needs to be added to the current node
            onTextNode(textHandler);
            // reset handler
            textHandler.dispose();
            textHandler = null;
        }

        try {
            QName nodeName = new QName(namespaceURI, localName);
            // decode node name
            nodeName = ISO9075.decode(nodeName);

            // properties
            String uuid = null;
            QName nodeTypeName = null;
            QName[] mixinTypes = null;

            ArrayList props = new ArrayList(atts.getLength());
            for (int i = 0; i < atts.getLength(); i++) {
                QName propName = new QName(atts.getURI(i), atts.getLocalName(i));
                // decode property name
                propName = ISO9075.decode(propName);

                // value(s)
                String attrValue = atts.getValue(i);
                Importer.TextValue[] propValues;
/*
                // @todo should attribute value be interpreted as LIST type (i.e. multi-valued property)?
                String[] strings = Text.explode(attrValue, ' ', true);
                propValues = new Value[strings.length];
                for (int j = 0; j < strings.length; j++) {
                    // decode encoded blanks in value
                    strings[j] = Text.replace(strings[j], "_x0020_", " ");
                    propValues[j] = InternalValue.create(strings[j]);
                }
*/
                if (propName.equals(JCR_PRIMARYTYPE)) {
                    // jcr:primaryType
                    if (attrValue.length() > 0) {
                        try {
                            nodeTypeName = QName.fromJCRName(attrValue, nsContext);
                        } catch (BaseException be) {
                            throw new SAXException("illegal jcr:primaryType value: "
                                    + attrValue, be);
                        }
                    }
                } else if (propName.equals(JCR_MIXINTYPES)) {
                    // jcr:mixinTypes
                    if (attrValue.length() > 0) {
                        try {
                            mixinTypes =
                                    new QName[]{QName.fromJCRName(attrValue, nsContext)};
                        } catch (BaseException be) {
                            throw new SAXException("illegal jcr:mixinTypes value: "
                                    + attrValue, be);
                        }
                    }
                } else if (propName.equals(JCR_UUID)) {
                    // jcr:uuid
                    if (attrValue.length() > 0) {
                        uuid = attrValue;
                    }
                } else {
                    propValues = new Importer.TextValue[1];
                    propValues[0] = new StringValue(atts.getValue(i));
                    props.add(new Importer.PropInfo(propName,
                            PropertyType.UNDEFINED, propValues));
                }
            }

            Importer.NodeInfo node =
                    new Importer.NodeInfo(nodeName, nodeTypeName, mixinTypes, uuid);
            // all information has been collected, now delegate to importer
            importer.startNode(node, props, nsContext);
            // push current node data onto stack
            stack.push(node);
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        /**
         * buffer character data; will be processed
         * in endElement and startElement method
         */
        if (textHandler == null) {
            textHandler = new StringBufferValue();
        }
        textHandler.append(ch, start, length);
    }

    /**
     * {@inheritDoc}
     */
    public void ignorableWhitespace(char ch[], int start, int length)
            throws SAXException {
        /**
         * buffer data reported by the ignorableWhitespace event;
         * will be processed in endElement and startElement method
         */
        if (textHandler == null) {
            textHandler = new StringBufferValue();
        }
        textHandler.append(ch, start, length);
    }

    /**
     * {@inheritDoc}
     */
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        if (textHandler != null && textHandler.length() > 0) {
            // there is character data that needs to be added to the current node
            onTextNode(textHandler);
            // reset handler
            textHandler.dispose();
            textHandler = null;
        }
        Importer.NodeInfo node = (Importer.NodeInfo) stack.peek();
        try {
            // call Importer
            importer.endNode(node);
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
        // we're done with this node, pop it from stack
        stack.pop();
    }

    /**
     * {@inheritDoc}
     */
    public void endDocument() throws SAXException {
        try {
            importer.end();
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }
}
