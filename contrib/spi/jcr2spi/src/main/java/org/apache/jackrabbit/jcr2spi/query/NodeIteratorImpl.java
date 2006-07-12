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
package org.apache.jackrabbit.jcr2spi.query;

import org.apache.jackrabbit.jcr2spi.ItemManager;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.IdIterator;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.QueryInfo;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Item;

import java.util.NoSuchElementException;

// DIFF JR: this class uses a different package than the jackrabbit original
/**
 * Implements a {@link javax.jcr.NodeIterator} returned by
 * {@link javax.jcr.query.QueryResult#getNodes()}.
 */
class NodeIteratorImpl implements ScoreNodeIterator {

    /** Logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(NodeIteratorImpl.class);

    /** ItemManager to turn Ids into Node instances */
    protected final ItemManager itemMgr;

    /** The namespace resolver */
    protected final NamespaceResolver resolver;

    /** The query result info */
    protected final QueryInfo queryInfo;

    /** The ItemId's of the result nodes */
    protected final IdIterator ids;

    /** Index of the jcr:score column. */
    protected final int scoreIndex;

    /** Current position of this node iterator */
    protected int pos = -1;

    /** Number of invalid nodes */
    protected int invalid = 0;

    /** Id of the next Node */
    private NodeId nextId;

    /** Reference to the next node instance */
    private Node next;

    /**
     * Creates a new <code>NodeIteratorImpl</code> instance.
     *
     * @param itemMgr   the <code>ItemManager</code> to turn Id's into
     *                  <code>Node</code> instances.
     * @param resolver  the namespace resolver.
     * @param queryInfo the query result.
     * @throws RepositoryException if an error occurs while creating a node
     *                             iterator.
     */
    // DIFF JR: use QueryInfo instead of UUID String[]
    NodeIteratorImpl(ItemManager itemMgr,
                     NamespaceResolver resolver,
                     QueryInfo queryInfo) throws RepositoryException {
        this.itemMgr = itemMgr;
        this.resolver = resolver;
        this.queryInfo = queryInfo;
        this.ids = queryInfo.getNodeIds();
        QName[] columnNames = queryInfo.getColumnNames();
        int idx = -1;
        for (int i = 0; i < columnNames.length; i++) {
            if (columnNames[i].getNamespaceURI().equals(QName.NS_JCR_URI)
                    && columnNames[i].getLocalName().startsWith(QName.JCR_SCORE.getLocalName())) {
                idx = i;
                break;
            }
        }
        if (idx == -1) {
            throw new RepositoryException("no jcr:score column in query result");
        }
        this.scoreIndex = idx;
        fetchNext();
    }

    /**
     * Returns the next <code>Node</code> in the result set.
     *
     * @return the next <code>Node</code> in the result set.
     * @throws NoSuchElementException if iteration has no more
     *   <code>Node</code>s.
     */
    public Node nextNode() throws NoSuchElementException {
        if (next == null) {
            throw new NoSuchElementException();
        }
        Node n = next;
        fetchNext();
        return n;
    }

    /**
     * Returns the next <code>Node</code> in the result set.
     *
     * @return the next <code>Node</code> in the result set.
     * @throws NoSuchElementException if iteration has no more
     *   <code>Node</code>s.
     */
    public Object next() throws NoSuchElementException {
        return nextNode();
    }

    /**
     * Skip a number of <code>Node</code>s in this iterator.
     *
     * @param skipNum the non-negative number of <code>Node</code>s to skip
     * @throws NoSuchElementException
     *          if skipped past the last <code>Node</code> in this iterator.
     */
    public void skip(long skipNum) throws NoSuchElementException {
        if (skipNum < 0) {
            throw new IllegalArgumentException("skipNum must not be negative");
        }
        if (skipNum == 0) {
            // do nothing
        } else {
            ids.skip(skipNum - 1);
            pos += skipNum - 1;
            fetchNext();
        }
    }

    /**
     * Returns the number of nodes in this iterator.
     * </p>
     * Note: The number returned by this method may differ from the number
     * of nodes actually returned by calls to hasNext() / getNextNode()! This
     * is because this iterator works on a lazy instantiation basis and while
     * iterating over the nodes some of them might have been deleted in the
     * meantime. Those will not be returned by getNextNode(). As soon as an
     * invalid node is detected, the size of this iterator is adjusted.
     *
     * @return the number of node in this iterator.
     */
    public long getSize() {
        if (ids.getSize() != -1) {
            return ids.getSize() - invalid;
        } else {
            return -1;
        }
    }

    /**
     * Returns the current position in this <code>NodeIterator</code>.
     *
     * @return the current position in this <code>NodeIterator</code>.
     */
    public long getPosition() {
        return pos - invalid;
    }

    /**
     * Returns <code>true</code> if there is another <code>Node</code>
     * available; <code>false</code> otherwise.
     *
     * @return <code>true</code> if there is another <code>Node</code>
     *  available; <code>false</code> otherwise.
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    /**
     * Returns the score of the node returned by {@link #nextNode()}. In other
     * words, this method returns the score value of the next <code>Node</code>.
     * 
     * @return the score of the node returned by {@link #nextNode()}.
     * @throws NoSuchElementException if there is no next node.
     */
    public float getScore() throws NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        String scoreString = queryInfo.getValues(nextId)[scoreIndex];
        try {
            return Float.parseFloat(scoreString);
        } catch (NumberFormatException e) {
            throw new NoSuchElementException();
        }
    }

    /**
     * Clears {@link #next} and tries to fetch the next Node instance.
     * When this method returns {@link #next} refers to the next available
     * node instance in this iterator. If {@link #next} is null when this
     * method returns, then there are no more valid element in this iterator.
     */
    private void fetchNext() {
        // reset
        next = null;
        while (next == null && ids.hasNext()) {
            try {
                ItemId id = ids.nextId();
                if (!id.denotesNode()) {
                    log.error("NodeId expected. Found PropertyId: " + id);
                    continue;
                }
                nextId = (NodeId)id;
                Item tmp = itemMgr.getItem(nextId);

                if (tmp.isNode()) {
                    next = (Node) tmp;
                } else {
                    log.warn("Item with Id is not a Node: " + nextId);
                    // try next
                    invalid++;
                    pos++;
                }
            } catch (Exception e) {
                log.error("Exception retrieving Node with Id: " + nextId);
                // try next
                invalid++;
                pos++;
            }
        }
        pos++;
    }
}
