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

import java.util.Iterator;
import java.util.ListIterator;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.Batch;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.PropertyId;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.Tree;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;

/**
 * A {@link ChangeLog} implementation which does basic consolidation on its
 * {@link org.apache.jackrabbit.spi.commons.batch.Operation Operation}s. That is, cancelling
 * operations are removed if possible. In general this is not possible across
 * {@link org.apache.jackrabbit.spi.commons.batch.Operations.Move move} operations. The individual
 * {@link CancelableOperation CancelableOperation} implementations document their behavior
 * concerning cancellation.
 */
public class ConsolidatingChangeLog extends AbstractChangeLog<ConsolidatingChangeLog.CancelableOperation> {
    private static final PathFactory PATH_FACTORY = PathFactoryImpl.getInstance();

    /**
     * Create a new instance of a consolidating change log.
     */
    public ConsolidatingChangeLog() {
        super();
    }

    /**
     * Create a {@link Path} from the {@link NodeId} of a parent and the {@link Name} of a
     * child.
     * @param parentId  node id of the parent
     * @param name  name of the child
     * @return  the path of the item <code>name</code> or <code>null</code> if <code>parentId</code>'s
     * path is not absolute
     * @throws RepositoryException
     */
    protected static Path getPath(NodeId parentId, Name name) throws RepositoryException {
        Path parent = parentId.getPath();
        if (!parent.isAbsolute()) {
            return null;
        }

        return PATH_FACTORY.create(parent, name, true);
    }

    /**
     * Determine the {@link Path} from an {@link ItemId}.
     * @param itemId
     * @return  path of the item <code>itemId</code> or <code>null</code> if <code>itemId</code>'s
     * path is not absolute
     */
    protected static Path getPath(ItemId itemId) {
        Path path = itemId.getPath();
        if (path != null && !path.isAbsolute()) {
            return null;
        }
        return path;
    }

    // -----------------------------------------------------< ChangeLog >---

    public void addNode(NodeId parentId, Name nodeName, Name nodetypeName, String uuid)
            throws RepositoryException {

        addOperation(CancelableOperations.addNode(parentId, nodeName, nodetypeName, uuid));
    }

    public void addProperty(NodeId parentId, Name propertyName, QValue value) throws RepositoryException {
        addOperation(CancelableOperations.addProperty(parentId, propertyName, value));
    }

    public void addProperty(NodeId parentId, Name propertyName, QValue[] values) throws RepositoryException {
        addOperation(CancelableOperations.addProperty(parentId, propertyName, values));
    }

    public void move(NodeId srcNodeId, NodeId destParentNodeId, Name destName) throws RepositoryException {
        addOperation(CancelableOperations.move(srcNodeId, destParentNodeId, destName));
    }

    public void remove(ItemId itemId) throws RepositoryException {
        addOperation(CancelableOperations.remove(itemId));
    }

    public void reorderNodes(NodeId parentId, NodeId srcNodeId, NodeId beforeNodeId) throws RepositoryException {
        addOperation(CancelableOperations.reorderNodes(parentId, srcNodeId, beforeNodeId));
    }

    public void setMixins(NodeId nodeId, Name[] mixinNodeTypeNames) throws RepositoryException {
        addOperation(CancelableOperations.setMixins(nodeId, mixinNodeTypeNames));
    }

    public void setPrimaryType(NodeId nodeId, Name primaryNodeTypeName) throws RepositoryException {
        addOperation(CancelableOperations.setPrimaryType(nodeId, primaryNodeTypeName));
    }

    public void setValue(PropertyId propertyId, QValue value) throws RepositoryException {
        addOperation(CancelableOperations.setValue(propertyId, value));
    }

    public void setValue(PropertyId propertyId, QValue[] values) throws RepositoryException {
        addOperation(CancelableOperations.setValue(propertyId, values));
    }

    @Override
    public void setTree(NodeId parentId, Tree contentTree) throws RepositoryException {
        addOperation(CancelableOperations.setTree(parentId, contentTree));
    }

