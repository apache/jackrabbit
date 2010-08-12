/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.jackrabbit.core.integration;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.flat.BTreeManager;
import org.apache.jackrabbit.commons.flat.ItemSequence;
import org.apache.jackrabbit.commons.flat.NodeSequence;
import org.apache.jackrabbit.commons.flat.PropertySequence;
import org.apache.jackrabbit.commons.flat.Rank;
import org.apache.jackrabbit.commons.flat.TreeManager;
import org.apache.jackrabbit.commons.flat.TreeTraverser;
import org.apache.jackrabbit.commons.flat.TreeTraverser.ErrorHandler;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ItemSequenceTest extends AbstractJCRTest {
    private Node testNode;
    private final ErrorHandler errorHandler = new ErrorHandler() {
        public void call(Item item, RepositoryException exception) {
            fail("An exception occurred on " + item + ": " + exception);
        }
    };

    @Override
    public void setUp() throws Exception {
        super.setUp();
        testNode = testRootNode.addNode("ItemSequenceTest", NodeType.NT_UNSTRUCTURED);
        superuser.save();
    }

    public void testEmptyNodeSequence() throws RepositoryException {
        Comparator<String> order = Rank.<String>comparableComparator();
        TreeManager treeManager = new BTreeManager(testNode, 5, 10, order, true);
        NodeSequence nodes = ItemSequence.createNodeSequence(treeManager, errorHandler);

        Iterator<Node> nodeIt = nodes.iterator();
        assertTrue(nodeIt.hasNext());
        assertTrue(treeManager.isRoot(nodeIt.next()));
        assertFalse(nodeIt.hasNext());

        checkTreeProperty(testNode, 5, 10, order);
        checkOrder(nodes, order);
        assertEmpty(nodes);
    }

    public void testSingletonNodeSequence() throws RepositoryException {
        Comparator<String> order = Rank.<String>comparableComparator();
        TreeManager treeManager = new BTreeManager(testNode, 5, 10, order, true);
        NodeSequence nodes = ItemSequence.createNodeSequence(treeManager, errorHandler);

        nodes.addNode("key", NodeType.NT_UNSTRUCTURED);
        assertTrue(nodes.hasItem("key"));

        Iterator<Node> nodeIt = nodes.iterator();
        assertTrue(nodeIt.hasNext());
        assertEquals("key", nodeIt.next().getName());
        assertFalse(nodeIt.hasNext());

        checkTreeProperty(testNode, 5, 10, order);
        checkOrder(nodes, order);

        nodes.removeNode("key");
        assertEmpty(nodes);
    }

    public void testNodeSequence() throws RepositoryException, IOException {
        Comparator<String> order = Rank.<String>comparableComparator();
        TreeManager treeManager = new BTreeManager(testNode, 5, 10, order, true);
        NodeSequence nodes = ItemSequence.createNodeSequence(treeManager, errorHandler);

        List<String> words = loadWords();
        Collections.shuffle(words);

        addAll(nodes, words);
        checkTreeProperty(testNode, 5, 10, order);
        checkOrder(nodes, order);

        Collections.shuffle(words);
        checkLookup(nodes, words);

        Collections.shuffle(words);
        List<String> toRemove = take(words.size()/5, words);
        removeAll(nodes, toRemove);
        checkNotFound(nodes, toRemove);
        checkLookup(nodes, words);

        removeAll(nodes, words);
        assertEmpty(nodes);
    }

    public void testEmptyPropertySequence() throws RepositoryException {
        Comparator<String> order = Rank.<String>comparableComparator();
        TreeManager treeManager = new BTreeManager(testNode, 2, 4, order, true);
        PropertySequence properties = ItemSequence.createPropertySequence(treeManager, errorHandler);

        Iterator<Property> propertyIt = properties.iterator();
        assertFalse(propertyIt.hasNext());
        assertEmpty(properties);
    }

    public void testSingletonPropertySequence() throws RepositoryException {
        Comparator<String> order = Rank.<String>comparableComparator();
        TreeManager treeManager = new BTreeManager(testNode, 2, 4, order, true);
        PropertySequence properties = ItemSequence.createPropertySequence(treeManager, errorHandler);

        ValueFactory vFactory = testNode.getSession().getValueFactory();
        properties.addProperty("key", vFactory.createValue("key_"));
        assertTrue(properties.hasItem("key"));

        Iterator<Property> propertyIt = properties.iterator();
        assertTrue(propertyIt.hasNext());
        assertEquals("key", propertyIt.next().getName());
        assertFalse(propertyIt.hasNext());

        properties.removeProperty("key");
        assertEmpty(properties);
    }

    public void testPropertySequence() throws RepositoryException, IOException {
        Comparator<String> order = Rank.<String>comparableComparator();
        TreeManager treeManager = new BTreeManager(testNode, 2, 4, order, true);
        PropertySequence properties = ItemSequence.createPropertySequence(treeManager, errorHandler);

        List<String> words = loadWords();
        Collections.shuffle(words);

        ValueFactory vFactory = testNode.getSession().getValueFactory();
        addAll(properties, words, vFactory);
        checkTreeProperty(testNode, 2, 4, order);

        Collections.shuffle(words);
        checkLookup(properties, words);

        Collections.shuffle(words);
        List<String> toRemove = take(words.size()/5, words);
        removeAll(properties, toRemove);
        checkNotFound(properties, toRemove);
        checkLookup(properties, words);

        removeAll(properties, words);
        assertEmpty(properties);
    }

    // -----------------------------------------------------< internal >---

    private static List<String> loadWords() throws FileNotFoundException, IOException {
        InputStreamReader wordList = new InputStreamReader(ItemSequenceTest.class.getResourceAsStream("words.txt"));
        BufferedReader wordReader = new BufferedReader(wordList);

        List<String> words = new LinkedList<String>();
        String word = wordReader.readLine();
        while (word != null) {
            words.add(word);
            word = wordReader.readLine();
        }
        return words;
    }

    /**
     * Remove first <code>count</code> items from <code>list</code> and return them
     * in as a separate list.
     */
    private static <T> List<T> take(int count, List<T> list) {
        List<T> removed = new LinkedList<T>();
        for (int k = 0; k < count; k++) {
            removed.add(list.remove(0));
        }
        return removed;
    }

    private static void addAll(NodeSequence nodes, List<String> words) throws RepositoryException {
        for (String name : words) {
            nodes.addNode(name, NodeType.NT_UNSTRUCTURED);
        }
    }

    private static void addAll(PropertySequence properties, List<String> words, ValueFactory vFactory)
            throws RepositoryException {

        for (String name : words) {
            properties.addProperty(name, vFactory.createValue(name + " value"));
        }
    }

    private static void checkTreeProperty(Node root, int minChildren, int maxChildren, Comparator<String> order)
            throws RepositoryException {

        int depth = -1;

        for (Node node : new TreeTraverser(root)) {
            String parentName = node.getName();

            if (node.hasNodes()) {
                int childCount = 0;
                for (NodeIterator nodes = node.getNodes(); nodes.hasNext(); ) {
                    Node child = nodes.nextNode();
                    childCount++;
                    if (!root.isSame(node)) {
                        assertTrue("Mismatching order: node " + node + " contains child " + child,
                                order.compare(parentName, child.getName()) <= 0);
                    }
                }
                if (!root.isSame(node)) {
                    assertTrue("Node " + node + " should have at least " + minChildren + " child nodes",
                            minChildren <= childCount);
                }
                assertTrue("Node " + node + " should have no more than " + maxChildren + " child nodes",
                        maxChildren >= childCount);
            }

            else {
                if (depth == -1) {
                    depth = node.getDepth();
                }
                else {
                    assertEquals("Node " + node + " has depth " + node.getDepth() + " instead of " + depth,
                            depth, node.getDepth());
                }

                int propCount = 0;
                for (PropertyIterator properties = node.getProperties(); properties.hasNext(); ) {
                    Property property = properties.nextProperty();
                    String propertyName = property.getName();
                    if (!JcrConstants.JCR_PRIMARYTYPE.equals(propertyName)) {
                        propCount++;
                        assertTrue("Mismatching order: node " + node + " contains property " + property,
                                order.compare(parentName, propertyName) <= 0);
                    }
                }

                if (propCount > 0) {
                    assertTrue("Node " + node + " should have at least " + minChildren + " properties",
                            minChildren <= propCount);
                    assertTrue("Node" + node + " should have no more than " + maxChildren + " properties",
                            maxChildren >= propCount);
                }
            }

        }
    }

    private static void checkOrder(NodeSequence nodes, Comparator<String> order) throws RepositoryException {
        Node p = null;
        for (Node n : nodes) {
            if (p != null) {
                assertTrue("Mismatching order: node " + p + " should occur before node " + n,
                        order.compare(p.getName(), n.getName()) < 0);
            }
            p = n;
        }
    }

    private static void checkLookup(NodeSequence nodes, List<String> keys) throws RepositoryException {
        for (String key : keys) {
            assertTrue("Missing key: " + key, nodes.hasItem(key));
            Node node = nodes.getItem(key);
            assertEquals("Key " + key + " does not match name of node " + node, key, node.getName());
        }
    }

    private static void checkLookup(PropertySequence properties, List<String> keys) throws RepositoryException {
        for (String key : keys) {
            assertTrue("Missing key: " + key, properties.hasItem(key));
            Property property = properties.getItem(key);
            assertEquals("Key " + key + " does not match name of property " + property, key, property.getName());
        }
    }

    private static void checkNotFound(NodeSequence nodes, List<String> keys) throws RepositoryException {
        for (String key : keys) {
            assertFalse("NodeSequence should not contain key " + key, nodes.hasItem(key));
            try {
                nodes.getItem(key);
                fail("NodeSequence should not contain key " + key);
            }
            catch (RepositoryException expected) { }
        }
    }

    private static void checkNotFound(PropertySequence properties, List<String> keys) throws RepositoryException {
        for (String key : keys) {
            assertFalse("PropertySequence should not contain key " + key, properties.hasItem(key));
            try {
                properties.getItem(key);
                fail("PropertySequence should not contain key " + key);
            }
            catch (RepositoryException expected) { }
        }
    }


    private static void removeAll(NodeSequence nodes, List<String> words) throws RepositoryException {
        for (String name : words) {
            nodes.removeNode(name);
        }
    }

    private static void removeAll(PropertySequence properties, List<String> words) throws RepositoryException {
        for (String name : words) {
            properties.removeProperty(name);
        }
    }

    private void assertEmpty(NodeSequence nodes) throws RepositoryException {
        for (Node n : nodes) {
            if (!n.isSame(testNode)) {
                fail("NodeSqeuence should be empty but found " + n.getPath());
            }
        }
    }

    private void assertEmpty(PropertySequence properties) throws RepositoryException {
        for (Property p : properties) {
            fail("PropertySqeuence should be empty but found " + p.getPath());
        }
    }

}
