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
package org.apache.jackrabbit.webdav.jcr.observation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.jackrabbit.webdav.observation.ObservationConstants;
import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.Namespace;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

public class InfoMapTest extends TestCase {

    public void testInfoMap()
            throws ParserConfigurationException, TransformerException, SAXException, IOException, RepositoryException {

        Session s = Mockito.mock(Session.class);
        Mockito.when(s.getNamespaceURI("jcr")).thenReturn("http://www.jcp.org/jcr/1.0");

        Map<String, String> map = new HashMap<String, String>();
        // mandated by JCR 2.0
        map.put("srcChildRelPath", "/x");
        // OAK extension, see https://issues.apache.org/jira/browse/OAK-1669
        map.put("jcr:primaryType", "nt:unstructured");
        Document doc = DomUtil.createDocument();
        Element container = DomUtil.createElement(doc, "x", null);
        doc.appendChild(container);
        SubscriptionImpl.serializeInfoMap(container, s, map);
        ByteArrayOutputStream xml = new ByteArrayOutputStream();
        DomUtil.transformDocument(doc, xml);

        // reparse
        Document tripped = DomUtil.parseDocument(new ByteArrayInputStream(xml.toByteArray()));
        Element top = tripped.getDocumentElement();
        assertEquals("x", top.getLocalName());
        Element emap = DomUtil.getChildElement(top, ObservationConstants.N_EVENTINFO);
        assertNotNull(emap);
        Element path = DomUtil.getChildElement(emap, "srcChildRelPath", null);
        assertNotNull(path);
        Element type = DomUtil.getChildElement(emap, "primaryType", Namespace.getNamespace("http://www.jcp.org/jcr/1.0"));
        assertNotNull(type);
    }
}
