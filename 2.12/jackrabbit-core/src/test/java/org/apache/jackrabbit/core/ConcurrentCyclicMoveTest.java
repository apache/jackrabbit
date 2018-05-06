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

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.test.AbstractJCRTest;

public class ConcurrentCyclicMoveTest extends AbstractJCRTest {

    private String testRootPath;

    public void testConcurrentSessionMove() throws RepositoryException {

        testRootPath = testRootNode.getPath();
        Node aa = testRootNode.addNode("a").addNode("aa");
        Node b = testRootNode.addNode("b");
        testRootNode.getSession().save();

        String aaId = aa.getIdentifier();
        String bId= b.getIdentifier();
        
        Session session1 = getHelper().getReadWriteSession();
        Session session2 = getHelper().getReadWriteSession();

        // results in /b/a/aa
        session1.move(testRootPath + "/a", testRootPath + "/b/a");
        assertEquals(testRootPath + "/b/a/aa", session1.getNodeByIdentifier(aaId).getPath());

        // results in a/aa/b
        session2.move(testRootPath + "/b", testRootPath + "/a/aa/b");
        assertEquals(testRootPath + "/a/aa/b", session2.getNodeByIdentifier(bId).getPath());

        session1.save();

        try {
            session2.getNodeByIdentifier(bId).getPath();
            fail("It should not be possible to access a cyclic path");
        } catch (InvalidItemStateException expected) {
        }

        try {
            session2.save();
            fail("Save should have failed. Possible cyclic persistent path created.");
        } catch (InvalidItemStateException expected) {
        }
    }


    public void testConcurrentWorkspaceMove() throws RepositoryException {

        testRootPath = testRootNode.getPath();
        testRootNode.addNode("b");
        Node aa = testRootNode.addNode("a").addNode("aa");
        testRootNode.getSession().save();

        String aaId = aa.getIdentifier();
        
        Session session1 = getHelper().getReadWriteSession();
        Session session2 = getHelper().getReadWriteSession();

        // results in /b/a/aa
        session1.getWorkspace().move(testRootPath + "/a", testRootPath + "/b/a");
        assertEquals(testRootPath + "/b/a/aa", session1.getNodeByIdentifier(aaId).getPath());
        
        // try to move b into a/aa (should fail as the above move is persisted
        try {
            session2.getWorkspace().move(testRootPath + "/b", testRootPath + "/a/aa/b");
            fail("Workspace.move() should not have succeeded. Possible cyclic path created.");
        } catch (PathNotFoundException e) {
            // expected.
        }
    }
}
