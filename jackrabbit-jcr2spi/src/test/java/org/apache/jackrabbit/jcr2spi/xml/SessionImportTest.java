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
package org.apache.jackrabbit.jcr2spi.xml;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.JcrConstants;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import javax.jcr.RepositoryException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.PropertyType;
import javax.jcr.Session;
import javax.jcr.Item;
import javax.jcr.Property;
import java.util.List;
import java.util.Arrays;

/**
 * <code>SessionImportTest</code>...
 */
public class SessionImportTest extends AbstractJCRTest {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testImportNameValueWithUnregisteredNamespace() throws RepositoryException, SAXException {
        String prefix = getUniquePrefix(superuser);
        String uri = getUnknownURI(superuser, "anyURI");
        String testValue = prefix + ":someLocalName";

        String svuri = Name.NS_SV_URI;
        String svprefix = Name.NS_SV_PREFIX + ":";

        ContentHandler ch = superuser.getImportContentHandler(testRootNode.getPath(), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        ch.startDocument();
        ch.startPrefixMapping(prefix, uri);

        String nN = "node";
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute(svuri, "name", svprefix + "name", "CDATA", nodeName1);
        ch.startElement(svuri, nN, svprefix + nN, attrs);

        // primary node type
        String pN = "property";
        attrs = new AttributesImpl();
        attrs.addAttribute(svuri, "name", svprefix + "name", "CDATA", JcrConstants.JCR_PRIMARYTYPE);
        attrs.addAttribute(svuri, "type", svprefix + "type", "CDATA", PropertyType.nameFromValue(PropertyType.NAME));
        ch.startElement(svuri, pN, svprefix + pN, attrs);
            ch.startElement(svuri, "value", svprefix + "value", new AttributesImpl());
            char[] val = testNodeType.toCharArray();
            ch.characters(val, 0, val.length);
            ch.endElement(svuri, "value", svprefix + "value");
        ch.endElement(svuri, pN, prefix + pN);

        // another name value
        attrs = new AttributesImpl();
        attrs.addAttribute(svuri, "name", svprefix + "name", "CDATA", propertyName1);
        attrs.addAttribute(svuri, "type", svprefix + "type", "CDATA", PropertyType.nameFromValue(PropertyType.NAME));
        ch.startElement(svuri, pN, svprefix + pN, attrs);
            ch.startElement(svuri, "value", svprefix + "value", new AttributesImpl());
            val = testValue.toCharArray();
            ch.characters(val, 0, val.length);
            ch.endElement(svuri, "value", svprefix + "value");
        ch.endElement(svuri, pN, svprefix + pN);

        ch.endElement(svuri, nN, svprefix + nN);
        ch.endDocument();

        // test if property has been imported with correct namespace
        String assignedPrefix = superuser.getNamespacePrefix(uri);
        assertTrue(superuser.getNamespaceURI(assignedPrefix).equals(uri));
        String path = testRootNode.getPath() + "/" + nodeName1 + "/" + propertyName1;

        assertTrue(superuser.itemExists(path));
        Item item = superuser.getItem(path);
        if (item.isNode()) {
            fail("Item with path " + path + " must be a property.");
        } else {
            Property prop = (Property) item;
            assertTrue(prop.getValue().getType() == PropertyType.NAME);
            String expectedValue = assignedPrefix + ":someLocalName";
            assertTrue(prop.getValue().getString().equals(expectedValue));
        }

        // try to save
        superuser.save();
    }

    private static String getUnknownURI(Session session, String uriHint) throws RepositoryException {
        String uri = uriHint;
        int index = 0;
        List uris = Arrays.asList(session.getWorkspace().getNamespaceRegistry().getURIs());
        while (uris.contains(uri)) {
            uri = uriHint + index;
            index++;
        }
        return uri;
    }

    /**
     * Returns a prefix that is unique among the already registered prefixes.
     *
     * @param uriHint namespace uri that serves as hint for the prefix generation
     * @return a unique prefix
     */
    public static String getUniquePrefix(Session session) throws RepositoryException {
        return "_pre" + (session.getNamespacePrefixes().length + 1);
    }
}