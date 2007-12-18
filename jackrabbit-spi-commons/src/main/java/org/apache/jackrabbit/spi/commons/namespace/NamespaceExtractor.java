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
package org.apache.jackrabbit.spi.commons.namespace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

import javax.jcr.NamespaceException;
import java.util.Map;
import java.util.HashMap;
import java.io.FileInputStream;

/**
 * Extracts namespace mapping information from an XML file.
 * XML file is parsed and all startPrefixMapping events
 * are intercepted. Scoping of prefix mapping within the XML file
 * may result in multiple namespace using the same prefix. This
 * is handled by mangling the prefix when required.
 *
 * The resulting NamespaceMapping implements NamespaceResolver
 * and can be used by tools (such as o.a.j.tools.nodetype.CompactNodeTypeDefWriter)
 * to resolve namespaces.
 */
public class NamespaceExtractor {
    private static Logger log = LoggerFactory.getLogger(NamespaceExtractor.class);
    private final NamespaceMapping mapping = new NamespaceMapping();
    private final Map basePrefixes = new HashMap();
    private String defaultBasePrefix;

    /**
     * Constructor
     * @param fileName
     * @param dpb
     * @throws NamespaceException
     */
    public NamespaceExtractor(String fileName, String dpb) throws NamespaceException {
        defaultBasePrefix = dpb;
        try{
            ContentHandler handler = new NamespaceExtractor.NamespaceHandler();
            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler(handler);
            parser.parse(new InputSource(new FileInputStream(fileName)));
        } catch(Exception e){
            throw new NamespaceException();
        }
    }

    /**
     * getNamespaceMapping
     * @return a NamespaceMapping
     */
    public NamespaceMapping getNamespaceMapping(){
        return mapping;
    }

    /**
     * SAX ContentHandler that reacts to namespace mappings in incoming XML.
     */
    private class NamespaceHandler extends DefaultHandler {
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            if (uri == null) uri = "";

            //Replace the empty prefix with the defaultBasePrefix
            if (prefix == null || prefix.equals("")){
                prefix = defaultBasePrefix;
            }

            try{
                // if prefix already used
                if (mapping.hasPrefix(prefix)){
                    int c;
                    Integer co = (Integer) basePrefixes.get(prefix);
                    if (co == null) {
                        basePrefixes.put(prefix, new Integer(1));
                        c = 1;
                    } else {
                        c = co.intValue() + 1;
                        basePrefixes.put(prefix, new Integer(c));
                    }
                    prefix = prefix + "_" + c;
                }
                mapping.setMapping(prefix, uri);
            } catch(NamespaceException e){
                String msg = e.getMessage();
                log.debug(msg);
            }
        }
    }
}
