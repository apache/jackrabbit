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
package org.apache.jackrabbit.commons.iterator;

import org.apache.jackrabbit.commons.predicate.Predicate;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import java.util.NoSuchElementException;

/**
 * A wrapper around a NodeIterator filtering out nodes from the base iterator
 * that don't match the specified {@link Predicate filter}. Due to the nature
 * of the filter mechanism the {@link #getSize() size} if the iterator may
 * shrink upon iteration.
 */
public class FilteringNodeIterator implements NodeIterator {

    protected final NodeIterator base;
    protected final Predicate filter;

    private Node next;
    private long position;

    public FilteringNodeIterator(NodeIterator base, Predicate filter) {
        this.base = base;
        this.filter = filter;
        next = seekNext();
    }

    //-----------------------------------------------------------< Iterator >---
    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * @see java.util.Iterator#next()
     */
    public Object next() {
        return nextNode();
    }

    /**
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    //-------------------------------------------------------< NodeIterator >---
    /**
     * @see javax.jcr.NodeIterator#nextNode()
     */
    public Node nextNode() {
        Node n = next;
        if (n == null) {
            throw new NoSuchElementException();
        }
        next = seekNext();
        position++;
        return n;
    }

    //------------------------------------------------------< RangeIterator >---
    /**
     * @see javax.jcr.RangeIterator#skip(long)
     */
    public void skip(long skipNum) {
        while (skipNum-- > 0) {
            next();
        }
    }

    /**
     * @see javax.jcr.RangeIterator#getSize()
     */
    public long getSize() {
        return -1;
    }

    /**
     * @see javax.jcr.RangeIterator#getPosition()
     */
    public long getPosition() {
        return position;
    }

    //--------------------------------------------------------------------------
    /**
     * 
     * @return
     */
    protected Node seekNext() {
        Node n = null;
        while (n == null && base.hasNext()) {
            Node nextRes = base.nextNode();
            if (filter.evaluate(nextRes)) {
                n = nextRes;
            }
        }
        return n;
    }
}