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
import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.jackrabbit.core.QName;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.NamespaceSupport;

import javax.jcr.NamespaceException;
import javax.jcr.RepositoryException;

/**
 * An <code>ImportHandler</code> instance can be used to import serialized
 * data in System View XML or Document View XML. Processing of the XML is handled
 * by specialized <code>ContentHandler</code>s (i.e. <code>SysViewImportHandler</code>
 * and <code>DocViewImportHandler</code>).
 * <p/>
 * The actual task of importing though is delegated to the implementation of
 * the <code>{@link Importer}</code> interface.
 */
public class ImportHandler extends DefaultHandler {

    private static Logger log = Logger.getLogger(ImportHandler.class);

    protected final Importer importer;
    protected final NamespaceRegistryImpl nsReg;
    protected final NamespaceResolver nsResolver;

    protected Locator locator;
    protected ContentHandler targetHandler;
    protected boolean systemViewXML;
    protected boolean initialized;

    protected final NamespaceContext nsContext;

    /**
     * this flag is used to determine whether a namespace context needs to be
     * started in the startElement event or if the namespace context has already
     * been started in a preceeding startPrefixMapping event;
     * the flag is set per element in the first startPrefixMapping event and is
     * cleared again in the following startElement event;
     */
    protected boolean nsContextStarted;

    public ImportHandler(Importer importer, NamespaceResolver nsResolver,
                         NamespaceRegistryImpl nsReg) {
        this.importer = importer;
        this.nsResolver = nsResolver;
        this.nsReg = nsReg;

        nsContext = new NamespaceContext();
    }

    //---------------------------------------------------------< ErrorHandler >
    /**
     * {@inheritDoc}
     */
    public void warning(SAXParseException e) throws SAXException {
        // log exception and carry on...
        log.warn("warning encountered at line: " + e.getLineNumber()
                + ", column: " + e.getColumnNumber()
                + " while parsing XML stream", e);
    }

    /**
     * {@inheritDoc}
     */
    public void error(SAXParseException e) throws SAXException {
        // log exception and carry on...
        log.error("error encountered at line: " + e.getLineNumber()
                + ", column: " + e.getColumnNumber()
                + " while parsing XML stream", e);
    }

    /**
     * {@inheritDoc}
     */
    public void fatalError(SAXParseException e) throws SAXException {
        // log and re-throw exception
        log.error("fatal error encountered at line: " + e.getLineNumber()
                + ", column: " + e.getColumnNumber()
                + " while parsing XML stream", e);
        throw e;
    }

    //-------------------------------------------------------< ContentHandler >
    /**
     * {@inheritDoc}
     */
    public void startDocument() throws SAXException {
        systemViewXML = false;
        initialized = false;
        targetHandler = null;

        /**
         * start initial context containing existing mappings reflected
         * by nsResolver
         */
        nsContext.reset();
        nsContext.pushContext();
        try {
            String[] uris = nsReg.getURIs();
            for (int i = 0; i < uris.length; i++) {
                nsContext.declarePrefix(nsResolver.getPrefix(uris[i]), uris[i]);
            }
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }

        // initialize flag
        nsContextStarted = false;
    }

    /**
     * {@inheritDoc}
     */
    public void endDocument() throws SAXException {
        // delegate to target handler
        targetHandler.endDocument();
        // cleanup
        nsContext.reset();
    }

    /**
     * {@inheritDoc}
     */
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        // check if new context needs to be started
        if (!nsContextStarted) {
            // entering new namespace context
            nsContext.pushContext();
            nsContextStarted = true;
        }

