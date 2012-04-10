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
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.persistence.check.ConsistencyReport;
import org.apache.jackrabbit.core.state.StaleItemStateException;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

public class MoveRemoveTest extends AbstractJCRTest {

    private String folder1Path;
    private String folder2Path;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Node folder1 = testRootNode.addNode("folder1");
        folder1Path = folder1.getPath();
        Node folder2 = testRootNode.addNode("folder2");
        folder2Path = folder2.getPath();
        folder1.addNode("node");
        testRootNode.getSession().save();
    }

    public void testMoveRemove() throws RepositoryException, NotExecutableException {
        Session session1 = getHelper().getSuperuserSession();
        Session session2 = getHelper().getSuperuserSession();
        session1.move(folder1Path + "/node", folder2Path + "/node");
        session2.getNode(folder1Path + "/node").remove();
        session1.save();
        try {
            session2.save();
        } catch (InvalidItemStateException e) {
            if (e.getCause() == null || e.getCause().getClass() != StaleItemStateException.class) {
                throw e;
            }
        }
        ConsistencyReport consistencyReport = TestHelper.checkConsistency(testRootNode.getSession(), false, null);
        //for (ReportItem item : consistencyReport.getItems()) {
        //    System.out.println(item.getMessage());
        //}
        assertTrue(consistencyReport.getItems().size() == 0);
    }
}
