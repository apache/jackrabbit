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
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Test the method {@link Query#getLanguage()}.
 *
 * @test
 * @sources GetLanguageTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.GetLanguageTest
 * @keywords level1
 */
public class GetLanguageTest extends AbstractQueryTest {

    /** A read-only session */
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
     * Tests if a XPath query returns {@link Query#XPATH} when calling
     * {@link Query#getLanguage()}.
     */
    public void testGetLanguage() throws RepositoryException {
        String statement = "/" + jcrRoot;
        Query q = session.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        assertEquals("Query returns wrong language.", Query.XPATH, q.getLanguage());
    }
}
