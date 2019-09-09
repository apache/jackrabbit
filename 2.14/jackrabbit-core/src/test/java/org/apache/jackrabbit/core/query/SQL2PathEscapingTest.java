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
package org.apache.jackrabbit.core.query;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Source;

import org.apache.jackrabbit.commons.JcrUtils;

/**
 * Test case for path escaping
 * 
 * Inspired by <a
 * href="https://issues.apache.org/jira/browse/JCR-2939">JCR-2939</a>
 * 
 */
public class SQL2PathEscapingTest extends AbstractQueryTest {

    private Node n1;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        n1 = testRootNode.addNode("a b");
        n1.addNode("x");
        n1.addNode("y");
        testRootNode.getSession().save();
    }

    @Override
    protected void tearDown() throws Exception {
        for (Node c : JcrUtils.getChildNodes(testRootNode)) {
            testRootNode.getSession().removeItem(c.getPath());
        }
        testRootNode.getSession().save();
        super.tearDown();
    }

    /**
     * not escaping the spaced path will not get you anything
     * 
     * @throws Exception
     */
    public void testGetChildrenNoEscaping() throws Exception {
        StringBuilder select = new StringBuilder();
        select.append("select * from [nt:base] AS selector where ISCHILDNODE(selector, ["
                + n1.getPath() + "]) ");
        checkResult(executeSQL2Query(select.toString()), 0);
    }

    /**
     * 
     * escaping the entire path should work
     * 
     * @throws Exception
     * 
     */
    public void testGetChildrenEscapedFull() throws Exception {
        StringBuilder select = new StringBuilder();
        select.append("select * from [nt:base] AS selector where ISCHILDNODE(selector, ['"
                + n1.getPath() + "']) ");
        checkResult(executeSQL2Query(select.toString()), 2);
    }

    /**
     * escaping just the spaced path should work too
     * 
     * @throws Exception
     */
    public void testGetChildrenEscapedNode() throws Exception {
        String path = n1.getParent().getPath() + "/'" + "a b" + "'";
        StringBuilder select = new StringBuilder();
        select.append("select * from [nt:base] AS selector where ISCHILDNODE(selector, ["
                + path + "]) ");
        checkResult(executeSQL2Query(select.toString()), 2);
    }

    /**
     * will build a query directly via the api using a spaced path
     * 
     * @throws Exception
     */
    public void testGetChildrenApiDirect() throws Exception {
        QueryObjectModelFactory qomf = qm.getQOMFactory();
        Source source1 = qomf.selector(NodeType.NT_BASE, "selector");
        Column[] columns = new Column[] { qomf.column("selector", null, null) };
        Constraint constraint2 = qomf.childNode("selector", n1.getPath());
        QueryObjectModel qom = qomf.createQuery(source1, constraint2, null,
                columns);
        checkResult(qom.execute(), 2);
    }

    /**
     * the statement behind the api should be consistent, and return a similar
     * query. in our case it should escape the paths that have spaces in them by
     * enclosing them in single quotes
     * 
     * @throws Exception
     */
    public void testGetChildrenApiStatement() throws Exception {

        QueryObjectModelFactory qomf = qm.getQOMFactory();
        Source source1 = qomf.selector(NodeType.NT_BASE, "selector");
        Column[] columns = new Column[] { qomf.column("selector", null, null) };
        Constraint constraint2 = qomf.childNode("selector", n1.getPath());
        QueryObjectModel qom = qomf.createQuery(source1, constraint2, null,
                columns);
        checkResult(executeSQL2Query(qom.getStatement()), 2);
    }

}
