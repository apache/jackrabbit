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

import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.log4j.Logger;

/**
 * <p>
 * ExpandedNodeTraverser collects only the children of the ancestors in the path from
 * the root node to the target node.
 * </p>
 * <p>
 * The parameter must be a Node instance or a String with the path to the target
 * Node.
 * </p>
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class ExpandedNodeTraverser extends AbstractTraverser
{
	private static Logger log = Logger.getLogger(ExpandedNodeTraverser.class);

    /**
     * Preorder strategy recusively only for parent Nodes.
     * 
     * @param node
     * @throws RepositoryException
     */
    private void preorder(Node myNode) throws RepositoryException
    {
        visit(myNode);

        if (!this.isAncestor(myNode) && !this.getTarget().isSame(myNode))
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
     *  
     */
    protected void internalTraverse() throws RepositoryException
    {
        // Override depth.
        this.depth = Integer.MAX_VALUE;
        this.preorder(node);
    }

    /**
     * Validates the parameter. <br>
     * It only accepts the target Node. <br>
     * The path (String) or a Node instance;
     * 
     * @throws IllegalArgumentException
     */
    public void setParameter(Object param)
    {
        if (param == null
                || (!(param instanceof String) && !(param instanceof Node)))
        {
            throw new IllegalArgumentException(
                    "The parameter is not a Node. Class: " + param);
        }

        if (param instanceof String)
        {
            try
            {
                Node node = this.node.getSession().getRootNode().getNode(
                        (String) param);
                this.parameter = node;
            } catch (PathNotFoundException e)
            {
                throw new IllegalArgumentException("No node in " + parameter);
            } catch (RepositoryException e)
            {
                log.error("Unable to get node" + e.getMessage(), e);
            }
        } else
        {
            this.parameter = param;
        }

    }

    private Node getTarget()
    {
        return (Node) this.parameter;
    }

    private boolean isAncestor(Node ancestor) throws ItemNotFoundException,
            AccessDeniedException, RepositoryException
    {
        if (ancestor.getDepth() >= this.getTarget().getDepth())
        {
            return false;
        }
        return this.getTarget().getAncestor(ancestor.getDepth()).isSame(
                ancestor);
    }

}