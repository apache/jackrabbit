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

import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A {@link ContentHandler} that serializes SAX events to a given
 * {@link Result} instance. The JAXP {@link SAXTransformerFactory}
 * facility is used for the serialization.
 * <p>
 * This class explicitly ensures that all namespace prefixes are also
 * present as xmlns attributes in the serialized XML document. This avoids
 * problems with Xalan's serialization behaviour which was (at least during
 * JDK 1.4) to ignore namespaces if they were not present as xmlns attributes.
 * <p>
 * NOTE: The code in this class was originally written for Apache Cocoon and
 * is included with some modifications here in Apache Jackrabbit. See the
 * org.apache.cocoon.serialization.AbstractTextSerializer class in the
 * cocoon-pipeline-impl component for the original code.
 *
 * @since Jackrabbit JCR Commons 1.5
 */
public class SerializingContentHandler extends DefaultContentHandler {

    /**
     * The character encoding used for serialization (UTF-8).
     * The encoding is fixed to make the text/xml content type safer to use.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1621">JCR-1621</a>
     */
    public static final String ENCODING = "UTF-8";

    /** The URI for xml namespaces */
    private static final String XML = "http://www.w3.org/XML/1998/namespace";

    /**
     * The factory used to create serializing SAX transformers.
     */
    private static final SAXTransformerFactory FACTORY =
        // Note that the cast from below is strictly speaking only valid when
        // the factory instance supports the SAXTransformerFactory.FEATURE
        // feature. But since this class would be useless without this feature,
        // it's no problem to fail with a ClassCastException here and prevent
        // this class from even being loaded. AFAIK all common JAXP
        // implementations do support this feature.
        (SAXTransformerFactory) TransformerFactory.newInstance();

    /**
     * Flag that indicates whether we need to work around the issue of
     * the serializer not automatically generating the required xmlns
     * attributes for the namespaces used in the document.
     */
    private static final boolean NEEDS_XMLNS_ATTRIBUTES =
        needsXmlnsAttributes();

    /**
     * Probes the available XML serializer for xmlns support. Used to set
     * the value of the {@link #NEEDS_XMLNS_ATTRIBUTES} flag.
     *
     * @return whether the XML serializer needs explicit xmlns attributes
     */
    private static boolean needsXmlnsAttributes() {
        try {
            StringWriter writer = new StringWriter();
            TransformerHandler probe = FACTORY.newTransformerHandler();
            probe.setResult(new StreamResult(writer));
            probe.startDocument();
            probe.startPrefixMapping("p", "uri");
            probe.startElement("uri", "e", "p:e", new AttributesImpl());
            probe.endElement("uri", "e", "p:e");
            probe.endPrefixMapping("p");
            probe.endDocument();
            return writer.toString().indexOf("xmlns") == -1;
        } catch (Exception e) {
            throw new UnsupportedOperationException("XML serialization fails");
        }
    }

    /**
     * Creates a serializing content handler that writes to the given stream.
     *
     * @param output serialization target
     * @return serializing content handler
     * @throws SAXException if the content handler could not be initialized
     */
    public static DefaultHandler getSerializer(OutputStream output)
            throws SAXException {
        return getSerializer(new StreamResult(output));
    }

    /**
     * Creates a serializing content handler that writes to the given writer.
     *
     * @param writer serialization target
     * @return serializing content handler
     * @throws SAXException if the content handler could not be initialized
     */
    public static DefaultHandler getSerializer(Writer writer)
            throws SAXException {
        return getSerializer(new StreamResult(writer));
    }

