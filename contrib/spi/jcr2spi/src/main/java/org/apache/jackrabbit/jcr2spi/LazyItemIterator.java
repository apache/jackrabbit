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
package org.apache.jackrabbit.jcr2spi;

import org.apache.jackrabbit.jcr2spi.state.ItemState;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RangeIterator;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.Version;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * <code>LazyItemIterator</code> is an id-based iterator that instantiates
 * the <code>Item</code>s only when they are requested.
 * <p/>
 * <strong>Important:</strong> <code>Item</code>s that appear to be nonexistent
 * for some reason (e.g. because of insufficient access rights or because they
 * have been removed since the iterator has been retrieved) are silently
 * skipped. As a result the size of the iterator as reported by
 * {@link #getSize()} always returns -1.
 */
public class LazyItemIterator implements NodeIterator, PropertyIterator, VersionIterator {

    /** Logger instance for this class */
    private static Logger log = LoggerFactory.getLogger(LazyItemIterator.class);

    private static final long UNDEFINED_SIZE = -1;

    /** the item manager that is used to lazily fetch the items */
    private final ItemManager itemMgr;

    /** the list of item states */
    private final List stateList;

    /** the position of the next item */
    private int pos;

    /** prefetched item to be returned on <code>{@link #next()}</code> */
    private Item next;

    /**
     * Creates a new <code>LazyItemIterator</code> instance.
     *
     * @param itemMgr item manager
     * @param itemStates Collection of item states
     */
    public LazyItemIterator(ItemManager itemMgr, Collection itemStates) {
        this.itemMgr = itemMgr;
        this.stateList = new ArrayList(itemStates);
        // prefetch first item
        pos = 0;
        prefetchNext();
    }

    /**
     * Prefetches next item.
     * <p/>
     * {@link #next} is set to the next available item in this iterator or to
     * <code>null</code> in case there are no more items.
     */
    private void prefetchNext() {
        // reset
        next = null;
        while (next == null && pos < stateList.size()) {
            ItemState state = (ItemState) stateList.get(pos);
            try {
                next = itemMgr.getItem(state);
            } catch (ItemNotFoundException e) {
                log.debug("ignoring nonexistent item " + state);
                // remove invalid id
                stateList.remove(pos);
                // try next
            } catch (RepositoryException e) {
                log.error("failed to fetch item " + state + ", skipping...", e);
                // remove invalid id
                stateList.remove(pos);
                // try next
            }
        }
    }

    //-------------------------------------------------------< NodeIterator >---
    /**
     * {@inheritDoc}
     * @see NodeIterator#nextNode()
     */
    public Node nextNode() {
        return (Node) next();
    }

    //---------------------------------------------------< PropertyIterator >---
    /**
     * {@inheritDoc}
     * @see PropertyIterator#nextProperty()
     */
    public Property nextProperty() {
        return (Property) next();
    }

    //----------------------------------------------------< VersionIterator >---
    /**
     * {@inheritDoc}
     * @see VersionIterator#nextVersion()
     */
    public Version nextVersion() {
        return (Version) next();
    }

    //------------------------------------------------------< RangeIterator >---
    /**
     * {@inheritDoc}
     * @see javax.jcr.RangeIterator#getPosition()
     */
    public long getPosition() {
        return pos;
    }

    /**
     * Always returns -1.
     *
     * @return always returns -1.
     * @see RangeIterator#getSize()
     */
    public long getSize() {
        // Always returns -1, since the original list may contains items that
        // are not accessible due to access constraints. -1 seems preferable
        // to returning a size that is not correct.
        return UNDEFINED_SIZE;
    }

    /**
     * {@inheritDoc}
     * @see RangeIterator#skip(long)
     */
    public void skip(long skipNum) {
        if (skipNum < 0) {
            throw new IllegalArgumentException("skipNum must not be negative");
        }
        if (skipNum == 0) {
            return;
        }
        if (next == null) {
            throw new NoSuchElementException();
        }

        // reset
        next = null;
        // skip the first (skipNum - 1) items without actually retrieving them
        while (--skipNum > 0) {
            pos++;
            if (pos >= stateList.size()) {
                // skipped past last item
                throw new NoSuchElementException();
            }
            ItemState state = (ItemState) stateList.get(pos);
            // eliminate invalid items from this iterator
            while (!itemMgr.itemExists(state)) {
                log.debug("ignoring nonexistent item " + state);
                // remove invalid id
                stateList.remove(pos);
                if (pos >= stateList.size()) {
                    // skipped past last item
                    throw new NoSuchElementException();
                }
                state = (ItemState) stateList.get(pos);
                // try next
                continue;
            }
        }
        // prefetch final item (the one to be returned on next())
        pos++;
        prefetchNext();
    }

    //-----------------------------------------------------------< Iterator >---
    /**
     * {@inheritDoc}
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * {@inheritDoc}
     * @see Iterator#next()
     */
    public Object next() {
        if (next == null) {
            throw new NoSuchElementException();
        }
        Item item = next;
        pos++;
        prefetchNext();
        return item;
    }

    /**
     * {@inheritDoc}
     * @see Iterator#remove()
     *
     * @throws UnsupportedOperationException always since removal is not implemented.
     */
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
}
