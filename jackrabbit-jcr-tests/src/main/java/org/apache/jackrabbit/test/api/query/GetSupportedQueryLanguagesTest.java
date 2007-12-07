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

import javax.jcr.query.QueryManager;
import javax.jcr.query.Query;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Repository;
import java.util.List;
import java.util.Arrays;

/**
 * Test the method {@link QueryManager#getSupportedQueryLanguages()}.
 *
 * @test
 * @sources GetSupportedQueryLanguagesTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.GetSupportedQueryLanguagesTest
 * @keywords level1
 */
public class GetSupportedQueryLanguagesTest extends AbstractQueryTest {

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
     * Tests if all implementations return {@link Query#XPATH} with
     * {@link QueryManager#getSupportedQueryLanguages()}. Tests if repositores
     * that have the SQL descriptor set in the repository return {@link Query#SQL}.
     */ 
    public void testGetSupportedQueryLanguages() throws RepositoryException {
        List langs = Arrays.asList(session.getWorkspace().getQueryManager().getSupportedQueryLanguages());
        // all repositories must support XPath
        assertTrue("XPath not retured with QueryManager.getSupportedQueryLanguages()",
                langs.contains(Query.XPATH));

        // if repository descriptor for sql is present also sql must be returned
        if (isSupported(Repository.OPTION_QUERY_SQL_SUPPORTED)) {
            assertTrue("SQL not returned with QueryManager.getSupportedQueryLanguages()",
                    langs.contains(Query.SQL));
        }

    }
}
