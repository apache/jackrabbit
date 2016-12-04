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

import static javax.jcr.query.Query.JCR_SQL2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.input.NullInputStream;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.query.AbstractIndexingTest;

/**
 * <code>SQL2IndexingAggregateTest</code> checks if aggregation rules defined in
 * workspace indexing-test work properly.
 * 
 * See src/test/repository/workspaces/indexing-test/indexing-configuration.xml
 */
public class SQL2IndexingAggregateTest extends AbstractIndexingTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        for (Node c : JcrUtils.getChildNodes(testRootNode)) {
            testRootNode.getSession().removeItem(c.getPath());
        }
        testRootNode.getSession().save();
    }

    /**
     * 
     * this test is very similar to
     * {@link SQL2IndexingAggregateTest#testNtFileAggregate()
     * testNtFileAggregate} but checks embedded index aggregates.
     * 
     * The aggregation hierarchy is defined in
     * src/test/repository/workspaces/indexing-test/indexing-configuration.xml
     * 
     * basically a folder aggregates other folders and files that aggregate a
     * stream of content.
     * 
     * see <a href="https://issues.apache.org/jira/browse/JCR-2989">JCR-2989</a>
     * 
     * nt:folder: recursive="true" recursiveLimit="10"
     * 
     */
    @SuppressWarnings("unchecked")
    public void testDeepHierarchy() throws Exception {

        // this parameter IS the 'recursiveLimit' defined in the index
        // config file
        int definedRecursiveLimit = 10;
        int levelsDeep = 14;

        List<Node> allNodes = new ArrayList<Node>();
        List<Node> updatedNodes = new ArrayList<Node>();
        List<Node> staleNodes = new ArrayList<Node>();

        String sqlBase = "SELECT * FROM [nt:folder] as f WHERE ";
        String sqlCat = sqlBase + " CONTAINS (f.*, 'cat')";
        String sqlDog = sqlBase + " CONTAINS (f.*, 'dog')";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(out, "UTF-8");
        writer.write("the quick brown fox jumps over the lazy dog.");
        writer.flush();

        Node folderRoot = testRootNode.addNode("myFolder", "nt:folder");
        Node folderChild = folderRoot;
        allNodes.add(folderChild);

        for (int i = 0; i < levelsDeep; i++) {
            folderChild.addNode("0" + i + "-dummy", "nt:folder");
            folderChild = folderChild.addNode("0" + i, "nt:folder");
            allNodes.add(folderChild);

            // -2 because:
            // 1 because 'i' starts at 0,
            // +
            // 1 because we are talking about same node type aggregation levels
            // extra to the current node that is updated
            if (i > levelsDeep - definedRecursiveLimit - 2) {
                updatedNodes.add(folderChild);
            }
        }
        staleNodes.addAll(CollectionUtils.disjunction(allNodes, updatedNodes));

        Node file = folderChild.addNode("myFile", "nt:file");
        Node resource = file.addNode("jcr:content", "nt:resource");
        resource.setProperty("jcr:lastModified", Calendar.getInstance());
        resource.setProperty("jcr:encoding", "UTF-8");
        resource.setProperty("jcr:mimeType", "text/plain");
        resource.setProperty("jcr:data", session.getValueFactory()
                .createBinary(new ByteArrayInputStream(out.toByteArray())));

        testRootNode.getSession().save();

        // because of the optimizations, the first save is expected to update
        // ALL nodes
        // executeSQL2Query(sqlDog, allNodes.toArray(new Node[] {}));

        // update jcr:data
        out.reset();
        writer.write("the quick brown fox jumps over the lazy cat.");
        writer.flush();
        resource.setProperty("jcr:data", session.getValueFactory()
                .createBinary(new ByteArrayInputStream(out.toByteArray())));
        testRootNode.getSession().save();
        executeSQL2Query(sqlDog, staleNodes.toArray(new Node[] {}));
        executeSQL2Query(sqlCat, updatedNodes.toArray(new Node[] {}));

        // replace jcr:content with unstructured
        resource.remove();
        Node unstrContent = file.addNode("jcr:content", "nt:unstructured");
        Node foo = unstrContent.addNode("foo");
        foo.setProperty("text", "the quick brown fox jumps over the lazy dog.");
        testRootNode.getSession().save();
        executeSQL2Query(sqlDog, allNodes.toArray(new Node[] {}));
        executeSQL2Query(sqlCat, new Node[] {});

        // remove foo
        foo.remove();
        testRootNode.getSession().save();
        executeSQL2Query(sqlDog, staleNodes.toArray(new Node[] {}));
        executeSQL2Query(sqlCat, new Node[] {});

    }

    /**
     * simple index aggregation from jcr:content to nt:file
     * 
     * The aggregation hierarchy is defined in
     * src/test/repository/workspaces/indexing-test/indexing-configuration.xml
     * 
     */
    public void testNtFileAggregate() throws Exception {

        String sqlBase = "SELECT * FROM [nt:file] as f"
                + " WHERE ISCHILDNODE([" + testRoot + "])";
        String sqlCat = sqlBase + " AND CONTAINS (f.*, 'cat')";
        String sqlDog = sqlBase + " AND CONTAINS (f.*, 'dog')";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(out, "UTF-8");
        writer.write("the quick brown fox jumps over the lazy dog.");
        writer.flush();

        Node file = testRootNode.addNode("myFile", "nt:file");
        Node resource = file.addNode("jcr:content", "nt:resource");
        resource.setProperty("jcr:lastModified", Calendar.getInstance());
        resource.setProperty("jcr:encoding", "UTF-8");
        resource.setProperty("jcr:mimeType", "text/plain");
        resource.setProperty("jcr:data", session.getValueFactory()
                .createBinary(new ByteArrayInputStream(out.toByteArray())));
        testRootNode.getSession().save();
        executeSQL2Query(sqlDog, new Node[] { file });

        // update jcr:data
        out.reset();
        writer.write("the quick brown fox jumps over the lazy cat.");
        writer.flush();
        resource.setProperty("jcr:data", session.getValueFactory()
                .createBinary(new ByteArrayInputStream(out.toByteArray())));
        testRootNode.getSession().save();
        executeSQL2Query(sqlDog, new Node[] {});
        executeSQL2Query(sqlCat, new Node[] { file });

        // replace jcr:content with unstructured
        resource.remove();
        Node unstrContent = file.addNode("jcr:content", "nt:unstructured");
        Node foo = unstrContent.addNode("foo");
        foo.setProperty("text", "the quick brown fox jumps over the lazy dog.");
        testRootNode.getSession().save();
        executeSQL2Query(sqlDog, new Node[] { file });
        executeSQL2Query(sqlCat, new Node[] {});

        // remove foo
        foo.remove();
        testRootNode.getSession().save();
        executeSQL2Query(sqlDog, new Node[] {});
        executeSQL2Query(sqlCat, new Node[] {});

        // replace jcr:content again with resource
        unstrContent.remove();
        resource = file.addNode("jcr:content", "nt:resource");
        resource.setProperty("jcr:lastModified", Calendar.getInstance());
        resource.setProperty("jcr:encoding", "UTF-8");
        resource.setProperty("jcr:mimeType", "text/plain");
        resource.setProperty("jcr:data", session.getValueFactory()
                .createBinary(new ByteArrayInputStream(out.toByteArray())));
        testRootNode.getSession().save();
        executeSQL2Query(sqlDog, new Node[] {});
        executeSQL2Query(sqlCat, new Node[] { file });

    }

    /**
     * JCR-3160 - Session#move doesn't trigger rebuild of parent node
     * aggregation
     */
    public void testAggregateMove() throws Exception {

        String sql = "SELECT * FROM [nt:folder] as f"
                + " WHERE ISDESCENDANTNODE([" + testRoot + "])"
                + " AND CONTAINS (f.*, 'dog')";

        Node folderA = testRootNode.addNode("folderA", "nt:folder");
        Node folderB = testRootNode.addNode("folderB", "nt:folder");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(out, "UTF-8");
        writer.write("the quick brown fox jumps over the lazy dog.");
        writer.flush();

        Node file = folderA.addNode("myFile", "nt:file");
        Node resource = file.addNode("jcr:content", "nt:resource");
        resource.setProperty("jcr:lastModified", Calendar.getInstance());
        resource.setProperty("jcr:encoding", "UTF-8");
        resource.setProperty("jcr:mimeType", "text/plain");
        resource.setProperty("jcr:data", session.getValueFactory()
                .createBinary(new ByteArrayInputStream(out.toByteArray())));
        
        testRootNode.getSession().save();
        executeSQL2Query(sql, new Node[] { folderA });

        testRootNode.getSession().move(file.getPath(),
                folderB.getPath() + "/myFile");
        testRootNode.getSession().save();
        executeSQL2Query(sql, new Node[] { folderB });
    }

    /**
     * By default, the recursive aggregation is turned off.
     * 
     * The aggregation hierarchy is defined in
     * src/test/repository/workspaces/indexing-test/indexing-configuration.xml
     */
    public void testDefaultRecursiveAggregation() throws Exception {

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
     * Tests that even if there is a rule to include same type node aggregates
     * by property name, the recursive flag will still ignore them.
     * 
     * It should issue a log warning, though.
     * 
     * The aggregation hierarchy is defined in
     * src/test/repository/workspaces/indexing-test/indexing-configuration.xml
     */
    public void testRecursiveAggregationExclusion() throws Exception {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(out, "UTF-8");
        writer.write("the quick brown fox jumps over the lazy dog.");
        writer.flush();

        Node parent = testRootNode.addNode(
                "testDefaultRecursiveAggregation_parent",
                JcrConstants.NT_UNSTRUCTURED);

        Node child = parent.addNode("aggregated-node",
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
     * checks that while text extraction runs in the background, the node is
     * available for query on any of its properties
     * 
     * see <a href="https://issues.apache.org/jira/browse/JCR-2980">JCR-2980</a>
     * 
     */
    public void testAsyncIndexQuery() throws Exception {

        Node n = testRootNode.addNode("justnode", JcrConstants.NT_UNSTRUCTURED);
        n.setProperty("type", "testnode");
        n.setProperty("jcr:encoding", "UTF-8");
        n.setProperty("jcr:mimeType", "text/plain");
        n.setProperty(
                "jcr:data",
                session.getValueFactory().createBinary(
                        new NullInputStream(1024 * 40)));
        testRootNode.getSession().save();

        String sql = "SELECT * FROM [nt:unstructured] as f "
                + " WHERE ISCHILDNODE([" + testRoot
                + "]) and type = 'testnode' ";
        checkResult(qm.createQuery(sql, JCR_SQL2).execute(), 1);
    }

    public void testAggregatedProperty() throws Exception {

        Node parent = testRootNode.addNode("parent",
                JcrConstants.NT_UNSTRUCTURED);
        Node child = parent.addNode("child", JcrConstants.NT_UNSTRUCTURED);
        child.setProperty("property",
                "the quick brown fox jumps over the lazy dog.");
        testRootNode.getSession().save();

        String sql = "SELECT * FROM [nt:unstructured] as u WHERE CONTAINS (u.*, 'dog') ";
        executeSQL2Query(sql, new Node[] { parent, child });
    }

}
