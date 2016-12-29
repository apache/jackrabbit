/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.jackrabbit.commons.flat;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import java.util.Comparator;
import java.util.Set;

/**
 * TreeManager instances are responsible for the mapping between sequence views
 * of {@link Node}s and {@link Property}s and an hierarchical JCR
 * representation. They are passed to the various factory methods in
 * {@link ItemSequence} to parameterize the behavior of {@link NodeSequence}s
 * and {@link PropertySequence}s.
 *
 * @see NodeSequence
 * @see PropertySequence
 */
public interface TreeManager {

    /**
     * @return the root node of the JCR sub-tree where the items of the sequence
     *         are be mapped to.
     */
    Node getRoot();

    /**
     * Determined whether the given <code>node</code> is the root node of the
     * JCR sub-tree.
     *
     * @param node Node to test for root
     * @return <code>getRoot().isSame(node)</code>.
     * @throws RepositoryException
     */
    boolean isRoot(Node node) throws RepositoryException;

    /**
     * Determines whether the given <code>node</code> is a leaf. Leaf nodes are
     * the nodes which are actually part of a {@link NodeSequence} or the
     * parents of the properties of a {@link PropertySequence}.
     *
     * @param node Node to test for leaf
     * @return <code>true</code> if <code>node</code> is a leaf node,
     *         <code>false</code> otherwise.
     * @throws RepositoryException
     */
    boolean isLeaf(Node node) throws RepositoryException;

    /**
     * Properties to ignore.
     * @return
     */
    public Set<String> getIgnoredProperties();

    /**
     * {@link Comparator} used for establishing the order of the keys in the
     * sequence.
     *
     * @return a <code>Comparator&lt;String&gt;</code> instance
     */
    Comparator<String> getOrder();

    /**
     * After the node <code>cause</code> has been inserted into the sequence
     * <code>itemSequence</code>, the implementation of this method may decide
     * to split the parent <code>node</code> of <code>cause</code> into two or
     * more new nodes. Splitting must be done such that the overall order of the
     * keys in this sequence obeys the order given by {@link #getOrder()} as
     * much as possible.
     *
     * @param itemSequence the {@link ItemSequence} where the new node
     *            <code>cause</code> has been inserted.
     * @param node the parent node of the newly inserted node
     * @param cause the newly inserted node or <code>null</code> if not known.
     * @throws RepositoryException
     */
    void split(ItemSequence itemSequence, Node node, Node cause) throws RepositoryException;

    /**
     * After the property <code>cause</code> has been inserted into the sequence
     * <code>itemSequence</code>, the implementation of this method may decide
     * to split the parent <code>node</code> of <code>cause</code> into two or
     * more new nodes. Splitting must be done such that the overall order of the
     * keys in this sequence obeys the order given by {@link #getOrder()} as
     * much as possible.
     *
     * @param itemSequence the {@link ItemSequence} where the new property
     *            <code>cause</code> has been inserted.
     * @param node the parent node of the newly inserted property
     * @param cause the newly inserted property or <code>null</code> if not
     *            known.
     * @throws RepositoryException
     */
    void split(ItemSequence itemSequence, Node node, Property cause) throws RepositoryException;

    /**
     * After the node <code>cause</code> has been deleted from the sequence
     * <code>itemSequence</code>, the implementation of this method may decide
     * to join the parent <code>node</code> of <code>cause</code> with some
     * other nodes. Joining must be done such that the overall order of the keys
     * in this sequence obeys the order given by {@link #getOrder()} as much as
     * possible.
     *
     * @param itemSequence the {@link ItemSequence} where the node
     *            <code>cause</code> has been deleted from.
     * @param node the parent node from which <code>cause</code> has been
     *            deleted.
     * @param cause the deleted node or <code>null</code> if not known.
     *            <em>Note:</em> <code>cause</code> might be stale.
     * @throws RepositoryException
     */
    void join(ItemSequence itemSequence, Node node, Node cause) throws RepositoryException;

    /**
     * After the property <code>cause</code> has been deleted from the sequence
     * <code>itemSequence</code>, the implementation of this method may decide
     * to join the parent <code>node</code> of <code>cause</code> with some
     * other nodes. Joining must be done such that the overall order of the keys
     * in this sequence obeys the order given by {@link #getOrder()} as much as
     * possible.
     *
     * @param itemSequence the {@link ItemSequence} where the property
     *            <code>cause</code> has been deleted from.
     * @param node the parent node from which <code>cause</code> has been
     *            deleted.
     * @param cause the deleted property or <code>null</code> if not known.
     *            <em>Note:</em> <code>cause</code> might be stale.
     * @throws RepositoryException
     */
    void join(ItemSequence itemSequence, Node node, Property cause) throws RepositoryException;

    /**
     * Whether to automatically save changes of the current session occurring
     * from adding/removing nodes and properties.
     *
     * @return <code>true</code> if changes should be automatically saved,
     *         <code>false</code> otherwiese.
     */
    boolean getAutoSave();
}
