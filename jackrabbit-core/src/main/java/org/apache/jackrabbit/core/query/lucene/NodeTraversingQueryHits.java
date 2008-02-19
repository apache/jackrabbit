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

import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.jackrabbit.core.NodeImpl;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * <code>NodeTraversingQueryHits</code> implements query hits that traverse
 * a node hierarchy.
 */
public class NodeTraversingQueryHits extends AbstractQueryHits {

    /**
     * The nodes to traverse.
     */
    private final Iterator nodes;

    /**
     * Creates query hits that consist of the nodes that are traversed from
     * a given <code>start</code> node.
     *
     * @param start the start node of the traversal.
     * @param includeStart whether to include the start node in the result.
     */
    public NodeTraversingQueryHits(Node start, boolean includeStart) {
        this.nodes = new TraversingNodeIterator(start);
        if (!includeStart) {
            nodes.next();
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Does nothing.
     */
    protected void doClose() throws IOException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation always returns <code>-1</code>.
     */
    public int getSize() {
        // don't know
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    public ScoreNode nextScoreNode() throws IOException {
        if (nodes.hasNext()) {
            NodeImpl n = (NodeImpl) nodes.next();
            return new ScoreNode(n.getNodeId(), 1.0f);
        } else {
            return null;
        }
    }

    /**
     * Implements a node iterator that traverses a node tree in document
     * order.
     */
    private class TraversingNodeIterator implements Iterator {

        /**
         * The current <code>Node</code>, which acts as the starting point for
         * the traversal.
         */
        private final Node currentNode;

        /**
         * The chain of iterators which includes the iterators of the children
         * of the current node.
         */
        private IteratorChain selfAndChildren;

        /**
         * Creates a <code>TraversingNodeIterator</code>.
         * @param start the node from where to start the traversal.
         */
        TraversingNodeIterator(Node start) {
            currentNode = start;
        }

        /**
         * @exception UnsupportedOperationException always.
         */
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

        /**
         * @inheritDoc
         */
        public boolean hasNext() {
            init();
            return selfAndChildren.hasNext();
        }

        /**
         * @inheritDoc
         */
        public Object next() {
            init();
            NodeImpl n = (NodeImpl) selfAndChildren.next();
            return n;
        }

        /**
         * Initializes the iterator chain once.
         */
        private void init() {
            if (selfAndChildren == null) {
                Iterator current = Arrays.asList(new Node[]{currentNode}).iterator();
                List allIterators = new ArrayList();
                allIterators.add(current);

                // create new TraversingNodeIterator for each child
                try {
                    NodeIterator children = currentNode.getNodes();
                    while (children.hasNext()) {
                        allIterators.add(new TraversingNodeIterator(children.nextNode()));
                    }
                } catch (RepositoryException e) {
                    // currentNode is probably stale
                }
                selfAndChildren = new IteratorChain(allIterators);
            }
        }
    }
}
