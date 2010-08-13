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
package org.apache.jackrabbit.api;

import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

/**
 * <code>JackrabbitNodeTest</code>...
 */
public class JackrabbitNodeTest  extends AbstractJCRTest {

    static final String SEQ_BEFORE = "jackrabbit";
    static final String SEQ_AFTER =  "jackraBbit";
    static final int RELPOS = 6;

    protected void setUp() throws Exception {
        super.setUp();
        assertTrue(testRootNode.getPrimaryNodeType().hasOrderableChildNodes());
        for (char c : SEQ_BEFORE.toCharArray()) {
            testRootNode.addNode(new String(new char[]{c}));
        }
    }

    public void testRename() throws RepositoryException {
        Node renamedNode = null;
        NodeIterator it = testRootNode.getNodes();
        int pos = 0;
        while (it.hasNext()) {
            Node n = it.nextNode();
            String name = n.getName();
            assertEquals(name, new String(new char[]{SEQ_BEFORE.charAt(pos)}));
            if (pos == RELPOS) {
                JackrabbitNode node = (JackrabbitNode) n;
                node.rename(name.toUpperCase());
                renamedNode = n;
            }
            pos++;
        }

        it = testRootNode.getNodes();
        pos = 0;
        while (it.hasNext()) {
            Node n = it.nextNode();
            String name = n.getName();
            assertEquals(name, new String(new char[]{SEQ_AFTER.charAt(pos)}));
            if (pos == RELPOS) {
                assertTrue(n.isSame(renamedNode));
            }
            pos++;
        }
    }
}
