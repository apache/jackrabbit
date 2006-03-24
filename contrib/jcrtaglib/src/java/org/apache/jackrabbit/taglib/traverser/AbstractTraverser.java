/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.taglib.traverser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;

/**
 * Abstract Traverser
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public abstract class AbstractTraverser implements Traverser
{
    /**
     * Traverse root
     */
    protected Node node;

    /**
     * Container of traversed nodes
     */
    protected Collection nodes = new ArrayList();

    /**
     * Max depth from the relative root node
     */
    protected int depth = 0;

    /**
     * Per Children order
     */
    protected Comparator order;

    /**
     * Predicate (filter)
     */
    protected Predicate filter;

    /**
     * Parameter that affects the <code>Traverser<code> behaviour
     */
    protected Object parameter;

    /**
     * Constructor
     */
    public AbstractTraverser()
    {
        super();
    }

    /**
     * @return travesed nodes
     */
    public Collection getNodes()
    {
        return nodes ;
    }

    /**
     * Visit node
     * 
     * @param node
     */
    protected void visit(Node node)
    {
        if (this.filter== null || this.filter.evaluate(node))
        {
            this.nodes.add(node);
        } 
    }

    public int getDepth()
    {
        return depth;
    }

    public void setDepth(int depth)
    {
        this.depth = depth;
    }

    public Node getNode()
    {
        return node;
    }

    public void setNode(Node node)
    {
        this.node = node;
    }

    public void setFilter(Predicate predicate)
    {
        this.filter = predicate;
    }

    public void setOrder(Comparator comparator)
    {
        this.order = comparator;
    }

    /**
     * Get the children for the given node. <br>
     * Filtered and sorted if necesary.
     * 
     * @param node
     * @return @throws
     *         DepthExceededException
     */
    protected Iterator getChildren(Node node) throws DepthExceededException,
            RepositoryException
    {
        // Check depth
        if (node.getDepth() - this.node.getDepth() >= this.depth )
            throw new DepthExceededException();

        Iterator children = node.getNodes();

        // Sort
        if (this.order != null)
        {
            List l = IteratorUtils.toList(children);
            Collections.sort(l, this.order);
            children = l.iterator();
        }

        return children;
    }

    public void traverse() throws RepositoryException
    {
        this.nodes.clear();
        this.internalTraverse();
    }

    protected abstract void internalTraverse() throws RepositoryException;

    public void setParameter(Object parameter)
    {
        this.parameter = parameter;
    }
}