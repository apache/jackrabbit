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
package org.apache.jackrabbit.core;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.state.ItemStateManager;
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
 * {@link #getSize()} might appear to be shrinking while iterating over the
 * items.
 * todo should getSize() better always return -1?
 *
 * @see #getSize()
 */
public class LazyItemIterator implements NodeIterator, PropertyIterator {

    /** Logger instance for this class */
    private static Logger log = LoggerFactory.getLogger(LazyItemIterator.class);

    /**
     * The session context used to access the repository.
     */
    private final SessionContext sessionContext;

    /** the item manager that is used to lazily fetch the items */
    private final ItemManager itemMgr;

    /** the list of item ids */
    private final List<ItemId> idList;

    /** parent node id (when returning children nodes) or <code>null</code> */
    private final NodeId parentId;

    /** the position of the next item */
    private int pos;

    /** prefetched item to be returned on <code>{@link #next()}</code> */
    private Item next;

    /**
     * Creates a new <code>LazyItemIterator</code> instance.
     *
     * @param sessionContext session context
     * @param idList  list of item id's
     */
    public LazyItemIterator(SessionContext sessionContext, List< ? extends ItemId> idList) {
        this(sessionContext, idList, null);
    }

    /**
     * Creates a new <code>LazyItemIterator</code> instance, additionally taking
     * a parent id as parameter. This version should be invoked to strictly return
     * children nodes of a node.
     *
     * @param sessionContext session context
     * @param idList  list of item id's
     * @param parentId parent id.
     */
    public LazyItemIterator(SessionContext sessionContext, List< ? extends ItemId> idList, NodeId parentId) {
        this.sessionContext = sessionContext;
        this.itemMgr = sessionContext.getSessionImpl().getItemManager();
        this.idList = new ArrayList<ItemId>(idList);
        this.parentId = parentId;
        // prefetch first item
        pos = 0;
        prefetchNext();
    }

    /**
     * Prefetches next item.
     * <p>
     * {@link #next} is set to the next available item in this iterator or to
     * <code>null</code> in case there are no more items.
     */
    private void prefetchNext() {
        // reset
        next = null;
        while (next == null && pos < idList.size()) {
            ItemId id = idList.get(pos);
            try {
                if (parentId != null) {
                    next = itemMgr.getNode((NodeId) id, parentId);
                } else {
                    next = itemMgr.getItem(id);
                }
            } catch (ItemNotFoundException e) {
                log.debug("ignoring nonexistent item " + id);
                // remove invalid id
                idList.remove(pos);

                // maybe fix the root cause
                if (parentId != null && sessionContext.getSessionImpl().autoFixCorruptions()) {
                    try {
                        // it might be an access right problem
                        // we need to check if the item doesn't exist in the ism
                        ItemStateManager ism = sessionContext.getItemStateManager();
                        if (!ism.hasItemState(id)) {
                            NodeImpl p = (NodeImpl) itemMgr.getItem(parentId);
                            p.removeChildNode((NodeId) id);
                            p.save();
                        }
                    } catch (RepositoryException e2) {
                        log.error("could not fix repository inconsistency", e);
                        // ignore
                    }
                }

                // try next
            } catch (AccessDeniedException e) {
                log.debug("ignoring nonexistent item " + id);
                // remove invalid id
                idList.remove(pos);
                // try next
            } catch (RepositoryException e) {
                log.error("failed to fetch item " + id + ", skipping...", e);
                // remove invalid id
                idList.remove(pos);
                // try next
            }
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

    //--------------------------------------------------------< RangeIterator >
    /**
     * {@inheritDoc}
     */
    public long getPosition() {
        return pos;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that the size of the iterator as reported by {@link #getSize()}
     * might appear to be shrinking while iterating because items that for
     * some reason cannot be retrieved through this iterator are silently
     * skipped, thus reducing the size of this iterator.
     *
     * todo better to always return -1?
     */
    public long getSize() {
        return idList.size();
    }

    /**
     * {@inheritDoc}
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
            if (pos >= idList.size()) {
                // skipped past last item
                throw new NoSuchElementException();
            }
            ItemId id = idList.get(pos);
            // eliminate invalid items from this iterator
            while (!itemMgr.itemExists(id)) {
                log.debug("ignoring nonexistent item " + id);
                // remove invalid id
                idList.remove(pos);
                if (pos >= idList.size()) {
                    // skipped past last item
                    throw new NoSuchElementException();
                }
                id = idList.get(pos);
            }
        }
        // prefetch final item (the one to be returned on next())
        pos++;
        prefetchNext();
    }

    //-------------------------------------------------------------< Iterator >
    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * {@inheritDoc}
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
     *
     * @throws UnsupportedOperationException always since not implemented
     */
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }
}
