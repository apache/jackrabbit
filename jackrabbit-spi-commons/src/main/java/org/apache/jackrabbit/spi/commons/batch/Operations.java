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
package org.apache.jackrabbit.spi.commons.batch;

import java.util.Arrays;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.Tree;

/**
 * Factory for creating {@link Operation}s. The inner classes of this class
 * all implement the <code>Operation</code> interface. They are representatives
 * for the method calls on a {@link Batch}. In addition {@link Empty} represents
 * the empty operation which does nothing.
 */
public final class Operations {
    private Operations() {
        super();
    }

    // -------------------------------------------------------------< Empty >---
    /**
     * Representative of the empty {@link Operation} which does nothing when
     * applied to a {@link Batch}.
     */
    public static class Empty implements Operation {
        private static final Empty INSTANCE = new Empty();

        protected Empty() {
            super();
        }

        /**
         * This method has no effect.
         * {@inheritDoc}
         */
        public void apply(Batch batch) throws RepositoryException { /* nothing to do */ }

        @Override
        public String toString() {
            return "Empty[]";
        }

        @Override
        public boolean equals(Object other) {
            if (null == other) {
                return false;
            }
            return other instanceof Empty;
        }

        @Override
        public int hashCode() {
            return Empty.class.hashCode();
        }
    }

    /**
     * Factory method for creating an {@link Empty} operation.
     * @return
     */
    public static Operation empty() {
        return Empty.INSTANCE;
    }

    // -----------------------------------------------------------< AddNode >---
    /**
     * Representative of an add-node {@link Operation} which calls
     * {@link Batch#addNode(NodeId, Name, Name, String)} when applied to a {@link Batch}.
     */
    public static class AddNode implements Operation {
        protected final NodeId parentId;
        protected final Name nodeName;
        protected final Name nodetypeName;
        protected final String uuid;

        /**
         * Create a new add-node {@link Operation} for the given arguments.
         * @see Batch#addNode(NodeId, Name, Name, String)
         *
         * @param parentId
         * @param nodeName
         * @param nodetypeName
         * @param uuid
         */
        public AddNode(NodeId parentId, Name nodeName, Name nodetypeName, String uuid) {
            super();
            this.parentId = parentId;
            this.nodeName = nodeName;
            this.nodetypeName = nodetypeName;
            this.uuid = uuid;
        }

        /**
         * {@inheritDoc}
         */
        public void apply(Batch batch) throws RepositoryException {
            batch.addNode(parentId, nodeName, nodetypeName, uuid);
        }

        @Override
        public String toString() {
            return "AddNode[" + parentId + ", " + nodeName + ", " + nodetypeName + ", " + uuid + "]";
        }

        @Override
        public boolean equals(Object other) {
            if (null == other) {
                return false;
            }
            if (this == other) {
                return true;
            }
            if (other instanceof AddNode) {
                return equals((AddNode) other);
            }
            return false;
        }

        public boolean equals(AddNode other) {
            return Operations.equals(parentId, other.parentId)
                && Operations.equals(nodeName, other.nodeName)
                && Operations.equals(nodetypeName, other.nodetypeName)
                && Operations.equals(uuid, other.uuid);
        }

        @Override
        public int hashCode() {
            return 41 * (
                      41 * (
                          41 * (
                              41 + Operations.hashCode(parentId))
                          + Operations.hashCode(nodeName))
                      + Operations.hashCode(nodetypeName))
                  + Operations.hashCode(uuid);
        }

    }

    /**
     * Factory method for creating an {@link AddNode} operation.
     * @see Batch#addNode(NodeId, Name, Name, String)
     *
     * @param parentId
     * @param nodeName
     * @param nodetypeName
     * @param uuid
     * @return
     */
    public static Operation addNode(NodeId parentId, Name nodeName, Name nodetypeName, String uuid) {
        return new AddNode(parentId, nodeName, nodetypeName, uuid);
    }

    // -------------------------------------------------------< AddProperty >---
    /**
     * Representative of an add-property {@link Operation} which calls
     * {@link Batch#addProperty(NodeId, Name, QValue)} or {@link Batch#addProperty(NodeId, Name, QValue[])}
     * depending on whether the property is multi valued or not when applied to a {@link Batch}.
     */
    public static class AddProperty implements Operation {
        protected final NodeId parentId;
        protected final Name propertyName;
        protected final QValue[] values;
        protected final boolean isMultivalued;

