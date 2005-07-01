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
package org.apache.jackrabbit.core;

import org.apache.log4j.Logger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <code>LazyItemIterator</code> is an id-based iterator that instantiates
 * the <code>Item</code>s only when they are requested.
 */
class LazyItemIterator implements NodeIterator, PropertyIterator {

    private static Logger log = Logger.getLogger(LazyItemIterator.class);

    /**
     * the item manager that is used to fetch the items
     */
    private final ItemManager itemMgr;

    /**
     * the list of item ids
     */
    private final List idList;

    /**
     * the position of the next item
     */
    private int pos = 0;

    /**
     * Creates a new <code>LazyItemIterator</code> instance.
     *
     * @param itemMgr item manager
     * @param idList  list of item id's
     */
    public LazyItemIterator(ItemManager itemMgr, List idList) {
        this(itemMgr, idList, false);
    }

    /**
     * Creates a new <code>LazyItemIterator</code> instance.
     *
     * @param itemMgr        item manager
     * @param idList         list of item id's
     * @param skipInexistent if <code>true</code> the id's of those items
     *                       that appear to be non-existent will be filtered
     *                       out silently; otherwise such entries will cause
     *                       a <code>NoSuchElementException</code> on
     *                       <code>{@link #next()}</code> .
     */
    public LazyItemIterator(ItemManager itemMgr, List idList,
                            boolean skipInexistent) {
        this.itemMgr = itemMgr;
        if (skipInexistent) {
            // check existence of all items first
            this.idList = new ArrayList();
            Iterator iter = idList.iterator();
            while (iter.hasNext()) {
                ItemId id = (ItemId) iter.next();
                if (itemMgr.itemExists(id)) {
                    this.idList.add(id);
                }
            }
        } else {
            this.idList = idList;
        }
    }

    //---------------------------------------------------------< NodeIterator >
    /**
     * {@inheritDoc}
     */
    public Node nextNode() {
        return (Node) next();
    }

    //-----------------------------------------------------< PropertyIterator >
    /**
     * {@inheritDoc}
     */
    public Property nextProperty() {
        return (Property) next();
    }

    //------------------------------------------------------< RangeIterator >---
    /**
     * {@inheritDoc}
     */
    public long getPosition() {
        return pos;
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() {
        return idList.size();
    }

    /**
     * {@inheritDoc}
     */
    public void skip(long skipNum) {
        if (skipNum < 0) {
            throw new IllegalArgumentException("skipNum must be a positive number");
        }
        if (pos + skipNum > idList.size()) {
            throw new NoSuchElementException("skipNum + pos greater than size");
        }
        pos += skipNum;
    }

    //-------------------------------------------------------------< Iterator >
    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        return pos < idList.size();
    }

    /**
     * {@inheritDoc}
     */
    public Object next() {
        if (pos >= idList.size()) {
            throw new NoSuchElementException();
        }
        ItemId id = (ItemId) idList.get(pos++);
        try {
            return itemMgr.getItem(id);
        } catch (RepositoryException e) {
            log.debug("failed to fetch item " + id, e);
            throw new NoSuchElementException(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException always since not implemented
     */
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
}
