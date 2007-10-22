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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.spi.ItemId;

import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RangeIterator;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.Version;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

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

    /** Iterator over HierarchyEntry elements */
    private final Iterator iter;

    /** the position of the next item */
    private int pos;

    /** prefetched item to be returned on <code>{@link #next()}</code> */
    private Item next;

    /**
     * Creates a new <code>LazyItemIterator</code> instance.
     *
     * @param itemMgr item manager
     * @param hierarchyEntryIterator Iterator over HierarchyEntries
     */
    public LazyItemIterator(ItemManager itemMgr, Iterator hierarchyEntryIterator) {
        this.itemMgr = itemMgr;
        this.iter = hierarchyEntryIterator;
        // prefetch first item
        pos = 0;
        next = prefetchNext();
    }

    /**
     * Creates a new <code>LazyItemIterator</code> instance.
     *
     * @param itemMgr
     * @param hierarchyMgr
     * @param itemIds
     */
    public LazyItemIterator(ItemManager itemMgr, HierarchyManager hierarchyMgr,
                            Iterator itemIds)
        throws ItemNotFoundException, RepositoryException {
        this.itemMgr = itemMgr;
        List entries = new ArrayList();
        while (itemIds.hasNext()) {
            ItemId id = (ItemId) itemIds.next();
            entries.add(hierarchyMgr.getHierarchyEntry(id));
        }
        this.iter = entries.iterator();

        // prefetch first item
        pos = 0;
        next = prefetchNext();
    }

    /**
     * Prefetches next item.
     * <p/>
     * {@link #next} is set to the next available item in this iterator or to
     * <code>null</code> in case there are no more items.
     */
    private Item prefetchNext() {
        Item nextItem = null;
        while (nextItem == null && iter.hasNext()) {
            HierarchyEntry entry = (HierarchyEntry) iter.next();
            try {
                nextItem = itemMgr.getItem(entry);
            } catch (ItemNotFoundException e) {
                log.debug("Ignoring nonexistent item " + entry);
                // try the next
            } catch (RepositoryException e) {
                log.error("failed to fetch item " + entry + ", skipping...", e);
                // try the next
            }
        }
        return nextItem;
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
        // Always returns -1, since the entry-iterator may contains items that
        // are not accessible due to access constraints. -1 seems preferable
        // to returning a size that is not correct.
        return LazyItemIterator.UNDEFINED_SIZE;
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

        // skip the first (skipNum - 1) items without actually retrieving them
        while (--skipNum > 0) {
            pos++;
            HierarchyEntry entry = (HierarchyEntry) iter.next();
            // check if item exists but don't build Item instance.
            while (!itemMgr.itemExists(entry)) {
                log.debug("Ignoring nonexistent item " + entry);
                entry = (HierarchyEntry) iter.next();
            }
        }
        // prefetch final item (the one to be returned on next())
        pos++;
        next = prefetchNext();
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
        next = prefetchNext();
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
