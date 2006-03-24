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
package org.apache.jackrabbit.core.query;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

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
        ZipInputStream zis = null;
        if (xmlReader == null) {
            initParser();
        }

        InternalValue[] values = data.getValues();
        if (values.length > 0) {
            BLOBFileValue blob = (BLOBFileValue) values[0].internalValue();

            try {
                zis = new ZipInputStream(blob.getStream());
                ZipEntry ze = zis.getNextEntry();
                while (!ze.getName().equals("content.xml"))
                    ze = zis.getNextEntry();
                OOoContentHandler contentHandler = new OOoContentHandler();
                xmlReader.setContentHandler(contentHandler);
                xmlReader.parse(new InputSource(zis));
                zis.close();

                Map result = new HashMap();
                result.put(FieldNames.FULLTEXT, new StringReader(contentHandler.getContent()));
                return result;
            } catch (Exception ex) {
                throw new RepositoryException(ex);
            } finally {
                if (zis != null) {
                    try {
                        zis.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
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
