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

import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Arrays;

/**
 * <code>SimpleSelectionTest</code>...
 *
 * @test
 * @sources SimpleSelectionTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.SimpleSelectionTest
 * @keywords level1
 */
public class SimpleSelectionTest extends AbstractQueryTest {

    /**
     * A read-only session
     */
    private Session session;

    /**
     * Sets up the test cases
     */
    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
        session = helper.getReadOnlySession();
        testRootNode = session.getRootNode().getNode(testPath);
    }

    /**
     * Releases the session acquired in setUp().
     */
    protected void tearDown() throws Exception {
        if (session != null) {
            session.logout();
            session = null;
        }
        super.tearDown();
    }

    /**
     * Verifies that searching for a property from a single node returns only
     * one row and has the searched property
     *
     * @throws NotExecutableException if {@link #testRootNode} does not have any
     *                                child nodes.
     */
    public void testSingleProperty()
            throws RepositoryException, NotExecutableException {

        if (!testRootNode.hasNodes()) {
            throw new NotExecutableException("Workspace does not contains enough content.");
        }
        // build search query statement
        String firstChildpath = testRootNode.getNodes().nextNode().getPath();
        String propQuery = "/" + jcrRoot + firstChildpath + "[@" + jcrPrimaryType + "]";

        // execute search query
        Query query = session.getWorkspace().getQueryManager().createQuery(propQuery, Query.XPATH);
        QueryResult result = query.execute();

        assertEquals("Should have only 1 result", 1, getSize(result.getRows()));
        assertTrue("Should contain the searched property",
                Arrays.asList(result.getColumnNames()).contains(jcrPrimaryType));
    }
}