        private AddProperty(NodeId parentId, Name propertyName, QValue[] values, boolean isMultivalued) {
            super();
            this.parentId = parentId;
            this.propertyName = propertyName;
            this.values = values;
            this.isMultivalued = isMultivalued;
        }

        /**
         * Create a new add-property {@link Operation} for the given arguments.
         * @see Batch#addProperty(NodeId, Name, QValue)
         *
         * @param parentId
         * @param propertyName
         * @param value
         */
        public AddProperty(NodeId parentId, Name propertyName, QValue value) {
            this(parentId, propertyName, new QValue[] { value }, false);
        }

        /**
         * Create a new add-property {@link Operation} for the given arguments.
         * @see Batch#addProperty(NodeId, Name, QValue[])
         *
         * @param parentId
         * @param propertyName
         * @param values
         */
        public AddProperty(NodeId parentId, Name propertyName, QValue[] values) {
            this(parentId, propertyName, values, true);
        }

        /**
         * {@inheritDoc}
         */
        public void apply(Batch batch) throws RepositoryException {
            if (isMultivalued) {
                batch.addProperty(parentId, propertyName, values);
            }
            else {
                batch.addProperty(parentId, propertyName, values[0]);
            }
        }

        @Override
        public String toString() {
            return "AddProperty[" + parentId + ", " + propertyName + ", " + Arrays.toString(values) + "]";
        }

        @Override
        public boolean equals(Object other) {
            if (null == other) {
                return false;
            }
            if (this == other) {
                return true;
            }
            if (other instanceof AddProperty) {
                return equals((AddProperty) other);
            }
            return false;
        }

        public boolean equals(AddProperty other) {
            return Operations.equals(parentId, other.parentId)
                && Operations.equals(propertyName, other.propertyName)
                && isMultivalued == other.isMultivalued
                && Arrays.equals(values, other.values);
        }

        @Override
        public int hashCode() {
            return 41 * (
                       41 * (
                           41 + Operations.hashCode(parentId))
                       + Operations.hashCode(propertyName))
                   + Operations.hashCode(values);
        }
    }

    /**
     * Factory method for creating an {@link AddProperty} operation.
     *
     * @see Batch#addProperty(NodeId, Name, QValue)
     * @param parentId
     * @param propertyName
     * @param value
     * @return
     */
    public static Operation addProperty(NodeId parentId, Name propertyName, QValue value) {
        return new AddProperty(parentId, propertyName, value);
    }

    /**
     * Factory method for creating an {@link AddProperty} operation.
     *
     * @see Batch#addProperty(NodeId, Name, QValue[])
     * @param parentId
     * @param propertyName
     * @param values
     * @return
     */
    public static Operation addProperty(NodeId parentId, Name propertyName, QValue[] values) {
        return new AddProperty(parentId, propertyName, values);
    }

    // --------------------------------------------------------------< Move >---
    /**
     * Representative of a move {@link Operation} which calls
     * {@link Batch#move(NodeId, NodeId, Name)} when applied to a {@link Batch}.
     */
    public static class Move implements Operation {
        protected final NodeId srcNodeId;
        protected final NodeId destParentNodeId;
        protected final Name destName;

        /**
         * Create a new move {@link Operation} for the given arguments.
         *
         * @see Batch#move(NodeId, NodeId, Name)
         * @param srcNodeId
         * @param destParentNodeId
         * @param destName
         */
        public Move(NodeId srcNodeId, NodeId destParentNodeId, Name destName) {
            super();
            this.srcNodeId = srcNodeId;
            this.destParentNodeId = destParentNodeId;
            this.destName = destName;
        }

        /**
         * {@inheritDoc}
         */
        public void apply(Batch batch) throws RepositoryException {
            batch.move(srcNodeId, destParentNodeId, destName);
        }

        @Override
        public String toString() {
            return "Move[" + srcNodeId + ", " + destParentNodeId + ", " + destName + "]";
        }

        @Override
        public boolean equals(Object other) {
            if (null == other) {
                return false;
            }
            if (this == other) {
                return true;
            }
            if (other instanceof Move) {
                return equals((Move) other);
            }
            return false;
        }

        public boolean equals(Move other) {
            return Operations.equals(srcNodeId, other.srcNodeId)
                && Operations.equals(destParentNodeId, other.destParentNodeId)
                && Operations.equals(destName, other.destName);
        }

