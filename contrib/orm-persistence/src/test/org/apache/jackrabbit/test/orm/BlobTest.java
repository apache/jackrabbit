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
package org.apache.jackrabbit.test.orm;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.ValueFactory;

import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * BLOB insertion/retrieval test case.
 */
public class BlobTest extends AbstractJCRTest {

    private final static int BLOB_SIZE = 1*1024*1024;
    byte[] blobContent = new byte[BLOB_SIZE];

    public BlobTest() {
    }

    public void testBlob() throws Exception {
            Node rn = superuser.getRootNode();

            NodeIterator nodeIter = rn.getNodes();
            while (nodeIter.hasNext()) {
                Node curNode = nodeIter.nextNode();
            }

            log.println("Creating BLOB data of " + BLOB_SIZE + " bytes...");
            for (int i=0; i < BLOB_SIZE; i++) {
                blobContent[i] = (byte) (i % 256);
            }
            log.println("Adding BLOB node...");
            if (!rn.hasNode("blobnode")) {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(blobContent);
                Node n = rn.addNode("blobnode", "nt:unstructured");
                ValueFactory valueFactory = superuser.getValueFactory();
                n.setProperty("testprop", valueFactory.createValue("Hello, World."));
                n.setProperty("blobTest", inputStream);
                superuser.save();
            }
            log.println("Verifying BLOB node...");
            InputStream readInputStream = rn.getProperty("blobnode/blobTest").
                getStream();
            int ch = -1;
            int i=0;
            BufferedInputStream buf = new BufferedInputStream(readInputStream);
            while ((ch = buf.read()) != -1) {
                assertEquals((byte) ch, blobContent[i]);
                i++;
            }
            log.println("Removing BLOB node...");
            rn.getNode("blobnode").remove();
            superuser.save();
    }

}
