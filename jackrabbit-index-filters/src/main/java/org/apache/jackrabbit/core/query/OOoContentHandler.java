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
package org.apache.jackrabbit.core.query;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OOoContentHandler extends DefaultHandler {

    private StringBuffer content;
    private boolean appendChar;

    public OOoContentHandler() {
        content = new StringBuffer();
        appendChar = false;
    }

    /**
     * Returns the text content extracted from parsed content.xml
     */
    public String getContent() {
        return content.toString();
    }

    public void startElement(String namespaceURI, String localName,
                             String rawName, Attributes atts)
            throws SAXException {
        if (rawName.startsWith("text:")) {
            appendChar = true;
        }
    }

    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (appendChar) {
            content.append(ch, start, length).append(" ");
        }
    }

    public void endElement(java.lang.String namespaceURI,
                           java.lang.String localName,
                           java.lang.String qName)
            throws SAXException {
        appendChar = false;
    }
}
