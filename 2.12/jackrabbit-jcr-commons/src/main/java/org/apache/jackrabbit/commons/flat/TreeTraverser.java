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
package org.apache.jackrabbit.commons.flat;

import static org.apache.jackrabbit.commons.iterator.LazyIteratorChain.chain;

import org.apache.jackrabbit.commons.iterator.FilterIterator;
import org.apache.jackrabbit.commons.predicate.Predicate;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import java.util.Collections;
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
 * {@link #inclusionPolicy}. Error occurring while traversing are delegated to
 * an {@link #errorHandler}.
 * </p>
 */
public final class TreeTraverser implements Iterable<Node> {
    private final Node root;
    private final ErrorHandler errorHandler;
    private final InclusionPolicy<? super Node> inclusionPolicy;

    /**
     * Create a new instance of a TreeTraverser rooted at <code>node</code>.
     *
     * @param root The root node of the sub-tree to traverse
     * @param errorHandler Handler for errors while traversing
     * @param inclusionPolicy Inclusion policy to determine which nodes to
     *            include
     */
    public TreeTraverser(Node root, ErrorHandler errorHandler, InclusionPolicy<? super Node> inclusionPolicy) {
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
     * Inclusion policy to determine which items to include when traversing.
     * There a two predefined inclusion policies:
     * <ul>
     * <li>{@link #ALL} includes all items.</li>
     * <li>{@link #LEAVES} includes only leave nodes. A leaf node is a node
     * which does not have child nodes.</li>
     * </ul>
     */
    public interface InclusionPolicy<T extends Item> {

        /**
         * This inclusions policy includes all items.
         */
        public static InclusionPolicy<Item> ALL = new InclusionPolicy<Item>() {
            public boolean include(Item item) {
                return true;
            }
        };

        /**
         * This inclusion policy includes leave nodes only. A leaf
         * node is a node which does not have child nodes.
         */
        public static InclusionPolicy<Node> LEAVES = new InclusionPolicy<Node>() {
            public boolean include(Node node) {
                try {
                    return !node.hasNodes();
                }
                catch (RepositoryException e) {
                    return false;
                }
            }
        };

        /**
         * Call back method to determine whether to include a given item.
         *
         * @param item The item under consideration
         * @return <code>true</code> when <code>item</code> should be included.
         *         <code>false</code> otherwise.
         */
        boolean include(T item);
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
            InclusionPolicy<? super Node> inclusionPolicy) {

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
     * @param inclusionPolicy inclusion policy to determine properties items to include
     *
     * @return iterator of {@link Property}
     */
    public static Iterator<Property> propertyIterator(Iterator<Node> nodes, ErrorHandler errorHandler,
            InclusionPolicy<? super Property> inclusionPolicy) {

        return filter(chain(propertyIterators(nodes, errorHandler)), inclusionPolicy);
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
        return propertyIterator(nodes, ErrorHandler.IGNORE, InclusionPolicy.ALL);
    }

    /**
     * Create an iterator of the properties of all nodes of the sub-tree rooted
     * at <code>root</code>.
     *
     * @param root root node of the sub-tree to traverse
     * @param errorHandler handler for exceptions occurring on traversal
     * @param inclusionPolicy inclusion policy to determine which items to
     *            include
     * @return iterator of {@link Property}
     */
    public static Iterator<Property> propertyIterator(Node root, ErrorHandler errorHandler,
            InclusionPolicy<Item> inclusionPolicy) {

        return propertyIterator(nodeIterator(root, errorHandler, inclusionPolicy), errorHandler,
                inclusionPolicy);
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
    @SuppressWarnings("unchecked")
    private Iterator<Node> iterator(Node node) {
        if (inclusionPolicy.include(node)) {
            return chain(singleton(node), chain(childIterators(node)));
        }
        else {
            return chain(childIterators(node));
        }
    }

    /**
     * Returns an iterator of iterators of the child nodes of <code>node</code>.
     */
    private Iterator<Iterator<Node>> childIterators(Node node) {
        try {
            final NodeIterator childNodes = node.getNodes();
            return new Iterator<Iterator<Node>>() {
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
        } catch (RepositoryException e) {
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

            @SuppressWarnings("unchecked")
            public Iterator<Property> next() {
                Node n = nodes.next();
                try {
                    return n.getProperties();
                } catch (RepositoryException e) {
                    errorHandler.call(n, e);
                    return empty();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    // -----------------------------------------------------< utility >---

    private static <T> Iterator<T> empty() {
        return Collections.<T>emptySet().iterator();
    }

    private <T> Iterator<T> singleton(T value) {
        return Collections.singleton(value).iterator();
    }

    /**
     * Filtering items not matching the <code>inclusionPolicy</code> from
     * <code>iterator</code>.
     */
    private static <T extends Item> Iterator<T> filter(final Iterator<T> iterator,
            final InclusionPolicy<? super T> inclusionPolicy) {

        return new FilterIterator<T>(iterator, new Predicate() {
            @SuppressWarnings("unchecked")
            public boolean evaluate(Object object) {
                return inclusionPolicy.include((T) object);
            }
        });
    }

}