        try {
            // this will trigger NamespaceException if namespace is unknown
            nsContext.getPrefix(uri);
        } catch (NamespaceException nse) {
            // namespace is not yet registered ...
            String newPrefix;
            if ("".equals(prefix)) {
                /**
                 * the xml document specifies a default namespace
                 * (i.e. an empty prefix); we need to create a random
                 * prefix as the empty prefix is reserved according
                 * to the JCR spec.
                 */
                newPrefix = nsReg.getUniquePrefix(uri);
            } else {
                newPrefix = prefix;
            }
            // register new namespace
            try {
                nsReg.registerNamespace(newPrefix, uri);
            } catch (RepositoryException re) {
                throw new SAXException(re);
            }
        }
        // map namespace in this context to given prefix
        nsContext.declarePrefix(prefix, uri);
    }

    /**
     * {@inheritDoc}
     */
    public void endPrefixMapping(String prefix) throws SAXException {
        /**
         * nothing to do here as namespace context has already been popped
         * in endElement event
         */
    }

    /**
     * {@inheritDoc}
     */
    public void startElement(String namespaceURI, String localName, String qName,
                             Attributes atts) throws SAXException {
        // check if new context needs to be started
        if (!nsContextStarted) {
            // there hasn't been a preceeding startPrefixMapping event
            // so enter new namespace context
            nsContext.pushContext();
        } else {
            // reset flag
            nsContextStarted = false;
        }

        if (!initialized) {
            // the namespace of the first element determines the type of XML
            // (system view/document view)
            String nsURI;
            if (namespaceURI != null && !"".equals(namespaceURI)) {
                nsURI = namespaceURI;
            } else {
                try {
                    nsURI = QName.fromJCRName(qName, nsResolver).getNamespaceURI();
                } catch (BaseException e) {
                    // should never happen...
                    String msg = "internal error: failed to parse/resolve element name "
                            + qName;
                    log.debug(msg);
                    throw new SAXException(msg, e);
                }
            }
            systemViewXML = Constants.NS_SV_URI.equals(nsURI);

            if (systemViewXML) {
                targetHandler = new SysViewImportHandler(importer, nsContext);
            } else {
                targetHandler = new DocViewImportHandler(importer, nsContext);
            }
            targetHandler.startDocument();
            initialized = true;
        }

        // delegate to target handler
        targetHandler.startElement(namespaceURI, localName, qName, atts);
    }

    /**
     * {@inheritDoc}
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        // delegate to target handler
        targetHandler.characters(ch, start, length);
    }

    /**
     * {@inheritDoc}
     */
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        // leaving element, pop namespace context
        nsContext.popContext();

        // delegate to target handler
        targetHandler.endElement(namespaceURI, localName, qName);
    }

    /**
     * {@inheritDoc}
     */
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    //--------------------------------------------------------< inner classes >
    /**
     * <code>NamespaceContext</code> supports scoped namespace declarations.
     */
    class NamespaceContext implements NamespaceResolver {

        private final NamespaceSupport nsContext;

        /**
         * NamespaceSupport doesn't accept "" as default uri;
         * internally we're using " " instead
         */
        private static final String DUMMY_DEFAULT_URI = " ";
        private static final String DEFAULT_URI = Constants.NS_DEFAULT_URI;

        private static final String EMPTY_PREFIX = "";

        NamespaceContext() {
            nsContext = new NamespaceSupport();
        }

        /**
         * {@inheritDoc}
         */
        void popContext() {
            nsContext.popContext();
        }

        /**
         * {@inheritDoc}
         */
        void pushContext() {
            nsContext.pushContext();
        }

        /**
         * {@inheritDoc}
         */
        void reset() {
            nsContext.reset();
        }

        /**
         * {@inheritDoc}
         */
        boolean declarePrefix(String prefix, String uri) {
            uri = DEFAULT_URI.equals(uri) ? DUMMY_DEFAULT_URI : uri;
            return nsContext.declarePrefix(prefix, uri);
        }

        //------------------------------------------------< NamespaceResolver >
        /**
         * {@inheritDoc}
         */
        public String getURI(String prefix) throws NamespaceException {
            String uri = nsContext.getURI(prefix);
            if (uri == null) {
                throw new NamespaceException("unknown prefix");
            }
            return DUMMY_DEFAULT_URI.equals(uri) ? DEFAULT_URI : uri;
        }

        /**
         * {@inheritDoc}
         */
        public String getPrefix(String uri) throws NamespaceException {
            uri = DEFAULT_URI.equals(uri) ? DUMMY_DEFAULT_URI : uri;
            String prefix = nsContext.getPrefix(uri);
            if (prefix == null) {
                /**
                 * NamespaceSupport#getPrefix will never return the empty
                 * (default) prefix; we have to do a reverse-lookup to check
                 * whether it's the current default namespace
                 */
                if (uri.equals(nsContext.getURI(EMPTY_PREFIX))) {
                    return EMPTY_PREFIX;
                }
                throw new NamespaceException("unknown uri");
            }
            return prefix;
        }
    }
}