        @Override
        public int hashCode() {
            return 41 * (
                        41 * (
                            41 + Operations.hashCode(srcNodeId))
                        + Operations.hashCode(destParentNodeId))
                    + Operations.hashCode(destName);
        }
    }

    /**
     * Factory method for creating a {@link Move} operation.
     *
     * @see Batch#move(NodeId, NodeId, Name)
     * @param srcNodeId
     * @param destParentNodeId
     * @param destName
     * @return
     */
    public static Operation move(NodeId srcNodeId, NodeId destParentNodeId, Name destName) {
        return new Move(srcNodeId, destParentNodeId, destName);
    }

    // ------------------------------------------------------------< Remove >---
    /**
     * Representative of a remove {@link Operation} which calls {@link Batch#remove(ItemId)} when
     * applied to a {@link Batch}.
     */
    public static class Remove implements Operation {
        protected final ItemId itemId;

        /**
         * Create a new remove {@link Operation} for the given arguments.
         *
         * @see Batch#move(NodeId, NodeId, Name)
         * @param itemId
         */
        public Remove(ItemId itemId) {
            super();
            this.itemId = itemId;
        }

        /**
         * {@inheritDoc}
         */
        public void apply(Batch batch) throws RepositoryException {
            batch.remove(itemId);
        }

        @Override
        public String toString() {
            return "Remove[" + itemId + "]";
        }

        @Override
        public boolean equals(Object other) {
            if (null == other) {
                return false;
            }
            if (this == other) {
                return true;
            }
            if (other instanceof Remove) {
                return equals((Remove) other);
            }
            return false;
        }

        public boolean equals(Remove other) {
            return Operations.equals(itemId, other.itemId);
        }

        @Override
        public int hashCode() {
            return 41 + Operations.hashCode(itemId);
        }
    }

    /**
     * Factory method for creating a {@link Remove} operation.
     *
     * @see Batch#move(NodeId, NodeId, Name)
     * @param itemId
     * @return
     */
    public static Operation remove(ItemId itemId) {
        return new Remove(itemId);
    }

    // ------------------------------------------------------< ReorderNodes >---
    /**
     * Representative of a reorder-nodes {@link Operation} which calls
     * {@link Batch#reorderNodes(NodeId, NodeId, NodeId)} when applied to a {@link Batch}.
     */
    public static class ReorderNodes implements Operation {
        protected final NodeId parentId;
        protected final NodeId srcNodeId;
        protected final NodeId beforeNodeId;

        /**
         * Create a new reorder-nodes {@link Operation} for the given arguments.
         *
         * @see Batch#reorderNodes(NodeId, NodeId, NodeId)
         * @param parentId
         * @param srcNodeId
         * @param beforeNodeId
         */
        public ReorderNodes(NodeId parentId, NodeId srcNodeId, NodeId beforeNodeId) {
            super();
            this.parentId = parentId;
            this.srcNodeId = srcNodeId;
            this.beforeNodeId = beforeNodeId;
        }

        /**
         * {@inheritDoc}
         */
        public void apply(Batch batch) throws RepositoryException {
            batch.reorderNodes(parentId, srcNodeId, beforeNodeId);
        }

        @Override
        public String toString() {
            return "ReorderNodes[" + parentId + ", " + srcNodeId + ", " + beforeNodeId + "]";
        }

        @Override
        public boolean equals(Object other) {
            if (null == other) {
                return false;
            }
            if (this == other) {
                return true;
            }
            if (other instanceof ReorderNodes) {
                return equals((ReorderNodes) other);
            }
            return false;
        }

        public boolean equals(ReorderNodes other) {
            return Operations.equals(parentId, other.parentId)
                && Operations.equals(srcNodeId, other.srcNodeId)
                && Operations.equals(beforeNodeId, other.beforeNodeId);
        }

        @Override
        public int hashCode() {
            return 41 * (
                        41 * (
                            41 + Operations.hashCode(parentId))
                        + Operations.hashCode(srcNodeId))
                    + Operations.hashCode(beforeNodeId);
        }
    }

    /**
     * Factory method for creating a reorder-nodes {@link Operation} for the given arguments.
     *
     * @see Batch#reorderNodes(NodeId, NodeId, NodeId)
     * @param parentId
     * @param srcNodeId
     * @param beforeNodeId
     * @return
     */
    public static Operation reorderNodes(NodeId parentId, NodeId srcNodeId, NodeId beforeNodeId) {
        return new ReorderNodes(parentId, srcNodeId, beforeNodeId);
    }

