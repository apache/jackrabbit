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
package org.apache.jackrabbit.taglib.traverser;

import java.util.Collection;
import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.collections.IteratorUtils;
import org.apache.log4j.Logger;

/**
 * This Traverser collects the children of the ancestors in the path from the
 * root node to any of the target nodes. <br>
 * The parameter must be a Collection or Iterator containing the target nodes.
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class ExpandedNodesTraverser extends AbstractTraverser
{

	private static Logger log = Logger.getLogger(ExpandedNodesTraverser.class);

    /**
     * Preorder strategy recusively only for parent Nodes.
     * 
     * @param node
     * @throws RepositoryException
     */
    private void preorder(Node myNode) throws RepositoryException
    {
        visit(myNode);

        
        if (!this.isAncestor(myNode) && !this.getTarget().contains(myNode))
        {
            return;
        }

        try
        {
            Iterator iter = this.getChildren(myNode);
            while (iter.hasNext())
            {
                this.preorder((Node) iter.next());
            }
        } catch (DepthExceededException e)
        {
            log.error("Depth should never be exceeded in this traverser.", e);
        }
    }

    /**
     * @inheritDoc
     */
    protected void internalTraverse() throws RepositoryException
    {
        // Override depth.
        this.depth = Integer.MAX_VALUE;
        this.preorder(node);
    }

    /**
     * Sets the parameter. <br>
     * It only accepts Collection or Iterator instances.<br>
     * The path (String) or a Node instance;
     * 
     * @throws IllegalArgumentException
     */
    public void setParameter(Object param)
    {

        if (param == null
                || (!(param instanceof Collection) && !(param instanceof Iterator)))
        {
            throw new IllegalArgumentException(
                    "The parameter is not a Collection or Iterator. " + param);
        }
        if (param instanceof Collection)
        {
            this.parameter = (Collection) param;
        } else if (param instanceof Iterator)
        {
            this.parameter = IteratorUtils.toList((Iterator) param);
        }
    }

    /**
     * Checks if the node is ancestor of any af the target nodes
     * 
     * @param ancestor
     * @return @throws
     *         RepositoryException
     * @throws AccessDeniedException
     * @throws ItemNotFoundException
     */
    private boolean isAncestor(Node ancestor) throws ItemNotFoundException,
            AccessDeniedException, RepositoryException
    {
        Iterator iter = this.getTarget().iterator();
        while (iter.hasNext())
        {
            Node target = (Node) iter.next();
            if (this.isAncestor(ancestor, target))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the node is ancestor of the given target node.
     * 
     * @param ancestor
     * @param target
     * @return @throws
     *         RepositoryException
     * @throws AccessDeniedException
     * @throws ItemNotFoundException
     */
    private boolean isAncestor(Node ancestor, Node target)
            throws ItemNotFoundException, AccessDeniedException,
            RepositoryException
    {
        if (ancestor.getDepth() >= target.getDepth())
        {
            return false;
        }
        return target.getAncestor(ancestor.getDepth()).isSame(ancestor);
    }

    /**
     * Get the collection of target nodes.
     * 
     * @return
     */
    private Collection getTarget()
    {
        return (Collection) this.parameter;
    }

}