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

import org.apache.jackrabbit.commons.flat.TreeTraverser.ErrorHandler;
import org.apache.jackrabbit.commons.flat.TreeTraverser.InclusionPolicy;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

/**
 * <p>
 * This class serves as main entry point for obtaining sequences of {@link Node}
 * s and {@link Property}s. It provides factory methods for creating
 * {@link NodeSequence}s and {@link PropertySequence}s.
 * </p>
 *
 * <p>
 * NodeSequence and PropertySequence instances provide a flat representation of
 * a JCR hierarchy rooted at a certain node. They allow iterating over all
 * items, retrieving items by key, checking whether a given key is mapped,
 * adding new items and removing existing items.
 * </p>
 *
 * <p>
 * The specifics of the mapping from the flat representation to the JCR
 * hierarchy are delegated to a {@link TreeManager}. Particularly the
 * TreeManager specifies the order of the items when retrieved as sequence and
 * when and how to add and remove intermediate nodes when new items are inserted
 * or removed.
 * </p>
 *
 * <p>
 * An {@link ErrorHandler} is used to handle exceptions which occur while
 * traversing the hierarchy.
 * </p>
 *
 * @see TreeTraverser
 * @see NodeSequence
 * @see PropertySequence
 * @see TreeManager
 */
public abstract class ItemSequence {

    /**
     * The {@link TreeManager} instance managing the mapping between the
     * sequence view and the JCR hierarchy.
     */
    protected final TreeManager treeManager;

    /**
     * The {@link ErrorHandler} instance used for handling exceptions occurring
     * while traversing the hierarchy.
     */
    protected final ErrorHandler errorHandler;

    /**
     * @see TreeManager#getRoot()
     */
    protected final Node root;

    /**
     * @see TreeManager#getOrder()
     */
    protected final Comparator<String> order;

    /**
     * @see TreeManager#getAutoSave()
     */
    protected final boolean autoSave;