    // ---------------------------------------------------------< SetMixins >---
    /**
     * Representative of a set-mixin {@link Operation} which calls
     * {@link Batch#setMixins(NodeId, Name[])} when applied to a {@link Batch}.
     */
    public static class SetMixins implements Operation {
        protected final NodeId nodeId;
        protected final Name[] mixinNodeTypeNames;

        /**
         * Create a new set-mixin {@link Operation} for the given arguments.
         *
         * @see Batch#setMixins(NodeId, Name[])
         * @param nodeId
         * @param mixinNodeTypeNames
         */
        public SetMixins(NodeId nodeId, Name[] mixinNodeTypeNames) {
            super();
            this.nodeId = nodeId;
            this.mixinNodeTypeNames = mixinNodeTypeNames;
        }

        /**
         * {@inheritDoc}
         */
        public void apply(Batch batch) throws RepositoryException {
            batch.setMixins(nodeId, mixinNodeTypeNames);
        }

        @Override
        public String toString() {
            return "SetMixins[" + nodeId + ", " + Arrays.toString(mixinNodeTypeNames) + "]";
        }

        @Override
        public boolean equals(Object other) {
            if (null == other) {
                return false;
            }
            if (this == other) {
                return true;
            }
            if (other instanceof SetMixins) {
                return equals((SetMixins) other);
            }
            return false;
        }

        public boolean equals(SetMixins other) {
            return Operations.equals(nodeId, other.nodeId)
                && Arrays.equals(mixinNodeTypeNames, other.mixinNodeTypeNames);
        }

        @Override
        public int hashCode() {
            return 41 * (
                        41 + Operations.hashCode(nodeId))
                    + Operations.hashCode(mixinNodeTypeNames);
        }
    }

    /**
     * Factory method for creating a set-mixin {@link Operation} for the given arguments.
     *
     * @see Batch#setMixins(NodeId, Name[])
     * @param nodeId
     * @param mixinNodeTypeNames
     * @return
     */
    public static Operation setMixins(NodeId nodeId, Name[] mixinNodeTypeNames) {
        return new SetMixins(nodeId, mixinNodeTypeNames);
    }

    // ----------------------------------------------------< SetPrimaryType >---
    /**
     * Representative of a set-mixin {@link Operation} which calls
     * {@link Batch#setMixins(NodeId, Name[])} when applied to a {@link Batch}.
     */
    public static class SetPrimaryType implements Operation {
        protected final NodeId nodeId;
        protected final Name primaryTypeName;

        /**
         * Create a new set-mixin {@link Operation} for the given arguments.
         *
         * @see Batch#setMixins(NodeId, Name[])
         * @param nodeId
         * @param primaryTypeName
         */
        public SetPrimaryType(NodeId nodeId, Name primaryTypeName) {
            super();
            this.nodeId = nodeId;
            this.primaryTypeName = primaryTypeName;
        }

        /**
         * {@inheritDoc}
         */
        public void apply(Batch batch) throws RepositoryException {
            batch.setPrimaryType(nodeId, primaryTypeName);
        }

        @Override
        public String toString() {
            return "SetPrimaryType[" + nodeId + ", " + primaryTypeName + "]";
        }

        @Override
        public boolean equals(Object other) {
            if (null == other) {
                return false;
            }
            if (this == other) {
                return true;
            }
            if (other instanceof SetPrimaryType) {
                return equals((SetPrimaryType) other);
            }
            return false;
        }

        public boolean equals(SetPrimaryType other) {
            return Operations.equals(nodeId, other.nodeId)
                && primaryTypeName.equals(other.primaryTypeName);
        }

        @Override
        public int hashCode() {
            return 41 * (
                    41 + Operations.hashCode(nodeId))
                    + Operations.hashCode(primaryTypeName);
        }
    }

    /**
     * Factory method for creating a set-primaryType {@link Operation} for the given arguments.
     *
     * @see Batch#setPrimaryType(NodeId, Name)
     * @param nodeId
     * @param primaryTypeName
     * @return
     */
    public static Operation setPrimaryType(NodeId nodeId, Name primaryTypeName) {
        return new SetPrimaryType(nodeId, primaryTypeName);
    }

