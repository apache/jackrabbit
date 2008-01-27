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
package org.apache.jackrabbit.jcr2spi.hierarchy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.commons.collections.list.AbstractLinkedList;

import javax.jcr.RepositoryException;
import javax.jcr.ItemNotFoundException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.lang.ref.WeakReference;
import java.lang.ref.Reference;

/**
 * <code>ChildNodeEntriesImpl</code> implements a memory sensitive implementation
 * of the <code>ChildNodeEntries</code> interface.
 */
final class ChildNodeEntriesImpl implements ChildNodeEntries {

    private static Logger log = LoggerFactory.getLogger(ChildNodeEntriesImpl.class);

    private int status = STATUS_OK;

    /**
     * Linked list of {@link NodeEntry} instances.
     */
    private final LinkedEntries entries;

    /**
     * Map used for lookup by name.
     */
    private final NameMap entriesByName;

    private final NodeEntry parent;
    private final EntryFactory factory;

    /**
     * Create a new <code>ChildNodeEntries</code> collection
     */
    ChildNodeEntriesImpl(NodeEntry parent, EntryFactory factory) throws ItemNotFoundException, RepositoryException {
        entriesByName = new NameMap();
        entries = new LinkedEntries();

        this.parent = parent;
        this.factory = factory;

        if (parent.getStatus() == Status.NEW || Status.isTerminal(parent.getStatus())) {
            return; // cannot retrieve child-entries from persistent layer
        }

        NodeId id = parent.getWorkspaceId();
        Iterator it = factory.getItemStateFactory().getChildNodeInfos(id);
        // simply add all child entries to the empty collection
        while (it.hasNext()) {
            ChildInfo ci = (ChildInfo) it.next();
            NodeEntry entry = factory.createNodeEntry(parent, ci.getName(), ci.getUniqueID());
            add(entry, ci.getIndex());
        }
    }

    /**
     * @see ChildNodeEntries#getStatus()
     */
    public int getStatus() {
        return status;
    }

