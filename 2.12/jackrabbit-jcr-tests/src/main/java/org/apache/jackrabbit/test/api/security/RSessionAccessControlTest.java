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
package org.apache.jackrabbit.test.api.security;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.test.RepositoryStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** <code>RSessionAccessControlTest</code>... */
public class RSessionAccessControlTest extends AbstractAccessControlTest {

    private static Logger log = LoggerFactory.getLogger(RSessionAccessControlTest.class);

    private Session readOnlySession;
    private String testNodePath;
    private String testPropertyPath;

    protected void setUp() throws Exception {
        super.setUp();
        Node n = testRootNode.addNode(nodeName1, testNodeType);
        testNodePath = n.getPath();
        Value v = getJcrValue(superuser, RepositoryStub.PROP_PROP_VALUE1, RepositoryStub.PROP_PROP_TYPE1, "test");
        Property p = n.setProperty(propertyName1, v);
        testPropertyPath = p.getPath();
        testRootNode.getSession().save();

        readOnlySession = getHelper().getReadOnlySession();
    }

    protected void tearDown() throws Exception {
        if (readOnlySession != null) {
            readOnlySession.logout();
        }
        super.tearDown();
    }

    public void testSetProperty() throws RepositoryException {
        Node n = (Node) readOnlySession.getItem(testNodePath);
        try {
            n.setProperty(propertyName1, "otherValue");
            n.save();
            fail("A read only session must not be allowed to modify a property value");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testSetValue() throws RepositoryException {
        Property p = (Property) readOnlySession.getItem(testPropertyPath);
        try {
            p.setValue("otherValue");
            p.save();
            fail("A read only session must not be allowed to modify a property value");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testDeleteNode() throws Exception {
        Node n = (Node) readOnlySession.getItem(testNodePath);
        try {
            n.remove();
            readOnlySession.save();
            fail("A read only session must not be allowed to remove a node");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testDeleteProperty() throws Exception {
        Property p = (Property) readOnlySession.getItem(testPropertyPath);
        try {
            p.remove();
            readOnlySession.save();
            fail("A read only session must not be allowed to remove a property.");
        } catch (AccessDeniedException e) {
            // success
        }
    }

    public void testMoveNode() throws Exception {
        Node n = (Node) readOnlySession.getItem(testNodePath);
        String destPath = testRootNode.getPath() + "/" + nodeName2;

        try {
            readOnlySession.move(n.getPath(), destPath);
            readOnlySession.save();
            fail("A read only session must not be allowed to move a node");
        } catch (AccessDeniedException e) {
            // expected
            log.debug(e.getMessage());
        }
    }

    public void testWorkspaceMoveNode() throws Exception {
        Node n = (Node) readOnlySession.getItem(testNodePath);
        String destPath = testRootNode.getPath() + "/" + nodeName2;
        try {
            readOnlySession.getWorkspace().move(n.getPath(), destPath);
            fail("A read only session must not be allowed to move a node");
        } catch (AccessDeniedException e) {
            // expected
            log.debug(e.getMessage());
        }
    }

    public void testCopyNode() throws Exception {
        Node n = (Node) readOnlySession.getItem(testNodePath);
        String destPath = testRootNode.getPath() + "/" + nodeName2;
        try {
            readOnlySession.getWorkspace().copy(n.getPath(), destPath);
            fail("A read only session must not be allowed to copy a node");
        } catch (AccessDeniedException e) {
            // expected
            log.debug(e.getMessage());
        }
    }
}