    /**
     * Determines the cancellation behavior from the list of {@link ChangeLogImpl#operations operations}
     * and the current operation <code>op</code>:
     * <ul>
     * <li>When the current operation is cancelled by the last operation, the list of operations
     *   is not modified.</li>
     * <li>When the current operation and the last operation cancel each other, the last operation is
     *   removed from the list of operations.</li>
     * <li>When the last operation is cancelled by this operation, the last operation is removed from
     *   the list of operations and determination of cancellation starts from scratch.</li>
     * <li>Otherwise add the current operation to the list of operations.</li>
     * </ul>
     */
    @Override
    public void addOperation(CancelableOperation op) throws RepositoryException {
        CancelableOperation otherOp = op;
        for (OperationsBackwardWithSentinel it = new OperationsBackwardWithSentinel(); it.hasNext(); ) {
            CancelableOperation thisOp = it.next();
            switch (thisOp.cancel(otherOp)) {
                case CancelableOperation.CANCEL_THIS:
                    it.remove();
                    continue;
                case CancelableOperation.CANCEL_OTHER:
                    return;
                case CancelableOperation.CANCEL_BOTH:
                    it.remove();
                    return;
                case CancelableOperation.CANCEL_NONE:
                    super.addOperation(otherOp);
                    return;
                default:
                    assert false : "Invalid case in switch";
            }
        }
    }

    // -----------------------------------------------------< private >---

    private class OperationsBackwardWithSentinel implements Iterator<CancelableOperation> {
        private final ListIterator<CancelableOperation> it = operations.listIterator(operations.size());
        private boolean last = !it.hasPrevious();
        private boolean done;

        public boolean hasNext() {
            return it.hasPrevious() || last;
        }

        public CancelableOperation next() {
            if (last) {
                done = true;
                return CancelableOperations.empty();
            }
            else {
                CancelableOperation o = it.previous();
                last = !it.hasPrevious();
                return o;
            }
        }

        public void remove() {
            if (done) {
                throw new IllegalStateException("Cannot remove last element");
            }
            else {
                it.remove();
            }
        }
    }

    // -----------------------------------------------------< CancelableOperations >---

    /**
     * This class represent an {@link Operation} which can be cancelled by another operation
     * or which cancels another operation.
     */
    protected interface CancelableOperation extends Operation {

        /**
         * The other operation cancels this operations
         */
        public static final int CANCEL_THIS = 0;

        /**
         * This operation cancels the other operation
         */
        public static final int CANCEL_OTHER = 1;

        /**
         * This operation and the other operation cancel each other mutually
         */
        public static final int CANCEL_BOTH = 2;

        /**
         * No cancellation
         */
        public static final int CANCEL_NONE = 3;

        /**
         * Determines the cancellation behavior of the <code>other</code> operation
         * on this operation.
         * @param other
         * @return  Either {@link #CANCEL_THIS}, {@link #CANCEL_OTHER}, {@link #CANCEL_OTHER}
         *   or {@link #CANCEL_NONE}
         * @throws RepositoryException
         */
        public int cancel(CancelableOperation other) throws RepositoryException;
    }

    /**
     * Factory for creating {@link ConsolidatingChangeLog.CancelableOperation CancelableOperation}s.
     * The inner classes of this class all implement the <code>CancelableOperation</code> interface.
     *
     * @see Operation
     */
    protected static final class CancelableOperations {
        private CancelableOperations() {
            super();
        }

        // -----------------------------------------------------< Empty >---

        /**
         * An <code>Empty</code> operation never cancels another operation and is never
         * cancelled by any other operation.
         */
        public static class Empty extends Operations.Empty implements CancelableOperation {

            /**
             * @return {@link ConsolidatingChangeLog.CancelableOperation#CANCEL_NONE}
             */
            public int cancel(CancelableOperation other) throws RepositoryException {
                return CANCEL_NONE;
            }
        }

        /**
         * Factory method for creating an {@link Empty Empty} operation.
         * @return
         */
        public static CancelableOperation empty() {
            return new Empty();
        }

        // -----------------------------------------------------< AddNode >---

        /**
         * An <code>AddNode</code> operation is is cancelled by a
         * {@link ConsolidatingChangeLog.CancelableOperations.Remove Remove} operation higher up the tree.
         * The remove operation is also cancelled if it is targeted at the same node than this add
         * operation.
         */
        public static class AddNode extends Operations.AddNode implements CancelableOperation {

            public AddNode(NodeId parentId, Name nodeName, Name nodetypeName, String uuid) {
                super(parentId, nodeName, nodetypeName, uuid);
            }

