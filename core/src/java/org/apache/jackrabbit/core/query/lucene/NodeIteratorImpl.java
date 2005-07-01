/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.log4j.Logger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.util.NoSuchElementException;

/**
 * Implements a {@link javax.jcr.NodeIterator} returned by
 * {@link javax.jcr.query.QueryResult#getNodes()}.
 */
class NodeIteratorImpl implements NodeIterator {

    /** Logger instance for this class */
    private static final Logger log = Logger.getLogger(NodeIteratorImpl.class);

    /** The UUIDs of the nodes in the result set */
    protected final String[] uuids;

    /** The score values for the nodes in the result set */
    protected final Float[] scores;

    /** ItemManager to turn UUIDs into Node instances */
    protected final ItemManager itemMgr;

    /** Current position in the UUID array */
    private int pos = 0;

    /**
     * Creates a new <code>NodeIteratorImpl</code> instance.
     * @param itemMgr the <code>ItemManager</code> to turn UUIDs into
     *   <code>Node</code> instances.
     * @param uuids the UUIDs of the result nodes.
     * @param scores the corresponding score values for each result node.
     */
    NodeIteratorImpl(ItemManager itemMgr,
                     String[] uuids,
                     Float[] scores) {
        this.itemMgr = itemMgr;
        this.uuids = uuids;
        this.scores = scores;
    }

    /**
     * Returns the next <code>Node</code> in the result set.
     * @return the next <code>Node</code> in the result set.
     * @throws NoSuchElementException if iteration has no more
     *   <code>Node</code>s.
     */
    public Node nextNode() throws NoSuchElementException {
        return nextNodeImpl();
    }

    /**
     * Returns the next <code>Node</code> in the result set.
     * @return the next <code>Node</code> in the result set.
     * @throws NoSuchElementException if iteration has no more
     *   <code>Node</code>s.
     */
    public Object next() throws NoSuchElementException {
        return nextNode();
    }

    /**
     * Skip a number of <code>Node</code>s in this iterator.
     * @param skipNum the non-negative number of <code>Node</code>s to skip
     * @throws NoSuchElementException
     *          if skipped past the last <code>Node</code> in this iterator.
     */
    public void skip(long skipNum) throws NoSuchElementException {
        if (skipNum < 0) {
            throw new IllegalArgumentException("skipNum must not be negative");
        }
        if ((pos + skipNum) > uuids.length) {
            throw new NoSuchElementException();
        }
        pos += skipNum;
    }

    /**
     * Returns the number of <code>Node</code>s in this
     * <code>NodeIterator</code>.
     * @return the number of <code>Node</code>s in this
     *   <code>NodeIterator</code>.
     */
    public long getSize() {
        return uuids.length;
    }

    /**
     * Returns the current position in this <code>NodeIterator</code>.
     * @return the current position in this <code>NodeIterator</code>.
     */
    public long getPosition() {
        return pos;
    }

    /**
     * Returns <code>true</code> if there is another <code>Node</code>
     * available; <code>false</code> otherwise.
     * @return <code>true</code> if there is another <code>Node</code>
     *  available; <code>false</code> otherwise.
     */
    public boolean hasNext() {
        return pos < uuids.length;
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
     * @return the score of the node returned by {@link #nextNode()}.
     * @throws NoSuchElementException if there is no next node.
     */
    float getScore() throws NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return scores[pos].floatValue();
    }

    /**
     * Returns the next <code>Node</code> in the result set.
     * @return the next <code>Node</code> in the result set.
     * @throws NoSuchElementException if iteration has no more
     *   <code>Node</code>s.
     */
    NodeImpl nextNodeImpl() throws NoSuchElementException {
        if (pos >= uuids.length) {
            throw new NoSuchElementException();
        }
        try {
            return (NodeImpl) itemMgr.getItem(new NodeId(uuids[pos++]));
        } catch (RepositoryException e) {
            log.error("Exception retrieving Node with UUID: "
                    + uuids[pos - 1] + ": " + e.toString());
            throw new NoSuchElementException();
        }
    }
}
