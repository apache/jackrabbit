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

import static javax.jcr.query.Query.JCR_SQL2;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.commons.cnd.CndImporter;

/**
 * Test case for OUTER JOIN queries with JCR_SQL2
 * 
 * Inspired by <a
 * href="https://issues.apache.org/jira/browse/JCR-2933">JCR-2933</a>
 * 
 */
public class SQL2OuterJoinTest extends AbstractQueryTest {

    private Node n2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        NodeTypeManager manager = superuser.getWorkspace().getNodeTypeManager();
        if (!manager.hasNodeType("test:SamplePage")) {
            StringBuilder defs = new StringBuilder();
            defs.append("[test:SamplePage]\n");
            defs.append("  - n1prop1\n");
            defs.append("  + * (nt:base) = nt:unstructured \n");
            defs.append("[test:SampleContent]\n");
            defs.append("  - n2prop1");
            Reader cndReader = new InputStreamReader(new ByteArrayInputStream(
                    defs.toString().getBytes()));
            CndImporter.registerNodeTypes(cndReader, superuser);
        }

        Node n1 = testRootNode.addNode("node1", "test:SamplePage");
        n1.setProperty("n1prop1", "page1");
        n2 = n1.addNode("node2", "test:SampleContent");
        n2.setProperty("n2prop1", "content1");
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

    public void testOuterJoin() throws Exception {
        StringBuilder join = new StringBuilder();
        join.append(" Select * from [test:SamplePage] as page left outer join [test:SampleContent] as content on ISDESCENDANTNODE(content,page)");
        checkResult(qm.createQuery(join.toString(), JCR_SQL2).execute(), 1);
    }

    /**
     * Test case for <a
     * href="https://issues.apache.org/jira/browse/JCR-2933">JCR-2933</a>
     * 
     * Outer Join that works OOTB
     */
    public void testOuterJoinWithCondition() throws Exception {
        StringBuilder join = new StringBuilder();
        join.append(" Select * from [test:SamplePage] as page left outer join [test:SampleContent] as content on ISDESCENDANTNODE(content,page) where page.n1prop1 = 'page1' and content.n2prop1 = 'content1' ");
        checkResult(qm.createQuery(join.toString(), JCR_SQL2).execute(), 1);
    }

    /**
     * Test case for <a
     * href="https://issues.apache.org/jira/browse/JCR-2933">JCR-2933</a>
     * 
     * Outer Join that does not work on missing where clause, hence the jira
     * issue
     */
    public void testOuterJoinMissingProperty() throws Exception {
        StringBuilder join = new StringBuilder();
        join.append(" Select * from [test:SamplePage] as page left outer join [test:SampleContent] as content on ISDESCENDANTNODE(content,page) where page.n1prop1 = 'page1' and content.n2prop1 = 'XXX' ");
        checkResult(qm.createQuery(join.toString(), JCR_SQL2).execute(), 0);
    }

    /**
     * Test case for <a
     * href="https://issues.apache.org/jira/browse/JCR-2933">JCR-2933</a>
     * 
     * Outer Join that does not work on missing child node, no WHERE condition
     */
    public void testOuterJoinMissingNode() throws Exception {
        testRootNode.getSession().removeItem(n2.getPath());
        testRootNode.getSession().save();

        StringBuilder join = new StringBuilder();
        join.append(" Select * from [test:SamplePage] as page left outer join [test:SampleContent] as content on ISDESCENDANTNODE(content,page)");
        checkResult(qm.createQuery(join.toString(), JCR_SQL2).execute(), 1);
    }

    public void testOuterJoinMissingNodeWithCondition() throws Exception {
        testRootNode.getSession().removeItem(n2.getPath());
        testRootNode.getSession().save();

        StringBuilder join = new StringBuilder();
        join.append(" Select * from [test:SamplePage] as page left outer join [test:SampleContent] as content on ISDESCENDANTNODE(content,page) where page.n1prop1 = 'page1' and content.n2prop1 = 'XXX' ");
        checkResult(qm.createQuery(join.toString(), JCR_SQL2).execute(), 0);
    }

    public void testOuterJoinExtraNode() throws Exception {
        Node n3 = testRootNode.addNode("node3", "test:SamplePage");
        n3.setProperty("n1prop1", "page1");
        testRootNode.getSession().save();

        StringBuilder join = new StringBuilder();
        join.append(" Select * from [test:SamplePage] as page left outer join [test:SampleContent] as content on ISDESCENDANTNODE(content,page)");
        checkResult(qm.createQuery(join.toString(), JCR_SQL2).execute(), 2);
    }

    public void testOuterJoinExtraNodeWithCondition() throws Exception {
        Node n3 = testRootNode.addNode("node3", "test:SamplePage");
        n3.setProperty("n1prop1", "page1");
        testRootNode.getSession().save();

        StringBuilder join = new StringBuilder();
        join.append(" Select * from [test:SamplePage] as page left outer join [test:SampleContent] as content on ISDESCENDANTNODE(content,page) where page.n1prop1 = 'page1' and content.n2prop1 = 'XXX' ");
        checkResult(qm.createQuery(join.toString(), JCR_SQL2).execute(), 0);
    }

    public void testOuterJoinDoubleJoinSplit() throws Exception {
        Node n3 = testRootNode.addNode("node3", "test:SamplePage");
        n3.setProperty("n1prop1", "page2");

        Node n4 = n3.addNode("node2", "test:SampleContent");
        n4.setProperty("n2prop1", "content1");
        testRootNode.getSession().save();

        StringBuilder join = new StringBuilder();
        join.append("Select * from [test:SamplePage] as page left outer join [test:SampleContent] as content on ISDESCENDANTNODE(content,page) where (page.n1prop1 = 'page1' and content.n2prop1 = 'content1') or (page.n1prop1 = 'page2' and content.n2prop1 = 'content1') ");
        checkResult(qm.createQuery(join.toString(), JCR_SQL2).execute(), 2);
    }
}
