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
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.nodetype.EffectiveNodeType;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.name.NameConstants;

import javax.jcr.nodetype.NoSuchNodeTypeException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Internal utility class used for merging concurrent changes that occurred
 * on different <code>NodeState</code> instances representing the same node.
 * <p>
 * See http://issues.apache.org/jira/browse/JCR-584.
 */
class NodeStateMerger {

    /**
     * Tries to silently merge the given <code>state</code> with its
     * externally (e.g. through another session) modified overlayed state
     * in order to avoid an <code>InvalidItemStateException</code>.
     * <p>
     * See http://issues.apache.org/jira/browse/JCR-584.
     * See also http://issues.apache.org/jira/browse/JCR-3290.
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
                if (!mergeMixinTypes(state, overlayedState, context)) {
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
                        // locally added or moved?
                        if (context.isAdded(cne.getId()) || (context.isModified(cne.getId()) && isParent(state, cne, context))) {
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
                        }
                    }

                    for (ChildNodeEntry cne : state.getRemovedChildNodeEntries()) {
                        // locally removed?
                        if (context.isDeleted(cne.getId()) || context.isModified(cne.getId())) {
                            // a child node entry has been removed from this node state
                            removed.add(cne);
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

    /**
     * 
     * @param state
     * @param overlayedState
     * @return true if the mixin type names are the same in both node states or
     * if the mixin modifications do not conflict (and could be merged); false
     * otherwise.
     */
    private static boolean mergeMixinTypes(NodeState state, NodeState overlayedState, MergeContext ctx) {
        Set<Name> mixins = new HashSet<Name>(state.getMixinTypeNames());
        Set<Name> overlayedMixins = new HashSet<Name>(overlayedState.getMixinTypeNames());
        if (mixins.equals(overlayedMixins)) {
            // no net effect modifications at all -> merge child items defined
            // by the mixins according to the general rule.
            return true;
        }

        PropertyId mixinPropId = new PropertyId(state.getNodeId(), NameConstants.JCR_MIXINTYPES);

        boolean mergeDone;
        if (ctx.isAdded(mixinPropId)) {
            // jcr:mixinTypes property was created for 'state'.
            // changes is safe (without need to merge),
            // - overlayed state doesn't have any mixins OR
            // - overlayed state got the same (or a subset) added
            // and non of the items defined by the new mixin(s) collides with
            // existing items on the overlayed state
            if (overlayedMixins.isEmpty() || mixins.containsAll(overlayedMixins)) {
                mixins.removeAll(overlayedMixins);
                mergeDone = !conflicts(state, mixins, ctx, true);
            } else {
                // different mixins added in overlayedState and state
                // -> don't merge
                mergeDone = false;
            }
        } else if (ctx.isDeleted(mixinPropId)) {
            // jcr:mixinTypes property was removed in 'state'.
            // we can't determine if there was any change to mixin types in the
            // overlayed state.
            // -> don't merge.
            mergeDone = false;
        } else if (ctx.isModified(mixinPropId)) {
            /* jcr:mixinTypes property was modified in 'state'.
               NOTE: if the mixins of the overlayed state was modified as well
               the property (jcr:mixinTypes) cannot not be persisted (stale).

               since there is not way to determine if the overlayed mixins have
               been modified just check for conflicts related to a net mixin
               addition.
             */
            if (mixins.containsAll(overlayedMixins)) {
                // net result of modifications is only addition.
                // -> so far the changes are save if there are no conflicts
                //    caused by mixins modification in 'state'.
                //    NOTE: the save may still fail if the mixin property has
                //    been modified in the overlayed state as well.
                mixins.removeAll(overlayedMixins);
                mergeDone = !conflicts(state, mixins, ctx, true);
            } else {
                // net result is either a removal in 'state' or modifications
                // in both node states.
                // -> don't merge.
                mergeDone = false;
            }
        } else {
            // jcr:mixinTypes property was added or modified in the overlayed
            // state but neither added nor modified in 'state'.
            if (overlayedMixins.containsAll(mixins)) {
                // the modification in the overlayed state only includes the
                // addition of mixin node types, but no removal.
                // -> need to check if any added items from state would
                //    collide with the items defined by the new mixin on the
                //    overlayed state.
                overlayedMixins.removeAll(mixins);
                if (!conflicts(state, overlayedMixins, ctx, false)) {
                    // update the mixin names in 'state'. the child items defined
                    // by the new mixins will be added later on during merge of
                    // child nodes and properties.
                    state.setMixinTypeNames(overlayedMixins);
                    mergeDone = true;
                } else {
                    mergeDone = false;
                }
            } else {
                // either remove-mixin(s) or both add and removal of mixin in
                // the overlayed state.
                // -> we cannot merge easily
                mergeDone = false;
            }
        }

        return mergeDone;
    }

