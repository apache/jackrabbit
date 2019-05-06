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

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.iterator.FilterIterator;
import org.apache.jackrabbit.commons.predicate.Predicate;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This {@link TreeManager} implementation provides B+-tree like behavior. That
 * is items of a sequence (i.e. {@link NodeSequence} or {@link PropertySequence})
 * are mapped to a sub-tree in JCR in a way such that only leave nodes carry
 * actual values, the sub-tree is always balanced and ordered. This
 * implementation does in contrast to a full B+-tree implementation <em>not</em>
 * join nodes after deletions. This does not affect the order of items and also
 * leaves the tree balanced wrt. its depths. It might however result in a sparse
 * tree. That is, the tree might get unbalanced wrt. its weights.
 * <p>
 * The nodes in the JCR sub tree are arranged such that any node named <code>x</code>
 * only contains child nodes with names greater or equal to <code>x</code>.
 * The implementation keeps the child nodes in the sub tree ordered if the
 * respective node type supports ordering of child nodes.
 * Ordering is always wrt. to a {@link Comparator} on the respective keys.
 * For lexical order this arrangement corresponds to how words are arranged in a multi
 * volume encyclopedia.
 * <p>
 * Example usage:
 * <pre>
 * // Create a new TreeManager instance rooted at node. Splitting of nodes takes place
 * // when the number of children of a node exceeds 40 and is done such that each new
 * // node has at least 40 child nodes. The keys are ordered according to the natural
 * // order of java.lang.String.
 * TreeManager treeManager = new BTreeManager(node, 20, 40, Rank.&lt;String&gt;comparableComparator(), true);
 *
 * // Create a new NodeSequence with that tree manager
 * NodeSequence nodes = ItemSequence.createNodeSequence(treeManager);
 *
 * // Add nodes with key &quot;jcr&quot; and &quot;day&quot;
 * nodes.addNode(&quot;jcr&quot;, NodeType.NT_UNSTRUCTURED);
 * nodes.addNode(&quot;day&quot;, NodeType.NT_UNSTRUCTURED);
 *
 * // Iterate over the node in the sequence.
 * // Prints &quot;day jcr &quot;
 * for (Node n : nodes) {
 *     System.out.print(n.getName() + &quot; &quot;);
 * }
 *
 * // Retrieve node with key &quot;jcr&quot;
 * Node n = nodes.getItem(&quot;jcr&quot;);
 *
 * // Remove node with key &quot;day&quot;
 * nodes.removeNode(&quot;day&quot;);
 * </pre>
 */
public class BTreeManager implements TreeManager {
    private final Node root;
    private final int minChildren;
    private final int maxChildren;
    private final Comparator<String> order;
    private final boolean autoSave;
    private final Comparator<Item> itemOrder;

    private final Set<String> ignoredProperties = new HashSet<String>(Arrays.asList(
            JcrConstants.JCR_PRIMARYTYPE,
            JcrConstants.JCR_MIXINTYPES));

    /**
     * Create a new {@link BTreeManager} rooted at Node <code>root</code>.
     *
     * @param root the root of the JCR sub-tree where the items of the sequence
     *            are stored.
     * @param minChildren minimal number of children for a node after splitting.
     * @param maxChildren maximal number of children for a node after which
     *            splitting occurs.
     * @param order order according to which the keys are stored
     * @param autoSave determines whether the current session is saved after
     *            add/delete operations.
     * @throws RepositoryException
     */
    public BTreeManager(Node root, int minChildren, int maxChildren, Comparator<String> order, boolean autoSave)
            throws RepositoryException {
        super();

        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        if (minChildren <= 0) {
            throw new IllegalArgumentException("minChildren must be positive");
        }
        if (2 * minChildren > maxChildren) {
            throw new IllegalArgumentException("maxChildren must be at least twice minChildren");
        }
        if (order == null) {
            throw new IllegalArgumentException("order must not be null");
        }

        this.root = root;
        this.minChildren = minChildren;
        this.maxChildren = maxChildren;
        this.order = order;
        this.autoSave = autoSave;
        this.itemOrder = new Comparator<Item>() {
            public int compare(Item i1, Item i2) {
                try {
                    return BTreeManager.this.order.compare(i1.getName(), i2.getName());
                }
                catch (RepositoryException e) {
                    throw new WrappedRepositoryException(e);
                }
            }
        };
    }

    /**
     * Properties to ignore. The default set contains {@link JcrConstants#JCR_PRIMARYTYPE}
     * and {@link JcrConstants#JCR_MIXINTYPES}.
     *
     * @return
     */
    public Set<String> getIgnoredProperties() {
        return ignoredProperties;
    }

