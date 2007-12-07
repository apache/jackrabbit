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

import javax.jcr.query.Query;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryManager;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Tests if queries with predicates are accepted. Test cases in this class only
 * perform tests that check if the QueryManager accepts the query, but the tests
 * will not execute the query and check its results.
 *
 * @test
 * @sources PredicatesTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.PredicatesTest
 * @keywords level1
 */
public class PredicatesTest extends AbstractQueryTest {

    /**
     * the node type of the root node
     */
    private String nodeTypeName;

    /**
     * A read-only session
     */
    private Session session;

    /**
     * the query manager of the session
     */
    private QueryManager qm;

    /**
     * Sets up the test cases
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
        session = helper.getReadOnlySession();
        testRootNode = session.getRootNode().getNode(testPath);

        nodeTypeName = session.getRootNode().getPrimaryNodeType().getName();
        qm = session.getWorkspace().getQueryManager();
    }

    /**
     * Releases the session acquired in setUp().
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        qm = null;
        super.tearDown();
    }

    /**
     * Verifies that the value of a property can be searched
     *
     * @throws RepositoryException
     */
    public void testEquality() throws RepositoryException {
        String stmt = "/" + jcrRoot + "/" + testPath+ "/*[@" + jcrPrimaryType + "='" + nodeTypeName + "']";

        try {
            qm.createQuery(stmt, Query.XPATH);
        } catch (InvalidQueryException e) {
            fail("invalid statement syntax for '" + stmt + "'");
        }
    }

    /**
     * Verifies that the or operator is accepted for properties's values
     *
     * @throws RepositoryException
     */
    public void testCombinedOr() throws RepositoryException {
        String stmt = "/" + jcrRoot + "/" + testPath+ "/*[@" + jcrPrimaryType + "='" + nodeTypeName + "' or @" + jcrPrimaryType + "='" + ntBase + "']";

        try {
            qm.createQuery(stmt, Query.XPATH);
        } catch (InvalidQueryException e) {
            fail("invalid statement syntax for '" + stmt + "'");
        }
    }

    /**
     * Verifies that the or operator is accepted for a property name
     *
     * @throws RepositoryException
     */
    public void testOr() throws RepositoryException {
        String stmt = "/" + jcrRoot + "/" + testPath+ "/*[@" + jcrPrimaryType + " or @" + jcrMixinTypes + "]";

        try {
            qm.createQuery(stmt, Query.XPATH);
        } catch (InvalidQueryException e) {
            fail("invalid statement syntax for '" + stmt + "'");
        }
    }

    /**
     * Verifies that the and operator is accepted for a property name
     *
     * @throws RepositoryException
     */
    public void testAnd() throws RepositoryException {
        String stmt = "/" + jcrRoot + "/" + testPath+ "/*[@" + jcrPrimaryType + " and @" + jcrMixinTypes + "]";

        try {
            qm.createQuery(stmt, Query.XPATH);
        } catch (InvalidQueryException e) {
            fail("invalid statement syntax for '" + stmt + "'");
        }
    }

    /**
     * Verifies that the and operator is accepted for properties's values
     *
     * @throws RepositoryException
     */
    public void testCombinedAnd() throws RepositoryException {
        String stmt = "/" + jcrRoot + "/" + testPath+ "/*[@" + jcrPrimaryType + "='" + nodeTypeName + "' and @" + jcrPrimaryType + "='" + ntBase + "']";

        try {
            qm.createQuery(stmt, Query.XPATH);
        } catch (InvalidQueryException e) {
            fail("invalid statement syntax for '" + stmt + "'");
        }
    }
}