            /**
             * @return
             * <ul>
             * <li>{@link ConsolidatingChangeLog.CancelableOperation#CANCEL_BOTH CANCEL_BOTH} if
             *   <code>other</code> is an instance of
             *   {@link ConsolidatingChangeLog.CancelableOperations.Remove Remove} and has this node
             *   as target.</li>
             * <li>{@link ConsolidatingChangeLog.CancelableOperation#CANCEL_THIS CANCEL_THIS} if
             *  <code>other</code> is an instance of
             *  {@link ConsolidatingChangeLog.CancelableOperations.Remove Remove} and has an node higher up
             *  the hierarchy as target.</li>
             * <li>{@link ConsolidatingChangeLog.CancelableOperation#CANCEL_NONE CANCEL_NONE} otherwise.</li>
             * </ul>
             */
            public int cancel(CancelableOperation other) throws RepositoryException {
                if (other instanceof Remove) {
                    Path thisPath = ConsolidatingChangeLog.getPath(parentId, nodeName);
                    Path otherPath = ConsolidatingChangeLog.getPath(((Remove) other).itemId);
                    if (thisPath == null || otherPath == null) {
                        return CANCEL_NONE;
                    }
                    if (thisPath.equals(otherPath)) {
                        return CANCEL_BOTH;
                    }
                    return (thisPath.isDescendantOf(otherPath))
                        ? CANCEL_THIS
                        : CANCEL_NONE;
                }
                return CANCEL_NONE;
            }
        }

        /**
         * Factory method for creating an {@link AddNode AddNode} operation.
         * @see Batch#addNode(NodeId, Name, Name, String)
         *
         * @param parentId
         * @param nodeName
         * @param nodetypeName
         * @param uuid
         * @return
         */
        public static CancelableOperation addNode(NodeId parentId, Name nodeName, Name nodetypeName, String uuid) {
            return new AddNode(parentId, nodeName, nodetypeName, uuid);
        }

        // ---------------------------------------------------< AddProperty >---
        /**
         * <code>AddProperty</code> operations might cancel with
         * {@link ConsolidatingChangeLog.CancelableOperations.Remove Remove} and
         * {@link ConsolidatingChangeLog.CancelableOperations.SetValue SetValue} operations.
         */
        public static class AddProperty extends Operations.AddProperty implements CancelableOperation {

            public AddProperty(NodeId parentId, Name propertyName, QValue value) {
                super(parentId, propertyName, value);
            }

            public AddProperty(NodeId parentId, Name propertyName, QValue[] values) {
                super(parentId, propertyName, values);
            }

            /**
             * @return
             * <ul>
             * <li>{@link ConsolidatingChangeLog.CancelableOperation#CANCEL_BOTH CANCEL_BOTH} if
             *  <code>other</code> is an instance of
             *  {@link ConsolidatingChangeLog.CancelableOperations.Remove Remove} and has this property as
             *  target or if <code>other</code> is an instance of
             *  {@link ConsolidatingChangeLog.CancelableOperations.SetValue SetValue} for a value of
             *  <code>null</code> and has this property as target.</li>
             * <li>{@link ConsolidatingChangeLog.CancelableOperation#CANCEL_THIS CANCEL_THIS} if
             *   <code>other</code> is an instance of
             *   {@link ConsolidatingChangeLog.CancelableOperations.Remove Remove} and has a node higher up
             *   the hierarchy as target.</li>
             * <li>{@link ConsolidatingChangeLog.CancelableOperation#CANCEL_OTHER CANCEL_OTHER} if
             *   <code>other</code> is an instance of
             *   {@link ConsolidatingChangeLog.CancelableOperations.SetValue SetValue} and has this
             *   property as target.</li>
             * <li>{@link ConsolidatingChangeLog.CancelableOperation#CANCEL_NONE CANCEL_NONE} otherwise.</li>
             * </ul>
             */
            public int cancel(CancelableOperation other) throws RepositoryException {
                if (other instanceof Remove) {
                    Path thisPath = ConsolidatingChangeLog.getPath(parentId, propertyName);
                    Path otherPath = ConsolidatingChangeLog.getPath(((Remove) other).itemId);
                    if (thisPath == null || otherPath == null) {
                        return CANCEL_NONE;
                    }
                    if (thisPath.equals(otherPath)) {
                        return CANCEL_BOTH;
                    }
                    return (thisPath.isDescendantOf(otherPath))
                        ? CANCEL_THIS
                        : CANCEL_NONE;
                }
                if (other instanceof SetValue) {
                    SetValue setValue = (SetValue) other;
                    Path thisPath = ConsolidatingChangeLog.getPath(parentId, propertyName);
                    Path otherPath = ConsolidatingChangeLog.getPath(setValue.propertyId);
                    if (thisPath == null || otherPath == null) {
                        return CANCEL_NONE;
                    }
                    if (thisPath.equals(otherPath)) {
                        if (!isMultivalued && setValue.values[0] == null) {
                            return CANCEL_BOTH;
                        }
                        else if (values.length == setValue.values.length) {
                            for (int k = 0; k < values.length; k++) {
                                if (!values[k].equals(setValue.values[k])) {
                                    return CANCEL_NONE;
                                }
                            }
                            return CANCEL_OTHER;
                        }
                    }
                }
                return CANCEL_NONE;
            }
        }

