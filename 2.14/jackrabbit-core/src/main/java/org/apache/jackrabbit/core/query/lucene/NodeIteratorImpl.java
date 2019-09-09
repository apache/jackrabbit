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

import org.apache.jackrabbit.core.ItemManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.session.SessionOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.NodeIterator;

import java.util.NoSuchElementException;

/**
 * Implements a {@link javax.jcr.NodeIterator} returned by
 * {@link javax.jcr.query.QueryResult#getNodes()}.
 */
class NodeIteratorImpl implements NodeIterator {

    /** Logger instance for this class */
    private static final Logger log = LoggerFactory.getLogger(NodeIteratorImpl.class);

    /**
     * Component context of the current session
     */
    protected final SessionContext sessionContext;

    /** The node ids of the nodes in the result set with their score value */
    protected final ScoreNodeIterator scoreNodes;

    /** The index for the default selector within {@link #scoreNodes} */
    private final int selectorIndex;

    /** Number of invalid nodes */
    protected int invalid = 0;

    /** Reference to the next node instance */
    private NodeImpl next;

    /**
     * Whether this iterator had been initialized.
     */
    private boolean initialized;

    /**
     * Creates a new <code>NodeIteratorImpl</code> instance.
     *
     * @param sessionContext component context of the current session
     * @param scoreNodes    iterator over score nodes.
     * @param selectorIndex the index for the default selector within
     *                      <code>scoreNodes</code>.
     */
    NodeIteratorImpl(
            SessionContext sessionContext, ScoreNodeIterator scoreNodes,
            int selectorIndex) {
        this.sessionContext = sessionContext;
        this.scoreNodes = scoreNodes;
        this.selectorIndex = selectorIndex;
    }

    /**
     * Returns the next <code>Node</code> in the result set.
     * @return the next <code>Node</code> in the result set.
     * @throws NoSuchElementException if iteration has no more
     *   <code>Node</code>s.
     */
    public Node nextNode() throws NoSuchElementException {
        initialize();
        if (next == null) {
            throw new NoSuchElementException();
        }
        NodeImpl n = next;
        fetchNext();
        return n;
    }

    /**
     * Returns the next <code>Node</code> in the result set.
     * @return the next <code>Node</code> in the result set.
     * @throws NoSuchElementException if iteration has no more
     *   <code>Node</code>s.
     */
    public Object next() throws NoSuchElementException {
        initialize();
        return nextNode();
    }

    /**
     * Skip a number of <code>Node</code>s in this iterator.
     * @param skipNum the non-negative number of <code>Node</code>s to skip
     * @throws NoSuchElementException
     *          if skipped past the last <code>Node</code> in this iterator.
     */
    public void skip(long skipNum) throws NoSuchElementException {
        initialize();
        if (skipNum > 0) {
            scoreNodes.skip(skipNum - 1);
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
        long size = scoreNodes.getSize();
        if (size == -1) {
            return size;
        } else {
            return size - invalid;
        }
    }

    /**
     * Returns the current position in this <code>NodeIterator</code>.
     * @return the current position in this <code>NodeIterator</code>.
     */
    public long getPosition() {
        initialize();
        long position = scoreNodes.getPosition() - invalid;
        // scoreNode.getPosition() is one ahead
        // if there is a prefetched node
        if (next != null) {
            position--;
        }
        return position;
    }

    /**
     * Returns <code>true</code> if there is another <code>Node</code>
     * available; <code>false</code> otherwise.
     * @return <code>true</code> if there is another <code>Node</code>
     *  available; <code>false</code> otherwise.
     */
    public boolean hasNext() {
        initialize();
        return next != null;
    }

    /**
     * @throws UnsupportedOperationException always.
     */
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    /**
     * Clears {@link #next} and tries to fetch the next Node instance.
     * When this method returns {@link #next} refers to the next available
     * node instance in this iterator. If {@link #next} is null when this
     * method returns, then there are no more valid element in this iterator.
     */
    protected void fetchNext() {
        try {
            next = null; // reset
            sessionContext.getSessionState().perform(new FetchNext());
        } catch (RepositoryException e) {
            log.warn("Failed to fetch next node", e);
        }
    }

    private class FetchNext implements SessionOperation<Object> {

        public Object perform(SessionContext context) {

            ItemManager itemMgr = context.getItemManager();
            while (next == null && scoreNodes.hasNext()) {
                ScoreNode[] sn = scoreNodes.nextScoreNodes();
                try {
                    next = (NodeImpl) itemMgr.getItem(
                            sn[selectorIndex].getNodeId());
                } catch (RepositoryException e) {
                    log.warn("Failed to retrieve query result node "
                            + sn[selectorIndex].getNodeId(), e);
                    // try next
                    invalid++;
                }
            }

            return this;
        }

    }

    protected void initialize() {
        if (!initialized) {
            fetchNext();
            initialized = true;
        }
    }
}
