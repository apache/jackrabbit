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
package org.apache.jackrabbit.core.data;

import org.apache.jackrabbit.test.AbstractJCRTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

/**
 * Test importing and exporting large binary and text objects.
 */
public class ExportImportTest extends AbstractJCRTest {

    /**
     * Test importing a large text property with Unicode characters larger than 255.
     */
    public void testExportImportText() throws RepositoryException {
        doTestExportImportLargeText("Hello \t\r\n!".toCharArray());
        doTestExportImportLargeText("World\f\f\f.".toCharArray());
        doTestExportImportLargeText("Hello\t\n\n.\n".toCharArray());
        doTestExportImportLargeRandomText(100);
        doTestExportImportLargeRandomText(10000);
        doTestExportImportLargeRandomText(100000);
    }
    
    private void doTestExportImportLargeRandomText(int len) throws RepositoryException {
        char[] chars = new char[len];
        Random random = new Random(1);
        // The UCS code values 0xd800-0xdfff (UTF-16 surrogates)
        // as well as 0xfffe and 0xffff (UCS non-characters)
        // should not appear in conforming UTF-8 streams.
        // (String.getBytes("UTF-8") only returns 1 byte for 0xd800-0xdfff)            
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) random.nextInt(0xd000);
        }
        doTestExportImportLargeText(chars);
    }
    
    private void doTestExportImportLargeText(char[] chars) throws RepositoryException {
        Session session = getHelper().getReadWriteSession();
        try {
            Node root = session.getRootNode();
            clean(root);
            Node test = root.addNode("testText");
            session.save();
            String s = new String(chars);
            test.setProperty("text", s);
            session.save();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            session.exportSystemView("/testText", out, false, false);
            byte[] output = out.toByteArray();
            Node test2 = root.addNode("testText2");
            Node test3 = root.addNode("testText3");
            session.save();
            session.importXML("/testText2", new ByteArrayInputStream(output), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            session.save();
            session.getWorkspace().importXML("/testText3", new ByteArrayInputStream(output), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            test2 = root.getNode("testText2");
            test2 = test2.getNode("testText");
            test3 = root.getNode("testText3");
            test3 = test3.getNode("testText");
            String s2 = test2.getProperty("text").getString();
            String s3 = test3.getProperty("text").getString();
            assertEquals(s.length(), s2.length());
            assertEquals(s.length(), s3.length());
            assertEquals(s, s2);
            assertEquals(s, s3);
            clean(root);
        } catch (Exception e) {
            e.printStackTrace();
            assertFalse(e.getMessage(), true);
        } finally {
            session.logout();
        }
    }

    
    /**
     * Test a node type with a binary default value
     * @throws RepositoryException
     */
    public void testExportImportBinary() throws RepositoryException {
        doTestExportImportBinary(0);
        doTestExportImportBinary(10);
        doTestExportImportBinary(10000);
        doTestExportImportBinary(100000);
    }

    private void doTestExportImportBinary(int len) throws RepositoryException {
        Session session = getHelper().getReadWriteSession();
        try {
            Node root = session.getRootNode();
            clean(root);
            Node test = root.addNode("testBinary");
            session.save();
            byte[] data = new byte[len];
            Random random = new Random(1);
            random.nextBytes(data);
            ValueFactory vf = session.getValueFactory();
            test.setProperty("data", vf.createBinary(new ByteArrayInputStream(data)));
            session.save();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            session.exportSystemView("/testBinary", out, false, false);
            byte[] output = out.toByteArray();
            Node test2 = root.addNode("testBinary2");
            Node test3 = root.addNode("testBinary3");
            session.save();
            session.importXML("/testBinary2", new ByteArrayInputStream(output), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            session.save();
            session.getWorkspace().importXML("/testBinary3", new ByteArrayInputStream(output), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            test2 = root.getNode("testBinary2");
            test2 = test2.getNode("testBinary");
            test3 = root.getNode("testBinary3");
            test3 = test3.getNode("testBinary");
            byte[] data2 = readFromStream(test2.getProperty("data").getBinary().getStream());
            byte[] data3 = readFromStream(test3.getProperty("data").getBinary().getStream());
            assertEquals(data.length, data2.length);
            assertEquals(data.length, data3.length);
            for (int i = 0; i < len; i++) {
                assertEquals(data[i], data2[i]);
                assertEquals(data[i], data3[i]);
            }
            clean(root);
        } catch (Exception e) {
            e.printStackTrace();
            assertFalse(e.getMessage(), true);
        } finally {
            session.logout();
        }
    }

    private byte[] readFromStream(InputStream in) throws IOException {
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        while (true) {
            int x = in.read();
            if (x < 0) {
                break;
            }
            out2.write(x);
        }
        return out2.toByteArray();
    }

    private void clean(Node root) throws RepositoryException {
        while (root.hasNode("testBinary")) {
            root.getNode("testBinary").remove();
        }
        while (root.hasNode("testBinary2")) {
            root.getNode("testBinary2").remove();
        }
        while (root.hasNode("testBinary3")) {
            root.getNode("testBinary3").remove();
        }
        while (root.hasNode("testText")) {
            root.getNode("testText").remove();
        }
        while (root.hasNode("testText2")) {
            root.getNode("testText2").remove();
        }
        while (root.hasNode("testText3")) {
            root.getNode("testText3").remove();
        }
        root.getSession().save();
    }
}
