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
package org.apache.jackrabbit.commons.visitor;

import java.util.LinkedList;

import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.predicate.Predicate;

public abstract class FilteringItemVisitor implements ItemVisitor {

    /**
     * Predicate that defines which items are included.
     */
    protected Predicate includePredicate = Predicate.TRUE;

    /**
     * Predicate that defines which items are traversed.
     */
    protected Predicate traversalPredicate = Predicate.TRUE;

    /**
     * Do we want to walk all properties of nodes?
     * The default is false.
     */
    protected boolean walkProperties = false;

    /**
     * indicates if traversal should be done in a breadth-first
     * manner rather than depth-first (which is the default)
     */
    protected boolean breadthFirst = false;

    /**
     * the 0-based level up to which the hierarchy should be traversed
     * (if it's -1, the hierarchy will be traversed until there are no
     * more children of the current item)
     */
    protected int maxLevel = -1;

    /**
     * queues used to implement breadth-first traversal
     */
    protected LinkedList currentQueue;
    protected LinkedList nextQueue;

    /**
     * used to track hierarchy level of item currently being processed
     */
    protected int currentLevel;

    public void setMaxLevel(final int ml) {
        this.maxLevel = ml;
    }

    public void setBreadthFirst(final boolean flag) {
        if ( this.breadthFirst != flag ) {
            this.breadthFirst = flag;
            if (breadthFirst) {
                this.currentQueue = new LinkedList();
                this.nextQueue = new LinkedList();
            } else {
                this.currentQueue = null;
                this.nextQueue = null;
            }

        }
    }
    public void setWalkProperties(final boolean flag) {
        this.walkProperties = flag;
    }

    public void setIncludePredicate(final Predicate ip) {
        this.includePredicate = ip;
    }

    public void setTraversalPredicate(final Predicate tp) {
        this.traversalPredicate = tp;
    }

    /**
     * Implement this method to add behaviour performed before a
     * <code>Property</code> is visited.
     *
     * @param property the <code>Property</code> that is accepting this visitor.
     * @param level    hierarchy level of this property (the root node starts at level 0)
     * @throws RepositoryException if an error occurrs
     */
    protected abstract void entering(Property property, int level)
        throws RepositoryException;

    /**
     * Implement this method to add behaviour performed before a
     * <code>Node</code> is visited.
     *
     * @param node  the <code>Node</code> that is accepting this visitor.
     * @param level hierarchy level of this node (the root node starts at level 0)
     * @throws RepositoryException if an error occurrs
     */
    protected abstract void entering(Node node, int level)
        throws RepositoryException;

    /**
     * Implement this method to add behaviour performed after a
     * <code>Property</code> is visited.
     *
     * @param property the <code>Property</code> that is accepting this visitor.
     * @param level    hierarchy level of this property (the root node starts at level 0)
     * @throws RepositoryException if an error occurrs
     */
    protected abstract void leaving(Property property, int level)
        throws RepositoryException;

    /**
     * Implement this method to add behaviour performed after a
     * <code>Node</code> is visited.
     *
     * @param node  the <code>Node</code> that is accepting this visitor.
     * @param level hierarchy level of this node (the root node starts at level 0)
     * @throws RepositoryException if an error occurrs
     */
    protected abstract void leaving(Node node, int level)
        throws RepositoryException;

    /**
     * Called when the Visitor is passed to a <code>Property</code>.
     * <p>
     * It calls <code>TraversingItemVisitor.entering(Property, int)</code> followed by
     * <code>TraversingItemVisitor.leaving(Property, int)</code>. Implement these abstract methods to
     * specify behaviour on 'arrival at' and 'after leaving' the <code>Property</code>.
     * <p>
     * <p>
     * If this method throws, the visiting process is aborted.
     *
     * @param property the <code>Property</code> that is accepting this visitor.
     * @throws RepositoryException if an error occurrs
     */
    public void visit(Property property) throws RepositoryException {
        if ( this.walkProperties && this.includePredicate.evaluate(property) ) {
            entering(property, currentLevel);
            leaving(property, currentLevel);
        }
    }

    /**
     * Called when the Visitor is passed to a <code>Node</code>.
     * <p>
     * It calls <code>TraversingItemVisitor.entering(Node, int)</code> followed by
     * <code>TraversingItemVisitor.leaving(Node, int)</code>. Implement these abstract methods to
     * specify behaviour on 'arrival at' and 'after leaving' the <code>Node</code>.
     * <p>
     * If this method throws, the visiting process is aborted.
     *
     * @param node the <code>Node</code> that is accepting this visitor.
     * @throws RepositoryException if an error occurrs
     */
    public void visit(Node node)
    throws RepositoryException {
        if ( this.traversalPredicate.evaluate(node) ) {
            if ( this.includePredicate == this.traversalPredicate || this.includePredicate.evaluate(node) )  {
                try {
                    if (!breadthFirst) {
                        // depth-first traversal
                        entering(node, currentLevel);
                        if (maxLevel == -1 || currentLevel < maxLevel) {
                            currentLevel++;
                            if ( this.walkProperties ) {
                                PropertyIterator propIter = node.getProperties();
                                while (propIter.hasNext()) {
                                    propIter.nextProperty().accept(this);
                                }
                            }
                            NodeIterator nodeIter = node.getNodes();
                            while (nodeIter.hasNext()) {
                                nodeIter.nextNode().accept(this);
                            }
                            currentLevel--;
                        }
                        leaving(node, currentLevel);
                    } else {
                        // breadth-first traversal
                        entering(node, currentLevel);
                        leaving(node, currentLevel);

                        if (maxLevel == -1 || currentLevel < maxLevel) {
                            if ( this.walkProperties ) {
                                PropertyIterator propIter = node.getProperties();
                                while (propIter.hasNext()) {
                                    nextQueue.addLast(propIter.nextProperty());
                                }
                            }
                            NodeIterator nodeIter = node.getNodes();
                            while (nodeIter.hasNext()) {
                                nextQueue.addLast(nodeIter.nextNode());
                            }
                        }

                        while (!currentQueue.isEmpty() || !nextQueue.isEmpty()) {
                            if (currentQueue.isEmpty()) {
                                currentLevel++;
                                currentQueue = nextQueue;
                                nextQueue = new LinkedList();
                            }
                            Item e = (Item) currentQueue.removeFirst();
                            e.accept(this);
                        }
                        currentLevel = 0;
                    }
                } catch (RepositoryException re) {
                    currentLevel = 0;
                    throw re;
                }
            }
        }
    }
}
