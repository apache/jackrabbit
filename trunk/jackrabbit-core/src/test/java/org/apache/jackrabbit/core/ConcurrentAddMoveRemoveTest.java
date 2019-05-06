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

import java.util.Random;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.integration.random.operation.Operation;
import org.apache.jackrabbit.core.persistence.check.ConsistencyReport;

/**
 * <code>ConcurrentAddMoveRemoveTest</code> performs a test with 5 threads which
 * concurrently add, (workspace) move and remove nodes. This test is intended to 
 * make sure these concurrent actions don't lead to inconsistencies in the database.
 * See also: JCR-3292.
 */
public class ConcurrentAddMoveRemoveTest extends AbstractConcurrencyTest {

    private static final int RUN_NUM_SECONDS = 20;

    private long end;

    private String folder1Path;
    private String folder2Path;

    protected void setUp() throws Exception {
        super.setUp();
        end = System.currentTimeMillis() + RUN_NUM_SECONDS * 1000;
        folder1Path = testRootNode.addNode("folder1").getPath();
        folder2Path = testRootNode.addNode("folder2").getPath();
        testRootNode.getSession().save();
    }

    @Override
    public void tearDown() throws Exception {
        ConsistencyReport consistencyReport = TestHelper.checkConsistency(testRootNode.getSession(), false, null);
        //for (ReportItem item : consistencyReport.getItems()) {
        //    System.out.println(item.getMessage());
        //}
        assertTrue(consistencyReport.getItems().size() == 0);
        super.tearDown();
    }

    public void testRandomOperations() throws RepositoryException {
        runTask(new AddMoveRemoveTask(end, folder1Path, folder2Path), 5, testRootNode.getPath());
    }

    public static class AddMoveRemoveTask implements Task {

        private final long end;
        private final String folder1Path;
        private final String folder2Path;

        private final Random random = new Random();

        public AddMoveRemoveTask(long end, String folder1Path, String folder2Path) {
            this.end = end;
            this.folder1Path = folder1Path;
            this.folder2Path = folder2Path;
        }

        public void execute(final Session session, final Node test) throws RepositoryException {
            while (end > System.currentTimeMillis()) {
                try {
                    getRandomOperation(session).execute();
                } catch (RepositoryException e) {
                    // RepositoryExceptions are expected during concurrent actions.
                    session.refresh(false);
                } catch (Exception e) {
                    // only a RepositoryException is allowed from a Task
                    throw new RepositoryException("Failure during concurrent execution", e);
                }
            }
        }

        private Operation getRandomOperation(Session session) throws RepositoryException {
            switch (random.nextInt(3)) {
            case 0:
                return new Add(session, getRandomParent());
            case 1: {
                String folderPath = getRandomParent();
                return new WorkspaceMove(session, getRandomChild(session, folderPath),
                        folderPath.equals(folder1Path) ? folder2Path : folder1Path);
            }
            default: {
                String folderPath = getRandomParent();
                return new Remove(session, getRandomChild(session, folderPath));
            }
            }
        }

        private String getRandomChild(Session session, String parentPath) throws RepositoryException {
            Node parent = session.getNode(parentPath);
            NodeIterator nodes = parent.getNodes();
            int size = (int) nodes.getSize();
            if (size > 0) {
                int i = 1;
                int offset = random.nextInt(size);
                while (nodes.hasNext()) {
                    if (i == offset) {
                        return nodes.nextNode().getPath();
                    }
                    nodes.nextNode();
                    i++;
                }
            }
            return null;
        }

        private String getRandomParent() {
            switch (random.nextInt(2)) {
            case 0:
                return folder1Path;
            default:
                return folder2Path;
            }
        }

        private class Add extends Operation {

            private final String name;

            public Add(Session s, String folderPath) {
                super(s, folderPath);
                this.name = getRandomText(4);
            }

            @Override
            public NodeIterator execute() throws Exception {
                getNode().addNode(name);
                getSession().save();
                return null;
            }
        }

        private class Remove extends Operation {

            public Remove(Session s, String path) {
                super(s, path);
            }

            @Override
            public NodeIterator execute() throws Exception {
                if (getPath() != null) {
                    getNode().remove();
                    getSession().save();
                }
                return null;
            }

        }

        private class WorkspaceMove extends Operation {

            private final String folderPath;

            public WorkspaceMove(Session s, String path, String folderPath) {
                super(s, path);
                this.folderPath = folderPath;
            }

            @Override
            public NodeIterator execute() throws Exception {
                if (getPath() != null) {
                    String destination = folderPath + "/" + getRandomText(4);
                    getSession().getWorkspace().move(getPath(), destination);
                }
                return null;
            }

        }
    }

}
