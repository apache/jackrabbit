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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RangeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionIterator;

import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>LazyItemIterator</code> is an id-based iterator that instantiates
 * the <code>Item</code>s only when they are requested.
 * <p>
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
    private final Iterator<? extends HierarchyEntry> iter;

    /**
     * The number of items.
     * Note, that the size may change over the time due to the lazy behaviour
     * of this iterator that may only upon iteration found out, that a
     * hierarchy entry has been invalidated or removed in the mean time.
     */
    private long size;
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
    public LazyItemIterator(ItemManager itemMgr, Iterator<? extends HierarchyEntry> hierarchyEntryIterator) {
        this.itemMgr = itemMgr;
        this.iter = hierarchyEntryIterator;
        if (hierarchyEntryIterator instanceof RangeIterator) {
            size = ((RangeIterator) hierarchyEntryIterator).getSize();
        } else {
            size = UNDEFINED_SIZE;
        }
        pos = 0;
        // fetch first item
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
                            Iterator<? extends ItemId> itemIds)
        throws ItemNotFoundException, RepositoryException {
        this.itemMgr = itemMgr;
        List<HierarchyEntry> entries = new ArrayList<HierarchyEntry>();
        while (itemIds.hasNext()) {
            ItemId id = itemIds.next();
            HierarchyEntry entry;
            if (id.denotesNode()) {
                entry = hierarchyMgr.getNodeEntry((NodeId) id);
            } else {
                entry = hierarchyMgr.getPropertyEntry((PropertyId) id);
            }
            entries.add(entry);
        }
        iter = entries.iterator();
        size = entries.size();
        pos = 0;
        // fetch first item
        next = prefetchNext();
    }

    /**
     * Prefetches next item.
     * <p>
     * {@link #next} is set to the next available item in this iterator or to
     * <code>null</code> in case there are no more items.
     */
    private Item prefetchNext() {
        Item nextItem = null;
        while (nextItem == null && iter.hasNext()) {
            HierarchyEntry entry = iter.next();
            try {
                nextItem = itemMgr.getItem(entry);
            } catch (RepositoryException e) {
                log.warn("Failed to fetch item " + entry.getName() + ", skipping.", e.getMessage());
                // reduce the size... and try the next one
                size--;
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
     * Returns the number of <code>Item</code>s in this iterator or -1 if the
     * size is unknown.
     * <p>
     * Note: The number returned by this method may differ from the number
     * of <code>Item</code>s actually returned by calls to hasNext() / getNextNode().
     * This is caused by the lazy instantiation behaviour of this iterator,
     * that may detect only upon iteration that an Item has been invalidated
     * or removed in the mean time. As soon as an invalid <code>Item</code> is
     * detected, the size of this iterator is adjusted.
     *
     * @return the number of <code>Item</code>s in this iterator.
     * @see RangeIterator#getSize()
     */
    public long getSize() {
        return size;
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
            HierarchyEntry entry = iter.next();
            // check if item exists but don't build Item instance.
            boolean itemExists = false;
            while(!itemExists){
                try{
                    itemExists = itemMgr.itemExists(entry);
                }catch(RepositoryException e){
                    log.warn("Failed to check that item {} exists",entry,e);
                }
                if(!itemExists){
                    log.debug("Ignoring nonexistent item {}", entry);
                    entry = iter.next();
                }
            }
        }
        // fetch final item (the one to be returned on next())
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
