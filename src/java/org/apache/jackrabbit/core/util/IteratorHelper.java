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
package org.apache.jackrabbit.core.util;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import java.util.Collection;
import java.util.Iterator;

/**
 * <code>IteratorHelper</code> is a utility class which
 * wraps an iterator and implements the various typed iterator
 * interfaces.
 */
public class IteratorHelper
        implements NodeIterator, PropertyIterator, NodeTypeIterator, StringIterator {

    static final long UNDETERMINED_SIZE = -1;

    private final Iterator iter;
    private long size;
    private long pos;

    /**
     * Constructs an <code>IteratorHelper</code> which is backed
     * by a <code>java.util.Collection</code>.
     *
     * @param c collection which should be iterated over.
     */
    public IteratorHelper(Collection c) {
        this(c.iterator());
        size = c.size();
    }

    /**
     * Constructs an <code>IteratorHelper</code> which is wrapping
     * a <code>java.util.Iterator</code>.
     *
     * @param iter iterator which should be wrapped.
     */
    public IteratorHelper(Iterator iter) {
        this.iter = iter;
        pos = 0;
        size = UNDETERMINED_SIZE;
    }

    /**
     * @see RangeIterator#skip(long)
     */
    public void skip(long skipNum) {
        while (skipNum-- > 0) {
            next();
        }
    }

    /**
     * @see RangeIterator#getSize()
     */
    public long getSize() {
        return size;
    }

    /**
     * @see RangeIterator#getPos()
     */
    public long getPos() {
        return pos;
    }

    /**
     * @see Iterator#hasNext()
     */
    public boolean hasNext() {
        return iter.hasNext();
    }

    /**
     * @see Iterator#next()
     */
    public Object next() {
        // all typed nextXXX methods should
        // delegate to this method
        Object obj = iter.next();
        // increment position
        pos++;
        return obj;
    }

    /**
     * @see Iterator#remove()
     */
    public void remove() {
        iter.remove();
    }

    /**
     * @see NodeIterator#nextNode()
     */
    public Node nextNode() {
        return (Node) next();
    }

    /**
     * @see PropertyIterator#nextProperty()
     */
    public Property nextProperty() {
        return (Property) next();
    }

    /**
     * @see NodeTypeIterator#nextNodeType
     */
    public NodeType nextNodeType() {
        return (NodeType) next();
    }

    /**
     * @see StringIterator#nextString()
     */
    public String nextString() {
        return (String) next();
    }
}
