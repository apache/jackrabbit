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

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** <code>MoveCombinedTest</code>... */
public class MoveCombinedTest extends AbstractMoveTest {

    private static Logger log = LoggerFactory.getLogger(MoveCombinedTest.class);

    private Session testSession;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testSession = getHelper().getReadOnlySession();
    }

    @Override
    protected void tearDown() throws Exception {
        if (testSession != null) {
            testSession.logout();
            testSession = null;
        }
        super.tearDown();
    }

    @Override
    protected boolean isSessionMove() {
        return true;
    }

    public void testMoveAndAddNode() throws RepositoryException {
        doMove(moveNode.getPath(), destinationPath);
        Node n = moveNode.addNode(nodeName3);
        superuser.save();

        assertTrue(testSession.itemExists(n.getPath()));
    }

    public void testMoveAndAddProperty() throws RepositoryException {
        doMove(moveNode.getPath(), destinationPath);
        Property p = moveNode.setProperty(propertyName1, "someValue");
        superuser.save();

        assertTrue(testSession.itemExists(p.getPath()));
    }

    public void testMoveAndSetPropertyValue() throws RepositoryException {
        Property p = moveNode.setProperty(propertyName1, "someValue");
        moveNode.save();

        doMove(moveNode.getPath(), destinationPath);
        p = moveNode.setProperty(propertyName1, "changedValue");
        superuser.save();

        assertTrue(testSession.itemExists(p.getPath()));
    }

    public void testMoveAndRemove() throws RepositoryException {
        Node n = moveNode.addNode(nodeName3);
        String nPath = n.getPath();
        superuser.save();

        doMove(moveNode.getPath(), destinationPath);
        n.remove();
        superuser.save();

        assertFalse(testSession.itemExists(nPath));
        assertFalse(testSession.itemExists(destinationPath + "/" + nodeName3));
    }

    public void testMoveAndSetMixin() throws RepositoryException {

        doMove(moveNode.getPath(), destinationPath);
        moveNode.addMixin(mixVersionable);
        superuser.save();

        Node n = (Node) testSession.getItem(destinationPath);
        assertTrue(n.isNodeType(mixVersionable));
    }
}