    /**
     * Creates a serializing content handler that writes to the given result.
     *
     * @param result serialization target
     * @return serializing content handler
     * @throws SAXException if the content handler could not be initialized
     */
    public static DefaultHandler getSerializer(Result result)
            throws SAXException {
        try {
            TransformerHandler handler = FACTORY.newTransformerHandler();
            handler.setResult(result);

            // Specify the output properties to avoid surprises especially in
            // character encoding or the output method (might be html for some
            // documents!)
            Transformer transformer = handler.getTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, ENCODING);
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            if (NEEDS_XMLNS_ATTRIBUTES) {
                // The serializer does not output xmlns declarations,
                // so we need to do it explicitly with this wrapper
                return new SerializingContentHandler(handler);
            } else {
                return new DefaultContentHandler(handler);
            }
        } catch (TransformerConfigurationException e) {
            throw new SAXException("Failed to initialize XML serializer", e);
        }
    }

    /**
     * The prefixes of startPrefixMapping() declarations for the coming element.
     */
    private List prefixList = new ArrayList();

    /**
     * The URIs of startPrefixMapping() declarations for the coming element.
     */
    private List uriList = new ArrayList();

    /**
     * Maps of URI<->prefix mappings. Used to work around a bug in the Xalan
     * serializer.
     */
    private Map uriToPrefixMap = new HashMap();
    private Map prefixToUriMap = new HashMap();

    /**
     * True if there has been some startPrefixMapping() for the coming element.
     */
    private boolean hasMappings = false;

    /**
     * Stack of the prefixes of explicitly generated prefix mapping calls
     * per each element level. An entry is appended at the beginning of each
     * {@link #startElement(String, String, String, Attributes)} call and
     * removed at the end of each {@link #endElement(String, String, String)}
     * call. By default the entry for each element is <code>null</code> to
     * avoid losing performance, but whenever the code detects a new prefix
     * mapping that needs to be registered, the <code>null</code> entry is
     * replaced with a list of explicitly registered prefixes for that node.
     * When that element is closed, the listed prefixes get unmapped.
     *
     * @see #checkPrefixMapping(String, String)
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1767">JCR-1767</a>
     */
    private final List addedPrefixMappings = new ArrayList();

    private SerializingContentHandler(ContentHandler handler) {
        super(handler);
    }

    public void startDocument() throws SAXException {
        // Cleanup
        this.uriToPrefixMap.clear();
        this.prefixToUriMap.clear();
        clearMappings();
        super.startDocument();
    }

    /**
     * Track mappings to be able to add <code>xmlns:</code> attributes
     * in <code>startElement()</code>.
     */
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        // Store the mappings to reconstitute xmlns:attributes
        // except prefixes starting with "xml": these are reserved
        // VG: (uri != null) fixes NPE in startElement
        if (uri != null && !prefix.startsWith("xml")) {
            this.hasMappings = true;
            this.prefixList.add(prefix);
            this.uriList.add(uri);

            // append the prefix colon now, in order to save concatenations later, but
            // only for non-empty prefixes.
            if (prefix.length() > 0) {
                this.uriToPrefixMap.put(uri, prefix + ":");
            } else {
                this.uriToPrefixMap.put(uri, prefix);
            }

            this.prefixToUriMap.put(prefix, uri);
        }
        super.startPrefixMapping(prefix, uri);
    }

    /**
     * Checks whether a prefix mapping already exists for the given namespace
     * and generates the required {@link #startPrefixMapping(String, String)}
     * call if the mapping is not found. By default the registered prefix
     * is taken from the given qualified name, but a different prefix is
     * automatically selected if that prefix is already used.
     *
     * @see <a href="https://issues.apache.org/jira/browse/JCR-1767">JCR-1767</a>
     * @param uri namespace URI
     * @param qname element name with the prefix, or <code>null</code>
     * @throws SAXException if the prefix mapping can not be added
     */
    private void checkPrefixMapping(String uri, String qname)
            throws SAXException {
        // Only add the prefix mapping if the URI is not already known
        if (uri != null && uri.length() > 0 && !uri.startsWith("xml")
                && !uriToPrefixMap.containsKey(uri)) {
            // Get the prefix
            String prefix = "ns";
            if (qname != null && qname.length() > 0) {
                int colon = qname.indexOf(':');
                if (colon != -1) {
                    prefix = qname.substring(0, colon);
                }
            }

            // Make sure that the prefix is unique
            String base = prefix;
            for (int i = 2; prefixToUriMap.containsKey(prefix); i++) {
                prefix = base + i;
            }

            int last = addedPrefixMappings.size() - 1;
            List prefixes = (List) addedPrefixMappings.get(last);
            if (prefixes == null) {
                prefixes = new ArrayList();
                addedPrefixMappings.set(last, prefixes);
            }
            prefixes.add(prefix);

            startPrefixMapping(prefix, uri);
        }
    }

    /**
     * Ensure all namespace declarations are present as <code>xmlns:</code> attributes
     * and add those needed before calling superclass. This is a workaround for a Xalan bug
     * (at least in version 2.0.1) : <code>org.apache.xalan.serialize.SerializerToXML</code>
     * ignores <code>start/endPrefixMapping()</code>.
     */
    public void startElement(
            String eltUri, String eltLocalName, String eltQName, Attributes attrs)
            throws SAXException {
        // JCR-1767: Generate extra prefix mapping calls where needed
        addedPrefixMappings.add(null);
        checkPrefixMapping(eltUri, eltQName);
        for (int i = 0; i < attrs.getLength(); i++) {
            checkPrefixMapping(attrs.getURI(i), attrs.getQName(i));
        }

        // try to restore the qName. The map already contains the colon
        if (null != eltUri && eltUri.length() != 0 && this.uriToPrefixMap.containsKey(eltUri)) {
            eltQName = this.uriToPrefixMap.get(eltUri) + eltLocalName;
        }
        if (this.hasMappings) {
            // Add xmlns* attributes where needed

            // New Attributes if we have to add some.
            AttributesImpl newAttrs = null;

            int mappingCount = this.prefixList.size();
            int attrCount = attrs.getLength();

            for (int mapping = 0; mapping < mappingCount; mapping++) {

                // Build infos for this namespace
                String uri = (String) this.uriList.get(mapping);
                String prefix = (String) this.prefixList.get(mapping);
                String qName = prefix.equals("") ? "xmlns" : ("xmlns:" + prefix);

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
                    // Need to add this namespace
                    if (newAttrs == null) {
                        // Need to test if attrs is empty or we go into an infinite loop...
                        // Well know SAX bug which I spent 3 hours to remind of :-(
                        if (attrCount == 0) {
                            newAttrs = new AttributesImpl();
                        } else {
                            newAttrs = new AttributesImpl(attrs);
                        }
                    }

                    if (prefix.equals("")) {
                        newAttrs.addAttribute(XML, qName, qName, "CDATA", uri);
                    } else {
                        newAttrs.addAttribute(XML, prefix, qName, "CDATA", uri);
                    }
                }
            } // end for mapping

            // Cleanup for the next element
            clearMappings();

            // Start element with new attributes, if any
            super.startElement(eltUri, eltLocalName, eltQName, newAttrs == null ? attrs : newAttrs);
        } else {
            // Normal job
            super.startElement(eltUri, eltLocalName, eltQName, attrs);
        }
    }


    /**
     * Receive notification of the end of an element.
     * Try to restore the element qName.
     */
    public void endElement(String eltUri, String eltLocalName, String eltQName) throws SAXException {
        // try to restore the qName. The map already contains the colon
        if (null != eltUri && eltUri.length() != 0 && this.uriToPrefixMap.containsKey(eltUri)) {
            eltQName = this.uriToPrefixMap.get(eltUri) + eltLocalName;
        }

        super.endElement(eltUri, eltLocalName, eltQName);

        // JCR-1767: Generate extra prefix un-mapping calls where needed
        int last = addedPrefixMappings.size() - 1;
        List prefixes = (List) addedPrefixMappings.remove(last);
        if (prefixes != null) {
            Iterator iterator = prefixes.iterator();
            while (iterator.hasNext()) {
                endPrefixMapping((String) iterator.next());
            }
        }
    }

    /**
     * End the scope of a prefix-URI mapping:
     * remove entry from mapping tables.
     */
    public void endPrefixMapping(String prefix) throws SAXException {
        // remove mappings for xalan-bug-workaround.
        // Unfortunately, we're not passed the uri, but the prefix here,
        // so we need to maintain maps in both directions.
        if (this.prefixToUriMap.containsKey(prefix)) {
            this.uriToPrefixMap.remove(this.prefixToUriMap.get(prefix));
            this.prefixToUriMap.remove(prefix);
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

        super.endPrefixMapping(prefix);
    }

    public void endDocument() throws SAXException {
        // Cleanup
        this.uriToPrefixMap.clear();
        this.prefixToUriMap.clear();
        clearMappings();
        super.endDocument();
    }

    private void clearMappings() {
        this.hasMappings = false;
        this.prefixList.clear();
        this.uriList.clear();
    }

}