    /**
     *
     * @param state The state of the node to be saved.
     * @param addedMixins The added mixins to be used for testing
     * @param ctx
     * @param compareToOverlayed
     * @return true if a conflict can be determined, false otherwise.
     */
    private static boolean conflicts(NodeState state,
                                     Set<Name> addedMixins,
                                     MergeContext ctx, boolean compareToOverlayed) {
        try {
            // check for all added mixin types in one state if there are colliding
            // child items in the other state.
            // this is currently a simple check for named item definitions;
            // if the mixin defines residual item definitions -> return false.
            for (Name mixinName : addedMixins) {
                EffectiveNodeType ent = ctx.getEffectiveNodeType(mixinName);

                if (ent.getUnnamedItemDefs().length > 0) {
                    // the mixin defines residual child definitions -> cannot
                    // easily determine conflicts
                    return false;
                }

                NodeState overlayed = (NodeState) state.getOverlayedState();
                for (ChildNodeEntry cne : state.getChildNodeEntries()) {
                    if (ent.getNamedNodeDefs(cne.getName()).length > 0) {
                        if (ctx.isAdded(cne.getId()) || isAutoCreated(cne, ent)) {
                            if (!compareToOverlayed || overlayed.hasChildNodeEntry(cne.getName())) {
                                return true;
                            }
                        } // else: neither added nor autocreated in 'state' .

                    } // else: child node not defined by the added mixin type
                }

                for (Name propName : state.getPropertyNames()) {
                    if (ent.getNamedPropDefs(propName).length > 0) {
                        PropertyId pid = new PropertyId(state.getNodeId(), propName);
                        if (ctx.isAdded(pid) || isAutoCreated(propName, ent)) {
                            if (!compareToOverlayed || overlayed.hasPropertyName(propName)) {
                                return true;
                            }
                        } // else: neither added nor autocreated in 'state'
                    } // else: property not defined by added mixin
                }
            }
        } catch (NoSuchNodeTypeException e) {
            // unable to determine collision
            return true;
        }

        // no conflict detected
        return false;
    }

    private static boolean isParent(NodeState state, ChildNodeEntry entry, MergeContext context) {
        try {
            return state.getId().equals(context.getNodeState(entry.getId()).getParentId());
        } catch (ItemStateException e) {
            return false;
        }
    }

    private static boolean isAutoCreated(ChildNodeEntry cne, EffectiveNodeType ent) {
        for (QNodeDefinition def : ent.getAutoCreateNodeDefs()) {
            if (def.getName().equals(cne.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAutoCreated(Name propertyName, EffectiveNodeType ent) {
        for (QPropertyDefinition def : ent.getAutoCreatePropDefs()) {
            if (def.getName().equals(propertyName)) {
                return true;
            }
        }
        return false;
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
        EffectiveNodeType getEffectiveNodeType(Name ntName) throws NoSuchNodeTypeException;
        NodeState getNodeState(NodeId id) throws ItemStateException;
    }
}
