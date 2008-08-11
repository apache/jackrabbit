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

import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Value;
import javax.jcr.query.RowIterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

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
        testRootNode.save();
        String stmt = "/jcr:root" + testRootNode.getPath() +
                "/*[jcr:contains(@text, 'quick')] order by @jcr:score descending";
        List names = new ArrayList();
        for (NodeIterator it = executeQuery(stmt).getNodes(); it.hasNext(); ) {
            names.add(it.nextNode().getName());
        }
        assertEquals("Wrong sequence or number of results.",
                Arrays.asList(new String[]{nodeName3, nodeName2, nodeName1}),
                names);
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
}
