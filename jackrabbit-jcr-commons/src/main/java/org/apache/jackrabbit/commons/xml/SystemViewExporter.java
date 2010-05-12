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

import java.io.IOException;
import java.io.Writer;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.value.ValueHelper;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * System view exporter.
 *
 * @since Jackrabbit JCR Commons 1.5
 */
public class SystemViewExporter extends Exporter {

    /**
     * The <code>sv</code> namespace URI.
     */
    private static final String SV = "http://www.jcp.org/jcr/sv/1.0";

    /**
     * The <code>xs</code> namespace URI.
     */
    private static final String XS = "http://www.w3.org/2001/XMLSchema";

    /**
     * The <code>xsi</code> namespace URI.
     */
    private static final String XSI =
        "http://www.w3.org/2001/XMLSchema-instance";

    /**
     * Creates a system view exporter.
     *
     * @param session current session
     * @param handler SAX event handler for the export
     * @param recurse whether to recursively export the whole subtree
     * @param binary whether to export binary values
     */
    public SystemViewExporter(
            Session session, ContentHandler handler,
            boolean recurse, boolean binary) {
        super(session, handler, recurse, binary);
        addNamespace("sv", SV);
    }

    /**
     * Exports the given node as an <code>sv:node</code> element.
     */
    protected void exportNode(String uri, String local, Node node)
            throws RepositoryException, SAXException {
        addAttribute(SV, "name", getXMLName(uri, local));
        startElement(SV, "node");
        exportProperties(node);
        exportNodes(node);
        endElement(SV, "node");
    }

    /**
     * Calls {@link #exportProperty(String, String, int, Value[])}.
     */
    protected void exportProperty(String uri, String local, Value value)
            throws RepositoryException, SAXException {
        // start property element
        addAttribute(SV, "name", getXMLName(uri, local));
        addAttribute(SV, "type", PropertyType.nameFromValue(value.getType()));
        startElement(SV, "property");
        // value
        exportValue(value);
        endElement(SV, "property");
    }

    /**
     * Exports the given property as an <code>sv:property</code> element.
     */
    protected void exportProperty(
            String uri, String local, int type, Value[] values)
            throws RepositoryException, SAXException {
        // start property element
        addAttribute(SV, "name", getXMLName(uri, local));
        addAttribute(SV, "type", PropertyType.nameFromValue(type));
        addAttribute(SV, "multiple", Boolean.TRUE.toString());
        startElement(SV, "property");
        // values
        for (int i = 0; i < values.length; i++) {
            exportValue(values[i]);
        }
        endElement(SV, "property");
    }

    /**
     * Exports the given value as an <code>sv:value</code> element.
     *
     * @param value value to be exported
     */
    private void exportValue(Value value)
            throws RepositoryException, SAXException {
        try {
            boolean binary = mustSendBinary(value);
            if (binary) {
                addNamespace("xs", XS);
                addNamespace("xsi", XSI);
                addAttribute(XSI, "type", getXMLName(XS, "base64Binary"));
            }
            startElement(SV, "value");
            ValueHelper.serialize(value, false, binary, new Writer() {
                public void write(char[] cbuf, int off, int len)
                        throws IOException {
                    try {
                        SystemViewExporter.this.characters(cbuf, off, len);
                    } catch (Exception e) {
                        IOException exception = new IOException();
                        exception.initCause(e);
                        throw exception;
                    }
                }
                public void close() {
                }
                public void flush() {
                }
            });
            endElement(SV, "value");
        } catch (IOException e) {
            // check if the exception wraps a SAXException
            // (see Writer.write(char[], int, int) above)
            if (e.getCause() instanceof SAXException) {
                throw (SAXException) e.getCause();
            } else {
                throw new RepositoryException(e);
            }
        }
    }

    /**
     * Utility method for determining whether a non-binary value should
     * still be exported in base64 encoding to avoid emitting invalid XML
     * characters.
     *
     * @param value value to be exported
     * @return whether the value should be treated as a binary
     * @throws RepositoryException if a repository error occurs
     */
    private boolean mustSendBinary(Value value) throws RepositoryException {
        if (value.getType() != PropertyType.BINARY) {
            String string = value.getString();
            for (int i = 0; i < string.length(); i++) {
                char c = string.charAt(i);
                if (c >= 0 && c < 32 && c != '\n' && c != '\t') {
                    return true;
                }
            }
        }
        return false;
    }

}
