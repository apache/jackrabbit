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
package org.apache.jackrabbit.jcr2spi.xml;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.spi.Name;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.Workspace;
import javax.jcr.RepositoryException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>WorkspaceContentHandler</code>...
 */
public class WorkspaceContentHandler extends DefaultHandler {

    private static Logger log = LoggerFactory.getLogger(WorkspaceContentHandler.class);

    private final String parentAbsPath;
    private final int uuidBehavior;
    private final Workspace workspace;

    private final File tmpFile;
    private final ContentHandler delegatee;

    public WorkspaceContentHandler(Workspace workspace, String parentAbsPath, int uuidBehavior) throws RepositoryException {
        this.workspace = workspace;
        this.parentAbsPath = parentAbsPath;
        this.uuidBehavior = uuidBehavior;

        try {
            String tmpName = Text.md5(parentAbsPath);
            this.tmpFile = File.createTempFile("___" + tmpName, ".xml");

            SAXTransformerFactory stf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
            TransformerHandler th = stf.newTransformerHandler();
            th.setResult(new StreamResult(new FileOutputStream(tmpFile)));

            if (NamespaceFixingContentHandler.isRequired(stf)) {
                this.delegatee =  new NamespaceFixingContentHandler(th);
            } else {
                this.delegatee = th;
            }
        } catch (FileNotFoundException e) {
            throw new RepositoryException(e);
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (TransformerConfigurationException e) {
            throw new RepositoryException(e);
        } catch (TransformerException e) {
            throw new RepositoryException(e);
        } catch (SAXException e) {
            throw new RepositoryException(e);
        }
    }

    public void endDocument() throws SAXException {
        delegatee.endDocument();
        try {
            workspace.importXML(parentAbsPath, new FileInputStream(tmpFile), uuidBehavior);
        } catch (IOException e) {
            throw new SAXException(e);
        } catch (RepositoryException e) {
            throw new SAXException(e);
        } finally {
            tmpFile.delete();
        }
    }

    public void startDocument() throws SAXException {
        delegatee.startDocument();
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        delegatee.characters(ch, start, length);
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        delegatee.ignorableWhitespace(ch, start, length);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        delegatee.endPrefixMapping(prefix);
    }

    public void skippedEntity(String name) throws SAXException {
        delegatee.skippedEntity(name);
    }

    public void setDocumentLocator(Locator locator) {
        delegatee.setDocumentLocator(locator);
    }

    public void processingInstruction(String target, String data) throws SAXException {
        delegatee.processingInstruction(target, data);
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        delegatee.startPrefixMapping(prefix, uri);
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        delegatee.endElement(namespaceURI, localName, qName);
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        delegatee.startElement(namespaceURI, localName, qName, atts);
    }

    //--------------------------------------------------------< inner class >---
    /**
     * A ContentHandler implementation that ensures that all namespace prefixes
     * are also present as 'xmlns:' attributes. This used to circumvent Xalan's
     * serialization behaviour which ignores namespaces if they're not present
     * as 'xmlns:xxx' attributes.
     * The problem arises with SAX implementations such as the default present
     * with JDK 1.4.2.
     */
    private static class NamespaceFixingContentHandler implements ContentHandler {

        /** The wrapped content handler. */
        private final ContentHandler contentHandler;

        /**
         * The prefixes of startPrefixMapping() declarations for the coming element.
         */
        private final List prefixList = new ArrayList();

        /**
         * The URIs of startPrefixMapping() declarations for the coming element.
         */
        private final List uriList = new ArrayList();

        /**
         * Maps of URI<->prefix mappings. Used to work around a bug in the Xalan
         * serializer.
         */
        private final Map uriToPrefixMap = new HashMap();
        private final Map prefixToUriMap = new HashMap();

        /**
         * True if there has been some startPrefixMapping() for the coming element.
         */
        private boolean hasMappings = false;

        /**
         * Create an instance of this ContentHandler implementation wrapping
         * the given ContentHandler.
         *
         * @param ch ContentHandler to be wrapped.
         */
        private NamespaceFixingContentHandler(ContentHandler ch) {
            this.contentHandler = ch;
        }

        /**
         * Checks if the used TransformerHandler implementation correctly handles
         * namespaces set using <code>startPrefixMapping()</code>, but wants them
         * also as 'xmlns:' attributes.<p/>
         * The check consists in sending SAX events representing a minimal namespaced document
         * with namespaces defined only with calls to <code>startPrefixMapping</code> (no
         * xmlns:xxx attributes) and check if they are present in the resulting text.
         */
        private  static boolean isRequired(SAXTransformerFactory factory)
                throws TransformerException, SAXException {
            // Serialize a minimal document to check how namespaces are handled.
            StringWriter writer = new StringWriter();

            String uri = "namespaceuri";
            String prefix = "nsp";
            String check = "xmlns:" + prefix + "='" + uri + "'";

            TransformerHandler handler = factory.newTransformerHandler();
            handler.setResult(new StreamResult(writer));

            // Output a single element
            handler.startDocument();
            handler.startPrefixMapping(prefix, uri);
            handler.startElement(uri, "element", "element", new AttributesImpl());
            handler.endElement(uri, "element", "element");
            handler.endPrefixMapping(prefix);
            handler.endDocument();

            String text = writer.toString();

            // Check if the namespace is there (replace " by ' to be sure of what we search in)
            return (text.replace('"', '\'').indexOf(check) == -1);
        }

        private void clearMappings() {
            hasMappings = false;
            prefixList.clear();
            uriList.clear();
        }

        //-------------------------------------------------< ContentHandler >---
        /**
         * @see ContentHandler#startDocument()
         */
        public void startDocument() throws SAXException {
            uriToPrefixMap.clear();
            prefixToUriMap.clear();
            clearMappings();
            contentHandler.startDocument();
        }

        /**
         * Track mappings to be able to add <code>xmlns:</code> attributes
         * in <code>startElement()</code>.
         *
         * @see org.xml.sax.ContentHandler#startPrefixMapping(String, String)
         */
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            // Store the mappings to reconstitute xmlns:attributes
            // except prefixes starting with "xml": these are reserved
            // VG: (uri != null) fixes NPE in startElement
            if (uri != null && !prefix.startsWith("xml")) {
                hasMappings = true;
                prefixList.add(prefix);
                uriList.add(uri);

                // append the prefix colon now, in order to save concatenations
                // later, but only for non-empty prefixes.
                if (prefix.length() > 0) {
                    uriToPrefixMap.put(uri, prefix + ":");
                } else {
                    uriToPrefixMap.put(uri, prefix);
                }
                prefixToUriMap.put(prefix, uri);
            }
            contentHandler.startPrefixMapping(prefix, uri);
        }

        /**
         * Ensure all namespace declarations are present as <code>xmlns:</code> attributes
         * and add those needed before calling superclass. This is a workaround for a Xalan bug
         * (at least in version 2.0.1) : <code>org.apache.xalan.serialize.SerializerToXML</code>
         * ignores <code>start/endPrefixMapping()</code>.
         */
        public void startElement(String eltUri, String eltLocalName, String eltQName, Attributes attrs)
                throws SAXException {

            // try to restore the qName. The map already contains the colon
            if (null != eltUri && eltUri.length() != 0 && uriToPrefixMap.containsKey(eltUri)) {
                eltQName = uriToPrefixMap.get(eltUri) + eltLocalName;
            }
            if (!hasMappings) {
                // no mappings present -> simply delegate.
                contentHandler.startElement(eltUri, eltLocalName, eltQName, attrs);
            } else {
                // add xmlns* attributes where needed
                AttributesImpl newAttrs = null;
                int attrCount = attrs.getLength();

                // check if namespaces present in the uri/prefix list are present
                // in the attributes and eventually add them to the attributes.
                for (int mapping = 0; mapping < prefixList.size(); mapping++) {

                    // Build infos for the namespace
                    String uri = (String) uriList.get(mapping);
                    String prefix = (String) prefixList.get(mapping);
                    String qName = prefix.length() == 0 ? "xmlns" : ("xmlns:" + prefix);

                    // Search for the corresponding xmlns* attribute
                    boolean found = false;
                    for (int attr = 0; attr < attrCount; attr++) {
                        if (qName.equals(attrs.getQName(attr))) {
                            // Check if mapping and attribute URI match
                            if (!uri.equals(attrs.getValue(attr))) {
                                throw new SAXException("URI in prefix mapping and attribute do not match");
                            }
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        // Need to add the namespace
                        if (newAttrs == null) {
                            // test if attrs passed into this call is empty in
                            // order to avoid an infinite loop (known SAX bug)
                            if (attrCount == 0) {
                                newAttrs = new AttributesImpl();
                            } else {
                                newAttrs = new AttributesImpl(attrs);
                            }
                        }

                        if (prefix.length() == 0) {
                            newAttrs.addAttribute(Name.NS_XML_URI, "xmlns", "xmlns", "CDATA", uri);
                        } else {
                            newAttrs.addAttribute(Name.NS_XML_URI, prefix, qName, "CDATA", uri);
                        }
                    }
                } // end for mapping

                // cleanup mappings for the next element
                clearMappings();

                // delegate startElement call to the wrapped content handler
                // with new attributes if adjustment for namespaces was made.
                contentHandler.startElement(eltUri, eltLocalName, eltQName, newAttrs == null ? attrs : newAttrs);
            }
        }


        /**
         * Receive notification of the end of an element.
         * Try to restore the element qName.
         *
         * @see org.xml.sax.ContentHandler#endElement(String, String, String)
         */
        public void endElement(String eltUri, String eltLocalName, String eltQName) throws SAXException {
            // try to restore the qName. The map already contains the colon
            if (null != eltUri && eltUri.length() != 0 && uriToPrefixMap.containsKey(eltUri)) {
                eltQName = uriToPrefixMap.get(eltUri) + eltLocalName;
            }
            contentHandler.endElement(eltUri, eltLocalName, eltQName);
        }

        /**
         * End the scope of a prefix-URI mapping:
         * remove entry from mapping tables.
         *
         * @see org.xml.sax.ContentHandler#endPrefixMapping(String)
         */
        public void endPrefixMapping(String prefix) throws SAXException {
            // remove mappings for xalan-bug-workaround.
            // Unfortunately, we're not passed the uri, but the prefix here,
            // so we need to maintain maps in both directions.
            if (prefixToUriMap.containsKey(prefix)) {
                uriToPrefixMap.remove(prefixToUriMap.get(prefix));
                prefixToUriMap.remove(prefix);
            }

            if (hasMappings) {
                // most of the time, start/endPrefixMapping calls have an element event between them,
                // which will clear the hasMapping flag and so this code will only be executed in the
                // rather rare occasion when there are start/endPrefixMapping calls with no element
                // event in between. If we wouldn't remove the items from the prefixList and uriList here,
                // the namespace would be incorrectly declared on the next element following the
                // endPrefixMapping call.
                int pos = prefixList.lastIndexOf(prefix);
                if (pos != -1) {
                    prefixList.remove(pos);
                    uriList.remove(pos);
                }
            }
            contentHandler.endPrefixMapping(prefix);
        }

        /**
         * @see org.xml.sax.ContentHandler#endDocument()
         */
        public void endDocument() throws SAXException {
            uriToPrefixMap.clear();
            prefixToUriMap.clear();
            clearMappings();
            contentHandler.endDocument();
        }

        /**
         * @see org.xml.sax.ContentHandler#characters(char[], int, int)
         */
        public void characters(char[] ch, int start, int length) throws SAXException {
            contentHandler.characters(ch, start, length);
        }

        /**
         * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
         */
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            contentHandler.ignorableWhitespace(ch, start, length);
        }

        /**
         * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
         */
        public void processingInstruction(String target, String data) throws SAXException {
            contentHandler.processingInstruction(target, data);
        }

        /**
         * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
         */
        public void setDocumentLocator(Locator locator) {
            contentHandler.setDocumentLocator(locator);
        }

        /**
         * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
         */
        public void skippedEntity(String name) throws SAXException {
            contentHandler.skippedEntity(name);
        }
    }
}