        /**
         * Factory method for creating an {@link AddProperty AddProperty} operation.
         *
         * @see Batch#addProperty(NodeId, Name, QValue)
         * @param parentId
         * @param propertyName
         * @param value
         * @return
         */
        public static CancelableOperation addProperty(NodeId parentId, Name propertyName, QValue value) {
            return new AddProperty(parentId, propertyName, value);
        }

        /**
         * Factory method for creating an {@link AddProperty AddProperty} operation.
         *
         * @see Batch#addProperty(NodeId, Name, QValue[])
         * @param parentId
         * @param propertyName
         * @param values
         * @return
         */
        public static CancelableOperation addProperty(NodeId parentId, Name propertyName, QValue[] values) {
            return new AddProperty(parentId, propertyName, values);
        }

        // ----------------------------------------------------------< Move >---
        /**
         * An <code>Move</code> operation never cancels another operation and is never
         * cancelled by any other operation.
         */
        public static class Move extends Operations.Move implements CancelableOperation {

            public Move(NodeId srcNodeId, NodeId destParentNodeId, Name destName) {
                super(srcNodeId, destParentNodeId, destName);
            }

            /**
             * @return {@link ConsolidatingChangeLog.CancelableOperation#CANCEL_NONE CANCEL_NONE}
             */
            public int cancel(CancelableOperation other) {
                return CANCEL_NONE;
            }
        }

        /**
         * Factory method for creating a {@link Move Move} operation.
         *
         * @see Batch#move(NodeId, NodeId, Name)
         * @param srcNodeId
         * @param destParentNodeId
         * @param destName
         * @return
         */
        public static CancelableOperation move(NodeId srcNodeId, NodeId destParentNodeId, Name destName) {
            return new Move(srcNodeId, destParentNodeId, destName);
        }

        // --------------------------------------------------------< Remove >---
        /**
         * An <code>Remove</code> operation never cancels another operation and is never
         * cancelled by any other operation.
         */
        public static class Remove extends Operations.Remove implements CancelableOperation {

            public Remove(ItemId itemId) {
                super(itemId);
            }

            /**
             * @return {@link ConsolidatingChangeLog.CancelableOperation#CANCEL_NONE CANCEL_NONE}
             */
            public int cancel(CancelableOperation other) {
                return CANCEL_NONE;
            }
        }

        /**
         * Factory method for creating a {@link Remove Remove} operation.
         *
         * @see Batch#move(NodeId, NodeId, Name)
         * @param itemId
         * @return
         */
        public static CancelableOperation remove(ItemId itemId) {
            return new Remove(itemId);
        }

        // -------------------------------------------------< Reorder Nodes >---
        /**
         * A <code>ReorderNodes</code> operation might cancel with
         * {@link ConsolidatingChangeLog.CancelableOperations.Remove Remove} and
         * {@link ConsolidatingChangeLog.CancelableOperations.ReorderNodes ReorderNodes} operations.
         */
        public static class ReorderNodes extends Operations.ReorderNodes implements CancelableOperation {

            public ReorderNodes(NodeId parentId, NodeId srcNodeId, NodeId beforeNodeId) {
                super(parentId, srcNodeId, beforeNodeId);
            }

