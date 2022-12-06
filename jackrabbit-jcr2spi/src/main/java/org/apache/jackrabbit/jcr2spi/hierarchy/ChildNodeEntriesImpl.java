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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.jcr2spi.state.Status;
import org.apache.jackrabbit.spi.ChildInfo;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ChildNodeEntriesImpl</code> implements a memory sensitive implementation
 * of the <code>ChildNodeEntries</code> interface.
 */
final class ChildNodeEntriesImpl implements ChildNodeEntries {

    private static Logger log = LoggerFactory.getLogger(ChildNodeEntriesImpl.class);

    private boolean complete = false;

    /**
     * Map used for lookup by name.
     */
    private final NameMap entriesByName = new NameMap();

    private final NodeEntry parent;
    private final EntryFactory factory;

    /**
     * Linked list of {@link NodeEntry} instances.
     */
    private final LinkedEntries entries;

     /**
      * Create a new <code>ChildNodeEntries</code> collection from the given
      * <code>childNodeInfos</code> instead of retrieving them from the
      * persistent layer.
      *
      * @param parent
      * @param factory
      * @param childNodeInfos The complete list of child infos or
      * <code>null</code> if an 'empty' ChildNodeEntriesImpl should be created.
      * In the latter case, individual child entries will be added on demand
      * and the complete list will be retrieved only to answer {@link #iterator()}
      * if the passed boolean is <code>true</code>.
      */
     ChildNodeEntriesImpl(NodeEntry parent, EntryFactory factory, Iterator<ChildInfo> childNodeInfos) {
         this.parent = parent;
         this.factory = factory;
         this.entries = new LinkedEntries(factory, parent);

         if (childNodeInfos != null) {
             while (childNodeInfos.hasNext()) {
                 ChildInfo ci = childNodeInfos.next();
                 NodeEntry entry = factory.createNodeEntry(parent, ci.getName(), ci.getUniqueID());
                 add(entry, ci.getIndex());
             }
             complete = true;
         } else {
             complete = false;
         }
     }

    /**
     * @param childEntry
     * @return The node entry that directly follows the given <code>childEntry</code>
     * or <code>null</code> if the given <code>childEntry</code> has no successor
     * or was not found in this <code>ChildNodeEntries</code>.
     */
    NodeEntry getNext(NodeEntry childEntry) {
        LinkedEntries.LinkNode ln = entries.getLinkNode(childEntry);
        LinkedEntries.LinkNode nextLn = (ln == null) ? null : ln.getNextLinkNode();
        return (nextLn == null) ? null : nextLn.getNodeEntry();
    }

    /**
     * @param childEntry
     * @return The node entry that directly precedes the given <code>childEntry</code>
     * or <code>null</code> if the given <code>childEntry</code> is the first
     * or was not found in this <code>ChildNodeEntries</code>.
     */
    NodeEntry getPrevious(NodeEntry childEntry) {
        LinkedEntries.LinkNode ln = entries.getLinkNode(childEntry);
        LinkedEntries.LinkNode prevLn = (ln == null) ? null : ln.getPreviousLinkNode();
        return (prevLn == null) ? null : prevLn.getNodeEntry();
    }

    /**
     * @see ChildNodeEntries#isComplete()
     */
    public boolean isComplete() {
        return (parent.getStatus() != Status.INVALIDATED && complete) ||
                parent.getStatus() == Status.NEW ||
                Status.isTerminal(parent.getStatus());
    }

    /**
     * @see ChildNodeEntries#reload()
     */
    public synchronized void reload() throws ItemNotFoundException, RepositoryException {
        if (isComplete()) {
            // nothing to do
            return;
        }

        NodeId id = parent.getWorkspaceId();
        Iterator<ChildInfo> childNodeInfos = factory.getItemStateFactory().getChildNodeInfos(id);
        update(childNodeInfos);
    }

