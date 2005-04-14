/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
     * Saves a {@link javax.jcr.query.Query.XPATH} query at:
     * <code>testRoot + "/" + nodeName1</code>.
     */
    public void testSave() throws RepositoryException {
        Query query = superuser.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        query.storeAsNode(testRoot + "/" + nodeName1);

        assertTrue("Node has not been saved", testRootNode.hasNode(nodeName1));

        Node queryNode = testRootNode.getNode(nodeName1);
        assertEquals("Query node is not of type nt:query", ntQuery, queryNode.getPrimaryNodeType().getName());

        Query query2 = superuser.getWorkspace().getQueryManager().getQuery(queryNode);
        assertEquals("Persisted query does not match initial query.", query.getStatement(), query2.getStatement());
    }

    /**
     * Tests if an {@link javax.jcr.ItemExistsException} is thrown when a query
     * is saved on an existing node.
     */
    public void testItemExistsException() throws RepositoryException {
        Query query = superuser.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        query.storeAsNode(testRoot + "/" + nodeName1);

        // create another one
        query = superuser.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        try {
            query.storeAsNode(testRoot + "/" + nodeName1);
            fail("Query.save() did not throw ItemExistsException");
        } catch (ItemExistsException e) {
            // expected behaviour
        }
    }

    /**
     * Tests if a {@link javax.jcr.PathNotFoundException} is thrown when a query
     * is saved to a non existent path.
     */
    public void testPathNotFoundException() throws RepositoryException {
        Query query = superuser.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        try {
            query.storeAsNode(testRoot + "/" + nodeName1 + "/" + nodeName1);
            fail("Query.save() must throw PathNotFoundException on invalid path");
        } catch (PathNotFoundException e) {
            // expected behaviour
        }
    }

    /**
     * Tests if a {@link javax.jcr.version.VersionException} is thrown when a
     * query is saved under a checked in node.
     * <p/>
     * The tests creates a node under <code>testRoot</code> with name
     * <code>nodeName1</code> and adds a mix:versionable mixin if the node is
     * not already versionable.
     * Then the test tries to save a query as <code>nodeName2</code> under node
     * <code>nodeName1</code>.
     */
    public void testVersionException() throws RepositoryException, NotExecutableException {
        // check if repository supports versioning
        if (superuser.getRepository().getDescriptor(Repository.OPTION_VERSIONING_SUPPORTED) == null) {
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
            fail("Query.save() must throw VersionException, parent node is checked in.");
        } catch (VersionException e) {
            // expected behaviour
        }
    }

    /**
     * Tests if a {@link javax.jcr.nodetype.ConstraintViolationException} is
     * thrown if a query is saved under a node which does not allow child nodes.
     * <p/>
     * The test creates a node <code>nodeName1</code> of type <code>testNodeType</code>
     * under <code>testRoot</code>. Then the test tries to save a query as
     * <code>nodeName2</code> under <code>nodeName1</code>.
     *
     */
    public void testConstraintViolationException() throws RepositoryException {
        Query query = superuser.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        testRootNode.addNode(nodeName1, testNodeType);
        try {
            query.storeAsNode(testRoot + "/" + nodeName1 + "/" + nodeName2);
            fail("Query.save() must throw ConstraintViolationException, parent node does not allow child nodes.");
        } catch (ConstraintViolationException e) {
            // expected behaviour
        }
    }

    /**
     * Tests if a {@link javax.jcr.lock.LockException} is thrown if a query is
     * saved under a node locked by another <code>Session</code>.
     * <p/>
     * The test creates a node <code>nodeName1</code> of type <code>testNodeType</code>
     * under <code>testRoot</code> and locks the node with the superuser session.
     * Then the test tries to save a query as <code>nodeName2</code> under
     * <code>nodeName1</code> with the readWrite <code>Session</code>.
     */
    public void testLockException() throws RepositoryException, NotExecutableException {
        // check if repository supports locking
        if (superuser.getRepository().getDescriptor(Repository.OPTION_LOCKING_SUPPORTED) == null) {
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
        Query query = readWrite.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        try {
            query.storeAsNode(testRoot + "/" + nodeName1 + "/" + nodeName2);
            fail("Query.save() must throw LockException, parent node is locked.");
        } catch (LockException e) {
            // expected behaviour
        } finally {
            lockable.unlock();
        }
    }

    /**
     * Tests if the a {@link javax.jcr.RepositoryException} is thrown when
     * an malformed path is passed in {@link javax.jcr.query.Query#storeAsNode(String)}.
     */
    public void testRepositoryException() throws RepositoryException {
        Query query = superuser.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        try {
            query.storeAsNode(testRoot + "/invalid[path");
            fail("Query.save() must throw RepositoryException on malformed path.");
        } catch (RepositoryException e) {
            // expected behaviour
        }
    }
}
