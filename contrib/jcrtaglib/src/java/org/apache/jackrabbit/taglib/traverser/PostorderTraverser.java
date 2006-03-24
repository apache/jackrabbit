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

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.IteratorUtils;

/**
 * Postorder traverse strategy
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public class PostorderTraverser extends AbstractTraverser
{

    /**
     * Postorder traversal
     * 
     * @param node
     * @throws RepositoryException
     */
    private void postorder(Node node) throws RepositoryException
    {
        try
        {
            Object[] nodes = IteratorUtils.toArray(this.getChildren(node)) ;
            CollectionUtils.reverseArray(nodes);
            Iterator iter = IteratorUtils.arrayIterator(nodes);
            while (iter.hasNext())
            {
                this.postorder((Node) iter.next());
            }
        } catch (DepthExceededException e)
        {
            // Do nothing
        }
        visit(node);
    }

    /**
     * Traverse the node children tree
     * 
     * @throws RepositoryException
     */
    protected void internalTraverse() throws RepositoryException
    {
        this.postorder(this.node);
    }
}