    /**
     * This implementations splits <code>node</code> when its number of child
     * nodes exceeds the maximum number specified in the constructor. Splitting
     * is done such that after the split each of the new child nodes contains at
     * least as many nodes as specified in the constructor.
     *
     * @see org.apache.jackrabbit.commons.flat.TreeManager#split(org.apache.jackrabbit.commons.flat.ItemSequence,
     *      javax.jcr.Node, javax.jcr.Node)
     */
    @SuppressWarnings("deprecation")
    public void split(ItemSequence itemSequence, Node node, Node cause) throws RepositoryException {
        SizedIterator<Node> childNodes = getNodes(node);
        int count = (int) childNodes.getSize();
        if (count >= 0 && count <= maxChildren) {
            return;
        }

        split(node, new Rank<Node>(childNodes, Node.class, count, itemOrder), itemSequence);
    }

    /**
     * This implementations splits <code>node</code> when its number of
     * properties exceeds the maximum number specified in the constructor.
     * Splitting is done such that after the split each of the new child nodes
     * contains at least as many nodes as specified in the constructor.
     *
     * @see org.apache.jackrabbit.commons.flat.TreeManager#split(org.apache.jackrabbit.commons.flat.ItemSequence,
     *      javax.jcr.Node, javax.jcr.Property)
     */
    @SuppressWarnings("deprecation")
    public void split(ItemSequence itemSequence, Node node, Property cause) throws RepositoryException {
        SizedIterator<Property> properties = getProperties(node);
        int count = (int) properties.getSize();
        if (count >= 0 && count <= maxChildren) {
            return;
        }

        split(node, new Rank<Property>(properties, Property.class, count, itemOrder), itemSequence);
    }

    /**
     * This implementation does not actually join any nodes. It does however
     * delete <code>node</code> if {@link #getNodes(Node)} returns an empty
     * iterator. It does further recursively delete any parent of
     * <code>node</code> which does not have any child node.
     *
     * @see org.apache.jackrabbit.commons.flat.TreeManager#join(org.apache.jackrabbit.commons.flat.ItemSequence,
     *      javax.jcr.Node, javax.jcr.Node)
     */
    @SuppressWarnings("deprecation")
    public void join(ItemSequence itemSequence, Node node, Node cause) throws RepositoryException {
        SizedIterator<Node> nodes = getNodes(node);
        long count = nodes.getSize();
        if (count < 0) {
            for (count = 0; nodes.hasNext(); count++) {
                nodes.next();
            }
        }

        if (count == 0) {
            removeRec(node);
        }
    }

    /**
     * This implementation does not actually join any nodes. It does however
     * delete <code>node</code> if {@link #getProperties(Node)} returns an empty
     * iterator. It does further recursively delete any parent of
     * <code>node</code> which does not have any child node.
     *
     * @see org.apache.jackrabbit.commons.flat.TreeManager#join(org.apache.jackrabbit.commons.flat.ItemSequence,
     *      javax.jcr.Node, javax.jcr.Property)
     */
    @SuppressWarnings("deprecation")
    public void join(ItemSequence itemSequence, Node node, Property cause) throws RepositoryException {
        SizedIterator<Property> properties = getProperties(node);
        long count = properties.getSize();
        if (count < 0) {
            for (count = 0; properties.hasNext(); count++) {
                properties.next();
            }
        }

        if (count == 0) {
            removeRec(node);
        }
    }

    public Node getRoot() {
        return root;
    }

    public boolean isRoot(Node node) throws RepositoryException {
        return node.isSame(root);
    }

    /**
     * Returns <code>!node.hasNodes()</code>
     * @see org.apache.jackrabbit.commons.flat.TreeManager#isLeaf(javax.jcr.Node)
     */
    public boolean isLeaf(Node node) throws RepositoryException {
        return !node.hasNodes();
    }

    public Comparator<String> getOrder() {
        return order;
    }

    public boolean getAutoSave() {
        return autoSave;
    }

    // -----------------------------------------------------< internal >---

    /**
     * Returns a {@link SizedIterator} of the child nodes of <code>node</code>.
     */
    @SuppressWarnings("deprecation")
    protected SizedIterator<Node> getNodes(Node node) throws RepositoryException {
        NodeIterator nodes = node.getNodes();
        return getSizedIterator(convert(nodes), nodes.getSize());
    }