    /**
     * Mark <code>ChildNodeEntries</code> in order to force reloading the
     * entries.
     *
     * @see ChildNodeEntries#setStatus(int)
     */
    public void setStatus(int status) {
        if (status == STATUS_INVALIDATED || status == STATUS_OK) {
            this.status = status;
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * @see ChildNodeEntries#reload()
     */
    public synchronized void reload() throws ItemNotFoundException, RepositoryException {
        if (status == STATUS_OK ||
            parent.getStatus() == Status.NEW || Status.isTerminal(parent.getStatus())) {
            // nothing to do
            return;
        }

        NodeId id = parent.getWorkspaceId();
        Iterator it = factory.getItemStateFactory().getChildNodeInfos(id);
        // create list from all ChildInfos (for multiple loop)
        List cInfos = new ArrayList();
        while (it.hasNext()) {
            cInfos.add(it.next());
        }
        // first make sure the ordering of all existing entries is ok
        NodeEntry entry = null;
        for (it = cInfos.iterator(); it.hasNext();) {
            ChildInfo ci = (ChildInfo) it.next();
            NodeEntry nextEntry = get(ci);
            if (nextEntry != null) {
                if (entry != null) {
                    reorder(entry, nextEntry);
                }
                entry = nextEntry;
            }
        }
        // then insert the 'new' entries
        List newEntries = new ArrayList();
        for (it = cInfos.iterator(); it.hasNext();) {
            ChildInfo ci = (ChildInfo) it.next();
            NodeEntry beforeEntry = get(ci);
            if (beforeEntry == null) {
                NodeEntry ne = factory.createNodeEntry(parent, ci.getName(), ci.getUniqueID());
                newEntries.add(ne);
            } else {
                // insert all new entries from the list BEFORE the existing
                // 'nextEntry'. Then clear the list.
                for (int i = 0; i < newEntries.size(); i++) {
                    add((NodeEntry) newEntries.get(i), beforeEntry);
                }
                newEntries.clear();
            }
        }
        // deal with new entries at the end
        for (int i = 0; i < newEntries.size(); i++) {
            add((NodeEntry) newEntries.get(i));
        }
        // finally reset the status
        setStatus(ChildNodeEntries.STATUS_OK);
    }

    /**
     * @see ChildNodeEntries#iterator()
     */
    public Iterator iterator() {
        List l = new ArrayList(entries.size());
        for (Iterator it = entries.linkNodeIterator(); it.hasNext();) {
            l.add(((LinkedEntries.LinkNode)it.next()).getNodeEntry());
        }
        return Collections.unmodifiableList(l).iterator();
    }

    /**
     * @see ChildNodeEntries#get(Name)
     */
    public List get(Name nodeName) {
        return entriesByName.getList(nodeName);
    }

    /**
     * @see ChildNodeEntries#get(Name, int)
     */
    public NodeEntry get(Name nodeName, int index) {
        if (index < Path.INDEX_DEFAULT) {
            throw new IllegalArgumentException("index is 1-based");
        }
        return entriesByName.getNodeEntry(nodeName, index);
    }

    /**
     * @see ChildNodeEntries#get(Name, String)
     */
    public NodeEntry get(Name nodeName, String uniqueID) {
        if (uniqueID == null || nodeName == null) {
            throw new IllegalArgumentException();
        }
        Iterator cneIter = get(nodeName).iterator();
        while (cneIter.hasNext()) {
            NodeEntry cne = (NodeEntry) cneIter.next();
            if (uniqueID.equals(cne.getUniqueID())) {
                return cne;
            }
        }
        return null;
    }

    /**
     * @see ChildNodeEntries#get(ChildInfo)
     */
    public NodeEntry get(ChildInfo childInfo) {
        String uniqueID = childInfo.getUniqueID();
        NodeEntry child = null;
        if (uniqueID != null) {
            child = get(childInfo.getName(), uniqueID);
        }
        // try to load the child entry by name and index.
        // this is required in case of a null uniqueID OR if the child entry has
        // been created but never been resolved and therefore the uniqueID might
        // be unknown.
        if (child == null) {
            int index = childInfo.getIndex();
            child = entriesByName.getNodeEntry(childInfo.getName(), index);
        }
        return child;
    }

    /**
     * Adds a <code>NodeEntry</code> to the end of the list. Same as
     * {@link #add(NodeEntry, int)}, where the index is {@link Path#INDEX_UNDEFINED}.
     *
     * @param cne the <code>NodeEntry</code> to add.
     * @see ChildNodeEntries#add(NodeEntry)
     */
     public void add(NodeEntry cne) {
        internalAdd(cne, Path.INDEX_UNDEFINED);
    }

    /**
     * @see ChildNodeEntries#add(NodeEntry, int)
     */
    public void add(NodeEntry cne, int index) {
        if (index < Path.INDEX_UNDEFINED) {
            throw new IllegalArgumentException("Invalid index" + index);
        }
        internalAdd(cne, index);
    }

    /**
     *
     * @param entry
     * @param index
     * @return
     */
    private LinkedEntries.LinkNode internalAdd(NodeEntry entry, int index) {
        Name nodeName = entry.getName();

        // retrieve ev. sibling node with same index. if index is 'undefined'
        // the existing entry is always null and no reordering occurs.
        LinkedEntries.LinkNode existing = null;
        if (index >= Path.INDEX_DEFAULT) {
            existing = entriesByName.getLinkNode(nodeName, index);
        }

        // in case index greater than default -> create intermediate entries.
        // TODO: TOBEFIXED in case of orderable node the order in the 'linked-entries' must be respected.
        for (int i = Path.INDEX_DEFAULT; i < index; i++) {
            LinkedEntries.LinkNode previous = entriesByName.getLinkNode(nodeName, i);
            if (previous == null) {
                NodeEntry sibling = factory.createNodeEntry(parent, nodeName, null);
                internalAdd(sibling, i);
            }
        }

        // add new entry
        LinkedEntries.LinkNode ln = entries.add(entry);
        entriesByName.put(nodeName, ln);

        // reorder the child entries if, the new entry must be inserted rather
        // than appended at the end of the list.
        if (existing != null) {
            reorder(nodeName, ln, existing);
        }
        return ln;
    }

    /**
     * @see ChildNodeEntries#add(NodeEntry, NodeEntry)
     */
    public void add(NodeEntry entry, NodeEntry beforeEntry) {
        if (beforeEntry != null) {
            // the link node where the new entry is ordered before
            LinkedEntries.LinkNode beforeLN = entries.getLinkNode(beforeEntry);
            if (beforeLN == null) {
                throw new NoSuchElementException();
            }
            LinkedEntries.LinkNode insertLN = internalAdd(entry, Path.INDEX_UNDEFINED);
            reorder(entry.getName(), insertLN, beforeLN);
        } else {
            // 'before' is null -> simply append new entry at the end
            add(entry);
        }
    }

    /**
     * Removes the child node entry refering to the node state.
     *
     * @param childEntry the entry to be removed.
     * @return the removed entry or <code>null</code> if there is no such entry.
     * @see ChildNodeEntries#remove(NodeEntry)
     */
    public synchronized NodeEntry remove(NodeEntry childEntry) {
        LinkedEntries.LinkNode ln = entries.removeNodeEntry(childEntry);
        if (ln != null) {
            entriesByName.remove(childEntry.getName(), ln);
            return childEntry;
        } else {
            return null;
        }
    }

    /**
     * Reorders an existing <code>NodeState</code> before another
     * <code>NodeState</code>. If <code>beforeNode</code> is
     * <code>null</code> <code>insertNode</code> is moved to the end of the
     * child node entries.
     *
     * @param insertNode the NodeEntry to move.
     * @param beforeNode the NodeEntry where <code>insertNode</code> is
     * reordered to.
     * @return the NodeEntry that followed the 'insertNode' before the reordering.
     * @throws NoSuchElementException if <code>insertNode</code> or
     * <code>beforeNode</code> does not have a <code>NodeEntry</code>
     * in this <code>ChildNodeEntries</code>.
     * @see ChildNodeEntries#reorder(NodeEntry, NodeEntry)
     */
    public NodeEntry reorder(NodeEntry insertEntry, NodeEntry beforeEntry) {
        // the link node to move
        LinkedEntries.LinkNode insertLN = entries.getLinkNode(insertEntry);
        if (insertLN == null) {
            throw new NoSuchElementException();
        }
        // the link node where insertLN is ordered before
        LinkedEntries.LinkNode beforeLN = (beforeEntry != null) ? entries.getLinkNode(beforeEntry) : null;
        if (beforeEntry != null && beforeLN == null) {
            throw new NoSuchElementException();
        }

        NodeEntry previousBefore = insertLN.getNextLinkNode().getNodeEntry();
        if (previousBefore != beforeEntry) {
            reorder(insertEntry.getName(), insertLN, beforeLN);
        }
        return previousBefore;
    }

    /**
     *
     * @param insertObj
     * @param insertLN
     * @param beforeLN
     */
    private void reorder(Name insertName, LinkedEntries.LinkNode insertLN, LinkedEntries.LinkNode beforeLN) {
        // reorder named map
        if (entriesByName.containsSiblings(insertName)) {
            int position;
            if (beforeLN == null) {
                // reorder to the end -> use illegal position as marker
                position = - 1;
            } else {
                // count all SNS-entries that are before 'beforeLN' in order to
                // determine the new position of the reordered node regarding
                // his siblings.
                position = 0;
                for (Iterator it = entries.linkNodeIterator(); it.hasNext(); ) {
                    LinkedEntries.LinkNode ln = (LinkedEntries.LinkNode) it.next();
                    if (ln == beforeLN) {
                        break;
                    } else if (ln != insertLN && ln.getNodeEntry().getName().equals(insertName)) {
                        position++;
                    } // else: ln == inserLN OR no SNS -> not relevant for position count
                }
            }
            entriesByName.reorder(insertName, insertLN, position);
        }
        // reorder in linked list
        entries.reorderNode(insertLN, beforeLN);
    }

    //-------------------------------------------------< AbstractLinkedList >---
    /**
     * An implementation of a linked list which provides access to the internal
     * LinkNode which links the entries of the list.
     */
    private final class LinkedEntries extends AbstractLinkedList {

        LinkedEntries() {
            super();
            init();
        }

        /**
         * Returns the matching <code>LinkNode</code> from a list or a single
         * <code>LinkNode</code>. This method will return <code>null</code>
         * if none of the entries matches either due to missing entry for given
         * state name or due to missing availability of the <code>NodeEntry</code>.
         *
         * @param nodeEntry the <code>NodeEntry</code> that is compared to the
         * resolution of any <code>NodeEntry</code> that matches by name.
         * @return the matching <code>LinkNode</code> or <code>null</code>
         */
        private LinkedEntries.LinkNode getLinkNode(NodeEntry nodeEntry) {
            for (Iterator it = linkNodeIterator(); it.hasNext();) {
                LinkedEntries.LinkNode ln = (LinkedEntries.LinkNode) it.next();
                if (ln.getNodeEntry() == nodeEntry) {
                    return ln;
                }
            }
            // not found
            return null;
        }

        /**
         * Adds a child node entry to this list.
         *
         * @param cne the child node entry to add.
         * @return the LinkNode which refers to the added <code>NodeEntry</code>.
         */
        LinkedEntries.LinkNode add(NodeEntry cne) {
            LinkedEntries.LinkNode ln = new LinkedEntries.LinkNode(cne);
            addNode(ln, header);
            return ln;
        }

        /**
         * Remove the LinkEntry the contains the given NodeEntry as value.
         *
         * @param cne NodeEntry to be removed.
         * @return LinkedEntries.LinkNode that has been removed.
         */
        LinkedEntries.LinkNode removeNodeEntry(NodeEntry cne) {
            LinkedEntries.LinkNode ln = getLinkNode(cne);
            if (ln != null) {
                ln.remove();
            }
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
        void reorderNode(LinkedEntries.LinkNode insert, LinkedEntries.LinkNode before) {
            removeNode(insert);
            if (before == null) {
                addNode(insert, header);
            } else {
                addNode(insert, before);
            }
        }

        /**
         * Create a new <code>LinkNode</code> for a given {@link NodeEntry}
         * <code>value</code>.
         *
         * @param value a child node entry.
         * @return a wrapping {@link LinkedEntries.LinkNode}.
         * @see AbstractLinkedList#createNode(Object)
         */
        protected Node createNode(Object value) {
            return new LinkedEntries.LinkNode(value);
        }

        /**
         * @return a new <code>LinkNode</code>.
         * @see AbstractLinkedList#createHeaderNode()
         */
        protected Node createHeaderNode() {
            return new LinkedEntries.LinkNode();
        }

        /**
         * @return iterator over all LinkNode entries in this list.
         */
        private Iterator linkNodeIterator() {
            return new LinkNodeIterator();
        }

        //----------------------------------------------------------------------
        /**
         * Extends the <code>AbstractLinkedList.Node</code>.
         */
        private final class LinkNode extends Node {

            private final Name qName;

            protected LinkNode() {
                super();
                qName = null;
            }

            protected LinkNode(Object value) {
                super(new WeakReference(value));
                qName = ((NodeEntry) value).getName();
            }

            protected void setValue(Object value) {
                throw new UnsupportedOperationException("Not implemented");
            }

            protected Object getValue() {
                Reference val = (Reference) super.getValue();
                // if the nodeEntry has been g-collected in the mean time
                // create a new NodeEntry in order to avoid returning null.
                NodeEntry ne = (val == null) ?  null : (NodeEntry) val.get();
                if (ne == null && this != header) {
                    ne = factory.createNodeEntry(parent, qName, null);
                    super.setValue(new WeakReference(ne));
                }
                return ne;
            }

            /**
             * @return the wrapped <code>NodeEntry</code>.
             */
            public NodeEntry getNodeEntry() {
                return (NodeEntry) getValue();
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
            public LinkedEntries.LinkNode getNextLinkNode() {
                return (LinkedEntries.LinkNode) super.getNextNode();
            }
        }

        //----------------------------------------------------------------------
        private class LinkNodeIterator implements Iterator {

            private LinkedEntries.LinkNode next = ((LinkedEntries.LinkNode) header).getNextLinkNode();
            private int expectedModCount = modCount;

            public boolean hasNext() {
                checkModCount();
                return next != header;
            }

            public Object next() {
                checkModCount();
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                LinkedEntries.LinkNode n = next;
                next = next.getNextLinkNode();
                return n;
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }

            private void checkModCount() {
                if (expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }



    //--------------------------------------------------------------------------
    /**
     * Mapping of Name to LinkNode OR List of LinkNode(s) in case of SNSiblings.
     */
    private static class NameMap {

        private Map snsMap = new HashMap();
        private Map nameMap = new HashMap();

        /**
         * Return true if more than one NodeEnty with the given name exists.
         *
         * @param qName
         * @return
         */
        public boolean containsSiblings(Name qName) {
            return snsMap.containsKey(qName);
        }

        /**
         * Returns a single <code>NodeEntry</code> or an unmodifiable
         * <code>List</code> of NodeEntry objects.
         *
         * @param qName
         * @return a single <code>NodeEntry</code> or a <code>List</code> of
         * NodeEntry objects.
         */
        private Object get(Name qName) {
            Object val = nameMap.get(qName);
            if (val != null) {
                return ((LinkedEntries.LinkNode) val).getNodeEntry();
            } else {
                List l = (List) snsMap.get(qName);
                if (l != null) {
                    List nodeEntries = new ArrayList(l.size());
                    for (Iterator it = l.iterator(); it.hasNext();) {
                        LinkedEntries.LinkNode ln = (LinkedEntries.LinkNode) it.next();
                        nodeEntries.add(ln.getNodeEntry());
                    }
                    return nodeEntries;
                }
            }
            return null;
        }

        /**
         * Returns a unmodifiable List of NodeEntry objects even if the name map
         * only contains a single entry for the given name. If no matching entry
         * exists for the given qualified name an empty list is returned.
         *
         * @param qName
         * @return
         */
        public List getList(Name qName) {
            Object obj = get(qName);
            if (obj == null) {
                return Collections.EMPTY_LIST;
            } else if (obj instanceof List) {
                List l = new ArrayList((List)obj);
                return Collections.unmodifiableList(l);
            } else {
                // NodeEntry
                return Collections.singletonList(obj);
            }
        }

        public NodeEntry getNodeEntry(Name qName, int index) {
            Object obj = get(qName);
            if (obj == null) {
                return null;
            }
            if (obj instanceof List) {
                // map entry is a list of siblings
                return findMatchingEntry((List) obj, index);
            } else {
                // map entry is a single child node entry
                if (index == Path.INDEX_DEFAULT) {
                    return (NodeEntry) obj;
                }
            }
            return null;
        }

        public LinkedEntries.LinkNode getLinkNode(Name qName, int index) {
            if (index < Path.INDEX_DEFAULT) {
                throw new IllegalArgumentException("Illegal index " + index);
            }

            LinkedEntries.LinkNode val = (LinkedEntries.LinkNode) nameMap.get(qName);
            if (val != null) {
                return (index == Path.INDEX_DEFAULT) ? val : null;
            } else {
                // look in snsMap
                List l = (List) snsMap.get(qName);
                int pos = index - 1; // Index of NodeEntry is 1-based
                return (l != null && pos < l.size()) ? (LinkedEntries.LinkNode) l.get(pos) : null;
            }
        }

        public void put(Name qName, LinkedEntries.LinkNode value) {
            // if 'nameMap' already contains a single entry -> move it to snsMap
            LinkedEntries.LinkNode single = (LinkedEntries.LinkNode) nameMap.remove(qName);
            List l;
            if (single != null) {
                l = new ArrayList();
                l.add(single);
                snsMap.put(qName, l);
            } else {
                // if 'snsMap' already contains list
                l = (List) snsMap.get(qName);
            }

            if (l == null) {
                nameMap.put(qName, value);
            } else {
                l.add(value);
            }
        }

        public LinkedEntries.LinkNode remove(Name qName, LinkedEntries.LinkNode value) {
            Object rm = nameMap.remove(qName);
            if (rm == null) {
                List l = (List) snsMap.get(qName);
                if (l != null && l.remove(value)) {
                    rm = value;
                }
            }
            return ((LinkedEntries.LinkNode) rm);
        }

        public void reorder(Name qName, LinkedEntries.LinkNode insertValue, int position) {
            List sns = (List) snsMap.get(qName);
            if (sns == null) {
                // no same name siblings -> no special handling required
                return;
            }
            // reorder sns in the name-list
            if (position < 0) {
                // simply move to end of list
                sns.remove(insertValue);
                sns.add(insertValue);
            } else {
                sns.remove(insertValue);
                sns.add(position, insertValue);
            }
        }

        /**
         *
         * @param siblings
         * @param index
         * @param checkValidity
         * @return
         */
        private static NodeEntry findMatchingEntry(List siblings, int index) {
            // shortcut if index can never match
            if (index > siblings.size()) {
                return null;
            } else {
                return (NodeEntry) siblings.get(index - 1);
            }
        }
    }
}
