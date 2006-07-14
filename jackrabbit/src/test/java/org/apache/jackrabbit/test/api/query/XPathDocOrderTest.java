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
 * Tests if the repository supports document order in XPath. The tests will
 * check the repository descriptor {@link javax.jcr.Repository#QUERY_XPATH_DOC_ORDER}
 * first and throw a {@link org.apache.jackrabbit.test.NotExecutableException}
 * if the descriptor is not present.
 * <p/>
 * This is a level 1 test, therefore does not write content to the workspace.
 * The tests require the following content in the default workspace:
 * <p/>
 * At least three nodes under the {@link #testRoot}.
 *
 * @test
 * @sources XPathDocOrderTest.java
 * @executeClass org.apache.jackrabbit.test.api.query.XPathDocOrderTest
 * @keywords level1
 */
public class XPathDocOrderTest extends AbstractQueryTest {

    protected void setUp() throws Exception {
        isReadOnly = true;
        super.setUp();
    }

    /**
     * Tests the <code>position()</code> function.
     * <p/>
     * For configuration description see {@link XPathDocOrderTest}.
     */
    public void testDocOrderPositionFunction() throws Exception {
        StringBuffer tmp = new StringBuffer("/");
        tmp.append(jcrRoot).append(testRoot).append("/*");
        tmp.append("[position()=2]");
        String resultPath = "";
        for (NodeIterator nodes = testRootNode.getNodes(); nodes.hasNext() && nodes.getPosition() < 2;) {
            resultPath = nodes.nextNode().getPath();
        }
        docOrderTest(new Statement(tmp.toString(), Query.XPATH), resultPath);
    }

    /**
     * Tests if position index and document order on child axis returns the
     * correct node.
     * <p/>
     * For configuration description see {@link XPathDocOrderTest}.
     */
    public void testDocOrderPositionIndex() throws Exception {
        StringBuffer tmp = new StringBuffer("/");
        tmp.append(jcrRoot).append(testRoot).append("/*");
        tmp.append("[2]");
        String resultPath = "";
        for (NodeIterator nodes = testRootNode.getNodes(); nodes.hasNext() && nodes.getPosition() < 2;) {
            resultPath = nodes.nextNode().getPath();
        }
        docOrderTest(new Statement(tmp.toString(), Query.XPATH), resultPath);
    }

    /**
     * Tests the <code>last()</code> function.
     * <p/>
     * For configuration description see {@link XPathDocOrderTest}.
     */
    public void testDocOrderLastFunction() throws Exception {
        StringBuffer tmp = new StringBuffer("/");
        tmp.append(jcrRoot).append(testRoot).append("/*");
        tmp.append("[position()=last()]");
        String resultPath = "";
        for (NodeIterator nodes = testRootNode.getNodes(); nodes.hasNext();) {
            resultPath = nodes.nextNode().getPath();
        }
        docOrderTest(new Statement(tmp.toString(), Query.XPATH), resultPath);
    }

    /**
     * Tests the <code>first()</code> function.
     * <p/>
     * For configuration description see {@link XPathDocOrderTest}.
     */
    public void testDocOrderFirstFunction() throws Exception {
        StringBuffer tmp = new StringBuffer("/");
        tmp.append(jcrRoot).append(testRoot).append("/*");
        tmp.append("[first()]");
        String resultPath = testRootNode.getNodes().nextNode().getPath();
        docOrderTest(new Statement(tmp.toString(), Query.XPATH), resultPath);
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
        if (!isSupported(Repository.QUERY_XPATH_DOC_ORDER)) {
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