    /**
     * Create a new {@link ItemSequence} instance.
     *
     * @param treeManager The {@link TreeManager} for managing the mapping
     *            between the sequence view and the JCR hierarchy.
     * @param errorHandler The {@link ErrorHandler} for handling exceptions
     *            occurring while traversing the hierarchy.
     * @throws IllegalArgumentException If either <code>treeManager</code> is
     *             <code>null</code> or {@link TreeManager#getRoot()} return
     *             <code>null</code> or {@link TreeManager#getOrder()} return
     *             <code>null</code>.
     */
    protected ItemSequence(TreeManager treeManager, ErrorHandler errorHandler) {
        super();

        if (treeManager == null) {
            throw new IllegalArgumentException("tree manager must not be null");
        }
        if (treeManager.getRoot() == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        if (treeManager.getOrder() == null) {
            throw new IllegalArgumentException("order must not be null");
        }

        this.treeManager = treeManager;
        this.errorHandler = errorHandler;
        this.root = treeManager.getRoot();
        this.order = treeManager.getOrder();
        this.autoSave = treeManager.getAutoSave();
    }

    /**
     * Create a new {@link NodeSequence} instance.
     *
     * @param treeManager The {@link TreeManager} for managing the mapping
     *            between the sequence view and the JCR hierarchy.
     * @param errorHandler The {@link ErrorHandler} for handling exceptions
     *            occurring while
     * @return
     */
    public static NodeSequence createNodeSequence(TreeManager treeManager, ErrorHandler errorHandler) {
        return new NodeSequenceImpl(treeManager, errorHandler);
    }

    /**
     * Create a new {@link NodeSequence} instance.
     *
     * @param treeManager The {@link TreeManager} for managing the mapping
     *            between the sequence view and the JCR hierarchy.
     * @return
     */
    public static NodeSequence createNodeSequence(TreeManager treeManager) {
        return new NodeSequenceImpl(treeManager, ErrorHandler.IGNORE);
    }

    /**
     * Create a new {@link PropertySequence} instance.
     *
     * @param treeManager The {@link TreeManager} for managing the mapping
     *            between the sequence view and the JCR hierarchy.
     * @param errorHandler The {@link ErrorHandler} for handling exceptions
     *            occurring while
     * @return
     */
    public static PropertySequence createPropertySequence(TreeManager treeManager, ErrorHandler errorHandler) {
        return new PropertySequenceImpl(treeManager, errorHandler);
    }

    /**
     * Create a new {@link PropertySequence} instance.
     *
     * @param treeManager The {@link TreeManager} for managing the mapping
     *            between the sequence view and the JCR hierarchy.
     * @return
     */
    public static PropertySequence createPropertySequence(TreeManager treeManager) {
        return new PropertySequenceImpl(treeManager, ErrorHandler.IGNORE);
    }

    /**
     * Create a new {@link NodeSequence} instance with the same parameterization
     * as this instance.
     *
     * @return
     */
    public NodeSequence getNodeSequence() {
        return new NodeSequenceImpl(treeManager, errorHandler);
    }

    /**
     * Create a new {@link PropertySequence} instance with the same
     * parametrization as this instance.
     *
     * @return
     */
    public PropertySequence getPropertySequence() {
        return new PropertySequenceImpl(treeManager, errorHandler);
    }

    // -----------------------------------------------------< internal >---

    /**
     * Returns the parent node for the given key. When the key is not present in
     * this sequence already, the returned node is the node that would contain
     * that key if it where present.
     */
    protected abstract Node getParent(String key) throws RepositoryException;

    /**
     * Returns the predecessor node for the given
     * <code>key</code>. That is the node
     * whose key directly precedes the passed <code>key</code> in the order
     * determined by {@link TreeManager#getOrder()}. There are two cases:
     * <ul>
     * <li>A node with the given <code>key</code> is mapped: then that node is
     * returned.</li>
     * <li>A node with the given <code>key</code> is not mapped: the the node
     * where that would contain that key if present is returned.</li>
     * </ul>
     */
    protected final Node getPredecessor(String key) throws RepositoryException {
        Node p = root;
        Node n;
        while ((n = getPredecessor(p, key)) != null) {
            p = n;
        }

        return p;
    }

    /**
     * Returns the direct predecessor of <code>key</code> amongst
     * <code>node</code>'s child nodes wrt. to {@link TreeManager#getOrder()}.
     * Returns <code>null</code> if either <code>node</code> has no child nodes
     * or <code>node</code> is a leaf (see {@link TreeManager#isLeaf(Node)}) or
     * <code>key</code> is smaller than all the keys of all child nodes of
     * <code>node</code>.
     */
    protected final Node getPredecessor(Node node, String key) throws RepositoryException {
        if (!node.hasNodes() || treeManager.isLeaf(node)) {
            return null;
        }

        // Shortcut for exact match
        try {
            return node.getNode(key);
        }
        catch (PathNotFoundException ignore) { }

        // Search for direct predecessor of key in the nodes children
        // todo performance: for ordered nodes use binary search
        NodeIterator childNodes = node.getNodes();
        Node p = null;
        while (childNodes.hasNext()) {
            Node n = childNodes.nextNode();
            String childKey = n.getName();
            if (order.compare(key, childKey) > 0 && (p == null || order.compare(childKey, p.getName()) > 0)) {
                p = n;
            }
        }

        return p;
    }

    /**
     * Returns the successor node for the given
     * <code>key</code>. That is the node
     * whose key directly succeeds the passed <code>key</code> in the order
     * determined by {@link TreeManager#getOrder()}. There are two cases:
     * <ul>
     * <li>A node with the given <code>key</code> is mapped: then that node is
     * returned.</li>
     * <li>A node with the given <code>key</code> is not mapped: the the node
     * where that would contain that key if present is returned.</li>
     * </ul>
     */
    protected final Node getSuccessor(Node node, String key) throws RepositoryException {
        if (!node.hasNodes() || treeManager.isLeaf(node)) {
            return null;
        }

        // Shortcut for exact match
        try {
            return node.getNode(key);
        }
        catch (PathNotFoundException ignore) { }

        // Search for direct successor of key in the nodes children
        // todo performance: for ordered nodes use binary search
        NodeIterator childNodes = node.getNodes();
        Node s = null;
        while (childNodes.hasNext()) {
            Node n = childNodes.nextNode();
            String childKey = n.getName();
            if (order.compare(key, childKey) < 0 && (s == null || order.compare(childKey, s.getName()) < 0)) {
                s = n;
            }
        }

        return s;
    }

    /**
     * Returns the node with the minimal key wrt. {@link TreeManager#getOrder()}.
     * For the empty sequence this is {@link TreeManager#getRoot()}.
     */
    protected final Node getMinimal() throws RepositoryException {
        Node p = null;
        Node n = root;
        while ((n = getMinimal(n)) != null) {
            p = n;
        }

        return p;
    }

    /**
     * Returns the node amongst the child nodes of <code>node</code> whose key
     * is minimal wrt. {@link TreeManager#getOrder()}. Returns <code>null</code>
     * id either <code>node</code> has no child nodes or <code>node</code> is a
     * leaf (see {@link TreeManager#isLeaf(Node)}).
     */
    protected final Node getMinimal(Node node) throws RepositoryException {
        if (!node.hasNodes() || treeManager.isLeaf(node)) {
            return null;
        }

        // Search for minimal key in the nodes children
        // todo performance: for ordered nodes use binary search
        NodeIterator childNodes = node.getNodes();
        Node p = childNodes.nextNode();
        String minKey = p.getName();
        while (childNodes.hasNext()) {
            Node n = childNodes.nextNode();
            if (order.compare(n.getName(), minKey) < 0) {
                p = n;
                minKey = p.getName();
            }
        }

        return p;
    }

    /**
     * Rename the path of the node with the minimal key. That is, assuming
     * <code>node</code> is the node with the minimal key (see
     * {@link #getMinimal()}), this method renames every segment of the path of
     * <code>node</code> up to {@link TreeManager#getRoot()} to <code>key</code>
     * . <em>Note: </em> If <code>node</code> is not the node with the minimal
     * key, the behavior of this method is not specified.
     */
    protected final void renamePath(Node node, String key) throws RepositoryException {
        if (!treeManager.isRoot(node)) {
            Node p = node.getParent();
            renamePath(p, key);
            Session s = node.getSession();
            s.move(node.getPath(), p.getPath() + "/" + key);

            // If orderable, move to first child node
            if (p.getPrimaryNodeType().hasOrderableChildNodes()) {
                p.orderBefore(key, p.getNodes().nextNode().getName());
            }
        }
    }

    protected static class NodeSequenceImpl extends ItemSequence implements NodeSequence {
        private final InclusionPolicy<Node> inclusionPolicy = new InclusionPolicy<Node>() {
            public boolean include(Node node) {
                try {
                    return treeManager.isLeaf(node);
                }
                catch (RepositoryException e) {
                    return false;
                }
            }
        };

        public NodeSequenceImpl(TreeManager treeManager, ErrorHandler errorHandler)  {
            super(treeManager, errorHandler);
        }

        public Iterator<Node> iterator() {
            return TreeTraverser.nodeIterator(root, errorHandler, inclusionPolicy);
        }

        public Node getItem(String key) throws RepositoryException {
            return getParent(key).getNode(key);
        }

        public boolean hasItem(String key) throws RepositoryException {
            return getParent(key).hasNode(key);
        }

        public Node addNode(String key, String primaryNodeTypeName) throws RepositoryException {
            Node parent = getOrCreateParent(key);
            if (parent.hasNode(key)) {
                throw new ItemExistsException(key);
            }

            Node n;
            if (parent.getPrimaryNodeType().hasOrderableChildNodes()) {
                Node dest = getSuccessor(parent, key);
                n = parent.addNode(key, primaryNodeTypeName);
                parent.orderBefore(key, dest == null ? null : dest.getName());
            }
            else {
                n = parent.addNode(key, primaryNodeTypeName);
            }

            treeManager.split(this, parent, n);

            if (autoSave) {
                parent.getSession().save();
            }

            return n;
        }

        public void removeNode(String key) throws RepositoryException {
            Node parent = getParent(key);
            Node n = parent.getNode(key);
            n.remove();
            treeManager.join(this, parent, n);

            if (autoSave) {
                parent.getSession().save();
            }
        }

        @Override
        public Node getParent(String key) throws RepositoryException {
            Node p = getPredecessor(key);
            if (treeManager.isLeaf(p) && !treeManager.isRoot(p)) {
                return p.getParent();
            }
            else {
                return p;
            }
        }

        private Node getOrCreateParent(String key) throws RepositoryException {
            Node p = getParent(key);
            if (treeManager.isRoot(p)) {
                Node min = getMinimal();
                if (min != null) {
                    p = min.getParent();
                    renamePath(p, key);
                }
            }
            return p;
        }

    }

    protected static class PropertySequenceImpl extends ItemSequence implements PropertySequence {
        private final InclusionPolicy<Property> inclusionPolicy = new InclusionPolicy<Property>() {
            private final Set<String> ignoredProperties = treeManager.getIgnoredProperties();
            public boolean include(Property property) {
                try {
                    return !ignoredProperties.contains(property.getName());
                }
                catch (RepositoryException e) {
                    return false;
                }
            }
        };

        public PropertySequenceImpl(TreeManager treeManager, ErrorHandler errorHandler) {
            super(treeManager, errorHandler);
        }

        public Iterator<Property> iterator() {
            return TreeTraverser.propertyIterator(getNodeSequence().iterator(), errorHandler, inclusionPolicy);
        }

        public Property getItem(String key) throws RepositoryException {
            return getParent(key).getProperty(key);
        }

        public boolean hasItem(String key) throws RepositoryException {
            return getParent(key).hasProperty(key);
        }

        public Property addProperty(String key, Value value) throws RepositoryException {
            Node parent = getOrCreateParent(key);
            if (parent.hasProperty(key)) {
                throw new ItemExistsException(key);
            }

            Property p = parent.setProperty(key, value);
            treeManager.split(this, parent, p);

            if (autoSave) {
                p.getSession().save();
            }

            return p;
        }

        public void removeProperty(String key) throws RepositoryException {
            Node parent = getParent(key);
            Property p = parent.getProperty(key);
            p.remove();
            treeManager.join(this, parent, p);

            if (autoSave) {
                parent.getSession().save();
            }
        }

        @Override
        public Node getParent(String key) throws RepositoryException {
            return getPredecessor(key);
        }

        private Node getOrCreateParent(String key) throws RepositoryException {
            Node p = getParent(key);
            if (treeManager.isRoot(p)) {
                Node min = getMinimal();
                if (min != null) {
                    p = min;
                    renamePath(p, key);
                }
            }
            return p;
        }

    }

}
