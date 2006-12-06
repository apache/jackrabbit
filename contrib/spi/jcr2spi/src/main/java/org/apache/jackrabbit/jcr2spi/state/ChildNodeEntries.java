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
package org.apache.jackrabbit.jcr2spi.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.jcr2spi.state.entry.ChildNodeEntry;
import org.apache.jackrabbit.jcr2spi.state.entry.ChildNodeReference;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.commons.collections.list.AbstractLinkedList;
import org.apache.commons.collections.iterators.UnmodifiableIterator;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * <code>ChildNodeEntries</code> represents an insertion-ordered
 * collection of <code>ChildNodeEntry</code>s that also maintains
 * the index values of same-name siblings on insertion and removal.
 * <p/>
 * <code>ChildNodeEntries</code> also provides an unmodifiable
 * <code>Collection</code> view.
 */
final class ChildNodeEntries implements Collection {

    private static Logger log = LoggerFactory.getLogger(ChildNodeEntries.class);

    private final NodeState nodeState;

    /**
     * Linked list of {@link ChildNodeEntry} instances.
     */
    private final LinkedEntries entries = new LinkedEntries();

    /**
     * map used for lookup by name
     * (key=name, value=either a single {@link AbstractLinkedList.Node} or a
     * list of {@link AbstractLinkedList.Node}s which are sns entries)
     */
    private final Map nameMap = new HashMap();

    /**
     * Create <code>ChildNodeEntries</code> for the given node state.
     * 
     * @param nodeState
     */
    ChildNodeEntries(NodeState nodeState) {
        this.nodeState = nodeState;
    }

    /**
     * Create <code>ChildNodeEntries</code> for the given node state.
     *
     * @param nodeState
     */
    ChildNodeEntries(NodeState nodeState, ChildNodeEntries base) {
        this.nodeState = nodeState;
        for (Iterator it = base.iterator(); it.hasNext();) {
            ChildNodeEntry baseCne = (ChildNodeEntry) it.next();
            ChildNodeEntry cne = ChildNodeReference.create(nodeState, baseCne.getName(), baseCne.getUUID(), nodeState.isf, nodeState.idFactory);
            add(cne);
        }
    }

    /**
     * Returns true, if this ChildNodeEntries contains a entry that matches
     * the given name and either index or uuid:<br>
     * If <code>uuid</code> is not <code>null</code> the given index is
     * ignored since it is not required to identify a child node entry.
     * Otherwise the given index is used.
     *
     * @param name
     * @param index
     * @param uuid
     * @return
     */
    boolean contains(QName name, int index, String uuid) {
        if (uuid == null) {
            return contains(name, index);
        } else {
            return contains(name, uuid);
        }
    }

    /**
     *
     * @param name
     * @param index
     * @return
     */
    private boolean contains(QName name, int index) {
        if (!nameMap.containsKey(name) || index < Path.INDEX_DEFAULT) {
            // no matching child node entry
            return false;
        }
        Object o = nameMap.get(name);
        if (o instanceof List) {
            // SNS
            int listIndex = index - 1;
            return listIndex < ((List) o).size();
        } else {
            // single child node with this name -> matches only if request
            // index equals the default-index
            return index == Path.INDEX_DEFAULT;
        }
    }

