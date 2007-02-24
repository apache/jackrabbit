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
package org.apache.jackrabbit.test.api.query;

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.version.VersionException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.NotExecutableException;

/**
 * Tests the method {@link javax.jcr.query.Query#storeAsNode(String)}.
 *
 * @test
 * @sources SaveTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.SaveTest
 * @keywords level2
 */
public class SaveTest extends AbstractJCRTest {

    /** Simple XPath statement for test cases */
    private String statement;

    protected void setUp() throws Exception {
        super.setUp();
        statement = "//*[@jcr:primaryType='" + ntBase + "']";
    }

    /**
     * Stores a {@link javax.jcr.query.Query#XPATH} query at:
     * <code>testRoot + "/" + nodeName1</code>.
     * @throws NotExecutableException if nt:query is not supported.
     */
    public void testSave() throws RepositoryException, NotExecutableException {
        checkNtQuery();
        Query query = superuser.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        query.storeAsNode(testRoot + "/" + nodeName1);

        assertTrue("Node has not been stored", testRootNode.hasNode(nodeName1));

        Node queryNode = testRootNode.getNode(nodeName1);
        assertTrue("Query node is not of type nt:query", queryNode.isNodeType(ntQuery));

        Query query2 = superuser.getWorkspace().getQueryManager().getQuery(queryNode);
        assertEquals("Persisted query does not match initial query.", query.getStatement(), query2.getStatement());
    }

    /**
     * Tests if an {@link javax.jcr.ItemExistsException} is thrown when a query
     * is stored on an existing node and same name siblings are not allowed.
     * @throws NotExecutableException if nt:query is not supported.
     */
    public void testItemExistsException() throws RepositoryException, NotExecutableException {
        checkNtQuery();
        Query query = superuser.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        Node qNode = query.storeAsNode(testRoot + "/" + nodeName1);

        // create another one
        query = superuser.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        try {
            query.storeAsNode(testRoot + "/" + nodeName1);
            if (!qNode.getDefinition().allowsSameNameSiblings()) {
                // must throw if same name siblings are not allowed
                fail("Query.storeAsNode() did not throw ItemExistsException");
            }
        } catch (ItemExistsException e) {
            if (qNode.getDefinition().allowsSameNameSiblings()) {
                fail("Query.storeAsNode() must not throw ItemExistsException " +
                        "when same name siblings are allowed");
            } else {
                // expected behaviour
            }
        }
    }

    /**
     * Tests if a {@link javax.jcr.PathNotFoundException} is thrown when a query
     * is stored to a non existent path.
     * @throws NotExecutableException if nt:query is not supported.
     */
    public void testPathNotFoundException() throws RepositoryException, NotExecutableException {
        checkNtQuery();
        Query query = superuser.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        try {
            query.storeAsNode(testRoot + "/" + nodeName1 + "/" + nodeName1);
            fail("Query.storeAsNode() must throw PathNotFoundException on invalid path");
        } catch (PathNotFoundException e) {
            // expected behaviour
        }
    }

    /**
     * Tests if a {@link javax.jcr.version.VersionException} is thrown when a
     * query is stored under a checked in node.
     * <p/>
     * The tests creates a node under <code>testRoot</code> with name
     * <code>nodeName1</code> and adds a mix:versionable mixin if the node is
     * not already versionable.
     * Then the test tries to store a query as <code>nodeName2</code> under node
     * <code>nodeName1</code>.
     * @throws NotExecutableException if nt:query is not supported.
     */
    public void testVersionException() throws RepositoryException, NotExecutableException {
        checkNtQuery();
        // check if repository supports versioning
        if (!isSupported(Repository.OPTION_VERSIONING_SUPPORTED)) {
            throw new NotExecutableException();
        }

        Query query = superuser.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        // create a node that is versionable
        Node versionable = testRootNode.addNode(nodeName1, testNodeType);
        // or try to make it versionable if it is not
        if (!versionable.isNodeType(mixVersionable)) {
            if (versionable.canAddMixin(mixVersionable)) {
                versionable.addMixin(mixVersionable);
            } else {
                fail("Node " + nodeName1 + " is not versionable and does not allow to add mix:versionable");
            }
        }
        testRootNode.save();
        versionable.checkin();

        try {
            query.storeAsNode(testRoot + "/" + nodeName1 + "/" + nodeName2);
            fail("Query.storeAsNode() must throw VersionException, parent node is checked in.");
        } catch (VersionException e) {
            // expected behaviour
        }
    }

