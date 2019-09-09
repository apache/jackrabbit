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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.apache.jackrabbit.core.query.AbstractIndexingTest;


/**
 * <code>IndexingRuleTest</code> performs indexing rule tests.
 */
public class IndexingRuleTest extends AbstractIndexingTest {

    private static final String NT_UNSTRUCTURED = "nt:unstructured";

    private static final String TEXT = "the quick brown fox jumps over the lazy dog";

    public void testRegexp() throws RepositoryException {
        Node node1 = testRootNode.addNode(nodeName1, NT_UNSTRUCTURED);
        node1.setProperty("rule", "regexp");
        node1.setProperty("Text", "foo");
        Node node2 = testRootNode.addNode(nodeName2, NT_UNSTRUCTURED);
        node2.setProperty("rule", "regexp");
        node2.setProperty("OtherText", "foo");
        Node node3 = testRootNode.addNode(nodeName3, NT_UNSTRUCTURED);
        node3.setProperty("rule", "regexp");
        node3.setProperty("Textle", "foo");
        testRootNode.save();
        String stmt = "/jcr:root" + testRootNode.getPath() +
                "/*[jcr:contains(., 'foo')]";
        checkResult(executeQuery(stmt), new Node[]{node1, node2});
    }

    public void testBoost() throws RepositoryException {
        Node node1 = testRootNode.addNode(nodeName1, NT_UNSTRUCTURED);
        node1.setProperty("rule", "boost1");
        node1.setProperty("text", TEXT);
        Node node2 = testRootNode.addNode(nodeName2, NT_UNSTRUCTURED);
        node2.setProperty("rule", "boost2");
        node2.setProperty("text", TEXT);
        Node node3 = testRootNode.addNode(nodeName3, NT_UNSTRUCTURED);
        node3.setProperty("rule", "boost3");
        node3.setProperty("text", TEXT);
        testRootNode.getSession().save();
        String stmt = "/jcr:root"
                + testRootNode.getPath()
                + "/*[jcr:contains(@text, 'quick')] order by @jcr:score descending";
        executeXPathQuery(stmt, new Node[] { node3, node2, node1 });
    }

    public void testNodeScopeIndex() throws RepositoryException {
        Node node1 = testRootNode.addNode(nodeName1, NT_UNSTRUCTURED);
        node1.setProperty("rule", "nsiTrue");
        node1.setProperty("text", TEXT);
        Node node2 = testRootNode.addNode(nodeName2, NT_UNSTRUCTURED);
        node2.setProperty("rule", "nsiFalse");
        node2.setProperty("text", TEXT);
        testRootNode.save();
        String stmt = "/jcr:root" + testRootNode.getPath() +
                "/*[jcr:contains(., 'quick')]";
        checkResult(executeQuery(stmt), new Node[]{node1});
    }

    public void testNodeType() throws RepositoryException {
        // assumes there is an index-rule for nt:hierarchyNode that
        // does not include the property jcr:created
        Node node1 = testRootNode.addNode(nodeName1, "nt:folder");
        testRootNode.save();
        String stmt = "/jcr:root" + testRootNode.getPath() +
                "/*[@" + jcrCreated + " = xs:dateTime('" +
                node1.getProperty(jcrCreated).getString() + "')]";
        checkResult(executeQuery(stmt), new Node[]{});
    }

