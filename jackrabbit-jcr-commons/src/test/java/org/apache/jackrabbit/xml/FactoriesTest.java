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
package org.apache.jackrabbit.xml;

import org.apache.jackrabbit.commons.xml.Factories;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.util.UUID;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FactoriesTest {

    String randomName = UUID.randomUUID().toString();

    String xxeXml =
            "<!DOCTYPE root [\n" +
                    "  <!ELEMENT root ANY >\n" +
                    "  <!ENTITY xxe SYSTEM \"file:///tmp/" + randomName + "\" >]>\n" +
                    "<root>&xxe;</root>";

    @Test
    // verify that default builder is vulnerable
    public void testDefaultParserDoesResolveExternalEntities() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = null;

        try (ByteArrayInputStream input = new ByteArrayInputStream(xxeXml.getBytes())) {
            doc = db.parse(input);
        } catch (FileNotFoundException expected) {
            assertTrue("FileNotFoundException message should contain file name '"
                    + randomName + "', but was: '" + expected.getMessage() + "'", expected.getMessage().contains(randomName));
        }

        assertNull("There should be no doc: " + doc, doc);
    }

    @Test
    // verify that utility builder is not vulnerable
    public void testParserDoesNotResolveExternalEntities() throws Exception {
        DocumentBuilderFactory dbf = Factories.safeDocumentBuilderFactory();

        DocumentBuilder db = dbf.newDocumentBuilder();

        try (ByteArrayInputStream input = new ByteArrayInputStream(xxeXml.getBytes())) {
            db.parse(input);
        } catch (SAXParseException expected) {
            // all good
        }
    }

    @Test
    public void testSafeEntityResolver() {
        EntityResolver test = Factories.nonResolvingEntityResolver();
        assertThrows(Exception.class, () -> test.resolveEntity("-//The Apache Software Foundation//DTD Jackrabbit 1.6//EN" ,
                "http://jackrabbit.apache.org/dtd/repository-1.6.dtd"));
    }
}