            /**
             * @return
             * <ul>
             * <li>{@link ConsolidatingChangeLog.CancelableOperation#CANCEL_THIS CANCEL_THIS} if
             *   <code>other</code> is an instance of
             *   {@link ConsolidatingChangeLog.CancelableOperations.Remove Remove} and has an node higher up
             *   the hierarchy or this node as target. Or if <code>other</code> is an instance of
             *   {@link ConsolidatingChangeLog.CancelableOperations.ReorderNodes ReorderNodes} which
             *   has this node as target and neither <code>srcNodeId</code> nor <code>beforeNodeId</code>
             *   has same name siblings.</li>
             * <li>{@link ConsolidatingChangeLog.CancelableOperation#CANCEL_NONE CANCEL_NONE} otherwise.</li>
             * </ul>
             */
            public int cancel(CancelableOperation other) throws RepositoryException {
                if (other instanceof Remove) {
                    Path thisPath = ConsolidatingChangeLog.getPath(srcNodeId);
                    Path otherPath = ConsolidatingChangeLog.getPath(((Remove) other).itemId);
                    if (thisPath == null || otherPath == null) {
                        return CANCEL_NONE;
                    }
                    return thisPath.isDescendantOf(otherPath) || thisPath.equals(otherPath)
                        ? CANCEL_THIS
                        : CANCEL_NONE;
                }
                if (other instanceof ReorderNodes) {
                    Path thisPath = ConsolidatingChangeLog.getPath(parentId);
                    Path otherPath = ConsolidatingChangeLog.getPath(((ReorderNodes) other).parentId);
                    if (thisPath == null || otherPath == null) {
                        return CANCEL_NONE;
                    }
                    return thisPath.equals(otherPath) && !hasSNS(srcNodeId) && !hasSNS(beforeNodeId)
                        ? CANCEL_THIS
                        : CANCEL_NONE;
                }
                return CANCEL_NONE;
            }

            private boolean hasSNS(NodeId nodeId) {
                if (nodeId != null) {
                    Path path = ConsolidatingChangeLog.getPath(nodeId);
                    return path != null && path.getIndex() > 1;
                }

                return false;
            }
        }

        /**
         * Factory method for creating a {@link ReorderNodes ReorderNodes} operation.
         *
         * @see Batch#reorderNodes(NodeId, NodeId, NodeId)
         * @param parentId
         * @param srcNodeId
         * @param beforeNodeId
         * @return
         */
        public static CancelableOperation reorderNodes(NodeId parentId, NodeId srcNodeId, NodeId beforeNodeId) {
            return new ReorderNodes(parentId, srcNodeId, beforeNodeId);
        }

        // -----------------------------------------------------< SetMixins >---
        /**
         * A <code>SetMixins</code> operation might cancel with
         * {@link ConsolidatingChangeLog.CancelableOperations.Remove Remove} and
         * {@link ConsolidatingChangeLog.CancelableOperations.SetMixins SetMixins} operations.
         */
        public static class SetMixins extends Operations.SetMixins implements CancelableOperation {

            public SetMixins(NodeId nodeId, Name[] mixinNodeTypeNames) {
                super(nodeId, mixinNodeTypeNames);
            }

            /**
             * @return
             * <ul>
             * <li>{@link ConsolidatingChangeLog.CancelableOperation#CANCEL_THIS CANCEL_THIS} if
             *   <code>other</code> is an instance of
             *   {@link ConsolidatingChangeLog.CancelableOperations.Remove Remove} and has an node higher up
             *   the hierarchy or this node as target. Or if <code>other</code> is an instance of
             *   {@link ConsolidatingChangeLog.CancelableOperations.SetMixins SetMixins} which has this node
             *   as target and has the same <code>mixinNodeTypeNames</code>.</li>
             * <li>{@link ConsolidatingChangeLog.CancelableOperation#CANCEL_NONE CANCEL_NONE} otherwise.</li>
             * </ul>
             */
            public int cancel(CancelableOperation other) throws RepositoryException {
                if (other instanceof Remove) {
                    Path thisPath = ConsolidatingChangeLog.getPath(nodeId);
                    Path otherPath = ConsolidatingChangeLog.getPath(((Remove) other).itemId);
                    if (thisPath == null || otherPath == null) {
                        return CANCEL_NONE;
                    }
                    return thisPath.isDescendantOf(otherPath) || thisPath.equals(otherPath)
                        ? CANCEL_THIS
                        : CANCEL_NONE;
                }
                if (other instanceof SetMixins) {
                    SetMixins setMixin = (SetMixins) other;
                    if (mixinNodeTypeNames.length == setMixin.mixinNodeTypeNames.length) {
                        Path thisPath = ConsolidatingChangeLog.getPath(nodeId);
                        Path otherPath = ConsolidatingChangeLog.getPath(setMixin.nodeId);
                        if (thisPath == null || otherPath == null) {
                            return CANCEL_NONE;
                        }
                        if (thisPath.equals(otherPath)) {
                            for (int k = 0; k < mixinNodeTypeNames.length; k++) {
                                if (!mixinNodeTypeNames[k].equals(setMixin.mixinNodeTypeNames[k])) {
                                    return CANCEL_NONE;
                                }
                            }
                            return CANCEL_THIS;
                        }
                    }
                }
                return CANCEL_NONE;
            }
        }