    /**
     * Tests if a {@link javax.jcr.nodetype.ConstraintViolationException} is
     * thrown if a query is stored under a node which does not allow child nodes.
     * <p/>
     * The test creates a node <code>nodeName1</code> of type <code>testNodeType</code>
     * under <code>testRoot</code>. Then the test tries to store a query as
     * <code>nodeName2</code> under <code>nodeName1</code>.
     * @throws NotExecutableException if nt:query is not supported.
     */
    public void testConstraintViolationException() throws RepositoryException, NotExecutableException {
        checkNtQuery();
        Query query = superuser.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        testRootNode.addNode(nodeName1, testNodeType);
        try {
            query.storeAsNode(testRoot + "/" + nodeName1 + "/" + nodeName2);
            fail("Query.storeAsNode() must throw ConstraintViolationException, parent node does not allow child nodes.");
        } catch (ConstraintViolationException e) {
            // expected behaviour
        }
    }

    /**
     * Tests if a {@link javax.jcr.lock.LockException} is thrown if a query is
     * stored under a node locked by another <code>Session</code>.
     * <p/>
     * The test creates a node <code>nodeName1</code> of type <code>testNodeType</code>
     * under <code>testRoot</code> and locks the node with the superuser session.
     * Then the test tries to store a query as <code>nodeName2</code> under
     * <code>nodeName1</code> with the readWrite <code>Session</code>.
     * @throws NotExecutableException if nt:query is not supported.
     */
    public void testLockException() throws RepositoryException, NotExecutableException {
        checkNtQuery();
        // check if repository supports locking
        if (!isSupported(Repository.OPTION_LOCKING_SUPPORTED)) {
            throw new NotExecutableException();
        }
        // create a node that is lockable
        Node lockable = testRootNode.addNode(nodeName1, testNodeType);
        // or try to make it lockable if it is not
        if (!lockable.isNodeType(mixLockable)) {
            if (lockable.canAddMixin(mixLockable)) {
                lockable.addMixin(mixLockable);
            } else {
                fail("Node " + nodeName1 + " is not lockable and does not allow to add mix:lockable");
            }
        }
        testRootNode.save();
        lockable.lock(false, true);

        Session readWrite = helper.getReadWriteSession();
        try {
            Query query = readWrite.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
            query.storeAsNode(testRoot + "/" + nodeName1 + "/" + nodeName2);
            fail("Query.storeAsNode() must throw LockException, parent node is locked.");
        } catch (LockException e) {
            // expected behaviour
        } finally {
            readWrite.logout();
            lockable.unlock();
        }
    }

    /**
     * Tests if the a {@link javax.jcr.RepositoryException} is thrown when
     * an malformed path is passed in {@link javax.jcr.query.Query#storeAsNode(String)}.
     * @throws NotExecutableException if nt:query is not supported.
     */
    public void testRepositoryException() throws RepositoryException, NotExecutableException {
        checkNtQuery();
        Query query = superuser.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        try {
            query.storeAsNode(testRoot + "/invalid[path");
            fail("Query.storeAsNode() must throw RepositoryException on malformed path.");
        } catch (RepositoryException e) {
            // expected behaviour
        }
    }

    //-------------------------------< internal >-------------------------------

    /**
     * Checks if the repository supports the nt:query node type otherwise throws
     * a <code>NotExecutableException</code>.
     *
     * @throws NotExecutableException if nt:query is not supported.
     */
    private void checkNtQuery() throws RepositoryException, NotExecutableException {
        try {
            superuser.getWorkspace().getNodeTypeManager().getNodeType(ntQuery);
        } catch (NoSuchNodeTypeException e) {
            // not supported
            throw new NotExecutableException("repository does not support nt:query");
        }
    }
}
