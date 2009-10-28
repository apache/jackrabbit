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
package org.apache.jackrabbit.extractor;

import org.apache.xerces.parsers.AbstractSAXParser;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.NamespaceContext;
import org.apache.xerces.xni.XMLLocator;
import org.apache.xerces.xni.XMLString;
import org.apache.xerces.xni.XNIException;
import org.cyberneko.html.HTMLConfiguration;

/**
 * Helper class for HTML parsing
 */
public class HTMLParser extends AbstractSAXParser {

    private StringBuffer buffer;

    public HTMLParser() {

        super(new HTMLConfiguration());
    }

    public void startDocument(XMLLocator arg0,
                              String arg1,
                              NamespaceContext arg2,
                              Augmentations arg3) throws XNIException {

        super.startDocument(arg0, arg1, arg2, arg3);

        buffer = new StringBuffer();
    }

    public void characters(XMLString xmlString, Augmentations augmentations)
            throws XNIException {

        super.characters(xmlString, augmentations);

        buffer.append(xmlString.toString());
    }

    private String filterAndJoin(String text) {

        boolean space = false;
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if ((c == '\n') || (c == ' ') || Character.isWhitespace(c)) {
                if (space) {
                    continue;
                } else {
                    space = true;
                    buffer.append(' ');
                    continue;
                }
            } else {
                if (!Character.isLetterOrDigit(c)) {
                    if (!space) {
                        space = true;
                        buffer.append(' ');
                        continue;
                    }
                    continue;
                }
            }
            space = false;
            buffer.append(c);
        }
        return buffer.toString();
    }

    /**
     * Returns parsed content
     *
     * @return String Parsed content
     */
    public String getContents() {

        String text = filterAndJoin(buffer.toString());
        return text;
    }
}
