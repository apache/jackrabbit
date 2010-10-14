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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.Name;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Internal utility class used for merging concurrent changes that occurred
 * on different <code>NodeState</code> instances representing the same node.
 * <p/>
 * See http://issues.apache.org/jira/browse/JCR-584.
 */
class NodeStateMerger {

    /**
     * Tries to silently merge the given <code>state</code> with its
     * externally (e.g. through another session) modified overlayed state
     * in order to avoid an <code>InvalidItemStateException</code>.
     * <p/>
     * See http://issues.apache.org/jira/browse/JCR-584.
     *
     * @param state node state whose modified overlayed state should be
     *        merged
     * @param context used for analyzing the context of the modifications
     * @return true if the changes could be successfully merged into the
     *         given node state; false otherwise
     */
    static boolean merge(NodeState state, MergeContext context) {

        NodeState overlayedState = (NodeState) state.getOverlayedState();
        if (overlayedState == null
                || state.getModCount() == overlayedState.getModCount()) {
            return false;
        }

        synchronized (overlayedState) {
            synchronized (state) {
                /**
                 * some examples for trivial non-conflicting changes:
                 * - s1 added child node a, s2 removes child node b
                 * - s1 adds child node a, s2 adds child node b
                 * - s1 adds child node a, s2 adds mixin type
                 *
                 * conflicting changes causing staleness:
                 * - s1 added non-sns child node or property a,
                 *   s2 added non-sns child node or property a => name clash
                 * - either session reordered child nodes
                 *   (some combinations could possibly be merged)
                 * - either session moved node
                 */

                // compare current state with externally modified overlayed
                // state and determine what has been changed by whom

                // child node entries order
                if (!state.getReorderedChildNodeEntries().isEmpty()) {
                    // for now we don't even try to merge the result of
                    // a reorder operation
                    return false;
                }

                // the primary node type
                if (!state.getNodeTypeName().equals(overlayedState.getNodeTypeName())) {
                    // the primary node type has changed either in 'state' or 'overlayedState'.
                    return false;
                }

                // mixin types
                if (!state.getMixinTypeNames().equals(overlayedState.getMixinTypeNames())) {
                    // the mixins have been modified but by just looking at the diff we
                    // can't determine where the change happened since the diffs of either
                    // removing a mixin from the overlayed or adding a mixin to the
                    // transient state would look identical...
                    return false;
                }

                // parent id
                if (state.getParentId() != null
                        && !state.getParentId().equals(overlayedState.getParentId())) {
                    return false;
                }

                // child node entries
                if (!state.getChildNodeEntries().equals(
                        overlayedState.getChildNodeEntries())) {
                    ArrayList<ChildNodeEntry> added = new ArrayList<ChildNodeEntry>();
                    ArrayList<ChildNodeEntry> removed = new ArrayList<ChildNodeEntry>();

                    for (ChildNodeEntry cne : state.getAddedChildNodeEntries()) {

                        if (context.isAdded(cne.getId()) || context.isModified(cne.getId())) {
                            // a new child node entry has been added to this state;
                            // check for name collisions with other state
                            if (overlayedState.hasChildNodeEntry(cne.getName())) {
                                // conflicting names
                                if (cne.getIndex() < 2) {
                                    // check if same-name siblings are allowed
                                    if (!context.allowsSameNameSiblings(cne.getId())) {
                                        return false;
                                    }
                                }
                                // assume same-name siblings are allowed since index is >= 2
                            }

                            added.add(cne);
                        } else {
                            // externally added
                        }
                    }

                    for (ChildNodeEntry cne : state.getRemovedChildNodeEntries()) {
                        if (context.isDeleted(cne.getId()) || context.isModified(cne.getId())) {
                            // a child node entry has been removed from this node state
                            removed.add(cne);
                        } else {
                            // externally removed
                        }
                    }

                    // copy child node entries from other state and
                    // re-apply changes made on this state
                    state.setChildNodeEntries(overlayedState.getChildNodeEntries());
                    for (ChildNodeEntry cne : removed) {
                        state.removeChildNodeEntry(cne.getId());
                    }
                    for (ChildNodeEntry cne : added) {
                        state.addChildNodeEntry(cne.getName(), cne.getId());
                    }
                }

                // property names
                if (!state.getPropertyNames().equals(
                        overlayedState.getPropertyNames())) {
                    HashSet<Name> added = new HashSet<Name>();
                    HashSet<Name> removed = new HashSet<Name>();

                    for (Name name : state.getAddedPropertyNames()) {
                        PropertyId propId =
                                new PropertyId(state.getNodeId(), name);
                        if (context.isAdded(propId)) {
                            added.add(name);
                        }
                    }
                    for (Name name : state.getRemovedPropertyNames()) {
                        PropertyId propId =
                                new PropertyId(state.getNodeId(), name);
                        if (context.isDeleted(propId)) {
                            // a property name has been removed from this state
                            removed.add(name);
                        }
                    }

                    // copy property names from other and
                    // re-apply changes made on this state
                    state.setPropertyNames(overlayedState.getPropertyNames());
                    for (Name name : added) {
                        state.addPropertyName(name);
                    }
                    for (Name name : removed) {
                        state.removePropertyName(name);
                    }
                }

                // finally sync modification count
                state.setModCount(overlayedState.getModCount());

                return true;
            }
        }
    }

    //-----------------------------------------------------< inner interfaces >

    /**
     * The context of a modification.
     */
    static interface MergeContext {
        boolean isAdded(ItemId id);
        boolean isDeleted(ItemId id);
        boolean isModified(ItemId id);
        boolean allowsSameNameSiblings(NodeId id);
    }
}
