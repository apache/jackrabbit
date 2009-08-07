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

import javax.jcr.nodetype.NodeType;
import javax.jcr.query.RowIterator;
import javax.jcr.query.Row;
import javax.jcr.Value;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;

/**
 * Implements common setup methods for level 2 queries.
 */
public abstract class AbstractQueryLevel2Test extends AbstractQueryTest {

    /**
     * Creates two nodes with name {@link #nodeName1} and {@link #nodeName2}
     * with nodetype {@link #testNodeType}. The node type must allow a String
     * property with name {@link #propertyName1} which is fulltext indexed.
     */
    protected void setUpFullTextTest() throws RepositoryException {
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        node.setProperty(propertyName1, "The quick brown fox jumps over the lazy dog.");

        node = testRootNode.addNode(nodeName2, testNodeType);
        node.setProperty(propertyName1, "The quick brown cat jumps over the lazy dog.");
        testRootNode.save();
    }

    /**
     * Creates three nodes with names: {@link #nodeName1}, {@link #nodeName2}
     * and {@link #nodeName3}. All nodes are of node type {@link #testNodeType}.
     * the node type must allow a String property with name {@link
     * #propertyName1}.
     */
    protected void setUpRangeTest() throws RepositoryException {
        Node node = testRootNode.addNode(nodeName1, testNodeType);
        node.setProperty(propertyName1, "a");

        node = testRootNode.addNode(nodeName2, testNodeType);
        node.setProperty(propertyName1, "b");

        Node cNode = node.addNode(nodeName3, testNodeType);
        cNode.setProperty(propertyName1, "c");
        testRootNode.save();
    }

    /**
     * Creates three nodes with names: {@link #nodeName1}, {@link #nodeName2}
     * and {@link #nodeName3}. All nodes are of node type {@link #testNodeType}.
     * the node type must allow a String property with name {@link
     * #propertyName1} and a multi valued String property with name {@link
     * #propertyName2}.
     * <p/>
     * If the node type does not support multi values for {@link #propertyName2}
     * a {@link org.apache.jackrabbit.test.NotExecutableException} is thrown.
     */
    protected void setUpMultiValueTest() throws RepositoryException, NotExecutableException {
        // check if NodeType supports mvp
        NodeType nt = superuser.getWorkspace().getNodeTypeManager().getNodeType(testNodeType);
        Value[] testValue = new Value[]{superuser.getValueFactory().createValue("one"), superuser.getValueFactory().createValue("two"), superuser.getValueFactory().createValue("three")};
        if (!nt.canSetProperty(propertyName2, testValue)) {
            throw new NotExecutableException("Property " + propertyName2 + " of NodeType " + testNodeType + " does not allow multi values");
        }

        Node node = testRootNode.addNode(nodeName1, testNodeType);
        node.setProperty(propertyName1, "existence");
        node.setProperty(propertyName2, testValue);

        node = testRootNode.addNode(nodeName2, testNodeType);
        node.setProperty(propertyName1, "nonexistence");
        node.setProperty(propertyName2, new String[]{"one", "three"});

        Node cNode = node.addNode(nodeName3, testNodeType);
        cNode.setProperty(propertyName1, "existence");
        testRootNode.save();
    }

    /**
     * Tests if all results contain only the searched value is contained in the
     * selected property
     *
     * @param itr           rows of the query result.
     * @param propertyName  selected property, that should contain the value.
     * @param expectedValue the value that is expected to be found
     */
    protected void checkValue(RowIterator itr,
                              String propertyName,
                              String expectedValue) throws RepositoryException {
        while (itr.hasNext()) {
            Row row = itr.nextRow();
            // check fullText
            Value value = row.getValue(propertyName);
            if (value == null) {
                fail("Search Test: fails result does not contain value for selected property");
            }
            assertEquals("Value in query result row does not match expected value",
                    expectedValue, value.getString());
        }
    }

    /**
     * Checks if all nodes in <code>itr</code> have a property with name
     * <code>propertyName</code> and have the <code>expectedValue</code>.
     *
     * @param itr           the nodes to check.
     * @param propertyName  the name of the property.
     * @param expectedValue the exected value of the property.
     * @throws RepositoryException if an error occurs.
     */
    protected void checkValue(NodeIterator itr,
                              String propertyName,
                              String expectedValue) throws RepositoryException {
        while (itr.hasNext()) {
            Node node = itr.nextNode();
            // check fullText
            Value value = node.getProperty(propertyName).getValue();
            assertEquals("Value in query result row does not match expected value",
                    expectedValue, value.getString());
        }
    }
}