    /**
     * Returns a {@link SizedIterator} of the properties of <code>node</code>
     * which excludes the <code>jcr.primaryType</code> property.
     */
    @SuppressWarnings("deprecation")
    protected SizedIterator<Property> getProperties(Node node) throws RepositoryException {
        final PropertyIterator properties = node.getProperties();

        long size = properties.getSize();
        for (Iterator<String> ignored = ignoredProperties.iterator(); size > 0 && ignored.hasNext(); ) {
            if (node.hasProperty(ignored.next())) {
                size--;
            }
        }

        return getSizedIterator(filterProperties(convert(properties)), size);
    }

    /**
     * Creates and return an intermediate node for the given <code>name</code>
     * as child node of <code>parent</code>.
     */
    protected Node createIntermediateNode(Node parent, String name) throws RepositoryException {
        return parent.addNode(name);
    }

    /**
     * Move <code>node</code> to the new <code>parent</code>.
     */
    protected void move(Node node, Node parent) throws RepositoryException {
        String oldPath = node.getPath();
        String newPath = parent.getPath() + "/" + node.getName();
        node.getSession().move(oldPath, newPath);
    }

    /**
     * Move <code>property</code> to the new <code>parent</code>.
     */
    protected void move(Property property, Node parent) throws RepositoryException {
        parent.setProperty(property.getName(), property.getValue());
        property.remove();
    }

    /**
     * Wraps <code>iterator</code> into a {@link SizedIterator} given a
     * <code>size</code>. The value of the <code>size</code> parameter must
     * correctly reflect the number of items in <code>iterator</code>.
     */
    @SuppressWarnings("deprecation")
    protected final <T> SizedIterator<T> getSizedIterator(final Iterator<T> iterator, final long size) {
        return new SizedIterator<T>() {
            public boolean hasNext() {
                return iterator.hasNext();
            }

            public T next() {
                return iterator.next();
            }

            public void remove() {
                iterator.remove();
            }

            public long getSize() {
                return size;
            }
        };
    }

    // -----------------------------------------------------< internal >---

    @SuppressWarnings("unchecked")
    private static Iterator<Property> convert(PropertyIterator it) {
        return it;
    }

    @SuppressWarnings("unchecked")
    private static Iterator<Node> convert(NodeIterator it) {
        return it;
    }

    private <T extends Item> void split(Node node, Rank<T> ranking, ItemSequence itemSequence) throws RepositoryException {
        if (ranking.size() <= maxChildren) {
            return;
        }

        try {
            Node grandParent;
            if (isRoot(node)) {
                grandParent = node;
            }
            else {
                grandParent = node.getParent();

                // leave first minChildren items where they are
                ranking.take(minChildren);
            }

            // move remaining items to new parents
            for (int k = ranking.size() / minChildren; k > 0; k--) {
                T item = ranking.take(1).next();
                String key = item.getName();

                Node newParent;
                if (grandParent.getPrimaryNodeType().hasOrderableChildNodes()) {
                    Node dest = itemSequence.getSuccessor(grandParent, key);
                    newParent = createIntermediateNode(grandParent, key);
                    grandParent.orderBefore(key, dest == null ? null : dest.getName());
                }
                else {
                    newParent = createIntermediateNode(grandParent, key);
                }

                move(item, newParent);

                int c = k > 1 ? minChildren - 1 : ranking.size();
                Iterator<T> remaining = ranking.take(c);

                // If ordered, ranking returns an ordered iterator. So order will be correct here
                while (remaining.hasNext()) {
                    move(remaining.next(), newParent);
                }
            }

            // If we did not reach root yet, recursively split the parent
            if (!node.isSame(root)) {
                split(itemSequence, grandParent, (Node) null);
            }
        }
        catch (WrappedRepositoryException e) {
            throw e.wrapped();
        }
    }

    private <T extends Item> void move(T item, Node parent) throws RepositoryException {
        if (item.isNode()) {
            move((Node) item, parent);
        }
        else {
            move((Property) item, parent);
        }
    }

    private void removeRec(Node node) throws RepositoryException {
        Node n = node;
        while (!n.hasNodes() && !isRoot(n)) {
            Node d = n;
            n = n.getParent();
            d.remove();
        }
    }

    /**
     * Filtering ignored properties from the given properties.
     */
    private Iterator<Property> filterProperties(Iterator<Property> properties) {
        return new FilterIterator<Property>(properties, new Predicate() {
            public boolean evaluate(Object object) {
                try {
                    Property p = (Property) object;
                    return !ignoredProperties.contains(p.getName());
                }
                catch (RepositoryException ignore) {
                    return true;
                }
            }
        });
    }

    private static class WrappedRepositoryException extends RuntimeException {
        private final RepositoryException wrapped;

        public WrappedRepositoryException(RepositoryException e) {
            super();
            this.wrapped = e;
        }

        public RepositoryException wrapped() {
            return wrapped;
        }
    }

}