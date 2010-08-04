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
package org.apache.jackrabbit.flat;

import static org.apache.jackrabbit.commons.iterator.Iterators.empty;
import static org.apache.jackrabbit.commons.iterator.Iterators.iteratorChain;
import static org.apache.jackrabbit.commons.iterator.Iterators.properties;
import static org.apache.jackrabbit.commons.iterator.Iterators.singleton;
import static org.apache.jackrabbit.commons.iterator.LazyIteratorChain.chain;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import java.util.Iterator;

/**
 * <p>
 * Utility class for traversing the {@link Item}s of a JCR hierarchy rooted at a
 * specific {@link Node}.
 * </p>
 *
 * <p>
 * This class provides an {@link Iterator} of JCR items either through its
 * implementation of {@link Iterable} or through various static factory methods.
 * The iterators return its elements in pre-order. That is, each node occurs
 * before its child nodes are traversed. The order in which child nodes are
 * traversed is determined by the underlying JCR implementation. Generally the
 * order is not specified unless a {@link Node} has orderable child nodes.
 * </p>
 *
 * <p>
 * Whether a specific node is included is determined by an
 * {@link #InclusionPolicy}. Error occurring while traversing are delegated to
 * an {@link #ErrorHandler}.
 * </p>
 */
public final class TreeTraverser implements Iterable<Node> {
    private final Node root;
    private final ErrorHandler errorHandler;
    private final InclusionPolicy inclusionPolicy;

    /**
     * Create a new instance of a TreeTraverser rooted at <code>node</code>.
     *
     * @param root The root node of the sub-tree to traverse
     * @param errorHandler Handler for errors while traversing
     * @param inclusionPolicy Inclusion policy to determine which nodes to
     *            include
     */
    public TreeTraverser(Node root, ErrorHandler errorHandler, InclusionPolicy inclusionPolicy) {
        super();
        this.root = root;
        this.errorHandler = errorHandler == null? ErrorHandler.IGNORE : errorHandler;
        this.inclusionPolicy = inclusionPolicy;
    }

    /**
     * Create a new instance of a TreeTraverser rooted at <code>node</code>.
     *
     * @param root The root node of the sub-tree to traverse
     */
    public TreeTraverser(Node root) {
        this(root, ErrorHandler.IGNORE, InclusionPolicy.ALL);
    }

    /**
     * Error handler for handling {@link RepositoryException}s occurring on
     * traversal. The predefined {@link #IGNORE} error handler can be used to
     * ignore all exceptions.
     */
    public interface ErrorHandler {

        /**
         * Predefined error handler which ignores all exceptions.
         */
        public static ErrorHandler IGNORE = new ErrorHandler() {
            public void call(Item item, RepositoryException exception) { /* ignore */ }
        };

        /**
         * This call back method is called whenever an error occurs while
         * traversing.
         *
         * @param item The item which was the target of an operation which
         *            failed and caused the exception.
         * @param exception The exception which occurred.
         */
        void call(Item item, RepositoryException exception);
    }

    /**
     * Inclusion policy to determine which nodes to include when traversing.
     * There a two predefined inclusion policies:
     * <ul>
     * <li>{@link #ALL} includes all nodes.</li>
     * <li>{@link #LEAVES} includes only leave nodes. A leaf node is a node
     * which does not have childe nodes.</li>
     * </ul>
     */
    public interface InclusionPolicy {

        /**
         * This inclusions policy includes all nodes.
         */
        public static InclusionPolicy ALL = new InclusionPolicy() {
            public boolean include(Node node) {
                return true;
            }
        };

        /**
         * This inclusion policy only includes leave nodes. A leaf node is a
         * node which does not have child nodes.
         */
        public static InclusionPolicy LEAVES = new InclusionPolicy() {
            public boolean include(Node node) throws RepositoryException {
                return !node.hasNodes();
            }
        };

        /**
         * Call back method to determine whether to include a given node.
         *
         * @param node The node under consideration
         * @return <code>true</code> when <code>node</code> should be included.
         *         <code>false</code> otherwise.
         *
         * @throws RepositoryException
         */
        boolean include(Node node) throws RepositoryException;
    }

    /**
     * Create an iterator for the nodes of the sub-tree rooted at
     * <code>root</code>.
     *
     * @param root root node of the sub-tree to traverse
     * @param errorHandler handler for exceptions occurring on traversal
     * @param inclusionPolicy inclusion policy to determine which nodes to
     *            include
     * @return iterator of {@link Node}
     */
    public static Iterator<Node> nodeIterator(Node root, ErrorHandler errorHandler,
            InclusionPolicy inclusionPolicy) {

        return new TreeTraverser(root, errorHandler, inclusionPolicy).iterator();
    }

