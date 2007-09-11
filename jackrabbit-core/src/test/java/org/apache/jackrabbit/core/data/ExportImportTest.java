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

public class ExportImportTest extends AbstractJCRTest {

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
        try {
            Session session = helper.getReadWriteSession();
            Node root = session.getRootNode();
            clean(root);
            Node test = root.addNode("testBinary");
            session.save();
            byte[] data = new byte[len];
            Random random = new Random(1);
            random.nextBytes(data);
            test.setProperty("data", new ByteArrayInputStream(data));
            test.save();
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
            byte[] data2 = readFromStream(test2.getProperty("data").getStream());
            byte[] data3 = readFromStream(test3.getProperty("data").getStream());
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
        root.getSession().save();
    }
}
