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
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.ValueFactory;

/**
 * Tests copying binary values from one node to another.
 * See also <a href="https://issues.apache.org/jira/browse/JCR-1351">JCR-1351</a>
 * and <a href="https://issues.apache.org/jira/browse/JCR-1346">JCR-1346</a>
 */
public class CopyValueTest extends AbstractJCRTest {

    static {
        // to test without data store, enable the following line:
        // System.setProperty("org.jackrabbit.useDataStore", "false");
    }

    /**
     * Tests the Workspace.copy() operation with various lengths.
     */
    public void testCopyStream() throws Exception {
        doTestCopy(100000);
        doTestCopy(0);
        doTestCopy(1);
        doTestCopy(10);
        doTestCopy(100);
        doTestCopy(1000);
    }

    private void doTestCopy(int length) throws Exception {
        Node root = superuser.getRootNode();
        if (root.hasNode("testCopy")) {
            root.getNode("testCopy").remove();
            superuser.save();
        }
        Node testRoot = root.addNode("testCopy");
        Node n = testRoot.addNode("a");
        superuser.save();
        byte[] data = new byte[length + 1];
        ValueFactory vf = superuser.getValueFactory();
        n.setProperty("data", vf.createBinary(new ByteArrayInputStream(data)));
        superuser.save();
        data = new byte[length];
        n.setProperty("data", vf.createBinary(new ByteArrayInputStream(data)));
        Property p = testRoot.getNode("a").getProperty("data");
        assertEquals(length, p.getLength());
        superuser.getWorkspace().copy("/testCopy/a", "/testCopy/b");
        assertEquals(length, p.getLength());
        p = testRoot.getNode("b").getProperty("data");
        assertEquals(length + 1, p.getLength());
        testRoot.remove();
        superuser.save();
    }

    /**
     * Test random operations:
     * create, delete, move, and verify node; save and refresh a session.
     * The test always runs the same sequence of operations.
     */
    public void testRandomOperations() throws Exception {
        Random random = new Random(1);
        Node root = superuser.getRootNode();
        if (root.hasNode("testRandom")) {
            root.getNode("testRandom").remove();
            superuser.save();
        }
        Node testRoot = root.addNode("testRandom");
        int len = 1000;
        int[] opCounts = new int[6];
        for (int i = 0; i < len; i++) {
            String node1 = "node" + random.nextInt(len / 20);
            boolean hasNode1 = testRoot.hasNode(node1);
            String node2 = "node" + random.nextInt(len / 20);
            boolean hasNode2 = testRoot.hasNode(node2);
            int op = random.nextInt(6);
            switch (op) {
            case 0: {
                if (hasNode1) {
                    log(node1 + " remove");
                    testRoot.getNode(node1).remove();
                }
                opCounts[op]++;
                Node n = testRoot.addNode(node1);
                int dataLength = Math.abs(Math.min(10000, (int) (10000 * random
                        .nextGaussian())));
                byte[] data = new byte[dataLength];
                log(node1 + " add len:" + dataLength);
                ValueFactory vf = superuser.getValueFactory();
                n.setProperty("data", vf.createBinary(new ByteArrayInputStream(data)));
                n.setProperty("len", dataLength);
                break;
            }
            case 1:
                opCounts[op]++;
                log("save");
                superuser.save();
                break;
            case 2:
                opCounts[op]++;
                log("refresh");
                superuser.refresh(false);
                break;
            case 3:
                if (hasNode1) {
                    opCounts[op]++;
                    log(node1 + " remove");
                    testRoot.getNode(node1).remove();
                }
                break;
            case 4: {
                if (hasNode1) {
                    opCounts[op]++;
                    Node n = testRoot.getNode(node1);
                    long dataLength = n.getProperty("len").getLong();
                    long l = n.getProperty("data").getLength();
                    log(node1 + " verify len: " + dataLength);
                    assertEquals(dataLength, l);
                }
                break;
            }
            case 5:
                if (hasNode1 && !hasNode2) {
                    opCounts[op]++;
                    log(node1 + " copy " + node2);
                    // todo: why is save required?
                    superuser.save();
                    superuser.getWorkspace().copy("/testRandom/" + node1,
                            "/testRandom/" + node2);
                }
                break;
            }
        }
        superuser.save();
        for (int i = 0; i < opCounts.length; i++) {
            log(i + ": " + opCounts[i]);
        }
        testRoot.remove();
        superuser.save();
    }

    private void log(String s) {
        // to log operations, enable the following line:
        // System.out.println(s);
    }

}
