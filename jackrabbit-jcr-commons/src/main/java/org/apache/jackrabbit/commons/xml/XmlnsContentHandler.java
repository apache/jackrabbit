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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Content handler proxy that adds explicit "xmlns" attributes for all
 * namespace mappings introduced through
 * {@link #startPrefixMapping(String, String)} calls.
 */
public class XmlnsContentHandler extends ProxyContentHandler {

    /**
     * Namespace of the "xmlns" attributes.
     */
    private static final String XMLNS_NAMESPACE =
        "http://www.w3.org/2000/xmlns/";

    /**
     * Namespace mappings recorded for the next element.
     */
    private final LinkedHashMap namespaces = new LinkedHashMap();

    public XmlnsContentHandler(ContentHandler handler) {
        super(handler);
    }

    //------------------------------------------------------< ContentHandler >

    /**
     * Records the namespace mapping and passes the call to the proxied
     * content handler.
     */
    public void startPrefixMapping(String prefix, String uri)
            throws SAXException {
        namespaces.put(prefix, uri);
        super.startPrefixMapping(prefix, uri);
    }

    /**
     * Adds the recorded namespace mappings (if any) as "xmlns" attributes
     * before passing the call on to the proxied content handler.
     */
    public void startElement(
            String namespaceURI, String localName, String qName,
            Attributes atts) throws SAXException {
        if (!namespaces.isEmpty()) {
            AttributesImpl attributes = new AttributesImpl(atts);
            Iterator iterator = namespaces.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String prefix = (String) entry.getKey();
                String uri = (String) entry.getValue();
                if (prefix.length() == 0) {
                    attributes.addAttribute(
                            XMLNS_NAMESPACE, "xmlns", "xmlns",
                            "CDATA", uri);
                } else {
                    attributes.addAttribute(
                            XMLNS_NAMESPACE, prefix, "xmlns:" + prefix,
                            "CDATA", uri);
                }
            }
            atts = attributes;
            namespaces.clear();
        }
        super.startElement(namespaceURI, localName, qName, atts);
    }

}
