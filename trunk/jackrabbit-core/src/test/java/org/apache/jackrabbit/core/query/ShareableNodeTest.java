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
package org.apache.jackrabbit.core.query;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

/**
 * <code>ShareableNodeTest</code> performs query tests with shareable nodes.
 */
public class ShareableNodeTest extends AbstractQueryTest {

    public void testPathConstraint() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1);
        Node n2 = testRootNode.addNode(nodeName2);
        Node s = n1.addNode(nodeName3);
        s.setProperty(propertyName1, "value");
        s.addMixin(mixShareable);
        Node n4 = s.addNode(nodeName4);
        n4.setProperty(propertyName2, "value");
        testRootNode.save();

        Workspace wsp = superuser.getWorkspace();
        wsp.clone(wsp.getName(), s.getPath(), n2.getPath() + "/" + s.getName(), false);

        String stmt = testPath + "/" + nodeName1 + "/*[@" + propertyName1 + "='value']";
        NodeIterator nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 1, nodes.getSize());
        // spec does not say which path must be returned -> use isSame()
        assertTrue("wrong node", s.isSame(nodes.nextNode()));

        stmt = testPath + "/" + nodeName2 + "/*[@" + propertyName1 + "='value']";
        nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 1, nodes.getSize());
        // spec does not say which path must be returned -> use isSame()
        assertTrue("wrong node", s.isSame(nodes.nextNode()));

        stmt = testPath + "//*[@" + propertyName1 + "='value']";
        nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 1, nodes.getSize());
        // spec does not say which path must be returned -> use isSame()
        assertTrue("wrong node", s.isSame(nodes.nextNode()));

        stmt = testPath + "//*[@" + propertyName2 + "='value']";
        nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 1, nodes.getSize());
        // spec does not say which path must be returned -> use isSame()
        assertTrue("wrong node", n4.isSame(nodes.nextNode()));

        // remove a node from the shared set
        s.removeShare();
        testRootNode.save();

        s = n2.getNode(nodeName3);

        stmt = testPath + "/" + nodeName1 + "/*[@" + propertyName1 + "='value']";
        nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 0, nodes.getSize());

        stmt = testPath + "/" + nodeName2 + "/*[@" + propertyName1 + "='value']";
        nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 1, nodes.getSize());
        // spec does not say which path must be returned -> use isSame()
        assertTrue("wrong node", s.isSame(nodes.nextNode()));

        stmt = testPath + "//*[@" + propertyName1 + "='value']";
        nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 1, nodes.getSize());
        // spec does not say which path must be returned -> use isSame()
        assertTrue("wrong node", s.isSame(nodes.nextNode()));

        // remove remaining node from the shared set
        s.removeShare();
        testRootNode.save();

        stmt = testPath + "/" + nodeName1 + "/*[@" + propertyName1 + "='value']";
        nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 0, nodes.getSize());

        stmt = testPath + "/" + nodeName2 + "/*[@" + propertyName1 + "='value']";
        nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 0, nodes.getSize());

        stmt = testPath + "//*[@" + propertyName1 + "='value']";
        nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 0, nodes.getSize());
    }

    public void testName() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1);
        Node n2 = testRootNode.addNode(nodeName2);
        Node s = n1.addNode(nodeName3);
        s.addMixin(mixShareable);
        testRootNode.save();

        Workspace wsp = superuser.getWorkspace();
        wsp.clone(wsp.getName(), s.getPath(), n2.getPath() + "/" + nodeName4, false);

        String stmt = testPath + "//" + nodeName3;
        NodeIterator nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 1, nodes.getSize());
        // spec does not say which path must be returned -> use isSame()
        assertTrue("wrong node", s.isSame(nodes.nextNode()));

        stmt = testPath + "//" + nodeName4;
        nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 1, nodes.getSize());
        // spec does not say which path must be returned -> use isSame()
        assertTrue("wrong node", s.isSame(nodes.nextNode()));

        // remove a node from the shared set
        s.removeShare();
        testRootNode.save();

        s = n2.getNode(nodeName4);

        stmt = testPath + "//" + nodeName3;
        nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 0, nodes.getSize());

        stmt = testPath + "//" + nodeName4;
        nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 1, nodes.getSize());
        // spec does not say which path must be returned -> use isSame()
        assertTrue("wrong node", s.isSame(nodes.nextNode()));

        // remove remaining node from the shared set
        s.removeShare();
        testRootNode.save();

        stmt = testPath + "//" + nodeName3;
        nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 0, nodes.getSize());

        stmt = testPath + "//" + nodeName4;
        nodes = executeQuery(stmt).getNodes();
        assertEquals("wrong result size", 0, nodes.getSize());
    }
}