    /**
     * Create an iterator for the nodes of the sub-tree rooted at
     * <code>root</code>. Exceptions occurring on traversal are ignored.
     *
     * @param root root node of the sub-tree to traverse
     * @return iterator of {@link Node}
     */
    public static Iterator<Node> nodeIterator(Node root) {
        return nodeIterator(root, ErrorHandler.IGNORE, InclusionPolicy.ALL);
    }

    /**
     * Create an iterator of the properties for a given iterator of nodes. The
     * order of the returned properties is only specified so far that if node
     * <code>n1</code> occurs before node <code>n2</code> in the iterator of
     * nodes, then any property of <code>n1</code> will occur before any
     * property of <code>n2</code>.
     *
     * @param nodes nodes whose properties to chain
     * @param errorHandler handler for exceptions occurring on traversal
     * @return iterator of {@link Property}
     */
    public static Iterator<Property> propertyIterator(Iterator<Node> nodes, ErrorHandler errorHandler) {
        return chain(propertyIterators(nodes, errorHandler));
    }

    /**
     * Create an iterator of the properties for a given iterator of nodes. The
     * order of the returned properties is only specified so far that if node
     * <code>n1</code> occurs before node <code>n2</code> in the iterator of
     * nodes, then any property of <code>n1</code> will occur before any
     * property of <code>n2</code>. Exceptions occurring on traversal are
     * ignored.
     *
     * @param nodes nodes whose properties to chain
     * @return iterator of {@link Property}
     */
    public static Iterator<Property> propertyIterator(Iterator<Node> nodes) {
        return propertyIterator(nodes, ErrorHandler.IGNORE);
    }

    /**
     * Create an iterator of the properties of all nodes of the sub-tree rooted
     * at <code>root</code>.
     *
     * @param root root node of the sub-tree to traverse
     * @param errorHandler handler for exceptions occurring on traversal
     * @param inclusionPolicy inclusion policy to determine which nodes to
     *            include
     * @return iterator of {@link Property}
     */
    public static Iterator<Property> propertyIterator(Node root, ErrorHandler errorHandler,
            InclusionPolicy inclusionPolicy) {

        return propertyIterator(nodeIterator(root, errorHandler, inclusionPolicy), errorHandler);
    }

    /**
     * Create an iterator of the properties of all nodes of the sub-tree rooted
     * at <code>root</code>. Exceptions occurring on traversal are ignored.
     *
     * @param root root node of the sub-tree to traverse
     * @return iterator of {@link Property}
     */
    public static Iterator<Property> propertyIterator(Node root) {
        return propertyIterator(root, ErrorHandler.IGNORE, InclusionPolicy.ALL);
    }

    /**
     * Returns an iterator of {@link Node} for this instance.
     *
     * @see TreeTraverser#TreeTraverser(Node, ErrorHandler, InclusionPolicy)
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Node> iterator() {
        return iterator(root);
    }

    // -----------------------------------------------------< internal >---

    /**
     * Returns an iterator of the nodes of the sub-tree rooted at
     * <code>node</code>.
     */
    private Iterator<Node> iterator(Node node) {
        try {
            if (inclusionPolicy.include(node)) {
                return iteratorChain(singleton(node), chain(childIterators(node)));
            }
            else {
                return chain(childIterators(node));
            }
        }
        catch (RepositoryException e) {
            errorHandler.call(node, e);
            return empty();
        }
    }

    /**
     * Returns an iterator of iterators of the child nodes of <code>node</code>.
     */
    private Iterator<Iterator<Node>> childIterators(final Node node) {
        try {
            return new Iterator<Iterator<Node>>() {
                private final NodeIterator childNodes = node.getNodes();

                public boolean hasNext() {
                    return childNodes.hasNext();
                }

                public Iterator<Node> next() {
                    return iterator(childNodes.nextNode());
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
        catch (RepositoryException e) {
            errorHandler.call(node, e);
            return empty();
        }
    }

    /**
     * Returns an iterator of all properties of all <code>nodes</code>. For node
     * <code>n1</code> occurring before node <code>n2</code> in
     * <code>nodes</code>, any property of <code>n1</code> will occur before any
     * property of <code>n2</code> in the iterator.
     */
    private static Iterator<Iterator<Property>> propertyIterators(final Iterator<Node> nodes,
            final ErrorHandler errorHandler) {

        return new Iterator<Iterator<Property>>() {
            public boolean hasNext() {
                return nodes.hasNext();
            }

            public Iterator<Property> next() {
                Node n = nodes.next();
                try {
                    return properties(n.getProperties());
                }
                catch (RepositoryException e) {
                    errorHandler.call(n, e);
                    return empty();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

}
