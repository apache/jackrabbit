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
import javax.jcr.RepositoryException;

/**
 * <code>ChildAxisQueryTest</code> tests queries with a child axis in their
 * predicates.
 */
public class ChildAxisQueryTest extends AbstractQueryTest {

    /**
     * Predicate with child node axis in a relation
     */
    public void testRelationQuery() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1);
        n1.setProperty(propertyName1, 1);
        Node n2 = testRootNode.addNode(nodeName1);
        n2.setProperty(propertyName1, 2);
        Node n3 = testRootNode.addNode(nodeName1);
        n3.setProperty(propertyName1, 3);

        testRootNode.save();

        String base = testPath + "[" + nodeName1 + "/@" + propertyName1;
        executeXPathQuery(base + " = 1]", new Node[]{testRootNode});
        executeXPathQuery(base + " = 2]", new Node[]{testRootNode});
        executeXPathQuery(base + " = 3]", new Node[]{testRootNode});
        executeXPathQuery(base + " = 4]", new Node[]{});
        executeXPathQuery(base + " > 0]", new Node[]{testRootNode});
        executeXPathQuery(base + " > 1]", new Node[]{testRootNode});
        executeXPathQuery(base + " > 2]", new Node[]{testRootNode});
        executeXPathQuery(base + " > 3]", new Node[]{});
        executeXPathQuery(base + " >= 1]", new Node[]{testRootNode});
        executeXPathQuery(base + " >= 2]", new Node[]{testRootNode});
        executeXPathQuery(base + " >= 3]", new Node[]{testRootNode});
        executeXPathQuery(base + " >= 4]", new Node[]{});
        executeXPathQuery(base + " < 1]", new Node[]{});
        executeXPathQuery(base + " < 2]", new Node[]{testRootNode});
        executeXPathQuery(base + " < 3]", new Node[]{testRootNode});
        executeXPathQuery(base + " < 4]", new Node[]{testRootNode});
        executeXPathQuery(base + " <= 0]", new Node[]{});
        executeXPathQuery(base + " <= 1]", new Node[]{testRootNode});
        executeXPathQuery(base + " <= 2]", new Node[]{testRootNode});
        executeXPathQuery(base + " <= 3]", new Node[]{testRootNode});
        executeXPathQuery(base + " != 0]", new Node[]{testRootNode});
        executeXPathQuery(base + " != 1]", new Node[]{testRootNode});
        executeXPathQuery(base + " != 2]", new Node[]{testRootNode});
        executeXPathQuery(base + " != 3]", new Node[]{testRootNode});
    }

    public void testRelationQueryDeep() throws RepositoryException {
        Node n = testRootNode.addNode(nodeName1).addNode(nodeName2);
        Node n1 = n.addNode(nodeName3);
        n1.setProperty(propertyName1, 1);
        Node n2 = n.addNode(nodeName3);
        n2.setProperty(propertyName1, 2);
        Node n3 = n.addNode(nodeName3);
        n3.setProperty(propertyName1, 3);

        testRootNode.save();

        String base = testPath + "[" + nodeName1 + "/" + nodeName2 + "/" +
                nodeName3 + "/@" + propertyName1;
        executeXPathQuery(base + " = 1]", new Node[]{testRootNode});
        executeXPathQuery(base + " = 2]", new Node[]{testRootNode});
        executeXPathQuery(base + " = 3]", new Node[]{testRootNode});
        executeXPathQuery(base + " = 4]", new Node[]{});
        executeXPathQuery(base + " > 0]", new Node[]{testRootNode});
        executeXPathQuery(base + " > 1]", new Node[]{testRootNode});
        executeXPathQuery(base + " > 2]", new Node[]{testRootNode});
        executeXPathQuery(base + " > 3]", new Node[]{});
        executeXPathQuery(base + " >= 1]", new Node[]{testRootNode});
        executeXPathQuery(base + " >= 2]", new Node[]{testRootNode});
        executeXPathQuery(base + " >= 3]", new Node[]{testRootNode});
        executeXPathQuery(base + " >= 4]", new Node[]{});
        executeXPathQuery(base + " < 1]", new Node[]{});
        executeXPathQuery(base + " < 2]", new Node[]{testRootNode});
        executeXPathQuery(base + " < 3]", new Node[]{testRootNode});
        executeXPathQuery(base + " < 4]", new Node[]{testRootNode});
        executeXPathQuery(base + " <= 0]", new Node[]{});
        executeXPathQuery(base + " <= 1]", new Node[]{testRootNode});
        executeXPathQuery(base + " <= 2]", new Node[]{testRootNode});
        executeXPathQuery(base + " <= 3]", new Node[]{testRootNode});
        executeXPathQuery(base + " != 0]", new Node[]{testRootNode});
        executeXPathQuery(base + " != 1]", new Node[]{testRootNode});
        executeXPathQuery(base + " != 2]", new Node[]{testRootNode});
        executeXPathQuery(base + " != 3]", new Node[]{testRootNode});
    }

    public void testMultiRelation() throws RepositoryException {
        Node level1 = testRootNode.addNode(nodeName1);
        level1.setProperty(propertyName1, "foo");
        Node level2 = level1.addNode(nodeName2);
        level2.setProperty(propertyName1, "bar");
        Node n1 = level2.addNode(nodeName3);
        n1.setProperty(propertyName2, 1);
        Node n2 = level2.addNode(nodeName3);
        n2.setProperty(propertyName2, 2);
        Node n3 = level2.addNode(nodeName3);
        n3.setProperty(propertyName2, 3);

        testRootNode.save();

        String base = testPath + "[" + nodeName1 + "/" + nodeName2 + "/" +
                nodeName3 + "/@" + propertyName2;
        executeXPathQuery(base + " = 1]", new Node[]{testRootNode});
        executeXPathQuery(base + " = 1 and " + nodeName1 + "/@" +
                propertyName1 + " = 'foo' and " + nodeName1 + "/" + nodeName2 +
                "/@" + propertyName1 + " = 'bar']", new Node[]{testRootNode});
        executeXPathQuery(base + " = 1 and " + nodeName1 + "/@" +
                propertyName1 + " = 'foo' and " + nodeName1 + "/" + nodeName2 +
                "/@" + propertyName1 + " = 'bar']", new Node[]{testRootNode});
        executeXPathQuery(base + " = 1 and " + nodeName1 + "/@" +
                propertyName1 + " = 'foo' and " + nodeName2 +
                "/@" + propertyName1 + " = 'bar']", new Node[]{});
    }

    public void testLike() throws RepositoryException {
        Node n1 = testRootNode.addNode(nodeName1);
        n1.setProperty(propertyName1, "foo");
        Node n2 = testRootNode.addNode(nodeName1);
        n2.setProperty(propertyName1, "foobar");
        Node n3 = testRootNode.addNode(nodeName1);
        n3.setProperty(propertyName1, "foo bar");

        testRootNode.save();

        String base = testPath + "[jcr:like(" + nodeName1 + "/@" + propertyName1;
        executeXPathQuery(base + ", 'fo_')]", new Node[]{testRootNode});
        executeXPathQuery(base + ", 'foo_ar')]", new Node[]{testRootNode});
        executeXPathQuery(base + ", 'foo %')]", new Node[]{testRootNode});
        executeXPathQuery(base + ", 'f_oba')]", new Node[]{});
    }

    public void testContains() throws RepositoryException {
        Node level1 = testRootNode.addNode(nodeName1);
        level1.setProperty(propertyName1, "The quick brown fox jumps over the lazy dog.");
        Node level2 = level1.addNode(nodeName2);
        level2.setProperty(propertyName1, "Franz jagt im total verwahrlosten Taxi quer durch Bayern.");
        Node n1 = level2.addNode(nodeName3);
        n1.setProperty(propertyName2, 1);
        Node n2 = level2.addNode(nodeName3);
        n2.setProperty(propertyName2, 2);
        Node n3 = level2.addNode(nodeName3);
        n3.setProperty(propertyName2, 3);

        testRootNode.save();

        String base = testPath + "[jcr:contains(";
        executeXPathQuery(base + nodeName1 + "/@" + propertyName1 + ", 'lazy')" +
                " and " + nodeName1 + "/" + nodeName2 + "/" + nodeName3 + "/@" + propertyName2 + " = 2]",
                new Node[]{testRootNode});
        executeXPathQuery(base + nodeName1 + "/" + nodeName2 + "/@" + propertyName1 + ", 'franz')" +
                " and " + nodeName1 + "/" + nodeName2 + "/" + nodeName3 + "/@" + propertyName2 + " = 3]",
                new Node[]{testRootNode});
        executeXPathQuery(base + nodeName1 + ", 'lazy')" +
                " and " + nodeName1 + "/" + nodeName2 + "/" + nodeName3 + "/@" + propertyName2 + " = 1]",
                new Node[]{testRootNode});
        executeXPathQuery(base + nodeName1 + "/" + nodeName2 + ", 'franz')" +
                " and " + nodeName1 + "/" + nodeName2 + "/" + nodeName3 + "/@" + propertyName2 + " = 1]",
                new Node[]{testRootNode});
    }

    public void testStarNameTest() throws RepositoryException {
        Node level1 = testRootNode.addNode(nodeName1);
        level1.setProperty(propertyName1, "The quick brown fox jumps over the lazy dog.");
        Node level2 = level1.addNode(nodeName2);
        level2.setProperty(propertyName1, "Franz jagt im total verwahrlosten Taxi quer durch Bayern.");
        Node n1 = level2.addNode(nodeName3);
        n1.setProperty(propertyName2, 1);
        Node n2 = level2.addNode(nodeName3);
        n2.setProperty(propertyName2, 2);
        Node n3 = level2.addNode(nodeName4);
        n3.setProperty(propertyName2, 3);

        testRootNode.save();

        String base = testPath + "[jcr:contains(";
        executeXPathQuery(base + nodeName1 + "/@" + propertyName1 + ", 'lazy')" +
                " and " + nodeName1 + "/" + nodeName2 + "/" + nodeName3 + "/@" + propertyName2 + " = 3]",
                new Node[]{});
        executeXPathQuery(base + nodeName1 + "/@" + propertyName1 + ", 'lazy')" +
                " and " + nodeName1 + "/" + nodeName2 + "/*/@" + propertyName2 + " = 3]",
                new Node[]{testRootNode});

        executeXPathQuery(base + "*/@" + propertyName1 + ", 'lazy')]",
                new Node[]{testRootNode});
        executeXPathQuery(base + nodeName1 + "/*, 'franz')]",
                new Node[]{testRootNode});
        executeXPathQuery(base + "*/*, 'franz')]",
                new Node[]{testRootNode});
        executeXPathQuery(base + "*/*, 'lazy')]",
                new Node[]{});
    }

    public void testSimpleQuery() throws Exception {
        Node foo = testRootNode.addNode("foo");
        testRootNode.addNode("bar");

        testRootNode.save();

        String sql = "SELECT * FROM nt:unstructured WHERE jcr:path LIKE '"+testRoot+"/foo'";
        executeSQLQuery(sql, new Node[] {foo});
    }

    /**
     * JCR-3337
     */
    public void testNotIsDescendantNodeQuery() throws Exception {
        Node foo = testRootNode.addNode("foo");
        testRootNode.getSession().save();
        String sql = "SELECT a.* FROM [nt:base] as a WHERE isdescendantnode(a,'"
                + testRootNode.getPath()
                + "') and not isdescendantnode(a,'"
                + foo.getPath() + "')";
        executeSQL2Query(sql, new Node[] {foo});
    }
}