    public void testUseInExcerpt() throws RepositoryException {
        Node node = testRootNode.addNode(nodeName1, NT_UNSTRUCTURED);
        node.setProperty("rule", "excerpt");
        node.setProperty("title", "Apache Jackrabbit");
        // the value below is for testing https://issues.apache.org/jira/browse/JCR-3610
        node.setProperty("foo", "<some>markup</some>");
        node.setProperty("text", "Jackrabbit is a JCR implementation");
        testRootNode.save();
        String stmt = "/jcr:root" + testRootNode.getPath() +
                "/*[jcr:contains(., 'jackrabbit implementation')]/rep:excerpt(.)";
        RowIterator rows = executeQuery(stmt).getRows();
        assertTrue("No results returned", rows.hasNext());
        Value excerpt = rows.nextRow().getValue("rep:excerpt(.)");
        assertNotNull("No excerpt created", excerpt);
        assertTrue("Title must not be present in excerpt",
                excerpt.getString().indexOf("Apache") == -1);
        assertTrue("Missing highlight",
                excerpt.getString().indexOf("<strong>implementation</strong>") != -1);

        stmt = "/jcr:root" + testRootNode.getPath() +
                "/*[jcr:contains(., 'apache')]/rep:excerpt(.)";
        rows = executeQuery(stmt).getRows();
        assertTrue("No results returned", rows.hasNext());
        excerpt = rows.nextRow().getValue("rep:excerpt(.)");
        assertNotNull("No excerpt created", excerpt);
        assertTrue("Title must not be present in excerpt",
                excerpt.getString().indexOf("Apache") == -1);
    }

    public void testExcerptOnExcludedProperty() throws RepositoryException {
        Node node = testRootNode.addNode(nodeName1, NT_UNSTRUCTURED);
        node.setProperty("rule", "excerpt");
        node.setProperty("title", TEXT);
        testRootNode.save();
        String stmt = "/jcr:root" + testRootNode.getPath() +
                "/*[jcr:contains(., 'quick')]/rep:excerpt(.)";
        QueryResult result = executeQuery(stmt);
        checkResult(result, new Node[]{node});
        Value excerpt = result.getRows().nextRow().getValue("rep:excerpt(.)");
        assertNotNull("No excerpt created", excerpt);
    }

    public void testUseInExcerptWithAggregate() throws RepositoryException {
        Node node = testRootNode.addNode(nodeName1, NT_UNSTRUCTURED);
        node.setProperty("rule", "excerpt");
        node.setProperty("title", "Apache Jackrabbit");
        node.setProperty("text", "Jackrabbit is a JCR implementation");
        Node aggregated = node.addNode("aggregated-node", NT_UNSTRUCTURED);
        aggregated.setProperty("rule", "excerpt");
        aggregated.setProperty("title", "Apache Jackrabbit");
        aggregated.setProperty("text", "Jackrabbit is a JCR implementation");
        testRootNode.save();

        String stmt = "/jcr:root" + testRootNode.getPath() +
                "/*[jcr:contains(., 'jackrabbit')]/rep:excerpt(.)";
        RowIterator rows = executeQuery(stmt).getRows();
        assertTrue("No results returned", rows.hasNext());
        Value excerpt;
        while (rows.hasNext()) {
            excerpt = rows.nextRow().getValue("rep:excerpt(.)");
            assertNotNull("No excerpt created", excerpt);
            assertTrue("Title must not be present in excerpt",
                    excerpt.getString().indexOf("Apache") == -1);
            int idx = 0;
            int numHighlights = 0;
            for (;;) {
                idx = excerpt.getString().indexOf("<strong>", idx);
                if (idx == -1) {
                    break;
                }
                numHighlights++;
                int endIdx = excerpt.getString().indexOf("</strong>", idx);
                assertEquals("wrong highlight", "Jackrabbit",
                        excerpt.getString().substring(idx + "<strong>".length(), endIdx));
                idx = endIdx;
            }
            assertTrue("Missing highlight", numHighlights > 0);
        }

        stmt = "/jcr:root" + testRootNode.getPath() +
                "/*[jcr:contains(., 'apache')]/rep:excerpt(.)";
        rows = executeQuery(stmt).getRows();
        assertTrue("No results returned", rows.hasNext());
        excerpt = rows.nextRow().getValue("rep:excerpt(.)");
        assertNotNull("No excerpt created", excerpt);
        assertTrue("Title must not be present in excerpt",
                excerpt.getString().indexOf("Apache") == -1);
    }
}