    /**
     * Update the child node entries according to the child-infos obtained
     * from the persistence layer.
     * NOTE: the status of the entries already present is not respected. Thus
     * new or removed entries are not touched in order not to modify the
     * transient status of the parent. Operations that affect the set or order
     * of child entries (AddNode, Move, Reorder) currently assert the
     * completeness of the ChildNodeEntries, therefore avoiding an update
     * resulting in inconsistent entries.
     *
     * @param childNodeInfos
     * @see HierarchyEntry#reload(boolean) that ignores items with
     * pending changes.
     * @see org.apache.jackrabbit.jcr2spi.operation.AddNode
     * @see org.apache.jackrabbit.jcr2spi.operation.Move
     * @see org.apache.jackrabbit.jcr2spi.operation.ReorderNodes
     */
    synchronized void update(Iterator<ChildInfo> childNodeInfos) {
        // insert missing entries and reorder all if necessary.
        LinkedEntries.LinkNode prevLN = null;
        while (childNodeInfos.hasNext()) {
            ChildInfo ci = childNodeInfos.next();
            LinkedEntries.LinkNode ln = entriesByName.getLinkNode(ci.getName(), ci.getIndex(), ci.getUniqueID());
            if (ln == null) {
                // add missing at the correct position.
                NodeEntry entry = factory.createNodeEntry(parent, ci.getName(), ci.getUniqueID());
                ln = internalAddAfter(entry, ci.getIndex(), prevLN);
            } else if (prevLN != null) {
                // assert correct order of existing
                if (prevLN != ln) {
                    reorderAfter(ln, prevLN);
                } else {
                    // there was an existing entry but it's the same as the one
                    // created/retrieved before. getting here indicates that
                    // the SPI implementation provided invalid childNodeInfos.
                    log.error("ChildInfo iterator contains multiple entries with the same name|index or uniqueID -> ignore ChildNodeInfo.");
                }
            }
            prevLN = ln;
        }
        // finally reset the status
        complete = true;
    }

    /**
     * @see ChildNodeEntries#iterator()
     */
    public Iterator<NodeEntry> iterator() {
        List<NodeEntry> l = new ArrayList<NodeEntry>(entries.size());
        for (Iterator<LinkedEntries.LinkNode> it = entries.linkNodeIterator(); it.hasNext();) {
            l.add(it.next().getNodeEntry());
        }
        return Collections.unmodifiableList(l).iterator();
    }

    /**
     * @see ChildNodeEntries#get(Name)
     */
    public List<NodeEntry> get(Name nodeName) {
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
        for (NodeEntry cne : get(nodeName)) {
            if (uniqueID.equals(cne.getUniqueID())) {
                return cne;
            }
        }
        return null;
    }

    /**
     * Adds a <code>NodeEntry</code> to the end of the list. Same as
     * {@link #add(NodeEntry, int)}, where the index is {@link Path#INDEX_UNDEFINED}.
     *
     * @param cne the <code>NodeEntry</code> to add.
     * @see ChildNodeEntries#add(NodeEntry)
     */
     public synchronized void add(NodeEntry cne) {
        internalAdd(cne, Path.INDEX_UNDEFINED);
    }

    /**
     * @see ChildNodeEntries#add(NodeEntry, int)
     */
    public synchronized void add(NodeEntry cne, int index) {
        if (index < Path.INDEX_UNDEFINED) {
            throw new IllegalArgumentException("Invalid index" + index);
        }
        internalAdd(cne, index);
    }

    /**
     * @see ChildNodeEntries#add(NodeEntry, int, NodeEntry)
     */
    public synchronized void add(NodeEntry entry, int index, NodeEntry beforeEntry) {
        if (beforeEntry != null) {
            // the link node where the new entry is ordered before
            LinkedEntries.LinkNode beforeLN = entries.getLinkNode(beforeEntry);
            if (beforeLN == null) {
                throw new NoSuchElementException();
            }
            LinkedEntries.LinkNode insertLN = internalAdd(entry, index);
            reorder(entry.getName(), insertLN, beforeLN);
        } else {
            // 'before' is null -> simply append new entry at the end
            add(entry);
        }
    }

    /**
     *
     * @param entry
     * @param index
     * @return the <code>LinkNode</code> belonging to the added entry.
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
        LinkedEntries.LinkNode ln = entries.add(entry, index);
        entriesByName.put(nodeName, index, ln);

        // reorder the child entries if, the new entry must be inserted rather
        // than appended at the end of the list.
        if (existing != null) {
            reorder(nodeName, ln, existing);
        }
        return ln;
    }

    /**
     * Add the specified new entry after the specified <code>insertAfter</code>.
     *
     * @param newEntry
     * @param index
     * @param insertAfter
     * @return the <code>LinkNode</code> associated with the <code>newEntry</code>.
     */
    private LinkedEntries.LinkNode internalAddAfter(NodeEntry newEntry, int index,
                                                    LinkedEntries.LinkNode insertAfter) {
        LinkedEntries.LinkNode ln = entries.addAfter(newEntry, index, insertAfter);
        entriesByName.put(newEntry.getName(), index, ln);
        return ln;
    }

