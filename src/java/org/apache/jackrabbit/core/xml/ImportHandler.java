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
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

/**
 * An <code>ImportHandler</code> instance can be used to import serialized
 * data in System View XML or Document View XML. The actual task of importing
 * is delegated to specialized <code>ContentHandler</code>s (i.e.
 * <code>SysViewImportHandler</code> and <code>DocViewImportHandler</code>).
 */
public class ImportHandler extends DefaultHandler {

    private static Logger log = Logger.getLogger(ImportHandler.class);

    protected Locator locator;
    protected ContentHandler targetHandler;

    protected SessionImpl session;
    protected NodeImpl importTargetNode;
    protected NamespaceRegistryImpl nsReg;
    protected boolean systemViewXML;
    protected boolean initialized;

    public ImportHandler(NodeImpl importTargetNode, NamespaceRegistryImpl nsReg, SessionImpl session) {
        this.importTargetNode = importTargetNode;
        this.session = session;
        this.nsReg = nsReg;
    }

    //---------------------------------------------------------< ErrorHandler >
    /**
     * @see ErrorHandler#warning(SAXParseException)
     */
    public void warning(SAXParseException e) throws SAXException {
        // log exception and carry on...
        log.warn("warning encountered at line: " + e.getLineNumber() + ", column: " + e.getColumnNumber() + " while parsing XML stream", e);
    }

    /**
     * @see ErrorHandler#error(org.xml.sax.SAXParseException)
     */
    public void error(SAXParseException e) throws SAXException {
        // log exception and carry on...
        log.error("error encountered at line: " + e.getLineNumber() + ", column: " + e.getColumnNumber() + " while parsing XML stream", e);
    }

    /**
     * @see ErrorHandler#fatalError(SAXParseException)
     */
    public void fatalError(SAXParseException e) throws SAXException {
        // log and re-throw exception
        log.error("fatal error encountered at line: " + e.getLineNumber() + ", column: " + e.getColumnNumber() + " while parsing XML stream", e);
        throw e;
    }

    //-------------------------------------------------------< ContentHandler >
    /**
     * @see ContentHandler#startDocument()
     */
    public void startDocument() throws SAXException {
        systemViewXML = false;
        initialized = false;
        targetHandler = null;
    }

    /**
     * @see ContentHandler#endDocument()
     */
    public void endDocument() throws SAXException {
        // delegate to target handler
        targetHandler.endDocument();
    }

    /**
     * @see ContentHandler#startPrefixMapping(String, String)
     */
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        try {
            String oldPrefix = session.getNamespacePrefix(uri);
            // namespace is already registered; check prefix
            if (!"".equals(prefix) && !oldPrefix.equals(prefix)) {
                /**
                 * namespace is mapped to different prefix;
                 * try to remap it to given prefix
                 */
                try {
                    session.setNamespacePrefix(prefix, uri);
                } catch (RepositoryException re) {
                    throw new SAXException("failed to remap namespace " + uri + " to prefix " + prefix, re);
                }
            }
        } catch (NamespaceException nse) {
            // namespace is not yet registered, try to register it
            if ("".equals(prefix)) {
                /**
                 * the xml document specifies a default namespace (i.e. an empty prefix);
                 * we need to create a random prefix as the empty prefix is reserved
                 * according to the JCR spec.
                 */
                prefix = nsReg.getUniquePrefix(uri);
            }
            try {
                nsReg.registerNamespace(prefix, uri);
            } catch (RepositoryException re) {
                throw new SAXException("failed to register namespace " + uri + " with prefix " + prefix, re);
            }
        } catch (RepositoryException re) {
            throw new SAXException("failed to check prefix for namespace " + uri, re);
        }
    }

    /**
     * @see ContentHandler#startElement(String, String, String, Attributes)
     */
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (!initialized) {
            // the namespace of the first element determines the type of XML
            // (system view/document view)
            String nsURI;
            if (namespaceURI != null && !"".equals(namespaceURI)) {
                nsURI = namespaceURI;
            } else {
                try {
                    nsURI = QName.fromJCRName(qName, session.getNamespaceResolver()).getNamespaceURI();
                } catch (BaseException e) {
                    // should never happen...
                    String msg = "internal error: failed to parse/resolve element name " + qName;
                    log.debug(msg);
                    throw new SAXException(msg, e);
                }
            }
            systemViewXML = NamespaceRegistryImpl.NS_SV_URI.equals(nsURI);

            if (systemViewXML) {
                targetHandler = new SysViewImportHandler(importTargetNode, session);
            } else {
                targetHandler = new DocViewImportHandler(importTargetNode, session);
            }
            targetHandler.startDocument();
            initialized = true;
        }

        // delegate to target handler
        targetHandler.startElement(namespaceURI, localName, qName, atts);
    }

    /**
     * @see ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        // delegate to target handler
        targetHandler.characters(ch, start, length);
    }

    /**
     * @see ContentHandler#endElement(String, String, String)
     */
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        // delegate to target handler
        targetHandler.endElement(namespaceURI, localName, qName);
    }

    /**
     * @see ContentHandler#setDocumentLocator(Locator)
     */
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }
}
