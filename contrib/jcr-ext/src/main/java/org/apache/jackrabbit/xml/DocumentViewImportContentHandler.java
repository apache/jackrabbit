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
package org.apache.jackrabbit.xml;

import java.util.Stack;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * TODO
 */
public class DocumentViewImportContentHandler implements ContentHandler {

    private Session session;
    
    private Stack stack;
    
    private Node node;
    
    private StringBuffer text;
    
    public DocumentViewImportContentHandler(Node parent) throws RepositoryException {
        this.session = parent.getSession();
        this.stack = new Stack();
        this.node = parent;
        this.text = new StringBuffer();
    }
    
    
    private String getName(String uri, String local, String qname)
            throws RepositoryException {
        if (uri == null || uri.length() == 0) {
            return local;
        }
        
        try {
            return session.getNamespacePrefix(uri) + ":" + local;
        } catch (NamespaceException ex) {
            // fall through
        }
        
        int i = qname.indexOf(':');
        String prefix = (i != -1) ? qname.substring(0, i) : "ext";
        try {
            String base = prefix;
            for (int no = 1; true; prefix = base + no++) {
                session.getNamespaceURI(prefix);
            }
        } catch (NamespaceException ex) {
            // fall through
        }

        session.getWorkspace().getNamespaceRegistry()
            .registerNamespace(prefix, uri);
        return getName(uri, local, qname);
    }
    
    public void startElement(String uri, String localName, String qName,
            Attributes atts) throws SAXException {
        try {
            importText();
            
            stack.push(node);

            node = node.addNode(getName(uri, localName, qName));
            for (int i = 0; i < atts.getLength(); i++) {
                String name = getName(atts.getURI(i), atts.getLocalName(i),
                        atts.getQName(i));
                node.setProperty(name, atts.getValue(i));
            }
        } catch (RepositoryException ex) {
            throw new SAXException(ex);
        }
    }

    /**
     * TODO
     * {@inheritDoc}
     */
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        try {
            importText();
            
            node = (Node) stack.pop();
        } catch (RepositoryException ex) {
            throw new SAXException(ex);
        }
    }

    /**
     * Appends the received characters to the current text buffer.
     * The accumulated contents of the text buffer is written to an
     * jcr:xmltext node when an element boundary is reached.  
     * {@inheritDoc}
     */
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        text.append(ch, start, length);
    }
    
    /**
     * Imports the accumulated XML character data as an jcr:xmltext node.
     * The character data is stored as a jcr:xmlcharacters string property
     * of the created node. The character data buffer is then cleared.
     * <p>
     * This method does nothing if the character data buffer is empty, and
     * can therefore be invoked whenever an element boundary is reached to
     * handle the importing of any accumulated character data.   
     * 
     * @throws RepositoryException
     */
    private void importText() throws RepositoryException {
        if (text.length() > 0) {
            Node xmltext = node.addNode("jcr:xmltext");
            xmltext.setProperty("jcr:xmlcharacters", text.toString());
            text.setLength(0);
        }
    }

    private String unescapeName(String name) {
        StringBuffer buffer = new StringBuffer();
        for (int i = name.indexOf("_x"); i != -1; i = name.indexOf("_x")) {
            buffer.append(name.substring(0, i));
            if (i + 6 < name.length() && name.charAt(i + 6) == '_') {
                try {
                    buffer.append(
                            (char) Integer.parseInt(name.substring(2, 6), 16));
                    name = name.substring(i + 7);
                } catch (NumberFormatException ex) {
                    buffer.append(name.charAt(i));
                    name = name.substring(i + 1);
                }
            } else {
                buffer.append(name.charAt(i));
                name = name.substring(i + 1);
            }
        }
        buffer.append(name);
        return buffer.toString();
    }

    
    /** Ignored. */
    public void setDocumentLocator(Locator locator) {
    }

    /** Ignored. */
    public void startDocument() {
    }

    /** Ignored. */
    public void endDocument() throws SAXException {
    }

    /** Ignored. */
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
    }

    /** Ignored. */
    public void endPrefixMapping(String prefix) throws SAXException {
    }
    
    /** Ignored. */
    public void ignorableWhitespace(char[] ch, int start, int length)
            throws SAXException {
    }

    /** Ignored. */
    public void processingInstruction(String target, String data)
            throws SAXException {
    }

    /** Ignored. */
    public void skippedEntity(String name) throws SAXException {
    }

}
