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

import javax.jcr.RepositoryException;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 * Tests if the repository supports position index in XPath. The tests will
 * check the repository descriptor {@link javax.jcr.Repository#QUERY_XPATH_POS_INDEX}
 * first and throw a {@link org.apache.jackrabbit.test.NotExecutableException}
 * if the descriptor is not present.
 * <p/>
 * This is a level 1 test, therefore does not write content to the workspace.
 * The tests require the following content in the default workspace:
 * <p/>
 * At least three nodes with the name {@link #nodeName1} under the
 * {@link #testRoot}. 
 *
 * @test
 * @sources XPathPosIndexTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.XPathPosIndexTest
 * @keywords level1
 */
public class XPathPosIndexTest extends AbstractQueryTest {

    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
    }

    /**
     * Test if the indexed notation is supported.
     * <p/>
     * For configuration description see {@link XPathPosIndexTest}.
     */
    public void testDocOrderIndexedNotation() throws Exception {
        String path = testRoot + "/" + nodeName1 + "[2]";
        StringBuffer tmp = new StringBuffer("/");
        tmp.append(jcrRoot).append(path);
        docOrderTest(new Statement(tmp.toString(), Query.XPATH), path);
    }

    //-----------------------------< internal >---------------------------------

    /**
     * Executes a statement, checks if the Result contains exactly one node with
     * <code>path</code>.
     *
     * @param stmt to be executed
     * @param path the path of the node in the query result.
     */
    private void docOrderTest(Statement stmt, String path)
            throws RepositoryException, NotExecutableException {
        if (!isSupported(Repository.QUERY_XPATH_POS_INDEX)) {
            throw new NotExecutableException("Repository does not support document order on result set.");
        }

        int count = 0;
        // check precondition: at least 3 nodes
        for (NodeIterator it = testRootNode.getNodes(); it.hasNext(); it.nextNode()) {
            count++;
        }
        if (count < 3) {
            throw new NotExecutableException("Workspace does not contain enough content under: " + testRoot +
                    ". At least 3 nodes are required for this test.");
        }

        QueryResult result = execute(stmt);
        checkResult(result, 1);
        assertEquals("Wrong result node.", path, result.getNodes().nextNode().getPath());
    }
}