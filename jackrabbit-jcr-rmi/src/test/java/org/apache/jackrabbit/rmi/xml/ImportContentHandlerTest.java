/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.rmi.xml;

import java.io.UnsupportedEncodingException;

import javax.jcr.RepositoryException;

import junit.framework.TestCase;

import org.apache.jackrabbit.rmi.xml.ImportContentHandler;
import org.xml.sax.SAXException;

public class ImportContentHandlerTest extends TestCase {

    public void testImportContentHandler() throws RepositoryException, SAXException {
        // fail test if handler cannot be set up
        DummyImportContentHandler ch = new DummyImportContentHandler();

        // these may throw SAXException
        ch.startDocument();
        ch.startElement(null, "sample", "sample", null);
        ch.endElement(null, "sample", "sample");
        ch.endDocument();
        
        byte[] xml = ch.getXML();
        assertNotNull("Serialized XML is null", xml);
        assertTrue("Serialized XML is empty", xml.length > 0);
        
        // for the moment we don't actually care for the concrete contents
    }        
    
    private static class DummyImportContentHandler extends ImportContentHandler {

        private byte[] xml;
        
        DummyImportContentHandler() throws RepositoryException {
            super();
        }
        
        protected void importXML(byte[] xml) throws Exception {
            this.xml = xml;
        }
        
        byte[] getXML() {
            return xml;
        }
    }
}
