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

import org.apache.jackrabbit.core.*;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.jcr.RepositoryException;
import javax.jcr.StringValue;
import java.util.Stack;

/**
 * <code>DocViewImportHandler</code>  ...
 */
class DocViewImportHandler extends DefaultHandler {

    private static Logger log = Logger.getLogger(DocViewImportHandler.class);

    private Stack parents;
    private SessionImpl session;
    // buffer used to merge adjacent character data
    private StringBuffer text;

    DocViewImportHandler(NodeImpl importTargetNode, SessionImpl session) {
        this.session = session;
        parents = new Stack();

        parents.push(importTargetNode);

        text = new StringBuffer();
    }

    /**
     * Stores character data encountered in <code>{@link #characters(char[], int, int)}</code>
     * as <code>jcr:xmlcharacters</code> property of <code>jcr:xmltext</code>
     * child node.
     *
     * @param parent
     * @param text
     * @throws SAXException
     */
    protected void addTextNode(NodeImpl parent, String text) throws SAXException {
        if (text.length() > 0) {
            try {
                NodeImpl txtNode = (NodeImpl) parent.addNode(Constants.JCR_XMLTEXT);
                StringValue val = new StringValue(text.toString());
                txtNode.setProperty(Constants.JCR_XMLCHARACTERS, val);
            } catch (RepositoryException re) {
                throw new SAXException(re);
            }
        }
    }

    //-------------------------------------------------------< ContentHandler >
    /**
     * @see ContentHandler#startElement(String, String, String, Attributes)
     */
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (text.length() > 0) {
            // there is character data that needs to be added to the current node
            addTextNode((NodeImpl) parents.peek(), text.toString());
            // reset buffer
            text.setLength(0);
        }

        try {
            QName nodeName;
            if (namespaceURI != null && !"".equals(namespaceURI)) {
                nodeName = new QName(namespaceURI, localName);
            } else {
                try {
                    nodeName = QName.fromJCRName(qName, session.getNamespaceResolver());
                } catch (IllegalNameException ine) {
                    throw new SAXException("illegal node name: " + qName, ine);
                } catch (UnknownPrefixException upe) {
                    throw new SAXException("illegal node name: " + qName, upe);
                }
            }

            // @todo how should 'system' properties be handled in document view (e.g. jcr:primaryType,jcr:mixinTypes, jcr:uuid)?
            NodeImpl currentParent = (NodeImpl) parents.peek();
            currentParent = (NodeImpl) currentParent.addNode(nodeName, Constants.NT_UNSTRUCTURED);
            parents.push(currentParent);

            // properties
            for (int i = 0; i < atts.getLength(); i++) {
                if (atts.getQName(i).startsWith("xml:")) {
                    // skipping xml:space, xml:lang, etc.
                    log.debug("skipping reserved/system attribute " + atts.getQName(i));
                    continue;
                }
                QName propName;
                if (atts.getURI(i) != null && !"".equals(atts.getURI(i))) {
                    propName = new QName(atts.getURI(i), atts.getLocalName(i));
                } else {
                    try {
                        propName = QName.fromJCRName(atts.getQName(i), session.getNamespaceResolver());
                    } catch (IllegalNameException ine) {
                        throw new SAXException("illegal property name: " + atts.getQName(i), ine);
                    } catch (UnknownPrefixException upe) {
                        throw new SAXException("illegal property name: " + atts.getQName(i), upe);
                    }
                }
                StringValue val = new StringValue(atts.getValue(i));
                currentParent.setProperty(propName, val);
            }
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    /**
     * @see ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        /**
         * buffer character data; will be processed
         * in endElement and startElement method
         */
        text.append(ch, start, length);
    }

    /**
     * @see ContentHandler#endElement(String, String, String)
     */
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (text.length() > 0) {
            // there is character data that needs to be added to the current node
            addTextNode((NodeImpl) parents.peek(), text.toString());
            // reset buffer
            text.setLength(0);
        }
        parents.pop();
    }
}
