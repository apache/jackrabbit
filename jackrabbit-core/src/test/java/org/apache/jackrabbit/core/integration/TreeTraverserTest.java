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

import static org.apache.jackrabbit.spi.commons.iterator.Iterators.filterIterator;
import static org.apache.jackrabbit.spi.commons.iterator.Iterators.singleton;
import static org.apache.jackrabbit.spi.commons.iterator.Iterators.transformIterator;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.flat.TreeTraverser;
import org.apache.jackrabbit.spi.commons.iterator.Iterators;
import org.apache.jackrabbit.spi.commons.iterator.Transformer;
import org.apache.jackrabbit.test.AbstractJCRTest;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import junit.framework.AssertionFailedError;

public class TreeTraverserTest extends AbstractJCRTest {
    private Node testNode;

    private final TreeTraverser.ErrorHandler errorHandler = new TreeTraverser.ErrorHandler() {
        public void call(Item item, RepositoryException exception) {
            throw (AssertionFailedError) new AssertionFailedError().initCause(exception);
        }
    };

    private final List<String> nodes = Arrays.asList(
        "",
        "a",
        "a/b",
        "a/a",
        "a/c",
        "b",
        "c",
        "d",
        "d/a",
        "d/a/b",
        "d/a/b/c",
        "d/a/b/c/d",
        "d/a/b/c/d/e",
        "d/a/b/c/d/e/f",
        "e"
    );

    private final Set<String> properties = new HashSet<String>() {{
        add("v");
        add("w");
        add("x");
        add("a/v");
        add("a/w");
        add("a/x");
        add("a/a/a");
        add("d/a/b/c/d/e/f/q");
    }};

    private final List<String> leaves = new LinkedList<String>();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        testNode = testRootNode.addNode("TreeTraverserTest", NodeType.NT_UNSTRUCTURED);
        superuser.save();

        // Determine leave nodes
        outer:
        for (String node : nodes) {
            for (String other : nodes) {
                if (other.startsWith(node) && !other.equals(node)) {
                    continue outer;
                }
            }
            leaves.add(node);
        }
    }

    public void testTraverseNodesEmpty() throws RepositoryException {
        TreeTraverser traverser = new TreeTraverser(testNode, errorHandler, TreeTraverser.InclusionPolicy.ALL);
        checkNodes(traverser, singleton(""), testNode.getPath());
    }

    public void testTraverseLeavesEmpty() throws RepositoryException {
        TreeTraverser traverser = new TreeTraverser(testNode, errorHandler, TreeTraverser.InclusionPolicy.LEAVES);
        checkNodes(traverser, singleton(""), testNode.getPath());
    }

    public void testTraverseNodes() throws RepositoryException {
        addNodes(testNode, nodes);
        TreeTraverser traverser = new TreeTraverser(testNode, errorHandler, TreeTraverser.InclusionPolicy.ALL);
        checkNodes(traverser, nodes.iterator(), testNode.getPath());
    }

    public void testTraverseLeaves() throws RepositoryException {
        addNodes(testNode, nodes);
        TreeTraverser traverser = new TreeTraverser(testNode, errorHandler, TreeTraverser.InclusionPolicy.LEAVES);
        checkNodes(traverser, leaves.iterator(), testNode.getPath());
    }

    public void testTraversePropertiesEmpty() throws RepositoryException {
        Iterator<Node> nodeIt = TreeTraverser.nodeIterator(testNode);
        Iterator<Property> propertyIt = TreeTraverser.propertyIterator(nodeIt);
        checkProperties(propertyIt, Iterators.<String>empty(), testNode.getPath());

        addNodes(testNode, nodes);
        checkProperties(propertyIt, Iterators.<String>empty(), testNode.getPath());
    }

    public void testTraverseProperties() throws RepositoryException {
        addNodes(testNode, nodes);
        addProperties(testNode, properties);
        Iterator<Node> nodeIt = TreeTraverser.nodeIterator(testNode);
        Iterator<Property> propertyIt = TreeTraverser.propertyIterator(nodeIt);

        checkProperties(propertyIt, properties.iterator(), testNode.getPath());
    }

    // -----------------------------------------------------< internal >---

    private static void addNodes(Node root, Iterable<String> nodes) throws RepositoryException {
        for (String name : nodes) {
            if (!"".equals(name)) {
                root.addNode(name);
            }
        }
        root.getSession().save();
    }

    private static void addProperties(Node root, Iterable<String> properties) throws RepositoryException {
        for (String name : properties) {
            int i = name.lastIndexOf('/');
            Node n;
            String propName;
            if (i <= 0) {
                n = root;
                propName = name;
            }
            else {
                n = root.getNode(name.substring(0, i));
                propName = name.substring(i + 1);
            }
            n.setProperty(propName, propName + " value");
        }
        root.getSession().save();
    }

    private static void checkNodes(TreeTraverser treeTraverser, Iterator<String> expectedNodes, String pathPrefix)
            throws RepositoryException {

        for (Node node : treeTraverser) {
            assertTrue("No more nodes. Expected " + node, expectedNodes.hasNext());
            assertEquals(cat(pathPrefix, expectedNodes.next()), node.getPath());
        }

        assertFalse(expectedNodes.hasNext());
    }

    private static void checkProperties(Iterator<Property> actualProperties,
            Iterator<String> expectedProperties, String pathPrefix) {

        List<String> actualPaths = toList(property2Path(removeIgnored(actualProperties)));
        List<String> expectedPaths = toList(cat(pathPrefix, expectedProperties));

        Collections.sort(actualPaths);
        Collections.sort(expectedPaths);

        assertEquals(expectedPaths, actualPaths);
    }

    private static Iterator<Property> removeIgnored(Iterator<Property> properties) {
        return filterIterator(properties, new Predicate<Property>() {
            public boolean test(Property property) {
                try {
                    return !JcrConstants.JCR_PRIMARYTYPE.equals(property.getName());
                }
                catch (RepositoryException e) {
                    throw (AssertionFailedError) new AssertionFailedError().initCause(e);
                }
            }
        });
    }

    private static Iterator<String> property2Path(Iterator<Property> properties) {
        return transformIterator(properties, new Transformer<Property, String>() {
            public String transform(Property property) {
                try {
                    return property.getPath();
                }
                catch (RepositoryException e) {
                    throw (AssertionFailedError) new AssertionFailedError().initCause(e);
                }
            }
        });
    }

    private static <T> List<T> toList(Iterator<T> iterator) {
        List<T> list = new LinkedList<T>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }

    private static String cat(String s1, String s2) {
        if ("".equals(s2)) {
            return s1;
        }
        else {
            return s1 + "/" + s2;
        }
    }

    private static Iterator<String> cat(final String s, Iterator<String> strings) {
        return transformIterator(strings, new Transformer<String, String>() {
            public String transform(String string) {
                return cat(s, string);
            }
        });
    }

}
