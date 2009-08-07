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

import org.apache.jackrabbit.test.api.PropertyUtil;
import org.apache.jackrabbit.test.NotExecutableException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.PropertyType;
import javax.jcr.Property;
import javax.jcr.Node;
import javax.jcr.Value;
import java.util.List;
import java.util.ArrayList;

/**
 * Tests the XPath function jcr:deref() in a level 1 repository.
 *
 * @test
 * @sources DerefQueryLevel1Test.java
 * @executeClass org.apache.jackrabbit.test.api.query.DerefQueryLevel1Test
 * @keywords level1 deref
 */
public class DerefQueryLevel1Test extends AbstractQueryTest {

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
     * Test a deref query on a single valued reference property with a node test.
     * @throws NotExecutableException if the workspace does not have sufficient
     *  content.
     */
    public void testDerefSinglePropWithNodeTest()
            throws RepositoryException, NotExecutableException {
        Property refProp = PropertyUtil.searchProp(session, testRootNode, PropertyType.REFERENCE, Boolean.FALSE);
        if (refProp == null) {
            throw new NotExecutableException("Workspace does not contain a node with a reference property.");
        }
        Node target = refProp.getNode();
        String xpath = createStatement(refProp, target.getName());
        executeDerefQuery(session, xpath, new Node[]{target});
    }

    /**
     * Test a deref query on a single valued reference property with a '*' node
     * test.
     * @throws NotExecutableException if the workspace does not have sufficient
     *  content.
     */
    public void testDerefSinglePropWithNodeStar()
            throws RepositoryException, NotExecutableException {
        Property refProp = PropertyUtil.searchProp(session, testRootNode, PropertyType.REFERENCE, Boolean.FALSE);
        if (refProp == null) {
            throw new NotExecutableException("Workspace does not contain a node with a reference property.");
        }
        Node target = refProp.getNode();
        String xpath = createStatement(refProp, "*");
        executeDerefQuery(session, xpath, new Node[]{target});
    }

    /**
     * Test a deref query on a multi valued reference property with a node test.
     * @throws NotExecutableException if the workspace does not have sufficient
     *  content.
     */
    public void testDerefMultiPropWithNodeTest()
            throws RepositoryException, NotExecutableException {
        Property refProp = PropertyUtil.searchMultivalProp(testRootNode, PropertyType.REFERENCE);
        if (refProp == null) {
            throw new NotExecutableException("Workspace does not contain a node with a multivalue reference property.");
        }
        Value[] targets = refProp.getValues();
        Node[] targetNodes = new Node[targets.length];
        for (int i = 0; i < targets.length; i++) {
            targetNodes[i] = session.getNodeByUUID(targets[i].getString());
        }
        if (targetNodes.length == 0) {
            throw new NotExecutableException("Reference property does not contain a value");
        }
        String nodeName = targetNodes[0].getName();
        List resultNodes = new ArrayList();
        for (int i = 0; i < targetNodes.length; i++) {
            if (targetNodes[i].getName().equals(nodeName)) {
                resultNodes.add(targetNodes[i]);
            }
        }
        targetNodes = (Node[]) resultNodes.toArray(new Node[resultNodes.size()]);
        String xpath = createStatement(refProp, nodeName);
        executeDerefQuery(session, xpath, targetNodes);
    }

    /**
     * Test a deref query on a multi valued reference property with a '*' node.
     * @throws NotExecutableException if the workspace does not have sufficient
     *  content.
     */
    public void testDerefMultiPropWithNodeStar()
            throws RepositoryException, NotExecutableException {
        Property refProp = PropertyUtil.searchMultivalProp(testRootNode, PropertyType.REFERENCE);
        if (refProp == null) {
            throw new NotExecutableException("Workspace does not contain a node with a multivalue reference property.");
        }
        Value[] targets = refProp.getValues();
        Node[] targetNodes = new Node[targets.length];
        for (int i = 0; i < targets.length; i++) {
            targetNodes[i] = session.getNodeByUUID(targets[i].getString());
        }
        if (targetNodes.length == 0) {
            throw new NotExecutableException("Reference property does not contain a value");
        }
        String xpath = createStatement(refProp, "*");
        executeDerefQuery(session, xpath, targetNodes);
    }

    //----------------------------< internal >----------------------------------

    /**
     * Creates a xpath deref statement with a reference property and a nametest.
     * @param refProperty the reference property.
     * @param nameTest the nametest.
     * @return the xpath statement.
     */
    private String createStatement(Property refProperty, String nameTest)
            throws RepositoryException {
        StringBuffer stmt = new StringBuffer();
        stmt.append("/").append(jcrRoot).append(refProperty.getParent().getPath());
        stmt.append("/").append(jcrDeref).append("(@");
        stmt.append(refProperty.getName()).append(", '");
        stmt.append(nameTest).append("')");
        return stmt.toString();
    }

    /**
     * Executes the <code>xpath</code> query and checks the results against the
     * specified <code>nodes</code>.
     *
     * @param session the session to use for the query.
     * @param xpath   the xpath query.
     * @param nodes   the expected result nodes.
     * @throws NotExecutableException if this repository does not support the
     *                                jcr:deref() function.
     */
    private void executeDerefQuery(Session session,
                                   String xpath,
                                   Node[] nodes) throws NotExecutableException {
        try {
            executeXPathQuery(session, xpath, nodes);
        } catch (RepositoryException e) {
            // assume jcr:deref() is not supported
            throw new NotExecutableException(e.getMessage());
        }
    }
}