    /**
     *
     * @param name
     * @param uuid
     * @return
     */
    private boolean contains(QName name, String uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException();
        }
        if (!nameMap.containsKey(name)) {
            // no matching child node entry
            return false;
        }
        Object o = nameMap.get(name);
        if (o instanceof List) {
            // SNS
            for (Iterator it = ((List) o).iterator(); it.hasNext(); ) {
                LinkedEntries.LinkNode n = (LinkedEntries.LinkNode) it.next();
                ChildNodeEntry cne = n.getChildNodeEntry();
                if (uuid.equals(cne.getUUID())) {
                    return true;
                }
            }
        } else {
            // single child node with this name
            ChildNodeEntry cne = ((LinkedEntries.LinkNode) o).getChildNodeEntry();
            return uuid.equals(cne.getUUID());
        }
        // no matching entry found
        return false;
    }

    /**
     * Returns the <code>ChildNodeEntry</code> for the given
     * <code>nodeState</code>. Note, that this method does not check if the
     * given childNodeEntry (and its attached NodeState) is still valid.
     *
     * @param childState the child node state for which a entry is searched.
     * @return the <code>ChildNodeEntry</code> or <code>null</code> if there
     * is no <code>ChildNodeEntry</code> for the given <code>NodeState</code>.
     */
    ChildNodeEntry get(NodeState childState) {
        Object o = nameMap.get(childState.getQName());
        if (o == null) {
            // no matching child node entry
            return null;
        }
        if (o instanceof List) {
            // has same name sibling
            for (Iterator it = ((List) o).iterator(); it.hasNext(); ) {
                LinkedEntries.LinkNode n = (LinkedEntries.LinkNode) it.next();
                ChildNodeEntry cne = n.getChildNodeEntry();
                // only check available child node entries
                try {
                    if (cne.isAvailable() && cne.getNodeState() == childState) {
                        return cne;
                    }
                } catch (ItemStateException e) {
                    log.warn("error retrieving a child node state", e);
                }
            }
        } else {
            // single child node with this name
            ChildNodeEntry cne = ((LinkedEntries.LinkNode) o).getChildNodeEntry();
            try {
                if (cne.isAvailable() && cne.getNodeState() == childState) {
                    return cne;
                }
            } catch (ItemStateException e) {
                log.warn("error retrieving a child node state", e);
            }
        }
        // not found
        return null;
    }

    /**
     * Returns a <code>List</code> of <code>ChildNodeEntry</code>s for the
     * given <code>nodeName</code>. This method does <b>not</b> filter out
     * removed <code>ChildNodeEntry</code>s!
     *
     * @param nodeName the child node name.
     * @return same name sibling nodes with the given <code>nodeName</code>.
     */
    List get(QName nodeName) {
        Object obj = nameMap.get(nodeName);
        if (obj == null) {
            return Collections.EMPTY_LIST;
        }
        if (obj instanceof List) {
            final List sns = (List) obj;
            // map entry is a list of siblings
            return Collections.unmodifiableList(new AbstractList() {

                public Object get(int index) {
                    return ((LinkedEntries.LinkNode) sns.get(index)).getChildNodeEntry();
                }

                public int size() {
                    return sns.size();
                }

                public Iterator iterator() {
                    return new Iterator() {

                        private Iterator iter = sns.iterator();

                        public void remove() {
                            throw new UnsupportedOperationException("remove");
                        }

                        public boolean hasNext() {
                            return iter.hasNext();
                        }

                        public Object next() {
                            return ((LinkedEntries.LinkNode) iter.next()).getChildNodeEntry();
                        }
                    };
                }
            });
        } else {
            // map entry is a single child node entry
            return Collections.singletonList(((LinkedEntries.LinkNode) obj).getChildNodeEntry());
        }
    }

    /**
     * Returns the <code>ChildNodeEntry</code> with the given
     * <code>nodeName</code> and <code>index</code>. This method ignores
     * <code>ChildNodeEntry</code>s which are marked removed!
     *
     * @param nodeName name of the child node entry.
     * @param index    the index of the child node entry.
     * @return the <code>ChildNodeEntry</code> or <code>null</code> if there
     *         is no such <code>ChildNodeEntry</code>.
     */
    ChildNodeEntry get(QName nodeName, int index) {
        if (index < Path.INDEX_DEFAULT) {
            throw new IllegalArgumentException("index is 1-based");
        }

        Object obj = nameMap.get(nodeName);
        if (obj == null) {
            return null;
        }
        if (obj instanceof List) {
            // map entry is a list of siblings
            List siblings = (List) obj;
            // filter out removed states
            for (Iterator it = siblings.iterator(); it.hasNext(); ) {
                ChildNodeEntry cne = ((LinkedEntries.LinkNode) it.next()).getChildNodeEntry();
                if (cne.isAvailable()) {
                    try {
                        if (cne.getNodeState().isValid()) {
                            index--;
                        } else {
                            // child node removed
                        }
                    } catch (ItemStateException e) {
                        // should never happen, cne.isAvailable() returned true
                    }
                } else {
                    // then this child node entry has never been accessed
                    // before and is assumed valid // TODO: check if correct.
                    index--;
                }
                if (index == 0) {
                    return cne;
                }
            }
        } else {
            // map entry is a single child node entry
            if (index == Path.INDEX_DEFAULT) {
                return ((LinkedEntries.LinkNode) obj).getChildNodeEntry();
            }
        }
        return null;
    }

    /**
     *
     * @param nodeName
     * @param uuid
     * @return
     * @throws IllegalArgumentException if the given uuid is null.
     */
    ChildNodeEntry get(QName nodeName, String uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException();
        }
        Iterator cneIter = (nodeName != null) ? get(nodeName).iterator() : entries.iterator();
        while (cneIter.hasNext()) {
            ChildNodeEntry cne = (ChildNodeEntry) cneIter.next();
            if (uuid.equals(cne.getUUID())) {
                return cne;
            }
        }
        return null;
    }

    /**
     * Insert a new childnode entry at the position indicated by index.
     *
     * @param nodeName
     * @param uuid
     * @param index
     * @return
     */
    ChildNodeEntry add(QName nodeName, String uuid, int index) {
        ChildNodeEntry cne = ChildNodeReference.create(nodeState, nodeName, uuid, nodeState.isf, nodeState.idFactory);
        add(cne, index);
        return cne;
    }

    /**
     * Adds a <code>childNode</code> to the end of the list.
     *
     * @param childState the <code>NodeState</code> to add.
     * @return the <code>ChildNodeEntry</code> which was created for
     *         <code>childNode</code>.
     */
    ChildNodeEntry add(NodeState childState) {
        ChildNodeEntry cne = ChildNodeReference.create(childState, nodeState.isf, nodeState.idFactory);
        add(cne);
        return cne;
    }
    
    /**
     * Adds a <code>ChildNodeEntry</code> to the end of the list.
     *
     * @param cne the <code>ChildNodeEntry</code> to add.
     */
    private void add(ChildNodeEntry cne) {
        QName nodeName = cne.getName();
        List siblings = null;
        Object obj = nameMap.get(nodeName);
        if (obj != null) {
            if (obj instanceof List) {
                // map entry is a list of siblings
                siblings = (ArrayList) obj;
            } else {
                // map entry is a single child node entry,
                // convert to siblings list
                siblings = new ArrayList();
                siblings.add(obj);
                nameMap.put(nodeName, siblings);
            }
        }

        LinkedEntries.LinkNode ln = entries.add(cne);

        if (siblings != null) {
            siblings.add(ln);
        } else {
            nameMap.put(nodeName, ln);
        }
    }

    /**
     * Adds a <code>ChildNodeEntry</code>. If an entry with the given index
     * already exists, the the new sibling is inserted before.
     *
     * @param cne the <code>ChildNodeEntry</code> to add.
     */
    private void add(ChildNodeEntry cne, int index) {
        QName nodeName = cne.getName();

        // retrieve ev. sibling node with same index
        // if index is 'undefined' behave just as '#add(ChildNodeEntry).
        LinkedEntries.LinkNode existing = (index < Path.INDEX_DEFAULT) ? null : getLinkNode(nodeName, index);

        // add new entry (same as #add(ChildNodeEntry)
        List siblings = null;
        Object obj = nameMap.get(nodeName);
        if (obj != null) {
            if (obj instanceof List) {
                // map entry is a list of siblings
                siblings = (ArrayList) obj;
            } else {
                // map entry is a single child node entry,
                // convert to siblings list
                siblings = new ArrayList();
                siblings.add(obj);
                nameMap.put(nodeName, siblings);
            }
        }

        LinkedEntries.LinkNode ln = entries.add(cne);
        if (siblings != null) {
            siblings.add(ln);
        } else {
            nameMap.put(nodeName, ln);
        }

        // if new entry must be inserted instead of appended at the end
        // reorder entries now
        if (existing != null) {
            reorder(obj, ln, existing);
        }
    }

    /**
     * Removes the child node entry with the given <code>nodeName</code> and
     * <code>index</code>.
     *
     * @param nodeName the name of the child node entry to remove.
     * @param index    the index of the child node entry to remove.
     * @return the removed <code>ChildNodeEntry</code> or <code>null</code>
     *         if there is no matching <code>ChildNodeEntry</code>.
     */
    ChildNodeEntry remove(QName nodeName, int index) {
        if (index < Path.INDEX_DEFAULT) {
            throw new IllegalArgumentException("index is 1-based");
        }

        Object obj = nameMap.get(nodeName);
        if (obj == null) {
            return null;
        }

        if (obj instanceof LinkedEntries.LinkNode) {
            // map entry is a single child node entry
            if (index != Path.INDEX_DEFAULT) {
                return null;
            }
            LinkedEntries.LinkNode ln = (LinkedEntries.LinkNode) obj;
            nameMap.remove(nodeName);
            // remove LinkNode from entries
            ln.remove();
            return ln.getChildNodeEntry();
        }

        // map entry is a list of siblings
        List siblings = (List) obj;
        if (index > siblings.size()) {
            return null;
        }

        // remove from siblings list
        LinkedEntries.LinkNode ln = (LinkedEntries.LinkNode) siblings.remove(index - 1);
        ChildNodeEntry removedEntry = ln.getChildNodeEntry();
        // remove from ordered entries
        ln.remove();

        // clean up name lookup map if necessary
        if (siblings.size() == 0) {
            // no more entries with that name left:
            // remove from name lookup map as well
            nameMap.remove(nodeName);
        } else if (siblings.size() == 1) {
            // just one entry with that name left:
            // discard siblings list and update name lookup map accordingly
            nameMap.put(nodeName, siblings.get(0));
        }

        // we're done
        return removedEntry;
    }

    /**
     * Removes the child node entry refering to the node state.
     *
     * @param nodeState the node state whose entry is to be removed.
     * @return the removed entry or <code>null</code> if there is no such entry.
     */
    ChildNodeEntry remove(NodeState nodeState) {
        ChildNodeEntry entry = null;
        for (Iterator it = get(nodeState.getQName()).iterator(); it.hasNext(); ) {
            ChildNodeEntry tmp = (ChildNodeEntry) it.next();
            try {
                if (tmp.isAvailable() && tmp.getNodeState() == nodeState) {
                    entry = tmp;
                    break;
                }
            } catch (ItemStateException e) {
                log.warn("error accessing child node state: " + e.getMessage());
            }
        }
        if (entry != null) {
            return remove(entry.getName(), entry.getIndex());
        }
        return entry;
    }

    /**
     * Reorders an existing <code>NodeState</code> before another
     * <code>NodeState</code>. If <code>beforeNode</code> is
     * <code>null</code> <code>insertNode</code> is moved to the end of the
     * child node entries.
     *
     * @param insertNode the node state to move.
     * @param beforeNode the node state where <code>insertNode</code> is
     * reordered to.
     * @throws NoSuchItemStateException if <code>insertNode</code> or
     * <code>beforeNode</code> does not have a <code>ChildNodeEntry</code>
     * in this <code>ChildNodeEntries</code>.
     */
    void reorder(NodeState insertNode, NodeState beforeNode) throws NoSuchItemStateException {
        Object insertObj = nameMap.get(insertNode.getQName());
        // the link node to move
        LinkedEntries.LinkNode insertLN = getLinkNode(insertNode);
        // the link node where insertLN is ordered before
        LinkedEntries.LinkNode beforeLN = (beforeNode != null) ? getLinkNode(beforeNode) : null;

        reorder(insertObj, insertLN, beforeLN);
    }

    /**
     *
     * @param insertObj
     * @param insertLN
     * @param beforeLN
     */
    private void reorder(Object insertObj, LinkedEntries.LinkNode insertLN, LinkedEntries.LinkNode beforeLN) {
        if (insertObj instanceof List) {
            // adapt name lookup lists
            List insertList = (List) insertObj;
            if (beforeLN == null) {
                // simply move to end of list
                insertList.remove(insertLN);
                insertList.add(insertLN);
            } else {
                // move based on position of beforeLN
                // count our same name siblings until we reach beforeLN
                int snsCount = 0;
                QName insertName = insertLN.getChildNodeEntry().getName();
                for (Iterator it = entries.linkNodeIterator(); it.hasNext(); ) {
                    LinkedEntries.LinkNode ln = (LinkedEntries.LinkNode) it.next();
                    if (ln == beforeLN) {
                        insertList.remove(insertLN);
                        insertList.add(snsCount, insertLN);
                        break;
                    } else if (ln == insertLN) {
                        // do not increment snsCount for node to reorder
                    } else if (ln.getChildNodeEntry().getName().equals(insertName)) {
                        snsCount++;
                    }
                }
            }
        } else {
            // no same name siblings -> nothing to do.
        }

        // reorder in linked list
        entries.reorderNode(insertLN, beforeLN);
    }

    /**
     * If the given child state got a (new) uuid assigned or its removed,
     * its childEntry must be adjusted.
     *
     * @param childState
     */
    void replaceEntry(NodeState childState) {
        // NOTE: test if child-state needs to get a new entry not checked here.
        try {
            LinkedEntries.LinkNode ln = getLinkNode(childState);
            ChildNodeEntry newCne = ChildNodeReference.create(childState, nodeState.isf, nodeState.idFactory);
            entries.replaceNode(ln, newCne);
        } catch (NoSuchItemStateException e) {
            // should never occur.
            log.error("Internal Error: ", e);
        }
    }

    /**
     * Returns the matching <code>LinkNode</code> from a list or a single
     * <code>LinkNode</code>. This method will throw <code>NoSuchItemStateException</code>
     * if none of the entries matches either due to missing entry for given
     * state name or due to missing availability of the <code>ChildNodeEntry</code>.
     *
     * @param nodeState the <code>NodeState</code> that is compared to the
     * resolution of any <code>ChildNodeEntry</code> that matches by name.
     * @return the matching <code>LinkNode</code>.
     * @throws NoSuchItemStateException if none of the <code>LinkNode</code>s
     * matches.
     */
    private LinkedEntries.LinkNode getLinkNode(NodeState nodeState)
        throws NoSuchItemStateException {
        Object listOrLinkNode = nameMap.get(nodeState.getQName());
        if (listOrLinkNode == null) {
            // no matching child node entry
            throw new NoSuchItemStateException(nodeState.getQName().toString());
        }

        if (listOrLinkNode instanceof List) {
            // has same name sibling
            for (Iterator it = ((List) listOrLinkNode).iterator(); it.hasNext();) {
                LinkedEntries.LinkNode n = (LinkedEntries.LinkNode) it.next();
                ChildNodeEntry cne = n.getChildNodeEntry();
                // only check available child node entries
                try {
                    if (cne.isAvailable() && cne.getNodeState() == nodeState) {
                        return n;
                    }
                } catch (ItemStateException e) {
                    log.warn("error retrieving a child node state", e);
                }
            }
        } else {
            // single child node with this name
            ChildNodeEntry cne = ((LinkedEntries.LinkNode) listOrLinkNode).getChildNodeEntry();
            try {
                if (cne.isAvailable() && cne.getNodeState() == nodeState) {
                    return (LinkedEntries.LinkNode) listOrLinkNode;
                }
            } catch (ItemStateException e) {
                log.warn("error retrieving a child node state", e);
            }
        }
        throw new NoSuchItemStateException(nodeState.getQName().toString());
    }

    /**
     * Returns the matching <code>LinkNode</code> from a list or a single
     * <code>LinkNode</code>. This method will return <code>null</code>
     * if none of the entries matches.
     *
     * @param name
     * @param index
     * @return the matching <code>LinkNode</code> or <code>null</code>.
     */
    private LinkedEntries.LinkNode getLinkNode(QName name, int index) {
        Object listOrLinkNode = nameMap.get(name);
        if (listOrLinkNode == null) {
            // no matching child node entry
            return null;
        }

        if (listOrLinkNode instanceof List) {
            // has same name sibling -> check if list size matches
            int listIndex = index - 1;
            List lnList = (List) listOrLinkNode;
            if (listIndex < lnList.size()) {
                return (LinkedEntries.LinkNode) lnList.get(listIndex);
            }
        } else if (index == Path.INDEX_DEFAULT) {
            // single child node with this name -> matches is requested index
            // equals to the default index.
            return (LinkedEntries.LinkNode) listOrLinkNode;
        }

        // no matching entry
        return null;
    }
    //--------------------------------------------< unmodifiable Collection >---
    /**
     * @see Collection#contains(Object)
     */
    public boolean contains(Object o) {
        if (o instanceof ChildNodeEntry) {
            // narrow down to same name sibling nodes and check list
            return get(((ChildNodeEntry) o).getName()).contains(o);
        } else {
            return false;
        }
    }

    /**
     * @see Collection#containsAll(Collection)
     */
    public boolean containsAll(Collection c) {
        Iterator iter = c.iterator();
        while (iter.hasNext()) {
            if (!contains(iter.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * @see Collection#isEmpty()
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * @see Collection#iterator()
     */
    public Iterator iterator() {
        return UnmodifiableIterator.decorate(entries.iterator());
    }

    /**
     * @see Collection#size()
     */
    public int size() {
        return entries.size();
    }

    /**
     * @see Collection#toArray()
     */
    public Object[] toArray() {
        ChildNodeEntry[] array = new ChildNodeEntry[size()];
        return toArray(array);
    }

    /**
     * @see Collection#toArray(Object[])
     */
    public Object[] toArray(Object[] a) {
        if (!a.getClass().getComponentType().isAssignableFrom(ChildNodeEntry.class)) {
            throw new ArrayStoreException();
        }
        if (a.length < size()) {
            a = new ChildNodeEntry[size()];
        }
        Iterator iter = entries.iterator();
        int i = 0;
        while (iter.hasNext()) {
            a[i++] = iter.next();
        }
        while (i < a.length) {
            a[i++] = null;
        }
        return a;
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @see Collection#add(Object)
     */
    public boolean add(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @see Collection#addAll(Collection)
     */
    public boolean addAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @see Collection#clear()
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @see Collection#remove(Object)
     */
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @see Collection#removeAll(Collection)
     */
    public boolean removeAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws <code>UnsupportedOperationException</code>.
     *
     * @see Collection#retainAll(Collection)
     */
    public boolean retainAll(Collection c) {
        throw new UnsupportedOperationException();
    }

    //-------------------------------------------------< AbstractLinkedList >---
    /**
     * An implementation of a linked list which provides access to the internal
     * LinkNode which links the entries of the list.
     */
    private static final class LinkedEntries extends AbstractLinkedList {

        LinkedEntries() {
            super();
            init();
        }

        /**
         * Adds a child node entry to this list.
         *
         * @param cne the child node entry to add.
         * @return the LinkNode which refers to the added <code>ChildNodeEntry</code>.
         */
        LinkNode add(ChildNodeEntry cne) {
            LinkNode ln = (LinkNode) createNode(cne);
            addNode(ln, header);
            return ln;
        }

        /**
         * Reorders an existing <code>LinkNode</code> before another existing
         * <code>LinkNode</code>. If <code>before</code> is <code>null</code>
         * the <code>insert</code> node is moved to the end of the list.
         *
         * @param insert the node to reorder.
         * @param before the node where to reorder node <code>insert</code>.
         */
        void reorderNode(LinkNode insert, LinkNode before) {
            removeNode(insert);
            if (before == null) {
                addNode(insert, header);
            } else {
                addNode(insert, before);
            }
        }

        /**
         * Replace the value of the given LinkNode with a new childNodeEntry
         * value.
         *
         * @param node
         * @param value
         */
        void replaceNode(LinkNode node, ChildNodeEntry value) {
            updateNode(node, value);
        }

        /**
         * Create a new <code>LinkNode</code> for a given {@link ChildNodeEntry}
         * <code>value</code>.
         *
         * @param value a child node entry.
         * @return a wrapping {@link LinkedEntries.LinkNode}.
         * @see AbstractLinkedList#createNode(Object)
         */
        protected Node createNode(Object value) {
            return new LinkNode(value);
        }

        /**
         * @return a new <code>LinkNode</code>.
         * @see AbstractLinkedList#createHeaderNode()
         */
        protected Node createHeaderNode() {
            return new LinkNode();
        }

        /**
         * Returns an iterator over all
         * @return
         */
        Iterator linkNodeIterator() {
            return new Iterator() {

                private LinkNode next = ((LinkNode) header).getNextLinkNode();

                private int expectedModCount = modCount;

                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }

                public boolean hasNext() {
                    if (expectedModCount != modCount) {
                        throw new ConcurrentModificationException();
                    }
                    return next != header;
                }

                public Object next() {
                    if (expectedModCount != modCount) {
                        throw new ConcurrentModificationException();
                    }
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    LinkNode n = next;
                    next = next.getNextLinkNode();
                    return n;
                }
            };
        }

        //----------------------------------------------------------------------

        /**
         * Extends the <code>AbstractLinkedList.Node</code>.
         */
        private final class LinkNode extends AbstractLinkedList.Node {

            protected LinkNode() {
                super();
            }

            protected LinkNode(Object value) {
                super(value);
            }

            /**
             * @return the wrapped <code>ChildNodeEntry</code>.
             */
            public ChildNodeEntry getChildNodeEntry() {
                return (ChildNodeEntry) super.getValue();
            }

            /**
             * Removes this <code>LinkNode</code> from the linked list.
             */
            public void remove() {
                removeNode(this);
            }

            /**
             * @return the next LinkNode.
             */
            public LinkNode getNextLinkNode() {
                return (LinkNode) super.getNextNode();
            }
        }
    }
}