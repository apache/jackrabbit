/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the \"License\"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.webdav.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ParserTest extends TestCase {

    // see <http://en.wikipedia.org/wiki/Billion_laughs#Details>
    public void testBillionLaughs() throws UnsupportedEncodingException {

        String testBody = "<?xml version=\"1.0\"?>" + "<!DOCTYPE lolz [" + " <!ENTITY lol \"lol\">" + " <!ELEMENT lolz (#PCDATA)>"
                + " <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">"
                + " <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">"
                + " <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">"
                + " <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">"
                + " <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">"
                + " <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">"
                + " <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">"
                + " <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">"
                + " <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\">" + "]>" + "<lolz>&lol9;</lolz>";
        InputStream is = new ByteArrayInputStream(testBody.getBytes("UTF-8"));

        try {
            DomUtil.BUILDER_FACTORY.newDocumentBuilder().parse(is);
            fail("parsing this document should cause an exception");
        } catch (Exception expected) {
        }
    }

    public void testExternalEntities() throws IOException {

        String dname = "target";
        String fname = "test.xml";

        File f = new File(dname, fname);
        OutputStream os = new FileOutputStream(f);
        os.write("testdata".getBytes());
        os.close();

        String testBody = "<?xml version='1.0'?>\n<!DOCTYPE foo [" + " <!ENTITY test SYSTEM \"file:" + dname + "/" + fname + "\">"
                + "]>\n<foo>&test;</foo>";
        InputStream is = new ByteArrayInputStream(testBody.getBytes("UTF-8"));

        try {
            Document d = DomUtil.BUILDER_FACTORY.newDocumentBuilder().parse(is);
            Element root = d.getDocumentElement();
            String text = DomUtil.getText(root);
            fail("parsing this document should cause an exception, but the following external content was included: " + text);
        } catch (Exception expected) {
        }
    }
}