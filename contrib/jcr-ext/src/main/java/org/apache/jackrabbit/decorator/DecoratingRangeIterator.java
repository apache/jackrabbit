/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.decorator;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RangeIterator;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

/**
 * Range iterator that decorates all iterated objects. This class is used
 * as the base class of the various decorating iterator utility classes used
 * by the decorator layer.
 * <p>
 * All the method calls are delegated to the underlying iterator,
 * and best effort is made to decorate the objects returned by the
 * {@link #next() next()} method.
 */
public class DecoratingRangeIterator
        extends AbstractDecorator implements RangeIterator {

    /** The underlying iterator. */
    protected final RangeIterator iterator;

    /**
     * Creates a decorating iterator.
     *
     * @param factory decorator factory
     * @param session decorated session
     * @param iterator underlying iterator
     */
    public DecoratingRangeIterator(
            DecoratorFactory factory, Session session, RangeIterator iterator) {
        super(factory, session);
        this.iterator = iterator;
    }

    /**
     * Advances the underlying iterator.
     *
     * @param skipNum number of elements to skip
     * @see RangeIterator#skip(long)
     */
    public void skip(long skipNum) {
        iterator.skip(skipNum);
    }

    /**
     * Returns the size of the underlying iterator.
     *
     * @return size of the iterator
     * @see RangeIterator#getSize()
     */
    public long getSize() {
        return iterator.getSize();
    }

    /**
     * Returns the position of the underlying iterator.
     *
     * @return position of the iterator
     * @see RangeIterator#getPosition()
     */
    public long getPosition() {
        return iterator.getPosition();
    }

    /**
     * Checks whether the underlying iterator has more elements.
     *
     * @return <code>true</code> if more elements exist,
     *         <code>false</code> otherwise
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * Decorates and returns the next objects from the underlying iterator.
     *
     * @return decorated object
     * @throws UnsupportedOperationException if the returned object can not
     *                                       be decorated
     * @see java.util.Iterator#next()
     */
    public Object next() {
        Object object = iterator.next();
        if (object instanceof Version) {
            return factory.getVersionDecorator(session, (Version) object);
        } else if (object instanceof VersionHistory) {
            return factory.getVersionHistoryDecorator(session, (VersionHistory) object);
        } else if (object instanceof Node) {
            return factory.getNodeDecorator(session, (Node) object);
        } else if (object instanceof Property) {
            return factory.getPropertyDecorator(session, (Property) object);
        } else if (object instanceof Item) {
            return factory.getItemDecorator(session, (Item) object);
        } else {
            throw new UnsupportedOperationException("No decorator available for " + object);
        }
    }

    /**
     * Removes the current object from the underlying iterator.
     *
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        iterator.remove();
    }

}
