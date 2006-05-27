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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;

import java.io.IOException;
import java.io.StringReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.SAXException;

/**
 * Extracts texts from OpenOffice document data.
 */
public class OpenOfficeTextFilter implements TextFilter {
    private XMLReader xmlReader;

    public boolean canFilter(String mimeType) {
        return "application/vnd.oasis.opendocument.database".equalsIgnoreCase(mimeType) ||
                "application/vnd.oasis.opendocument.formula".equalsIgnoreCase(mimeType) ||
                "application/vnd.oasis.opendocument.graphics".equalsIgnoreCase(mimeType) ||
                "application/vnd.oasis.opendocument.presentation".equalsIgnoreCase(mimeType) ||
                "application/vnd.oasis.opendocument.spreadsheet".equalsIgnoreCase(mimeType) ||
                "application/vnd.oasis.opendocument.text".equalsIgnoreCase(mimeType);
    }

    public Map doFilter(PropertyState data, String encoding)
            throws RepositoryException {
        if (xmlReader == null) {
            initParser();
        }

        InternalValue[] values = data.getValues();
        if (values.length > 0) {
            final BLOBFileValue blob = (BLOBFileValue) values[0].internalValue();

            LazyReader reader = new LazyReader() {
                protected void initializeReader() throws IOException {
                    InputStream in;
                    try {
                        in = blob.getStream();
                    } catch (RepositoryException e) {
                        throw new IOException(e.getMessage());
                    }
                    try {
                        ZipInputStream zis = new ZipInputStream(in);
                        ZipEntry ze = zis.getNextEntry();
                        while (!ze.getName().equals("content.xml")) {
                            ze = zis.getNextEntry();
                        }
                        OOoContentHandler contentHandler = new OOoContentHandler();
                        xmlReader.setContentHandler(contentHandler);
                        try {
                            xmlReader.parse(new InputSource(zis));
                        } catch (SAXException e) {
                            throw new IOException(e.getMessage());
                        } finally {
                            zis.close();
                        }

                        delegate = new StringReader(contentHandler.getContent());
                    } finally {
                        in.close();
                    }
                }
            };

            Map result = new HashMap();
            result.put(FieldNames.FULLTEXT, reader);
            return result;
        } else {
            // multi value not supported
            throw new RepositoryException("Multi-valued binary properties not supported.");
        }

    }

    private void initParser() throws RepositoryException {
        try {
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setValidating(false);
            SAXParser saxParser = saxParserFactory.newSAXParser();
            xmlReader = saxParser.getXMLReader();
            xmlReader.setFeature("http://xml.org/sax/features/validation", false);
            xmlReader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception e) {
            throw new RepositoryException(e);
        }
    }

}
