/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.jcr.core.xml;

import org.apache.jackrabbit.jcr.core.*;
import org.apache.jackrabbit.jcr.core.nodetype.NodeTypeRegistry;
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
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.3 $, $Date: 2004/08/30 11:13:47 $
 */
class DocViewImportHandler extends DefaultHandler {

    private static Logger log = Logger.getLogger(DocViewImportHandler.class);

    private Stack parents;
    private SessionImpl session;

    DocViewImportHandler(NodeImpl importTargetNode, SessionImpl session) {
	this.session = session;
	parents = new Stack();

	parents.push(importTargetNode);
    }

    //-------------------------------------------------------< ContentHandler >
    /**
     * @see ContentHandler#startElement(String, String, String, Attributes)
     */
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
	try {
	    QName nodeName;
	    if (qName == null || "".equals(qName)) {
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
	    currentParent = (NodeImpl) currentParent.addNode(nodeName, NodeTypeRegistry.NT_UNSTRUCTURED);
	    parents.push(currentParent);

	    // properties
	    for (int i = 0; i < atts.getLength(); i++) {
		QName propName;
		if (atts.getQName(i) == null || "".equals(atts.getQName(i))) {
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
	// character data in document view:
	// store as jcr:xmlcharacters property of jcr:xmltext node
	// (need to store as node in order to maintain ordering)
	try {
	    NodeImpl currentParent = (NodeImpl) parents.peek();
	    NodeImpl txtNode = (NodeImpl) currentParent.addNode(DocViewSAXEventGenerator.NODENAME_XMLTEXT);
	    StringValue val = new StringValue(new String(ch, start, length));
	    txtNode.setProperty(DocViewSAXEventGenerator.PROPNAME_XMLCHARACTERS, val);
	} catch (RepositoryException re) {
	    throw new SAXException(re);
	}
    }

    /**
     * @see ContentHandler#endElement(String, String, String)
     */
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
	parents.pop();
    }
}
