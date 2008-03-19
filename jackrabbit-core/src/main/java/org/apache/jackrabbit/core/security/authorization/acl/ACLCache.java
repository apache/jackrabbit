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
package org.apache.jackrabbit.core.security.authorization.acl;

import org.apache.commons.collections.map.LinkedMap;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * ACLCache<br>
 * Caches already resolved ACLs for one workspace.<p>
 * The cache keeps per item the ACL if locally defined AND a reference to
 * the next id, which defines an ACL.
 * Thus if no entry is contained in the Cache a new one has to be build.
 *
 * @author tobi
 */
class ACLCache {

    /** the default logger */
    private static final Logger log = LoggerFactory.getLogger(ACLCache.class);

    /** the acl entries stored by the id of their applying item */
    private final Map entryByItemId = new HashMap();

    /** map of recently used leaf entries */
    private final LinkedMap lruMap = new LinkedMap();

    /** the acl entries stored by their id */
    private final Map entryByAclId = new HashMap();

    /** the maximum size of the cache */
    private final int maxSize;

    /** the name of this cache */
    private final String name;

    /**
     * creates a new ACLCache with the given name and a default maximal size.
     *
     * @param name
     */
    ACLCache(String name) {
        this(name, 0x10000);
    }

    /**
     * creates a new ACLCache with the given name
     *
     * @param name
     * @param maxSize
     */
    ACLCache(String name, int maxSize) {
        this.name = name;
        this.maxSize = maxSize;
    }

    /**
     * Return the effective ACL for the given item id.<br>
     *
     * @param id    of the Item the ACL should apply
     * @param touch if <code>true</code>, the item is pot. added to the lruMap
     *
     * @return the ACL or <code>null</code> if none is stored
     */
    ACLImpl getAcl(ItemId id, boolean touch) {
        Entry entry = (Entry) entryByItemId.get(id);
        if (entry == null) {
            return null;
        } else if (touch && !entry.hasChildren()) {
            // this is the only potential read-only access, so synchronize
            synchronized (lruMap) {
                lruMap.remove(id);
                lruMap.put(id, id);
            }
            if (log.isDebugEnabled()) {
                log.debug("Added entry to lruMap. {}", this);
            }
        }
        return entry.getAcl();
    }

    /**
     * Puts an ACL into this cache.<p>
     *
     * @param id       the acl applies
     * @param parentId id of the item the acl inherits from
     * @param acl
     */
    void cache(ItemId id, ItemId parentId, ACLImpl acl) throws RepositoryException {
        Entry entry = (Entry) entryByItemId.get(id);
        if (entry == null) {
            if (entryByItemId.size() > maxSize) {
                purge();
            }
            if (parentId == null) {
                new Entry(id, null, acl);
            } else {
                Entry parent = (Entry) entryByItemId.get(parentId);
                if (parent == null) {
                    throw new RepositoryException("Illegal state...parent ACL must be cached.");
                }
                new Entry(id, parent, acl);
            }
        } else {
            entry.setAcl(acl);
        }
    }

    /**
     * purges leaf-entries until 10% of max size is reached
     */
    private void purge() {
        // purge 10% of max size
        int goal = (maxSize * 90) / 100;
        int size = entryByItemId.size();
        while (entryByItemId.size() > goal && !lruMap.isEmpty()) {
            NodeId id = (NodeId) lruMap.remove(0);
            removeEntry(id);
        }
        if (log.isDebugEnabled()) {
            log.debug("Purged {} entries. {}", String.valueOf(size - entryByItemId.size()), this);
        }
    }

    /**
     * Invalidates the acl with the given id
     *
     * @param aclId
     */
    void invalidateEntry(NodeId aclId) {
        Entry entry = (Entry) entryByAclId.get(aclId);
        if (entry != null) {
            if (entry.parent != null) {
                entry.parent.invalidate();
            } else {
                entry.invalidate();
            }
        }
    }

    /**
     * Invalidates the acl that applies to the Item with the given id.
     *
     * @param itemId
     */
    void removeEntry(NodeId itemId) {
        Entry entry = (Entry) entryByItemId.get(itemId);
        if (entry != null) {
            entry.remove();
        }
    }

