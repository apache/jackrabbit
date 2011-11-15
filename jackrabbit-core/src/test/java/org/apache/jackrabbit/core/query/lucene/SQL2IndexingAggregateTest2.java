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
package org.apache.jackrabbit.core.query.lucene;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.query.AbstractIndexingTest;

/**
 * <code>SQL2IndexingAggregateTest2</code> checks if aggregation rules defined
 * in workspace indexing-test-2 work properly.
 * 
 * See src/test/repository/workspaces/indexing-test-2/indexing-configuration.xml
 */
public class SQL2IndexingAggregateTest2 extends AbstractIndexingTest {

    protected static final String WORKSPACE_NAME_NEW = "indexing-test-2";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        for (Node c : JcrUtils.getChildNodes(testRootNode)) {
            testRootNode.getSession().removeItem(c.getPath());
        }
        testRootNode.getSession().save();
    }

    @Override
    protected String getWorkspaceName() {
        return WORKSPACE_NAME_NEW;
    }

    /**
     * recursive="true" recursiveLimit="-1"
     * 
     * The aggregation hierarchy is defined in
     * src/test/repository/workspaces/indexing-test-2/indexing-configuration.xml
     * 
     * see <a href="https://issues.apache.org/jira/browse/JCR-2989">JCR-2989</a>
     */
    public void testNegativeRecursiveAggregationLimit() throws Exception {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(out, "UTF-8");
        writer.write("the quick brown fox jumps over the lazy dog.");
        writer.flush();

        Node parent = testRootNode.addNode(
                "testDefaultRecursiveAggregation_parent",
                JcrConstants.NT_UNSTRUCTURED);

        Node child = parent.addNode("testDefaultRecursiveAggregation_child",
                JcrConstants.NT_UNSTRUCTURED);
        child.setProperty("type", "testnode");
        child.setProperty("jcr:encoding", "UTF-8");
        child.setProperty("jcr:mimeType", "text/plain");
        child.setProperty(
                "jcr:data",
                session.getValueFactory().createBinary(
                        new ByteArrayInputStream(out.toByteArray())));
        testRootNode.getSession().save();

        String sqlBase = "SELECT * FROM [nt:unstructured] as u WHERE CONTAINS (u.*, 'dog') ";
        String sqlParent = sqlBase + " AND ISCHILDNODE([" + testRoot + "])";
        String sqlChild = sqlBase + " AND ISCHILDNODE([" + parent.getPath()
                + "])";

        executeSQL2Query(sqlParent, new Node[] {});
        executeSQL2Query(sqlChild, new Node[] { child });
    }

    /**
     * this should traverse the *entire* hierarchy to aggregate indexes
     * 
     * see <a href="https://issues.apache.org/jira/browse/JCR-2989">JCR-2989</a>
     */
    public void testUnlimitedRecursiveAggregation() throws Exception {

        long levelsDeep = AggregateRuleImpl.RECURSIVE_AGGREGATION_LIMIT_DEFAULT + 10;
        List<Node> expectedNodes = new ArrayList<Node>();

        String sqlBase = "SELECT * FROM [nt:folder] as f WHERE ";
        String sqlCat = sqlBase + " CONTAINS (f.*, 'cat')";
        String sqlDog = sqlBase + " CONTAINS (f.*, 'dog')";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(out, "UTF-8");
        writer.write("the quick brown fox jumps over the lazy dog.");
        writer.flush();

        Node folderRoot = testRootNode.addNode("myFolder", "nt:folder");
        Node folderChild = folderRoot;
        expectedNodes.add(folderChild);

        for (int i = 0; i < levelsDeep; i++) {
            folderChild.addNode("0" + i + "-dummy", "nt:folder");
            folderChild = folderChild.addNode("0" + i, "nt:folder");
            expectedNodes.add(folderChild);
        }

        Node file = folderChild.addNode("myFile", "nt:file");
        Node resource = file.addNode("jcr:content", "nt:resource");
        resource.setProperty("jcr:lastModified", Calendar.getInstance());
        resource.setProperty("jcr:encoding", "UTF-8");
        resource.setProperty("jcr:mimeType", "text/plain");
        resource.setProperty("jcr:data", session.getValueFactory()
                .createBinary(new ByteArrayInputStream(out.toByteArray())));

        testRootNode.getSession().save();
        waitForTextExtractionTasksToFinish();
        executeSQL2Query(sqlDog, expectedNodes.toArray(new Node[] {}));

        // update jcr:data
        out.reset();
        writer.write("the quick brown fox jumps over the lazy cat.");
        writer.flush();
        resource.setProperty("jcr:data", session.getValueFactory()
                .createBinary(new ByteArrayInputStream(out.toByteArray())));
        testRootNode.getSession().save();
        waitForTextExtractionTasksToFinish();
        executeSQL2Query(sqlDog, new Node[] {});
        executeSQL2Query(sqlCat, expectedNodes.toArray(new Node[] {}));

        // replace jcr:content with unstructured
        resource.remove();
        Node unstrContent = file.addNode("jcr:content", "nt:unstructured");
        Node foo = unstrContent.addNode("foo");
        foo.setProperty("text", "the quick brown fox jumps over the lazy dog.");
        testRootNode.getSession().save();
        waitForTextExtractionTasksToFinish();
        executeSQL2Query(sqlDog, expectedNodes.toArray(new Node[] {}));
        executeSQL2Query(sqlCat, new Node[] {});

        // remove foo
        foo.remove();
        testRootNode.getSession().save();
        waitForTextExtractionTasksToFinish();
        executeSQL2Query(sqlDog, new Node[] {});
        executeSQL2Query(sqlCat, new Node[] {});

    }
}
