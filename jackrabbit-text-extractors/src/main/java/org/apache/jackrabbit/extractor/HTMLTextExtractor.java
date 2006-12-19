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

import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;

/**
 * Text extractor for HyperText Markup Language (HTML).
 */
public class HTMLTextExtractor extends AbstractTextExtractor {

    /**
     * Creates a new <code>HTMLTextExtractor</code> instance.
     */
    public HTMLTextExtractor() {
        super(new String[]{"text/html"});
    }

    //-------------------------------------------------------< TextExtractor >

    /**
     * {@inheritDoc}
     */
    public Reader extractText(InputStream stream,
                              String type,
                              String encoding) throws IOException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            HTMLParser parser = new HTMLParser();
            SAXResult result = new SAXResult(new DefaultHandler());

            SAXSource source = new SAXSource(parser, new InputSource(stream));
            transformer.transform(source, result);

            return new StringReader(parser.getContents());
        } catch (TransformerConfigurationException e) {
            return new StringReader("");
        } catch (TransformerException e) {
            return new StringReader("");
        } finally {
            stream.close();
        }
    }
}