        /**
         * Factory method for creating a {@link SetMixins} operation.
         *
         * @see Batch#setMixins(NodeId, Name[])
         * @param nodeId
         * @param mixinNodeTypeNames
         * @return
         */
        public static CancelableOperation setMixins(NodeId nodeId, Name[] mixinNodeTypeNames) {
            return new SetMixins(nodeId, mixinNodeTypeNames);
        }

        // -----------------------------------------------------< SetMixins >---
        /**
         * A <code>SetPrimaryType</code> operation might cancel with
         * {@link ConsolidatingChangeLog.CancelableOperations.Remove Remove} and
         * {@link ConsolidatingChangeLog.CancelableOperations.SetPrimaryType SetPrimaryType} operations.
         */
        public static class SetPrimaryType extends Operations.SetPrimaryType implements CancelableOperation {

            public SetPrimaryType(NodeId nodeId, Name primaryTypeName) {
                super(nodeId, primaryTypeName);
            }

            /**
             * @return
             * <ul>
             * <li>{@link ConsolidatingChangeLog.CancelableOperation#CANCEL_THIS CANCEL_THIS} if
             *   <code>other</code> is an instance of
             *   {@link ConsolidatingChangeLog.CancelableOperations.Remove Remove} and has an node higher up
             *   the hierarchy or this node as target. Or if <code>other</code> is an instance of
             *   {@link ConsolidatingChangeLog.CancelableOperations.SetMixins SetMixins} which has this node
             *   as target and has the same <code>mixinNodeTypeNames</code>.</li>
             * <li>{@link ConsolidatingChangeLog.CancelableOperation#CANCEL_NONE CANCEL_NONE} otherwise.</li>
             * </ul>
             */
            public int cancel(CancelableOperation other) throws RepositoryException {
                if (other instanceof Remove) {
                    Path thisPath = ConsolidatingChangeLog.getPath(nodeId);
                    Path otherPath = ConsolidatingChangeLog.getPath(((Remove) other).itemId);
                    if (thisPath == null || otherPath == null) {
                        return CANCEL_NONE;
                    }
                    return thisPath.isDescendantOf(otherPath) || thisPath.equals(otherPath)
                        ? CANCEL_THIS
                        : CANCEL_NONE;
                }
                if (other instanceof SetPrimaryType) {
                    SetPrimaryType setPrimaryType = (SetPrimaryType) other;
                    if (primaryTypeName.equals(setPrimaryType.primaryTypeName)) {
                        Path thisPath = ConsolidatingChangeLog.getPath(nodeId);
                        Path otherPath = ConsolidatingChangeLog.getPath(setPrimaryType.nodeId);
                        if (thisPath == null || otherPath == null) {
                            return CANCEL_NONE;
                        }
                        if (thisPath.equals(otherPath)) {
                            return CANCEL_THIS;
                        }
                    }
                }
                return CANCEL_NONE;
            }
        }

        /**
         * Factory method for creating a {@link SetPrimaryType} operation.
         *
         * @see Batch#setPrimaryType(NodeId, Name)
         * @param nodeId
         * @param primaryTypeName
         * @return
         */
        public static CancelableOperation setPrimaryType(NodeId nodeId, Name primaryTypeName) {
            return new SetPrimaryType(nodeId, primaryTypeName);
        }