    /**
     * Removes the child node entry referring to the node state.
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
     * @param insertEntry the NodeEntry to move.
     * @param beforeEntry the NodeEntry where <code>insertNode</code> is
     * reordered to.
     * @return the NodeEntry that followed the 'insertNode' before the reordering.
     * @throws NoSuchElementException if <code>insertNode</code> or
     * <code>beforeNode</code> does not have a <code>NodeEntry</code>
     * in this <code>ChildNodeEntries</code>.
     * @see ChildNodeEntries#reorder(NodeEntry, NodeEntry)
     */
    public synchronized NodeEntry reorder(NodeEntry insertEntry, NodeEntry beforeEntry) {
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
     * @see ChildNodeEntries#reorderAfter(NodeEntry, NodeEntry)
     */
    public void reorderAfter(NodeEntry insertEntry, NodeEntry afterEntry) {
        // the link node to move
        LinkedEntries.LinkNode insertLN = entries.getLinkNode(insertEntry);
        if (insertLN == null) {
            throw new NoSuchElementException();
        }
        // the link node where insertLN is ordered before
        LinkedEntries.LinkNode afterLN = (afterEntry != null) ? entries.getLinkNode(afterEntry) : null;
        if (afterEntry != null && afterLN == null) {
            throw new NoSuchElementException();
        }

        LinkedEntries.LinkNode previousLN = insertLN.getPreviousLinkNode();
        if (previousLN != afterLN) {
            reorderAfter(insertLN, afterLN);
        } // else: already in correct position. nothing to do
    }

    /**
     *
     * @param insertName
     * @param insertLN
     * @param beforeLN
     */
    private void reorder(Name insertName, LinkedEntries.LinkNode insertLN,
                         LinkedEntries.LinkNode beforeLN) {
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
                for (Iterator<LinkedEntries.LinkNode> it = entries.linkNodeIterator(); it.hasNext(); ) {
                    LinkedEntries.LinkNode ln = it.next();
                    if (ln == beforeLN) {
                        break;
                    } else if (ln != insertLN && insertName.equals(ln.qName)) {
                        position++;
                    } // else: ln == insertLN OR no SNS -> not relevant for position count
                }
            }
            entriesByName.reorder(insertName, insertLN, position);
        }
        // reorder in linked list
        entries.reorderNode(insertLN, beforeLN);
    }

    /**
     *
     * @param insertLN
     * @param afterLN
     */
    private void reorderAfter(LinkedEntries.LinkNode insertLN, LinkedEntries.LinkNode afterLN) {
        // the link node to move
        if (insertLN == null) {
            throw new NoSuchElementException();
        }
        // the link node where insertLN is ordered after
        if (afterLN == null) {
            // move to first position
            afterLN = entries.getHeader();
        }

        LinkedEntries.LinkNode currentAfter = afterLN.getNextLinkNode();
        if (currentAfter == insertLN) {
            log.debug("Already ordered behind 'afterEntry'.");
            // nothing to do
            return;
        } else {
            // reorder named map
            Name insertName = insertLN.qName;
            if (entriesByName.containsSiblings(insertName)) {
                int position = -1; // default: reorder to the end.
                if (afterLN == entries.getHeader()) {
                    // move to the beginning
                    position = 0;
                } else {
                    // count all SNS-entries that are before 'afterLN' in order to
                    // determine the new position of the reordered node regarding
                    // his siblings.
                    position = 0;
                    for (Iterator<LinkedEntries.LinkNode> it = entries.linkNodeIterator(); it.hasNext(); ) {
                        LinkedEntries.LinkNode ln = it.next();
                        if (insertName.equals(ln.qName) && (ln != insertLN)) {
                            position++;
                        }
                        if (ln == afterLN) {
                            break;
                        }
                    }
                }
                entriesByName.reorder(insertName, insertLN, position);
            }
            // reorder in linked list
            entries.reorderNode(insertLN, currentAfter);
        }
    }

    //-------------------------------------------------< AbstractLinkedList >---

    //--------------------------------------------------------------------------
    /**
     * Mapping of Name to LinkNode OR List of LinkNode(s) in case of SNSiblings.
     */
    private static class NameMap {

        private final Map<Name, List<LinkedEntries.LinkNode>> snsMap = new HashMap<Name, List<LinkedEntries.LinkNode>>();
        private final Map<Name, LinkedEntries.LinkNode> nameMap = new HashMap<Name, LinkedEntries.LinkNode>();

        /**
         * Return true if more than one NodeEntry with the given name exists.
         *
         * @param qName
         * @return true if more than one NodeEntry with the given name exists.
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
            LinkedEntries.LinkNode val = nameMap.get(qName);
            if (val != null) {
                return val.getNodeEntry();
            } else {
                List<LinkedEntries.LinkNode> l = snsMap.get(qName);
                if (l != null) {
                    List<NodeEntry> nodeEntries = new ArrayList<NodeEntry>(l.size());
                    for (Iterator<LinkedEntries.LinkNode> it = l.iterator(); it.hasNext();) {
                        LinkedEntries.LinkNode ln = it.next();
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
         * exists for the given <code>Name</code> an empty list is returned.
         *
         * @param name
         * @return list of entries or an empty list.
         */
        @SuppressWarnings("unchecked")
        public List<NodeEntry> getList(Name name) {
            Object obj = get(name);
            if (obj == null) {
                return Collections.emptyList();
            } else if (obj instanceof List) {
                List<NodeEntry> l = new ArrayList<NodeEntry>((List<NodeEntry>) obj);
                return Collections.unmodifiableList(l);
            } else {
                // NodeEntry
                return Collections.singletonList((NodeEntry)obj);
            }
        }

        @SuppressWarnings("unchecked")
        public NodeEntry getNodeEntry(Name name, int index) {
            Object obj = get(name);
            if (obj == null) {
                return null;
            }
            if (obj instanceof List) {
                // map entry is a list of siblings
                return findMatchingEntry((List<NodeEntry>) obj, index);
            } else {
                // map entry is a single child node entry
                if (index == Path.INDEX_DEFAULT) {
                    return (NodeEntry) obj;
                }
            }
            return null;
        }

        public LinkedEntries.LinkNode getLinkNode(Name name, int index) {
            if (index < Path.INDEX_DEFAULT) {
                throw new IllegalArgumentException("Illegal index " + index);
            }

            LinkedEntries.LinkNode val = nameMap.get(name);
            if (val != null) {
                return (index == Path.INDEX_DEFAULT) ? val : null;
            } else {
                // look in snsMap
                List<LinkedEntries.LinkNode> l = snsMap.get(name);
                int pos = index - 1; // Index of NodeEntry is 1-based
                return (l != null && pos < l.size()) ? l.get(pos) : null;
            }
        }

        public LinkedEntries.LinkNode getLinkNode(Name name, int index, String uniqueID) {
            if (uniqueID != null) {
                // -> try if any entry matches.
                // if none matches it be might that entry doesn't have uniqueID
                // set yet -> search without uniqueID
                LinkedEntries.LinkNode val = nameMap.get(name);
                if (val != null) {
                    if (uniqueID.equals(val.getNodeEntry().getUniqueID())) {
                        return val;
                    }
                } else {
                    // look in snsMap
                    List<LinkedEntries.LinkNode> l = snsMap.get(name);
                    if (l != null) {
                        for (Iterator<LinkedEntries.LinkNode> it = l.iterator(); it.hasNext();) {
                            LinkedEntries.LinkNode ln = it.next();
                            if (uniqueID.equals(ln.getNodeEntry().getUniqueID())) {
                                return ln;
                            }
                        }
                    }
                }
            }
            // no uniqueID passed or not match.
            // try to load the child entry by name and index.
            return getLinkNode(name, index);
        }

        public void put(Name name, int index, LinkedEntries.LinkNode value) {
            // if 'nameMap' already contains a single entry -> move it to snsMap
            LinkedEntries.LinkNode single = nameMap.remove(name);
            List<LinkedEntries.LinkNode> l;
            if (single != null) {
                l = new ArrayList<LinkedEntries.LinkNode>();
                l.add(single);
                snsMap.put(name, l);
            } else {
                // if 'snsMap' already contains list
                l = snsMap.get(name);
            }

            if (l == null) {
                // no same name siblings -> simply put to the name map.
                nameMap.put(name, value);
            } else {
                // sibling(s) already present -> insert into the list
                int position = index - 1;
                if (position < 0 || position > l.size()) {
                    l.add(value); // invalid position -> append at the end.
                } else {
                    l.add(position, value); // insert with the correct index.
                }
            }
        }

        public LinkedEntries.LinkNode remove(Name name, LinkedEntries.LinkNode value) {
            LinkedEntries.LinkNode rm = nameMap.remove(name);
            if (rm == null) {
                List<LinkedEntries.LinkNode> l = snsMap.get(name);
                if (l != null && l.remove(value)) {
                    rm = value;
                }
            }
            return rm;
        }

        public void reorder(Name name, LinkedEntries.LinkNode insertValue, int position) {
            List<LinkedEntries.LinkNode> sns = snsMap.get(name);
            if (sns == null) {
                // no same name siblings -> no special handling required
                return;
            }
            // reorder sns in the name-list
            sns.remove(insertValue);
            if (position < 0 || position > sns.size()) {
                // simply move to end of list
                sns.add(insertValue);
            } else {
                sns.add(position, insertValue);
            }
        }

        /**
         *
         * @param siblings
         * @param index
         * @return matching entry or <code>null</code>.
         */
        private static NodeEntry findMatchingEntry(List<NodeEntry> siblings, int index) {
            // shortcut if index can never match
            if (index > siblings.size()) {
                return null;
            } else {
                return siblings.get(index - 1);
            }
        }
    }
}
