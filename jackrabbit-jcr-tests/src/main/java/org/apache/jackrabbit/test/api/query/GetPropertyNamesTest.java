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
import javax.jcr.query.QueryResult;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.PropertyDefinition;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Tests if the property names of an XPath query without a jcr:primaryType
 * predicate matches the ones declared in nt:base.
 *
 * @test
 * @sources GetPropertyNamesTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.GetPropertyNamesTest
 * @keywords level1
 */
public class GetPropertyNamesTest extends AbstractQueryTest {

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
     * Check if the property names from the search results match the
     * non-residual ones from the base node type
     */
    public void testGetPropertyNames() throws RepositoryException {
        String queryStatement = "/" + jcrRoot;

        // build and execute search query
        Query query = superuser.getWorkspace().getQueryManager().createQuery(queryStatement, Query.XPATH);
        QueryResult result = query.execute();

        // Get the node's non-residual properties
        PropertyDefinition[] pd = superuser.getWorkspace().getNodeTypeManager().getNodeType(ntBase).getDeclaredPropertyDefinitions();

        List singleValPropNames = new ArrayList();
        for (int i = 0; i < pd.length; i++) {
            // only keep the single-value properties
            if (!pd[i].isMultiple()) {
                singleValPropNames.add(pd[i].getName());
            }
        }
        // add jcr:path
        singleValPropNames.add(jcrPath);
        singleValPropNames.add(jcrScore);

        String[] foundPropertyNames = result.getColumnNames();
        Object[] realPropertyNames = singleValPropNames.toArray();

        // sort the 2 arrays before comparing them
        Arrays.sort(foundPropertyNames);
        Arrays.sort(realPropertyNames);

        assertTrue("Property names don't match", Arrays.equals(foundPropertyNames, realPropertyNames));
    }
}