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
package org.apache.jackrabbit.jcr2spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Node;

/**
 * <code>RevertMove</code>...
 */
public class RevertMoveTest extends AbstractMoveTest {

    private static Logger log = LoggerFactory.getLogger(RevertMoveTest.class);

    protected boolean isSessionMove() {
        return true;
    }

    /**
     * Implementation specific test, that expects that the scope of a refresh(false)
     * must include all nodes affected by the operations that affected the
     * subtree to be refreshed.
     * 
     * @throws RepositoryException
     */
    public void testRevertMovedNode() throws RepositoryException {
        String srcPath = moveNode.getPath();
        doMove(srcPath, destinationPath);
        Node afterMoveNode = (Node) testRootNode.getSession().getItem(destinationPath);

        try {
            afterMoveNode.refresh(false);
            fail("Node.refresh() on a transiently moved node should fail such as a 'save' would fail.");
        } catch (RepositoryException e) {
            // ok: works as expected. scope of 'refresh' is not complete
        }
    }
}