        // ------------------------------------------------------< SetValue >---
        /**
         * A <code>SetValue</code> operation might cancel with
         * {@link ConsolidatingChangeLog.CancelableOperations.Remove Remove} and
         * {@link ConsolidatingChangeLog.CancelableOperations.SetValue SetValue} operations.
         */
        public static class SetValue extends Operations.SetValue implements CancelableOperation {
            public SetValue(PropertyId propertyId, QValue value) {
                super(propertyId, value);
            }

            public SetValue(PropertyId propertyId, QValue[] values) {
                super(propertyId, values);
            }

            /**
             * @return
             * <ul>
             * <li>{@link ConsolidatingChangeLog.CancelableOperation#CANCEL_THIS CANCEL_THIS} if
             *   <code>other</code> is an instance of
             *   {@link ConsolidatingChangeLog.CancelableOperations.Remove Remove} and has an node higher up
             *   the hierarchy or this node as target. Or if <code>other</code> is an instance of
             *   {@link ConsolidatingChangeLog.CancelableOperations.SetValue SetValue} which has this
             *   property as target</li>
             * <li>{@link ConsolidatingChangeLog.CancelableOperation#CANCEL_NONE CANCEL_NONE} otherwise.</li>
             * </ul>
             */
            public int cancel(CancelableOperation other) throws RepositoryException {
                if (other instanceof Remove) {
                    Path thisPath = ConsolidatingChangeLog.getPath(propertyId);
                    Path otherPath = ConsolidatingChangeLog.getPath(((Remove) other).itemId);
                    if (thisPath == null || otherPath == null) {
                        return CANCEL_NONE;
                    }
                    return thisPath.isDescendantOf(otherPath) || thisPath.equals(otherPath)
                        ? CANCEL_THIS
                        : CANCEL_NONE;
                }
                if (other instanceof SetValue) {
                    Path thisPath = ConsolidatingChangeLog.getPath(propertyId);
                    Path otherPath = ConsolidatingChangeLog.getPath(((SetValue) other).propertyId);
                    if (thisPath == null || otherPath == null) {
                        return CANCEL_NONE;
                    }
                    if (thisPath.equals(otherPath)) {
                        return CANCEL_THIS;
                    }
                }
                return CANCEL_NONE;
            }
        }

        /**
         * Factory method for creating a {@link SetValue SetValue} operation.
         *
         * @see Batch#setValue(PropertyId, QValue)
         * @param propertyId
         * @param value
         * @return
         */
        public static CancelableOperation setValue(PropertyId propertyId, QValue value) {
            return new SetValue(propertyId, value);
        }

        /**
         * Factory method for creating a {@link SetValue SetValue} operation.
         *
         * @see Batch#setValue(PropertyId, QValue[])
         * @param propertyId
         * @param values
         * @return
         */
        public static CancelableOperation setValue(PropertyId propertyId, QValue[] values) {
            return new SetValue(propertyId, values);
        }


        //--------------------------------------------------------< SetTree >---
        public static class SetTree extends Operations.SetTree implements CancelableOperation {

            public SetTree(NodeId parentId, Tree contentTree) {
                super(parentId, contentTree);
            }

            /**
             * The cancellation only considers canceling the parent node, which corresponds
             * to the policy node.
             */
            public int cancel(CancelableOperation other) throws RepositoryException {
                if (other instanceof Remove) {
                    Path thisPath = ConsolidatingChangeLog.getPath(parentId, tree.getName());
                    Path otherPath = ConsolidatingChangeLog.getPath(((Remove) other).itemId);
                    if (thisPath == null || otherPath == null) {
                        return CANCEL_NONE;
                    }
                    if (thisPath.equals(otherPath)) {
                        return CANCEL_BOTH;
                    }
                    return (thisPath.isDescendantOf(otherPath))
                            ? CANCEL_THIS
                            : CANCEL_NONE;
                }
                return CANCEL_NONE;
            }
        }

        /**
         * Factory method for creating an {@link SetTree} operation.
         * @see Batch#setTree(NodeId, Tree)
         *
         * @param parentId
         * @param tree
         * @return
         */
        public static CancelableOperation setTree(NodeId parentId, Tree tree) {
            return new SetTree(parentId, tree);
        }
    }

}