    /**
     * closes the cache and clears all maps
     */
    void close() {
        entryByAclId.clear();
        entryByItemId.clear();
        lruMap.clear();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "name=" + name + ", entries=" + entryByItemId.size() + ", leaves=" + lruMap.size();
    }

    //--------------------------------------------------------------------------
    /**
     * Entry class for the acl cache. Every entry represents a accessontrollable
     * item of the workspace. the entry might have an assosiated ACL. the
     * parent-child relationships of parent-child ACLs are also recorded in the
     * entry. please note, that this is not the same as the parent-child
     * relationship of the actual repository items, since not all items must be
     * access controllable.
     */
    private class Entry {

        /**
         * the id of the access controlled item
         */
        private final ItemId id;

        /**
         * the ACL for the access controlled item
         */
        private ACLImpl acl;

        /**
         * the parent entry or <code>null</code> if root
         */
        private final Entry parent;

        /**
         * the set of child entries or <code>null</code> if no children
         */
        private Set children = new HashSet();

        /**
         * Creates a new ACLEntry at adds it to the cache
         *
         * @param id
         * @param acl
         */
        private Entry(ItemId id, Entry parent, ACLImpl acl) {
            this.id = id;
            this.acl = acl;
            this.parent = parent;
            if (parent != null) {
                parent.attachChild(this);
            }
            entryByItemId.put(id, this);
            setAcl(acl);
            if (log.isDebugEnabled()) {
                log.debug("Added new entry.");
            }
        }

        /**
         * Attaches a child entry to this one
         *
         * @param child
         */
        private void attachChild(Entry child) {
            children.add(child);
            // remove from lruMap
            if (lruMap.remove(id) != null) {
                if (log.isDebugEnabled()) {
                    log.debug("attachChild removed item from lru map. {}", ACLCache.this);
                }
            }

        }

        /**
         * Detaches a child entry
         * @param child
         */
        private void detachChild(Entry child) {
            children.remove(child);
            if (children.isEmpty()) {
                lruMap.put(id, id);
                if (log.isDebugEnabled()) {
                    log.debug("detachChild added item to lru map. {}", ACLCache.this);
                }
            }
        }

        /**
         * sets the acl
         *
         * @param newAcl
         */
        private void setAcl(ACLImpl newAcl) {
            if (newAcl == acl) {
                // ignore
                return;
            }
            // test if an acl is already present -> informInvalid.
            if (acl != null) {
                entryByAclId.remove(acl.getId());
            }
            // set the acl field to the new value
            acl = newAcl;
            if (newAcl != null) {
                entryByAclId.put(newAcl.getId(), this);
            }
        }

        /**
         * checks if this entry has children
         * @return <code>true</code> if this enrty has children
         */
        private boolean hasChildren() {
            return !children.isEmpty();
        }

        /**
         * returns the assigned acl
         * @return the acl
         */
        private ACLImpl getAcl() {
            return acl;
        }

        /**
         * invalidates this entry, i.e. clears the assigned acl and recursivly
         * invalidates all its children.
         */
        private void invalidate() {
            setAcl(null);
            Iterator iter = children.iterator();
            while (iter.hasNext()) {
                ((Entry) iter.next()).invalidate();
            }
        }

        /**
         * removes this entry from the cache and recursivly all children.
         */
        private void remove() {
            entryByItemId.remove(id);
            lruMap.remove(id);

            Iterator iter = children.iterator();
            while (iter.hasNext()) {
                ((Entry) iter.next()).remove();
                iter = children.iterator(); // to avoid concurrent mod excp.
            }

            if (parent != null) {
                parent.detachChild(this);
            }
            setAcl(null);
            if (log.isDebugEnabled()) {
                log.debug("Removed entry. {}", ACLCache.this);
            }
        }

        //---------------------------------------------------------< Object >---
        /**
         * @see Object#hashCode()
         */
        public int hashCode() {
            return id.hashCode();
        }

        /**
         * @see Object#equals(Object)
         */
        public boolean equals(Object obj) {
            if (obj==this) {
                return true;
            }
            if (obj instanceof Entry) {
                return id.equals(((Entry)obj).id);
            }
            return false;
        }
    }
}
