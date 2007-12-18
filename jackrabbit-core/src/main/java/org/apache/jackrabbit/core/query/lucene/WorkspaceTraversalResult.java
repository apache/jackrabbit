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
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.jcr.RepositoryException;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.NamespaceException;
import java.util.Iterator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Implements a query result that traverses the whole workspace and returns
 * the nodes in document order.
 */
public class WorkspaceTraversalResult implements QueryResult {

    /**
     * The session that issued the query.
     */
    private final Session session;

    /**
     * The select properties.
     */
    private final Name[] properties;

    /**
     * The namespace resolver of the session.
     */
    private final NamePathResolver resolver;

    /**
     * Creates a new <code>WorkspaceTraversalResult</code>.
     *
     * @param session    the session that issued the query.
     * @param properties the select properties.
     * @param resolver   the namespace resolver of the session.
     */
    public WorkspaceTraversalResult(Session session,
                             Name[] properties,
                             NamePathResolver resolver) {
        this.session = session;
        this.properties = properties;
        this.resolver = resolver;
    }

    /**
     * @inheritDoc
     */
    public String[] getColumnNames() throws RepositoryException {
        try {
            String[] propNames = new String[properties.length];
            for (int i = 0; i < properties.length; i++) {
                propNames[i] = resolver.getJCRName(properties[i]);
            }
            return propNames;
        } catch (NamespaceException e) {
            String msg = "encountered invalid property name";
            throw new RepositoryException(msg, e);

        }
    }

    /**
     * @inheritDoc
     */
    public RowIterator getRows() throws RepositoryException {
        return new RowIteratorImpl(getNodeIterator(), properties, resolver);
    }

    /**
     * @inheritDoc
     */
    public NodeIterator getNodes() throws RepositoryException {
        return getNodeIterator();
    }

    /**
     * Returns a {@link ScoreNodeIterator} that traverses the workspace in
     * document order.
     *
     * @return iterator that returns nodes in document order.
     * @throws RepositoryException if an error occurs while creating the
     *                             iterator.
     */
    private ScoreNodeIterator getNodeIterator() throws RepositoryException {
        return new TraversingNodeIterator(session.getRootNode(), Offset.ZERO);
    }

    /**
     * Implements a node iterator that traverses the workspace in document
     * order.
     */
    private class TraversingNodeIterator implements ScoreNodeIterator, Offset {

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
         * Offset of this iterator to calculate the value in
         * {@link #getPosition()}.
         */
        private final Offset offset;

        /**
         * Current (local) position in this iterator.
         */
        private long position;

        /**
         * Creates a <code>TraversingNodeIterator</code>.
         * @param start the node from where to start the traversal.
         * @param offset the offset to use to calculate the position.
         */
        TraversingNodeIterator(Node start, Offset offset) {
            currentNode = start;
            this.offset = offset;
        }

        /**
         * Returns always 1.
         * @return always 1.
         */
        public float getScore() {
            return 1.0f;
        }

        /**
         * @inheritDoc
         */
        public NodeImpl nextNodeImpl() {
            init();
            NodeImpl n = (NodeImpl) selfAndChildren.next();
            position++;
            return n;
        }

        /**
         * @inheritDoc
         */
        public Node nextNode() {
            return nextNodeImpl();
        }

        /**
         * @inheritDoc
         */
        public void skip(long skipNum) {
            while (skipNum > 0) {
                if (hasNext()) {
                    next();
                    skipNum--;
                } else {
                    throw new NoSuchElementException();
                }
            }
        }

        /**
         * Returns always -1 (unknown).
         * @return always -1.
         */
        public long getSize() {
            return -1;
        }

        /**
         * @inheritDoc
         */
        public long getPosition() {
            return offset.getValue() + position;
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
            return nextNode();
        }

        /**
         * Returns the offset value for this iterator. The offset is the current
         * position plus 1.
         * @return  the offset value for this iterator.
         */
        public long getValue() {
            return getPosition() + 1;
        }

        /**
         * Initializes the iterator chain once.
         */
        private void init() {
            if (selfAndChildren == null) {
                Iterator current = Arrays.asList(new Node[]{currentNode}).iterator();
                List allIterators = new ArrayList();
                allIterators.add(current);
                Offset offset = new Offset() {
                    public long getValue() {
                        return TraversingNodeIterator.this.offset.getValue() + 1;
                    }
                };

                // create new TraversingNodeIterator for each child
                try {
                    NodeIterator children = currentNode.getNodes();
                    while (children.hasNext()) {
                        offset = new TraversingNodeIterator(children.nextNode(), offset);
                        allIterators.add(offset);
                    }
                } catch (RepositoryException e) {
                    // currentNode is probably stale
                }
                selfAndChildren = new IteratorChain(allIterators);
            }
        }
    }

    /**
     * Helper class that holds an offset value.
     */
    interface Offset {

        /**
         * Offset that always returns the value 0.
         */
        Offset ZERO = new Offset() {
            public long getValue() {
                return 0;
            }
        };

        /**
         * Returns the value of this offset.
         * @return the value of this offset..
         */
        long getValue();
    }
}