    // ----------------------------------------------------------< SetValue >---
    /**
     * Representative of a set-value {@link Operation} which calls
     * {@link Batch#setValue(PropertyId, QValue)} or {@link Batch#setValue(PropertyId, QValue[])}
     * depending on whether the property is multi valued or not when applied to a {@link Batch}.
     */
    public static class SetValue implements Operation {
        protected final PropertyId propertyId;
        protected final QValue[] values;
        protected final boolean isMultivalued;

        private SetValue(PropertyId propertyId, QValue[] values, boolean isMultivalued) {
            super();
            this.propertyId = propertyId;
            this.values = values;
            this.isMultivalued = isMultivalued;
        }

        /**
         * Create a new set-value {@link Operation} for the given arguments.
         *
         * @see Batch#setValue(PropertyId, QValue)
         * @param propertyId
         * @param value
         */
        public SetValue(PropertyId propertyId, QValue value) {
            this(propertyId, new QValue[]{ value }, false);
        }

        /**
         * Create a new set-value {@link Operation} for the given arguments.
         *
         * @see Batch#setValue(PropertyId, QValue[])
         * @param propertyId
         * @param values
         */
        public SetValue(PropertyId propertyId, QValue[] values) {
            this(propertyId, values, true);
        }

        /**
         * {@inheritDoc}
         */
        public void apply(Batch batch) throws RepositoryException {
            if (isMultivalued) {
                batch.setValue(propertyId, values);
            }
            else {
                batch.setValue(propertyId, values[0]);
            }
        }

        @Override
        public String toString() {
            return "SetValue[" + propertyId + ", " + Arrays.toString(values) + "]";
        }

        @Override
        public boolean equals(Object other) {
            if (null == other) {
                return false;
            }
            if (this == other) {
                return true;
            }
            if (other instanceof SetValue) {
                return equals((SetValue) other);
            }
            return false;
        }

        public boolean equals(SetValue other) {
            return Operations.equals(propertyId, other.propertyId)
                && isMultivalued == other.isMultivalued
                && Arrays.equals(values, other.values);
        }

        @Override
        public int hashCode() {
            return 41 * (
                        41 + Operations.hashCode(propertyId))
                   + Operations.hashCode(values);
        }
    }

    /**
     * Factory method for creating set-value {@link Operation} for the given arguments.
     *
     * @see Batch#setValue(PropertyId, QValue)
     * @param propertyId
     * @param value
     * @return
     */
    public static Operation setValue(PropertyId propertyId, QValue value) {
        return new SetValue(propertyId, value);
    }

    /**
     * Factory method for creating a set-value {@link Operation} for the given arguments.
     *
     * @see Batch#setValue(PropertyId, QValue[])
     * @param propertyId
     * @param values
     * @return
     */
    public static Operation setValue(final PropertyId propertyId, final QValue[] values) {
        return new SetValue(propertyId, values);
    }

    // -----------------------------------------------------------< private >---

    protected static boolean equals(Object o1, Object o2) {
        return o1 == null
            ? o2 == null
            : o1.equals(o2);
    }

    protected static int hashCode(Object o) {
        return o == null
            ? 0
            : o.hashCode();
    }

    //--------------------------------------------------------------< SetTree >---
    public static class SetTree implements Operation {
        protected final NodeId parentId;
        protected final Tree tree;

        public SetTree(NodeId parentId, Tree tree) {
            super();
            this.parentId = parentId;
            this.tree = tree;
        }

        /**
         * {@inheritDoc}
         */
        public void apply(Batch batch) throws RepositoryException {
            batch.setTree(parentId, tree);
        }

        //----------------------------< Object >---
        @Override
        public String toString() {
            return "SetTree[" + parentId + ", " + tree+"]";
        }

        @Override
        public boolean equals(Object other) {
            if (null == other) {
                return false;
            }
            if (this == other) {
                return true;
            }
            if (other instanceof SetTree) {
                return equals((SetTree) other);
            }
            return false;
        }

        public boolean equals(SetTree other) {
            return Operations.equals(parentId, other.parentId)
                && Operations.equals(tree, other.tree);
        }

        @Override
        public int hashCode() {
            return 41 * (
                          41 + Operations.hashCode(parentId))
                       + Operations.hashCode(tree);
        }
    }

    /**
     * Factory method for creating an {@link SetTree} operation.
     * @see Batch#addNode(NodeId, Name, Name, String)
     */
    public static Operation setTree(NodeId parentId, Tree contentTree) {
        return new SetTree(parentId, contentTree);
    }
}
