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

import java.io.StringReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.BLOBFileValue;
import org.apache.jackrabbit.core.value.InternalValue;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Extracts texts from HTML documents. It uses nekohtml library.
 */
public class HTMLTextFilter implements TextFilter {

    private HTMLParser parser;
    private SAXResult result;
    private Transformer transformer;

    /**
     * @return <code>true</code> for <code>text/xml</code>, <code>false</code>
     *         otherwise.
     */
    public boolean canFilter(String mimeType) {
        return "text/html".equalsIgnoreCase(mimeType);
    }

    /**
     * Returns a map with a single entry for field {@link FieldNames#FULLTEXT}.
     *
     * @param data     object containing RTF document data
     * @param encoding text encoding is not used, since it is specified in the
     *                 data.
     * @return a map with a single Reader value for field {@link
     *         FieldNames#FULLTEXT}.
     * @throws RepositoryException if data is a multi-value property or if the
     *                             content can't be extracted
     */
    public Map doFilter(PropertyState data, String encoding)
            throws RepositoryException {

        if (parser == null) {
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
                        SAXSource source =
                                new SAXSource(parser,
                                        new InputSource(in));
                        transformer.transform(source, result);

                        String text = parser.getContents();

                        delegate = new StringReader(text);
                    } catch (TransformerException e) {
                        throw new IOException(e.getMessage());
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

    /**
     * Inits the parser engine
     *
     * @throws javax.jcr.RepositoryException If some error happens
     */
    private void initParser() throws javax.jcr.RepositoryException {


        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            transformer = factory.newTransformer();
            parser = new HTMLParser();
            result = new SAXResult(new DefaultHandler());
        } catch (TransformerConfigurationException e) {
            throw new RepositoryException(e);
        } catch (TransformerFactoryConfigurationError e) {
            throw new RepositoryException(e);
        }
    }
}
