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
package org.apache.jackrabbit.core;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.persistence.check.ConsistencyReport;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

public class AddMoveTest extends AbstractJCRTest {

    private String folder1Path;
    private String folder2Path;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Node folder1 = testRootNode.addNode("folder1");
        folder1Path = folder1.getPath();
        Node folder2 = testRootNode.addNode("folder2");
        folder2Path = folder2.getPath();
        folder1.addNode("node1");
        testRootNode.getSession().save();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testAddMove() throws RepositoryException, NotExecutableException {
        Session session1 = getHelper().getReadWriteSession();
        Session session2 = getHelper().getReadWriteSession();

        session1.getNode(folder1Path).addNode("node2");
        session2.move(folder1Path + "/node1", folder2Path + "/node1");
        session2.save();
        Node node = session1.getNode(folder2Path + "/node1");
        node.setProperty("foo", "bar");
        session1.save();

        ConsistencyReport consistencyReport = TestHelper.checkConsistency(testRootNode.getSession(), false, null);
        //for (ReportItem item : consistencyReport.getItems()) {
        //    System.out.println(item.getMessage());
        //}
        assertTrue(consistencyReport.getItems().size() == 0);
    }

    /**
     * Add a top level node and rename it. Exposes a bug in the {@code CachingHierarchyManager},
     * reported in JCR-3379.
     */
    public void testTopLevelAddMove() throws Exception {
        Session session = getHelper().getReadWriteSession();
        session.getRootNode().addNode("foo");
        session.save();
        Node fooNode = session.getNode("/foo");
        assertEquals("/foo", fooNode.getPath());
        session.move("/foo", "/bar");
        Node barNode = session.getNode("/bar");
        assertEquals("/bar", barNode.getPath());
    }

    /**
     * Add a top level node and remove it. Exposes a bug in the {@code CachingHierarchyManager},
     * reported in JCR-3368.
     */
    public void testTopLevelAddRemove() throws Exception {
        Session session = getHelper().getReadWriteSession();
        session.getRootNode().addNode("foo").addNode("bar");
        session.save();
        session.getNode("/foo").remove();
        assertFalse(session.getRootNode().hasNode("foo/bar"));
    }
}
